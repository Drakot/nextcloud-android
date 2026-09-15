/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2023 Parneet Singh <gurayaparneet@gmail.com>
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.nextcloud.client.mediaviewer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.download.FileDownloadHelper.Companion.instance
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.fileactions.FileAction
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.Companion.newInstance
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getTypedActivity
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentMediaViewerVideoBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.StreamMediaFileOperation
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * This fragment shows a preview of a downloaded media file (audio or video).
 *
 * Trying to get an instance with NULL [OCFile] or ownCloud [User] values will produce an
 * [IllegalStateException].
 *
 * By now, if the [OCFile] passed is not downloaded, an [IllegalStateException] is generated on
 * instantiation too.
 *
 * Creates an empty fragment for previews.
 *
 * MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically (for instance, when the
 * device is turned a aside).
 *
 * DO NOT CALL IT: an [OCFile] and [User] must be provided for a successful construction
 */
@Suppress("NestedBlockDepth", "ComplexMethod", "LongMethod", "TooManyFunctions", "ReturnCount")
class MediaViewerVideoFragment :
    FileFragment(),
    Injectable {
    private var user: User? = null
    private var savedPlaybackPosition: Long = 0

    private var autoplay = true
    private var isLivePhoto = false
    private val prepared = false

    private var videoUri: Uri? = null

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    lateinit var binding: FragmentMediaViewerVideoBinding

    private val exoplayerView: PlayerView
        get() = binding.exoplayerView

    private var emptyListView: ViewGroup? = null
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var screenMode = PlayerScreenMode.Normal

    private val hostActivity: MediaViewerActivity?
        get() = activity as? MediaViewerActivity

    private val exitFullscreenOnBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            setScreenMode(PlayerScreenMode.Normal)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            initArguments(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        Log_OC.v(TAG, "onCreateView")

        binding = FragmentMediaViewerVideoBinding.inflate(inflater, container, false)
        emptyListView = binding.emptyView.emptyListView
        setLoadingView()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log_OC.v(TAG, "onActivityCreated")

        checkArgumentsAfterViewCreation(savedInstanceState)

        toggleDrawerLockMode(containerActivity, DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, exitFullscreenOnBack)
        addMenuHost()
    }

    private fun checkArgumentsAfterViewCreation(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            checkNotNull(file) { "Instanced with a NULL OCFile" }
            checkNotNull(user) { "Instanced with a NULL ownCloud Account" }
        } else {
            file = savedInstanceState.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
            user = savedInstanceState.getParcelableArgument(EXTRA_USER, User::class.java)
            savedPlaybackPosition = savedInstanceState.getInt(EXTRA_PLAY_POSITION).toLong()
            autoplay = savedInstanceState.getBoolean(EXTRA_PLAYING)
            screenMode = savedInstanceState.getString(EXTRA_SCREEN_MODE)
                ?.let(PlayerScreenMode::valueOf) ?: PlayerScreenMode.Normal
        }
    }

    private fun initArguments(bundle: Bundle) {
        file = bundle.getParcelableArgument(FILE, OCFile::class.java)
        user = bundle.getParcelableArgument(USER, User::class.java)

        savedPlaybackPosition = bundle.getLong(PLAYBACK_POSITION)
        autoplay = bundle.getBoolean(AUTOPLAY)
        isLivePhoto = bundle.getBoolean(IS_LIVE_PHOTO)
    }

    override fun onResume() {
        super.onResume()
        applyWindowInsets()
        prepareMedia()
    }

    @OptIn(UnstableApi::class)
    private fun applyWindowInsets() {
        binding.root.post {
            val rootInsets = ViewCompat.getRootWindowInsets(binding.root) ?: return@post
            exoplayerView.applyControlsInsets(
                rootInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
            )
        }
    }

    private fun setLoadingView() {
        binding.progress.visibility = View.VISIBLE
        binding.emptyView.emptyListView.visibility = View.GONE
    }

    private fun setVideoErrorMessage(
        headline: String,
        @StringRes message: Int = R.string.media_viewer_stream_not_possible_message
    ) {
        binding.emptyView.run {
            emptyListViewHeadline.text = headline
            emptyListViewText.setText(message)
            emptyListIcon.setImageResource(R.drawable.file_movie)
            emptyListViewText.visibility = View.VISIBLE
            emptyListIcon.visibility = View.VISIBLE
            emptyListView.visibility = View.VISIBLE
        }

        binding.progress.visibility = View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        toggleDrawerLockMode(containerActivity, DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        outState.run {
            putParcelable(EXTRA_FILE, file)
            putParcelable(EXTRA_USER, user)

            savedPlaybackPosition = exoPlayer?.currentPosition ?: savedPlaybackPosition
            autoplay = exoPlayer?.isPlaying ?: autoplay
            putLong(EXTRA_PLAY_POSITION, savedPlaybackPosition)
            putBoolean(EXTRA_PLAYING, autoplay)
            putString(EXTRA_SCREEN_MODE, screenMode.name)
        }
    }

    private fun prepareMedia() {
        if (file == null || !isAdded) {
            Log_OC.d(TAG, "File is null or fragment not attached to a context.")
            return
        }
        prepareForVideo()
    }

    @Suppress("DEPRECATION", "TooGenericExceptionCaught")
    private fun prepareForVideo() {
        if (exoPlayer != null) {
            playVideo()
            return
        }

        lifecycleScope.launch {
            try {
                val client = withContext(Dispatchers.IO) {
                    clientFactory.createNextcloudClient(accountManager.user)
                }
                val ctx = this@MediaViewerVideoFragment.context ?: return@launch

                withContext(Dispatchers.Main) {
                    createExoPlayer(ctx, client)
                    playVideo()
                }
            } catch (e: CreationException) {
                Log_OC.e(TAG, "error setting up ExoPlayer", e)
            }
        }
    }

    private fun createExoPlayer(context: Context, client: NextcloudClient) {
        exoPlayer = MediaViewerExoPlayer.create(context, client)
        exoPlayer?.let {
            val listener = MediaViewerPlayerListener(context, exoplayerView, it) { goBackToLivePhoto() }
            it.addListener(listener)
        }
        mediaSession = MediaSession.Builder(
            context,
            exoPlayer as Player
        ).setId(System.currentTimeMillis().toString()).build()
    }

    private fun releaseVideoPlayer() {
        exoPlayer?.let {
            savedPlaybackPosition = it.currentPosition
            autoplay = it.playWhenReady
            it.release()
            mediaSession?.release()
        }
        mediaSession = null
        exoPlayer = null
    }

    private fun goBackToLivePhoto() {
        if (!isLivePhoto) {
            return
        }

        showActionBar()
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun showActionBar() {
        hostActivity?.setChromeVisible(true)
    }

    @OptIn(UnstableApi::class)
    private fun setupVideoView() {
        exoplayerView.run {
            setShowNextButton(false)
            setShowPreviousButton(false)
            setControllerVisibilityListener(
                PlayerView.ControllerVisibilityListener { visibility ->
                    binding.rotateScreenButton.isVisible = screenMode.isFullscreen && visibility == View.VISIBLE
                    if (screenMode.isFullscreen) {
                        return@ControllerVisibilityListener
                    }
                    hostActivity?.setChromeVisible(isControllerFullyVisible)
                }
            )
            player = exoPlayer
            attachGestureOverlay()
            showController()
        }
        binding.rotateScreenButton.setOnClickListener { hostActivity?.toggleLandscape() }
        applyScreenMode()
    }

    @OptIn(UnstableApi::class)
    private fun setScreenMode(mode: PlayerScreenMode) {
        screenMode = mode
        applyScreenMode()
        if (!mode.isFullscreen) {
            hostActivity?.setChromeVisible(true)
            exoplayerView.showController()
        }
    }

    /**
     * Fullscreen is done in place: the surrounding chrome disappears and the pager stops reacting to swipes, so the
     * video never has to be moved to another surface.
     */
    @OptIn(UnstableApi::class)
    private fun applyScreenMode() {
        val isFullscreen = screenMode.isFullscreen
        exitFullscreenOnBack.isEnabled = isFullscreen
        exoplayerView.setFullscreenButton(isFullscreen) { setScreenMode(screenMode.toggled()) }
        hostActivity?.setPagingEnabled(!isFullscreen)
        binding.rotateScreenButton.isVisible = isFullscreen && isControllerShown()
        if (isFullscreen) {
            hostActivity?.setChromeVisible(false)
        }
    }

    /**
     * media3 animates its controls in two stages, so [PlayerView.isControllerFullyVisible] is false while the bars
     * are still sliding in. The plain view visibility is enough to decide whether the rotate button belongs on screen.
     */
    private fun isControllerShown(): Boolean =
        exoplayerView.findViewById<View>(androidx.media3.ui.R.id.exo_controller)?.isVisible == true

    private fun addMenuHost() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menu.removeItem(R.id.action_search)
                    menuInflater.inflate(R.menu.media_viewer_video, menu)
                    menuInflater.inflate(R.menu.custom_menu_placeholder, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_rotate_screen -> {
                            hostActivity?.toggleLandscape()
                            true
                        }

                        R.id.custom_menu_placeholder_item -> {
                            if (containerActivity.storageManager == null || file == null) return false

                            val updatedFile = containerActivity.storageManager.getFileById(file.fileId)
                            file = updatedFile
                            file?.let { newFile ->
                                showFileActions(newFile)
                            }

                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun showFileActions(file: OCFile) {
        val additionalFilter = FileAction.getFilePreviewActions(getFile())
        newInstance(file, false, additionalFilter)
            .setResultListener(childFragmentManager, this) { itemId: Int -> this.onFileActionChosen(itemId) }
            .show(childFragmentManager, "actions")
    }

    private fun onFileActionChosen(itemId: Int) {
        when (itemId) {
            R.id.action_send_share_file -> {
                sendShareFile()
            }

            R.id.action_open_file_with -> {
                openFile()
            }

            R.id.action_remove_file -> {
                val dialog = RemoveFilesDialogFragment.newInstance(file)
                dialog.show(requireFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION)
            }

            R.id.action_see_details -> {
                seeDetails()
            }

            R.id.action_sync_file -> {
                getTypedActivity(FileActivity::class.java)?.showSyncLoadingDialog(file.isFolder)
                containerActivity.fileOperationsHelper.syncFileOrFolder(file)
            }

            R.id.action_cancel_sync -> {
                containerActivity.fileOperationsHelper.cancelTransference(file)
            }

            R.id.action_stream_media -> {
                containerActivity.fileOperationsHelper.streamMediaFile(file)
            }

            R.id.action_export_file -> {
                val list = ArrayList<OCFile>()
                list.add(file)
                containerActivity.fileOperationsHelper.exportFiles(
                    list,
                    context,
                    view,
                    backgroundJobManager
                )
            }

            R.id.action_download_file -> {
                instance().downloadFileIfNotStartedBefore(user!!, file)
            }
        }
    }

    /**
     * Update the file of the fragment with file value
     *
     * @param file Replaces the held file with a new one
     */
    fun updateFile(file: OCFile?) {
        setFile(file)
    }

    private fun seeDetails() {
        releaseVideoPlayer()
        containerActivity.showDetails(file)
    }

    private fun sendShareFile() {
        releaseVideoPlayer()
        containerActivity.fileOperationsHelper.sendShareFile(file)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun playVideo() {
        setupVideoView()
        if (file.isDown) {
            playVideoUri(file.storageUri)
            return
        }

        lifecycleScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) {
                    loadStreamUrl(user, clientFactory, file.localId)
                }
                if (uri != null) {
                    videoUri = uri
                    playVideoUri(uri)
                } else {
                    emptyListView?.visibility = View.VISIBLE
                    setVideoErrorMessage(getString(R.string.stream_not_possible_headline))
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, "Loading stream url not possible: $e")
            }
        }
    }

    private fun loadStreamUrl(user: User?, clientFactory: ClientFactory?, fileId: Long): Uri? {
        val client: OwnCloudClient? = try {
            clientFactory?.create(user)
        } catch (e: CreationException) {
            Log_OC.e(TAG, "Loading stream url not possible: $e")
            return null
        }

        val sfo = StreamMediaFileOperation(fileId)
        val result = sfo.execute(client)

        if (result?.isSuccess == false) {
            return null
        }

        return (result?.data?.get(0) as String).toUri()
    }

    private fun playVideoUri(uri: Uri) {
        binding.progress.visibility = View.GONE

        exoPlayer?.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer?.playWhenReady = autoplay
        exoPlayer?.prepare()

        if (savedPlaybackPosition >= 0) {
            exoPlayer?.seekTo(savedPlaybackPosition)
        }

        // only autoplay video once
        autoplay = false
    }

    override fun onPause() {
        releaseVideoPlayer()
        hostActivity?.setPagingEnabled(true)
        super.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log_OC.v(TAG, "onConfigurationChanged $this")
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log_OC.v(TAG, "onActivityResult $this")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            savedPlaybackPosition = data?.getLongExtra(EXTRA_START_POSITION, 0) ?: 0L
            autoplay = data?.getBooleanExtra(EXTRA_AUTOPLAY, false) ?: false
        }
    }

    /**
     * Opens the previewed file with an external application.
     */
    private fun openFile() {
        containerActivity.fileOperationsHelper.openFile(file)
    }

    val position: Long
        get() {
            if (prepared) {
                savedPlaybackPosition = exoPlayer?.currentPosition ?: 0
            }
            Log_OC.v(TAG, "getting position: $savedPlaybackPosition")
            return savedPlaybackPosition
        }

    private fun toggleDrawerLockMode(containerActivity: ContainerActivity, lockMode: Int) {
        (containerActivity as DrawerActivity).setDrawerLockMode(lockMode)
    }

    override fun onDetach() {
        exoPlayer?.let {
            it.stop()
            it.release()
        }

        super.onDetach()
    }

    companion object {
        private val TAG: String = MediaViewerVideoFragment::class.java.simpleName

        const val EXTRA_FILE: String = "FILE"
        const val EXTRA_USER: String = "USER"
        const val EXTRA_AUTOPLAY: String = "AUTOPLAY"
        const val EXTRA_START_POSITION: String = "START_POSITION"

        private const val EXTRA_PLAY_POSITION = "PLAY_POSITION"
        private const val EXTRA_PLAYING = "PLAYING"
        private const val EXTRA_SCREEN_MODE = "SCREEN_MODE"

        private const val FILE = "FILE"
        private const val USER = "USER"
        private const val PLAYBACK_POSITION = "PLAYBACK_POSITION"
        private const val AUTOPLAY = "AUTOPLAY"
        private const val IS_LIVE_PHOTO = "IS_LIVE_PHOTO"

        fun newInstance(
            fileToDetail: OCFile?,
            user: User?,
            startPlaybackPosition: Long = 0,
            autoplay: Boolean = false,
            isLivePhoto: Boolean = false
        ): MediaViewerVideoFragment = MediaViewerVideoFragment().apply {
            arguments = Bundle().apply {
                putParcelable(FILE, fileToDetail)
                putParcelable(USER, user)
                putLong(PLAYBACK_POSITION, startPlaybackPosition)
                putBoolean(AUTOPLAY, autoplay)
                putBoolean(IS_LIVE_PHOTO, isLivePhoto)
            }
        }

        fun isAudioOrVideo(file: OCFile?): Boolean =
            file != null && (MimeTypeUtil.isAudio(file) || MimeTypeUtil.isVideo(file))
    }
}
