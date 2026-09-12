/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

/**
 * Reader mode turns the side thirds of a page into previous/next taps. It is only offered for real folders
 * holding at least [MIN_IMAGES] items, so single photos and the gallery keep the plain tap-to-toggle behaviour.
 */
object ReaderMode {
    const val MIN_IMAGES = 10

    fun isEnabled(itemCount: Int, isRealFolder: Boolean): Boolean = isRealFolder && itemCount >= MIN_IMAGES
}
