/**
 * AccessibilitySettings.kt - Settings screen for VoiceOS Accessibility
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-01-28
 */
package com.augmentalis.voiceoscore.accessibility.ui

import android.content.Intent
<<<<<<< HEAD
import android.content.res.Configuration
=======
>>>>>>> AVA-Development
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
<<<<<<< HEAD
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
=======
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
>>>>>>> AVA-Development
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceoscore.accessibility.ui.theme.AccessibilityTheme
import com.augmentalis.voiceoscore.accessibility.ui.utils.DepthLevel
import com.augmentalis.voiceoscore.accessibility.ui.utils.GlassMorphismConfig
import com.augmentalis.voiceoscore.accessibility.ui.utils.glassMorphism
import com.augmentalis.voiceoscore.accessibility.viewmodel.PerformanceMode
import com.augmentalis.voiceoscore.accessibility.viewmodel.SettingsViewModel

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

<<<<<<< HEAD
/**
 * Adaptive spacing for Settings screen based on orientation
 */
data class SettingsAdaptiveSpacing(
    val horizontalPadding: Dp,
    val cardPadding: Dp,
    val itemSpacing: Dp,
    val iconSize: Dp
)

/**
 * Get adaptive spacing for settings screen
 */
@Composable
fun getSettingsAdaptiveSpacing(): SettingsAdaptiveSpacing {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    return if (isLandscape) {
        SettingsAdaptiveSpacing(
            horizontalPadding = 12.dp,
            cardPadding = 10.dp,
            itemSpacing = 8.dp,
            iconSize = 18.dp
        )
    } else {
        SettingsAdaptiveSpacing(
            horizontalPadding = 16.dp,
            cardPadding = 14.dp,
            itemSpacing = 12.dp,
            iconSize = 20.dp
        )
    }
}

=======
>>>>>>> AVA-Development
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
<<<<<<< HEAD
    val spacing = getSettingsAdaptiveSpacing()
    val localConfig = LocalConfiguration.current
    val isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE

    @Suppress("UNUSED_VARIABLE")
    val configuration by settingsViewModel.configuration.collectAsState()
=======

    @Suppress("UNUSED_VARIABLE")
    val configuration by settingsViewModel.configuration.collectAsState() // Configuration passed to child components
>>>>>>> AVA-Development
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
<<<<<<< HEAD
=======
            // Top App Bar
>>>>>>> AVA-Development
            SettingsTopBar(
                onNavigateBack = onNavigateBack,
                onOpenSystemSettings = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
<<<<<<< HEAD
                },
                isLandscape = isLandscape
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing),
                contentPadding = PaddingValues(vertical = spacing.itemSpacing)
            ) {
=======
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
>>>>>>> AVA-Development
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
<<<<<<< HEAD
                                modifier = Modifier.padding(spacing.cardPadding),
                                color = Color(0xFFFF5722),
                                style = MaterialTheme.typography.bodySmall
=======
                                modifier = Modifier.padding(16.dp),
                                color = Color(0xFFFF5722),
                                style = MaterialTheme.typography.bodyMedium
>>>>>>> AVA-Development
                            )
                        }
                    }
                }
<<<<<<< HEAD

                item {
                    LanguageControlSection(settingsViewModel = settingsViewModel, spacing = spacing)
                }

                item {
                    ServiceControlSection(settingsViewModel = settingsViewModel, spacing = spacing)
                }

                item {
                    HandlerTogglesSection(
                        settingsViewModel = settingsViewModel,
                        handlerStates = handlerStates,
                        spacing = spacing
                    )
                }

                item {
                    PerformanceSettingsSection(
                        settingsViewModel = settingsViewModel,
                        performanceMode = performanceMode,
                        spacing = spacing
                    )
                }

                item {
                    CursorSettingsSection(settingsViewModel = settingsViewModel, spacing = spacing)
                }

                item {
                    AdvancedSettingsSection(settingsViewModel = settingsViewModel, spacing = spacing)
=======
                // Service Control Section
                item {
                    LanguageControlSection(
                        settingsViewModel = settingsViewModel
                    )
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
>>>>>>> AVA-Development
                }
            }
        }

<<<<<<< HEAD
=======
        // Loading overlay
>>>>>>> AVA-Development
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
<<<<<<< HEAD
                CircularProgressIndicator(color = Color(0xFF2196F3))
=======
                CircularProgressIndicator(
                    color = Color(0xFF2196F3)
                )
>>>>>>> AVA-Development
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    onNavigateBack: () -> Unit,
<<<<<<< HEAD
    onOpenSystemSettings: () -> Unit,
    isLandscape: Boolean = false
=======
    onOpenSystemSettings: () -> Unit
>>>>>>> AVA-Development
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 0.dp,
<<<<<<< HEAD
                    backgroundOpacity = 0.08f,
                    borderOpacity = 0.1f,
                    borderWidth = 0.dp,
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.1f
                ),
                depth = DepthLevel(0.6f)
=======
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 0.dp,
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.8f)
>>>>>>> AVA-Development
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(0.dp)
    ) {
<<<<<<< HEAD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isLandscape) 4.dp else 8.dp, vertical = if (isLandscape) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp)
                )
            }
            Text(
                text = "Settings",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = if (isLandscape) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenSystemSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "System Settings",
                    tint = Color.White,
                    modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp)
                )
            }
        }
=======
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
>>>>>>> AVA-Development
    }
}

@Composable
fun ServiceControlSection(
<<<<<<< HEAD
    settingsViewModel: SettingsViewModel,
    spacing: SettingsAdaptiveSpacing
=======
    settingsViewModel: SettingsViewModel
>>>>>>> AVA-Development
) {
    val showToasts by settingsViewModel.showToasts.collectAsState()
    val verboseLogging by settingsViewModel.verboseLogging.collectAsState()
    val configuration by settingsViewModel.configuration.collectAsState()

    SettingsSection(
        title = "Service Control",
<<<<<<< HEAD
        icon = Icons.Default.Power,
        spacing = spacing
    ) {
        SettingsToggle(
            title = "Service Enabled",
            description = "Enable/disable accessibility service",
            isChecked = configuration.isEnabled,
            onCheckedChange = { },
            spacing = spacing
        )
        SettingsToggle(
            title = "Show Notifications",
            description = "Display toast messages",
            isChecked = showToasts,
            onCheckedChange = { settingsViewModel.updateShowToasts(it) },
            spacing = spacing
        )
        SettingsToggle(
            title = "Verbose Logging",
            description = "Enable debug logging",
            isChecked = verboseLogging,
            onCheckedChange = { settingsViewModel.updateVerboseLogging(it) },
            spacing = spacing
=======
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
>>>>>>> AVA-Development
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageControlSection(
<<<<<<< HEAD
    settingsViewModel: SettingsViewModel,
    spacing: SettingsAdaptiveSpacing
) {
    var expanded by remember { mutableStateOf(false) }
=======
    settingsViewModel: SettingsViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
>>>>>>> AVA-Development
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = settingsViewModel.selectedOption.value,
            onValueChange = {},
            readOnly = true,
<<<<<<< HEAD
            label = { Text("Language", style = MaterialTheme.typography.labelSmall) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall
=======
            label = { Text("Select an option") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
>>>>>>> AVA-Development
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            settingsViewModel.options.forEach { option ->
                DropdownMenuItem(
<<<<<<< HEAD
                    text = { Text(option, style = MaterialTheme.typography.bodySmall) },
=======
                    text = { Text(option) },
>>>>>>> AVA-Development
                    onClick = {
                        settingsViewModel.onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun HandlerTogglesSection(
    settingsViewModel: SettingsViewModel,
<<<<<<< HEAD
    handlerStates: Map<String, Boolean>,
    spacing: SettingsAdaptiveSpacing
) {
    SettingsSection(
        title = "Handlers",
        icon = Icons.Default.Tune,
        spacing = spacing
    ) {
        settingsViewModel.getAllHandlerDefinitions().forEach { handler ->
            val isEnabled = handlerStates[handler.id] ?: false
=======
    handlerStates: Map<String, Boolean>
) {
    SettingsSection(
        title = "Command Handlers",
        icon = Icons.Default.Tune
    ) {
        settingsViewModel.getAllHandlerDefinitions().forEach { handler ->
            val isEnabled = handlerStates[handler.id] ?: false

>>>>>>> AVA-Development
            SettingsToggle(
                title = handler.name,
                description = handler.description,
                isChecked = isEnabled,
<<<<<<< HEAD
                enabled = !handler.isCore,
                onCheckedChange = { settingsViewModel.toggleHandler(handler.id, it) },
                spacing = spacing
=======
                enabled = !handler.isCore, // Core handlers cannot be disabled
                onCheckedChange = { enabled ->
                    settingsViewModel.toggleHandler(handler.id, enabled)
                }
>>>>>>> AVA-Development
            )
        }
    }
}

@Composable
fun PerformanceSettingsSection(
    settingsViewModel: SettingsViewModel,
<<<<<<< HEAD
    performanceMode: PerformanceMode,
    spacing: SettingsAdaptiveSpacing
=======
    performanceMode: PerformanceMode
>>>>>>> AVA-Development
) {
    val dynamicCommandsEnabled by settingsViewModel.dynamicCommandsEnabled.collectAsState()
    val cacheEnabled by settingsViewModel.cacheEnabled.collectAsState()
    val maxCacheSize by settingsViewModel.maxCacheSize.collectAsState()

    @Suppress("UNUSED_VARIABLE")
<<<<<<< HEAD
    val configuration by settingsViewModel.configuration.collectAsState()

    SettingsSection(
        title = "Performance",
        icon = Icons.Default.Speed,
        spacing = spacing
    ) {
        Text(
            text = "Mode",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
=======
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
>>>>>>> AVA-Development
        )

        PerformanceMode.values().forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { settingsViewModel.updatePerformanceMode(mode) }
<<<<<<< HEAD
                    .padding(vertical = 2.dp),
=======
                    .padding(vertical = 4.dp),
>>>>>>> AVA-Development
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = performanceMode == mode,
                    onClick = { settingsViewModel.updatePerformanceMode(mode) },
<<<<<<< HEAD
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4285F4)),
                    modifier = Modifier.size(spacing.iconSize + 8.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.bodySmall,
=======
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF4285F4)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.bodyMedium,
>>>>>>> AVA-Development
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = mode.description,
<<<<<<< HEAD
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
=======
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
>>>>>>> AVA-Development
                    )
                }
            }
        }

<<<<<<< HEAD
        Spacer(modifier = Modifier.height(spacing.itemSpacing))

        SettingsToggle(
            title = "Dynamic Commands",
            description = "Generate from screen",
            isChecked = dynamicCommandsEnabled,
            onCheckedChange = { settingsViewModel.updateDynamicCommandsEnabled(it) },
            spacing = spacing
        )
        SettingsToggle(
            title = "Caching",
            description = "Cache frequent commands",
            isChecked = cacheEnabled,
            onCheckedChange = { settingsViewModel.updateCacheEnabled(it) },
            spacing = spacing
        )
        SettingsSlider(
            title = "Cache Size",
            description = "Max cached commands",
=======
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
>>>>>>> AVA-Development
            value = maxCacheSize.toFloat(),
            range = 10f..500f,
            steps = 48,
            enabled = cacheEnabled,
<<<<<<< HEAD
            onValueChange = { settingsViewModel.updateMaxCacheSize(it.toInt()) },
            spacing = spacing
=======
            onValueChange = { value ->
                settingsViewModel.updateMaxCacheSize(value.toInt())
            }
>>>>>>> AVA-Development
        )
    }
}

@Composable
fun CursorSettingsSection(
<<<<<<< HEAD
    settingsViewModel: SettingsViewModel,
    spacing: SettingsAdaptiveSpacing
=======
    settingsViewModel: SettingsViewModel
>>>>>>> AVA-Development
) {
    val cursorEnabled by settingsViewModel.cursorEnabled.collectAsState()
    val cursorSize by settingsViewModel.cursorSize.collectAsState()
    val cursorSpeed by settingsViewModel.cursorSpeed.collectAsState()

    SettingsSection(
<<<<<<< HEAD
        title = "Cursor",
        icon = Icons.Default.CenterFocusStrong,
        spacing = spacing
    ) {
        SettingsToggle(
            title = "Voice Cursor",
            description = "Enable voice cursor",
            isChecked = cursorEnabled,
            onCheckedChange = { settingsViewModel.updateCursorEnabled(it) },
            spacing = spacing
        )
        SettingsSlider(
            title = "Size",
            description = "Cursor size (dp)",
=======
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
>>>>>>> AVA-Development
            value = cursorSize,
            range = 24f..96f,
            steps = 8,
            enabled = cursorEnabled,
<<<<<<< HEAD
            onValueChange = { settingsViewModel.updateCursorSize(it) },
            spacing = spacing
        )
        SettingsSlider(
            title = "Speed",
            description = "Movement speed",
=======
            onValueChange = { value ->
                settingsViewModel.updateCursorSize(value)
            }
        )

        SettingsSlider(
            title = "Cursor Speed",
            description = "Movement speed multiplier",
>>>>>>> AVA-Development
            value = cursorSpeed,
            range = 0.1f..3.0f,
            steps = 28,
            enabled = cursorEnabled,
<<<<<<< HEAD
            onValueChange = { settingsViewModel.updateCursorSpeed(it) },
            spacing = spacing
=======
            onValueChange = { value ->
                settingsViewModel.updateCursorSpeed(value)
            }
>>>>>>> AVA-Development
        )
    }
}

@Composable
fun AdvancedSettingsSection(
<<<<<<< HEAD
    settingsViewModel: SettingsViewModel,
    spacing: SettingsAdaptiveSpacing
=======
    settingsViewModel: SettingsViewModel
>>>>>>> AVA-Development
) {
    val uiScrapingEnabled by settingsViewModel.uiScrapingEnabled.collectAsState()
    val configuration by settingsViewModel.configuration.collectAsState()

    SettingsSection(
        title = "Advanced",
<<<<<<< HEAD
        icon = Icons.Default.Science,
        spacing = spacing
    ) {
        SettingsToggle(
            title = "UI Scraping",
            description = "Extract screen info (experimental)",
            isChecked = uiScrapingEnabled,
            onCheckedChange = { settingsViewModel.updateUiScrapingEnabled(it) },
            spacing = spacing
        )
        SettingsToggle(
            title = "Fingerprint Gestures",
            description = "Use fingerprint for gestures",
            isChecked = configuration.fingerprintGesturesEnabled,
            onCheckedChange = { },
            spacing = spacing
        )

        Spacer(modifier = Modifier.height(spacing.itemSpacing))
=======
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
>>>>>>> AVA-Development

        Button(
            onClick = { settingsViewModel.resetToDefaults() },
            modifier = Modifier.fillMaxWidth(),
<<<<<<< HEAD
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
            contentPadding = PaddingValues(vertical = spacing.cardPadding / 2)
        ) {
            Text(
                text = "Reset",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
=======
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4285F4)
            )
        ) {
            Text(
                text = "Reset to Defaults",
                color = Color.White
>>>>>>> AVA-Development
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
<<<<<<< HEAD
    spacing: SettingsAdaptiveSpacing,
=======
>>>>>>> AVA-Development
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
<<<<<<< HEAD
                    cornerRadius = 10.dp,
                    backgroundOpacity = 0.08f,
                    borderOpacity = 0.1f,
                    borderWidth = 0.5.dp,
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.08f
                ),
                depth = DepthLevel(0.5f)
=======
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.12f
                ),
                depth = DepthLevel(0.6f)
>>>>>>> AVA-Development
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
<<<<<<< HEAD
            modifier = Modifier.padding(spacing.cardPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = spacing.itemSpacing / 2)
=======
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
>>>>>>> AVA-Development
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
<<<<<<< HEAD
                    modifier = Modifier.size(spacing.iconSize),
                    tint = Color(0xFF4285F4)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
=======
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF4285F4)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
>>>>>>> AVA-Development
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
<<<<<<< HEAD
=======

>>>>>>> AVA-Development
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
<<<<<<< HEAD
    onCheckedChange: (Boolean) -> Unit,
    spacing: SettingsAdaptiveSpacing
=======
    onCheckedChange: (Boolean) -> Unit
>>>>>>> AVA-Development
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!isChecked) }
<<<<<<< HEAD
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) Color.White else Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) Color.White.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.4f)
            )
        }
=======
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

>>>>>>> AVA-Development
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
<<<<<<< HEAD
    onValueChange: (Float) -> Unit,
    spacing: SettingsAdaptiveSpacing
=======
    onValueChange: (Float) -> Unit
>>>>>>> AVA-Development
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
<<<<<<< HEAD
            .padding(vertical = 2.dp)
=======
            .padding(vertical = 8.dp)
>>>>>>> AVA-Development
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
<<<<<<< HEAD
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) Color.White else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) Color.White.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.4f)
                )
            }
=======
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

>>>>>>> AVA-Development
            Text(
                text = when {
                    value >= 1000 -> "${(value / 1000f).toInt()}k"
                    value.rem(1f) == 0f -> value.toInt().toString()
                    else -> String.format("%.1f", value)
                },
<<<<<<< HEAD
                style = MaterialTheme.typography.labelMedium,
=======
                style = MaterialTheme.typography.bodyLarge,
>>>>>>> AVA-Development
                color = if (enabled) Color(0xFF4285F4) else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }

<<<<<<< HEAD
        Spacer(modifier = Modifier.height(2.dp))
=======
        Spacer(modifier = Modifier.height(8.dp))
>>>>>>> AVA-Development

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