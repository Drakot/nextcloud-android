/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

sealed class ComicSyncState {
    data object Idle : ComicSyncState()

    data class Running(val libraryPath: String, val foldersChecked: Int) : ComicSyncState()

    data class Finished(val libraryPath: String, val foldersChecked: Int, val failedFolders: Int) : ComicSyncState()

    fun isRunningFor(libraryPath: String): Boolean = this is Running && this.libraryPath == libraryPath
}
