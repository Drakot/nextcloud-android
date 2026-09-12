/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import android.content.Context
import android.preference.PreferenceManager

/**
 * The custom viewer is opt-out: the switch in the settings screen writes this key and the launch points read it.
 */
object MediaViewerSettings {
    const val PREFERENCE_KEY = "custom_media_viewer"
    private const val DEFAULT_ENABLED = true

    // The legacy settings screen still stores its switches through the framework PreferenceManager.
    @Suppress("DEPRECATION")
    fun isEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFERENCE_KEY, DEFAULT_ENABLED)
}
