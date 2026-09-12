/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerGestureZoneTest {

    private val width = 1000

    @Test
    fun `tap on the left side seeks backward`() {
        assertEquals(PlayerGestureZone.SEEK_BACKWARD, PlayerGestureZone.from(0f, width))
        assertEquals(PlayerGestureZone.SEEK_BACKWARD, PlayerGestureZone.from(399f, width))
    }

    @Test
    fun `tap in the middle toggles playback`() {
        assertEquals(PlayerGestureZone.PLAY_PAUSE, PlayerGestureZone.from(400f, width))
        assertEquals(PlayerGestureZone.PLAY_PAUSE, PlayerGestureZone.from(500f, width))
        assertEquals(PlayerGestureZone.PLAY_PAUSE, PlayerGestureZone.from(600f, width))
    }

    @Test
    fun `tap on the right side seeks forward`() {
        assertEquals(PlayerGestureZone.SEEK_FORWARD, PlayerGestureZone.from(601f, width))
        assertEquals(PlayerGestureZone.SEEK_FORWARD, PlayerGestureZone.from(1000f, width))
    }

    @Test
    fun `unmeasured view never seeks`() {
        assertEquals(PlayerGestureZone.PLAY_PAUSE, PlayerGestureZone.from(10f, 0))
    }
}
