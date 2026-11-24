package com.fitforge.treinos.domain

enum class MediaFormat(val extension: String) {
    GIF("gif"), WEBP("webp"), PNG("png"), JPEG("jpg")
}

sealed interface MediaCheck {
    data class Ok(val format: MediaFormat) : MediaCheck
    data class TooLarge(val bytes: Long) : MediaCheck
    data class TooBig(val width: Int, val height: Int) : MediaCheck
    data object UnsupportedFormat : MediaCheck
    data object Unreadable : MediaCheck
}

/**
 * Validação da imagem de um exercício personalizado.
 * O formato é identificado pelos primeiros bytes do arquivo, não pela extensão:
 * um ".gif" que na verdade é outra coisa é recusado.
 */
object MediaValidator {
    const val MAX_BYTES: Long = 2L * 1024 * 1024
    const val MAX_DIMENSION = 720
    const val HEADER_SIZE = 12

    fun detectFormat(header: ByteArray): MediaFormat? {
        fun at(i: Int) = if (i < header.size) header[i].toInt() and 0xFF else -1
        fun ascii(start: Int, text: String) = text.indices.all { at(start + it) == text[it].code }
        return when {
            ascii(0, "GIF87a") || ascii(0, "GIF89a") -> MediaFormat.GIF
            ascii(0, "RIFF") && ascii(8, "WEBP") -> MediaFormat.WEBP
            at(0) == 0x89 && ascii(1, "PNG") -> MediaFormat.PNG
            at(0) == 0xFF && at(1) == 0xD8 && at(2) == 0xFF -> MediaFormat.JPEG
            else -> null
        }
    }

    /** [width]/[height] vêm da decodificação só dos limites da imagem; <= 0 significa ilegível. */
    fun validate(sizeBytes: Long, header: ByteArray, width: Int, height: Int): MediaCheck {
        if (sizeBytes > MAX_BYTES) return MediaCheck.TooLarge(sizeBytes)
        val format = detectFormat(header) ?: return MediaCheck.UnsupportedFormat
        if (width <= 0 || height <= 0) return MediaCheck.Unreadable
        if (width > MAX_DIMENSION || height > MAX_DIMENSION) return MediaCheck.TooBig(width, height)
        return MediaCheck.Ok(format)
    }

    fun message(check: MediaCheck): String? = when (check) {
        is MediaCheck.Ok -> null
        is MediaCheck.TooLarge ->
            "Arquivo muito grande (${"%.1f".format(check.bytes / 1024.0 / 1024.0)} MB). O limite é 2 MB."
        is MediaCheck.TooBig ->
            "Imagem de ${check.width}×${check.height} px. O limite é $MAX_DIMENSION px no maior lado."
        MediaCheck.UnsupportedFormat -> "Formato não suportado. Use GIF, WebP, PNG ou JPG."
        MediaCheck.Unreadable -> "Não foi possível ler a imagem."
    }
}
