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
<<<<<<< HEAD
    val isSelected: Boolean = false,
    val integrityConfidence: RecoveryConfidence = RecoveryConfidence.HIGH,
    val sourceVolume: String = "Internal Storage"
=======
    val isSelected: Boolean = false
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
) {
    val isRecoverable: Boolean
        get() = isTrashed || isHidden || storageType == MediaStorageType.APP_CACHE
}
