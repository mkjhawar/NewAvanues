package com.augmentalis.cockpit.mvp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.avanues.cockpit.core.window.WindowType
import com.augmentalis.cockpit.mvp.components.GlassmorphicSurface

@Composable
fun ControlPanel(
    windowCount: Int,
    maxWindows: Int = 6,
    onAddWindow: (title: String, type: WindowType, color: String) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    // Get bottom insets to avoid navigation bar and add padding
    val bottomInset = with(androidx.compose.ui.platform.LocalDensity.current) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    }

    // Floating pill-shaped command bar with glassmorphic styling (75% size)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                bottom = OceanTheme.spacingDefault + bottomInset,
                top = OceanTheme.spacingMedium
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassmorphicSurface(
            modifier = Modifier.wrapContentWidth(),
            shape = OceanTheme.glassShapeLarge,
            tonalElevation = OceanTheme.elevationHigh,
            shadowElevation = OceanTheme.shadowElevationDefault
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = OceanTheme.spacingMedium, vertical = OceanTheme.spacingSmall),
                horizontalArrangement = Arrangement.spacedBy(OceanTheme.spacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add window icon button (75% size)
                IconButton(
                    onClick = { showAddDialog = true },
                    enabled = windowCount < maxWindows,
                    modifier = Modifier.size(OceanTheme.buttonSizeSmall)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Window",
                        tint = if (windowCount < maxWindows) OceanTheme.primary else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(OceanTheme.iconSizeSmall)
                    )
                }

                // Divider (75% size)
                Box(
                    modifier = Modifier
                        .width(OceanTheme.borderWidthDefault)
                        .height(OceanTheme.iconSizeSmall)
                        .background(OceanTheme.glassBorder)
                )

                // Reset icon button (75% size)
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(OceanTheme.buttonSizeSmall)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(OceanTheme.iconSizeSmall)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddWindowDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, type, color ->
                onAddWindow(title, type, color)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddWindowDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, WindowType, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(WindowType.ANDROID_APP) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Window") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Window Title") },
                    singleLine = true
                )

                Text("Window Type:", style = MaterialTheme.typography.labelMedium)
                WindowType.values().forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(getTypeLabel(type))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val color = when (selectedType) {
                            WindowType.ANDROID_APP -> "#FF6B9D"
                            WindowType.WEB_APP -> "#4ECDC4"
                            WindowType.WIDGET -> "#95E1D3"
                            WindowType.REMOTE_DESKTOP -> "#FFD93D"
                        }
                        onConfirm(title, selectedType, color)
                    }
                },
                enabled = title.isNotBlank()
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

private fun getTypeLabel(type: WindowType): String = when (type) {
    WindowType.ANDROID_APP -> "Android App"
    WindowType.WEB_APP -> "Web App"
    WindowType.WIDGET -> "Widget"
    WindowType.REMOTE_DESKTOP -> "Remote Desktop"
}
