/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager

/**
 * Servers without preview generation cannot hand out cover thumbnails, so the first page of each comic is fetched
 * instead and the cover is rendered from the local copy. Downloads go in shelf order and are capped per pass;
 * later passes continue once the earlier pages are on the device.
 */
object ComicCoverDownloads {
    private const val MAX_DOWNLOADS_PER_PASS = 12

    fun requestMissing(user: User, storageManager: FileDataStorageManager, items: List<ComicShelfItem>) {
        items.asSequence()
            .map { it.cover }
            .filter { !it.isDown }
            .take(MAX_DOWNLOADS_PER_PASS)
            .forEach { ComicPageDownloads.requestIfMissing(user, storageManager, it) }
    }
}
