package com.augmentalis.webavanue

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.Download
import com.augmentalis.webavanue.DownloadStatus
import com.augmentalis.webavanue.DownloadViewModel

/**
 * DownloadListScreen - Main downloads screen
 *
 * Features:
 * - List all downloads (active and completed)
 * - Filter by status (All, Active, Completed, Failed)
 * - Cancel active downloads
 * - Retry failed downloads
 * - Clear completed downloads
 * - Progress indicators for active downloads
 *
 * @param viewModel DownloadViewModel for state and actions
 * @param onNavigateBack Callback to navigate back
 * @param onDownloadClick Callback when download is clicked (open file)
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadListScreen(
    viewModel: DownloadViewModel,
    onNavigateBack: () -> Unit = {},
    onDownloadClick: (Download) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val downloads by viewModel.downloads.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedStatus by remember { mutableStateOf<DownloadStatus?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    // Filter downloads by status
    val filteredDownloads = remember(downloads, selectedStatus) {
        if (selectedStatus == null) {
            downloads
        } else {
            downloads.filter { it.status == selectedStatus }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Clear completed downloads
                    if (downloads.any { it.status == DownloadStatus.COMPLETED }) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Completed"
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { selectedStatus = null },
                    label = { Text("All (${downloads.size})") }
                )

                FilterChip(
                    selected = selectedStatus == DownloadStatus.DOWNLOADING,
                    onClick = { selectedStatus = DownloadStatus.DOWNLOADING },
                    label = {
                        Text("Active (${downloads.count { it.status == DownloadStatus.DOWNLOADING }})")
                    }
                )

                FilterChip(
                    selected = selectedStatus == DownloadStatus.COMPLETED,
                    onClick = { selectedStatus = DownloadStatus.COMPLETED },
                    label = {
                        Text("Completed (${downloads.count { it.status == DownloadStatus.COMPLETED }})")
                    }
                )

                FilterChip(
                    selected = selectedStatus == DownloadStatus.FAILED,
                    onClick = { selectedStatus = DownloadStatus.FAILED },
                    label = {
                        Text("Failed (${downloads.count { it.status == DownloadStatus.FAILED }})")
                    }
                )
            }

            HorizontalDivider()

            // Downloads list
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    error != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(onClick = { viewModel.loadDownloads() }) {
                                Text("Retry")
                            }
                        }
                    }

                    filteredDownloads.isEmpty() -> {
                        EmptyDownloadsState(
                            statusFilter = selectedStatus,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(filteredDownloads, key = { it.id }) { download ->
                                DownloadItem(
                                    download = download,
                                    onClick = { onDownloadClick(download) },
                                    onCancel = { viewModel.cancelDownload(download.id) },
                                    onRetry = { viewModel.retryDownload(download.id) },
                                    onDelete = { viewModel.deleteDownload(download.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Clear completed dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Completed Downloads?") },
            text = {
                Text("This will remove all completed downloads from the list. Files will remain on disk.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCompletedDownloads()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * EmptyDownloadsState - Shown when no downloads match filter
 */
@Composable
fun EmptyDownloadsState(
    statusFilter: DownloadStatus?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val message = when (statusFilter) {
            null -> "No downloads yet"
            DownloadStatus.DOWNLOADING -> "No active downloads"
            DownloadStatus.COMPLETED -> "No completed downloads"
            DownloadStatus.FAILED -> "No failed downloads"
            else -> "No downloads"
        }

        Text(
            text = message,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = when (statusFilter) {
                null -> "Downloaded files will appear here"
                DownloadStatus.DOWNLOADING -> "Start a download to see it here"
                DownloadStatus.COMPLETED -> "Completed downloads will appear here"
                DownloadStatus.FAILED -> "Failed downloads will appear here"
                else -> ""
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
