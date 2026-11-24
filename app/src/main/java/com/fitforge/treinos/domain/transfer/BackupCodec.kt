package com.fitforge.treinos.domain.transfer

import com.fitforge.treinos.domain.MuscleGroups
import com.fitforge.treinos.domain.WorkoutDraft
import com.fitforge.treinos.domain.WorkoutRules
import kotlinx.serialization.SerializationException

/** Backup já validado, pronto para substituir os dados do aparelho. */
data class BackupContent(
    val customExercises: List<CustomExerciseJson>,
    val workouts: List<WorkoutDraft>,
    /** Exercícios citados nos treinos que não existem nem no catálogo nem no backup. */
    val skippedExerciseIds: List<String>
)

sealed interface BackupDecodeResult {
    data class Error(val message: String) : BackupDecodeResult
    data class Ok(val content: BackupContent) : BackupDecodeResult
}

object BackupCodec {

    fun encode(file: BackupFile): String = TransferFormat.json.encodeToString(BackupFile.serializer(), file)

    /**
     * @param catalogIds ids do catálogo embutido no app.
     * @param mediaInZip nomes dos arquivos encontrados em "midia/" no zip.
     */
    fun decode(text: String, catalogIds: Set<String>, mediaInZip: Set<String>): BackupDecodeResult {
        val file = try {
            TransferFormat.json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            return BackupDecodeResult.Error("backup.json inválido.")
        } catch (e: IllegalArgumentException) {
            return BackupDecodeResult.Error("backup.json inválido.")
        }
        if (file.formato != TransferFormat.BACKUP_FORMAT) {
            return BackupDecodeResult.Error("Este arquivo não é um backup do Forja Treinos.")
        }
        if (file.versao != TransferFormat.VERSION) {
            return BackupDecodeResult.Error("Versão de backup ${file.versao} não suportada.")
        }
        if (file.treinos.size > TransferFormat.BACKUP_MAX_WORKOUTS ||
            file.exerciciosPersonalizados.size > TransferFormat.BACKUP_MAX_CUSTOM
        ) {
            return BackupDecodeResult.Error("Backup com itens demais.")
        }

        val customIds = HashSet<String>()
        for (custom in file.exerciciosPersonalizados) {
            if (!custom.id.startsWith("custom-") || custom.id in catalogIds || !customIds.add(custom.id)) {
                return BackupDecodeResult.Error("Exercício personalizado com id inválido: ${custom.id}.")
            }
            WorkoutRules.validateExerciseName(custom.nome)?.let {
                return BackupDecodeResult.Error("Exercício \"${custom.nome}\": $it")
            }
            if (custom.grupo !in MuscleGroups.ORDER) {
                return BackupDecodeResult.Error("Exercício \"${custom.nome}\": grupo \"${custom.grupo}\" desconhecido.")
            }
            val media = custom.midia
            if (media != null && (!TransferFormat.SAFE_MEDIA_NAME.matches(media) || media !in mediaInZip)) {
                return BackupDecodeResult.Error("Imagem do exercício \"${custom.nome}\" ausente ou inválida.")
            }
        }

        val known = catalogIds + customIds
        val skipped = LinkedHashSet<String>()
        val drafts = file.treinos.mapIndexed { index, treino ->
            validateWorkoutJson(treino)?.let {
                return BackupDecodeResult.Error("Treino ${index + 1} (\"${treino.nome}\"): $it")
            }
            val (draft, unknown) = treino.toDraft(known)
            skipped += unknown
            draft
        }
        return BackupDecodeResult.Ok(
            BackupContent(
                customExercises = file.exerciciosPersonalizados,
                workouts = drafts.filter { it.items.isNotEmpty() },
                skippedExerciseIds = skipped.toList()
            )
        )
    }
}
