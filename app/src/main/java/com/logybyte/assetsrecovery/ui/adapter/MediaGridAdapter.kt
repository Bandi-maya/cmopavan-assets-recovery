package com.logybyte.assetsrecovery.ui.adapter

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import com.logybyte.assetsrecovery.R
import com.logybyte.assetsrecovery.data.model.MediaItem
import com.logybyte.assetsrecovery.data.model.MediaType
import com.logybyte.assetsrecovery.data.model.RecoveryStatus
import com.logybyte.assetsrecovery.databinding.ItemMediaGridBinding
import com.logybyte.assetsrecovery.util.FormatUtils

class MediaGridAdapter(
    private val onItemClick: (MediaItem) -> Unit,
    private val onSelectionToggle: (MediaItem) -> Unit
) : ListAdapter<MediaItem, MediaGridAdapter.MediaViewHolder>(MediaDiffCallback()) {

    private var selectedUris: Set<Uri> = emptySet()

    fun setSelectedUris(uris: Set<Uri>) {
        selectedUris = uris
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MediaViewHolder(private val binding: ItemMediaGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaItem) {
            val isSelected = selectedUris.contains(item.uri)

            binding.tvFilename.text = item.displayName
            binding.tvFilesize.text = FormatUtils.formatFileSize(item.size)

            // Video indicator and duration
            if (item.mediaType == MediaType.VIDEO) {
                binding.ivPlayIndicator.visibility = View.VISIBLE
                if (item.durationMs > 0) {
                    binding.tvDuration.visibility = View.VISIBLE
                    binding.tvDuration.text = FormatUtils.formatDuration(item.durationMs)
                } else {
                    binding.tvDuration.visibility = View.GONE
                }
            } else {
                binding.ivPlayIndicator.visibility = View.GONE
                binding.tvDuration.visibility = View.GONE
            }

            // Status Badge
            when {
                item.recoveryStatus == RecoveryStatus.RECOVERED -> {
                    binding.tvStatusBadge.visibility = View.VISIBLE
                    binding.tvStatusBadge.text = "Recovered"
                    binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#10B981"))
                }
                item.isTrashed -> {
                    binding.tvStatusBadge.visibility = View.VISIBLE
                    binding.tvStatusBadge.text = "Trash"
                    binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#EF4444"))
                }
                item.isHidden -> {
                    binding.tvStatusBadge.visibility = View.VISIBLE
                    binding.tvStatusBadge.text = "Hidden"
                    binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#8B5CF6"))
                }
                else -> {
                    binding.tvStatusBadge.visibility = View.GONE
                }
            }

            // Selection state
            binding.cbSelect.setOnCheckedChangeListener(null)
            binding.cbSelect.isChecked = isSelected
            binding.cbSelect.setOnCheckedChangeListener { _, _ ->
                onSelectionToggle(item)
            }

            binding.cardMedia.isChecked = isSelected
            binding.cardMedia.setOnClickListener {
                onItemClick(item)
            }

            binding.cardMedia.setOnLongClickListener {
                onSelectionToggle(item)
                true
            }

            // Thumbnail loading with Coil
            binding.ivThumbnail.load(item.uri) {
                crossfade(true)
                scale(Scale.FILL)
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_background)
            }
        }
    }

    class MediaDiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem.id == newItem.id && oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem == newItem
        }
    }
}
