package com.logybyte.assetsrecovery.data.recovery

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.logybyte.assetsrecovery.data.model.MediaItem
import com.logybyte.assetsrecovery.data.model.MediaType
import com.logybyte.assetsrecovery.data.model.RecoveryResult
import com.logybyte.assetsrecovery.data.model.RecoveryStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class RecoveryEngine(private val context: Context) {

    private val recoverySubDir = "RecoveredMedia"

    suspend fun recoverItems(
        items: List<MediaItem>,
        onProgress: (current: Int, total: Int, item: MediaItem) -> Unit
    ): RecoveryResult = withContext(Dispatchers.IO) {
        var succeeded = 0
        var failed = 0
        var skipped = 0
        val updatedItems = mutableListOf<MediaItem>()

        items.forEachIndexed { index, item ->
            onProgress(index + 1, items.size, item)

            if (item.recoveryStatus == RecoveryStatus.RECOVERED) {
                skipped++
                updatedItems.add(item)
                return@forEachIndexed
            }

            try {
                val recoveredUri = recoverSingleItem(item)
                if (recoveredUri != null && verifyRecoveredFile(recoveredUri, item.size)) {
                    succeeded++
                    updatedItems.add(
                        item.copy(
                            recoveryStatus = RecoveryStatus.RECOVERED,
                            recoveryPath = recoveredUri.toString()
                        )
                    )
                } else {
                    failed++
                    updatedItems.add(item.copy(recoveryStatus = RecoveryStatus.FAILED))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                failed++
                updatedItems.add(item.copy(recoveryStatus = RecoveryStatus.FAILED))
            }
        }

        RecoveryResult(
            totalAttempted = items.size,
            succeededCount = succeeded,
            failedCount = failed,
            skippedCount = skipped,
            recoveredItems = updatedItems,
            outputDirectoryName = "Pictures/$recoverySubDir"
        )
    }

    private fun recoverSingleItem(item: MediaItem): Uri? {
        val resolver = context.contentResolver
        val isImage = item.mediaType == MediaType.IMAGE
        val collection = if (isImage) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val relativeDir = if (isImage) {
            "${Environment.DIRECTORY_PICTURES}/$recoverySubDir"
        } else {
            "${Environment.DIRECTORY_MOVIES}/$recoverySubDir"
        }

        val cleanName = getNonCollidingName(item.displayName)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (API 29+) Scoped Storage MediaStore insertion
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
                put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val targetUri = resolver.insert(collection, values) ?: return null

            var copySuccess = false
            try {
                openSourceInputStream(item)?.use { input ->
                    resolver.openOutputStream(targetUri)?.use { output ->
                        input.copyTo(output)
                        copySuccess = true
                    }
                }
            } finally {
                // Clear pending flag
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(targetUri, values, null, null)
            }

            if (copySuccess) targetUri else null
        } else {
            // Android 9 and lower direct file writing + MediaScanner indexing
            val parentDir = if (isImage) {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), recoverySubDir)
            } else {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), recoverySubDir)
            }

            if (!parentDir.exists()) {
                parentDir.mkdirs()
            }

            val targetFile = File(parentDir, cleanName)
            var copySuccess = false

            openSourceInputStream(item)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                    copySuccess = true
                }
            }

            if (copySuccess && targetFile.exists() && targetFile.length() > 0) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(item.mimeType),
                    null
                )
                Uri.fromFile(targetFile)
            } else {
                null
            }
        }
    }

    private fun openSourceInputStream(item: MediaItem): InputStream? {
        val resolver = context.contentResolver
        return try {
            if (item.uri.scheme == "content") {
                resolver.openInputStream(item.uri)
            } else if (item.uri.scheme == "file" || item.path.isNotEmpty()) {
                val file = if (item.path.isNotEmpty()) File(item.path) else File(item.uri.path ?: "")
                if (file.exists() && file.canRead()) {
                    FileInputStream(file)
                } else {
                    resolver.openInputStream(item.uri)
                }
            } else {
                resolver.openInputStream(item.uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun verifyRecoveredFile(uri: Uri, originalSize: Long): Boolean {
        return try {
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                file.exists() && file.length() > 0 && (originalSize <= 0 || file.length() > 0)
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val available = stream.available()
                    available > 0 || stream.read() != -1
                } ?: false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getNonCollidingName(originalName: String): String {
        val dotIndex = originalName.lastIndexOf('.')
        val prefix = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
        val ext = if (dotIndex > 0) originalName.substring(dotIndex) else ""
        val timestamp = System.currentTimeMillis() % 10000
        return "${prefix}_recovered_${timestamp}$ext"
    }
}
