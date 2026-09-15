/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.OCFile

/**
 * Stores the last page read per comic folder. Keys use the server file id so renaming or moving the folder keeps
 * the reading position.
 */
class ComicProgressStore(private val provider: ArbitraryDataProvider, private val accountName: String) {

    fun lastReadPage(comicFolder: OCFile): Int {
        val stored = provider.getValue(accountName, keyFor(comicFolder))
        return stored.toIntOrNull() ?: 0
    }

    fun saveLastReadPage(comicFolder: OCFile, page: Int) {
        provider.storeOrUpdateKeyValue(accountName, keyFor(comicFolder), page.toString())
    }

    fun reset(comicFolder: OCFile) {
        provider.deleteKeyForAccount(accountName, keyFor(comicFolder))
    }

    private fun keyFor(comicFolder: OCFile): String = KEY_PREFIX + comicFolder.localId

    companion object {
        private const val KEY_PREFIX = "comic_last_page_"
    }
}
