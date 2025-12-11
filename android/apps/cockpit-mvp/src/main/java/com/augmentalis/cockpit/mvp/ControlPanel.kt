package com.augmentalis.cockpit.mvp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.avanues.cockpit.core.window.WindowContent
import com.avanues.cockpit.core.window.WindowType
import com.augmentalis.cockpit.mvp.components.GlassmorphicSurface

/**
 * Control panel with add and reset buttons
 * Includes haptic feedback for all interactions
 */
@Composable
fun ControlPanel(
    windowCount: Int,
    maxWindows: Int = 6,
    onAddWindow: (title: String, type: WindowType, color: String, content: WindowContent) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticFeedbackManager(context) }
    val presetManager = remember { WindowPresetManager(context) }
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
                    onClick = {
                        if (windowCount < maxWindows) {
                            hapticManager.performSuccess()
                            showAddDialog = true
                        } else {
                            hapticManager.performError()
                        }
                    },
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
                    onClick = {
                        hapticManager.performMediumTap()
                        onReset()
                    },
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
            presetManager = presetManager,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, type, color, content ->
                onAddWindow(title, type, color, content)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddWindowDialog(
    presetManager: WindowPresetManager,
    onDismiss: () -> Unit,
    onConfirm: (String, WindowType, String, WindowContent) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(WindowType.WEB_APP) }
    var url by remember { mutableStateOf("https://google.com") }
    var packageName by remember { mutableStateOf("com.android.calculator2") }
    var saveAsPreset by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("default") }
    val presets = remember { presetManager.loadPresets() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Window") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Preset List Section
                if (presets.isNotEmpty()) {
                    Text("Saved Presets:", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(presets) { preset ->
                            PresetRow(
                                preset = preset,
                                onClick = {
                                    title = preset.title
                                    selectedType = preset.type
                                    when (val content = preset.content) {
                                        is WindowContent.WebContent -> url = content.url
                                        is WindowContent.FreeformAppContent -> packageName = content.packageName
                                        else -> {}
                                    }
                                }
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Window Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Window Title") },
                    singleLine = true
                )

                // Window Type Selection
                Text("Window Type:", style = MaterialTheme.typography.labelMedium)
                WindowType.values()
                    .filter { it != WindowType.WIDGET }
                    .forEach { type ->
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

                // URL Input (WEB_APP)
                if (selectedType == WindowType.WEB_APP) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        singleLine = true,
                        placeholder = { Text("https://example.com") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (title.isNotBlank()) {
                                    val color = when (selectedType) {
                                        WindowType.ANDROID_APP -> "#FF6B9D"
                                        WindowType.WEB_APP -> "#4ECDC4"
                                        WindowType.WIDGET -> "#95E1D3"
                                        WindowType.REMOTE_DESKTOP -> "#FFD93D"
                                    }
                                    val content = when (selectedType) {
                                        WindowType.WEB_APP -> WindowContent.WebContent(url)
                                        WindowType.ANDROID_APP -> WindowContent.FreeformAppContent(packageName)
                                        else -> WindowContent.MockContent
                                    }

                                    // Save preset if checkbox is checked
                                    if (saveAsPreset) {
                                        presetManager.savePreset(
                                            title = title,
                                            type = selectedType,
                                            content = content,
                                            groupName = groupName,
                                            color = color
                                        )
                                    }

                                    onConfirm(title, selectedType, color, content)
                                }
                            }
                        )
                    )
                }

                // Package Name Input (ANDROID_APP)
                if (selectedType == WindowType.ANDROID_APP) {
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package Name") },
                        singleLine = true,
                        placeholder = { Text("com.example.app") }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Save as Preset Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { saveAsPreset = !saveAsPreset }
                ) {
                    Checkbox(
                        checked = saveAsPreset,
                        onCheckedChange = { saveAsPreset = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save as preset")
                }

                // Group Name (only visible when saving preset)
                if (saveAsPreset) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        placeholder = { Text("default") }
                    )
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

                        // Create WindowContent based on type
                        val content = when (selectedType) {
                            WindowType.WEB_APP -> WindowContent.WebContent(url)
                            WindowType.ANDROID_APP -> WindowContent.FreeformAppContent(packageName)
                            else -> WindowContent.MockContent
                        }

                        // Save preset if checkbox is checked
                        if (saveAsPreset) {
                            presetManager.savePreset(
                                title = title,
                                type = selectedType,
                                content = content,
                                groupName = groupName,
                                color = color
                            )
                        }

                        onConfirm(title, selectedType, color, content)
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
    WindowType.WEB_APP -> "Web Page (URL)"
    WindowType.WIDGET -> "Widget"
    WindowType.REMOTE_DESKTOP -> "Remote Desktop"
}

@Composable
private fun PresetRow(
    preset: com.avanues.cockpit.presets.WindowPreset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color(android.graphics.Color.parseColor(preset.color)).copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${getTypeLabel(preset.type)} • ${preset.groupName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = Color(android.graphics.Color.parseColor(preset.color)),
                        shape = MaterialTheme.shapes.extraSmall
                    )
            )
        }
    }
}
