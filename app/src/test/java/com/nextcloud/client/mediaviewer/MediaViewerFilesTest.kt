/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaViewerFilesTest {

    private fun image(path: String): OCFile = OCFile(path).apply { mimeType = "image/jpeg" }

    @Test
    fun `a file stored twice in the database becomes a single page`() {
        val first = image("/comic/1.jpg")
        val duplicateOfFirst = image("/comic/1.jpg")
        val second = image("/comic/2.jpg")

        val pages = MediaViewerFiles.prepare(listOf(first, duplicateOfFirst, second))

        assertEquals(2, pages.size)
        assertEquals(listOf("/comic/1.jpg", "/comic/2.jpg"), pages.map { it.remotePath })
    }

    @Test
    fun `same file names in different folders stay separate pages`() {
        val pages = MediaViewerFiles.prepare(listOf(image("/a/IMG_1.jpg"), image("/b/IMG_1.jpg")))

        assertEquals(2, pages.size)
    }
}
