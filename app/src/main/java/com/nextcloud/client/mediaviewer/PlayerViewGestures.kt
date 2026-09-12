/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.core.view.children
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
fun PlayerView.toggleController() {
    if (isControllerFullyVisible) {
        hideController()
    } else {
        showController()
    }
}

/**
 * Installs the tap and double tap handling on top of the video surface. The overlay lives in the PlayerView's own
 * overlay frame, so the playback controls drawn above it keep receiving their clicks. Automatic pop-up of the
 * controls is switched off: media3 would otherwise show them on every buffering pause, e.g. right after a seek,
 * and the hosts tie their toolbar to the controls' visibility.
 */
@OptIn(UnstableApi::class)
fun PlayerView.attachGestureOverlay() {
    controllerAutoShow = false

    val container = overlayFrameLayout ?: return
    if (container.children.any { it is PlayerGestureOverlay }) {
        return
    }
    container.addView(
        PlayerGestureOverlay(this),
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
}
