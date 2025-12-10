// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/settings/ModelDownloadScreen.kt
// created: 2025-11-07
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Model Download Screen for MLC-LLM models
 *
 * Allows users to download and manage on-device AI models from HuggingFace.
 * Integrates with HuggingFaceModelDownloader for progress tracking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadScreen(
    viewModel: ISettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Download AI Models") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Header info card
            item {
                InfoCard()
            }

            // Available models
            items(getAvailableLlmModels()) { modelInfo ->
                ModelDownloadCard(
                    modelInfo = modelInfo,
                    downloadState = uiState.modelDownloadStates[modelInfo.modelId],
                    onDownloadClick = { viewModel.startModelDownload(modelInfo.modelId) },
                    onCancelClick = { viewModel.cancelModelDownload(modelInfo.modelId) },
                    onDeleteClick = { viewModel.deleteModel(modelInfo.modelId) }
                )
            }

            // Spacer at bottom
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Info card explaining model downloads
 */
@Composable
fun InfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "On-Device AI Models",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Download models to enable offline AI inference. Models are quantized (4-bit) for mobile efficiency. First download may take several minutes on WiFi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Model download card showing model info and download controls
 */
@Composable
fun ModelDownloadCard(
    modelInfo: LlmModelInfo,
    downloadState: ModelDownloadState?,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Model name and size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = modelInfo.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = modelInfo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Model size badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = modelInfo.sizeDisplay,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Model specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModelSpec(
                    icon = Icons.Default.Memory,
                    label = "VRAM",
                    value = modelInfo.vramDisplay
                )
                ModelSpec(
                    icon = Icons.Default.Speed,
                    label = "Quantization",
                    value = "4-bit"
                )
                if (modelInfo.recommended) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Recommended",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Download state and actions
            when {
                downloadState?.isDownloading == true -> {
                    // Downloading
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading ${downloadState.currentFile}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${downloadState.percentage.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = downloadState.percentage / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${downloadState.downloadedMB} MB / ${downloadState.totalMB} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onCancelClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel Download")
                        }
                    }
                }
                downloadState?.isDownloaded == true -> {
                    // Downloaded
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Downloaded",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
                else -> {
                    // Not downloaded
                    Button(
                        onClick = onDownloadClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Model")
                    }
                }
            }
        }
    }
}

/**
 * Model specification row
 */
@Composable
fun ModelSpec(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Model download state
 */
data class ModelDownloadState(
    val isDownloading: Boolean = false,
    val isDownloaded: Boolean = false,
    val currentFile: String = "",
    val percentage: Float = 0f,
    val downloadedMB: Int = 0,
    val totalMB: Int = 0
)

/**
 * LLM Model information
 */
data class LlmModelInfo(
    val modelId: String,
    val displayName: String,
    val description: String,
    val sizeDisplay: String,
    val sizeMB: Int,
    val vramDisplay: String,
    val huggingFaceUrl: String,
    val modelLib: String,
    val recommended: Boolean = false
)

/**
 * Get available LLM models (from mlc-app-config.json)
 */
private fun getAvailableLlmModels(): List<LlmModelInfo> {
    return listOf(
        LlmModelInfo(
            modelId = "gemma-2-2b-it-q4f16_1-MLC",
            displayName = "Gemma 2 2B Instruct",
            description = "Google's efficient 2B model optimized for instruction following. Best balance of size and quality.",
            sizeDisplay = "~1.2 GB",
            sizeMB = 1200,
            vramDisplay = "3 GB",
            huggingFaceUrl = "https://huggingface.co/mlc-ai/gemma-2-2b-it-q4f16_1-MLC",
            modelLib = "gemma2_q4f16_1_5cc7dbd3ae3d1040984d9720b2d7b7d4",
            recommended = true
        ),
        LlmModelInfo(
            modelId = "Qwen2.5-1.5B-Instruct-q4f16_1-MLC",
            displayName = "Qwen 2.5 1.5B Instruct",
            description = "Alibaba's compact 1.5B model with strong multilingual capabilities. Fastest inference.",
            sizeDisplay = "~900 MB",
            sizeMB = 900,
            vramDisplay = "3.9 GB",
            huggingFaceUrl = "https://huggingface.co/mlc-ai/Qwen2.5-1.5B-Instruct-q4f16_1-MLC",
            modelLib = "qwen2_5_q4f16_1"
        ),
        LlmModelInfo(
            modelId = "Llama-3.2-3B-Instruct-q4f16_0-MLC",
            displayName = "Llama 3.2 3B Instruct",
            description = "Meta's latest 3B model with improved reasoning. Higher quality but slower.",
            sizeDisplay = "~1.8 GB",
            sizeMB = 1800,
            vramDisplay = "4.6 GB",
            huggingFaceUrl = "https://huggingface.co/mlc-ai/Llama-3.2-3B-Instruct-q4f16_0-MLC",
            modelLib = "llama3_2_q4f16_0"
        ),
        LlmModelInfo(
            modelId = "Phi-3.5-mini-instruct-q4f16_0-MLC",
            displayName = "Phi 3.5 Mini Instruct",
            description = "Microsoft's 3.8B model with strong code and reasoning capabilities.",
            sizeDisplay = "~2.2 GB",
            sizeMB = 2200,
            vramDisplay = "4.2 GB",
            huggingFaceUrl = "https://huggingface.co/mlc-ai/Phi-3.5-mini-instruct-q4f16_0-MLC",
            modelLib = "phi3_5_q4f16_0"
        ),
        LlmModelInfo(
            modelId = "Mistral-7B-Instruct-v0.3-q4f16_1-MLC",
            displayName = "Mistral 7B Instruct v0.3",
            description = "Mistral's 7B model with strong performance across tasks. Requires high-end device.",
            sizeDisplay = "~4.0 GB",
            sizeMB = 4000,
            vramDisplay = "4.1 GB",
            huggingFaceUrl = "https://huggingface.co/mlc-ai/Mistral-7B-Instruct-v0.3-q4f16_1-MLC",
            modelLib = "mistral_q4f16_1"
        )
    )
}
