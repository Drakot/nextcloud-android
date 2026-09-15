/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import android.view.Menu
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragment
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Long-press actions of one comic on the shelf. Downloading reuses the folder sync behind the "Download" action of
 * the file list, so pages arrive with the same conflict handling and notifications as any other folder.
 */
class ComicContextMenu(
    private val fragment: Fragment,
    private val host: Host,
    private val library: OCFile,
    private val storageManager: FileDataStorageManager,
    private val progressStore: ComicProgressStore
) {
    interface Host {
        fun onComicProgressChanged()

        fun showComicMessage(message: String)
    }

    fun show(comic: Comic, anchor: View) {
        PopupMenu(fragment.requireContext(), anchor).apply {
            menu.add(Menu.NONE, MENU_START_OVER, Menu.NONE, R.string.comic_start_over)
            menu.add(Menu.NONE, MENU_MARK_AS_READ, Menu.NONE, R.string.comic_mark_as_read)
            menu.add(Menu.NONE, MENU_DOWNLOAD, Menu.NONE, R.string.filedetails_download)
            menu.add(Menu.NONE, MENU_OPEN_FOLDER, Menu.NONE, R.string.comic_open_folder)
            setOnMenuItemClickListener { item -> onItemSelected(item.itemId, comic) }
        }.show()
    }

    private fun onItemSelected(itemId: Int, comic: Comic): Boolean {
        when (itemId) {
            MENU_START_OVER -> {
                progressStore.reset(comic.folder)
                ComicReaderLauncher.open(fragment.requireContext(), comic, fromStart = true)
            }

            MENU_MARK_AS_READ -> {
                progressStore.saveLastReadPage(comic.folder, comic.pageCount - 1)
                host.onComicProgressChanged()
            }

            MENU_DOWNLOAD -> download(comic)

            MENU_OPEN_FOLDER -> hostActivity()?.openComicFolder(comic.folder, library)

            else -> return false
        }
        return true
    }

    private fun download(comic: Comic) {
        val activity = hostActivity() ?: return
        if (!FileStorageUtils.checkIfEnoughSpace(comic.folder)) {
            SyncFileNotEnoughSpaceDialogFragment
                .newInstance(comic.folder, FileOperationsHelper.getAvailableSpaceOnDevice())
                .show(fragment.parentFragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
            return
        }
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { ComicPageDownloads.forgetStalePaths(storageManager, comic.pages) }
            activity.fileOperationsHelper.syncFileOrFolder(comic.folder)
            host.showComicMessage(fragment.getString(R.string.downloader_download_in_progress_ticker))
        }
    }

    private fun hostActivity(): FileDisplayActivity? = fragment.activity as? FileDisplayActivity

    private companion object {
        const val MENU_START_OVER = 1
        const val MENU_MARK_AS_READ = 2
        const val MENU_DOWNLOAD = 3
        const val MENU_OPEN_FOLDER = 4
    }
}
