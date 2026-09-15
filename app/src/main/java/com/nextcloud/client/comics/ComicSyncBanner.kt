/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import androidx.core.view.isVisible
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentComicShelfBinding

/**
 * Shows the running sync of one library above the shelf and reports once when that sync finishes, so the shelf
 * can reload and mention folders that failed.
 */
class ComicSyncBanner(
    private val binding: FragmentComicShelfBinding,
    private val libraryPath: String,
    private val onFinished: (failedFolders: Int) -> Unit
) {
    private var wasRunning = false

    fun render(state: ComicSyncState) {
        val running = state.isRunningFor(libraryPath)
        binding.comicSyncContainer.isVisible = running
        if (state is ComicSyncState.Running && running) {
            binding.comicSyncText.text = binding.root.resources
                .getQuantityString(R.plurals.comic_library_syncing, state.foldersChecked, state.foldersChecked)
        }
        if (wasRunning && state is ComicSyncState.Finished && state.libraryPath == libraryPath) {
            onFinished(state.failedFolders)
        }
        wasRunning = running
    }
}
