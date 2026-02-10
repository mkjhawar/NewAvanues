package com.augmentalis.webavanue

import android.R.attr.contentDescription
import android.text.format.Formatter.formatFileSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.augmentalis.avanueui.theme.AvanueTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.Download
import com.augmentalis.webavanue.DownloadStatus

/**
 * DownloadItem - Individual download item in list
 *
 * Features:
 * - Shows filename, file size, and status
 * - Progress bar for in-progress downloads
 * - Cancel button for active downloads
 * - Retry button for failed downloads
 * - Delete button for completed/failed downloads
 * - Click to open file (completed downloads only)
 *
 * @param download Download data
 * @param onClick Callback when download is clicked (open file)
 * @param onCancel Callback when cancel button is clicked
 * @param onRetry Callback when retry button is clicked
 * @param onDelete Callback when delete button is clicked
 * @param modifier Modifier for customization
 */
@Composable
fun DownloadItem(
    download: Download,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.clickable(
            enabled = download.status == DownloadStatus.COMPLETED
        ) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = AvanueTheme.colors.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Icon + Filename + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon
                Icon(
                    imageVector = when (download.status) {
                        DownloadStatus.DOWNLOADING -> Icons.Default.Refresh
                        DownloadStatus.COMPLETED -> Icons.Default.CheckCircle
                        DownloadStatus.FAILED, DownloadStatus.UNKNOWN -> Icons.Default.Warning
                        DownloadStatus.PAUSED -> Icons.Default.Refresh
                        DownloadStatus.PENDING -> Icons.Default.Close
                        DownloadStatus.CANCELLED -> Icons.Default.Close
                    },
                    contentDescription = null,
                    tint = when (download.status) {
                        DownloadStatus.COMPLETED -> AvanueTheme.colors.primary
                        DownloadStatus.FAILED, DownloadStatus.CANCELLED -> AvanueTheme.colors.error
                        else -> AvanueTheme.colors.textSecondary
                    },
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Filename and size
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.filename,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = buildString {
                            append(formatFileSize(download.downloadedSize))
                            if (download.fileSize > 0) {
                                append(" / ${formatFileSize(download.fileSize)}")
                            }
                            append(" • ${formatStatus(download.status)}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AvanueTheme.colors.textSecondary
                    )
                }

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (download.status) {
                        DownloadStatus.DOWNLOADING -> {
                            // Cancel button
                            IconButton(onClick = onCancel) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = AvanueTheme.colors.error
                                )
                            }
                        }

                        DownloadStatus.FAILED -> {
                            // Retry button
                            IconButton(onClick = onRetry) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = AvanueTheme.colors.primary
                                )
                            }

                            // Delete button
                            IconButton(onClick = { showDeleteConfirmation = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = AvanueTheme.colors.textSecondary
                                )
                            }
                        }

                        DownloadStatus.COMPLETED -> {
                            // Delete button
                            IconButton(onClick = { showDeleteConfirmation = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = AvanueTheme.colors.textSecondary
                                )
                            }
                        }

                        DownloadStatus.CANCELLED -> {
                            // Retry button
                            IconButton(onClick = onRetry) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = AvanueTheme.colors.primary
                                )
                            }

                            // Delete button
                            IconButton(onClick = { showDeleteConfirmation = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = AvanueTheme.colors.textSecondary
                                )
                            }
                        }

                        else -> {
                            // No actions for pending/paused
                        }
                    }
                }
            }

            // Progress bar (for in-progress downloads)
            if (download.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))

                val progress = if (download.fileSize > 0) {
                    download.downloadedSize.toFloat() / download.fileSize.toFloat()
                } else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Speed and ETA row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Download speed
                    Text(
                        text = formatSpeed(download.downloadSpeed),
                        style = MaterialTheme.typography.labelSmall,
                        color = AvanueTheme.colors.primary
                    )

                    // Progress percentage
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = AvanueTheme.colors.textSecondary
                    )

                    // ETA
                    if (download.estimatedTimeRemaining > 0) {
                        Text(
                            text = "${formatETA(download.estimatedTimeRemaining)} remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = AvanueTheme.colors.textSecondary
                        )
                    }
                }
            }

            // Error message (for failed downloads)
            if (download.status == DownloadStatus.FAILED) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Download failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.error
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Download?") },
            text = {
                Text(
                    buildString {
                        append("Are you sure you want to delete \"${download.filename}\"?")
                        if (download.status == DownloadStatus.COMPLETED) {
                            append("\n\nThe file will be removed from disk.")
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete", color = AvanueTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Format file size in human-readable format
 */
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1.0 -> "%.2f GB".format(gb)
        mb >= 1.0 -> "%.2f MB".format(mb)
        kb >= 1.0 -> "%.2f KB".format(kb)
        else -> "$bytes B"
    }
}

/**
 * Format download status
 */
private fun formatStatus(status: DownloadStatus): String {
    return when (status) {
        DownloadStatus.PENDING -> "Pending"
        DownloadStatus.DOWNLOADING -> "Downloading"
        DownloadStatus.PAUSED -> "Paused"
        DownloadStatus.COMPLETED -> "Completed"
        DownloadStatus.FAILED, DownloadStatus.UNKNOWN -> "Failed"
        DownloadStatus.CANCELLED -> "Cancelled"
    }
}

/**
 * Format download speed in human-readable format
 */
private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec == 0L -> "0 B/s"
        bytesPerSec < 1024 -> "$bytesPerSec B/s"
        bytesPerSec < 1024 * 1024 -> "${bytesPerSec / 1024} KB/s"
        else -> "%.1f MB/s".format(bytesPerSec / (1024.0 * 1024.0))
    }
}

/**
 * Format estimated time remaining in human-readable format
 */
private fun formatETA(seconds: Long): String {
    return when {
        seconds == 0L -> "calculating..."
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
