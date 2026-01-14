/**
 * AccessibilitySettings.kt - Settings screen for VoiceOS Accessibility
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Settings activity with service configuration options, handler toggles,
 * and performance mode selection using glassmorphism UI.
 */
package com.augmentalis.voiceos.accessibility.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceos.accessibility.config.ServiceConfiguration
import com.augmentalis.voiceos.accessibility.ui.theme.AccessibilityTheme
import com.augmentalis.voiceos.accessibility.ui.utils.glassMorphism
import com.augmentalis.voiceos.accessibility.ui.utils.GlassMorphismConfig
import com.augmentalis.voiceos.accessibility.ui.utils.DepthLevel
import com.augmentalis.voiceos.accessibility.viewmodel.SettingsViewModel
import com.augmentalis.voiceos.accessibility.viewmodel.PerformanceMode

class AccessibilitySettings : ComponentActivity() {
    
    private val settingsViewModel: SettingsViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AccessibilityTheme {
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    @Suppress("UNUSED_VARIABLE")
    val configuration by settingsViewModel.configuration.collectAsState() // Configuration passed to child components
    val performanceMode by settingsViewModel.performanceMode.collectAsState()
    val handlerStates by settingsViewModel.handlerStates.collectAsState()
    val isLoading by settingsViewModel.isLoading.collectAsState()
    val errorMessage by settingsViewModel.errorMessage.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            SettingsTopBar(
                onNavigateBack = onNavigateBack,
                onOpenSystemSettings = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            )
            
            // Settings Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Error message if any
                if (!errorMessage.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFF5722).copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                modifier = Modifier.padding(16.dp),
                                color = Color(0xFFFF5722),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                // Service Control Section
                item {
                    ServiceControlSection(
                        settingsViewModel = settingsViewModel
                    )
                }
                
                // Handler Toggles Section
                item {
                    HandlerTogglesSection(
                        settingsViewModel = settingsViewModel,
                        handlerStates = handlerStates
                    )
                }
                
                // Performance Settings Section
                item {
                    PerformanceSettingsSection(
                        settingsViewModel = settingsViewModel,
                        performanceMode = performanceMode
                    )
                }
                
                // Cursor Settings Section
                item {
                    CursorSettingsSection(
                        settingsViewModel = settingsViewModel
                    )
                }
                
                // Advanced Settings Section
                item {
                    AdvancedSettingsSection(
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
        
        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    onNavigateBack: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 0.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 0.dp,
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.8f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(0.dp)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = onOpenSystemSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "System Settings",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}

@Composable
fun ServiceControlSection(
    settingsViewModel: SettingsViewModel
) {
    val showToasts by settingsViewModel.showToasts.collectAsState()
    val verboseLogging by settingsViewModel.verboseLogging.collectAsState()
    val configuration by settingsViewModel.configuration.collectAsState()
    
    SettingsSection(
        title = "Service Control",
        icon = Icons.Default.Power
    ) {
        SettingsToggle(
            title = "Service Enabled",
            description = "Enable/disable the accessibility service",
            isChecked = configuration.isEnabled,
            onCheckedChange = { /* Service enable/disable handled by system settings */ }
        )
        
        SettingsToggle(
            title = "Show Notifications",
            description = "Display toast messages for commands",
            isChecked = showToasts,
            onCheckedChange = { enabled ->
                settingsViewModel.updateShowToasts(enabled)
            }
        )
        
        SettingsToggle(
            title = "Verbose Logging",
            description = "Enable detailed debug logging",
            isChecked = verboseLogging,
            onCheckedChange = { enabled ->
                settingsViewModel.updateVerboseLogging(enabled)
            }
        )
    }
}

@Composable
fun HandlerTogglesSection(
    settingsViewModel: SettingsViewModel,
    handlerStates: Map<String, Boolean>
) {
    SettingsSection(
        title = "Command Handlers",
        icon = Icons.Default.Tune
    ) {
        settingsViewModel.getAllHandlerDefinitions().forEach { handler ->
            val isEnabled = handlerStates[handler.id] ?: false
            
            SettingsToggle(
                title = handler.name,
                description = handler.description,
                isChecked = isEnabled,
                enabled = !handler.isCore, // Core handlers cannot be disabled
                onCheckedChange = { enabled ->
                    settingsViewModel.toggleHandler(handler.id, enabled)
                }
            )
        }
    }
}

@Composable
fun PerformanceSettingsSection(
    settingsViewModel: SettingsViewModel,
    performanceMode: PerformanceMode
) {
    val dynamicCommandsEnabled by settingsViewModel.dynamicCommandsEnabled.collectAsState()
    val cacheEnabled by settingsViewModel.cacheEnabled.collectAsState()
    val maxCacheSize by settingsViewModel.maxCacheSize.collectAsState()
    @Suppress("UNUSED_VARIABLE")
    val configuration by settingsViewModel.configuration.collectAsState() // Reserved for future configuration display
    
    SettingsSection(
        title = "Performance",
        icon = Icons.Default.Speed
    ) {
        // Performance Mode Selection
        Text(
            text = "Performance Mode",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        PerformanceMode.values().forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { settingsViewModel.updatePerformanceMode(mode) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = performanceMode == mode,
                    onClick = { settingsViewModel.updatePerformanceMode(mode) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF4285F4)
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = mode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingsToggle(
            title = "Dynamic Commands",
            description = "Generate commands based on screen content",
            isChecked = dynamicCommandsEnabled,
            onCheckedChange = { enabled ->
                settingsViewModel.updateDynamicCommandsEnabled(enabled)
            }
        )
        
        SettingsToggle(
            title = "Command Caching",
            description = "Cache frequently used commands",
            isChecked = cacheEnabled,
            onCheckedChange = { enabled ->
                settingsViewModel.updateCacheEnabled(enabled)
            }
        )
        
        SettingsSlider(
            title = "Cache Size",
            description = "Maximum number of cached commands",
            value = maxCacheSize.toFloat(),
            range = 10f..500f,
            steps = 48,
            enabled = cacheEnabled,
            onValueChange = { value ->
                settingsViewModel.updateMaxCacheSize(value.toInt())
            }
        )
    }
}

@Composable
fun CursorSettingsSection(
    settingsViewModel: SettingsViewModel
) {
    val cursorEnabled by settingsViewModel.cursorEnabled.collectAsState()
    val cursorSize by settingsViewModel.cursorSize.collectAsState()
    val cursorSpeed by settingsViewModel.cursorSpeed.collectAsState()
    
    SettingsSection(
        title = "Cursor Settings",
        icon = Icons.Default.CenterFocusStrong
    ) {
        SettingsToggle(
            title = "Voice Cursor",
            description = "Enable voice-controlled cursor",
            isChecked = cursorEnabled,
            onCheckedChange = { enabled ->
                settingsViewModel.updateCursorEnabled(enabled)
            }
        )
        
        SettingsSlider(
            title = "Cursor Size",
            description = "Size of the cursor in dp",
            value = cursorSize,
            range = 24f..96f,
            steps = 8,
            enabled = cursorEnabled,
            onValueChange = { value ->
                settingsViewModel.updateCursorSize(value)
            }
        )
        
        SettingsSlider(
            title = "Cursor Speed",
            description = "Movement speed multiplier",
            value = cursorSpeed,
            range = 0.1f..3.0f,
            steps = 28,
            enabled = cursorEnabled,
            onValueChange = { value ->
                settingsViewModel.updateCursorSpeed(value)
            }
        )
    }
}

@Composable
fun AdvancedSettingsSection(
    settingsViewModel: SettingsViewModel
) {
    val uiScrapingEnabled by settingsViewModel.uiScrapingEnabled.collectAsState()
    val configuration by settingsViewModel.configuration.collectAsState()
    
    SettingsSection(
        title = "Advanced",
        icon = Icons.Default.Science
    ) {
        SettingsToggle(
            title = "UI Scraping",
            description = "Extract detailed screen information (experimental)",
            isChecked = uiScrapingEnabled,
            onCheckedChange = { enabled ->
                settingsViewModel.updateUiScrapingEnabled(enabled)
            }
        )
        
        SettingsToggle(
            title = "Fingerprint Gestures",
            description = "Use fingerprint sensor for gestures (if available)",
            isChecked = configuration.fingerprintGesturesEnabled,
            onCheckedChange = { _ ->
                // This feature is not implemented in SettingsViewModel yet
                // Could be added if needed
            }
        )
        
        // Reset to defaults button
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { settingsViewModel.resetToDefaults() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4285F4)
            )
        ) {
            Text(
                text = "Reset to Defaults",
                color = Color.White
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
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
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.12f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF4285F4)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            content()
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    description: String,
    isChecked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!isChecked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) Color.White else Color.Gray,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) Color.White.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.5f)
            )
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF4285F4),
                checkedTrackColor = Color(0xFF4285F4).copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun SettingsSlider(
    title: String,
    description: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) Color.White else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) Color.White.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.5f)
                )
            }
            
            Text(
                text = when {
                    value >= 1000 -> "${(value / 1000f).toInt()}k"
                    value.rem(1f) == 0f -> value.toInt().toString()
                    else -> String.format("%.1f", value)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) Color(0xFF4285F4) else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = if (enabled) onValueChange else { _ -> },
            valueRange = range,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF4285F4),
                activeTrackColor = Color(0xFF4285F4),
                inactiveTrackColor = Color.Gray.copy(alpha = 0.3f),
                disabledThumbColor = Color.Gray,
                disabledActiveTrackColor = Color.Gray.copy(alpha = 0.2f),
                disabledInactiveTrackColor = Color.Gray.copy(alpha = 0.1f)
            )
        )
    }
}

/**
 * Data class for handler information
 */
private data class HandlerInfo(
    val title: String,
    val description: String,
    val isEnabled: Boolean,
    val onToggle: (Boolean) -> Unit
)