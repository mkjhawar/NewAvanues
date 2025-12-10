// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/components/ThemePicker.kt
// created: 2025-11-22
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.augmentalis.ava.preferences.ThemeMode
import com.augmentalis.ava.ui.theme.AccentColor

/**
 * Theme Mode Picker
 *
 * Allows users to select theme mode: Light, Dark, or Auto (system)
 *
 * @param selectedMode Currently selected theme mode
 * @param onModeSelected Callback when theme mode is selected
 * @param modifier Optional modifier
 *
 * @author AVA AI Team
 * @version 1.0.0
 */
@Composable
fun ThemeModePicker(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Theme mode selector" }
    ) {
        Text(
            text = "Theme Mode",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemeModeOption(
                mode = ThemeMode.LIGHT,
                icon = Icons.Default.LightMode,
                label = "Light",
                isSelected = selectedMode == ThemeMode.LIGHT,
                onClick = { onModeSelected(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f)
            )

            ThemeModeOption(
                mode = ThemeMode.DARK,
                icon = Icons.Default.DarkMode,
                label = "Dark",
                isSelected = selectedMode == ThemeMode.DARK,
                onClick = { onModeSelected(ThemeMode.DARK) },
                modifier = Modifier.weight(1f)
            )

            ThemeModeOption(
                mode = ThemeMode.AUTO,
                icon = Icons.Default.Brightness6,
                label = "Auto",
                isSelected = selectedMode == ThemeMode.AUTO,
                onClick = { onModeSelected(ThemeMode.AUTO) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Theme Mode Option Card
 *
 * Individual option card for theme mode selection
 */
@Composable
private fun ThemeModeOption(
    mode: ThemeMode,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                contentDescription = "$label theme mode, ${if (isSelected) "selected" else "not selected"}"
            },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Accent Color Picker
 *
 * Allows users to select custom accent color for the app
 *
 * @param selectedColor Currently selected accent color
 * @param onColorSelected Callback when accent color is selected
 * @param isDarkMode Whether dark mode is currently active
 * @param modifier Optional modifier
 *
 * @author AVA AI Team
 * @version 1.0.0
 */
@Composable
fun AccentColorPicker(
    selectedColor: AccentColor,
    onColorSelected: (AccentColor) -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Accent color selector" }
    ) {
        Text(
            text = "Accent Color",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(AccentColor.values()) { color ->
                AccentColorOption(
                    accentColor = color,
                    isSelected = selectedColor == color,
                    isDarkMode = isDarkMode,
                    onClick = { onColorSelected(color) }
                )
            }
        }
    }
}

/**
 * Accent Color Option
 *
 * Individual color option in accent color picker
 */
@Composable
private fun AccentColorOption(
    accentColor: AccentColor,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val colorValue = accentColor.getColor(isDarkMode)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .semantics {
                role = Role.RadioButton
                contentDescription = "${accentColor.displayName} accent color, ${if (isSelected) "selected" else "not selected"}"
            }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colorValue)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = if (isDarkMode) Color.Black else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = accentColor.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Dynamic Color Toggle
 *
 * Toggle switch for Material You dynamic colors (Android 12+)
 *
 * @param enabled Whether dynamic color is enabled
 * @param onEnabledChange Callback when toggle state changes
 * @param isAvailable Whether dynamic color is available on this device
 * @param modifier Optional modifier
 *
 * @author AVA AI Team
 * @version 1.0.0
 */
@Composable
fun DynamicColorToggle(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    isAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (isAvailable) {
                    "Material You dynamic colors, ${if (enabled) "enabled" else "disabled"}"
                } else {
                    "Material You dynamic colors not available on this device"
                }
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Material You",
                style = MaterialTheme.typography.titleMedium,
                color = if (isAvailable) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            Text(
                text = if (isAvailable) {
                    "Use colors from your wallpaper"
                } else {
                    "Requires Android 12 or higher"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isAvailable) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            enabled = isAvailable,
            modifier = Modifier.semantics {
                contentDescription = if (isAvailable) {
                    "Toggle Material You dynamic colors"
                } else {
                    "Material You not available"
                }
            }
        )
    }
}

/**
 * Theme Preview Card
 *
 * Shows a preview of the current theme configuration
 *
 * @param themeMode Current theme mode
 * @param accentColor Current accent color
 * @param isDarkMode Whether dark mode is active
 * @param modifier Optional modifier
 */
@Composable
fun ThemePreviewCard(
    themeMode: ThemeMode,
    accentColor: AccentColor,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Theme preview" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sample UI elements showing theme
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor.getColor(isDarkMode)
                )
            ) {
                Text(text = "Primary Button")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Primary text color",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Secondary text color",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
