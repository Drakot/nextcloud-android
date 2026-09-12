/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

/**
 * What a single tap on an image page should do. Page turning is only offered in reader mode
 * and while the image is not zoomed in, so panning a zoomed page never flips it by accident.
 */
enum class ReaderTapZone {
    PREVIOUS_PAGE,
    TOGGLE_CHROME,
    NEXT_PAGE;

    companion object {
        private const val SIDE_ZONE_FRACTION = 0.3f

        @Suppress("ReturnCount")
        fun from(x: Float, width: Int, isReaderMode: Boolean, isZoomed: Boolean, isRtl: Boolean): ReaderTapZone {
            if (!isReaderMode || isZoomed || width <= 0) return TOGGLE_CHROME

            val sideZoneWidth = width * SIDE_ZONE_FRACTION
            val zone = when {
                x < sideZoneWidth -> PREVIOUS_PAGE
                x > width - sideZoneWidth -> NEXT_PAGE
                else -> TOGGLE_CHROME
            }
            return if (isRtl) zone.mirrored() else zone
        }

        private fun ReaderTapZone.mirrored(): ReaderTapZone = when (this) {
            PREVIOUS_PAGE -> NEXT_PAGE
            NEXT_PAGE -> PREVIOUS_PAGE
            TOGGLE_CHROME -> TOGGLE_CHROME
        }
    }
}
