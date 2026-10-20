package com.logybyte.assetsrecovery.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            .coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return if (digitGroups == 0) {
            String.format(Locale.US, "%d %s", bytes, units[digitGroups])
        } else {
            String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }
    }

    fun formatDate(timestampSec: Long): String {
        if (timestampSec <= 0) return "Unknown"
        // Sometimes dateModified is in seconds, sometimes milliseconds
        val millis = if (timestampSec < 100000000000L) timestampSec * 1000L else timestampSec
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "00:00"
        val totalSeconds = durationMs / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
