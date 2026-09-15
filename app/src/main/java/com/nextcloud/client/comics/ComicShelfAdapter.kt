/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.owncloud.android.databinding.ItemComicBinding
import com.owncloud.android.utils.theme.ViewThemeUtils

class ComicShelfAdapter(
    private val thumbnailGenerator: ThumbnailGenerator,
    private val viewThemeUtils: ViewThemeUtils,
    private val listener: ComicShelfListener
) : ListAdapter<Comic, ComicViewHolder>(ComicDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComicViewHolder {
        val binding = ItemComicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ComicViewHolder(binding, thumbnailGenerator, viewThemeUtils, listener)
    }

    override fun onBindViewHolder(holder: ComicViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private object ComicDiffCallback : DiffUtil.ItemCallback<Comic>() {
        override fun areItemsTheSame(oldItem: Comic, newItem: Comic): Boolean =
            oldItem.folder.remotePath == newItem.folder.remotePath

        override fun areContentsTheSame(oldItem: Comic, newItem: Comic): Boolean =
            oldItem.lastReadPage == newItem.lastReadPage &&
                oldItem.pageCount == newItem.pageCount &&
                oldItem.cover.remotePath == newItem.cover.remotePath &&
                oldItem.cover.isDown == newItem.cover.isDown
    }
}
