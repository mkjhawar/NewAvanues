package com.augmentalis.webavanue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import com.augmentalis.avanueui.theme.AvanueTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.TabGroupColor

/**
 * TabGroupDialog - Create or edit a tab group
 *
 * Features:
 * - Title input field
 * - Color picker (8 colors)
 * - Save/Cancel buttons
 * - Voice command: "create group [name]"
 *
 * @param visible Whether dialog is visible
 * @param initialTitle Initial title (empty for new group)
 * @param initialColor Initial color (BLUE for new group)
 * @param onSave Callback when save is clicked (title, color)
 * @param onCancel Callback when cancel is clicked
 * @param modifier Modifier for customization
 */
@Composable
fun TabGroupDialog(
    visible: Boolean,
    initialTitle: String = "",
    initialColor: TabGroupColor = TabGroupColor.BLUE,
    onSave: (title: String, color: TabGroupColor) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    var title by remember { mutableStateOf(initialTitle) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = if (initialTitle.isBlank()) "Create Tab Group" else "Edit Tab Group",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title input
                Text(
                    text = "Group Name",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("e.g. Work, Shopping, Research")
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AvanueTheme.colors.primary,
                        unfocusedBorderColor = AvanueTheme.colors.border,
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
                    )
                )

                // Color picker
                Text(
                    text = "Group Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(TabGroupColor.entries) { color ->
                        ColorOption(
                            color = color,
                            isSelected = color == selectedColor,
                            onClick = { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, selectedColor)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save", color = AvanueTheme.colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = AvanueTheme.colors.surface,
        modifier = modifier
    )
}

/**
 * Color option in the color picker
 */
@Composable
private fun ColorOption(
    color: TabGroupColor,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(android.graphics.Color.parseColor(color.colorHex)))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
