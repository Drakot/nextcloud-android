/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import com.nextcloud.client.comics.ComicShelfFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Keeps the injector bindings of the media viewer and the comic shelf out of the shared ComponentsModule, so
 * upstream changes there never collide with these screens.
 */
@Module
abstract class MediaViewerInjectionModule {
    @ContributesAndroidInjector
    abstract fun mediaViewerActivity(): MediaViewerActivity

    @ContributesAndroidInjector
    abstract fun mediaViewerImageFragment(): MediaViewerImageFragment

    @ContributesAndroidInjector
    abstract fun mediaViewerVideoFragment(): MediaViewerVideoFragment

    @ContributesAndroidInjector
    abstract fun comicShelfFragment(): ComicShelfFragment
}
