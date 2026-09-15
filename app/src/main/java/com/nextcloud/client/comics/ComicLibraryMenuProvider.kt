/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.mediaviewer.MediaViewerSettings
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.fragment.OCFileListFragment
import kotlinx.coroutines.launch

/**
 * Toolbar entries that switch between shelf and folder listing and that create or remove the library marker.
 * Lives in the activity so the toggle is reachable both from the shelf and from any folder inside a library.
 */
class ComicLibraryMenuProvider(private val activity: FileDisplayActivity) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.comic_library, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        val state = currentState()
        menu.findItem(R.id.action_comic_show_shelf)?.let {
            it.isVisible = state.canShowShelf
            activity.viewThemeUtils.platform.colorToolbarMenuIcon(activity, it)
        }
        menu.findItem(R.id.action_comic_show_folder)?.let {
            it.isVisible = state.isShelfShown
            activity.viewThemeUtils.platform.colorToolbarMenuIcon(activity, it)
        }
        menu.findItem(R.id.action_comic_mark_library)?.isVisible = state.canMarkCurrentFolder
        menu.findItem(R.id.action_comic_unmark_library)?.isVisible = state.canUnmarkCurrentFolder
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        val state = currentState()
        when (menuItem.itemId) {
            R.id.action_comic_show_shelf -> state.library?.let { showShelf(it) }
            R.id.action_comic_show_folder -> state.library?.let { showFolder(it) }
            R.id.action_comic_mark_library -> state.currentDir?.let { changeMarker(it, create = true) }
            R.id.action_comic_unmark_library -> state.library?.let { changeMarker(it, create = false) }
            else -> return false
        }
        return true
    }

    private fun currentState(): ComicLibraryMenuState {
        val currentDir = activity.currentDir
        val enabled = MediaViewerSettings.isEnabled(activity)
        if (!enabled || currentDir == null) {
            return ComicLibraryMenuState(currentDir, null, isShelfShown = false, isFileList = false)
        }
        val library = ComicLibraryMarker.findLibraryContaining(currentDir, activity.storageManager)
        val leftFragment = activity.leftFragment
        return ComicLibraryMenuState(
            currentDir = currentDir,
            library = library,
            isShelfShown = leftFragment is ComicShelfFragment,
            isFileList = leftFragment is OCFileListFragment && leftFragment.isSearchFragment.not()
        )
    }

    private fun showShelf(library: OCFile) {
        ComicLibraryPreferences.setViewMode(activity, library, ComicLibraryViewMode.SHELF)
        if (activity.currentDir?.remotePath == library.remotePath) {
            activity.showComicShelf(library)
            return
        }
        activity.listOfFilesFragment?.run {
            setFileDepth(library)
            onItemClicked(library)
        }
    }

    private fun showFolder(library: OCFile) {
        ComicLibraryPreferences.setViewMode(activity, library, ComicLibraryViewMode.FOLDER)
        activity.exitComicShelf(browseUp = false)
    }

    private fun changeMarker(folder: OCFile, create: Boolean) {
        val user = activity.user.orElse(null) ?: return
        val operations = ComicLibraryMarkerOperations(activity, user, activity.storageManager)
        activity.lifecycleScope.launch {
            val success = if (create) operations.markAsLibrary(folder) else operations.unmarkLibrary(folder)
            if (!success) {
                showMarkerError()
                return@launch
            }
            onMarkerChanged(folder, create)
        }
    }

    private fun showMarkerError() {
        val root = activity.findViewById<View>(android.R.id.content)
        Snackbar.make(root, R.string.comic_library_marker_failed, Snackbar.LENGTH_LONG).show()
    }

    private fun onMarkerChanged(folder: OCFile, created: Boolean) {
        if (created) {
            ComicLibraryPreferences.setViewMode(activity, folder, ComicLibraryViewMode.SHELF)
            activity.showComicShelf(folder)
            return
        }
        if (activity.leftFragment is ComicShelfFragment) {
            activity.exitComicShelf(browseUp = false)
        }
        activity.listOfFilesFragment?.refreshDirectory()
        activity.invalidateMenu()
    }
}
