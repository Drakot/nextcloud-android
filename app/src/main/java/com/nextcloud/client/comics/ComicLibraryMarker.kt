/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile

/**
 * A folder becomes a comic library when it contains an empty marker file. Keeping the flag inside the folder
 * makes it travel with the data to every device and client instead of living in one app's preferences.
 */
object ComicLibraryMarker {
    const val FILE_NAME = ".comics"

    private const val MAX_ANCESTOR_DEPTH = 32

    fun remotePathFor(folder: OCFile): String = folder.remotePath + FILE_NAME

    fun isLibrary(folder: OCFile?, storageManager: FileDataStorageManager): Boolean {
        if (folder == null || !folder.canBeLibrary()) {
            return false
        }
        return storageManager.getFolderContent(folder, false).any { it.fileName == FILE_NAME }
    }

    private fun OCFile.canBeLibrary(): Boolean = isFolder && !isEncrypted && !isRootDirectory

    fun markerOf(folder: OCFile, storageManager: FileDataStorageManager): OCFile? =
        storageManager.getFolderContent(folder, false).firstOrNull { it.fileName == FILE_NAME }

    /**
     * Returns the library that contains [folder], the folder itself included, or null when it is outside any
     * library. Groupers and comics live inside a library, so this is how they find their shelf.
     */
    fun findLibraryContaining(folder: OCFile?, storageManager: FileDataStorageManager): OCFile? {
        var current = folder
        var depth = 0
        while (current != null && !current.isRootDirectory && depth < MAX_ANCESTOR_DEPTH) {
            if (isLibrary(current, storageManager)) {
                return current
            }
            current = storageManager.getFileById(current.parentId)
            depth++
        }
        return null
    }
}
