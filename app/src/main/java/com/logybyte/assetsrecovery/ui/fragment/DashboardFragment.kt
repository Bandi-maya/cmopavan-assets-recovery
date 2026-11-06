package com.logybyte.assetsrecovery.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.logybyte.assetsrecovery.R
import com.logybyte.assetsrecovery.data.model.MediaType
import com.logybyte.assetsrecovery.data.model.RecoveryStatus
import com.logybyte.assetsrecovery.databinding.FragmentDashboardBinding
import com.logybyte.assetsrecovery.ui.MainActivity
import com.logybyte.assetsrecovery.ui.viewmodel.RecoveryViewModel
import com.logybyte.assetsrecovery.ui.viewmodel.ScanUiState
import com.logybyte.assetsrecovery.util.PermissionHelper
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecoveryViewModel by activityViewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
<<<<<<< HEAD
    ) { _ ->
        if (PermissionHelper.hasStoragePermissions(requireContext())) {
=======
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
            binding.cardPermission.visibility = View.GONE
            viewModel.startScan()
        } else {
            binding.cardPermission.visibility = View.VISIBLE
            Toast.makeText(
                requireContext(),
                R.string.permission_required,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
        observeViewModel()
        checkPermissions()
    }

    private fun checkPermissions() {
        if (!PermissionHelper.hasStoragePermissions(requireContext())) {
            binding.cardPermission.visibility = View.VISIBLE
        } else {
            binding.cardPermission.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnGrantPermission.setOnClickListener {
            permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
        }

        binding.btnStartScan.setOnClickListener {
            if (PermissionHelper.hasStoragePermissions(requireContext())) {
                viewModel.startScan()
            } else {
                permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
            }
        }

        binding.btnCancelScan.setOnClickListener {
            viewModel.cancelScan()
            Toast.makeText(requireContext(), R.string.toast_scan_cancelled, Toast.LENGTH_SHORT).show()
        }

        // Quick Navigation to other tabs
        binding.btnQuickImages.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_images)
        }
        binding.cardImagesStat.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_images)
        }

        binding.btnQuickVideos.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_videos)
        }
        binding.cardVideosStat.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_videos)
        }

        binding.btnQuickRecovered.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_recovered)
        }
        binding.cardRecoveredStat.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_recovered)
        }

        binding.cardRecoverableStat.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_images)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.scanState.collect { state ->
                        when (state) {
                            is ScanUiState.Idle -> {
                                binding.btnStartScan.visibility = View.VISIBLE
                                binding.layoutScanningProgress.visibility = View.GONE
                            }
                            is ScanUiState.Scanning -> {
                                binding.btnStartScan.visibility = View.GONE
                                binding.layoutScanningProgress.visibility = View.VISIBLE
                                binding.tvScanStatus.text = "Scanning device (${state.count} items found)..."
                                binding.tvScanFile.text = state.currentFile
                            }
                            is ScanUiState.Finished -> {
                                binding.btnStartScan.visibility = View.VISIBLE
                                binding.layoutScanningProgress.visibility = View.GONE
                                val seconds = String.format("%.1f", state.result.durationMs / 1000f)
                                binding.tvLastScanInfo.text = "Last scan: ${state.result.totalScanned} items in ${seconds}s"
                            }
                            is ScanUiState.Error -> {
                                binding.btnStartScan.visibility = View.VISIBLE
                                binding.layoutScanningProgress.visibility = View.GONE
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.allMediaItems.collect { items ->
                        val imagesCount = items.count { it.mediaType == MediaType.IMAGE }
                        val videosCount = items.count { it.mediaType == MediaType.VIDEO }
                        val recoverableCount = items.count { it.isRecoverable }
                        val recoveredCount = items.count { it.recoveryStatus == RecoveryStatus.RECOVERED }

                        binding.tvStatImagesCount.text = imagesCount.toString()
                        binding.tvStatVideosCount.text = videosCount.toString()
                        binding.tvStatRecoverableCount.text = recoverableCount.toString()
                        binding.tvStatRecoveredCount.text = recoveredCount.toString()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
