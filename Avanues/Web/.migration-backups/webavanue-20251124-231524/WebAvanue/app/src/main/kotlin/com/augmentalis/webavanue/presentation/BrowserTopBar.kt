package com.augmentalis.webavanue.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Top bar for the browser with URL input and navigation controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserTopBar(
    currentUrl: String,
    pageTitle: String,
    isLoading: Boolean,
    isFavorite: Boolean,
    onUrlChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onReload: () -> Unit,
    onToggleFavorite: () -> Unit,
    onVoiceCommand: () -> Unit,
    modifier: Modifier = Modifier
) {
    var urlText by remember(currentUrl) { mutableStateOf(currentUrl) }
    var isEditing by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column {
            // Navigation controls and URL bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Back button
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                // Forward button
                IconButton(onClick = onNavigateForward) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward"
                    )
                }

                // URL TextField
                OutlinedTextField(
                    value = if (isEditing) urlText else pageTitle.ifEmpty { currentUrl },
                    onValueChange = { urlText = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Search or enter URL") },
                    leadingIcon = {
                        if (currentUrl.startsWith("https://")) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Secure",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Language,
                                contentDescription = "Web"
                            )
                        }
                    },
                    trailingIcon = {
                        if (isEditing) {
                            IconButton(onClick = {
                                isEditing = false
                                urlText = currentUrl
                            }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Cancel"
                                )
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit URL"
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            val processedUrl = processUrl(urlText)
                            onUrlChange(processedUrl)
                            isEditing = false
                        }
                    )
                )

                // Reload/Stop button
                IconButton(onClick = onReload) {
                    Icon(
                        if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                        contentDescription = if (isLoading) "Stop" else "Reload"
                    )
                }

                // Favorite button
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }

                // Voice command button
                IconButton(onClick = onVoiceCommand) {
                    Icon(
                        Icons.Outlined.Mic,
                        contentDescription = "Voice command"
                    )
                }
            }

            // Loading progress indicator
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Process URL input to ensure proper format
 */
private fun processUrl(input: String): String {
    val trimmed = input.trim()
    return when {
        trimmed.isEmpty() -> "https://www.google.com"
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
        else -> "https://www.google.com/search?q=${trimmed.replace(" ", "+")}"
    }
}