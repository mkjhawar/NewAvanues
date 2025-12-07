package com.augmentalis.voiceui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

/**
 * MagicThemeCustomizer - Live theme customization with real-time preview
 * 
 * Features:
 * - 🎨 Live color editing
 * - 📐 Shape customization
 * - 📝 Typography adjustment
 * - 👁️ Real-time preview
 * - 💾 Theme export/import
 * - 🎭 Multiple theme presets
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicThemeCustomizer(
    onThemeChanged: (MagicThemeData) -> Unit = {},
    onClose: () -> Unit = {}
) {
    var currentTheme by remember { mutableStateOf(MagicThemeData.default()) }
    var selectedTab by remember { mutableStateOf(ThemeTab.COLORS) }
    var showPreview by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            ThemeCustomizerTopBar(
                currentTheme = currentTheme,
                onClose = onClose,
                onSave = { onThemeChanged(currentTheme) },
                onTogglePreview = { showPreview = !showPreview }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Left Panel - Customization Controls
            Box(
                modifier = Modifier
                    .weight(if (showPreview) 0.5f else 1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Tab Row
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ThemeTab.values().forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.title) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title
                                    )
                                }
                            )
                        }
                    }
                    
                    // Content based on selected tab
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        when (selectedTab) {
                            ThemeTab.COLORS -> ColorCustomizer(
                                theme = currentTheme,
                                onThemeChanged = { currentTheme = it }
                            )
                            ThemeTab.TYPOGRAPHY -> TypographyCustomizer(
                                theme = currentTheme,
                                onThemeChanged = { currentTheme = it }
                            )
                            ThemeTab.SHAPES -> ShapeCustomizer(
                                theme = currentTheme,
                                onThemeChanged = { currentTheme = it }
                            )
                            ThemeTab.EFFECTS -> EffectsCustomizer(
                                theme = currentTheme,
                                onThemeChanged = { currentTheme = it }
                            )
                            ThemeTab.PRESETS -> PresetsSelector(
                                onPresetSelected = { currentTheme = it }
                            )
                        }
                    }
                }
            }
            
            // Right Panel - Live Preview
            if (showPreview) {
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    currentTheme.gradientStart,
                                    currentTheme.gradientEnd
                                )
                            )
                        )
                ) {
                    LivePreview(theme = currentTheme)
                }
            }
        }
    }
}

@Composable
private fun ColorCustomizer(
    theme: MagicThemeData,
    onThemeChanged: (MagicThemeData) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Primary Colors",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            ColorPickerRow(
                label = "Primary",
                color = theme.primary,
                onColorChanged = { onThemeChanged(theme.copy(primary = it)) }
            )
        }
        
        item {
            ColorPickerRow(
                label = "Secondary",
                color = theme.secondary,
                onColorChanged = { onThemeChanged(theme.copy(secondary = it)) }
            )
        }
        
        item {
            ColorPickerRow(
                label = "Tertiary",
                color = theme.tertiary,
                onColorChanged = { onThemeChanged(theme.copy(tertiary = it)) }
            )
        }
        
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                "Gradient Colors",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            ColorPickerRow(
                label = "Gradient Start",
                color = theme.gradientStart,
                onColorChanged = { onThemeChanged(theme.copy(gradientStart = it)) }
            )
        }
        
        item {
            ColorPickerRow(
                label = "Gradient End",
                color = theme.gradientEnd,
                onColorChanged = { onThemeChanged(theme.copy(gradientEnd = it)) }
            )
        }
        
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                "Surface Colors",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            ColorPickerRow(
                label = "Background",
                color = theme.background,
                onColorChanged = { onThemeChanged(theme.copy(background = it)) }
            )
        }
        
        item {
            ColorPickerRow(
                label = "Surface",
                color = theme.surface,
                onColorChanged = { onThemeChanged(theme.copy(surface = it)) }
            )
        }
        
        item {
            ColorPickerRow(
                label = "Card Background",
                color = theme.cardBackground,
                onColorChanged = { onThemeChanged(theme.copy(cardBackground = it)) }
            )
        }
        
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                "Text Colors",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            ColorPickerRow(
                label = "Text Primary",
                color = theme.textPrimary,
                onColorChanged = { onThemeChanged(theme.copy(textPrimary = it)) }
            )
        }
        
        item {
            ColorPickerRow(
                label = "Text Secondary",
                color = theme.textSecondary,
                onColorChanged = { onThemeChanged(theme.copy(textSecondary = it)) }
            )
        }
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    color: Color,
    onColorChanged: (Color) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "#${color.toArgb().toUInt().toString(16).uppercase().padStart(8, '0')}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
            )
        }
    }
    
    if (showDialog) {
        ColorPickerDialog(
            initialColor = color,
            onColorSelected = {
                onColorChanged(it)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun TypographyCustomizer(
    theme: MagicThemeData,
    onThemeChanged: (MagicThemeData) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Font Sizes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            SliderRow(
                label = "Display Large",
                value = theme.displayLargeSize,
                onValueChange = { onThemeChanged(theme.copy(displayLargeSize = it)) },
                valueRange = 40f..80f,
                unit = "sp"
            )
        }
        
        item {
            SliderRow(
                label = "Headline Large",
                value = theme.headlineLargeSize,
                onValueChange = { onThemeChanged(theme.copy(headlineLargeSize = it)) },
                valueRange = 24f..40f,
                unit = "sp"
            )
        }
        
        item {
            SliderRow(
                label = "Body Large",
                value = theme.bodyLargeSize,
                onValueChange = { onThemeChanged(theme.copy(bodyLargeSize = it)) },
                valueRange = 12f..20f,
                unit = "sp"
            )
        }
        
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                "Font Weights",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            FontWeightSelector(
                label = "Headlines",
                selectedWeight = theme.headlineFontWeight,
                onWeightSelected = { onThemeChanged(theme.copy(headlineFontWeight = it)) }
            )
        }
        
        item {
            FontWeightSelector(
                label = "Body Text",
                selectedWeight = theme.bodyFontWeight,
                onWeightSelected = { onThemeChanged(theme.copy(bodyFontWeight = it)) }
            )
        }
    }
}

@Composable
private fun ShapeCustomizer(
    theme: MagicThemeData,
    onThemeChanged: (MagicThemeData) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Corner Radius",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            SliderRow(
                label = "Small Components",
                value = theme.smallCornerRadius,
                onValueChange = { onThemeChanged(theme.copy(smallCornerRadius = it)) },
                valueRange = 0f..20f,
                unit = "dp"
            )
        }
        
        item {
            SliderRow(
                label = "Medium Components",
                value = theme.mediumCornerRadius,
                onValueChange = { onThemeChanged(theme.copy(mediumCornerRadius = it)) },
                valueRange = 0f..32f,
                unit = "dp"
            )
        }
        
        item {
            SliderRow(
                label = "Large Components",
                value = theme.largeCornerRadius,
                onValueChange = { onThemeChanged(theme.copy(largeCornerRadius = it)) },
                valueRange = 0f..50f,
                unit = "dp"
            )
        }
        
        item {
            SliderRow(
                label = "Card Radius",
                value = theme.cardCornerRadius,
                onValueChange = { onThemeChanged(theme.copy(cardCornerRadius = it)) },
                valueRange = 0f..40f,
                unit = "dp"
            )
        }
        
        item {
            SliderRow(
                label = "Button Radius",
                value = theme.buttonCornerRadius,
                onValueChange = { onThemeChanged(theme.copy(buttonCornerRadius = it)) },
                valueRange = 0f..50f,
                unit = "dp"
            )
        }
    }
}

@Composable
private fun EffectsCustomizer(
    theme: MagicThemeData,
    onThemeChanged: (MagicThemeData) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Shadows & Elevation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            SliderRow(
                label = "Card Elevation",
                value = theme.cardElevation,
                onValueChange = { onThemeChanged(theme.copy(cardElevation = it)) },
                valueRange = 0f..24f,
                unit = "dp"
            )
        }
        
        item {
            SliderRow(
                label = "Button Elevation",
                value = theme.buttonElevation,
                onValueChange = { onThemeChanged(theme.copy(buttonElevation = it)) },
                valueRange = 0f..12f,
                unit = "dp"
            )
        }
        
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                "Animations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            SwitchRow(
                label = "Enable Animations",
                checked = theme.animationsEnabled,
                onCheckedChange = { onThemeChanged(theme.copy(animationsEnabled = it)) }
            )
        }
        
        item {
            SliderRow(
                label = "Animation Duration",
                value = theme.animationDuration.toFloat(),
                onValueChange = { onThemeChanged(theme.copy(animationDuration = it.toInt())) },
                valueRange = 100f..1000f,
                unit = "ms"
            )
        }
        
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                "Glassmorphism",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            SwitchRow(
                label = "Enable Glassmorphism",
                checked = theme.glassmorphismEnabled,
                onCheckedChange = { onThemeChanged(theme.copy(glassmorphismEnabled = it)) }
            )
        }
        
        item {
            SliderRow(
                label = "Blur Amount",
                value = theme.blurAmount,
                onValueChange = { onThemeChanged(theme.copy(blurAmount = it)) },
                valueRange = 0f..25f,
                unit = "dp"
            )
        }
    }
}

@Composable
private fun LivePreview(
    theme: MagicThemeData
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PreviewCard(
                theme = theme,
                title = "Preview Card",
                subtitle = "This is how your cards will look"
            )
        }
        
        item {
            PreviewButtons(theme = theme)
        }
        
        item {
            PreviewInputFields(theme = theme)
        }
        
        item {
            PreviewTypography(theme = theme)
        }
        
        item {
            PreviewList(theme = theme)
        }
    }
}

@Composable
private fun PreviewCard(
    theme: MagicThemeData,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = theme.cardElevation.dp,
                shape = RoundedCornerShape(theme.cardCornerRadius.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = theme.cardBackground
        ),
        shape = RoundedCornerShape(theme.cardCornerRadius.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = theme.headlineLargeSize.sp,
                fontWeight = FontWeight(theme.headlineFontWeight),
                color = theme.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = theme.bodyLargeSize.sp,
                fontWeight = FontWeight(theme.bodyFontWeight),
                color = theme.textSecondary
            )
        }
    }
}

@Composable
private fun PreviewButtons(theme: MagicThemeData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = {},
            modifier = Modifier
                .weight(1f)
                .shadow(theme.buttonElevation.dp),
            shape = RoundedCornerShape(theme.buttonCornerRadius.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.primary
            )
        ) {
            Text("Primary Button")
        }
        
        OutlinedButton(
            onClick = {},
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(theme.buttonCornerRadius.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = theme.primary
            )
        ) {
            Text("Outlined Button")
        }
    }
}

@Composable
private fun PreviewInputFields(theme: MagicThemeData) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = "Sample text input",
            onValueChange = {},
            label = { Text("Text Field") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(theme.mediumCornerRadius.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.primary,
                unfocusedBorderColor = theme.textSecondary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun PreviewTypography(theme: MagicThemeData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = theme.surface
        ),
        shape = RoundedCornerShape(theme.cardCornerRadius.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Display Large",
                fontSize = theme.displayLargeSize.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary
            )
            Text(
                "Headline Large",
                fontSize = theme.headlineLargeSize.sp,
                fontWeight = FontWeight(theme.headlineFontWeight),
                color = theme.textPrimary
            )
            Text(
                "Body Large",
                fontSize = theme.bodyLargeSize.sp,
                fontWeight = FontWeight(theme.bodyFontWeight),
                color = theme.textSecondary
            )
        }
    }
}

@Composable
private fun PreviewList(theme: MagicThemeData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = theme.cardBackground
        ),
        shape = RoundedCornerShape(theme.cardCornerRadius.dp)
    ) {
        Column {
            repeat(3) { index ->
                ListItem(
                    headlineContent = { 
                        Text(
                            "List Item ${index + 1}",
                            color = theme.textPrimary
                        )
                    },
                    supportingContent = { 
                        Text(
                            "Supporting text",
                            color = theme.textSecondary
                        )
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(theme.gradientStart, theme.gradientEnd)
                                    )
                                )
                        )
                    }
                )
                if (index < 2) {
                    HorizontalDivider(
                        color = theme.textSecondary.copy(alpha = 0.12f)
                    )
                }
            }
        }
    }
}

// Supporting Components

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text("${value.toInt()}$unit")
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun FontWeightSelector(
    label: String,
    selectedWeight: Int,
    onWeightSelected: (Int) -> Unit
) {
    Column {
        Text(label, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(300, 400, 500, 600, 700).forEach { weight ->
                FilterChip(
                    selected = selectedWeight == weight,
                    onClick = { onWeightSelected(weight) },
                    label = {
                        Text(
                            when (weight) {
                                300 -> "Light"
                                400 -> "Regular"
                                500 -> "Medium"
                                600 -> "SemiBold"
                                700 -> "Bold"
                                else -> weight.toString()
                            }
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeCustomizerTopBar(
    @Suppress("UNUSED_PARAMETER")
    currentTheme: MagicThemeData,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onTogglePreview: () -> Unit
) {
    TopAppBar(
        title = { Text("Magic Theme Customizer") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        },
        actions = {
            IconButton(onClick = onTogglePreview) {
                Icon(Icons.Default.Settings, contentDescription = "Toggle Preview")
            }
            IconButton(onClick = { /* Export theme */ }) {
                Icon(Icons.Default.Share, contentDescription = "Export Theme")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text("Apply Theme")
            }
        }
    )
}

// Placeholder for color picker dialog
@Composable
private fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    // This would be a full color picker implementation
    // For now, using a simple dialog with preset colors
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Select Color",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Grid of color options
                val colors = listOf(
                    Color(0xFF9C88FF), Color(0xFFB794F6), Color(0xFFF687B3),
                    Color(0xFF667EEA), Color(0xFF764BA2), Color(0xFF8B7AE3),
                    Color(0xFFED64A6), Color(0xFF4299E1), Color(0xFF48BB78),
                    Color(0xFFED8936), Color(0xFFF56565), Color(0xFFECC94B)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colors) { color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { onColorSelected(color) }
                                .border(
                                    width = if (color == initialColor) 3.dp else 0.dp,
                                    color = if (color == initialColor) Color.Black else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetsSelector(
    onPresetSelected: (MagicThemeData) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(MagicThemePresets.all) { preset ->
            PresetCard(
                preset = preset,
                onSelect = { onPresetSelected(preset.theme) }
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: ThemePreset,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = preset.theme.cardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    preset.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Color preview circles
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    preset.theme.primary,
                    preset.theme.secondary,
                    preset.theme.tertiary,
                    preset.theme.gradientStart,
                    preset.theme.gradientEnd
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

// Data Classes

enum class ThemeTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    COLORS("Colors", Icons.Default.Settings),
    TYPOGRAPHY("Typography", Icons.Default.Settings),
    SHAPES("Shapes", Icons.Default.Settings),
    EFFECTS("Effects", Icons.Default.Settings),
    PRESETS("Presets", Icons.Default.Settings)
}

data class MagicThemeData(
    // Colors
    val primary: Color = Color(0xFF9C88FF),
    val secondary: Color = Color(0xFFB794F6),
    val tertiary: Color = Color(0xFF4299E1),
    val background: Color = Color(0xFFF8F9FF),
    val surface: Color = Color(0xFFFFFFFF),
    val cardBackground: Color = Color(0xFFFFFFFF),
    val textPrimary: Color = Color(0xFF2D3436),
    val textSecondary: Color = Color(0xFF636E72),
    val gradientStart: Color = Color(0xFF9C88FF),
    val gradientEnd: Color = Color(0xFFF687B3),
    
    // Typography
    val displayLargeSize: Float = 57f,
    val headlineLargeSize: Float = 32f,
    val bodyLargeSize: Float = 16f,
    val headlineFontWeight: Int = 600,
    val bodyFontWeight: Int = 400,
    
    // Shapes
    val smallCornerRadius: Float = 8f,
    val mediumCornerRadius: Float = 16f,
    val largeCornerRadius: Float = 24f,
    val cardCornerRadius: Float = 20f,
    val buttonCornerRadius: Float = 28f,
    
    // Effects
    val cardElevation: Float = 4f,
    val buttonElevation: Float = 6f,
    val animationsEnabled: Boolean = true,
    val animationDuration: Int = 300,
    val glassmorphismEnabled: Boolean = true,
    val blurAmount: Float = 10f
) {
    companion object {
        fun default() = MagicThemeData()
    }
}

data class ThemePreset(
    val name: String,
    val description: String,
    val theme: MagicThemeData
)

object MagicThemePresets {
    val dream = ThemePreset(
        name = "Dream",
        description = "Soft gradients with purple and pink",
        theme = MagicThemeData()
    )
    
    val ocean = ThemePreset(
        name = "Ocean",
        description = "Cool blues and aqua tones",
        theme = MagicThemeData(
            primary = Color(0xFF006BA0),
            secondary = Color(0xFF00ACC1),
            tertiary = Color(0xFF00E5FF),
            gradientStart = Color(0xFF006BA0),
            gradientEnd = Color(0xFF00E5FF)
        )
    )
    
    val sunset = ThemePreset(
        name = "Sunset",
        description = "Warm oranges and reds",
        theme = MagicThemeData(
            primary = Color(0xFFFF6B6B),
            secondary = Color(0xFFFFD93D),
            tertiary = Color(0xFFFF9A00),
            gradientStart = Color(0xFFFF6B6B),
            gradientEnd = Color(0xFFFFD93D)
        )
    )
    
    val forest = ThemePreset(
        name = "Forest",
        description = "Natural greens and earth tones",
        theme = MagicThemeData(
            primary = Color(0xFF2E7D32),
            secondary = Color(0xFF66BB6A),
            tertiary = Color(0xFF8BC34A),
            gradientStart = Color(0xFF2E7D32),
            gradientEnd = Color(0xFF8BC34A)
        )
    )
    
    val minimal = ThemePreset(
        name = "Minimal",
        description = "Clean black and white",
        theme = MagicThemeData(
            primary = Color(0xFF000000),
            secondary = Color(0xFF424242),
            tertiary = Color(0xFF757575),
            gradientStart = Color(0xFF000000),
            gradientEnd = Color(0xFF424242),
            cardCornerRadius = 4f,
            buttonCornerRadius = 4f
        )
    )
    
    val all = listOf(dream, ocean, sunset, forest, minimal)
}