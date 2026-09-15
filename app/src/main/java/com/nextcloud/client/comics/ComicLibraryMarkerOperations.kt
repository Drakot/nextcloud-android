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
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.operations.RefreshFolderOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Creates or deletes the marker file on the server and refreshes the folder so the local database reflects it
 * immediately instead of waiting for the next sync.
 */
class ComicLibraryMarkerOperations(
    private val context: Context,
    private val user: User,
    private val storageManager: FileDataStorageManager
) {
    suspend fun markAsLibrary(folder: OCFile): Boolean = withContext(Dispatchers.IO) {
        val localFile = File(context.cacheDir, ComicLibraryMarker.FILE_NAME)
        if (!localFile.exists() && !localFile.createNewFile()) {
            Log_OC.e(TAG, "Cannot create local marker file")
            return@withContext false
        }
        val modifiedSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        val upload = UploadFileRemoteOperation(
            localFile.absolutePath,
            ComicLibraryMarker.remotePathFor(folder),
            MARKER_MIME_TYPE,
            modifiedSeconds
        )
        val uploaded = upload.execute(user, context).isSuccess
        uploaded && refresh(folder)
    }

    suspend fun unmarkLibrary(folder: OCFile): Boolean = withContext(Dispatchers.IO) {
        val marker = ComicLibraryMarker.markerOf(folder, storageManager) ?: return@withContext true
        val removed = RemoveFileRemoteOperation(marker.remotePath).execute(user, context).isSuccess
        if (removed) {
            storageManager.removeFile(marker, true, true)
        }
        removed && refresh(folder)
    }

    private fun refresh(folder: OCFile): Boolean {
        val operation = RefreshFolderOperation(
            folder,
            System.currentTimeMillis(),
            false,
            true,
            storageManager,
            user,
            context
        )
        return operation.execute(user, context).isSuccess
    }

    companion object {
        private const val TAG = "ComicLibraryMarkerOperations"
        private const val MARKER_MIME_TYPE = "application/octet-stream"
    }
}
