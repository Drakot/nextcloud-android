/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertEquals
import org.junit.Test

class ComicPagesTest {

    private fun image(path: String): OCFile = OCFile(path).apply { mimeType = "image/jpeg" }

    private fun folder(path: String): OCFile = OCFile(path).apply { mimeType = "DIR" }

    @Test
    fun `pages follow natural ascending file name order`() {
        val content = listOf(image("/c/10.jpg"), image("/c/2.jpg"), image("/c/1.jpg"), image("/c/03.jpg"))

        val pages = ComicPages.of(content).map { it.fileName }

        assertEquals(listOf("1.jpg", "2.jpg", "03.jpg", "10.jpg"), pages)
    }

    @Test
    fun `folders and hidden files are not pages`() {
        val content = listOf(folder("/c/extras/"), image("/c/.cover.jpg"), image("/c/1.jpg"))

        val pages = ComicPages.of(content).map { it.fileName }

        assertEquals(listOf("1.jpg"), pages)
    }
}
