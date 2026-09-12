/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.MainApp

/**
 * Same wiring as the stock player factory but with the seek steps used by the gesture overlay.
 */
object MediaViewerExoPlayer {
    private const val SEEK_INCREMENT_MS = 10_000L

    @OptIn(UnstableApi::class)
    fun create(context: Context, nextcloudClient: NextcloudClient): ExoPlayer {
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(nextcloudClient.client)
        okHttpDataSourceFactory.setUserAgent(MainApp.getUserAgent())
        val mediaSourceFactory = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(context, okHttpDataSourceFactory)
        )
        return ExoPlayer
            .Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
            .build()
    }
}
