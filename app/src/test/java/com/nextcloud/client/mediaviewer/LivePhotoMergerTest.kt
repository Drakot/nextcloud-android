/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class LivePhotoMergerTest {

    private fun file(path: String, mimeType: String, localId: Long, linkedLocalId: Long? = null): OCFile =
        OCFile(path).apply {
            this.mimeType = mimeType
            this.localId = localId
            setLivePhoto(linkedLocalId?.toString())
        }

    @Test
    fun `the video half of a live photo is folded into its still image`() {
        val photo = file("/IMG_1.HEIC", "image/heic", localId = 1, linkedLocalId = 2)
        val video = file("/IMG_1.MOV", "video/quicktime", localId = 2, linkedLocalId = 1)
        val other = file("/other.jpg", "image/jpeg", localId = 3)

        val merged = LivePhotoMerger.merge(listOf(photo, video, other))

        assertEquals(listOf(photo, other), merged)
        assertSame(video, photo.livePhotoVideo)
    }

    @Test
    fun `a video listed before its still image is folded as well`() {
        val video = file("/IMG_1.MOV", "video/quicktime", localId = 2, linkedLocalId = 1)
        val photo = file("/IMG_1.HEIC", "image/heic", localId = 1, linkedLocalId = 2)

        val merged = LivePhotoMerger.merge(listOf(video, photo))

        assertEquals(listOf(photo), merged)
        assertSame(video, photo.livePhotoVideo)
    }

    @Test
    fun `a dangling link keeps the file and adds nothing`() {
        val photo = file("/IMG_1.HEIC", "image/heic", localId = 1, linkedLocalId = 99)

        val merged = LivePhotoMerger.merge(listOf(photo))

        assertEquals(listOf(photo), merged)
        assertNull(photo.livePhotoVideo)
    }

    @Test
    fun `plain images and videos are untouched`() {
        val image = file("/a.jpg", "image/jpeg", localId = 1)
        val video = file("/b.mp4", "video/mp4", localId = 2)

        assertEquals(listOf(image, video), LivePhotoMerger.merge(listOf(image, video)))
    }
}
