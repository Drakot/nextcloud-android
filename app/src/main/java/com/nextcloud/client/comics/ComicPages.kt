/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.sort.AlphanumericComparator

/**
 * Pages are the visible images of a folder in natural ascending file name order, regardless of the sort
 * order the user picked for that folder in the file list.
 */
object ComicPages {
    fun of(folderContent: List<OCFile>): List<OCFile> {
        val images = folderContent.filter { !it.isFolder && !it.isHidden && MimeTypeUtil.isImage(it) }
        return images.sortedWith { first, second -> AlphanumericComparator.compare(first, second) }
    }
}
