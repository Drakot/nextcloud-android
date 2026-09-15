/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import android.content.res.Configuration
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
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
 * Streaming-style shelf of the comics below a library folder. Covers come from the first page of every comic,
 * the play button opens the reader at the last read page and tapping the cover opens the plain folder listing.
 */
class ComicShelfFragment :
    Fragment(),
    Injectable,
    ComicShelfListener {

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var thumbnailGenerator: ThumbnailGenerator

    private var binding: FragmentComicShelfBinding? = null
    private lateinit var library: OCFile
    private lateinit var storageManager: FileDataStorageManager
    private lateinit var progressStore: ComicProgressStore
    private lateinit var adapter: ComicShelfAdapter
    private var syncWasRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        library = requireNotNull(requireArguments().getParcelableArgument(ARG_LIBRARY, OCFile::class.java))
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
        }
        requireActivity().addMenuProvider(shelfMenuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            (activity as? FileDisplayActivity)?.exitComicShelf(browseUp = true)
        }
        observeSync()
    }

    override fun onResume() {
        super.onResume()
        loadComics()
        ComicLibrarySync.start(requireContext(), accountManager.user, library, ignoreEtags = false)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        (binding?.comicShelfList?.layoutManager as? GridLayoutManager)?.spanCount = columnCount()
    }

    override fun onDestroyView() {
        binding = null
        supportActionBar()?.subtitle = null
        super.onDestroyView()
    }

    private fun supportActionBar(): ActionBar? = (activity as? AppCompatActivity)?.supportActionBar

    private fun columnCount(): Int = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        LANDSCAPE_COLUMNS
    } else {
        PORTRAIT_COLUMNS
    }

    private fun observeSync() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ComicLibrarySync.state.collect { renderSyncState(it) }
            }
        }
    }

    private fun renderSyncState(state: ComicSyncState) {
        val binding = binding ?: return
        val running = state.isRunningFor(library.remotePath)
        binding.comicSyncContainer.isVisible = running
        if (state is ComicSyncState.Running && running) {
            binding.comicSyncText.text =
                resources.getQuantityString(R.plurals.comic_library_syncing, state.foldersChecked, state.foldersChecked)
        }
        if (syncWasRunning && state is ComicSyncState.Finished && state.libraryPath == library.remotePath) {
            loadComics()
            if (state.failedFolders > 0) {
                val failed = state.failedFolders
                showSnackbar(resources.getQuantityString(R.plurals.comic_library_sync_failed, failed, failed))
            }
        }
        syncWasRunning = running
    }

    private fun loadComics() {
        viewLifecycleOwner.lifecycleScope.launch {
            val comics = withContext(Dispatchers.IO) {
                ComicShelfBuilder(
                    folderContent = { storageManager.getFolderContent(it, false) },
                    lastReadPage = progressStore::lastReadPage
                ).build(library)
            }
            adapter.submitList(comics)
            binding?.comicShelfEmpty?.isVisible = comics.isEmpty()
            updateSubtitle(comics.size)
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

    override fun onOpenComicFolder(comic: Comic) {
        (activity as? FileDisplayActivity)?.openComicFolder(comic.folder, library)
    }

    override fun onComicLongPressed(comic: Comic, anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add(Menu.NONE, MENU_START_OVER, Menu.NONE, R.string.comic_start_over)
            menu.add(Menu.NONE, MENU_MARK_AS_READ, Menu.NONE, R.string.comic_mark_as_read)
            menu.add(Menu.NONE, MENU_OPEN_FOLDER, Menu.NONE, R.string.comic_open_folder)
            setOnMenuItemClickListener { item -> onComicMenuItem(item.itemId, comic) }
        }.show()
    }

    private fun onComicMenuItem(itemId: Int, comic: Comic): Boolean {
        when (itemId) {
            MENU_START_OVER -> {
                progressStore.reset(comic.folder)
                ComicReaderLauncher.open(requireContext(), comic, fromStart = true)
            }

            MENU_MARK_AS_READ -> {
                progressStore.saveLastReadPage(comic.folder, comic.pageCount - 1)
                loadComics()
            }

            MENU_OPEN_FOLDER -> onOpenComicFolder(comic)

            else -> return false
        }
        return true
    }

    private val shelfMenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.comic_shelf, menu)
            menu.findItem(R.id.action_comic_sync)?.let {
                viewThemeUtils.platform.colorToolbarMenuIcon(requireContext(), it)
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            if (menuItem.itemId != R.id.action_comic_sync) {
                return false
            }
            ComicLibrarySync.start(requireContext(), accountManager.user, library, ignoreEtags = true)
            return true
        }
    }

    companion object {
        private const val ARG_LIBRARY = "library"
        private const val PORTRAIT_COLUMNS = 3
        private const val LANDSCAPE_COLUMNS = 5
        private const val MENU_START_OVER = 1
        private const val MENU_MARK_AS_READ = 2
        private const val MENU_OPEN_FOLDER = 3

        fun newInstance(library: OCFile): ComicShelfFragment = ComicShelfFragment().apply {
            arguments = bundleOf(ARG_LIBRARY to library)
        }
    }
}
