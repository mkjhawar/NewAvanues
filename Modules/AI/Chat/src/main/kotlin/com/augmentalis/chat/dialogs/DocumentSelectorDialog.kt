// filename: Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/ui/dialogs/DocumentSelectorDialog.kt
// created: 2025-11-22
// author: RAG Settings Integration Specialist
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.chat.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.rag.domain.Document
import com.augmentalis.rag.domain.DocumentStatus

/**
 * Document Selector Dialog for RAG Settings (Phase 2 - Task 1)
 *
 * Multi-select dialog for choosing documents to use in RAG.
 *
 * Features:
 * - Multi-select with checkboxes
 * - Document title, size, and status display
 * - Search/filter capability
 * - Empty state handling
 * - Material 3 design
 *
 * Usage:
 * ```kotlin
 * if (showDialog) {
 *     DocumentSelectorDialog(
 *         documents = availableDocuments,
 *         selectedDocumentIds = selectedIds,
 *         onDismiss = { showDialog = false },
 *         onConfirm = { selectedIds ->
 *             viewModel.setSelectedDocumentIds(selectedIds)
 *             showDialog = false
 *         }
 *     )
 * }
 * ```
 */
@Composable
fun DocumentSelectorDialog(
    documents: List<Document>,
    selectedDocumentIds: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSelection by remember { mutableStateOf(selectedDocumentIds) }
    var searchQuery by remember { mutableStateOf("") }

    // Filter documents based on search query
    val filteredDocuments = remember(documents, searchQuery) {
        if (searchQuery.isBlank()) {
            documents
        } else {
            documents.filter { doc ->
                doc.title.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Only show indexed documents (ready to use)
    val indexedDocuments = remember(filteredDocuments) {
        filteredDocuments.filter { it.status == DocumentStatus.INDEXED }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = "Select Documents",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${currentSelection.size} document(s) selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textPrimary.copy(alpha = 0.7f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search documents...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    }
                )

                HorizontalDivider()

                // Document list
                if (indexedDocuments.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = AvanueTheme.colors.textPrimary.copy(alpha = 0.4f)
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) {
                                    "No documents match your search"
                                } else {
                                    "No indexed documents available"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = AvanueTheme.colors.textPrimary.copy(alpha = 0.6f)
                            )
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Add and index documents first",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AvanueTheme.colors.textPrimary.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(indexedDocuments) { document ->
                            DocumentListItem(
                                document = document,
                                isSelected = currentSelection.contains(document.id),
                                onSelectionChange = { isSelected ->
                                    currentSelection = if (isSelected) {
                                        currentSelection + document.id
                                    } else {
                                        currentSelection - document.id
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(currentSelection) },
                enabled = indexedDocuments.isNotEmpty()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Individual document list item with checkbox
 */
@Composable
private fun DocumentListItem(
    document: Document,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelectionChange(!isSelected) },
        color = if (isSelected) {
            AvanueTheme.colors.primaryContainer
        } else {
            AvanueTheme.colors.surface
        },
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) {
                        AvanueTheme.colors.onPrimaryContainer
                    } else {
                        AvanueTheme.colors.textPrimary
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = document.fileType.extension.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            AvanueTheme.colors.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            AvanueTheme.colors.textPrimary.copy(alpha = 0.6f)
                        }
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            AvanueTheme.colors.onPrimaryContainer.copy(alpha = 0.5f)
                        } else {
                            AvanueTheme.colors.textPrimary.copy(alpha = 0.4f)
                        }
                    )

                    Text(
                        text = formatFileSize(document.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            AvanueTheme.colors.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            AvanueTheme.colors.textPrimary.copy(alpha = 0.6f)
                        }
                    )

                    if (document.chunkCount > 0) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) {
                                AvanueTheme.colors.onPrimaryContainer.copy(alpha = 0.5f)
                            } else {
                                AvanueTheme.colors.textPrimary.copy(alpha = 0.4f)
                            }
                        )

                        Text(
                            text = "${document.chunkCount} chunks",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) {
                                AvanueTheme.colors.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                AvanueTheme.colors.textPrimary.copy(alpha = 0.6f)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Format file size in human-readable format
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
