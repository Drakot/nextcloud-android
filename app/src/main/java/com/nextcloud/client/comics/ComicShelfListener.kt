/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import android.view.View

interface ComicShelfListener {
    fun onReadComic(comic: Comic)

    fun onOpenComicFolder(comic: Comic)

    fun onComicLongPressed(comic: Comic, anchor: View)
}
