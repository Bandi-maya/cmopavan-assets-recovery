package com.logybyte.assetsrecovery.data.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val path: String,
    val size: Long,
    val dateModified: Long,
    val mimeType: String,
    val mediaType: MediaType,
    val storageType: MediaStorageType,
    val isTrashed: Boolean = false,
    val isHidden: Boolean = false,
    val durationMs: Long = 0L,
    val recoveryStatus: RecoveryStatus = RecoveryStatus.NONE,
    val recoveryPath: String? = null,
    val isSelected: Boolean = false
) {
    val isRecoverable: Boolean
        get() = isTrashed || isHidden || storageType == MediaStorageType.APP_CACHE
}
