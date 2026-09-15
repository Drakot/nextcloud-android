/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.owncloud.android.R
import com.owncloud.android.utils.theme.ViewThemeUtils

class ComicShelfMenuProvider(
    private val context: Context,
    private val viewThemeUtils: ViewThemeUtils,
    private val keepsFolders: () -> Boolean,
    private val onSync: () -> Unit,
    private val onToggleKeepFolders: () -> Unit
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.comic_shelf, menu)
        menu.findItem(R.id.action_comic_sync)?.let { viewThemeUtils.platform.colorToolbarMenuIcon(context, it) }
        menu.findItem(R.id.action_comic_keep_folders)?.isChecked = keepsFolders()
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_comic_sync -> onSync()
            R.id.action_comic_keep_folders -> onToggleKeepFolders()
            else -> return false
        }
        return true
    }
}
