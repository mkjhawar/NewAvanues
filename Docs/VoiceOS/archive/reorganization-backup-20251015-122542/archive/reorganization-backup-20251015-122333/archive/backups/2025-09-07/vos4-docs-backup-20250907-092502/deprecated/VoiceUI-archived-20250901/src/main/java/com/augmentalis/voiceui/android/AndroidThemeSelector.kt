/**
 * AndroidThemeSelector.kt - UI for developers to select and preview themes
 * 
 * Provides a visual theme selector with live preview, allowing developers
 * to choose exactly how their app looks on Android devices.
 */

package com.augmentalis.voiceui.android

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceui.theming.CustomTheme
import com.augmentalis.voiceui.theming.ThemeManager
import kotlinx.coroutines.delay

/**
 * ANDROID THEME SELECTOR UI
 * 
 * Beautiful theme selection interface that allows developers to:
 * - Browse all available themes
 * - See live previews
 * - Compare themes side by side
 * - Test with their content
 * - Save favorite themes
 * - Create custom themes
 */
@Composable
fun AndroidThemeSelector(
    onThemeSelected: (AndroidTheme) -> Unit,
    onCreateCustomTheme: () -> Unit = {},
    currentTheme: AndroidTheme? = null,
    showPreview: Boolean = true
) {
    var selectedCategory by remember { mutableStateOf(AndroidThemeSystem.Companion.ThemeCategory.MATERIAL_DESIGN) }
    var selectedTheme by remember { mutableStateOf(currentTheme) }
    var previewMode by remember { mutableStateOf(PreviewMode.COMPONENTS) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val themeSystem = remember { AndroidThemeSystem(context) }
    val availableThemes = remember { themeSystem.getAllAvailableThemes() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        ThemeSelectorHeader(
            selectedTheme = selectedTheme,
            onCreateCustom = onCreateCustomTheme
        )
        
        // Category Tabs
        ThemeCategoryTabs(
            categories = AndroidThemeSystem.Companion.ThemeCategory.values().toList(),
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )
        
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Theme List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                ThemeList(
                    themes = availableThemes[selectedCategory] ?: emptyList(),
                    selectedTheme = selectedTheme,
                    onThemeSelected = { theme ->
                        selectedTheme = theme
                        onThemeSelected(theme)
                    }
                )
            }
            
            // Preview Panel
            if (showPreview && selectedTheme != null) {
                Spacer(modifier = Modifier.width(16.dp))
                
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                ) {
                    ThemePreviewPanel(
                        theme = selectedTheme!!,
                        previewMode = previewMode,
                        onPreviewModeChange = { previewMode = it }
                    )
                }
            }
        }
    }
}

/**
 * Theme Selector Header
 */
@Composable
private fun ThemeSelectorHeader(
    selectedTheme: AndroidTheme?,
    onCreateCustom: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Android Theme Selector",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (selectedTheme != null) {
                    Text(
                        text = "Current: ${selectedTheme.name}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCreateCustom,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Create Custom Theme")
                }
                
                Button(
                    onClick = { /* Import theme */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Import Theme")
                }
            }
        }
    }
}

/**
 * Theme Category Tabs
 */
@Composable
private fun ThemeCategoryTabs(
    categories: List<AndroidThemeSystem.Companion.ThemeCategory>,
    selectedCategory: AndroidThemeSystem.Companion.ThemeCategory,
    onCategorySelected: (AndroidThemeSystem.Companion.ThemeCategory) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = categories.indexOf(selectedCategory),
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        categories.forEach { category ->
            Tab(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = getCategoryDisplayName(category),
                        fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

/**
 * Theme List
 */
@Composable
private fun ThemeList(
    themes: List<AndroidTheme>,
    selectedTheme: AndroidTheme?,
    onThemeSelected: (AndroidTheme) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(themes) { theme ->
            ThemeCard(
                theme = theme,
                isSelected = theme.id == selectedTheme?.id,
                onClick = { onThemeSelected(theme) }
            )
        }
    }
}

/**
 * Individual Theme Card
 */
@Composable
private fun ThemeCard(
    theme: AndroidTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color preview circles
            ThemeColorPreview(theme)
            
            // Theme info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = theme.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (theme.minSdkVersion > 21) {
                    Text(
                        text = "Requires Android ${getAndroidVersion(theme.minSdkVersion)}+",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            
            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Theme color preview circles
 */
@Composable
private fun ThemeColorPreview(theme: AndroidTheme) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.theme.colors.primary)
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.theme.colors.secondary)
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.theme.colors.background)
                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
        )
    }
}

/**
 * Theme Preview Panel
 */
@Composable
private fun ThemePreviewPanel(
    theme: AndroidTheme,
    previewMode: PreviewMode,
    onPreviewModeChange: (PreviewMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Preview mode selector
            PreviewModeSelector(
                currentMode = previewMode,
                onModeSelected = onPreviewModeChange
            )
            
            // Preview content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (previewMode) {
                    PreviewMode.COMPONENTS -> ComponentsPreview(theme)
                    PreviewMode.SAMPLE_APP -> SampleAppPreview(theme)
                    PreviewMode.COLOR_PALETTE -> ColorPalettePreview(theme)
                    PreviewMode.TYPOGRAPHY -> TypographyPreview(theme)
                    PreviewMode.ANIMATIONS -> AnimationsPreview(theme)
                }
            }
        }
    }
}

/**
 * Preview Mode Selector
 */
@Composable
private fun PreviewModeSelector(
    currentMode: PreviewMode,
    onModeSelected: (PreviewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PreviewMode.values().forEach { mode ->
            FilterChip(
                selected = mode == currentMode,
                onClick = { onModeSelected(mode) },
                label = { Text(mode.displayName) }
            )
        }
    }
}

/**
 * Components Preview
 */
@Composable
private fun ComponentsPreview(theme: AndroidTheme) {
    // Apply theme
    ThemeManager.registerTheme(theme.theme)
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Buttons", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) { Text("Primary") }
                OutlinedButton(onClick = {}) { Text("Outlined") }
                TextButton(onClick = {}) { Text("Text") }
            }
        }
        
        item {
            Text("Text Fields", fontWeight = FontWeight.Bold)
            var text by remember { mutableStateOf("") }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Enter text") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            Text("Cards", fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Card Title", fontWeight = FontWeight.Bold)
                    Text("Card content goes here")
                }
            }
        }
        
        item {
            Text("Switches & Checkboxes", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                var checked by remember { mutableStateOf(true) }
                Switch(checked = checked, onCheckedChange = { checked = it })
                Checkbox(checked = checked, onCheckedChange = { checked = it })
                RadioButton(selected = checked, onClick = { checked = !checked })
            }
        }
        
        item {
            Text("Progress Indicators", fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                CircularProgressIndicator()
            }
        }
        
        item {
            Text("Chips", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Assist") })
                FilterChip(selected = true, onClick = {}, label = { Text("Filter") })
                InputChip(selected = false, onClick = {}, label = { Text("Input") })
            }
        }
    }
}

/**
 * Sample App Preview
 */
@Composable
private fun SampleAppPreview(theme: AndroidTheme) {
    // Apply theme and show a sample app screen
    ThemeManager.registerTheme(theme.theme)
    
    Column(modifier = Modifier.fillMaxSize()) {
        // App bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = theme.theme.colors.primary
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sample App",
                    color = theme.theme.colors.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = theme.theme.colors.onPrimary
                )
            }
        }
        
        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.theme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(5) { index ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = theme.theme.colors.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(theme.theme.colors.secondary)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Item ${index + 1}",
                                fontWeight = FontWeight.Medium,
                                color = theme.theme.colors.onSurface
                            )
                            Text(
                                "Description text here",
                                fontSize = 12.sp,
                                color = theme.theme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.theme.colors.primary
                            )
                        ) {
                            Text("Action")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Color Palette Preview
 */
@Composable
private fun ColorPalettePreview(theme: AndroidTheme) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val colors = listOf(
            "Primary" to theme.theme.colors.primary,
            "Secondary" to theme.theme.colors.secondary,
            "Background" to theme.theme.colors.background,
            "Surface" to theme.theme.colors.surface,
            "Error" to theme.theme.colors.error,
            "On Primary" to theme.theme.colors.onPrimary,
            "On Secondary" to theme.theme.colors.onSecondary,
            "On Background" to theme.theme.colors.onBackground,
            "On Surface" to theme.theme.colors.onSurface
        )
        
        items(colors) { (name, color) ->
            ColorSwatch(name = name, color = color)
        }
    }
}

/**
 * Color Swatch
 */
@Composable
private fun ColorSwatch(name: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
        )
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = color.toHexString(),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Typography Preview
 */
@Composable
private fun TypographyPreview(theme: AndroidTheme) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Heading 1",
                style = TextStyle(
                    fontSize = theme.theme.typography.h1.fontSize,
                    fontWeight = theme.theme.typography.h1.fontWeight
                )
            )
        }
        item {
            Text(
                "Heading 2",
                style = TextStyle(
                    fontSize = theme.theme.typography.h2.fontSize,
                    fontWeight = theme.theme.typography.h2.fontWeight
                )
            )
        }
        item {
            Text(
                "Heading 3",
                style = TextStyle(
                    fontSize = theme.theme.typography.h3.fontSize,
                    fontWeight = theme.theme.typography.h3.fontWeight
                )
            )
        }
        item {
            Text(
                "Body 1 - Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                style = TextStyle(
                    fontSize = theme.theme.typography.body1.fontSize,
                    fontWeight = theme.theme.typography.body1.fontWeight
                )
            )
        }
        item {
            Text(
                "Body 2 - Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                style = TextStyle(
                    fontSize = theme.theme.typography.body2.fontSize,
                    fontWeight = theme.theme.typography.body2.fontWeight
                )
            )
        }
        item {
            Text(
                "BUTTON TEXT",
                style = MaterialTheme.typography.labelLarge
            )
        }
        item {
            Text(
                "Caption text",
                style = TextStyle(
                    fontSize = theme.theme.typography.caption.fontSize,
                    fontWeight = theme.theme.typography.caption.fontWeight
                )
            )
        }
    }
}

/**
 * Animations Preview
 */
@Composable
private fun AnimationsPreview(theme: AndroidTheme) {
    var animate by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            animate = !animate
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Animation Speeds", fontWeight = FontWeight.Bold)
        
        // Fast animation
        AnimatedVisibility(
            visible = animate,
            enter = fadeIn(animationSpec = tween(theme.theme.animations.fast.duration.toInt())),
            exit = fadeOut(animationSpec = tween(theme.theme.animations.fast.duration.toInt()))
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text("Fast Animation (${theme.theme.animations.fast.duration}ms)", modifier = Modifier.padding(16.dp))
            }
        }
        
        // Normal animation
        AnimatedVisibility(
            visible = animate,
            enter = slideInHorizontally(animationSpec = tween(theme.theme.animations.normal.duration.toInt())),
            exit = slideOutHorizontally(animationSpec = tween(theme.theme.animations.normal.duration.toInt()))
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text("Normal Animation (${theme.theme.animations.normal.duration}ms)", modifier = Modifier.padding(16.dp))
            }
        }
        
        // Slow animation
        AnimatedVisibility(
            visible = animate,
            enter = scaleIn(animationSpec = tween(theme.theme.animations.slow.duration.toInt())),
            exit = scaleOut(animationSpec = tween(theme.theme.animations.slow.duration.toInt()))
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text("Slow Animation (${theme.theme.animations.slow.duration}ms)", modifier = Modifier.padding(16.dp))
            }
        }
    }
}

/**
 * Preview modes
 */
enum class PreviewMode(val displayName: String) {
    COMPONENTS("Components"),
    SAMPLE_APP("Sample App"),
    COLOR_PALETTE("Colors"),
    TYPOGRAPHY("Typography"),
    ANIMATIONS("Animations")
}

/**
 * Helper functions
 */
private fun getCategoryDisplayName(category: AndroidThemeSystem.Companion.ThemeCategory): String {
    return when (category) {
        AndroidThemeSystem.Companion.ThemeCategory.MATERIAL_DESIGN -> "Material Design"
        AndroidThemeSystem.Companion.ThemeCategory.DEVICE_DEFAULT -> "Device Optimized"
        AndroidThemeSystem.Companion.ThemeCategory.MANUFACTURER -> "Manufacturer"
        AndroidThemeSystem.Companion.ThemeCategory.CLASSIC_ANDROID -> "Classic Android"
        AndroidThemeSystem.Companion.ThemeCategory.VOICEUI_THEMES -> "VoiceUI Themes"
        AndroidThemeSystem.Companion.ThemeCategory.DEVELOPER_CUSTOM -> "My Themes"
    }
}

private fun getAndroidVersion(sdkVersion: Int): String {
    return when (sdkVersion) {
        31, 32, 33 -> "12"
        30 -> "11"
        29 -> "10"
        28 -> "9"
        27, 26 -> "8"
        25, 24 -> "7"
        23 -> "6"
        22, 21 -> "5"
        else -> sdkVersion.toString()
    }
}

private fun Color.toHexString(): String {
    return "#${Integer.toHexString(this.hashCode()).uppercase().padStart(8, '0')}"
}