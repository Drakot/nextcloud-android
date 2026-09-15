/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import android.content.Context
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.owncloud.android.datamodel.OCFile

/**
 * Remembers, per library, whether the user last wanted the shelf or the plain folder listing, and whether the shelf
 * flattens every comic into one grid or keeps the folder structure.
 */
@Suppress("DEPRECATION")
object ComicLibraryPreferences {
    private const val KEY_PREFIX = "comic_library_view_mode_"
    private const val LAYOUT_KEY_PREFIX = "comic_library_layout_"

    fun viewModeOf(context: Context, library: OCFile): ComicLibraryViewMode {
        val stored = PreferenceManager.getDefaultSharedPreferences(context).getString(keyFor(library), null)
        return stored?.let { name -> ComicLibraryViewMode.entries.firstOrNull { it.name == name } }
            ?: ComicLibraryViewMode.SHELF
    }

    fun setViewMode(context: Context, library: OCFile, mode: ComicLibraryViewMode) {
        PreferenceManager.getDefaultSharedPreferences(context).edit { putString(keyFor(library), mode.name) }
    }

    fun layoutOf(context: Context, library: OCFile): ComicShelfLayout {
        val stored = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(LAYOUT_KEY_PREFIX + library.remotePath, null)
        return stored?.let { name -> ComicShelfLayout.entries.firstOrNull { it.name == name } }
            ?: ComicShelfLayout.FLAT
    }

    fun setLayout(context: Context, library: OCFile, layout: ComicShelfLayout) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(LAYOUT_KEY_PREFIX + library.remotePath, layout.name)
        }
    }

    private fun keyFor(library: OCFile): String = KEY_PREFIX + library.remotePath
}
