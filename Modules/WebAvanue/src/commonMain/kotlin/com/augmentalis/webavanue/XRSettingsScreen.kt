package com.augmentalis.webavanue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.augmentalis.avanueui.theme.AvanueTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.BrowserSettings

/**
 * XR Settings Screen - Configure WebXR preferences.
 *
 * REQ-XR-007: Performance Optimization (user control)
 *
 * Settings:
 * - Enable/disable WebXR
 * - AR/VR toggles
 * - Performance mode
 * - Auto-pause timeout
 * - FPS indicator
 * - WiFi-only mode
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XRSettingsScreen(
    settings: BrowserSettings,
    onSettingsChange: (BrowserSettings) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebXR Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master switch
            SettingsCard(
                title = "WebXR Support",
                icon = Icons.Default.Info
            ) {
                SwitchSettingItem(
                    title = "Enable WebXR",
                    description = "Allow websites to use AR/VR features",
                    checked = settings.enableWebXR,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(enableWebXR = it))
                    },
                    icon = Icons.Default.Info
                )
            }

            // Session types
            SettingsCard(
                title = "Session Types",
                icon = Icons.Default.Info,
                enabled = settings.enableWebXR
            ) {
                SwitchSettingItem(
                    title = "Augmented Reality (AR)",
                    description = "Camera-based AR experiences",
                    checked = settings.enableAR,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(enableAR = it))
                    },
                    enabled = settings.enableWebXR,
                    icon = Icons.Default.Info
                )

                HorizontalDivider()

                SwitchSettingItem(
                    title = "Virtual Reality (VR)",
                    description = "360° immersive VR experiences",
                    checked = settings.enableVR,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(enableVR = it))
                    },
                    enabled = settings.enableWebXR,
                    icon = Icons.Default.Info
                )
            }

            // Performance
            SettingsCard(
                title = "Performance",
                icon = Icons.Default.Info,
                enabled = settings.enableWebXR
            ) {
                SelectionSettingItem(
                    title = "Performance Mode",
                    description = getPerformanceModeDescription(settings.xrPerformanceMode),
                    currentValue = settings.xrPerformanceMode.name,
                    options = BrowserSettings.XRPerformanceMode.values().map { it.name },
                    onValueChange = { newMode ->
                        val mode = BrowserSettings.XRPerformanceMode.valueOf(newMode)
                        onSettingsChange(settings.copy(xrPerformanceMode = mode))
                    },
                    enabled = settings.enableWebXR,
                    icon = Icons.Default.Info
                )

                HorizontalDivider()

                SliderSettingItem(
                    title = "Auto-Pause Timeout",
                    description = "Pause XR sessions after ${settings.xrAutoPauseTimeout} minutes of inactivity",
                    value = settings.xrAutoPauseTimeout.toFloat(),
                    valueRange = 5f..60f,
                    steps = 10,
                    onValueChange = {
                        onSettingsChange(settings.copy(xrAutoPauseTimeout = it.toInt()))
                    },
                    enabled = settings.enableWebXR,
                    icon = Icons.Default.Info
                )

                HorizontalDivider()

                SwitchSettingItem(
                    title = "Show FPS Indicator",
                    description = "Display frame rate during XR sessions",
                    checked = settings.xrShowFPSIndicator,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(xrShowFPSIndicator = it))
                    },
                    enabled = settings.enableWebXR,
                    icon = Icons.Default.Info
                )
            }

            // Data usage
            SettingsCard(
                title = "Data Usage",
                icon = Icons.Default.Info,
                enabled = settings.enableWebXR
            ) {
                SwitchSettingItem(
                    title = "WiFi Only",
                    description = "Only allow XR sessions on WiFi networks",
                    checked = settings.xrRequireWiFi,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(xrRequireWiFi = it))
                    },
                    enabled = settings.enableWebXR,
                    icon = Icons.Default.Info
                )
            }

            // Info card
            InfoCard(enabled = settings.enableWebXR)
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) AvanueTheme.colors.primary else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) AvanueTheme.colors.textPrimary else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f)
                )
            }

            HorizontalDivider()

            // Content
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SwitchSettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) AvanueTheme.colors.textSecondary else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) AvanueTheme.colors.textPrimary else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f)
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) AvanueTheme.colors.textSecondary else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionSettingItem(
    title: String,
    description: String,
    currentValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) AvanueTheme.colors.textSecondary else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f),
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) AvanueTheme.colors.textPrimary else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f)
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) AvanueTheme.colors.textSecondary else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (enabled) expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = currentValue.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    enabled = enabled
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.replace("_", " ")) },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderSettingItem(
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) AvanueTheme.colors.textSecondary else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f),
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) AvanueTheme.colors.textPrimary else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f)
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) AvanueTheme.colors.textSecondary else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun InfoCard(enabled: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = AvanueTheme.colors.primaryContainer.copy(alpha = if (enabled) 1f else 0.38f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = if (enabled) AvanueTheme.colors.onPrimaryContainer else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f)
            )

            Column {
                Text(
                    text = "About WebXR",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) AvanueTheme.colors.onPrimaryContainer else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "WebXR allows websites to create immersive AR and VR experiences directly in the browser. " +
                            "Your privacy is protected - camera and sensor data stays on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) AvanueTheme.colors.onPrimaryContainer else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f)
                )
            }
        }
    }
}

private fun getPerformanceModeDescription(mode: BrowserSettings.XRPerformanceMode): String {
    return when (mode) {
        BrowserSettings.XRPerformanceMode.HIGH_QUALITY -> "90fps target, maximum quality, higher battery drain"
        BrowserSettings.XRPerformanceMode.BALANCED -> "60fps target, balanced quality and battery"
        BrowserSettings.XRPerformanceMode.BATTERY_SAVER -> "45fps target, extended battery life"
    }
}
