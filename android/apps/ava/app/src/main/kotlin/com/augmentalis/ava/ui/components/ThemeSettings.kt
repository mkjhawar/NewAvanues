// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/components/ThemeSettings.kt
// created: 2025-11-22
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.ui.components

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.augmentalis.ava.preferences.ThemeMode
import com.augmentalis.ava.ui.theme.AccentColor
import kotlinx.coroutines.launch

/**
 * Theme Settings Component
 *
 * Complete theme customization UI for AVA AI settings screen.
 * Includes theme mode, accent color, and dynamic color options.
 *
 * @param currentThemeMode Current theme mode from preferences
 * @param currentAccentColor Current accent color from preferences
 * @param useDynamicColor Whether dynamic color is enabled
 * @param onThemeModeChanged Callback when theme mode changes
 * @param onAccentColorChanged Callback when accent color changes
 * @param onDynamicColorChanged Callback when dynamic color toggle changes
 * @param modifier Optional modifier
 *
 * @author AVA AI Team
 * @version 1.0.0
 */
@Composable
fun ThemeSettings(
    currentThemeMode: ThemeMode,
    currentAccentColor: AccentColor,
    useDynamicColor: Boolean,
    onThemeModeChanged: suspend (ThemeMode) -> Unit,
    onAccentColorChanged: suspend (AccentColor) -> Unit,
    onDynamicColorChanged: suspend (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val systemInDarkMode = isSystemInDarkTheme()

    // Determine if dark mode is active based on current settings
    val isDarkMode = when (currentThemeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> systemInDarkMode
    }

    // Check if dynamic color is available (Android 12+)
    val isDynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Theme settings section" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section Title
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Divider()

            // Theme Mode Picker
            ThemeModePicker(
                selectedMode = currentThemeMode,
                onModeSelected = { mode ->
                    scope.launch {
                        onThemeModeChanged(mode)
                    }
                }
            )

            Divider()

            // Material You Toggle (only on Android 12+)
            if (isDynamicColorAvailable) {
                DynamicColorToggle(
                    enabled = useDynamicColor,
                    onEnabledChange = { enabled ->
                        scope.launch {
                            onDynamicColorChanged(enabled)
                        }
                    },
                    isAvailable = true
                )

                Divider()
            }

            // Accent Color Picker (disabled if dynamic color is enabled)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = if (useDynamicColor) {
                            "Accent color picker disabled when Material You is enabled"
                        } else {
                            "Accent color picker"
                        }
                    }
            ) {
                if (useDynamicColor && isDynamicColorAvailable) {
                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Disabled when Material You is active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                } else {
                    AccentColorPicker(
                        selectedColor = currentAccentColor,
                        onColorSelected = { color ->
                            scope.launch {
                                onAccentColorChanged(color)
                            }
                        },
                        isDarkMode = isDarkMode
                    )
                }
            }

            Divider()

            // Theme Preview
            ThemePreviewCard(
                themeMode = currentThemeMode,
                accentColor = currentAccentColor,
                isDarkMode = isDarkMode
            )

            // Info text
            Text(
                text = "Theme changes apply immediately. Your selection is saved automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Compact Theme Settings for Settings Screen
 *
 * A more compact version suitable for inclusion in the settings list
 */
@Composable
fun CompactThemeSettings(
    currentThemeMode: ThemeMode,
    currentAccentColor: AccentColor,
    useDynamicColor: Boolean,
    onThemeModeChanged: suspend (ThemeMode) -> Unit,
    onAccentColorChanged: suspend (AccentColor) -> Unit,
    onDynamicColorChanged: suspend (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val systemInDarkMode = isSystemInDarkTheme()

    val isDarkMode = when (currentThemeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> systemInDarkMode
    }

    val isDynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme Mode Picker
        ThemeModePicker(
            selectedMode = currentThemeMode,
            onModeSelected = { mode ->
                scope.launch {
                    onThemeModeChanged(mode)
                }
            }
        )

        // Material You Toggle
        if (isDynamicColorAvailable) {
            DynamicColorToggle(
                enabled = useDynamicColor,
                onEnabledChange = { enabled ->
                    scope.launch {
                        onDynamicColorChanged(enabled)
                    }
                },
                isAvailable = true
            )
        }

        // Accent Color Picker (if not using dynamic colors)
        if (!useDynamicColor || !isDynamicColorAvailable) {
            AccentColorPicker(
                selectedColor = currentAccentColor,
                onColorSelected = { color ->
                    scope.launch {
                        onAccentColorChanged(color)
                    }
                },
                isDarkMode = isDarkMode
            )
        }
    }
}
