/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.owncloud.android.datamodel.OCFile
import java.io.File

/**
 * One entry of the shelf: a folder whose images are the pages, ordered by file name. [lastReadPage] is
 * zero-based and always points to an existing page. [isCoverOnDevice] is captured when the shelf is built because
 * [OCFile.isDown] looks at the file system, so two snapshots would otherwise compare equal once the download lands.
 */
data class Comic(
    override val folder: OCFile,
    val pages: List<OCFile>,
    val lastReadPage: Int,
    override val isCoverOnDevice: Boolean = pages.first().isOnDevice()
) : ComicShelfItem {
    val title: String get() = folder.fileName
    override val cover: OCFile get() = pages.first()
    val pageCount: Int get() = pages.size
    val isFinished: Boolean get() = lastReadPage >= pageCount - 1 && pageCount > 0
    val isStarted: Boolean get() = lastReadPage > 0
}

internal fun OCFile.isOnDevice(): Boolean = storagePath?.takeIf { it.isNotEmpty() }?.let { File(it).exists() } == true
