/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2026 Philipp Hasper <vcs@hasper.info>
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.mediaviewer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.IntentFilter
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.nextcloud.client.account.User
import com.nextcloud.client.database.entity.SyncedFolderEntity
import com.nextcloud.client.comics.ComicPageDownloads
import com.nextcloud.client.comics.ComicProgressStore
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.editimage.EditImageActivity
import com.nextcloud.client.jobs.download.FileDownloadEventBroadcaster
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.download.FileDownloadWorker
import com.nextcloud.client.jobs.download.SendShareDownloader
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.model.WorkerState
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSerializableArgument
import com.nextcloud.utils.extensions.observeWorker
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.VirtualFolderType
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.OnFilesRemovedListener
import com.owncloud.android.ui.dialog.SendShareDialog
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.preview.FileDownloadFragment
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.io.Serializable
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Holds a swiping gallery where image and video files contained in a Nextcloud directory are shown.
 */
@Suppress("TooManyFunctions")
class MediaViewerActivity :
    FileActivity(),
    FileFragment.ContainerActivity,
    OnRemoteOperationListener,
    OnFilesRemovedListener,
    SendShareDialog.SendShareDialogDownloader,
    Injectable {
    private var viewPager: ViewPager2? = null
    private var pagerAdapter: MediaViewerPagerAdapter? = null
    private var savedPosition: Int? = null

    private val sendShareDownloader by lazy { SendShareDownloader(this, localBroadcastManager) }

    private val downloadStartReceiver = DownloadStartReceiver()
    private val downloadFinishReceiver = DownloadFinishReceiver()

    private val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowCompat.getInsetsController(window, window.decorView)
    }

    private var isDownloadWorkStarted = false
    private var screenState = MediaViewerScreenState.Idle
    private var isChromeVisible = true

    var isReaderModeEnabled = false
        private set

    private val isComicMode: Boolean get() = intent.getBooleanExtra(EXTRA_COMIC_MODE, false)
    private var comicProgress: ComicProgressStore? = null
    private var comicFolder: OCFile? = null

    private val pageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            selectPage(position)
        }

        override fun onPageScrollStateChanged(state: Int) {
            if (state == ViewPager2.SCROLL_STATE_IDLE) {
                resetZoomOfInactivePages()
            }
        }
    }

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var localBroadcastManager: LocalBroadcastManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isChromeVisible = savedInstanceState?.getBoolean(KEY_SYSTEM_VISIBLE, true) ?: true
        if (!isChromeVisible) {
            supportActionBar?.hide()
        }

        setContentView(R.layout.preview_image_activity)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setupDrawer(menuItemId)

        val chosenFile = intent.getParcelableArgument(EXTRA_FILE, OCFile::class.java)

        supportActionBar?.let {
            updateActionBarTitleAndHomeButton(chosenFile)
            viewThemeUtils.files.setWhiteBackButton(this, it)
            it.setDisplayHomeAsUpEnabled(true)
            it.setBackgroundDrawable(R.color.black.toDrawable())
        }

        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        val requestWaitingForBinder = savedInstanceState?.getBoolean(KEY_WAITING_FOR_BINDER) ?: false
        if (requestWaitingForBinder) {
            screenState = MediaViewerScreenState.WaitingForBinder
        }

        observeWorkerState()
        applyDisplayCutOutTopPadding()
        handleBackPress()

        lifecycle.addObserver(sendShareDownloader)
        sendShareDownloader.restoreState(savedInstanceState)
    }

    override fun downloadFile(file: OCFile, packageName: String, activityName: String) {
        sendShareDownloader.downloadFile(file, packageName, activityName)
    }

    override fun getMenuItemId(): Int = R.id.nav_gallery

    private fun applyDisplayCutOutTopPadding() {
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            val displayCutout = insets.displayCutout
            if (displayCutout != null) {
                val safeInsetTop = displayCutout.safeInsetTop
                val viewPager = findViewById<View>(R.id.fragmentPager)
                viewPager.setPadding(
                    viewPager.paddingLeft,
                    safeInsetTop,
                    viewPager.paddingRight,
                    viewPager.paddingBottom
                )
                viewPager.setBackgroundColor(ContextCompat.getColor(this, R.color.black))
            }

            view.onApplyWindowInsets(insets)
        }
    }

    fun toggleActionBarVisibility(hide: Boolean) {
        setChromeVisible(!hide)
    }

    /**
     * Shows or hides everything around the media at once: action bar, system bars and the reader bar.
     */
    fun setChromeVisible(visible: Boolean) {
        isChromeVisible = visible

        // the content is laid out edge to edge so that showing and hiding the bars does not move it
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val systemBars = WindowInsetsCompat.Type.systemBars()
        if (visible) {
            windowInsetsController.show(systemBars)
            supportActionBar?.show()
        } else {
            windowInsetsController.hide(systemBars)
            supportActionBar?.hide()
        }
    }

    fun setPagingEnabled(enabled: Boolean) {
        viewPager?.isUserInputEnabled = enabled
    }

    fun showNextPage() {
        moveToPage(1)
    }

    fun showPreviousPage() {
        moveToPage(-1)
    }

    private fun moveToPage(offset: Int) {
        val pager = viewPager ?: return
        val lastPosition = (pagerAdapter?.itemCount ?: 0) - 1
        pager.setCurrentItem((pager.currentItem + offset).coerceIn(0, lastPosition), true)
    }

    private fun applyReaderMode(adapter: MediaViewerPagerAdapter, isRealFolder: Boolean) {
        isReaderModeEnabled = isComicMode || ReaderMode.isEnabled(adapter.itemCount, isRealFolder)
        if (isReaderModeEnabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun initViewPager(user: User) {
        val virtualFolderType = intent.getSerializableArgument(
            PreviewImageActivity.EXTRA_VIRTUAL_TYPE,
            Serializable::class.java
        )
        val isRealFolder = virtualFolderType == null || virtualFolderType === VirtualFolderType.NONE
        if (!isRealFolder) {
            val type = virtualFolderType as VirtualFolderType

            pagerAdapter = MediaViewerPagerAdapter(
                this,
                type,
                user,
                storageManager,
                preferences
            )
        } else {
            val parentFolder = file?.let { storageManager.getFileById(it.parentId) }
                ?: storageManager.getFileByEncryptedRemotePath(OCFile.ROOT_PATH)

            pagerAdapter = MediaViewerPagerAdapter(
                this,
                parentFolder,
                user,
                storageManager,
                MainApp.isOnlyOnDevice(),
                preferences,
                isComicMode
            )
            setupComicProgress(user, parentFolder)
        }

        viewPager = findViewById(R.id.fragmentPager)

        var position = if (savedPosition !=
            null
        ) {
            savedPosition
        } else {
            file?.let { pagerAdapter?.getFilePosition(it) }
        }
        position = position?.toDouble()?.let { max(it, 0.0).toInt() }

        viewPager?.adapter = pagerAdapter
        viewPager?.offscreenPageLimit = if (isComicMode) {
            COMIC_PRELOAD_PAGES
        } else {
            ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        }
        pagerAdapter?.let { applyReaderMode(it, isRealFolder) }
        viewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        viewPager?.registerOnPageChangeCallback(pageChangeCallback)
        if (position != null) {
            viewPager?.setCurrentItem(position, false)
        }

        if (position == 0 && file?.isDown == false) {
            // this is necessary because mViewPager.setCurrentItem(0) just after setting the
            // adapter does not result in a call to #onPageSelected(0)
            screenState = MediaViewerScreenState.WaitingForBinder
        }
    }

    override fun onFilesRemoved() {
        initViewPager()
    }

    override fun onAutoUploadFolderRemoved(
        entities: List<SyncedFolderEntity>,
        filesToRemove: List<OCFile>,
        onlyLocalCopy: Boolean
    ) = Unit

    fun initViewPager() {
        if (user.isPresent) {
            initViewPager(user.get())
        }
    }

    fun updateViewPagerAfterDeletionAndAdvanceForward() {
        val deletePosition = viewPager?.currentItem ?: return
        pagerAdapter?.let { adapter ->
            val nextPosition = min(deletePosition, adapter.itemCount - 1)
            viewPager?.setCurrentItem(nextPosition, true)
            adapter.delete(deletePosition)
            // Page needs to be reselected after the adapter has been updated. Otherwise, wrong title is shown
            selectPage(nextPosition)
        }
    }

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sendRefreshSearchEventBroadcast()
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != android.R.id.home) {
            return super.onOptionsItemSelected(item)
        }

        sendRefreshSearchEventBroadcast()

        if (isDrawerOpen) {
            closeDrawer()
        } else {
            backToDisplayActivity()
        }

        return true
    }

    private fun sendRefreshSearchEventBroadcast() {
        val intent = Intent(GalleryFragment.REFRESH_SEARCH_EVENT_RECEIVER)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    public override fun onStart() {
        super.onStart()
        registerReceivers()

        val optionalUser = user
        if (optionalUser.isPresent) {
            var file: OCFile? = file ?: throw IllegalStateException("Instanced with a NULL OCFile")
            // / Validate handled file (first media item to preview)
            require(MimeTypeUtil.isImageOrVideo(file)) { "Non-image/video file passed as argument" }

            // Update file according to DB file, if it is possible
            if (file!!.fileId > FileDataStorageManager.ROOT_PARENT_ID) {
                file = storageManager.getFileById(file.fileId)
            }

            if (file != null) {
                // / Refresh the activity according to the Account and OCFile set
                setFile(file) // reset after getting it fresh from storageManager
                updateActionBarTitle(getFile()?.fileName)
                if (pagerAdapter == null) {
                    initViewPager(optionalUser.get())
                }
            } else {
                // handled file not in the current Account
                finish()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_WAITING_FOR_BINDER, screenState == MediaViewerScreenState.WaitingForBinder)
        outState.putBoolean(KEY_SYSTEM_VISIBLE, isSystemUIVisible)
        sendShareDownloader.saveState(outState)
    }

    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        super.onRemoteOperationFinish(operation, result)

        if (operation is RemoveFileOperation) {
            pagerAdapter?.let {
                if (it.itemCount <= 1) {
                    backToDisplayActivity()
                    return
                }
            }

            if (result.isSuccess) {
                updateViewPagerAfterDeletionAndAdvanceForward()
            }
        } else if (operation is SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish(result)
        }
    }

    private fun onSynchronizeFileOperationFinish(result: RemoteOperationResult<*>) {
        if (result.isSuccess) {
            supportInvalidateOptionsMenu()
        }
    }

    private fun observeWorkerState() {
        observeWorker { state: WorkerState? ->
            when (state) {
                else -> {
                    Log_OC.d(TAG, "Worker stopped")
                    isDownloadWorkStarted = false
                }
            }
        }
    }

    /**
     * Only the page of the downloaded file is refreshed. Encrypted files are shown by a [FileDownloadFragment]
     * until downloaded and need a different fragment type, so the pager is rebuilt for them.
     */
    private fun showDownloadedFile(downloadedFile: OCFile) {
        val adapter = pagerAdapter ?: return
        val position = adapter.positionOf(downloadedFile.fileId)
        if (position < 0) {
            return
        }
        adapter.updateFile(position, downloadedFile)

        when (val fragment = fragmentShowing(downloadedFile.fileId)) {
            is MediaViewerImageFragment -> fragment.showDownloadedFile(downloadedFile)
            is FileDownloadFragment -> initViewPager()
            else -> Unit
        }
    }

    private fun fragmentShowing(fileId: Long): FileFragment? = supportFragmentManager.fragments
        .filterIsInstance<FileFragment>()
        .firstOrNull { it.file?.fileId == fileId }

    private fun selectPageOnDownload() {
        screenState = MediaViewerScreenState.Idle
        Log_OC.d(
            TAG,
            "Simulating reselection of current page after connection " +
                "of download binder"
        )
        selectPage(viewPager?.currentItem)
    }

    private fun onImageDownloadComplete(downloadedFile: OCFile?) {
        dismissLoadingDialog()
        screenState = MediaViewerScreenState.Idle
        file = downloadedFile
        file?.let {
            startEditImageActivity(it)
        }
    }

    private fun registerReceivers() {
        localBroadcastManager.run {
            val downloadStartIntentFilter = IntentFilter(FileDownloadEventBroadcaster.ACTION_DOWNLOAD_ENQUEUED)
            registerReceiver(downloadStartReceiver, downloadStartIntentFilter)

            val downloadFinishIntentFilter = IntentFilter(FileDownloadEventBroadcaster.ACTION_DOWNLOAD_COMPLETED)
            registerReceiver(downloadFinishReceiver, downloadFinishIntentFilter)
        }
    }

    private fun unregisterReceivers() {
        localBroadcastManager.run {
            unregisterReceiver(downloadStartReceiver)
            unregisterReceiver(downloadFinishReceiver)
        }
    }

    public override fun onStop() {
        unregisterReceivers()
        super.onStop()
    }

    private fun backToDisplayActivity() {
        sendRefreshSearchEventBroadcast()
        finish()
    }

    @SuppressFBWarnings("DLS")
    override fun showDetails(file: OCFile) {
        val intent = Intent(this, FileDisplayActivity::class.java).apply {
            setAction(FileDisplayActivity.ACTION_DETAILS)
            putExtra(EXTRA_FILE, file)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        startActivity(intent)
        finish()
    }

    override fun showDetails(file: OCFile, activeTab: Int) {
        showDetails(file)
    }

    fun requestForDownload(file: OCFile?) {
        if (file == null) return
        val user = user.orElseThrow { RuntimeException() }
        FileDownloadHelper.instance().downloadFileIfNotStartedBefore(user, file)
    }

    /**
     * This method will be invoked when a new page becomes selected. Animation is not necessarily
     * complete.
     *
     * @param position        Position index of the new selected page
     */
    fun selectPage(position: Int?) {
        if (position == null) return
        savedPosition = position

        val currentFile = pagerAdapter?.getFileAt(position)

        if (!isDownloadWorkStarted) {
            screenState = MediaViewerScreenState.WaitingForBinder
        } else {
            if (currentFile != null) {
                if (currentFile.isEncrypted &&
                    !currentFile.isDown &&
                    pagerAdapter?.pendingErrorAt(position) == false
                ) {
                    requestForDownload(currentFile)
                }
            }
        }

        if (currentFile != null) {
            updateActionBarTitle(currentFile.fileName)
            setDrawerIndicatorEnabled(false)
        }
        updatePageCounter(position)
        saveComicProgress(position)
        preloadComicPages(position)
    }

    /**
     * Reading a comic means swiping forward page after page, so the following pages are fetched while the current
     * one is on screen. Pages already on the device are decoded by their own fragments thanks to the offscreen limit.
     */
    private fun preloadComicPages(position: Int) {
        val adapter = pagerAdapter ?: return
        if (!isComicMode) {
            return
        }
        val user = user.orElse(null) ?: return
        PagePreloadWindow.nextPositions(position, adapter.itemCount, COMIC_PRELOAD_PAGES)
            .mapNotNull { adapter.getFileAt(it) }
            .forEach { ComicPageDownloads.requestIfMissing(user, storageManager, it) }
    }

    private fun setupComicProgress(user: User, parentFolder: OCFile?) {
        if (!isComicMode || parentFolder == null) {
            return
        }
        comicFolder = parentFolder
        comicProgress = ComicProgressStore(ArbitraryDataProviderImpl(this), user.accountName)
    }

    private fun saveComicProgress(position: Int) {
        val folder = comicFolder ?: return
        comicProgress?.saveLastReadPage(folder, position)
    }

    private fun updateActionBarTitle(title: String?) {
        supportActionBar?.title = title
    }

    private fun updatePageCounter(position: Int) {
        val itemCount = pagerAdapter?.itemCount ?: 0
        supportActionBar?.subtitle = if (itemCount > 1) {
            whiteText(getString(R.string.preview_image_page_counter, position + 1, itemCount))
        } else {
            null
        }
    }

    private fun whiteText(text: String): CharSequence = SpannableString(text).apply {
        val white = ForegroundColorSpan(ContextCompat.getColor(this@MediaViewerActivity, R.color.white))
        setSpan(white, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun resetZoomOfInactivePages() {
        val currentPosition = viewPager?.currentItem ?: return
        val currentFileId = pagerAdapter?.getFileAt(currentPosition)?.fileId
        supportFragmentManager.fragments
            .filterIsInstance<MediaViewerImageFragment>()
            .filter { it.file?.fileId != currentFileId }
            .forEach { it.resetZoom() }
    }

    /**
     * Class waiting for broadcast events from the [FileDownloadWorker] service.
     *
     *
     * Updates the UI when a download is started or finished, provided that it is relevant for the
     * folder displayed in the gallery.
     */
    private inner class DownloadFinishReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log_OC.d(TAG, "Download worker stopped")
            isDownloadWorkStarted = false
            val accountName = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_ACCOUNT_NAME)
            val downloadedRemotePath = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_REMOTE_PATH)
            if (account.name != accountName || downloadedRemotePath == null) {
                return
            }
            val file = storageManager.getFileByEncryptedRemotePath(downloadedRemotePath) ?: return

            if (screenState == MediaViewerScreenState.Edit) {
                onImageDownloadComplete(file)
            } else {
                showDownloadedFile(file)
            }
        }
    }

    private inner class DownloadStartReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log_OC.d(TAG, "Download worker started")
            isDownloadWorkStarted = true

            if (screenState == MediaViewerScreenState.WaitingForBinder) {
                selectPageOnDownload()
            }
        }
    }

    val isSystemUIVisible: Boolean
        get() = isChromeVisible

    fun toggleLandscape() {
        requestedOrientation = if (isLandscapeForced) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    val isLandscapeForced: Boolean
        get() = requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

    fun toggleFullScreen() {
        setChromeVisible(!isChromeVisible)
    }

    fun startImageEditor(file: OCFile) {
        if (file.isDown) {
            startEditImageActivity(file)
        } else {
            showLoadingDialog(getString(R.string.preview_image_downloading_image_for_edit))
            screenState = MediaViewerScreenState.Edit
            requestForDownload(file)
        }
    }

    private fun startEditImageActivity(file: OCFile) {
        if (!file.isDown) {
            DisplayUtils.showSnackMessage(this, R.string.preview_image_file_is_not_downloaded)
            return
        }

        val intent = Intent(this, EditImageActivity::class.java).apply {
            putExtra(EditImageActivity.EXTRA_FILE, file)
        }
        startActivity(intent)
    }

    override fun onBrowsedDownTo(folder: OCFile) = Unit
    override fun onTransferStateChanged(file: OCFile, downloading: Boolean, uploading: Boolean) = Unit

    companion object {
        val TAG: String = MediaViewerActivity::class.java.simpleName
        const val EXTRA_COMIC_MODE = "COMIC_MODE"
        private const val COMIC_PRELOAD_PAGES = 2
        private const val KEY_WAITING_FOR_BINDER = "WAITING_FOR_BINDER"
        private const val KEY_SYSTEM_VISIBLE = "TRUE"
    }
}
