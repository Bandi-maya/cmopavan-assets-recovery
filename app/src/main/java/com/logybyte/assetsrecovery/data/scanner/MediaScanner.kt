package com.logybyte.assetsrecovery.data.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
<<<<<<< HEAD
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.logybyte.assetsrecovery.data.model.MediaItem
import com.logybyte.assetsrecovery.data.model.MediaStorageType
import com.logybyte.assetsrecovery.data.model.MediaType
import com.logybyte.assetsrecovery.data.model.RecoveryConfidence
import com.logybyte.assetsrecovery.data.model.ScanProgress
=======
import android.os.CancellationSignal
import android.os.Environment
import android.provider.MediaStore
import com.logybyte.assetsrecovery.data.model.MediaItem
import com.logybyte.assetsrecovery.data.model.MediaStorageType
import com.logybyte.assetsrecovery.data.model.MediaType
import com.logybyte.assetsrecovery.data.model.ScanProgress
import com.logybyte.assetsrecovery.data.model.ScanResult
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.coroutines.coroutineContext

class MediaScanner(private val context: Context) {

    private val supportedImageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
    private val supportedVideoExtensions = setOf("mp4", "mkv", "mov", "3gp", "webm", "avi", "ts")

    fun scanMedia(): Flow<ScanProgress> = flow {
        var totalCount = 0
<<<<<<< HEAD
        val seenPaths = HashSet<String>()

        // 1. Scan MediaStore Images (Active and Trashed)
        queryMediaStoreImages(context) { path, item ->
            totalCount++
            if (path.isNotEmpty()) {
                seenPaths.add(normalizePath(path))
            }
            emit(ScanProgress(totalCount, item.path.ifEmpty { item.displayName }, item))
        }

        // 2. Scan MediaStore Videos (Active and Trashed)
        queryMediaStoreVideos(context) { path, item ->
            totalCount++
            if (path.isNotEmpty()) {
                seenPaths.add(normalizePath(path))
            }
            emit(ScanProgress(totalCount, item.path.ifEmpty { item.displayName }, item))
        }

        // 3. Scan Accessible File System Volumes Recursively
        val storageRoots = getAccessibleStorageRoots()
        for (volume in storageRoots) {
            if (!coroutineContext.isActive) break

            emit(ScanProgress(totalCount, "Scanning volume: ${volume.name}", null))
            scanDirectoryRecursively(
                directory = volume,
                depth = 0,
                seenPaths = seenPaths,
                volumeName = volume.name
            ) { item ->
                totalCount++
                seenPaths.add(normalizePath(item.path))
=======
        val items = mutableListOf<MediaItem>()
        val seenPaths = HashSet<String>()

        // 1. Scan MediaStore Images (Active and Trashed)
        val images = queryMediaStoreImages(context) { path, item ->
            totalCount++
            items.add(item)
            seenPaths.add(path.lowercase())
        }
        for (item in images) {
            emit(ScanProgress(totalCount, item.path, item))
        }

        // 2. Scan MediaStore Videos (Active and Trashed)
        val videos = queryMediaStoreVideos(context) { path, item ->
            totalCount++
            items.add(item)
            seenPaths.add(path.lowercase())
        }
        for (item in videos) {
            emit(ScanProgress(totalCount, item.path, item))
        }

        // 3. Scan Accessible File System for Hidden / Orphaned Media
        val storageRoot = Environment.getExternalStorageDirectory()
        if (storageRoot != null && storageRoot.exists() && storageRoot.canRead()) {
            scanDirectory(storageRoot, depth = 0, seenPaths = seenPaths) { item ->
                totalCount++
                items.add(item)
                seenPaths.add(item.path.lowercase())
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
                emit(ScanProgress(totalCount, item.path, item))
            }
        }

        // Final completion event
        emit(ScanProgress(totalCount, "Scan Completed", null))
    }.flowOn(Dispatchers.IO)

<<<<<<< HEAD
    private fun getAccessibleStorageRoots(): List<File> {
        val roots = mutableListOf<File>()

        // Primary internal shared storage
        val primaryStorage = Environment.getExternalStorageDirectory()
        if (primaryStorage != null && primaryStorage.exists() && primaryStorage.canRead()) {
            roots.add(primaryStorage)
        }

        // Discover secondary volumes (e.g. SD cards)
        try {
            val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
            for (dir in externalDirs) {
                if (dir != null) {
                    val root = findStorageVolumeRoot(dir)
                    if (root != null && root.exists() && root.canRead() && !roots.any { it.absolutePath == root.absolutePath }) {
                        roots.add(root)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore volume discovery failures
        }

        return roots
    }

    private fun findStorageVolumeRoot(file: File): File? {
        var current: File? = file
        while (current != null) {
            val parent = current.parentFile
            if (parent != null && parent.absolutePath == "/storage") {
                return current
            }
            current = parent
        }
        return null
    }

    private suspend fun queryMediaStoreImages(
        context: Context,
        onFound: suspend (String, MediaItem) -> Unit
    ) {
=======
    private suspend fun queryMediaStoreImages(
        context: Context,
        onFound: (String, MediaItem) -> Unit
    ): List<MediaItem> {
        val list = mutableListOf<MediaItem>()
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projection.add(MediaStore.MediaColumns.IS_TRASHED)
        }

        val contentResolver = context.contentResolver
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        try {
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bundle = Bundle().apply {
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                    putString(
                        android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                        "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
                    )
                }
                contentResolver.query(uri, projection.toTypedArray(), bundle, null)
            } else {
                contentResolver.query(
                    uri,
                    projection.toTypedArray(),
                    null,
                    null,
                    "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
                )
            }

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = c.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val dataCol = c.getColumnIndex(MediaStore.Images.Media.DATA)
                val sizeCol = c.getColumnIndex(MediaStore.Images.Media.SIZE)
                val dateCol = c.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                val mimeCol = c.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val trashedCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    c.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
                } else -1

                while (c.moveToNext() && coroutineContext.isActive) {
                    val id = c.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(uri, id)
                    val name = if (nameCol >= 0) c.getString(nameCol) ?: "Image_$id" else "Image_$id"
                    val path = if (dataCol >= 0) c.getString(dataCol) ?: "" else ""
                    val size = if (sizeCol >= 0) c.getLong(sizeCol) else 0L
                    val dateModified = if (dateCol >= 0) c.getLong(dateCol) else 0L
                    val mimeType = if (mimeCol >= 0) c.getString(mimeCol) ?: "image/jpeg" else "image/jpeg"
                    val isTrashed = if (trashedCol >= 0) c.getInt(trashedCol) == 1 else false
                    val isHidden = path.contains("/.") || name.startsWith(".")

                    val storageType = when {
                        isTrashed -> MediaStorageType.MEDIA_TRASH
                        isHidden -> MediaStorageType.HIDDEN_ORPHANED
                        else -> MediaStorageType.ACTIVE_GALLERY
                    }

                    val item = MediaItem(
                        id = id,
                        uri = contentUri,
                        displayName = name,
                        path = path,
                        size = size,
                        dateModified = dateModified,
                        mimeType = mimeType,
                        mediaType = MediaType.IMAGE,
                        storageType = storageType,
                        isTrashed = isTrashed,
<<<<<<< HEAD
                        isHidden = isHidden,
                        integrityConfidence = RecoveryConfidence.HIGH,
                        sourceVolume = "Internal MediaStore"
                    )
=======
                        isHidden = isHidden
                    )
                    list.add(item)
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
                    onFound(path, item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
<<<<<<< HEAD
=======
        return list
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
    }

    private suspend fun queryMediaStoreVideos(
        context: Context,
<<<<<<< HEAD
        onFound: suspend (String, MediaItem) -> Unit
    ) {
=======
        onFound: (String, MediaItem) -> Unit
    ): List<MediaItem> {
        val list = mutableListOf<MediaItem>()
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projection.add(MediaStore.MediaColumns.IS_TRASHED)
        }

        val contentResolver = context.contentResolver
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        try {
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bundle = Bundle().apply {
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                    putString(
                        android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                        "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
                    )
                }
                contentResolver.query(uri, projection.toTypedArray(), bundle, null)
            } else {
                contentResolver.query(
                    uri,
                    projection.toTypedArray(),
                    null,
                    null,
                    "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
                )
            }

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = c.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val dataCol = c.getColumnIndex(MediaStore.Video.Media.DATA)
                val sizeCol = c.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateCol = c.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val mimeCol = c.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val durationCol = c.getColumnIndex(MediaStore.Video.Media.DURATION)
                val trashedCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    c.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
                } else -1

                while (c.moveToNext() && coroutineContext.isActive) {
                    val id = c.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(uri, id)
                    val name = if (nameCol >= 0) c.getString(nameCol) ?: "Video_$id" else "Video_$id"
                    val path = if (dataCol >= 0) c.getString(dataCol) ?: "" else ""
                    val size = if (sizeCol >= 0) c.getLong(sizeCol) else 0L
                    val dateModified = if (dateCol >= 0) c.getLong(dateCol) else 0L
                    val mimeType = if (mimeCol >= 0) c.getString(mimeCol) ?: "video/mp4" else "video/mp4"
                    val duration = if (durationCol >= 0) c.getLong(durationCol) else 0L
                    val isTrashed = if (trashedCol >= 0) c.getInt(trashedCol) == 1 else false
                    val isHidden = path.contains("/.") || name.startsWith(".")

                    val storageType = when {
                        isTrashed -> MediaStorageType.MEDIA_TRASH
                        isHidden -> MediaStorageType.HIDDEN_ORPHANED
                        else -> MediaStorageType.ACTIVE_GALLERY
                    }

                    val item = MediaItem(
                        id = id,
                        uri = contentUri,
                        displayName = name,
                        path = path,
                        size = size,
                        dateModified = dateModified,
                        mimeType = mimeType,
                        mediaType = MediaType.VIDEO,
                        storageType = storageType,
                        isTrashed = isTrashed,
                        isHidden = isHidden,
<<<<<<< HEAD
                        durationMs = duration,
                        integrityConfidence = RecoveryConfidence.HIGH,
                        sourceVolume = "Internal MediaStore"
                    )
=======
                        durationMs = duration
                    )
                    list.add(item)
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
                    onFound(path, item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
<<<<<<< HEAD
    }

    private suspend fun scanDirectoryRecursively(
        directory: File,
        depth: Int,
        seenPaths: Set<String>,
        volumeName: String,
        onItemDiscovered: suspend (MediaItem) -> Unit
    ) {
        if (depth > 8 || !coroutineContext.isActive) return
        val nameLower = directory.name.lowercase()

        // Skip restricted / inaccessible directories on Android 11+ to avoid security exceptions
        if (nameLower == "android" && depth <= 1) {
            // Check Android/media (accessible on Android 11+ for WhatsApp/Telegram), skip data and obb
            val mediaDir = File(directory, "media")
            if (mediaDir.exists() && mediaDir.canRead()) {
                scanDirectoryRecursively(mediaDir, depth + 1, seenPaths, volumeName, onItemDiscovered)
=======
        return list
    }

    private suspend fun scanDirectory(
        directory: File,
        depth: Int,
        seenPaths: Set<String>,
        onItemDiscovered: suspend (MediaItem) -> Unit
    ) {
        if (depth > 6 || !coroutineContext.isActive) return
        val nameLower = directory.name.lowercase()

        // Skip restricted or inaccessible directories
        if (nameLower == "android" && depth == 1) {
            // Check Android/media, but skip Android/data and Android/obb
            val mediaDir = File(directory, "media")
            if (mediaDir.exists() && mediaDir.canRead()) {
                scanDirectory(mediaDir, depth + 1, seenPaths, onItemDiscovered)
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
            }
            return
        }

        val files = try {
            directory.listFiles()
        } catch (e: SecurityException) {
            null
        } ?: return

        val hasNoMedia = files.any { it.name.equals(".nomedia", ignoreCase = true) }

        for (file in files) {
            if (!coroutineContext.isActive) return

            if (file.isDirectory) {
<<<<<<< HEAD
                val dirName = file.name.lowercase()
                // Do not skip recycle bins or hidden thumbnail folders:
                val isTrashDir = dirName.contains("trash") || dirName.contains("recycle") || dirName == ".lost.dir"
                val isAllowedHiddenDir = dirName.startsWith(".") && (
                        dirName.contains("thumb") ||
                        dirName.contains("status") ||
                        dirName.contains("trash") ||
                        dirName.contains("cache")
                )

                if (!dirName.startsWith(".") || isAllowedHiddenDir || isTrashDir) {
                    scanDirectoryRecursively(file, depth + 1, seenPaths, volumeName, onItemDiscovered)
                }
            } else if (file.isFile && file.length() > 512) { // Filter out 0-byte stubs
                val absPath = file.absolutePath
                val normalizedPath = normalizePath(absPath)
                if (seenPaths.contains(normalizedPath)) {
=======
                // Don't recurse into common non-media heavy folders
                if (!file.name.startsWith(".") || file.name.equals(".statuses", ignoreCase = true) || file.name.equals(".trashed", ignoreCase = true)) {
                    scanDirectory(file, depth + 1, seenPaths, onItemDiscovered)
                }
            } else if (file.isFile && file.length() > 1024) { // Ignore 0-byte or tiny corrupt stubs
                val absPath = file.absolutePath
                if (seenPaths.contains(absPath.lowercase())) {
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
                    continue
                }

                val ext = file.extension.lowercase()
                val isImage = supportedImageExtensions.contains(ext)
                val isVideo = supportedVideoExtensions.contains(ext)

                if (isImage || isVideo) {
<<<<<<< HEAD
                    // Inspect file signature and headers
                    val sigResult = FileSignatureValidator.inspectFile(file)
                    val confidence = sigResult.confidence

                    val isTrashPath = absPath.contains("/.trash", ignoreCase = true) ||
                            absPath.contains("/.trashed", ignoreCase = true) ||
                            absPath.contains("/recycle", ignoreCase = true)

                    val isCache = absPath.contains("/cache", ignoreCase = true) ||
                            absPath.contains("/.thumbnails", ignoreCase = true)

                    val isHidden = hasNoMedia || file.name.startsWith(".") || absPath.contains("/.") || isTrashPath

                    val mediaType = if (isImage) MediaType.IMAGE else MediaType.VIDEO
                    val mimeType = sigResult.detectedMimeType ?: if (isImage) "image/$ext" else "video/$ext"

                    val storageType = when {
                        isTrashPath -> MediaStorageType.MEDIA_TRASH
                        isCache -> MediaStorageType.APP_CACHE
                        isHidden -> MediaStorageType.HIDDEN_ORPHANED
                        else -> MediaStorageType.ACTIVE_GALLERY
                    }
=======
                    val isHidden = hasNoMedia || file.name.startsWith(".") || absPath.contains("/.")
                    val mediaType = if (isImage) MediaType.IMAGE else MediaType.VIDEO
                    val mimeType = if (isImage) "image/$ext" else "video/$ext"
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53

                    val item = MediaItem(
                        id = System.currentTimeMillis() + file.hashCode(),
                        uri = Uri.fromFile(file),
                        displayName = file.name,
                        path = absPath,
                        size = file.length(),
                        dateModified = file.lastModified() / 1000L,
                        mimeType = mimeType,
                        mediaType = mediaType,
<<<<<<< HEAD
                        storageType = storageType,
                        isTrashed = isTrashPath,
                        isHidden = isHidden,
                        integrityConfidence = confidence,
                        sourceVolume = volumeName
=======
                        storageType = if (isHidden) MediaStorageType.HIDDEN_ORPHANED else MediaStorageType.ACTIVE_GALLERY,
                        isTrashed = false,
                        isHidden = isHidden
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
                    )
                    onItemDiscovered(item)
                }
            }
        }
    }
<<<<<<< HEAD

    private fun normalizePath(path: String): String {
        return path.trim().lowercase()
    }
=======
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
}
