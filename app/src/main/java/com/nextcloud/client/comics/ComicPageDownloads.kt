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
        if (!page.storagePath.isNullOrEmpty()) {
            page.storagePath = null
            storageManager.saveFile(page)
        }
        FileDownloadHelper.instance().downloadFileIfNotStartedBefore(user, page)
    }
}
