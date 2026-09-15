/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import android.content.Context
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RefreshFolderOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.ArrayDeque

/**
 * Fetches the metadata of every folder below a library so the shelf can show covers and page counts without the
 * user opening each comic. The first run of a big library is slow, later runs skip every folder whose etag did not
 * change, which costs no request at all because the parent listing already carries the children's etags.
 *
 * The walk runs in an application-wide scope so leaving the shelf does not cancel it.
 */
object ComicLibrarySync {
    private const val TAG = "ComicLibrarySync"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow<ComicSyncState>(ComicSyncState.Idle)

    val state: StateFlow<ComicSyncState> = mutableState

    fun start(context: Context, user: User, library: OCFile, ignoreEtags: Boolean) {
        if (mutableState.value is ComicSyncState.Running) {
            Log_OC.d(TAG, "Sync already running, ignoring request for ${library.remotePath}")
            return
        }
        mutableState.value = ComicSyncState.Running(library.remotePath, 0)
        val appContext = context.applicationContext
        scope.launch {
            val storageManager = FileDataStorageManager(user, appContext.contentResolver)
            val walker = LibraryWalker(appContext, user, storageManager, ignoreEtags, library.remotePath)
            mutableState.value = walker.walk(library)
        }
    }

    private class LibraryWalker(
        private val context: Context,
        private val user: User,
        private val storageManager: FileDataStorageManager,
        private val ignoreEtags: Boolean,
        private val libraryPath: String
    ) {
        private var checked = 0
        private var failed = 0

        fun walk(library: OCFile): ComicSyncState {
            val pending = ArrayDeque<OCFile>()
            pending.add(library)
            while (pending.isNotEmpty()) {
                val folder = pending.removeFirst()
                if (!refresh(folder)) {
                    failed++
                }
                checked++
                mutableState.value = ComicSyncState.Running(libraryPath, checked)
                subfoldersOf(folder).forEach { pending.add(it) }
            }
            return ComicSyncState.Finished(libraryPath, checked, failed)
        }

        private fun subfoldersOf(folder: OCFile): List<OCFile> {
            val refreshed = storageManager.getFileByEncryptedRemotePath(folder.remotePath) ?: folder
            return storageManager.getFolderContent(refreshed, false).filter { it.isFolder && !it.isEncrypted }
        }

        private fun refresh(folder: OCFile): Boolean {
            if (folder.isEncrypted || isUpToDate(folder)) {
                return true
            }
            val operation = RefreshFolderOperation(folder, storageManager, user, context)
            return runCatching { operation.execute(user, context).isSuccess }
                .onFailure { Log_OC.e(TAG, "Refresh of ${folder.remotePath} failed", it) }
                .getOrDefault(false)
        }

        private fun isUpToDate(folder: OCFile): Boolean =
            !ignoreEtags && !folder.etag.isNullOrEmpty() && !folder.isEtagChanged
    }
}
