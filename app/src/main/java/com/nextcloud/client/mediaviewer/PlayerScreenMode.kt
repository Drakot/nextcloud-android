/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

enum class PlayerScreenMode {
    Normal,
    Fullscreen;

    val isFullscreen: Boolean
        get() = this == Fullscreen

    fun toggled(): PlayerScreenMode = if (isFullscreen) Normal else Fullscreen
}
