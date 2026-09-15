/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.owncloud.android.datamodel.OCFile

sealed interface ComicShelfItem {
    val folder: OCFile
    val cover: OCFile
    val isCoverOnDevice: Boolean
}
