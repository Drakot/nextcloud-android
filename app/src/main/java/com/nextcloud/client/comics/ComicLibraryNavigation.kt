/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.nextcloud.client.mediaviewer.MediaViewerSettings
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileDisplayActivity

/**
 * Entry point for the hooks in [FileDisplayActivity]: whenever the file list lands on a folder, the shelf replaces
 * it if the folder is a library the user last viewed as a shelf.
 */
object ComicLibraryNavigation {
    fun showShelfIfPreferred(activity: FileDisplayActivity, folder: OCFile?): Boolean {
        activity.invalidateMenu()
        if (folder == null || !prefersShelf(activity, folder)) {
            return false
        }
        activity.showComicShelf(folder)
        return true
    }

    private fun prefersShelf(activity: FileDisplayActivity, folder: OCFile): Boolean =
        MediaViewerSettings.isEnabled(activity) &&
            ComicLibraryMarker.isLibrary(folder, activity.storageManager) &&
            ComicLibraryPreferences.viewModeOf(activity, folder) == ComicLibraryViewMode.SHELF
}
