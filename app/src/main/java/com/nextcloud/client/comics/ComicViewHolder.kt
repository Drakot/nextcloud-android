/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.utils.thumbnail.ThumbnailArguments
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.owncloud.android.R
import com.owncloud.android.databinding.ItemComicBinding
import com.owncloud.android.utils.theme.ViewThemeUtils

class ComicViewHolder(
    private val binding: ItemComicBinding,
    private val thumbnailGenerator: ThumbnailGenerator,
    viewThemeUtils: ViewThemeUtils,
    private val listener: ComicShelfListener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        viewThemeUtils.material.colorProgressBar(binding.comicProgress, ColorRole.PRIMARY)
    }

    fun bind(comic: Comic) {
        binding.comicTitle.text = comic.title
        binding.comicCover.setImageResource(R.drawable.file_image)
        thumbnailGenerator.setThumbnail(comic.cover, binding.comicCover, ThumbnailArguments.none.copy(isGrid = true))
        bindProgress(comic)
        bindClicks(comic)
    }

    private fun bindProgress(comic: Comic) {
        val resources = binding.root.resources
        binding.comicBadge.text = if (comic.isStarted) {
            resources.getString(R.string.comic_progress, comic.lastReadPage + 1, comic.pageCount)
        } else {
            resources.getQuantityString(R.plurals.comic_pages, comic.pageCount, comic.pageCount)
        }
        binding.comicGroupIcon.isVisible = false
        binding.comicFinished.isVisible = comic.isFinished
        binding.comicProgress.isInvisible = !comic.isStarted
        binding.comicProgress.max = comic.pageCount
        binding.comicProgress.progress = comic.lastReadPage + 1
    }

    private fun bindClicks(comic: Comic) {
        binding.comicCoverCard.setOnClickListener { listener.onReadComic(comic) }
        binding.comicCoverCard.setOnLongClickListener { view: View ->
            listener.onComicLongPressed(comic, view)
            true
        }
    }
}
