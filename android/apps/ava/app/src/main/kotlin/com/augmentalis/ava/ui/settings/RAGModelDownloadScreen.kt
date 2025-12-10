// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/settings/RAGModelDownloadScreen.kt
// created: 2025-11-23
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
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * RAG Embedding Model Download Screen
 *
 * Allows users to download AON embedding models for RAG (Retrieval-Augmented Generation).
 * Models are wrapped in AVA-AON format with authentication.
 *
 * ## Features:
 * - On-demand download from HuggingFace
 * - Progress tracking with cancelation
 * - Model management (delete, verify)
 * - License tier support (free/pro/enterprise)
 * - Int8 quantization (75% space savings)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RAGModelDownloadScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Replace with actual ViewModel when Settings module is integrated
    val downloadStates = remember { mutableStateMapOf<String, ModelDownloadState>() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("RAG Embedding Models") },
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
                RAGInfoCard()
            }

            // Available embedding models
            items(getAvailableRAGModels()) { modelInfo ->
                RAGModelDownloadCard(
                    modelInfo = modelInfo,
                    downloadState = downloadStates[modelInfo.modelId],
                    onDownloadClick = {
                        // TODO: Implement download via ViewModel
                        downloadStates[modelInfo.modelId] = ModelDownloadState(isDownloading = true)
                    },
                    onCancelClick = {
                        // TODO: Implement cancel via ViewModel
                        downloadStates.remove(modelInfo.modelId)
                    },
                    onDeleteClick = {
                        // TODO: Implement delete via ViewModel
                        downloadStates.remove(modelInfo.modelId)
                    }
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
 * Info card explaining RAG model downloads
 */
@Composable
fun RAGInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
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
                        text = "RAG Embedding Models",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "These models convert text into semantic vectors for document search. " +
                               "All models are Int8 quantized (75% smaller) and wrapped in AVA-AON " +
                               "format for security. First download may take 1-3 minutes on WiFi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Storage location info
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Storage Locations",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Models: /sdcard/ava-ai-models/embeddings/\n" +
                               "Documents: /sdcard/Android/data/com.augmentalis.ava/files/rag/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * RAG model download card
 */
@Composable
fun RAGModelDownloadCard(
    modelInfo: RAGModelInfo,
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
                    icon = Icons.Default.Layers,
                    label = "Dimension",
                    value = "${modelInfo.dimension}"
                )
                ModelSpec(
                    icon = Icons.Default.Language,
                    label = "Languages",
                    value = modelInfo.languages
                )
                ModelSpec(
                    icon = Icons.Default.Compress,
                    label = "Quantization",
                    value = "Int8"
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

            // License tier badge
            if (modelInfo.licenseTier > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (modelInfo.licenseTier == 2) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (modelInfo.licenseTier == 2) Icons.Default.Diamond else Icons.Default.Upgrade,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when (modelInfo.licenseTier) {
                                1 -> "Pro"
                                2 -> "Enterprise"
                                else -> "Free"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                                text = "Downloading...",
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
 * RAG Model information
 */
data class RAGModelInfo(
    val modelId: String,
    val displayName: String,
    val description: String,
    val sizeDisplay: String,
    val sizeMB: Int,
    val dimension: Int,
    val languages: String,
    val baseModel: String,
    val huggingFaceUrl: String,
    val licenseTier: Int = 0,  // 0=free, 1=pro, 2=enterprise
    val recommended: Boolean = false
)

/**
 * Get available RAG embedding models
 *
 * Models are automatically wrapped with AON authentication for authorized apps:
 * - com.augmentalis.ava
 * - com.augmentalis.avaconnect
 * - com.augmentalis.voiceos
 */
private fun getAvailableRAGModels(): List<RAGModelInfo> {
    return listOf(
        // FREE TIER
        RAGModelInfo(
            modelId = "AVA-384-Base-INT8",
            displayName = "AVA Base (English)",
            description = "Best balance of speed and quality for English text. Ideal for documents, articles, and knowledge bases.",
            sizeDisplay = "~90 MB",
            sizeMB = 90,
            dimension = 384,
            languages = "English",
            baseModel = "all-MiniLM-L6-v2",
            huggingFaceUrl = "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2",
            licenseTier = 0,
            recommended = true
        ),
        RAGModelInfo(
            modelId = "AVA-384-Fast-INT8",
            displayName = "AVA Fast (English)",
            description = "Faster inference with slightly lower quality. Good for real-time search and quick lookups.",
            sizeDisplay = "~61 MB",
            sizeMB = 61,
            dimension = 384,
            languages = "English",
            baseModel = "paraphrase-MiniLM-L3-v2",
            huggingFaceUrl = "https://huggingface.co/sentence-transformers/paraphrase-MiniLM-L3-v2",
            licenseTier = 0
        ),

        // PRO TIER
        RAGModelInfo(
            modelId = "AVA-768-Qual-INT8",
            displayName = "AVA Quality (English)",
            description = "Highest quality for English. 768-dimensional embeddings for better semantic understanding. Requires Pro license.",
            sizeDisplay = "~420 MB",
            sizeMB = 420,
            dimension = 768,
            languages = "English",
            baseModel = "all-mpnet-base-v2",
            huggingFaceUrl = "https://huggingface.co/sentence-transformers/all-mpnet-base-v2",
            licenseTier = 1
        ),
        RAGModelInfo(
            modelId = "AVA-384-Multi-INT8",
            displayName = "AVA Multilingual (50+ Languages)",
            description = "Supports 50+ languages including Spanish, French, German, Chinese, Japanese. Requires Pro license.",
            sizeDisplay = "~470 MB",
            sizeMB = 470,
            dimension = 384,
            languages = "50+ languages",
            baseModel = "paraphrase-multilingual-MiniLM-L12-v2",
            huggingFaceUrl = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
            licenseTier = 1,
            recommended = true
        ),
        RAGModelInfo(
            modelId = "AVA-768-Multi-INT8",
            displayName = "AVA Multilingual Quality",
            description = "Highest quality multilingual model. 768-dimensional embeddings for 50+ languages. Requires Pro license.",
            sizeDisplay = "~1.1 GB",
            sizeMB = 1100,
            dimension = 768,
            languages = "50+ languages",
            baseModel = "paraphrase-multilingual-mpnet-base-v2",
            huggingFaceUrl = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-mpnet-base-v2",
            licenseTier = 1
        ),

        // LANGUAGE-SPECIFIC (PRO)
        RAGModelInfo(
            modelId = "AVA-384-ZH-INT8",
            displayName = "AVA Chinese",
            description = "Optimized for Chinese (Simplified & Traditional). Better performance than multilingual model for Chinese-only documents.",
            sizeDisplay = "~220 MB",
            sizeMB = 220,
            dimension = 384,
            languages = "Chinese (ZH)",
            baseModel = "sbert-chinese-general-v2",
            huggingFaceUrl = "https://huggingface.co/shibing624/text2vec-base-chinese",
            licenseTier = 1
        ),
        RAGModelInfo(
            modelId = "AVA-768-JA-INT8",
            displayName = "AVA Japanese",
            description = "Optimized for Japanese text. Trained on Japanese corpus for better semantic understanding. Requires Pro license.",
            sizeDisplay = "~340 MB",
            sizeMB = 340,
            dimension = 768,
            languages = "Japanese (JA)",
            baseModel = "sentence-bert-base-ja-mean-tokens-v2",
            huggingFaceUrl = "https://huggingface.co/sonoisa/sentence-bert-base-ja-mean-tokens-v2",
            licenseTier = 1
        )
    )
}
