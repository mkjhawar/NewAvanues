/**
 * WhisperModelDownloadUI.kt - UI components for Whisper model management
 * 
 * Copyright (C) Augmentalis Inc
 * Author: VOS4 Development Team
 * Created: 2025-08-31
 * 
 * Provides Compose UI components for downloading and managing Whisper models
 */
package com.augmentalis.speechrecognition.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.augmentalis.voiceos.speech.engines.whisper.WhisperModelSize
import com.augmentalis.voiceos.speech.engines.whisper.ModelDownloadState
import com.augmentalis.voiceos.speech.engines.whisper.WhisperModelManager
import kotlinx.coroutines.launch

/**
 * Main dialog for Whisper model download and management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhisperModelDownloadDialog(
    modelManager: WhisperModelManager,
    currentModel: WhisperModelSize? = null,
    onDismiss: () -> Unit,
    onModelSelected: (WhisperModelSize) -> Unit
) {
    val downloadState by modelManager.downloadState.collectAsState()
    val downloadedModels = remember { mutableStateListOf<WhisperModelSize>() }
    val recommendedModel = remember { modelManager.getRecommendedModel() }
    val scope = rememberCoroutineScope()
    
    // Load downloaded models
    LaunchedEffect(Unit) {
        downloadedModels.clear()
        downloadedModels.addAll(modelManager.getDownloadedModels())
    }
    
    Dialog(
        onDismissRequest = {
            if (downloadState !is ModelDownloadState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = downloadState !is ModelDownloadState.Downloading,
            dismissOnClickOutside = downloadState !is ModelDownloadState.Downloading
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                WhisperModelHeader(
                    onClose = {
                        if (downloadState !is ModelDownloadState.Downloading) {
                            onDismiss()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Storage info
                StorageInfoCard(modelManager)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Download state
                AnimatedVisibility(
                    visible = downloadState !is ModelDownloadState.Idle,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    DownloadStateCard(
                        state = downloadState,
                        onCancel = { modelManager.cancelDownload() }
                    )
                }
                
                // Model list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(WhisperModelSize.values().filter { it != WhisperModelSize.LARGE }) { model ->
                        ModelCard(
                            model = model,
                            isDownloaded = downloadedModels.contains(model),
                            isCurrentModel = currentModel == model,
                            isRecommended = model == recommendedModel,
                            isDownloading = downloadState is ModelDownloadState.Downloading,
                            onDownload = {
                                scope.launch {
                                    modelManager.downloadModel(model)
                                    downloadedModels.clear()
                                    downloadedModels.addAll(modelManager.getDownloadedModels())
                                }
                            },
                            onSelect = {
                                onModelSelected(model)
                                onDismiss()
                            },
                            onDelete = {
                                modelManager.deleteModel(model)
                                downloadedModels.remove(model)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog header with title and close button
 */
@Composable
private fun WhisperModelHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Whisper Models",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Download and manage speech recognition models",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close"
            )
        }
    }
}

/**
 * Storage information card
 */
@Composable
private fun StorageInfoCard(modelManager: WhisperModelManager) {
    val totalUsageMB = remember { 
        // Calculate total usage from downloaded models
        val models = modelManager.getDownloadedModels()
        models.sumOf { model ->
            when (model) {
                WhisperModelSize.TINY -> 39L
                WhisperModelSize.BASE -> 74L
                WhisperModelSize.SMALL -> 244L
                WhisperModelSize.MEDIUM -> 769L
                WhisperModelSize.LARGE -> 1550L
            }
        }
    }
    val availableStorageMB = remember { modelManager.getAvailableStorageMB() }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = SpeechIcons.storage(),
                    contentDescription = "Storage",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Models: ${totalUsageMB}MB",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                text = "Available: ${availableStorageMB}MB",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Download state card showing progress
 */
@Composable
private fun DownloadStateCard(
    state: ModelDownloadState,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                is ModelDownloadState.Error -> MaterialTheme.colorScheme.errorContainer
                is ModelDownloadState.Completed -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (state) {
                is ModelDownloadState.Downloading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Downloading...",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${state.downloadedMB.toInt()}MB / ${state.totalMB.toInt()}MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Cancel button
                        IconButton(onClick = onCancel) {
                            Icon(
                                imageVector = SpeechIcons.cancel(),
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Progress bar
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${state.progress.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                is ModelDownloadState.Verifying -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Verifying model integrity...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                is ModelDownloadState.Completed -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Model downloaded successfully!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                is ModelDownloadState.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = SpeechIcons.error(),
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                else -> {}
            }
        }
    }
}

/**
 * Individual model card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelCard(
    model: WhisperModelSize,
    isDownloaded: Boolean,
    isCurrentModel: Boolean,
    isRecommended: Boolean,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isDownloaded && !isDownloading) {
                    Modifier.clickable { onSelect() }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrentModel -> MaterialTheme.colorScheme.primaryContainer
                isDownloaded -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isRecommended) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Model icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrentModel -> MaterialTheme.colorScheme.primary
                            isDownloaded -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = model.modelName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        isCurrentModel || isDownloaded -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Model info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.modelName.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text("RECOMMENDED", fontSize = 10.sp)
                        }
                    }
                    
                    if (isCurrentModel) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ) {
                            Text("ACTIVE", fontSize = 10.sp)
                        }
                    }
                }
                
                Text(
                    text = "${model.approximateSize} • ${model.languages} languages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = getModelDescription(model),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Action buttons
            when {
                isDownloaded -> {
                    if (!isCurrentModel) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                !isDownloading -> {
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = SpeechIcons.download(),
                            contentDescription = "Download",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download")
                    }
                }
            }
        }
    }
}

/**
 * Simple progress indicator for inline use
 */
@Composable
fun WhisperModelDownloadProgress(
    state: ModelDownloadState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        when (state) {
            is ModelDownloadState.Downloading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Downloading: ${state.progress.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            is ModelDownloadState.Verifying -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verifying...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            is ModelDownloadState.Completed -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Complete",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Green
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Green
                    )
                }
            }
            
            is ModelDownloadState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            else -> {}
        }
    }
}

/**
 * Get model description based on size
 */
private fun getModelDescription(model: WhisperModelSize): String {
    return when (model) {
        WhisperModelSize.TINY -> "Fast, basic accuracy, low memory"
        WhisperModelSize.BASE -> "Good balance of speed and accuracy"
        WhisperModelSize.SMALL -> "High accuracy, slower processing"
        WhisperModelSize.MEDIUM -> "Professional quality, high memory"
        WhisperModelSize.LARGE -> "Maximum accuracy, very high memory"
    }
}
