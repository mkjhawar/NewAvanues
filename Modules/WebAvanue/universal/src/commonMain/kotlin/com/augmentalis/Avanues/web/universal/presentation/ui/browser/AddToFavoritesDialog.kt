package com.augmentalis.Avanues.web.universal.presentation.ui.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * AddToFavoritesDialog - Dialog for adding/editing/removing favorites
 *
 * Features:
 * - Shows "Remove from Favorites" if already favorited
 * - Edit title and URL when adding
 * - Optional description
 * - Compact size (not full screen)
 * - Dark 3D theme
 *
 * @param visible Whether dialog is visible
 * @param initialTitle Initial title (from page)
 * @param initialUrl Initial URL (from page)
 * @param isFavorited Whether the page is already favorited
 * @param onSave Callback with title, url, description (only for adding)
 * @param onRemove Callback to remove from favorites
 * @param onCancel Callback when user cancels
 * @param modifier Modifier for customization
 */
@Composable
fun AddToFavoritesDialog(
    visible: Boolean,
    initialTitle: String = "",
    initialUrl: String = "",
    isFavorited: Boolean = false,
    onSave: (title: String, url: String, description: String) -> Unit,
    onRemove: () -> Unit = {},
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    var description by remember { mutableStateOf("") }

    // Dark 3D theme colors
    val bgDialog = Color(0xFF0F3460).copy(alpha = 0.98f)
    val accentColor = Color(0xFF60A5FA)
    val removeColor = Color(0xFFEF5350)

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false  // Control width ourselves
        )
    ) {
        Surface(
            modifier = modifier
                .widthIn(min = 280.dp, max = 320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = bgDialog,
            shadowElevation = 8.dp
        ) {
            if (isFavorited) {
                // Already favorited - show compact remove dialog
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon and title
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "Already in Favorites",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFE8E8E8)
                    )

                    Text(
                        text = initialTitle.ifBlank { initialUrl },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFA0A0A0),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel button
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFA0A0A0)
                            )
                        ) {
                            Text("Keep")
                        }

                        // Remove button
                        Button(
                            onClick = onRemove,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = removeColor,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkRemove,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove")
                        }
                    }
                }
            } else {
                // Not favorited - show add dialog
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with icon
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Add to Favorites",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFE8E8E8)
                        )
                    }

                    // Title field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color(0xFF2D4A6F),
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = Color(0xFFA0A0A0),
                            focusedTextColor = Color(0xFFE8E8E8),
                            unfocusedTextColor = Color(0xFFE8E8E8),
                            cursorColor = accentColor
                        )
                    )

                    // URL field (smaller)
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("URL") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color(0xFF2D4A6F),
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = Color(0xFFA0A0A0),
                            focusedTextColor = Color(0xFFE8E8E8),
                            unfocusedTextColor = Color(0xFFE8E8E8),
                            cursorColor = accentColor
                        )
                    )

                    // Description field (optional, smaller)
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        label = { Text("Notes (optional)") },
                        maxLines = 2,
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color(0xFF2D4A6F),
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = Color(0xFFA0A0A0),
                            focusedTextColor = Color(0xFFE8E8E8),
                            unfocusedTextColor = Color(0xFFE8E8E8),
                            cursorColor = accentColor
                        )
                    )

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        // Cancel button
                        TextButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFFA0A0A0)
                            )
                        ) {
                            Text("Cancel")
                        }

                        // Save button
                        Button(
                            onClick = {
                                if (title.isNotBlank() && url.isNotBlank()) {
                                    onSave(title, url, description)
                                }
                            },
                            enabled = title.isNotBlank() && url.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF2D4A6F),
                                disabledContentColor = Color(0xFF6C6C6C)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
