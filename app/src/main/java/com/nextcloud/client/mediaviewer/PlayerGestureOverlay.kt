/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.view.GestureDetectorCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.owncloud.android.R
import com.owncloud.android.databinding.ViewPlayerGestureOverlayBinding
import java.util.concurrent.TimeUnit

/**
 * Transparent layer on top of the video surface that turns taps into player actions: a single tap toggles the
 * controls, a double tap in the middle toggles playback, and a double tap on the left or right starts a seek
 * burst. Every further tap on the same side adds one more step while the indicator shows the total; the player
 * only jumps once the burst ends. Seeking never brings the controls up; it only keeps them up when already shown.
 */
@SuppressLint("ViewConstructor")
@OptIn(UnstableApi::class)
class PlayerGestureOverlay(private val playerView: PlayerView) : FrameLayout(playerView.context) {

    private val binding = ViewPlayerGestureOverlayBinding.inflate(LayoutInflater.from(context), this)
    private val seekAccumulator = SeekAccumulator()
    private val commitSeek = Runnable { commitPendingSeek() }

    private val gestureDetector = GestureDetectorCompat(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (!seekAccumulator.isActive) {
                    return false
                }
                addSeekStep(PlayerGestureZone.from(e.x, width))
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!seekAccumulator.isActive) {
                    performClick()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                handleDoubleTap(PlayerGestureZone.from(e.x, width))
                return true
            }
        }
    )

    init {
        isClickable = true
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = gestureDetector.onTouchEvent(event)

    override fun performClick(): Boolean {
        super.performClick()
        playerView.toggleController()
        return true
    }

    override fun onDetachedFromWindow() {
        commitPendingSeek()
        super.onDetachedFromWindow()
    }

    private fun handleDoubleTap(zone: PlayerGestureZone) {
        if (zone == PlayerGestureZone.PLAY_PAUSE) {
            if (!seekAccumulator.isActive) {
                playerView.player?.let(::togglePlayback)
            }
            return
        }
        addSeekStep(zone)
    }

    private fun togglePlayback(player: Player) {
        if (player.isPlaying) {
            player.pause()
            playerView.showController()
        } else {
            player.play()
        }
    }

    private fun addSeekStep(zone: PlayerGestureZone) {
        if (zone == PlayerGestureZone.PLAY_PAUSE) {
            return
        }
        val player = playerView.player ?: return
        val stepMs = if (zone == PlayerGestureZone.SEEK_BACKWARD) {
            player.seekBackIncrement
        } else {
            player.seekForwardIncrement
        }

        if (!seekAccumulator.add(zone, stepMs)) {
            commitPendingSeek()
            seekAccumulator.add(zone, stepMs)
        }
        showPendingSeek()
        removeCallbacks(commitSeek)
        postDelayed(commitSeek, SEEK_COMMIT_DELAY_MS)
    }

    private fun showPendingSeek() {
        val isBackward = seekAccumulator.zone == PlayerGestureZone.SEEK_BACKWARD
        val indicator = if (isBackward) binding.seekBackwardIndicator else binding.seekForwardIndicator
        val textRes = if (isBackward) {
            R.string.media_player_seek_backward_seconds
        } else {
            R.string.media_player_seek_forward_seconds
        }

        indicator.text = context.getString(textRes, TimeUnit.MILLISECONDS.toSeconds(seekAccumulator.pendingMs))
        indicator.animate().cancel()
        indicator.alpha = 1f
        indicator.visibility = View.VISIBLE

        if (playerView.isControllerFullyVisible) {
            playerView.showController()
        }
    }

    private fun commitPendingSeek() {
        removeCallbacks(commitSeek)
        val player = playerView.player
        if (player != null && seekAccumulator.isActive) {
            player.seekTo(seekAccumulator.targetPosition(player.currentPosition, player.duration))
        }
        seekAccumulator.reset()
        fadeOut(binding.seekBackwardIndicator)
        fadeOut(binding.seekForwardIndicator)
    }

    private fun fadeOut(indicator: TextView) {
        if (indicator.visibility != View.VISIBLE) {
            return
        }
        indicator.animate()
            .alpha(0f)
            .setStartDelay(INDICATOR_HOLD_MS)
            .setDuration(INDICATOR_FADE_MS)
            .withEndAction { indicator.visibility = View.GONE }
            .start()
    }

    companion object {
        private const val SEEK_COMMIT_DELAY_MS = 600L
        private const val INDICATOR_HOLD_MS = 350L
        private const val INDICATOR_FADE_MS = 250L
    }
}
