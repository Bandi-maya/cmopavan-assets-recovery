package com.logybyte.assetsrecovery.data.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.provider.MediaStore
import com.logybyte.assetsrecovery.data.model.MediaItem
import com.logybyte.assetsrecovery.data.model.MediaStorageType
import com.logybyte.assetsrecovery.data.model.MediaType
import com.logybyte.assetsrecovery.data.model.ScanProgress
import com.logybyte.assetsrecovery.data.model.ScanResult
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
                emit(ScanProgress(totalCount, item.path, item))
            }
        }

        // Final completion event
        emit(ScanProgress(totalCount, "Scan Completed", null))
    }.flowOn(Dispatchers.IO)

    private suspend fun queryMediaStoreImages(
        context: Context,
        onFound: (String, MediaItem) -> Unit
    ): List<MediaItem> {
        val list = mutableListOf<MediaItem>()
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
                        isHidden = isHidden
                    )
                    list.add(item)
                    onFound(path, item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private suspend fun queryMediaStoreVideos(
        context: Context,
        onFound: (String, MediaItem) -> Unit
    ): List<MediaItem> {
        val list = mutableListOf<MediaItem>()
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
                        durationMs = duration
                    )
                    list.add(item)
                    onFound(path, item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                // Don't recurse into common non-media heavy folders
                if (!file.name.startsWith(".") || file.name.equals(".statuses", ignoreCase = true) || file.name.equals(".trashed", ignoreCase = true)) {
                    scanDirectory(file, depth + 1, seenPaths, onItemDiscovered)
                }
            } else if (file.isFile && file.length() > 1024) { // Ignore 0-byte or tiny corrupt stubs
                val absPath = file.absolutePath
                if (seenPaths.contains(absPath.lowercase())) {
                    continue
                }

                val ext = file.extension.lowercase()
                val isImage = supportedImageExtensions.contains(ext)
                val isVideo = supportedVideoExtensions.contains(ext)

                if (isImage || isVideo) {
                    val isHidden = hasNoMedia || file.name.startsWith(".") || absPath.contains("/.")
                    val mediaType = if (isImage) MediaType.IMAGE else MediaType.VIDEO
                    val mimeType = if (isImage) "image/$ext" else "video/$ext"

                    val item = MediaItem(
                        id = System.currentTimeMillis() + file.hashCode(),
                        uri = Uri.fromFile(file),
                        displayName = file.name,
                        path = absPath,
                        size = file.length(),
                        dateModified = file.lastModified() / 1000L,
                        mimeType = mimeType,
                        mediaType = mediaType,
                        storageType = if (isHidden) MediaStorageType.HIDDEN_ORPHANED else MediaStorageType.ACTIVE_GALLERY,
                        isTrashed = false,
                        isHidden = isHidden
                    )
                    onItemDiscovered(item)
                }
            }
        }
    }
}
