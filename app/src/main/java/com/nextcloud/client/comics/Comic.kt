/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.owncloud.android.datamodel.OCFile

/**
 * One entry of the shelf: a folder whose images are the pages, ordered by file name. [lastReadPage] is
 * zero-based and always points to an existing page.
 */
data class Comic(val folder: OCFile, val pages: List<OCFile>, val lastReadPage: Int) {
    val title: String get() = folder.fileName
    val cover: OCFile get() = pages.first()
    val pageCount: Int get() = pages.size
    val isFinished: Boolean get() = lastReadPage >= pageCount - 1 && pageCount > 0
    val isStarted: Boolean get() = lastReadPage > 0
}
