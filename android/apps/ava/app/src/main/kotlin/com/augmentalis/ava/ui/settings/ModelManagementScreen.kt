/**
 * Model Management Screen
 *
 * UI for downloading and managing LLM models.
 * Features:
 * - Browse available models from HuggingFace
 * - Download models with progress tracking
 * - View installed models
 * - Delete models
 * - Storage usage visualization
 *
 * Created: 2025-12-06
 * Author: AVA AI Team
 */

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.augmentalis.ava.features.llm.download.LLMModelDownloader
import com.augmentalis.ava.features.llm.download.ModelStorageManager

/**
 * Model Management Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagementScreen(
    viewModel: ModelManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val storageInfo by viewModel.storageInfo.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LLM Model Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadModels() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Storage usage card
            storageInfo?.let { info ->
                StorageUsageCard(info)
            }

            // Tabs: Available | Installed
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Available") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Installed") }
                )
            }

            // Content based on selected tab
            when (uiState) {
                is ModelManagementUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ModelManagementUiState.Success -> {
                    val successState = uiState as ModelManagementUiState.Success

                    when (selectedTab) {
                        0 -> AvailableModelsTab(
                            availableModels = successState.availableModels,
                            installedModels = successState.installedModels,
                            downloadStates = downloadStates,
                            onDownload = { viewModel.downloadModel(it) },
                            onCancel = { viewModel.cancelDownload(it) }
                        )

                        1 -> InstalledModelsTab(
                            installedModels = successState.installedModels,
                            onDelete = { viewModel.deleteModel(it) }
                        )
                    }
                }

                is ModelManagementUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (uiState as ModelManagementUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = { viewModel.loadModels() },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Storage usage card
 */
@Composable
fun StorageUsageCard(info: ModelStorageManager.StorageInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Storage Usage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = (info.usedBytes.toFloat() / info.totalBytes),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Used: ${String.format("%.2f", info.usedGB)} GB",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Available: ${String.format("%.2f", info.availableGB)} GB",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "${info.modelCount} model(s) installed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Available models tab
 */
@Composable
fun AvailableModelsTab(
    availableModels: List<LLMModelDownloader.ModelInfo>,
    installedModels: List<Pair<String, Long>>,
    downloadStates: Map<String, LLMModelDownloader.DownloadState>,
    onDownload: (LLMModelDownloader.ModelInfo) -> Unit,
    onCancel: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(availableModels) { model ->
            val isInstalled = installedModels.any { it.first == model.modelId }
            val downloadState = downloadStates[model.modelId]

            AvailableModelCard(
                model = model,
                isInstalled = isInstalled,
                downloadState = downloadState,
                onDownload = { onDownload(model) },
                onCancel = { onCancel(model.modelId) }
            )
        }
    }
}

/**
 * Available model card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailableModelCard(
    model: LLMModelDownloader.ModelInfo,
    isInstalled: Boolean,
    downloadState: LLMModelDownloader.DownloadState?,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (!isInstalled && downloadState == null) onDownload() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = model.modelId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isInstalled) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Installed",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            model.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Size: ${model.getDisplaySize()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                model.quantization?.let { quant ->
                    Text(
                        text = "Quant: $quant",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Download progress
            when (downloadState) {
                is LLMModelDownloader.DownloadState.Downloading -> {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        LinearProgressIndicator(
                            progress = downloadState.progress,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${downloadState.getProgressPercentage()}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${String.format("%.1f", downloadState.getSpeedMBPerSec())} MB/s",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancel")
                        }
                    }
                }

                is LLMModelDownloader.DownloadState.Failed -> {
                    Text(
                        text = "Failed: ${downloadState.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                else -> {
                    if (!isInstalled) {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Download, "Download", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Installed models tab
 */
@Composable
fun InstalledModelsTab(
    installedModels: List<Pair<String, Long>>,
    onDelete: (String) -> Unit
) {
    if (installedModels.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "No models",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No models installed",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(installedModels) { (modelId, size) ->
                InstalledModelCard(
                    modelId = modelId,
                    sizeBytes = size,
                    onDelete = { onDelete(modelId) }
                )
            }
        }
    }
}

/**
 * Installed model card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstalledModelCard(
    modelId: String,
    sizeBytes: Long,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = modelId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val sizeGB = sizeBytes / (1024.0 * 1024.0 * 1024.0)
                Text(
                    text = "${String.format("%.2f", sizeGB)} GB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Model?") },
            text = { Text("Are you sure you want to delete $modelId? This will free ${String.format("%.2f", sizeBytes / (1024.0 * 1024.0 * 1024.0))} GB.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
