/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile

/**
 * A page removed from the device by hand keeps its previous local path in the database, and a download reuses that
 * path even when it lies outside the storage location the user has since configured. Forgetting the path first makes
 * the page land in the current location.
 */
object ComicPageDownloads {
    fun requestIfMissing(user: User, storageManager: FileDataStorageManager, page: OCFile) {
        if (page.isDown) {
            return
        }
        forgetStalePath(storageManager, page)
        FileDownloadHelper.instance().downloadFileIfNotStartedBefore(user, page)
    }

    fun forgetStalePaths(storageManager: FileDataStorageManager, pages: List<OCFile>) {
        pages.filter { !it.isDown }.forEach { forgetStalePath(storageManager, it) }
    }

    private fun forgetStalePath(storageManager: FileDataStorageManager, page: OCFile) {
        if (page.storagePath.isNullOrEmpty()) {
            return
        }
        page.storagePath = null
        storageManager.saveFile(page)
    }
}
