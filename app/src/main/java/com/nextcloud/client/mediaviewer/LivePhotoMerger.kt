/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.MimeTypeUtil
import java.util.Collections
import java.util.IdentityHashMap

/**
 * A live photo is stored as two files, the still image and a short video, linked through the server's
 * live photo metadata. The file list shows them as one entry; the pager has to do the same, otherwise every
 * live photo takes two pages and the second one looks like a duplicate of the first.
 */
object LivePhotoMerger {

    fun merge(files: List<OCFile>): MutableList<OCFile> {
        val byLocalId = files
            .filter { it.localId != 0L && it.localId != -1L }
            .associateBy { it.localId.toString() }
        // OCFile.equals only looks at the ids, which are not set for files that never reached the database
        val videosToHide: MutableSet<OCFile> = Collections.newSetFromMap(IdentityHashMap())

        for (file in files) {
            val linkedId = file.linkedFileIdForLivePhoto ?: continue
            val linkedFile = byLocalId[linkedId] ?: continue

            when {
                MimeTypeUtil.isVideo(linkedFile.mimeType) -> {
                    file.livePhotoVideo = linkedFile
                    videosToHide.add(linkedFile)
                }

                MimeTypeUtil.isVideo(file.mimeType) -> {
                    linkedFile.livePhotoVideo = file
                    videosToHide.add(file)
                }
            }
        }

        return files.filterNot { it in videosToHide }.toMutableList()
    }
}
