/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

/**
 * Collects the seek steps of a burst of taps on one side of the player. The player position is only changed
 * once the burst ends, so tapping five times skips 50 s in one jump instead of five stuttering jumps.
 */
class SeekAccumulator {
    var zone: PlayerGestureZone? = null
        private set

    var pendingMs: Long = 0
        private set

    val isActive: Boolean
        get() = zone != null

    /**
     * Adds one step. Returns false when the tap landed on the other side, which ends the current burst
     * without adding to it; the caller commits and may start a new burst.
     */
    fun add(tapZone: PlayerGestureZone, stepMs: Long): Boolean {
        val current = zone
        if (current != null && current != tapZone) {
            return false
        }
        zone = tapZone
        pendingMs += stepMs
        return true
    }

    fun targetPosition(currentPositionMs: Long, durationMs: Long): Long {
        val signedOffset = if (zone == PlayerGestureZone.SEEK_BACKWARD) -pendingMs else pendingMs
        val upperBound = if (durationMs > 0) durationMs else Long.MAX_VALUE
        return (currentPositionMs + signedOffset).coerceIn(0, upperBound)
    }

    fun reset() {
        zone = null
        pendingMs = 0
    }
}
