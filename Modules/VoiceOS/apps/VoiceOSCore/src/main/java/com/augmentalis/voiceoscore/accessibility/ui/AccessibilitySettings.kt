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
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceoscore.accessibility.ui.theme.AccessibilityTheme
import com.augmentalis.voiceoscore.accessibility.ui.utils.DepthLevel
import com.augmentalis.voiceoscore.accessibility.ui.utils.GlassMorphismConfig
import com.augmentalis.voiceoscore.accessibility.ui.utils.glassMorphism
import com.augmentalis.voiceoscore.accessibility.viewmodel.PerformanceMode
import com.augmentalis.voiceoscore.accessibility.viewmodel.SettingsViewModel
import com.augmentalis.voiceoscore.cleanup.ui.CleanupPreviewActivity

class AccessibilitySettings : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    // P2 Task 2.3: Cleanup preview launcher
    private val cleanupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(
                this,
                "Cleanup completed successfully",
                Toast.LENGTH_SHORT
            ).show()
            settingsViewModel.refreshCleanupInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val lastCleanupTimestamp by settingsViewModel.lastCleanupTimestamp.collectAsState()
            val lastCleanupDeletedCount by settingsViewModel.lastCleanupDeletedCount.collectAsState()

            AccessibilityTheme {
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    onNavigateBack = { finish() },
                    lastCleanupTimestamp = lastCleanupTimestamp,
                    lastCleanupDeletedCount = lastCleanupDeletedCount,
                    onRunCleanup = {
                        cleanupLauncher.launch(CleanupPreviewActivity.createIntent(this))
                    }
                )
            }
        }
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    lastCleanupTimestamp: Long? = null,  // P2 Task 2.3
    lastCleanupDeletedCount: Int = 0,     // P2 Task 2.3
    onRunCleanup: () -> Unit = {}         // P2 Task 2.3
) {
    val context = LocalContext.current
    val spacing = getSettingsAdaptiveSpacing()
    val localConfig = LocalConfiguration.current
    val isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE

    @Suppress("UNUSED_VARIABLE")
    val configuration by settingsViewModel.configuration.collectAsState()
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
            SettingsTopBar(
                onNavigateBack = onNavigateBack,
                onOpenSystemSettings = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
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
                                modifier = Modifier.padding(spacing.cardPadding),
                                color = Color(0xFFFF5722),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

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
                }

                // P2 Task 2.3: Command Management Section
                item {
                    CommandManagementSection(
                        lastCleanupTimestamp = lastCleanupTimestamp,
                        lastCleanupDeletedCount = lastCleanupDeletedCount,
                        onRunCleanup = onRunCleanup,
                        spacing = spacing
                    )
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF2196F3))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    onNavigateBack: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    isLandscape: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 0.dp,
                    backgroundOpacity = 0.08f,
                    borderOpacity = 0.1f,
                    borderWidth = 0.dp,
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.1f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(0.dp)
    ) {
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
    }
}

@Composable
fun ServiceControlSection(
    settingsViewModel: SettingsViewModel,
    spacing: SettingsAdaptiveSpacing
) {
    val showToasts by settingsViewModel.showToasts.collectAsState()
    val verboseLogging by settingsViewModel.verboseLogging.collectAsState()
    val configuration by settingsViewModel.configuration.collectAsState()

    SettingsSection(
        title = "Service Control",
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
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageControlSection(
    settingsViewModel: SettingsViewModel,
    spacing: SettingsAdaptiveSpacing
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = settingsViewModel.selectedOption.value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Language", style = MaterialTheme.typography.labelSmall) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            settingsViewModel.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodySmall) },
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
            SettingsToggle(
                title = handler.name,
                description = handler.description,
                isChecked = isEnabled,
                enabled = !handler.isCore,
                onCheckedChange = { settingsViewModel.toggleHandler(handler.id, it) },
                spacing = spacing
            )
        }
    }
}

@Composable
fun PerformanceSettingsSection(
    settingsViewModel: SettingsViewModel,
    performanceMode: PerformanceMode,
    spacing: SettingsAdaptiveSpacing
) {
    val dynamicCommandsEnabled by settingsViewModel.dynamicCommandsEnabled.collectAsState()
    val cacheEnabled by settingsViewModel.cacheEnabled.collectAsState()
    val maxCacheSize by settingsViewModel.maxCacheSize.collectAsState()

    @Suppress("UNUSED_VARIABLE")
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
        )

        PerformanceMode.values().forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { settingsViewModel.updatePerformanceMode(mode) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = performanceMode == mode,
                    onClick = { settingsViewModel.updatePerformanceMode(mode) },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4285F4)),
                    modifier = Modifier.size(spacing.iconSize + 8.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = mode.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

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
            value = maxCacheSize.toFloat(),
            range = 10f..500f,
            steps = 48,
            enabled = cacheEnabled,
            onValueChange = { settingsViewModel.updateMaxCacheSize(it.toInt()) },
            spacing = spacing
        )
    }
}

@Composable
fun CursorSettingsSection(
    settingsViewModel: SettingsViewModel,
    spacing: SettingsAdaptiveSpacing
) {
    val cursorEnabled by settingsViewModel.cursorEnabled.collectAsState()
    val cursorSize by settingsViewModel.cursorSize.collectAsState()
    val cursorSpeed by settingsViewModel.cursorSpeed.collectAsState()

    SettingsSection(
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
            value = cursorSize,
            range = 24f..96f,
            steps = 8,
            enabled = cursorEnabled,
            onValueChange = { settingsViewModel.updateCursorSize(it) },
            spacing = spacing
        )
        SettingsSlider(
            title = "Speed",
            description = "Movement speed",
            value = cursorSpeed,
            range = 0.1f..3.0f,
            steps = 28,
            enabled = cursorEnabled,
            onValueChange = { settingsViewModel.updateCursorSpeed(it) },
            spacing = spacing
        )
    }
}

@Composable
fun AdvancedSettingsSection(
    settingsViewModel: SettingsViewModel,
    spacing: SettingsAdaptiveSpacing
) {
    val uiScrapingEnabled by settingsViewModel.uiScrapingEnabled.collectAsState()
    val configuration by settingsViewModel.configuration.collectAsState()

    SettingsSection(
        title = "Advanced",
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

        Button(
            onClick = { settingsViewModel.resetToDefaults() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
            contentPadding = PaddingValues(vertical = spacing.cardPadding / 2)
        ) {
            Text(
                text = "Reset",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    spacing: SettingsAdaptiveSpacing,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 10.dp,
                    backgroundOpacity = 0.08f,
                    borderOpacity = 0.1f,
                    borderWidth = 0.5.dp,
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.08f
                ),
                depth = DepthLevel(0.5f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = spacing.itemSpacing / 2)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(spacing.iconSize),
                    tint = Color(0xFF4285F4)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
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
    onCheckedChange: (Boolean) -> Unit,
    spacing: SettingsAdaptiveSpacing
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!isChecked) }
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
    onValueChange: (Float) -> Unit,
    spacing: SettingsAdaptiveSpacing
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
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
            Text(
                text = when {
                    value >= 1000 -> "${(value / 1000f).toInt()}k"
                    value.rem(1f) == 0f -> value.toInt().toString()
                    else -> String.format("%.1f", value)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) Color(0xFF4285F4) else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

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
 * P2 Task 2.3: Command Management Section
 * Shows last cleanup info and provides manual cleanup trigger
 */
@Composable
fun CommandManagementSection(
    lastCleanupTimestamp: Long?,
    lastCleanupDeletedCount: Int,
    onRunCleanup: () -> Unit,
    spacing: SettingsAdaptiveSpacing
) {
    SettingsSection(
        title = "Command Management",
        icon = Icons.Default.CleaningServices,
        spacing = spacing
    ) {
        Text(
            text = "Clean up deprecated commands to free space",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = spacing.itemSpacing)
        )

        // Last cleanup info
        lastCleanupTimestamp?.let { timestamp ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.itemSpacing),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F1E2E).copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.cardPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Last cleanup",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64B5F6)
                        )
                        val daysAgo = ((System.currentTimeMillis() - timestamp) / 86400000L).toInt()
                        val timeText = when {
                            daysAgo == 0 -> "Today"
                            daysAgo == 1 -> "Yesterday"
                            daysAgo < 7 -> "$daysAgo days ago"
                            daysAgo < 30 -> "${daysAgo / 7} weeks ago"
                            else -> "${daysAgo / 30} months ago"
                        }
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFF7043),
                            modifier = Modifier.size(spacing.iconSize)
                        )
                        Text(
                            text = "$lastCleanupDeletedCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF7043),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Cleanup button
        Button(
            onClick = onRunCleanup,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            ),
            contentPadding = PaddingValues(vertical = spacing.cardPadding)
        ) {
            Icon(
                imageVector = Icons.Default.CleaningServices,
                contentDescription = null,
                modifier = Modifier.size(spacing.iconSize)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Run Cleanup Now",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
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