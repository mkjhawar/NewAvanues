/**
 * HUDSettingsUI.kt
 * Path: /managers/HUDManager/src/main/java/com/augmentalis/hudmanager/ui/HUDSettingsUI.kt
 * 
 * Created: 2025-01-24
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Compose UI for HUD settings configuration
 * Provides comprehensive settings interface for users
 */

package com.augmentalis.voiceoscore.managers.hudmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import com.augmentalis.voiceoscore.managers.hudmanager.settings.*
import kotlinx.coroutines.launch

/**
 * Main HUD Settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HUDSettingsScreen(
    settingsManager: HUDSettingsManager,
    onBack: () -> Unit = {}
) {
    val settings by settingsManager.settings.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HUD Display Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        settingsManager.resetToDefaults()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset to defaults")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row for categories
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("General") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Display") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Visual") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Privacy") }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Performance") }
                )
            }
            
            // Tab content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // General Settings
                        item {
                            MasterToggle(
                                enabled = settings.hudEnabled,
                                onToggle = { settingsManager.toggleHUD(it) }
                            )
                        }
                        
                        item {
                            DisplayModeSelector(
                                currentMode = settings.displayMode,
                                onModeSelected = { settingsManager.setDisplayMode(it) }
                            )
                        }
                        
                        item {
                            PresetSelector(
                                onPresetSelected = { settingsManager.applyPreset(it) }
                            )
                        }
                    }
                    
                    1 -> {
                        // Display Elements
                        item {
                            SectionHeader("Display Elements")
                        }
                        
                        item {
                            DisplayElementsSection(
                                elements = settings.displayElements,
                                onToggle = { element ->
                                    settingsManager.toggleDisplayElement(element)
                                }
                            )
                        }
                    }
                    
                    2 -> {
                        // Visual Settings
                        item {
                            SectionHeader("Visual Settings")
                        }
                        
                        item {
                            VisualSettingsSection(
                                visual = settings.visual,
                                onTransparencyChange = { settingsManager.adjustTransparency(it) },
                                onBrightnessChange = { settingsManager.adjustBrightness(it) },
                                onThemeChange = { settingsManager.setColorTheme(it) }
                            )
                        }
                        
                        item {
                            PositioningSection(
                                positioning = settings.positioning,
                                onUpdate = { newPositioning ->
                                    settingsManager.updateSettings {
                                        copy(positioning = newPositioning)
                                    }
                                }
                            )
                        }
                    }
                    
                    3 -> {
                        // Privacy Settings
                        item {
                            SectionHeader("Privacy & Security")
                        }
                        
                        item {
                            PrivacySettingsSection(
                                privacy = settings.privacy,
                                onUpdate = { newPrivacy ->
                                    settingsManager.updateSettings {
                                        copy(privacy = newPrivacy)
                                    }
                                }
                            )
                        }
                    }
                    
                    4 -> {
                        // Performance Settings
                        item {
                            SectionHeader("Performance")
                        }
                        
                        item {
                            PerformanceModeSelector(
                                onModeSelected = { settingsManager.setPerformanceMode(it) }
                            )
                        }
                        
                        item {
                            PerformanceSettingsSection(
                                performance = settings.performance,
                                onUpdate = { newPerformance ->
                                    settingsManager.updateSettings {
                                        copy(performance = newPerformance)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MasterToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surfaceVariant
        )
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
                    text = "HUD Display",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (enabled) "Enabled" else "Disabled",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun DisplayModeSelector(
    currentMode: HUDDisplayMode,
    onModeSelected: (HUDDisplayMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Display Mode",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val modes = listOf(
                HUDDisplayMode.MINIMAL to "Minimal - Essential info only",
                HUDDisplayMode.CONTEXTUAL to "Contextual - Smart adaptive display",
                HUDDisplayMode.FULL to "Full - All information visible",
                HUDDisplayMode.CUSTOM to "Custom - Your preferences",
                HUDDisplayMode.DRIVING to "Driving - Navigation focused",
                HUDDisplayMode.PRIVACY to "Privacy - Secure mode"
            )
            
            modes.forEach { (mode, description) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onModeSelected(mode) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == mode,
                        onClick = { onModeSelected(mode) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = description)
                }
            }
        }
    }
}

@Composable
fun PresetSelector(
    onPresetSelected: (HUDPreset) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Presets",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PresetButton("Default", Icons.Default.Home) { 
                    onPresetSelected(HUDPreset.DEFAULT) 
                }
                PresetButton("Minimal", Icons.Default.RemoveCircle) { 
                    onPresetSelected(HUDPreset.MINIMAL) 
                }
                PresetButton("Driving", Icons.Default.DirectionsCar) { 
                    onPresetSelected(HUDPreset.DRIVING) 
                }
                PresetButton("Privacy", Icons.Default.Lock) { 
                    onPresetSelected(HUDPreset.PRIVACY) 
                }
            }
        }
    }
}

@Composable
fun PresetButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun DisplayElementsSection(
    elements: DisplayElements,
    onToggle: (DisplayElement) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val elementList = listOf(
                DisplayElement.BATTERY to "Battery Status" to elements.batteryStatus,
                DisplayElement.TIME to "Time" to elements.time,
                DisplayElement.DATE to "Date" to elements.date,
                DisplayElement.NOTIFICATIONS to "Notifications" to elements.notifications,
                DisplayElement.MESSAGES to "Messages" to elements.messages,
                DisplayElement.VOICE_COMMANDS to "Voice Commands" to elements.voiceCommands,
                DisplayElement.GAZE_TARGET to "Gaze Target" to elements.gazeTarget,
                DisplayElement.NAVIGATION to "Navigation Hints" to elements.navigationHints,
                DisplayElement.COMPASS to "Compass" to elements.compass,
                DisplayElement.SYSTEM_INFO to "System Diagnostics" to elements.systemDiagnostics
            )
            
            elementList.forEach { (element, enabled) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = element.second)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { onToggle(element.first) }
                    )
                }
            }
        }
    }
}

@Composable
fun VisualSettingsSection(
    visual: VisualSettings,
    onTransparencyChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onThemeChange: (ColorTheme) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Transparency Slider
            Text("Transparency: ${(visual.transparency * 100).toInt()}%")
            Slider(
                value = visual.transparency,
                onValueChange = onTransparencyChange,
                valueRange = 0f..1f,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Brightness Slider
            Text("Brightness: ${(visual.brightness * 100).toInt()}%")
            Slider(
                value = visual.brightness,
                onValueChange = onBrightnessChange,
                valueRange = 0.5f..2f,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Theme Selection
            Text("Color Theme", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ThemeButton("Auto", visual.colorTheme == ColorTheme.AUTO) {
                    onThemeChange(ColorTheme.AUTO)
                }
                ThemeButton("Light", visual.colorTheme == ColorTheme.LIGHT) {
                    onThemeChange(ColorTheme.LIGHT)
                }
                ThemeButton("Dark", visual.colorTheme == ColorTheme.DARK) {
                    onThemeChange(ColorTheme.DARK)
                }
            }
            
            // Animation Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Animations")
                Switch(
                    checked = visual.animations,
                    onCheckedChange = { /* Update animations */ }
                )
            }
        }
    }
}

@Composable
fun ThemeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(label)
    }
}

@Composable
fun PositioningSection(
    positioning: PositioningSettings,
    onUpdate: (PositioningSettings) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Display Positioning",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Distance Slider
            Text("Distance: ${positioning.hudDistance}m")
            Slider(
                value = positioning.hudDistance,
                onValueChange = { onUpdate(positioning.copy(hudDistance = it)) },
                valueRange = 1f..5f,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Text Size Selection
            Text("Text Size", modifier = Modifier.padding(top = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextSize.values().forEach { size ->
                    FilterChip(
                        selected = positioning.textSize == size,
                        onClick = { onUpdate(positioning.copy(textSize = size)) },
                        label = { Text(size.name.lowercase().replaceFirstChar { it.uppercaseChar() }) }
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacySettingsSection(
    privacy: PrivacySettings,
    onUpdate: (PrivacySettings) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            PrivacyToggle(
                label = "Hide in Public Spaces",
                description = "Automatically hide display in public areas",
                checked = privacy.hideInPublic,
                onCheckedChange = { onUpdate(privacy.copy(hideInPublic = it)) }
            )
            
            PrivacyToggle(
                label = "Blur Sensitive Content",
                description = "Blur passwords and credit card info",
                checked = privacy.blurSensitiveContent,
                onCheckedChange = { onUpdate(privacy.copy(blurSensitiveContent = it)) }
            )
            
            PrivacyToggle(
                label = "Disable in Meetings",
                description = "Auto-disable during calendar meetings",
                checked = privacy.disableInMeetings,
                onCheckedChange = { onUpdate(privacy.copy(disableInMeetings = it)) }
            )
            
            PrivacyToggle(
                label = "Incognito Mode",
                description = "No history or logging",
                checked = privacy.incognitoMode,
                onCheckedChange = { onUpdate(privacy.copy(incognitoMode = it)) }
            )
        }
    }
}

@Composable
fun PrivacyToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(
                description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun PerformanceModeSelector(
    onModeSelected: (PerformanceMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Performance Mode",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PerformanceModeButton(
                    label = "Battery Saver",
                    icon = Icons.Default.BatteryAlert,
                    onClick = { onModeSelected(PerformanceMode.BATTERY_SAVER) }
                )
                PerformanceModeButton(
                    label = "Balanced",
                    icon = Icons.Default.Balance,
                    onClick = { onModeSelected(PerformanceMode.BALANCED) }
                )
                PerformanceModeButton(
                    label = "Performance",
                    icon = Icons.Default.Speed,
                    onClick = { onModeSelected(PerformanceMode.PERFORMANCE) }
                )
            }
        }
    }
}

@Composable
fun PerformanceModeButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun PerformanceSettingsSection(
    performance: PerformanceSettings,
    onUpdate: (PerformanceSettings) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Target FPS: ${performance.targetFps}")
            Slider(
                value = performance.targetFps.toFloat(),
                onValueChange = { onUpdate(performance.copy(targetFps = it.toInt())) },
                valueRange = 30f..120f,
                steps = 5,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Battery Optimization")
                Switch(
                    checked = performance.batteryOptimization,
                    onCheckedChange = { onUpdate(performance.copy(batteryOptimization = it)) }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Adaptive Quality")
                Switch(
                    checked = performance.adaptiveQuality,
                    onCheckedChange = { onUpdate(performance.copy(adaptiveQuality = it)) }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}