package com.logybyte.assetsrecovery.data.model

data class ScanProgress(
    val scannedCount: Int,
    val currentPath: String,
    val lastDiscoveredItem: MediaItem? = null
)

data class ScanResult(
    val imagesFound: Int,
    val videosFound: Int,
    val recoverableCount: Int,
    val trashedCount: Int,
    val hiddenCount: Int,
    val activeCount: Int,
    val totalScanned: Int,
    val durationMs: Long,
    val items: List<MediaItem>
)

data class RecoveryResult(
    val totalAttempted: Int,
    val succeededCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
    val recoveredItems: List<MediaItem>,
    val outputDirectoryName: String = "Pictures/RecoveredMedia"
)
