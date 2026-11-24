package com.fitforge.treinos.data.media

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.fitforge.treinos.domain.MediaCheck
import com.fitforge.treinos.domain.MediaValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.UUID

sealed interface MediaImportResult {
    data class Ok(val fileName: String) : MediaImportResult
    data class Rejected(val message: String) : MediaImportResult
}

/**
 * Imagens dos exercícios personalizados. O arquivo escolhido pelo usuário é validado e
 * copiado para filesDir/custom_media, porque o Uri original pode deixar de existir.
 */
class CustomMediaStorage(private val context: Context) {

    val directory: File get() = File(context.filesDir, DIR).apply { mkdirs() }

    fun file(fileName: String): File = File(directory, fileName)

    suspend fun import(uri: Uri): MediaImportResult = withContext(Dispatchers.IO) {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { readLimited(it) }
        } catch (e: Exception) {
            null
        } ?: return@withContext MediaImportResult.Rejected("Não foi possível abrir o arquivo.")

        // Mais bytes que o limite: readLimited para de ler logo depois de MAX_BYTES.
        if (bytes.size > MediaValidator.MAX_BYTES) {
            return@withContext MediaImportResult.Rejected(
                MediaValidator.message(MediaCheck.TooLarge(bytes.size.toLong()))!!
            )
        }
        when (val check = checkBytes(bytes)) {
            is MediaCheck.Ok -> {
                val name = "custom-${UUID.randomUUID()}.${check.format.extension}"
                file(name).writeBytes(bytes)
                MediaImportResult.Ok(name)
            }
            else -> MediaImportResult.Rejected(MediaValidator.message(check)!!)
        }
    }

    /** Usado pela importação de backup, que já leu os bytes do zip. */
    fun checkBytes(bytes: ByteArray): MediaCheck {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return MediaValidator.validate(
            sizeBytes = bytes.size.toLong(),
            header = bytes.copyOf(minOf(bytes.size, MediaValidator.HEADER_SIZE)),
            width = options.outWidth,
            height = options.outHeight
        )
    }

    fun write(fileName: String, bytes: ByteArray) {
        file(fileName).writeBytes(bytes)
    }

    fun delete(fileName: String?) {
        if (fileName != null) file(fileName).delete()
    }

    /** Apaga arquivos que nenhum exercício referencia mais. */
    fun deleteOrphans(referenced: Set<String>) {
        directory.listFiles()?.filter { it.name !in referenced }?.forEach { it.delete() }
    }

    private fun readLimited(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            total += read
            if (total > MediaValidator.MAX_BYTES) break
        }
        return out.toByteArray()
    }

    companion object {
        const val DIR = "custom_media"
    }
}
