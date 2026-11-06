package com.logybyte.assetsrecovery

import com.logybyte.assetsrecovery.data.model.MediaType
import com.logybyte.assetsrecovery.data.model.RecoveryConfidence
import com.logybyte.assetsrecovery.data.scanner.FileSignatureValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class FileSignatureValidatorTest {

    @Test
    fun testInspectStream_jpegSignature() {
        val jpegHeader = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        )
        val stream = ByteArrayInputStream(jpegHeader)
        val result = FileSignatureValidator.inspectStream(stream, jpegHeader.size.toLong())

        assertTrue(result.isValid)
        assertEquals("image/jpeg", result.detectedMimeType)
        assertEquals(MediaType.IMAGE, result.detectedType)
        assertEquals(RecoveryConfidence.HIGH, result.confidence)
    }

    @Test
    fun testInspectStream_pngSignature() {
        val pngHeader = byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
            0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        )
        val stream = ByteArrayInputStream(pngHeader)
        val result = FileSignatureValidator.inspectStream(stream, pngHeader.size.toLong())

        assertTrue(result.isValid)
        assertEquals("image/png", result.detectedMimeType)
        assertEquals(MediaType.IMAGE, result.detectedType)
        assertEquals(RecoveryConfidence.HIGH, result.confidence)
    }

    @Test
    fun testInspectStream_mp4Signature() {
        // MP4 format: [4 bytes length][ftyp][major brand]
        val mp4Header = ByteArray(16)
        mp4Header[0] = 0x00
        mp4Header[1] = 0x00
        mp4Header[2] = 0x00
        mp4Header[3] = 0x18
        System.arraycopy("ftypmp42".toByteArray(Charsets.US_ASCII), 0, mp4Header, 4, 8)

        val stream = ByteArrayInputStream(mp4Header)
        val result = FileSignatureValidator.inspectStream(stream, 1024L)

        assertTrue(result.isValid)
        assertEquals("video/mp4", result.detectedMimeType)
        assertEquals(MediaType.VIDEO, result.detectedType)
        assertEquals(RecoveryConfidence.HIGH, result.confidence)
    }

    @Test
    fun testInspectStream_emptyOrTooShort() {
        val emptyStream = ByteArrayInputStream(ByteArray(4))
        val result = FileSignatureValidator.inspectStream(emptyStream, 4L)

        assertFalse(result.isValid)
        assertEquals(RecoveryConfidence.CORRUPTED, result.confidence)
    }
}
