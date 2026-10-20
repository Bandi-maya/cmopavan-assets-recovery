package com.logybyte.assetsrecovery

import android.net.Uri
import com.logybyte.assetsrecovery.data.model.FilterType
import com.logybyte.assetsrecovery.data.model.MediaItem
import com.logybyte.assetsrecovery.data.model.MediaStorageType
import com.logybyte.assetsrecovery.data.model.MediaType
import com.logybyte.assetsrecovery.data.model.RecoveryStatus
import com.logybyte.assetsrecovery.data.model.ScanResult
import com.logybyte.assetsrecovery.data.model.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class RecoveryLogicTest {

    private val dummyUri: Uri = mock(Uri::class.java)

    private fun createItem(
        id: Long,
        name: String,
        size: Long,
        date: Long,
        type: MediaType = MediaType.IMAGE,
        storage: MediaStorageType = MediaStorageType.ACTIVE_GALLERY,
        isTrashed: Boolean = false,
        isHidden: Boolean = false,
        status: RecoveryStatus = RecoveryStatus.NONE
    ): MediaItem {
        return MediaItem(
            id = id,
            uri = dummyUri,
            displayName = name,
            path = "/storage/emulated/0/$name",
            size = size,
            dateModified = date,
            mimeType = if (type == MediaType.IMAGE) "image/jpeg" else "video/mp4",
            mediaType = type,
            storageType = storage,
            isTrashed = isTrashed,
            isHidden = isHidden,
            recoveryStatus = status
        )
    }

    @Test
    fun testSortingByDateDesc() {
        val item1 = createItem(1, "A.jpg", 100, 1000)
        val item2 = createItem(2, "B.jpg", 200, 3000)
        val item3 = createItem(3, "C.jpg", 300, 2000)

        val list = listOf(item1, item2, item3)
        val sorted = list.sortedByDescending { it.dateModified }

        assertEquals(listOf(item2, item3, item1), sorted)
    }

    @Test
    fun testSortingBySizeDesc() {
        val item1 = createItem(1, "A.jpg", 100, 1000)
        val item2 = createItem(2, "B.jpg", 500, 3000)
        val item3 = createItem(3, "C.jpg", 250, 2000)

        val list = listOf(item1, item2, item3)
        val sorted = list.sortedByDescending { it.size }

        assertEquals(listOf(item2, item3, item1), sorted)
    }

    @Test
    fun testSortingByNameAsc() {
        val item1 = createItem(1, "zebra.jpg", 100, 1000)
        val item2 = createItem(2, "Apple.jpg", 200, 3000)
        val item3 = createItem(3, "banana.jpg", 300, 2000)

        val list = listOf(item1, item2, item3)
        val sorted = list.sortedBy { it.displayName.lowercase() }

        assertEquals(listOf(item2, item3, item1), sorted)
    }

    @Test
    fun testFilteringRecoverableOnly() {
        val item1 = createItem(1, "normal.jpg", 100, 1000, storage = MediaStorageType.ACTIVE_GALLERY)
        val item2 = createItem(2, "trashed.jpg", 200, 2000, storage = MediaStorageType.MEDIA_TRASH, isTrashed = true)
        val item3 = createItem(3, "hidden.jpg", 300, 3000, storage = MediaStorageType.HIDDEN_ORPHANED, isHidden = true)

        val list = listOf(item1, item2, item3)
        val filtered = list.filter { it.isRecoverable }

        assertEquals(2, filtered.size)
        assertTrue(filtered.contains(item2))
        assertTrue(filtered.contains(item3))
    }

    @Test
    fun testScanResultAggregation() {
        val item1 = createItem(1, "img1.jpg", 100, 1000, type = MediaType.IMAGE)
        val item2 = createItem(2, "vid1.mp4", 200, 2000, type = MediaType.VIDEO, isTrashed = true)
        val item3 = createItem(3, "img2.jpg", 300, 3000, type = MediaType.IMAGE, isHidden = true)

        val list = listOf(item1, item2, item3)
        val imagesFound = list.count { it.mediaType == MediaType.IMAGE }
        val videosFound = list.count { it.mediaType == MediaType.VIDEO }
        val trashedCount = list.count { it.isTrashed }
        val hiddenCount = list.count { it.isHidden }
        val recoverableCount = list.count { it.isRecoverable }

        val scanResult = ScanResult(
            imagesFound = imagesFound,
            videosFound = videosFound,
            recoverableCount = recoverableCount,
            trashedCount = trashedCount,
            hiddenCount = hiddenCount,
            activeCount = list.size - recoverableCount,
            totalScanned = list.size,
            durationMs = 1500L,
            items = list
        )

        assertEquals(2, scanResult.imagesFound)
        assertEquals(1, scanResult.videosFound)
        assertEquals(2, scanResult.recoverableCount)
        assertEquals(1, scanResult.trashedCount)
        assertEquals(1, scanResult.hiddenCount)
        assertEquals(1, scanResult.activeCount)
        assertEquals(3, scanResult.totalScanned)
    }

    @Test
    fun testEmptyCollectionBehavior() {
        val emptyList = emptyList<MediaItem>()
        assertTrue(emptyList.isEmpty())
        val sorted = emptyList.sortedByDescending { it.dateModified }
        assertTrue(sorted.isEmpty())
        val filtered = emptyList.filter { it.isRecoverable }
        assertTrue(filtered.isEmpty())
    }
}
