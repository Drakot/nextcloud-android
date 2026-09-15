/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import android.view.View
import androidx.annotation.OptIn
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
fun PlayerView.applyControlsInsets(insets: Insets) {
    val controller = findViewById<View>(androidx.media3.ui.R.id.exo_controller) ?: return
    controller.updatePadding(left = insets.left, right = insets.right, bottom = insets.bottom)
}

/**
 * PlayerControlView only applies fullscreen_enter_icon/fullscreen_exit_icon when the state actually changes, so the
 * state is flipped first to force the configured drawable onto the button.
 */
@OptIn(UnstableApi::class)
fun PlayerView.setFullscreenButton(isFullscreen: Boolean, onClick: () -> Unit) {
    setFullscreenButtonClickListener(null)
    setFullscreenButtonState(!isFullscreen)
    setFullscreenButtonState(isFullscreen)
    setFullscreenButtonClickListener { onClick() }
}
