package com.logybyte.assetsrecovery

import android.net.Uri
import com.logybyte.assetsrecovery.data.model.MediaItem
import com.logybyte.assetsrecovery.data.model.MediaStorageType
import com.logybyte.assetsrecovery.data.model.MediaType
import com.logybyte.assetsrecovery.data.model.RecoveryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

class MediaItemTest {

    private val dummyUri: Uri = mock(Uri::class.java)

    @Test
    fun testIsRecoverable_trashedItem() {
        val item = MediaItem(
            id = 1L,
            uri = dummyUri,
            displayName = "test.jpg",
            path = "/DCIM/.trashed/test.jpg",
            size = 1024L,
            dateModified = 1000L,
            mimeType = "image/jpeg",
            mediaType = MediaType.IMAGE,
            storageType = MediaStorageType.MEDIA_TRASH,
            isTrashed = true
        )
        assertTrue(item.isRecoverable)
    }

    @Test
    fun testIsRecoverable_hiddenItem() {
        val item = MediaItem(
            id = 2L,
            uri = dummyUri,
            displayName = ".hidden_photo.png",
            path = "/DCIM/.nomedia/.hidden_photo.png",
            size = 2048L,
            dateModified = 1000L,
            mimeType = "image/png",
            mediaType = MediaType.IMAGE,
            storageType = MediaStorageType.HIDDEN_ORPHANED,
            isHidden = true
        )
        assertTrue(item.isRecoverable)
    }

    @Test
    fun testIsRecoverable_activeGalleryItem() {
        val item = MediaItem(
            id = 3L,
            uri = dummyUri,
            displayName = "normal_camera_pic.jpg",
            path = "/DCIM/Camera/normal_camera_pic.jpg",
            size = 4096L,
            dateModified = 1000L,
            mimeType = "image/jpeg",
            mediaType = MediaType.IMAGE,
            storageType = MediaStorageType.ACTIVE_GALLERY,
            isTrashed = false,
            isHidden = false
        )
        assertFalse(item.isRecoverable)
    }

    @Test
    fun testRecoveryStatusUpdate() {
        val original = MediaItem(
            id = 4L,
            uri = dummyUri,
            displayName = "pic.jpg",
            path = "/path/pic.jpg",
            size = 1000L,
            dateModified = 1000L,
            mimeType = "image/jpeg",
            mediaType = MediaType.IMAGE,
            storageType = MediaStorageType.MEDIA_TRASH,
            isTrashed = true,
            recoveryStatus = RecoveryStatus.NONE
        )

        val recovered = original.copy(
            recoveryStatus = RecoveryStatus.RECOVERED,
            recoveryPath = "content://media/external/images/media/999"
        )

        assertEquals(RecoveryStatus.RECOVERED, recovered.recoveryStatus)
        assertEquals("content://media/external/images/media/999", recovered.recoveryPath)
    }
}
