package com.augmentalis.webavanue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dialog shown when askDownloadLocation setting is enabled
 *
 * Prompts user to select download location before starting each download.
 * Provides option to remember the selection and skip future prompts.
 *
 * ## Features
 * - Shows filename being downloaded
 * - Displays current selected path (or default)
 * - "Change" button to launch file picker
 * - "Remember" checkbox to save selection to settings
 * - "Download" button to proceed
 * - "Cancel" button to abort download
 *
 * ## Usage
 * ```kotlin
 * var showDialog by remember { mutableStateOf(false) }
 *
 * if (showDialog) {
 *     AskDownloadLocationDialog(
 *         filename = "document.pdf",
 *         defaultPath = settings.downloadPath,
 *         onPathSelected = { path, remember ->
 *             if (remember) {
 *                 // Save to settings
 *                 updateSettings(settings.copy(downloadPath = path))
 *             }
 *             startDownload(path)
 *             showDialog = false
 *         },
 *         onCancel = {
 *             showDialog = false
 *         }
 *     )
 * }
 * ```
 *
 * @param filename Name of file being downloaded (shown in dialog title)
 * @param defaultPath Current default download path (null = system default)
 * @param selectedPath Currently selected custom path (null = use default)
 * @param onLaunchFilePicker Callback to launch file picker, returns selected URI
 * @param onPathSelected Callback when user confirms: (path, rememberChoice)
 * @param onCancel Callback when user cancels (download will not start)
 * @param modifier Modifier for dialog customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskDownloadLocationDialog(
    filename: String,
    defaultPath: String?,
    selectedPath: String? = null,
    onLaunchFilePicker: () -> Unit,
    onPathSelected: (String, rememberChoice: Boolean) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State: Remember choice checkbox
    var rememberChoice by remember { mutableStateOf(false) }

    // Display path: selected path or default
    val displayPath = selectedPath ?: defaultPath ?: "Downloads"

    AlertDialog(
        onDismissRequest = onCancel,
        modifier = modifier,
        title = {
            Text(
                text = "Save Download",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filename display
                Text(
                    text = "Save \"$filename\" to:",
                    style = MaterialTheme.typography.bodyLarge
                )

                // Path selector card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Folder icon + path
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = displayPath,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Change button
                        IconButton(
                            onClick = onLaunchFilePicker
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Change location",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Remember choice checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it }
                    )
                    Text(
                        text = "Always use this location",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Explanation text (shown when checkbox checked)
                if (rememberChoice) {
                    Text(
                        text = "This location will be saved to settings and used for all future downloads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPath = selectedPath ?: defaultPath ?: ""
                    onPathSelected(finalPath, rememberChoice)
                }
            ) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Preview variant for development and testing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AskDownloadLocationDialogPreview() {
    MaterialTheme {
        AskDownloadLocationDialog(
            filename = "example-document.pdf",
            defaultPath = "Downloads",
            selectedPath = null,
            onLaunchFilePicker = {},
            onPathSelected = { _, _ -> },
            onCancel = {}
        )
    }
}
