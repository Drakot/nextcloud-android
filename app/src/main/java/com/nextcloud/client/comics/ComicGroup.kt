/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.owncloud.android.datamodel.OCFile

/**
 * A folder without pages of its own that leads to [comicCount] comics further down. Its cover borrows the cover
 * of the first comic below it.
 */
data class ComicGroup(
    override val folder: OCFile,
    val comicCount: Int,
    override val cover: OCFile,
    override val isCoverOnDevice: Boolean = cover.isOnDevice()
) : ComicShelfItem {
    val title: String get() = folder.fileName
}
