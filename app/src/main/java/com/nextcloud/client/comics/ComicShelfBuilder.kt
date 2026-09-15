/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.sort.AlphanumericComparator
import java.util.ArrayDeque

/**
 * Walks the library tree already present in the local database. Every folder holding images is a comic, folders
 * without images only group other folders and are never shown. Images lying directly in the library become a
 * comic named after the library itself.
 */
class ComicShelfBuilder(
    private val folderContent: (OCFile) -> List<OCFile>,
    private val lastReadPage: (OCFile) -> Int
) {
    fun build(library: OCFile): List<Comic> {
        val comics = mutableListOf<Comic>()
        val pending = ArrayDeque<OCFile>()
        pending.add(library)

        while (pending.isNotEmpty()) {
            val folder = pending.removeFirst()
            val content = folderContent(folder)
            comicOf(folder, content)?.let { comics.add(it) }
            content.filter { it.isFolder && !it.isEncrypted }.forEach { pending.add(it) }
        }

        return comics.sortedWith { first, second -> AlphanumericComparator.compare(first.folder, second.folder) }
    }

    private fun comicOf(folder: OCFile, content: List<OCFile>): Comic? {
        val pages = ComicPages.of(content)
        if (pages.isEmpty()) {
            return null
        }
        val lastPage = lastReadPage(folder).coerceIn(0, pages.size - 1)
        return Comic(folder, pages, lastPage)
    }
}
