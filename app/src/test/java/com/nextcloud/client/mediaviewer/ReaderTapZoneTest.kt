/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTapZoneTest {

    private val width = 1000

    private fun zone(x: Float, isReaderMode: Boolean = true, isZoomed: Boolean = false, isRtl: Boolean = false) =
        ReaderTapZone.from(x, width, isReaderMode, isZoomed, isRtl)

    @Test
    fun `left third turns to the previous page`() {
        assertEquals(ReaderTapZone.PREVIOUS_PAGE, zone(0f))
        assertEquals(ReaderTapZone.PREVIOUS_PAGE, zone(299f))
    }

    @Test
    fun `middle toggles the chrome`() {
        assertEquals(ReaderTapZone.TOGGLE_CHROME, zone(300f))
        assertEquals(ReaderTapZone.TOGGLE_CHROME, zone(500f))
        assertEquals(ReaderTapZone.TOGGLE_CHROME, zone(700f))
    }

    @Test
    fun `right third turns to the next page`() {
        assertEquals(ReaderTapZone.NEXT_PAGE, zone(701f))
        assertEquals(ReaderTapZone.NEXT_PAGE, zone(1000f))
    }

    @Test
    fun `right to left layouts mirror the page zones`() {
        assertEquals(ReaderTapZone.NEXT_PAGE, zone(0f, isRtl = true))
        assertEquals(ReaderTapZone.PREVIOUS_PAGE, zone(1000f, isRtl = true))
        assertEquals(ReaderTapZone.TOGGLE_CHROME, zone(500f, isRtl = true))
    }

    @Test
    fun `zoomed pages only toggle the chrome`() {
        assertEquals(ReaderTapZone.TOGGLE_CHROME, zone(0f, isZoomed = true))
        assertEquals(ReaderTapZone.TOGGLE_CHROME, zone(1000f, isZoomed = true))
    }

    @Test
    fun `outside reader mode every tap toggles the chrome`() {
        assertEquals(ReaderTapZone.TOGGLE_CHROME, zone(0f, isReaderMode = false))
        assertEquals(ReaderTapZone.TOGGLE_CHROME, zone(1000f, isReaderMode = false))
    }

    @Test
    fun `unmeasured view toggles the chrome`() {
        assertEquals(ReaderTapZone.TOGGLE_CHROME, ReaderTapZone.from(10f, 0, true, false, false))
    }
}
