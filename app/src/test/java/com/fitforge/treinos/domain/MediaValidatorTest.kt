package com.fitforge.treinos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaValidatorTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
    private fun ascii(text: String) = text.toByteArray(Charsets.US_ASCII)

    private val gif = ascii("GIF89a") + ByteArray(6)
    private val webp = ascii("RIFF") + bytes(0, 0, 0, 0) + ascii("WEBP")
    private val png = bytes(0x89) + ascii("PNG") + ByteArray(8)
    private val jpeg = bytes(0xFF, 0xD8, 0xFF, 0xE0) + ByteArray(8)

    @Test
    fun `detecta formato pelos primeiros bytes`() {
        assertEquals(MediaFormat.GIF, MediaValidator.detectFormat(gif))
        assertEquals(MediaFormat.GIF, MediaValidator.detectFormat(ascii("GIF87a")))
        assertEquals(MediaFormat.WEBP, MediaValidator.detectFormat(webp))
        assertEquals(MediaFormat.PNG, MediaValidator.detectFormat(png))
        assertEquals(MediaFormat.JPEG, MediaValidator.detectFormat(jpeg))
    }

    @Test
    fun `arquivo com extensao de imagem mas conteudo de texto e recusado`() {
        val fake = ascii("<html>not a gif</html>")
        assertNull(MediaValidator.detectFormat(fake))
        assertEquals(MediaCheck.UnsupportedFormat, MediaValidator.validate(fake.size.toLong(), fake, 100, 100))
        assertEquals(MediaCheck.UnsupportedFormat, MediaValidator.validate(0, ByteArray(0), 0, 0))
    }

    @Test
    fun `limite de 2 MB`() {
        val limit = MediaValidator.MAX_BYTES
        assertEquals(MediaCheck.Ok(MediaFormat.GIF), MediaValidator.validate(limit, gif, 400, 300))
        val tooLarge = MediaValidator.validate(limit + 1, gif, 400, 300)
        assertTrue(tooLarge is MediaCheck.TooLarge)
        assertNotNull(MediaValidator.message(tooLarge))
    }

    @Test
    fun `limite de 720 px no maior lado`() {
        assertEquals(MediaCheck.Ok(MediaFormat.PNG), MediaValidator.validate(1000, png, 720, 720))
        assertEquals(MediaCheck.TooBig(721, 300), MediaValidator.validate(1000, png, 721, 300))
        assertEquals(MediaCheck.TooBig(300, 1080), MediaValidator.validate(1000, png, 300, 1080))
    }

    @Test
    fun `imagem que nao decodifica e recusada`() {
        assertEquals(MediaCheck.Unreadable, MediaValidator.validate(1000, jpeg, -1, -1))
    }
}
