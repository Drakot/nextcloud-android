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
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.owncloud.android.databinding.ItemComicBinding
import com.owncloud.android.utils.theme.ViewThemeUtils

class ComicShelfAdapter(
    private val thumbnailGenerator: ThumbnailGenerator,
    private val viewThemeUtils: ViewThemeUtils,
    private val listener: ComicShelfListener
) : ListAdapter<ComicShelfItem, RecyclerView.ViewHolder>(ComicShelfDiffCallback) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is Comic -> TYPE_COMIC
        is ComicGroup -> TYPE_GROUP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemComicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return if (viewType == TYPE_GROUP) {
            ComicGroupViewHolder(binding, thumbnailGenerator, listener)
        } else {
            ComicViewHolder(binding, thumbnailGenerator, viewThemeUtils, listener)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Comic -> (holder as ComicViewHolder).bind(item)
            is ComicGroup -> (holder as ComicGroupViewHolder).bind(item)
        }
    }

    private object ComicShelfDiffCallback : DiffUtil.ItemCallback<ComicShelfItem>() {
        override fun areItemsTheSame(oldItem: ComicShelfItem, newItem: ComicShelfItem): Boolean =
            oldItem::class == newItem::class && oldItem.folder.remotePath == newItem.folder.remotePath

        override fun areContentsTheSame(oldItem: ComicShelfItem, newItem: ComicShelfItem): Boolean =
            oldItem.cover.remotePath == newItem.cover.remotePath &&
                oldItem.isCoverOnDevice == newItem.isCoverOnDevice &&
                sameDetails(oldItem, newItem)

        private fun sameDetails(oldItem: ComicShelfItem, newItem: ComicShelfItem): Boolean = when (oldItem) {
            is Comic ->
                newItem is Comic &&
                    oldItem.lastReadPage == newItem.lastReadPage &&
                    oldItem.pageCount == newItem.pageCount

            is ComicGroup -> newItem is ComicGroup && oldItem.comicCount == newItem.comicCount
        }
    }

    private companion object {
        const val TYPE_COMIC = 0
        const val TYPE_GROUP = 1
    }
}
