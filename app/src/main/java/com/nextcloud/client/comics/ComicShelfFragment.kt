/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.download.FileDownloadEventBroadcaster
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentComicShelfBinding
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Streaming-style shelf of the comics below a library folder. Covers come from the first page of every comic and
 * tapping one opens the reader at the last read page. With the folder structure kept, [folder] is the level shown
 * and its groups open a shelf of their own.
 */
class ComicShelfFragment :
    Fragment(),
    Injectable,
    ComicShelfListener,
    ComicContextMenu.Host {

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var thumbnailGenerator: ThumbnailGenerator

    private var binding: FragmentComicShelfBinding? = null
    private lateinit var library: OCFile
    private lateinit var folder: OCFile
    private lateinit var storageManager: FileDataStorageManager
    private lateinit var progressStore: ComicProgressStore
    private lateinit var adapter: ComicShelfAdapter
    private var syncBanner: ComicSyncBanner? = null

    val folderPath: String get() = folder.remotePath

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        library = requireNotNull(requireArguments().getParcelableArgument(ARG_LIBRARY, OCFile::class.java))
        folder = requireArguments().getParcelableArgument(ARG_FOLDER, OCFile::class.java) ?: library
        val user = accountManager.user
        storageManager = FileDataStorageManager(user, requireContext().contentResolver)
        progressStore = ComicProgressStore(ArbitraryDataProviderImpl(requireContext()), user.accountName)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentComicShelfBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ComicShelfAdapter(thumbnailGenerator, viewThemeUtils, this)
        binding?.run {
            comicShelfList.layoutManager = GridLayoutManager(requireContext(), columnCount())
            comicShelfList.adapter = adapter
            viewThemeUtils.material.colorProgressBar(comicSyncProgress, ColorRole.PRIMARY)
            syncBanner = ComicSyncBanner(this, library.remotePath, ::onSyncFinished)
        }
        val menuProvider = ComicShelfMenuProvider(
            context = requireContext(),
            viewThemeUtils = viewThemeUtils,
            keepsFolders = { layout() == ComicShelfLayout.FOLDERS },
            onSync = { ComicLibrarySync.start(requireContext(), accountManager.user, library, ignoreEtags = true) },
            onToggleKeepFolders = ::toggleLayout
        )
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { navigateBack() }
        observeSync()
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            coverDownloadedReceiver,
            IntentFilter(FileDownloadEventBroadcaster.ACTION_DOWNLOAD_COMPLETED)
        )
    }

    override fun onResume() {
        super.onResume()
        supportActionBar()?.title = folder.fileName
        loadComics()
        ComicLibrarySync.start(requireContext(), accountManager.user, library, ignoreEtags = false)
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(coverDownloadedReceiver)
        super.onStop()
    }

    private val coverDownloadedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val remotePath = intent?.getStringExtra(FileDownloadEventBroadcaster.EXTRA_REMOTE_PATH) ?: return
            if (remotePath.startsWith(library.remotePath)) {
                loadComics()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        (binding?.comicShelfList?.layoutManager as? GridLayoutManager)?.spanCount = columnCount()
    }

    override fun onDestroyView() {
        binding = null
        syncBanner = null
        supportActionBar()?.subtitle = null
        super.onDestroyView()
    }

    private fun supportActionBar(): ActionBar? = (activity as? AppCompatActivity)?.supportActionBar

    private fun columnCount(): Int = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        LANDSCAPE_COLUMNS
    } else {
        PORTRAIT_COLUMNS
    }

    private fun layout(): ComicShelfLayout = ComicLibraryPreferences.layoutOf(requireContext(), library)

    private fun toggleLayout() {
        val next = if (layout() == ComicShelfLayout.FOLDERS) ComicShelfLayout.FLAT else ComicShelfLayout.FOLDERS
        ComicLibraryPreferences.setLayout(requireContext(), library, next)
        requireActivity().invalidateMenu()
        if (next == ComicShelfLayout.FLAT && folder.remotePath != library.remotePath) {
            (activity as? FileDisplayActivity)?.showComicShelf(library)
            return
        }
        loadComics()
    }

    private fun navigateBack() {
        val activity = activity as? FileDisplayActivity ?: return
        if (folder.remotePath == library.remotePath) {
            activity.exitComicShelf(browseUp = true)
            return
        }
        val parent = storageManager.getFileById(folder.parentId) ?: library
        activity.showComicShelf(library, parent)
    }

    private fun observeSync() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ComicLibrarySync.state.collect { syncBanner?.render(it) }
            }
        }
    }

    private fun onSyncFinished(failedFolders: Int) {
        loadComics()
        if (failedFolders > 0) {
            showSnackbar(resources.getQuantityString(R.plurals.comic_library_sync_failed, failedFolders, failedFolders))
        }
    }

    private fun loadComics() {
        viewLifecycleOwner.lifecycleScope.launch {
            val keepFolders = layout() == ComicShelfLayout.FOLDERS
            val items = withContext(Dispatchers.IO) {
                val builder = ComicShelfBuilder(
                    folderContent = { storageManager.getFolderContent(it, false) },
                    lastReadPage = progressStore::lastReadPage
                )
                if (keepFolders) builder.buildLevel(folder) else builder.build(library)
            }
            adapter.submitList(items)
            binding?.comicShelfEmpty?.isVisible = items.isEmpty()
            updateSubtitle(items.sumOf { if (it is ComicGroup) it.comicCount else 1 })
            ComicCoverDownloads.requestMissing(accountManager.user, storageManager, items)
        }
    }

    private fun updateSubtitle(count: Int) {
        val actionBar = supportActionBar() ?: return
        val subtitle = SpannableString(resources.getQuantityString(R.plurals.comic_library_count, count, count))
        val color = ContextCompat.getColor(requireContext(), R.color.fontAppbar)
        subtitle.setSpan(ForegroundColorSpan(color), 0, subtitle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        actionBar.subtitle = subtitle
    }

    private fun showSnackbar(message: String) {
        val view = binding?.root ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).also { viewThemeUtils.material.themeSnackbar(it) }.show()
    }

    override fun onReadComic(comic: Comic) {
        ComicReaderLauncher.open(requireContext(), comic)
    }

    override fun onOpenGroup(group: ComicGroup) {
        (activity as? FileDisplayActivity)?.showComicShelf(library, group.folder)
    }

    override fun onComicLongPressed(comic: Comic, anchor: View) {
        ComicContextMenu(this, this, library, storageManager, progressStore).show(comic, anchor)
    }

    override fun onComicProgressChanged() = loadComics()

    override fun showComicMessage(message: String) = showSnackbar(message)

    companion object {
        private const val ARG_LIBRARY = "library"
        private const val ARG_FOLDER = "folder"
        private const val PORTRAIT_COLUMNS = 3
        private const val LANDSCAPE_COLUMNS = 5

        fun newInstance(library: OCFile, folder: OCFile = library): ComicShelfFragment = ComicShelfFragment().apply {
            arguments = bundleOf(ARG_LIBRARY to library, ARG_FOLDER to folder)
        }
    }
}
