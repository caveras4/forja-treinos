package com.fitforge.treinos.data.files

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

sealed interface ReadResult {
    /** [size] é o tamanho real lido; o texto só vem quando o arquivo cabe no limite. */
    data class Ok(val text: String, val size: Long) : ReadResult
    data class TooLarge(val size: Long) : ReadResult
    data object Failed : ReadResult
}

/** Lê e grava arquivos de texto escolhidos pelo seletor do sistema (Storage Access Framework). */
class TextFiles(private val contentResolver: ContentResolver) {

    suspend fun read(uri: Uri, maxBytes: Long): ReadResult = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    // Para de ler logo depois do limite: não carrega arquivo gigante na memória.
                    if (total > maxBytes) return@use ReadResult.TooLarge(total)
                    out.write(buffer, 0, read)
                }
                ReadResult.Ok(out.toString(Charsets.UTF_8.name()), total)
            } ?: ReadResult.Failed
        } catch (e: Exception) {
            ReadResult.Failed
        }
    }

    suspend fun write(uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
        } catch (e: Exception) {
            false
        }
    }
}
