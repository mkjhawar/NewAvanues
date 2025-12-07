package com.augmentalis.Avanues.web.universal.presentation.ui.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * AddToFavoritesDialog - Dialog for adding/editing favorites
 *
 * Features:
 * - Edit title and URL
 * - Optional description
 * - Folder selection (TODO)
 * - Tags (TODO)
 * - Save and Cancel buttons
 * - Dark 3D theme
 *
 * @param visible Whether dialog is visible
 * @param initialTitle Initial title (from page)
 * @param initialUrl Initial URL (from page)
 * @param onSave Callback with title, url, description
 * @param onCancel Callback when user cancels
 * @param modifier Modifier for customization
 */
@Composable
fun AddToFavoritesDialog(
    visible: Boolean,
    initialTitle: String = "",
    initialUrl: String = "",
    onSave: (title: String, url: String, description: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    var description by remember { mutableStateOf("") }

    // Dark 3D theme colors
    val bgDialog = Color(0xFF0F3460).copy(alpha = 0.98f)
    val bgSurface = Color(0xFF16213E)
    val accentColor = Color(0xFF60A5FA)

    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = modifier.width(360.dp),
            shape = RoundedCornerShape(16.dp),
            color = bgDialog,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with icon
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
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
                    placeholder = { Text("Page title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF2D4A6F),
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = Color(0xFFA0A0A0),
                        focusedTextColor = Color(0xFFE8E8E8),
                        unfocusedTextColor = Color(0xFFE8E8E8),
                        cursorColor = accentColor,
                        focusedPlaceholderColor = Color(0xFF6C6C6C),
                        unfocusedPlaceholderColor = Color(0xFF6C6C6C)
                    )
                )

                // URL field
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF2D4A6F),
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = Color(0xFFA0A0A0),
                        focusedTextColor = Color(0xFFE8E8E8),
                        unfocusedTextColor = Color(0xFFE8E8E8),
                        cursorColor = accentColor,
                        focusedPlaceholderColor = Color(0xFF6C6C6C),
                        unfocusedPlaceholderColor = Color(0xFF6C6C6C)
                    )
                )

                // Description field (optional)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    label = { Text("Description (optional)") },
                    placeholder = { Text("Add notes about this favorite") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF2D4A6F),
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = Color(0xFFA0A0A0),
                        focusedTextColor = Color(0xFFE8E8E8),
                        unfocusedTextColor = Color(0xFFE8E8E8),
                        cursorColor = accentColor,
                        focusedPlaceholderColor = Color(0xFF6C6C6C),
                        unfocusedPlaceholderColor = Color(0xFF6C6C6C)
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
                        Text("Save")
                    }
                }
            }
        }
    }
}
