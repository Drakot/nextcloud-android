/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComicLibraryMenuStateTest {

    private fun folder(path: String): OCFile = OCFile(path).apply {
        mimeType = "DIR"
        decryptedRemotePath = path
    }

    private val library = folder("/Comics/")

    @Test
    fun `a plain folder can only be marked`() {
        val state = ComicLibraryMenuState(folder("/Photos/"), null, isShelfShown = false, isFileList = true)

        assertTrue(state.canMarkCurrentFolder)
        assertFalse(state.canShowShelf)
        assertFalse(state.canUnmarkCurrentFolder)
    }

    @Test
    fun `the root folder cannot be marked`() {
        val state = ComicLibraryMenuState(folder(OCFile.ROOT_PATH), null, isShelfShown = false, isFileList = true)

        assertFalse(state.canMarkCurrentFolder)
    }

    @Test
    fun `inside a library the list offers the shelf and the shelf offers the list`() {
        val listState = ComicLibraryMenuState(folder("/Comics/Hulk/"), library, isShelfShown = false, isFileList = true)
        val shelfState = ComicLibraryMenuState(library, library, isShelfShown = true, isFileList = false)

        assertTrue(listState.canShowShelf)
        assertFalse(listState.canUnmarkCurrentFolder)
        assertFalse(shelfState.canShowShelf)
        assertTrue(shelfState.canUnmarkCurrentFolder)
    }

    @Test
    fun `search results never offer library actions`() {
        val state = ComicLibraryMenuState(folder("/Photos/"), null, isShelfShown = false, isFileList = false)

        assertFalse(state.canMarkCurrentFolder)
    }
}
