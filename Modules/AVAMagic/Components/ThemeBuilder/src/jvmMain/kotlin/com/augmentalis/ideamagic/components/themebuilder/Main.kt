package com.augmentalis.avanues.avamagic.components.themebuilder

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.augmentalis.avaelements.core.Theme
import com.augmentalis.avaelements.core.ThemePlatform
import com.augmentalis.avaelements.core.types.Color as AvaColor
import com.augmentalis.avanues.avamagic.components.themebuilder.UI.ThemeEditorWindow
import com.augmentalis.avanues.avamagic.components.themebuilder.UI.PropertyInspector
import com.augmentalis.avanues.avamagic.components.themebuilder.UI.PreviewCanvas
import com.augmentalis.avanues.avamagic.components.themebuilder.UI.ThemePresets
import com.augmentalis.avanues.avamagic.components.themebuilder.Engine.ExportFormat
import com.augmentalis.avanues.avamagic.components.themebuilder.State.ThemeBuilderState
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement as ComposeArrangement

/**
 * Main entry point for the Theme Builder desktop application
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AvaElements Theme Builder",
        state = rememberWindowState(width = 1600.dp, height = 900.dp)
    ) {
        ThemeBuilderApp()
    }
}

/**
 * Main Theme Builder application composable
 */
@Composable
@Preview
fun ThemeBuilderApp() {
    val editorWindow = remember { ThemeEditorWindow() }
    val state by editorWindow.state.collectAsState()
    val scope = rememberCoroutineScope()

    MaterialTheme(
        colorScheme = if (state.isDarkMode) darkColorScheme() else lightColorScheme()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(editorWindow, state)
            }
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Left Panel: Component Gallery (20%)
                ComponentGalleryPanel(
                    editorWindow = editorWindow,
                    state = state,
                    modifier = Modifier.weight(0.2f)
                )

                Divider(modifier = Modifier.width(1.dp).fillMaxHeight())

                // Center Panel: Preview Canvas (50%)
                PreviewCanvasPanel(
                    editorWindow = editorWindow,
                    state = state,
                    modifier = Modifier.weight(0.5f)
                )

                Divider(modifier = Modifier.width(1.dp).fillMaxHeight())

                // Right Panel: Property Inspector (30%)
                PropertyInspectorPanel(
                    editorWindow = editorWindow,
                    state = state,
                    modifier = Modifier.weight(0.3f)
                )
            }
        }
    }
}

/**
 * Top app bar with actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(editorWindow: ThemeEditorWindow, state: ThemeBuilderState) {
    val scope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text("AvaElements Theme Builder", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Editing: ${state.currentTheme.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            // Undo
            IconButton(
                onClick = { editorWindow.undo() },
                enabled = editorWindow.stateManager.canUndo()
            ) {
                Icon(Icons.Default.Undo, "Undo")
            }

            // Redo
            IconButton(
                onClick = { editorWindow.redo() },
                enabled = editorWindow.stateManager.canRedo()
            ) {
                Icon(Icons.Default.Redo, "Redo")
            }

            Divider(modifier = Modifier.width(1.dp).height(32.dp).padding(horizontal = 8.dp))

            // Toggle Dark Mode
            IconButton(onClick = { editorWindow.toggleDarkMode() }) {
                Icon(
                    if (state.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    "Toggle Dark Mode"
                )
            }

            // Toggle Grid
            IconButton(onClick = { editorWindow.toggleGrid() }) {
                Icon(Icons.Default.GridOn, "Toggle Grid", tint = if (state.showGrid) MaterialTheme.colorScheme.primary else LocalContentColor.current)
            }

            Divider(modifier = Modifier.width(1.dp).height(32.dp).padding(horizontal = 8.dp))

            // Load Preset
            IconButton(onClick = { showPresetDialog = true }) {
                Icon(Icons.Default.Palette, "Load Preset")
            }

            // Export
            Button(
                onClick = { showExportDialog = true },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Download, "Export", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export")
            }

            // Save
            Button(
                onClick = {
                    scope.launch {
                        if (editorWindow.saveTheme()) {
                            // Show success message
                        }
                    }
                },
                enabled = state.isDirty
            ) {
                Icon(Icons.Default.Save, "Save", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save")
            }
        }
    )

    // Export Dialog
    if (showExportDialog) {
        ExportDialog(
            editorWindow = editorWindow,
            onDismiss = { showExportDialog = false }
        )
    }

    // Preset Dialog
    if (showPresetDialog) {
        PresetDialog(
            editorWindow = editorWindow,
            onDismiss = { showPresetDialog = false }
        )
    }
}

/**
 * Component Gallery Panel
 */
@Composable
fun ComponentGalleryPanel(
    editorWindow: ThemeEditorWindow,
    state: ThemeBuilderState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            "Component Gallery",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        val components = editorWindow.componentGallery.getComponents()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            components.forEach { (category, items) ->
                Text(
                    category,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                items.forEach { component ->
                    ComponentItem(
                        component = component,
                        isSelected = state.selectedComponent == component.name,
                        onClick = { editorWindow.componentGallery.selectComponent(component.name) }
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Component item in the gallery
 */
@Composable
fun ComponentItem(
    component: PreviewCanvas.ComponentPreview,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                component.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                component.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Preview Canvas Panel
 */
@Composable
fun PreviewCanvasPanel(
    editorWindow: ThemeEditorWindow,
    state: ThemeBuilderState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            "Preview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        // Preview area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color(
                        red = state.currentTheme.colorScheme.background.red / 255f,
                        green = state.currentTheme.colorScheme.background.green / 255f,
                        blue = state.currentTheme.colorScheme.background.blue / 255f,
                        alpha = state.currentTheme.colorScheme.background.alpha
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Live Preview\n\n(Component rendering requires platform-specific implementation)",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(
                    red = state.currentTheme.colorScheme.onBackground.red / 255f,
                    green = state.currentTheme.colorScheme.onBackground.green / 255f,
                    blue = state.currentTheme.colorScheme.onBackground.blue / 255f
                )
            )
        }
    }
}

/**
 * Property Inspector Panel
 */
@Composable
fun PropertyInspectorPanel(
    editorWindow: ThemeEditorWindow,
    state: ThemeBuilderState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Properties",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        // Color Scheme Section
        PropertySection(
            title = "Color Scheme",
            editorWindow = editorWindow,
            category = PropertyInspector.PropertyCategory.COLOR_SCHEME
        )

        Spacer(Modifier.height(16.dp))

        // Typography Section
        PropertySection(
            title = "Typography",
            editorWindow = editorWindow,
            category = PropertyInspector.PropertyCategory.TYPOGRAPHY
        )

        Spacer(Modifier.height(16.dp))

        // Spacing Section
        PropertySection(
            title = "Spacing",
            editorWindow = editorWindow,
            category = PropertyInspector.PropertyCategory.SPACING
        )
    }
}

/**
 * Property section
 */
@Composable
fun PropertySection(
    title: String,
    editorWindow: ThemeEditorWindow,
    category: PropertyInspector.PropertyCategory
) {
    val properties = editorWindow.propertyInspector.getPropertiesByCategory(category)

    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        properties.take(5).forEach { property ->
            PropertyRow(property, editorWindow)
        }

        if (properties.size > 5) {
            Text(
                "+${properties.size - 5} more...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Property row
 */
@Composable
fun PropertyRow(
    property: PropertyInspector.PropertyDef,
    editorWindow: ThemeEditorWindow
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = ComposeArrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            property.displayName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )

        // Simple color preview for color properties
        if (property.type is PropertyInspector.PropertyType.ColorType) {
            val currentValue = editorWindow.propertyInspector.getCurrentValue(property.name)
            if (currentValue is AvaColor) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            Color(
                                red = currentValue.red / 255f,
                                green = currentValue.green / 255f,
                                blue = currentValue.blue / 255f,
                                alpha = currentValue.alpha
                            ),
                            RoundedCornerShape(4.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

/**
 * Export dialog
 */
@Composable
fun ExportDialog(
    editorWindow: ThemeEditorWindow,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.DSL) }
    var exportedCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Theme") },
        text = {
            Column {
                Text("Select export format:")
                Spacer(Modifier.height(16.dp))

                ExportFormat.values().forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = format }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(format.name)
                    }
                }

                if (exportedCode.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Preview:", style = MaterialTheme.typography.labelMedium)
                    Text(
                        exportedCode.take(200) + if (exportedCode.length > 200) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    exportedCode = editorWindow.exportTheme(selectedFormat)
                }
            ) {
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Preset selection dialog
 */
@Composable
fun PresetDialog(
    editorWindow: ThemeEditorWindow,
    onDismiss: () -> Unit
) {
    val presets = ThemePresets.getAllPresets()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Load Theme Preset") },
        text = {
            Column {
                presets.forEach { preset ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                editorWindow.loadPredefinedTheme(preset.platform)
                                onDismiss()
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(preset.name, fontWeight = FontWeight.Bold)
                            Text(
                                preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
