/**
 * WhisperModelDownloadScreen.kt - Compose UI for Whisper model management
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * AvanueTheme-compliant download UI with AVID voice identifiers on all
 * interactive elements. Shows model selection, download progress, and
 * storage management.
 */
package com.augmentalis.speechrecognition.whisper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.speechrecognition.whisper.LocalModelInfo
import com.augmentalis.speechrecognition.whisper.ModelDownloadState
import com.augmentalis.speechrecognition.whisper.WhisperModelManager
import com.augmentalis.speechrecognition.whisper.WhisperModelSize
import kotlinx.coroutines.flow.StateFlow

/**
 * Full-screen model management UI.
 *
 * Shows available models with size/speed/accuracy info, download progress,
 * and storage management. Uses AvanueTheme colors (MANDATORY).
 *
 * @param modelManager The WhisperModelManager instance
 * @param onModelReady Called when a model is downloaded and ready to use
 * @param onDismiss Called when user wants to close the screen
 */
@Composable
fun WhisperModelDownloadScreen(
    modelManager: WhisperModelManager,
    onModelReady: (WhisperModelSize) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val downloadState by modelManager.downloadState.collectAsState()
    val recommended = remember { modelManager.recommendModel() }
    val downloadedModels = remember { modelManager.getDownloadedModels() }
    val availableStorageMB = remember { modelManager.getAvailableStorageMB() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        AvanueTheme.colors.background,
                        AvanueTheme.colors.surface.copy(alpha = 0.6f),
                        AvanueTheme.colors.background
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Whisper Models",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AvanueTheme.colors.onBackground
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Voice: click Close"
                }
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = AvanueTheme.colors.onBackground
                )
            }
        }

        // Storage info
        Text(
            text = "Storage: ${availableStorageMB}MB available",
            fontSize = 12.sp,
            color = AvanueTheme.colors.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Download progress (if active)
        when (val state = downloadState) {
            is ModelDownloadState.Downloading -> {
                DownloadProgressCard(
                    state = state,
                    onCancel = { modelManager.cancelDownload() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            is ModelDownloadState.Verifying -> {
                VerifyingCard(state.modelSize)
                Spacer(modifier = Modifier.height(12.dp))
            }
            is ModelDownloadState.Failed -> {
                ErrorCard(state.modelSize, state.error)
                Spacer(modifier = Modifier.height(12.dp))
            }
            else -> {}
        }

        // Model list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(WhisperModelSize.entries.toList()) { modelSize ->
                val isDownloaded = downloadedModels.any { it.modelSize == modelSize }
                val isRecommended = modelSize == recommended
                val isDownloading = downloadState is ModelDownloadState.Downloading &&
                        (downloadState as ModelDownloadState.Downloading).modelSize == modelSize

                ModelCard(
                    modelSize = modelSize,
                    isDownloaded = isDownloaded,
                    isRecommended = isRecommended,
                    isDownloading = isDownloading,
                    localInfo = downloadedModels.firstOrNull { it.modelSize == modelSize },
                    onDownload = { /* trigger from ViewModel/coroutine scope */ },
                    onDelete = { modelManager.deleteModel(modelSize) },
                    onSelect = { onModelReady(modelSize) }
                )
            }
        }
    }
}

@Composable
private fun ModelCard(
    modelSize: WhisperModelSize,
    isDownloaded: Boolean,
    isRecommended: Boolean,
    isDownloading: Boolean,
    localInfo: LocalModelInfo?,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AvanueTheme.colors.surface)
            .padding(12.dp)
    ) {
        Column {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = modelSize.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AvanueTheme.colors.onSurface
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recommended",
                            fontSize = 10.sp,
                            color = AvanueTheme.colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isDownloaded) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Downloaded",
                            tint = AvanueTheme.colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Action buttons
                Row {
                    if (isDownloaded) {
                        TextButton(
                            onClick = onSelect,
                            modifier = Modifier.semantics {
                                contentDescription = "Voice: click Use ${modelSize.displayName}"
                            }
                        ) {
                            Text("Use", color = AvanueTheme.colors.primary)
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.semantics {
                                contentDescription = "Voice: click Delete ${modelSize.displayName}"
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = AvanueTheme.colors.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (!isDownloading) {
                        TextButton(
                            onClick = onDownload,
                            modifier = Modifier.semantics {
                                contentDescription = "Voice: click Download ${modelSize.displayName}"
                            }
                        ) {
                            Text("Download", color = AvanueTheme.colors.primary)
                        }
                    }
                }
            }

            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailChip("${modelSize.approxSizeMB}MB")
                DetailChip(
                    when {
                        modelSize.relativeSpeed <= 1.5f -> "Fast"
                        modelSize.relativeSpeed <= 3f -> "Moderate"
                        modelSize.relativeSpeed <= 8f -> "Slow"
                        else -> "Very Slow"
                    }
                )
                DetailChip(
                    when {
                        modelSize.relativeSpeed <= 1.5f -> "Basic"
                        modelSize.relativeSpeed <= 3f -> "Good"
                        modelSize.relativeSpeed <= 8f -> "Very Good"
                        else -> "Excellent"
                    }
                )
                if (modelSize.isEnglishOnly) {
                    DetailChip("English Only")
                } else {
                    DetailChip("Multilingual")
                }
            }

            // Local info
            if (localInfo != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Downloaded: ${"%.1f".format(localInfo.fileSizeMB)}MB" +
                            if (localInfo.isVerified) " (verified)" else "",
                    fontSize = 11.sp,
                    color = AvanueTheme.colors.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun DetailChip(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = AvanueTheme.colors.onSurface.copy(alpha = 0.6f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun DownloadProgressCard(
    state: ModelDownloadState.Downloading,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AvanueTheme.colors.primary.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Downloading ${state.modelSize.displayName}...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AvanueTheme.colors.onBackground
                )
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.semantics {
                        contentDescription = "Voice: click Cancel Download"
                    }
                ) {
                    Text("Cancel", color = AvanueTheme.colors.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { state.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AvanueTheme.colors.primary,
                trackColor = AvanueTheme.colors.surface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${"%.1f".format(state.downloadedMB)} / ${"%.1f".format(state.totalMB)} MB",
                    fontSize = 11.sp,
                    color = AvanueTheme.colors.onBackground.copy(alpha = 0.6f)
                )
                if (state.speedMBPerSec > 0) {
                    Text(
                        text = "${"%.1f".format(state.speedMBPerSec)} MB/s",
                        fontSize = 11.sp,
                        color = AvanueTheme.colors.onBackground.copy(alpha = 0.6f)
                    )
                }
                if (state.estimatedRemainingSeconds > 0) {
                    Text(
                        text = formatDuration(state.estimatedRemainingSeconds),
                        fontSize = 11.sp,
                        color = AvanueTheme.colors.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun VerifyingCard(modelSize: WhisperModelSize) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AvanueTheme.colors.surface)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = AvanueTheme.colors.primary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Verifying ${modelSize.displayName}...",
                fontSize = 14.sp,
                color = AvanueTheme.colors.onSurface
            )
        }
    }
}

@Composable
private fun ErrorCard(modelSize: WhisperModelSize, error: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AvanueTheme.colors.error.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = "Failed: ${modelSize.displayName}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AvanueTheme.colors.error
            )
            Text(
                text = error,
                fontSize = 12.sp,
                color = AvanueTheme.colors.onBackground.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
