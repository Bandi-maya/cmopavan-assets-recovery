package com.logybyte.assetsrecovery.ui.dialog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.logybyte.assetsrecovery.R
import com.logybyte.assetsrecovery.data.model.MediaItem
import com.logybyte.assetsrecovery.data.model.MediaType
import com.logybyte.assetsrecovery.data.model.RecoveryStatus
import com.logybyte.assetsrecovery.databinding.DialogMediaDetailBinding
import com.logybyte.assetsrecovery.util.FormatUtils
import java.io.File

class MediaDetailDialog(
    private val item: MediaItem,
    private val onRecoverClick: (MediaItem) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogMediaDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMediaDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvDetailName.text = item.displayName
        binding.tvDetailSize.text = FormatUtils.formatFileSize(item.size)
        binding.tvDetailDate.text = FormatUtils.formatDate(item.dateModified)
        binding.tvDetailPath.text = item.path.ifEmpty { item.uri.toString() }

        // Status text
        binding.tvDetailStatus.text = when {
            item.recoveryStatus == RecoveryStatus.RECOVERED -> "Recovered (Saved to gallery)"
            item.isTrashed -> "In Recycle Bin (Eligible for Recovery)"
            item.isHidden -> "Hidden / Orphaned File (Eligible for Recovery)"
            else -> "Active Media"
        }

        // Preview thumbnail
        binding.ivDetailPreview.load(item.uri) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_background)
        }

        // Video Play Button
        if (item.mediaType == MediaType.VIDEO) {
            binding.fabPlayVideo.visibility = View.VISIBLE
            binding.fabPlayVideo.setOnClickListener {
                playMedia(item)
            }
        } else {
            binding.fabPlayVideo.visibility = View.GONE
        }

        // Close button
        binding.btnDialogClose.setOnClickListener {
            dismiss()
        }

        // Share button
        binding.btnDetailShare.setOnClickListener {
            shareMedia(item)
        }

        // Recover button
        if (item.recoveryStatus == RecoveryStatus.RECOVERED) {
            binding.btnDetailRecover.isEnabled = false
            binding.btnDetailRecover.text = "Already Recovered"
        } else {
            binding.btnDetailRecover.setOnClickListener {
                onRecoverClick(item)
                dismiss()
            }
        }
    }

    private fun playMedia(item: MediaItem) {
        try {
            val shareableUri = getShareableUri(item)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(shareableUri, item.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No player available for this video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareMedia(item: MediaItem) {
        try {
            val shareableUri = getShareableUri(item)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, shareableUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Media"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to share file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getShareableUri(item: MediaItem): Uri {
        return if (item.uri.scheme == "file") {
            try {
                val file = File(item.uri.path ?: item.path)
                FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                item.uri
            }
        } else {
            item.uri
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MediaDetailDialog"
    }
}
