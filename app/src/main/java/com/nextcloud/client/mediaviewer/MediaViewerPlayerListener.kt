/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.mediaviewer

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.media.ErrorFormat
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC

@OptIn(UnstableApi::class)
class MediaViewerPlayerListener(
    private val context: Context,
    private val playerView: PlayerView,
    private val exoPlayer: ExoPlayer,
    private val onCompleted: () -> Unit = { }
) : Player.Listener {

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        if (playbackState == Player.STATE_ENDED) {
            onCompletion()
            playerView.showController()
            onCompleted()
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)
        playerView.controllerShowTimeoutMs = if (playWhenReady) {
            PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS
        } else {
            SHOW_CONTROLS_INDEFINITELY
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        Log_OC.d(TAG, "Exoplayer keep screen on: $isPlaying")
        playerView.keepScreenOn = isPlaying
    }

    private fun onCompletion() {
        exoPlayer.let {
            it.seekToDefaultPosition()
            it.pause()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Log_OC.e(TAG, "Exoplayer error", error)
        val message = ErrorFormat.toString(context, error)
        MaterialAlertDialogBuilder(context)
            .setMessage(message)
            .setPositiveButton(R.string.common_ok) { _: DialogInterface?, _: Int ->
                onCompletion()
            }
            .setCancelable(false)
            .show()
    }

    companion object {
        private const val TAG = "MediaViewerPlayerListener"
        private const val SHOW_CONTROLS_INDEFINITELY = 0
    }
}
