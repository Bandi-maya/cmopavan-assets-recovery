package com.logybyte.assetsrecovery.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.logybyte.assetsrecovery.R
import com.logybyte.assetsrecovery.data.model.FilterType
import com.logybyte.assetsrecovery.data.model.MediaItem
import com.logybyte.assetsrecovery.data.model.MediaType
import com.logybyte.assetsrecovery.data.model.SortOrder
import com.logybyte.assetsrecovery.databinding.FragmentGalleryBinding
import com.logybyte.assetsrecovery.ui.MainActivity
import com.logybyte.assetsrecovery.ui.adapter.MediaGridAdapter
import com.logybyte.assetsrecovery.ui.dialog.MediaDetailDialog
import com.logybyte.assetsrecovery.ui.viewmodel.RecoveryViewModel
import kotlinx.coroutines.launch

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecoveryViewModel by activityViewModels()
    private lateinit var adapter: MediaGridAdapter

    private var mediaType: MediaType? = null
    private var onlyRecovered: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val typeStr = arguments?.getString(ARG_MEDIA_TYPE)
        mediaType = if (typeStr != null) MediaType.valueOf(typeStr) else null
        onlyRecovered = arguments?.getBoolean(ARG_ONLY_RECOVERED, false) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilters()
        setupActions()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = MediaGridAdapter(
            onItemClick = { item ->
                showMediaDetail(item)
            },
            onSelectionToggle = { item ->
                viewModel.toggleSelection(item.uri)
            }
        )

        val spanCount = if (resources.configuration.screenWidthDp >= 600) 4 else 3
        binding.rvMedia.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.rvMedia.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.startScan()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupFilters() {
        if (onlyRecovered) {
            binding.scrollFilters.visibility = View.GONE
        } else {
            binding.scrollFilters.visibility = View.VISIBLE
            binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
                val filter = when {
                    checkedIds.contains(R.id.chip_filter_recoverable) -> FilterType.RECOVERABLE_ONLY
                    checkedIds.contains(R.id.chip_filter_trashed) -> FilterType.TRASHED
                    checkedIds.contains(R.id.chip_filter_hidden) -> FilterType.HIDDEN
                    checkedIds.contains(R.id.chip_filter_active) -> FilterType.ACTIVE
                    else -> FilterType.ALL
                }
                viewModel.setFilter(filter)
            }
        }
    }

    private fun setupActions() {
        binding.btnSort.setOnClickListener { v ->
            showSortMenu(v)
        }

        binding.btnSelectAll.setOnClickListener {
            val currentItems = viewModel.getFilteredAndSortedItems(mediaType, onlyRecovered)
            val selected = viewModel.selectedUris.value
            if (selected.size == currentItems.size && currentItems.isNotEmpty()) {
                viewModel.clearSelection()
            } else {
                viewModel.selectAll(currentItems)
            }
        }

        binding.btnClearSelection.setOnClickListener {
            viewModel.clearSelection()
        }

        binding.btnRecoverSelected.setOnClickListener {
            handleRecoverSelected()
        }

        binding.btnEmptyScan.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_dashboard)
        }
    }

    private fun showSortMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.apply {
            add(0, 1, 0, R.string.sort_date_desc)
            add(0, 2, 1, R.string.sort_date_asc)
            add(0, 3, 2, R.string.sort_size_desc)
            add(0, 4, 3, R.string.sort_size_asc)
            add(0, 5, 4, R.string.sort_name_asc)
            add(0, 6, 5, R.string.sort_name_desc)
        }

        popup.setOnMenuItemClickListener { item ->
            val order = when (item.itemId) {
                1 -> SortOrder.DATE_DESC
                2 -> SortOrder.DATE_ASC
                3 -> SortOrder.SIZE_DESC
                4 -> SortOrder.SIZE_ASC
                5 -> SortOrder.NAME_ASC
                6 -> SortOrder.NAME_DESC
                else -> SortOrder.DATE_DESC
            }
            viewModel.setSortOrder(order)
            true
        }
        popup.show()
    }

    private fun showMediaDetail(item: MediaItem) {
        val dialog = MediaDetailDialog(item) { itemToRecover ->
            viewModel.recoverSingle(itemToRecover) { result ->
                showRecoveryResultDialog(result)
            }
        }
        dialog.show(parentFragmentManager, MediaDetailDialog.TAG)
    }

    private fun handleRecoverSelected() {
        val count = viewModel.selectedUris.value.size
        if (count == 0) {
            Toast.makeText(requireContext(), R.string.toast_no_selection, Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.recovery_dialog_title)
            .setMessage("Are you sure you want to restore $count selected item(s) to Pictures/RecoveredMedia?")
            .setPositiveButton("Restore") { _, _ ->
                viewModel.recoverSelected { result ->
                    showRecoveryResultDialog(result)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showRecoveryResultDialog(result: com.logybyte.assetsrecovery.data.model.RecoveryResult) {
        val msg = getString(
            R.string.recovery_result_format,
            result.succeededCount,
            result.failedCount,
            result.skippedCount
        ) + "\n\nSaved to: ${result.outputDirectoryName}"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.recovery_success_title)
            .setMessage(msg)
            .setPositiveButton(R.string.action_close, null)
            .setNeutralButton("View Recovered") { _, _ ->
                (activity as? MainActivity)?.navigateToTab(R.id.nav_recovered)
            }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allMediaItems.collect { _ ->
                        updateList()
                    }
                }
                launch {
                    viewModel.filterType.collect { _ ->
                        updateList()
                    }
                }
                launch {
                    viewModel.sortOrder.collect { _ ->
                        updateList()
                    }
                }
                launch {
                    viewModel.selectedUris.collect { selected ->
                        adapter.setSelectedUris(selected)
                        updateSelectionUi(selected)
                    }
                }
                launch {
                    viewModel.isRecovering.collect { isRecovering ->
                        binding.btnRecoverSelected.isEnabled = !isRecovering
                    }
                }
            }
        }
    }

    private fun updateList() {
        val items = viewModel.getFilteredAndSortedItems(mediaType, onlyRecovered)
        adapter.submitList(items)
        binding.tvItemsCount.text = "${items.size} item${if (items.size == 1) "" else "s"}"

        if (items.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvMedia.visibility = View.GONE
            configureEmptyState()
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvMedia.visibility = View.VISIBLE
        }
    }

    private fun updateSelectionUi(selected: Set<android.net.Uri>) {
        val currentItems = viewModel.getFilteredAndSortedItems(mediaType, onlyRecovered)

        if (selected.isNotEmpty()) {
            binding.cardBottomActions.visibility = View.VISIBLE
            binding.btnRecoverSelected.text = getString(R.string.action_recover_selected, selected.size)
            binding.btnSelectAll.text = if (selected.size == currentItems.size && currentItems.isNotEmpty()) {
                getString(R.string.action_deselect_all)
            } else {
                getString(R.string.action_select_all)
            }
        } else {
            binding.cardBottomActions.visibility = View.GONE
            binding.btnSelectAll.text = getString(R.string.action_select_all)
        }
    }

    private fun configureEmptyState() {
        when {
            onlyRecovered -> {
                binding.tvEmptyTitle.setText(R.string.empty_recovered_title)
                binding.tvEmptyDesc.setText(R.string.empty_recovered_desc)
                binding.ivEmptyIcon.setImageResource(R.drawable.ic_recovered)
                binding.btnEmptyScan.text = "Go to Images"
                binding.btnEmptyScan.setOnClickListener {
                    (activity as? MainActivity)?.navigateToTab(R.id.nav_images)
                }
            }
            mediaType == MediaType.VIDEO -> {
                binding.tvEmptyTitle.setText(R.string.empty_videos_title)
                binding.tvEmptyDesc.setText(R.string.empty_videos_desc)
                binding.ivEmptyIcon.setImageResource(R.drawable.ic_videos)
                binding.btnEmptyScan.text = getString(R.string.btn_start_scan)
                binding.btnEmptyScan.setOnClickListener {
                    (activity as? MainActivity)?.navigateToTab(R.id.nav_dashboard)
                }
            }
            else -> {
                binding.tvEmptyTitle.setText(R.string.empty_images_title)
                binding.tvEmptyDesc.setText(R.string.empty_images_desc)
                binding.ivEmptyIcon.setImageResource(R.drawable.ic_images)
                binding.btnEmptyScan.text = getString(R.string.btn_start_scan)
                binding.btnEmptyScan.setOnClickListener {
                    (activity as? MainActivity)?.navigateToTab(R.id.nav_dashboard)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MEDIA_TYPE = "arg_media_type"
        private const val ARG_ONLY_RECOVERED = "arg_only_recovered"

        fun newInstance(type: MediaType?, onlyRecovered: Boolean = false): GalleryFragment {
            return GalleryFragment().apply {
                arguments = Bundle().apply {
                    if (type != null) {
                        putString(ARG_MEDIA_TYPE, type.name)
                    }
                    putBoolean(ARG_ONLY_RECOVERED, onlyRecovered)
                }
            }
        }
    }
}
