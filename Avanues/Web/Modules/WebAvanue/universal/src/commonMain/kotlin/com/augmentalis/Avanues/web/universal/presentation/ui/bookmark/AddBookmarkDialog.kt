package com.augmentalis.Avanues.web.universal.presentation.ui.bookmark

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.domain.model.Favorite

/**
 * AddBookmarkDialog - Dialog for adding or editing bookmarks
 *
 * Features:
 * - URL input field
 * - Title input field
 * - Folder selection (optional)
 * - Create new folder option
 * - Validation
 *
 * @param bookmark Existing bookmark to edit (null for new bookmark)
 * @param folders List of available folder names
 * @param initialFolderName Initial folder name for editing (resolved from folderId)
 * @param onDismiss Callback when dialog is dismissed
 * @param onSave Callback when bookmark is saved (url, title, folderName)
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookmarkDialog(
    bookmark: Favorite? = null,
    folders: List<String> = emptyList(),
    initialFolderName: String? = null,
    onDismiss: () -> Unit,
    onSave: (url: String, title: String, folder: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf(bookmark?.url ?: "") }
    var title by remember { mutableStateOf(bookmark?.title ?: "") }
    var selectedFolder by remember { mutableStateOf<String?>(initialFolderName) }
    var showFolderDropdown by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = if (bookmark != null) "Edit Bookmark" else "Add Bookmark"
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // URL field
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = when {
                            it.isBlank() -> "URL is required"
                            !isValidUrl(it) -> "Invalid URL"
                            else -> null
                        }
                    },
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    isError = urlError != null,
                    supportingText = urlError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = if (it.isBlank()) "Title is required" else null
                    },
                    label = { Text("Title") },
                    placeholder = { Text("My Bookmark") },
                    singleLine = true,
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Folder selection
                // FIX: ExposedDropdownMenuBox is a SubcomposeLayout which doesn't support intrinsic measurements.
                // AlertDialog uses intrinsic measurements to size content, causing crash.
                // Wrapping in Box with explicit size prevents intrinsic measurement queries.
                Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = showFolderDropdown,
                        onExpandedChange = { showFolderDropdown = it },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        OutlinedTextField(
                            value = selectedFolder ?: "No folder",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Folder (Optional)") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = showFolderDropdown
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                    ExposedDropdownMenu(
                        expanded = showFolderDropdown,
                        onDismissRequest = { showFolderDropdown = false }
                    ) {
                        // No folder option
                        DropdownMenuItem(
                            text = { Text("No folder") },
                            onClick = {
                                selectedFolder = null
                                showFolderDropdown = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null
                                )
                            }
                        )

                        // Existing folders
                        folders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder) },
                                onClick = {
                                    selectedFolder = folder
                                    showFolderDropdown = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null
                                    )
                                }
                            )
                        }

                        Divider()

                        // Create new folder
                        DropdownMenuItem(
                            text = { Text("Create new folder...") },
                            onClick = {
                                showFolderDropdown = false
                                showNewFolderDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                    }
                } // End Box wrapper for ExposedDropdownMenuBox
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Validate
                    val hasErrors = when {
                        url.isBlank() -> {
                            urlError = "URL is required"
                            true
                        }
                        !isValidUrl(url) -> {
                            urlError = "Invalid URL"
                            true
                        }
                        title.isBlank() -> {
                            titleError = "Title is required"
                            true
                        }
                        else -> false
                    }

                    if (!hasErrors) {
                        onSave(url, title, selectedFolder)
                    }
                },
                enabled = url.isNotBlank() && title.isNotBlank()
            ) {
                Text(if (bookmark != null) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // New folder dialog
    if (showNewFolderDialog) {
        NewFolderDialog(
            existingFolders = folders,
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { newFolder ->
                selectedFolder = newFolder
                showNewFolderDialog = false
            }
        )
    }
}

/**
 * NewFolderDialog - Dialog for creating a new bookmark folder
 */
@Composable
fun NewFolderDialog(
    existingFolders: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Folder") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = {
                    folderName = it
                    error = when {
                        it.isBlank() -> "Folder name is required"
                        existingFolders.contains(it) -> "Folder already exists"
                        else -> null
                    }
                },
                label = { Text("Folder Name") },
                placeholder = { Text("My Folder") },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(folderName) },
                enabled = folderName.isNotBlank() && !existingFolders.contains(folderName)
            ) {
                Text("Create")
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
 * Simple URL validation
 */
private fun isValidUrl(url: String): Boolean {
    return url.matches(Regex("^https?://.*"))
}
