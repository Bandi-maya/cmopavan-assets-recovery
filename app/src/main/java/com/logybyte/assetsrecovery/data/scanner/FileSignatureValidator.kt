package com.logybyte.assetsrecovery.data.scanner

import com.logybyte.assetsrecovery.data.model.MediaType
import com.logybyte.assetsrecovery.data.model.RecoveryConfidence
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

data class SignatureResult(
    val isValid: Boolean,
    val detectedMimeType: String?,
    val detectedType: MediaType?,
    val confidence: RecoveryConfidence
)

object FileSignatureValidator {

    // Magic Bytes signatures
    private val JPEG_HEADER = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG_HEADER = byteArrayOf(
        0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
        0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()
    )
    private val GIF87A_HEADER = "GIF87a".toByteArray(Charsets.US_ASCII)
    private val GIF89A_HEADER = "GIF89a".toByteArray(Charsets.US_ASCII)
    private val RIFF_HEADER = "RIFF".toByteArray(Charsets.US_ASCII)
    private val WEBP_MAGIC = "WEBP".toByteArray(Charsets.US_ASCII)
    private val FTYP_MAGIC = "ftyp".toByteArray(Charsets.US_ASCII)
    private val PDF_HEADER = "%PDF".toByteArray(Charsets.US_ASCII)

    fun inspectFile(file: File): SignatureResult {
        if (!file.exists() || !file.canRead() || file.length() < 16) {
            return SignatureResult(
                isValid = false,
                detectedMimeType = null,
                detectedType = null,
                confidence = RecoveryConfidence.CORRUPTED
            )
        }

        return try {
            FileInputStream(file).use { input ->
                inspectStream(input, file.length())
            }
        } catch (e: Exception) {
            SignatureResult(
                isValid = false,
                detectedMimeType = null,
                detectedType = null,
                confidence = RecoveryConfidence.LOW
            )
        }
    }

    fun inspectStream(input: InputStream, totalLength: Long): SignatureResult {
        val buffer = ByteArray(32)
        val readBytes = input.read(buffer, 0, buffer.size)
        if (readBytes < 8) {
            return SignatureResult(
                isValid = false,
                detectedMimeType = null,
                detectedType = null,
                confidence = RecoveryConfidence.CORRUPTED
            )
        }

        // 1. JPEG: FF D8 FF
        if (matchesPrefix(buffer, JPEG_HEADER)) {
            return SignatureResult(
                isValid = true,
                detectedMimeType = "image/jpeg",
                detectedType = MediaType.IMAGE,
                confidence = RecoveryConfidence.HIGH
            )
        }

        // 2. PNG: 89 50 4E 47 0D 0A 1A 0A
        if (matchesPrefix(buffer, PNG_HEADER)) {
            return SignatureResult(
                isValid = true,
                detectedMimeType = "image/png",
                detectedType = MediaType.IMAGE,
                confidence = RecoveryConfidence.HIGH
            )
        }

        // 3. GIF: GIF87a or GIF89a
        if (matchesPrefix(buffer, GIF87A_HEADER) || matchesPrefix(buffer, GIF89A_HEADER)) {
            return SignatureResult(
                isValid = true,
                detectedMimeType = "image/gif",
                detectedType = MediaType.IMAGE,
                confidence = RecoveryConfidence.HIGH
            )
        }

        // 4. WebP: RIFF (bytes 0..3) ... WEBP (bytes 8..11)
        if (matchesPrefix(buffer, RIFF_HEADER) && readBytes >= 12 && matchesOffset(buffer, 8, WEBP_MAGIC)) {
            return SignatureResult(
                isValid = true,
                detectedMimeType = "image/webp",
                detectedType = MediaType.IMAGE,
                confidence = RecoveryConfidence.HIGH
            )
        }

        // 5. MP4 / MOV / 3GP / QuickTime: bytes 4..7 are "ftyp"
        if (readBytes >= 12 && matchesOffset(buffer, 4, FTYP_MAGIC)) {
            val majorBrand = String(buffer, 8, 4, Charsets.US_ASCII)
            val isHeic = majorBrand.startsWith("heic") || majorBrand.startsWith("mif1") || majorBrand.startsWith("msf1")
            return if (isHeic) {
                SignatureResult(
                    isValid = true,
                    detectedMimeType = "image/heic",
                    detectedType = MediaType.IMAGE,
                    confidence = RecoveryConfidence.HIGH
                )
            } else {
                SignatureResult(
                    isValid = true,
                    detectedMimeType = "video/mp4",
                    detectedType = MediaType.VIDEO,
                    confidence = RecoveryConfidence.HIGH
                )
            }
        }

        // 6. PDF document signature
        if (matchesPrefix(buffer, PDF_HEADER)) {
            return SignatureResult(
                isValid = true,
                detectedMimeType = "application/pdf",
                detectedType = null,
                confidence = RecoveryConfidence.MEDIUM
            )
        }

        // If file is non-empty but header doesn't match standard known magic bytes
        return SignatureResult(
            isValid = totalLength > 1024,
            detectedMimeType = null,
            detectedType = null,
            confidence = if (totalLength > 1024) RecoveryConfidence.MEDIUM else RecoveryConfidence.LOW
        )
    }

    private fun matchesPrefix(data: ByteArray, pattern: ByteArray): Boolean {
        if (data.size < pattern.size) return false
        for (i in pattern.indices) {
            if (data[i] != pattern[i]) return false
        }
        return true
    }

    private fun matchesOffset(data: ByteArray, offset: Int, pattern: ByteArray): Boolean {
        if (data.size < offset + pattern.size) return false
        for (i in pattern.indices) {
            if (data[offset + i] != pattern[i]) return false
        }
        return true
    }
}
