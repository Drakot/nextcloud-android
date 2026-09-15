/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.owncloud.android.datamodel.OCFile

data class ComicLibraryMenuState(
    val currentDir: OCFile?,
    val library: OCFile?,
    val isShelfShown: Boolean,
    val isFileList: Boolean
) {
    val isInsideLibrary: Boolean get() = library != null
    val isAtLibraryRoot: Boolean get() = library != null && currentDir?.remotePath == library.remotePath
    val canShowShelf: Boolean get() = isInsideLibrary && !isShelfShown && isFileList
    val canMarkCurrentFolder: Boolean
        get() = currentDir?.isFolder == true && !currentDir.isRootDirectory && !currentDir.isEncrypted &&
            !isInsideLibrary && isFileList
    val canUnmarkCurrentFolder: Boolean get() = isAtLibraryRoot && (isShelfShown || isFileList)
}
