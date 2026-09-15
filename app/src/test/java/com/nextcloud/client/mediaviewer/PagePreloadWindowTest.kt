/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import org.junit.Assert.assertEquals
import org.junit.Test

class PagePreloadWindowTest {

    @Test
    fun `the two following pages are preloaded`() {
        assertEquals(listOf(4, 5), PagePreloadWindow.nextPositions(current = 3, itemCount = 10, count = 2))
    }

    @Test
    fun `the window stops at the last page`() {
        assertEquals(listOf(9), PagePreloadWindow.nextPositions(current = 8, itemCount = 10, count = 2))
        assertEquals(emptyList<Int>(), PagePreloadWindow.nextPositions(current = 9, itemCount = 10, count = 2))
    }

    @Test
    fun `an empty pager preloads nothing`() {
        assertEquals(emptyList<Int>(), PagePreloadWindow.nextPositions(current = 0, itemCount = 0, count = 2))
    }
}
