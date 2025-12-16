// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/ui/DocumentManagementScreen.kt
// created: 2025-11-06
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.ava.features.rag.domain.Document
import com.augmentalis.ava.features.rag.domain.DocumentStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * RAG Document Management Screen
 *
 * Features:
 * - List all indexed documents
 * - Add documents (files or URLs)
 * - Delete documents
 * - Rebuild clusters
 * - Processing status display
 * - Adaptive layout: Grid in landscape, list in portrait
 *
 * Usage:
 * ```kotlin
 * val viewModel = remember { DocumentManagementViewModel(repository) }
 * DocumentManagementScreen(viewModel = viewModel)
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentManagementScreen(
    viewModel: DocumentManagementViewModel,
    onNavigateToSearch: () -> Unit = {}
) {
    val documents by viewModel.documents.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val processingProgress by viewModel.processingProgress.collectAsState()
    val error by viewModel.error.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val clusterStats by viewModel.clusterStats.collectAsState()
    val windowSizeClass = rememberWindowSizeClass()

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<Document?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RAG Documents", color = Color.White) },
                actions = {
                    // Search button
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, "Search", tint = Color.White)
                    }
                    // Rebuild clusters button
                    IconButton(
                        onClick = { viewModel.rebuildClusters() },
                        enabled = !isProcessing && documents.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            "Rebuild Clusters",
                            tint = Color.White.copy(alpha = if (!isProcessing && documents.isNotEmpty()) 1f else 0.5f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.gradientBackground()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.gradientBackground(),
                containerColor = Color.Transparent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Add Document")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Processing indicator
            if (isProcessing) {
                LinearProgressIndicator(
                    progress = processingProgress,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Status message
            statusMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearStatus() }) {
                            Icon(Icons.Default.Close, "Dismiss")
                        }
                    }
                }
            }

            // Error message
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, "Dismiss")
                        }
                    }
                }
            }

            // Cluster stats
            clusterStats?.let { stats ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Clusters", stats.clusterCount.toString())
                        StatItem("Chunks", stats.chunkCount.toString())
                        StatItem("Avg Size", stats.avgClusterSize.toString())
                        StatItem("Time", "${stats.timeMs}ms")
                    }
                }
            }

            // Document list
            if (documents.isEmpty() && !isProcessing) {
                EmptyState(onAddDocument = { showAddDialog = true })
            } else {
                if (windowSizeClass.isLandscape && windowSizeClass.isMediumOrExpandedWidth) {
                    // LANDSCAPE: Grid layout (2-3 columns)
                    DocumentGrid(
                        documents = documents,
                        columns = if (windowSizeClass.isExpandedWidth) 3 else 2,
                        onDelete = { document -> showDeleteConfirmation = document }
                    )
                } else {
                    // PORTRAIT: List layout
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(documents, key = { it.id }) { document ->
                            DocumentCard(
                                document = document,
                                onDelete = { showDeleteConfirmation = document }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add document dialog
    if (showAddDialog) {
        AddDocumentDialog(
            onDismiss = { showAddDialog = false },
            onAddFile = { filePath, title ->
                viewModel.addDocument(filePath, title)
                showAddDialog = false
            },
            onAddUrl = { url, title ->
                viewModel.addDocumentFromUrl(url, title)
                showAddDialog = false
            }
        )
    }

    // Delete confirmation dialog
    showDeleteConfirmation?.let { document ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Delete Document?") },
            text = { Text("Are you sure you want to delete \"${document.title}\"? This will remove all associated chunks.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDocument(document)
                        showDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(onAddDocument: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Documents",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add documents to enable RAG search",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddDocument) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Document")
        }
    }
}

/**
 * Document grid for landscape mode
 */
@Composable
private fun DocumentGrid(
    documents: List<Document>,
    columns: Int,
    onDelete: (Document) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Group documents into rows
        val rows = documents.chunked(columns)
        items(rows.size) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rows[rowIndex].forEach { document ->
                    Box(modifier = Modifier.weight(1f)) {
                        DocumentCard(
                            document = document,
                            onDelete = { onDelete(document) }
                        )
                    }
                }
                // Fill empty spaces in the last row
                repeat(columns - rows[rowIndex].size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(
    document: Document,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (document.status) {
                DocumentStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                DocumentStatus.PROCESSING -> MaterialTheme.colorScheme.tertiaryContainer
                DocumentStatus.INDEXED -> MaterialTheme.colorScheme.surface
                DocumentStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                DocumentStatus.OUTDATED -> MaterialTheme.colorScheme.secondaryContainer
                DocumentStatus.DELETED -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document icon
            Icon(
                imageVector = when (document.fileType.name) {
                    "PDF" -> Icons.Default.PictureAsPdf
                    "HTML" -> Icons.Default.Language
                    "DOCX" -> Icons.Default.Description
                    else -> Icons.Default.Description
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Document info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${document.chunkCount} chunks • ${document.fileType.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Added ${dateFormat.format(Date(document.createdAt.toEpochMilliseconds()))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status indicator
            if (document.status == DocumentStatus.PROCESSING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else if (document.status == DocumentStatus.FAILED) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Failed",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Dialog for adding documents (file or URL)
 */
@Composable
private fun AddDocumentDialog(
    onDismiss: () -> Unit,
    onAddFile: (String, String) -> Unit,
    onAddUrl: (String, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var fileInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Document") },
        text = {
            Column {
                // Tab selector
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("File") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("URL") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title input (common)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tab content
                when (selectedTab) {
                    0 -> {
                        // File tab
                        OutlinedTextField(
                            value = fileInput,
                            onValueChange = { fileInput = it },
                            label = { Text("File Path") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("/path/to/document.pdf") },
                            supportingText = { Text("Supports PDF, DOCX, TXT") }
                        )
                    }
                    1 -> {
                        // URL tab
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("URL") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://example.com/doc") },
                            supportingText = { Text("Supports HTML, PDF URLs") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (selectedTab) {
                        0 -> if (fileInput.isNotBlank()) onAddFile(fileInput, title.ifBlank { "Untitled Document" })
                        1 -> if (urlInput.isNotBlank()) onAddUrl(urlInput, title.ifBlank { "Untitled Document" })
                    }
                },
                enabled = when (selectedTab) {
                    0 -> fileInput.isNotBlank()
                    1 -> urlInput.isNotBlank()
                    else -> false
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
