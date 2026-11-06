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
<<<<<<< HEAD
import com.logybyte.assetsrecovery.data.model.RecoveryConfidence
=======
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
import com.logybyte.assetsrecovery.data.model.RecoveryResult
import com.logybyte.assetsrecovery.data.model.RecoveryStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
<<<<<<< HEAD
import java.security.MessageDigest
=======
import java.io.OutputStream
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53

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
<<<<<<< HEAD
                // If it's a MediaStore trashed item, first try direct untrash restoration if permitted
                var restoredUri: Uri? = null
                if (item.isTrashed && item.uri.scheme == "content" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val unTrashValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_TRASHED, 0)
                        }
                        val rows = context.contentResolver.update(item.uri, unTrashValues, null, null)
                        if (rows > 0) {
                            restoredUri = item.uri
                        }
                    } catch (e: Exception) {
                        // Fall back to copy recovery
                        restoredUri = null
                    }
                }

                // If untrash didn't run or wasn't applicable, copy to RecoveredMedia directory
                if (restoredUri == null) {
                    restoredUri = recoverSingleItemByCopy(item)
                }

                if (restoredUri != null && verifyRecoveredFile(restoredUri, item.size)) {
=======
                val recoveredUri = recoverSingleItem(item)
                if (recoveredUri != null && verifyRecoveredFile(recoveredUri, item.size)) {
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
                    succeeded++
                    updatedItems.add(
                        item.copy(
                            recoveryStatus = RecoveryStatus.RECOVERED,
<<<<<<< HEAD
                            recoveryPath = restoredUri.toString(),
                            integrityConfidence = RecoveryConfidence.HIGH
=======
                            recoveryPath = recoveredUri.toString()
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
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

<<<<<<< HEAD
    private fun recoverSingleItemByCopy(item: MediaItem): Uri? {
=======
    private fun recoverSingleItem(item: MediaItem): Uri? {
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
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
<<<<<<< HEAD
            // Android 10+ Scoped Storage MediaStore insertion
=======
            // Android 10+ (API 29+) Scoped Storage MediaStore insertion
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
                put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val targetUri = resolver.insert(collection, values) ?: return null

            var copySuccess = false
<<<<<<< HEAD
            var sourceChecksum: String? = null
            var destChecksum: String? = null

            try {
                openSourceInputStream(item)?.use { input ->
                    resolver.openOutputStream(targetUri)?.use { output ->
                        val digestIn = MessageDigest.getInstance("SHA-256")
                        val digestOut = MessageDigest.getInstance("SHA-256")

                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            digestIn.update(buffer, 0, bytesRead)
                            output.write(buffer, 0, bytesRead)
                            digestOut.update(buffer, 0, bytesRead)
                        }
                        output.flush()

                        sourceChecksum = digestIn.digest().joinToString("") { "%02x".format(it) }
                        destChecksum = digestOut.digest().joinToString("") { "%02x".format(it) }
                        copySuccess = (sourceChecksum == destChecksum)
=======
            try {
                openSourceInputStream(item)?.use { input ->
                    resolver.openOutputStream(targetUri)?.use { output ->
                        input.copyTo(output)
                        copySuccess = true
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
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
<<<<<<< HEAD
                    val digestIn = MessageDigest.getInstance("SHA-256")
                    val digestOut = MessageDigest.getInstance("SHA-256")

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        digestIn.update(buffer, 0, bytesRead)
                        output.write(buffer, 0, bytesRead)
                        digestOut.update(buffer, 0, bytesRead)
                    }
                    output.flush()
                    copySuccess = (digestIn.digest().contentEquals(digestOut.digest()))
=======
                    input.copyTo(output)
                    copySuccess = true
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
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
<<<<<<< HEAD
        val timestamp = System.currentTimeMillis() % 100000
=======
        val timestamp = System.currentTimeMillis() % 10000
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
        return "${prefix}_recovered_${timestamp}$ext"
    }
}
