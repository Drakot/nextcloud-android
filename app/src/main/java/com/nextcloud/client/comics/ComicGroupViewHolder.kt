/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.utils.thumbnail.ThumbnailArguments
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.owncloud.android.R
import com.owncloud.android.databinding.ItemComicBinding

class ComicGroupViewHolder(
    private val binding: ItemComicBinding,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val listener: ComicShelfListener
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(group: ComicGroup) {
        binding.comicTitle.text = group.title
        binding.comicCover.setImageResource(R.drawable.file_image)
        thumbnailGenerator.setThumbnail(group.cover, binding.comicCover, ThumbnailArguments.none.copy(isGrid = true))
        binding.comicBadge.text =
            binding.root.resources.getQuantityString(R.plurals.comic_library_count, group.comicCount, group.comicCount)
        binding.comicGroupIcon.isVisible = true
        binding.comicFinished.isVisible = false
        binding.comicProgress.isInvisible = true
        binding.comicCoverCard.setOnClickListener { listener.onOpenGroup(group) }
        binding.comicCoverCard.setOnLongClickListener(null)
        binding.comicCoverCard.isLongClickable = false
    }
}
