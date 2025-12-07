/**
 * SettingsScreen.kt - Comprehensive settings interface for VoiceOS Accessibility
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-08-28
 */
package com.augmentalis.voiceoscore.accessibility.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceoscore.accessibility.config.ServiceConfiguration
import com.augmentalis.voiceoscore.accessibility.ui.utils.glassMorphism
import com.augmentalis.voiceoscore.accessibility.ui.utils.GlassMorphismConfig
import com.augmentalis.voiceoscore.accessibility.ui.utils.DepthLevel
import com.augmentalis.voiceoscore.accessibility.ui.utils.ThemeUtils

/**
 * Performance mode options
 */
enum class PerformanceMode(val displayName: String) {
    POWER_SAVER("Power Saver"),
    BALANCED("Balanced"), 
    HIGH_PERFORMANCE("High Performance")
}

/**
 * Main settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    configuration: ServiceConfiguration,
    onConfigurationChange: (ServiceConfiguration) -> Unit,
    onBack: () -> Unit
) {
    var currentConfig by remember { mutableStateOf(configuration) }
    var performanceMode by remember { 
        mutableStateOf(
            when {
                currentConfig.commandTimeout <= 3000 -> PerformanceMode.HIGH_PERFORMANCE
                currentConfig.commandTimeout >= 8000 -> PerformanceMode.POWER_SAVER
                else -> PerformanceMode.BALANCED
            }
        )
    }
    
    // Update configuration when performance mode changes
    LaunchedEffect(performanceMode) {
        currentConfig = when (performanceMode) {
            PerformanceMode.HIGH_PERFORMANCE -> currentConfig.copy(
                commandTimeout = 2000,
                maxCacheSize = 200,
                handlersEnabled = true
            )
            PerformanceMode.POWER_SAVER -> currentConfig.copy(
                commandTimeout = 10000,
                maxCacheSize = 50,
                handlersEnabled = true
            )
            PerformanceMode.BALANCED -> currentConfig.copy(
                commandTimeout = 5000,
                maxCacheSize = 100,
                handlersEnabled = true
            )
        }
        onConfigurationChange(currentConfig)
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            SettingsHeader(onBack = onBack)
        }
        
        // Performance Mode Section
        item {
            PerformanceModeSection(
                currentMode = performanceMode,
                onModeChange = { performanceMode = it }
            )
        }
        
        // Handler Toggles Section
        item {
            HandlerTogglesSection(
                configuration = currentConfig,
                onConfigurationChange = { newConfig ->
                    currentConfig = newConfig
                    onConfigurationChange(newConfig)
                }
            )
        }
        
        // Cursor Configuration Section
        item {
            CursorConfigurationSection(
                configuration = currentConfig,
                onConfigurationChange = { newConfig ->
                    currentConfig = newConfig
                    onConfigurationChange(newConfig)
                }
            )
        }
        
        // Cache Settings Section
        item {
            CacheSettingsSection(
                configuration = currentConfig,
                onConfigurationChange = { newConfig ->
                    currentConfig = newConfig
                    onConfigurationChange(newConfig)
                }
            )
        }
        
        // Advanced Settings Section
        item {
            AdvancedSettingsSection(
                configuration = currentConfig,
                onConfigurationChange = { newConfig ->
                    currentConfig = newConfig
                    onConfigurationChange(newConfig)
                }
            )
        }
    }
}

/**
 * Settings screen header
 */
@Composable
fun SettingsHeader(onBack: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.12f,
                    borderOpacity = 0.25f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF2196F3),
                    tintOpacity = 0.18f
                ),
                depth = DepthLevel(0.8f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .glassMorphism(
                        config = GlassMorphismConfig(
                            cornerRadius = 12.dp,
                            backgroundOpacity = 0.1f,
                            borderOpacity = 0.2f,
                            borderWidth = 1.dp,
                            tintColor = Color(0xFF2196F3),
                            tintOpacity = 0.15f
                        ),
                        depth = DepthLevel(0.4f)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF2196F3)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ThemeUtils.getTextColor(),
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Configure voice accessibility options",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ThemeUtils.getSecondaryTextColor()
                )
            }
        }
    }
}

/**
 * Performance mode selection section
 */
@Composable
fun PerformanceModeSection(
    currentMode: PerformanceMode,
    onModeChange: (PerformanceMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF4CAF50),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Performance",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF4CAF50)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Performance Mode",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThemeUtils.getTextColor(),
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Text(
                text = "Choose how the accessibility service balances performance and battery usage",
                style = MaterialTheme.typography.bodyMedium,
                color = ThemeUtils.getSecondaryTextColor(),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(modifier = Modifier.selectableGroup()) {
                PerformanceMode.values().forEach { mode ->
                    PerformanceModeItem(
                        mode = mode,
                        isSelected = currentMode == mode,
                        onSelect = { onModeChange(mode) }
                    )
                }
            }
        }
    }
}

/**
 * Individual performance mode item
 */
@Composable
fun PerformanceModeItem(
    mode: PerformanceMode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(role = Role.RadioButton) { onSelect() }
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 12.dp,
                    backgroundOpacity = if (isSelected) 0.15f else 0.08f,
                    borderOpacity = if (isSelected) 0.3f else 0.15f,
                    borderWidth = if (isSelected) 1.5.dp else 1.dp,
                    tintColor = getModeColor(mode),
                    tintOpacity = if (isSelected) 0.2f else 0.1f
                ),
                depth = DepthLevel(if (isSelected) 0.5f else 0.3f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = getModeColor(mode),
                    unselectedColor = ThemeUtils.getSecondaryTextColor()
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = mode.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = ThemeUtils.getTextColor(),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                
                Text(
                    text = getModeDescription(mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeUtils.getSecondaryTextColor()
                )
            }
        }
    }
}

/**
 * Handler toggles section
 */
@Composable
fun HandlerTogglesSection(
    configuration: ServiceConfiguration,
    onConfigurationChange: (ServiceConfiguration) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF673AB7),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = "Handlers",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF673AB7)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Voice Handler Modules",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThemeUtils.getTextColor(),
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Text(
                text = "Enable or disable specific command handling modules",
                style = MaterialTheme.typography.bodyMedium,
                color = ThemeUtils.getSecondaryTextColor(),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val handlers = listOf(
                HandlerInfo("All Handlers", "Enable/disable all voice command handlers", 
                    Icons.Default.Apps, configuration.handlersEnabled) { enabled ->
                    onConfigurationChange(configuration.copy(handlersEnabled = enabled))
                },
                HandlerInfo("App Launching", "Launch apps and switch between applications", 
                    Icons.AutoMirrored.Filled.Launch, configuration.appLaunchingEnabled) { enabled ->
                    onConfigurationChange(configuration.copy(appLaunchingEnabled = enabled))
                },
                HandlerInfo("Dynamic Commands", "Context-aware command generation", 
                    Icons.Default.AutoAwesome, configuration.dynamicCommandsEnabled) { enabled ->
                    onConfigurationChange(configuration.copy(dynamicCommandsEnabled = enabled))
                },
                HandlerInfo("UI Scraping", "Advanced UI element detection", 
                    Icons.Default.Search, configuration.uiScrapingEnabled) { enabled ->
                    onConfigurationChange(configuration.copy(uiScrapingEnabled = enabled))
                },
                HandlerInfo("Command Caching", "Cache commands for faster execution", 
                    Icons.Default.Storage, configuration.commandCachingEnabled) { enabled ->
                    onConfigurationChange(configuration.copy(commandCachingEnabled = enabled))
                },
                HandlerInfo("Cursor Mode", "Visual cursor for precise control", 
                    Icons.Default.TouchApp, configuration.cursorEnabled) { enabled ->
                    onConfigurationChange(configuration.copy(cursorEnabled = enabled))
                },
                HandlerInfo("Action Handler", "Core action processing (always enabled)", 
                    Icons.Default.PlayArrow, true, enabled = false) { }
            )
            
            handlers.forEach { handler ->
                HandlerToggleItem(
                    handlerInfo = handler,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Data class for handler information
 */
data class HandlerInfo(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isEnabled: Boolean,
    val enabled: Boolean = true,
    val onToggle: (Boolean) -> Unit
)

/**
 * Individual handler toggle item
 */
@Composable
fun HandlerToggleItem(
    handlerInfo: HandlerInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 12.dp,
                    backgroundOpacity = if (handlerInfo.isEnabled) 0.1f else 0.05f,
                    borderOpacity = if (handlerInfo.isEnabled) 0.2f else 0.1f,
                    borderWidth = 1.dp,
                    tintColor = if (handlerInfo.isEnabled) Color(0xFF4CAF50) else Color(0xFFFF5722),
                    tintOpacity = 0.12f
                ),
                depth = DepthLevel(0.3f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = handlerInfo.icon,
                contentDescription = handlerInfo.name,
                modifier = Modifier.size(20.dp),
                tint = if (handlerInfo.isEnabled) Color(0xFF4CAF50) else ThemeUtils.getDisabledTextColor()
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = handlerInfo.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (handlerInfo.isEnabled) ThemeUtils.getTextColor() else ThemeUtils.getDisabledTextColor(),
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = handlerInfo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (handlerInfo.isEnabled) ThemeUtils.getSecondaryTextColor() else ThemeUtils.getDisabledTextColor(),
                    fontSize = 11.sp
                )
            }
            
            Switch(
                checked = handlerInfo.isEnabled,
                onCheckedChange = handlerInfo.onToggle,
                enabled = handlerInfo.enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CAF50),
                    checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                    uncheckedThumbColor = Color(0xFFFF5722),
                    uncheckedTrackColor = Color(0xFFFF5722).copy(alpha = 0.3f)
                )
            )
        }
    }
}

/**
 * Cursor configuration section
 */
@Composable
fun CursorConfigurationSection(
    configuration: ServiceConfiguration,
    onConfigurationChange: (ServiceConfiguration) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFFFF9800),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusWeak,
                        contentDescription = "Cursor",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFFFF9800)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = "Voice Cursor",
                            style = MaterialTheme.typography.titleLarge,
                            color = ThemeUtils.getTextColor(),
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Text(
                            text = if (configuration.cursorEnabled) "Visual pointer for voice navigation" else "Disabled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThemeUtils.getSecondaryTextColor()
                        )
                    }
                }
                
                Switch(
                    checked = configuration.cursorEnabled,
                    onCheckedChange = { enabled ->
                        onConfigurationChange(configuration.copy(cursorEnabled = enabled))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFF9800),
                        checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.5f)
                    )
                )
            }
            
            if (configuration.cursorEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Cursor Options",
                    style = MaterialTheme.typography.titleMedium,
                    color = ThemeUtils.getTextColor(),
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Placeholder for cursor style options
                Text(
                    text = "• Crosshair style cursor\n• High contrast visibility\n• Smooth animation transitions",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeUtils.getSecondaryTextColor(),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * Cache settings section
 */
@Composable
fun CacheSettingsSection(
    configuration: ServiceConfiguration,
    @Suppress("UNUSED_PARAMETER") onConfigurationChange: (ServiceConfiguration) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF009688),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Cache",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF009688)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Cache Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThemeUtils.getTextColor(),
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Text(
                text = "Manage command caching for improved performance",
                style = MaterialTheme.typography.bodyMedium,
                color = ThemeUtils.getSecondaryTextColor(),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cache Size",
                        style = MaterialTheme.typography.titleMedium,
                        color = ThemeUtils.getTextColor(),
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "${configuration.maxCacheSize} commands",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeUtils.getSecondaryTextColor()
                    )
                }
                
                Text(
                    text = when {
                        configuration.maxCacheSize <= 50 -> "Small"
                        configuration.maxCacheSize <= 100 -> "Medium"
                        else -> "Large"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF009688),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Advanced settings section
 */
@Composable
fun AdvancedSettingsSection(
    configuration: ServiceConfiguration,
    @Suppress("UNUSED_PARAMETER") onConfigurationChange: (ServiceConfiguration) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF607D8B),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Advanced",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF607D8B)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Advanced Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThemeUtils.getTextColor(),
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Command timeout
            AdvancedSettingItem(
                title = "Command Timeout",
                description = "Maximum time to wait for command execution",
                value = "${configuration.commandTimeout}ms"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Handlers enabled
            AdvancedSettingItem(
                title = "Base Handlers",
                description = "Core handler functionality",
                value = if (configuration.handlersEnabled) "Enabled" else "Disabled"
            )
        }
    }
}

/**
 * Individual advanced setting item
 */
@Composable
fun AdvancedSettingItem(
    title: String,
    description: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ThemeUtils.getTextColor(),
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = ThemeUtils.getSecondaryTextColor(),
                fontSize = 11.sp
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF607D8B),
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Helper functions
private fun getModeColor(mode: PerformanceMode): Color {
    return when (mode) {
        PerformanceMode.HIGH_PERFORMANCE -> Color(0xFFFF5722)
        PerformanceMode.POWER_SAVER -> Color(0xFF4CAF50)
        PerformanceMode.BALANCED -> Color(0xFF2196F3)
    }
}

private fun getModeDescription(mode: PerformanceMode): String {
    return when (mode) {
        PerformanceMode.HIGH_PERFORMANCE -> "Faster response, higher battery usage"
        PerformanceMode.POWER_SAVER -> "Slower response, optimized for battery life"
        PerformanceMode.BALANCED -> "Good balance of performance and battery usage"
    }
}