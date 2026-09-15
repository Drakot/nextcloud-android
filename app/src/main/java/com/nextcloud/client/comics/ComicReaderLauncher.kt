/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import android.content.Context
import android.content.Intent
import com.nextcloud.client.mediaviewer.MediaViewerActivity
import com.owncloud.android.ui.activity.FileActivity

object ComicReaderLauncher {
    fun open(context: Context, comic: Comic, fromStart: Boolean = false) {
        val page = if (fromStart) 0 else comic.lastReadPage
        val intent = Intent(context, MediaViewerActivity::class.java).apply {
            putExtra(FileActivity.EXTRA_FILE, comic.pages[page])
            putExtra(MediaViewerActivity.EXTRA_COMIC_MODE, true)
        }
        context.startActivity(intent)
    }
}
