/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.owncloud.android.R
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.fragment.FileFragment

class MediaViewerErrorFragment : FileFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_media_viewer_error, container, false)

    companion object {
        fun newInstance(): FileFragment = MediaViewerErrorFragment().apply {
            arguments = bundleOf(FileActivity.EXTRA_FILE to null)
        }
    }
}
