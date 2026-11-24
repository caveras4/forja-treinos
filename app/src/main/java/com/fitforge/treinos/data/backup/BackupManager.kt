package com.fitforge.treinos.data.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.fitforge.treinos.data.local.AppDatabase
import com.fitforge.treinos.data.local.ExerciseEntity
import com.fitforge.treinos.data.media.CustomMediaStorage
import com.fitforge.treinos.data.repository.LocalWorkoutRepository
import com.fitforge.treinos.domain.MediaCheck
import com.fitforge.treinos.domain.MediaValidator
import com.fitforge.treinos.domain.transfer.BackupCodec
import com.fitforge.treinos.domain.transfer.BackupDecodeResult
import com.fitforge.treinos.domain.transfer.BackupFile
import com.fitforge.treinos.domain.transfer.CustomExerciseJson
import com.fitforge.treinos.domain.transfer.TransferFormat
import com.fitforge.treinos.domain.transfer.WorkoutExerciseJson
import com.fitforge.treinos.domain.transfer.WorkoutJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

sealed interface BackupResult {
    data class Exported(val workouts: Int, val customExercises: Int) : BackupResult
    data class Imported(val workouts: Int, val customExercises: Int, val skippedExercises: Int) : BackupResult
    data class Failed(val message: String) : BackupResult
}

/**
 * Backup completo em .zip: backup.json (treinos + exercícios personalizados) e a pasta
 * midia/ com as imagens personalizadas. Tudo local, escolhido pelo seletor de arquivos.
 */
class BackupManager(
    private val contentResolver: ContentResolver,
    private val database: AppDatabase,
    private val workoutRepository: LocalWorkoutRepository,
    private val mediaStorage: CustomMediaStorage
) {

    suspend fun export(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val workouts = database.workoutDao().getAll()
            val items = database.workoutExerciseDao().getAll().groupBy { it.workoutId }
            val customs = database.exerciseDao().getCustom()

            val file = BackupFile(
                formato = TransferFormat.BACKUP_FORMAT,
                versao = TransferFormat.VERSION,
                exerciciosPersonalizados = customs.map {
                    CustomExerciseJson(id = it.id, nome = it.name, grupo = it.muscleGroup, midia = it.mediaPath)
                },
                treinos = workouts.map { w ->
                    WorkoutJson(
                        nome = w.name,
                        foco = w.focus,
                        cor = w.colorKey,
                        exercicios = items[w.id].orEmpty().map {
                            WorkoutExerciseJson(it.exerciseId, it.sets, it.reps, it.weightKg)
                        }
                    )
                }.filter { it.exercicios.isNotEmpty() }
            )

            val out = contentResolver.openOutputStream(uri, "wt")
                ?: return@withContext BackupResult.Failed("Não foi possível criar o arquivo.")
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry(TransferFormat.BACKUP_JSON_ENTRY))
                zip.write(BackupCodec.encode(file).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                customs.mapNotNull { it.mediaPath }.forEach { name ->
                    val media = mediaStorage.file(name)
                    if (media.exists()) {
                        zip.putNextEntry(ZipEntry(TransferFormat.BACKUP_MEDIA_DIR + name))
                        media.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
            BackupResult.Exported(file.treinos.size, customs.size)
        } catch (e: Exception) {
            BackupResult.Failed("Falha ao exportar: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /** Substitui TODOS os treinos e exercícios personalizados pelos do backup. */
    suspend fun import(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val input = contentResolver.openInputStream(uri)
                ?: return@withContext BackupResult.Failed("Não foi possível abrir o arquivo.")
            val (jsonText, media) = input.use { readZip(it) }
                ?: return@withContext BackupResult.Failed("Arquivo de backup inválido ou corrompido.")

            for ((name, bytes) in media) {
                val check = mediaStorage.checkBytes(bytes)
                if (check !is MediaCheck.Ok) {
                    return@withContext BackupResult.Failed("Imagem $name recusada: ${MediaValidator.message(check)}")
                }
            }

            val catalogIds = database.exerciseDao().getAllIds().filterNot { it.startsWith("custom-") }.toSet()
            val content = when (val decoded = BackupCodec.decode(jsonText, catalogIds, media.keys)) {
                is BackupDecodeResult.Error -> return@withContext BackupResult.Failed(decoded.message)
                is BackupDecodeResult.Ok -> decoded.content
            }

            content.customExercises.mapNotNull { it.midia }.forEach { name ->
                mediaStorage.write(name, media.getValue(name))
            }

            database.withTransaction {
                database.workoutDao().deleteAll()
                database.exerciseDao().deleteAllCustom()
                database.exerciseDao().insertAll(
                    content.customExercises.map {
                        ExerciseEntity(it.id, it.nome.trim(), it.grupo, it.midia, isCustom = true)
                    }
                )
                content.workouts.forEach { workoutRepository.insertOrReplace(it, workoutId = null) }
            }
            mediaStorage.deleteOrphans(content.customExercises.mapNotNull { it.midia }.toSet())

            BackupResult.Imported(
                workouts = content.workouts.size,
                customExercises = content.customExercises.size,
                skippedExercises = content.skippedExerciseIds.size
            )
        } catch (e: Exception) {
            BackupResult.Failed("Falha ao importar: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /**
     * Lê só as entradas esperadas, com limite de tamanho por entrada e de quantidade.
     * Nomes fora do padrão (ex.: "../x") são ignorados, então não há como escrever fora da pasta.
     */
    private fun readZip(input: InputStream): Pair<String, Map<String, ByteArray>>? {
        var json: String? = null
        val media = LinkedHashMap<String, ByteArray>()
        var entries = 0
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++entries > MAX_ENTRIES) return null
                val name = entry.name
                when {
                    entry.isDirectory -> Unit
                    name == TransferFormat.BACKUP_JSON_ENTRY -> {
                        val bytes = readEntry(zip, TransferFormat.BACKUP_JSON_MAX_BYTES) ?: return null
                        json = bytes.toString(Charsets.UTF_8)
                    }
                    name.startsWith(TransferFormat.BACKUP_MEDIA_DIR) -> {
                        val fileName = name.removePrefix(TransferFormat.BACKUP_MEDIA_DIR)
                        if (TransferFormat.SAFE_MEDIA_NAME.matches(fileName)) {
                            media[fileName] = readEntry(zip, MediaValidator.MAX_BYTES) ?: return null
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        return json?.let { it to media }
    }

    private fun readEntry(zip: ZipInputStream, limit: Long): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val read = zip.read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private companion object {
        const val MAX_ENTRIES = 600
    }
}
