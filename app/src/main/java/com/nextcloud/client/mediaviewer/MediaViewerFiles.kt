/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import com.owncloud.android.datamodel.OCFile

/**
 * Turns the raw database content of a folder into the pages of the viewer. The file list applies the same two
 * clean-ups before showing a folder: the local database can hold the same remote file twice after overlapping
 * syncs, and a live photo's video must not become a page of its own.
 */
object MediaViewerFiles {

    fun prepare(files: List<OCFile>): MutableList<OCFile> = LivePhotoMerger.merge(files.distinctBy { it.remotePath })
}
