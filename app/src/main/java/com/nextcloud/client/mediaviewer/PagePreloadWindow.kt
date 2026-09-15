/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

/**
 * Pages worth fetching ahead of the one being read. Comics are read forward, so only the following pages count.
 */
object PagePreloadWindow {
    fun nextPositions(current: Int, itemCount: Int, count: Int): List<Int> =
        ((current + 1)..(current + count)).filter { it in 0 until itemCount }
}
