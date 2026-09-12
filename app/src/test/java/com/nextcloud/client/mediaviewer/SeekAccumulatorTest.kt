/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekAccumulatorTest {

    private val step = 10_000L

    @Test
    fun `starts inactive and becomes active with the first step`() {
        val accumulator = SeekAccumulator()
        assertFalse(accumulator.isActive)

        assertTrue(accumulator.add(PlayerGestureZone.SEEK_FORWARD, step))

        assertTrue(accumulator.isActive)
        assertEquals(PlayerGestureZone.SEEK_FORWARD, accumulator.zone)
        assertEquals(step, accumulator.pendingMs)
    }

    @Test
    fun `steps on the same side add up`() {
        val accumulator = SeekAccumulator()
        repeat(3) { accumulator.add(PlayerGestureZone.SEEK_BACKWARD, step) }

        assertEquals(3 * step, accumulator.pendingMs)
        assertEquals(70_000L, accumulator.targetPosition(currentPositionMs = 100_000L, durationMs = 600_000L))
    }

    @Test
    fun `a tap on the other side is rejected and keeps the burst untouched`() {
        val accumulator = SeekAccumulator()
        accumulator.add(PlayerGestureZone.SEEK_FORWARD, step)

        assertFalse(accumulator.add(PlayerGestureZone.SEEK_BACKWARD, step))

        assertEquals(PlayerGestureZone.SEEK_FORWARD, accumulator.zone)
        assertEquals(step, accumulator.pendingMs)
    }

    @Test
    fun `target position never leaves the media`() {
        val accumulator = SeekAccumulator()
        repeat(5) { accumulator.add(PlayerGestureZone.SEEK_BACKWARD, step) }
        assertEquals(0L, accumulator.targetPosition(currentPositionMs = 20_000L, durationMs = 600_000L))

        accumulator.reset()
        repeat(5) { accumulator.add(PlayerGestureZone.SEEK_FORWARD, step) }
        assertEquals(600_000L, accumulator.targetPosition(currentPositionMs = 580_000L, durationMs = 600_000L))
    }

    @Test
    fun `unknown duration does not clamp forward seeks`() {
        val accumulator = SeekAccumulator()
        accumulator.add(PlayerGestureZone.SEEK_FORWARD, step)

        assertEquals(30_000L, accumulator.targetPosition(currentPositionMs = 20_000L, durationMs = -1L))
    }

    @Test
    fun `reset clears the burst`() {
        val accumulator = SeekAccumulator()
        accumulator.add(PlayerGestureZone.SEEK_FORWARD, step)

        accumulator.reset()

        assertFalse(accumulator.isActive)
        assertEquals(0L, accumulator.pendingMs)
    }
}
