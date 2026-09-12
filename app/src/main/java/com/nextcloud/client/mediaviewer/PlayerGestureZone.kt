/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

/**
 * Horizontal region of the video surface a double tap landed on.
 */
enum class PlayerGestureZone {
    SEEK_BACKWARD,
    PLAY_PAUSE,
    SEEK_FORWARD;

    companion object {
        private const val SIDE_ZONE_FRACTION = 0.4f

        fun from(x: Float, width: Int): PlayerGestureZone {
            if (width <= 0) return PLAY_PAUSE

            val sideZoneWidth = width * SIDE_ZONE_FRACTION
            return when {
                x < sideZoneWidth -> SEEK_BACKWARD
                x > width - sideZoneWidth -> SEEK_FORWARD
                else -> PLAY_PAUSE
            }
        }
    }
}
