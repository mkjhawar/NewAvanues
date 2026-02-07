/**
 * SettingsComponents.kt - Shared settings UI components for all Avanues apps
 *
 * Consolidates duplicated settings components from:
 * - App SettingsScreen (SettingsItem, SettingsSwitch, SettingsSlider, SettingsSectionHeader)
 * - WebAvanue SettingComponents (SwitchSettingItem, SliderSettingItem, NavigationSettingItem)
 * - WebAvanue XRSettingsScreen (variants with icon support)
 *
 * All components accept optional icon parameter - app settings use icons,
 * WebAvanue mostly doesn't. Uses Material3 ListItem for proper spacing.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.avanueui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Section header for grouped settings - iOS-style uppercase label.
 *
 * Renders a small, primary-colored label above a group of related settings.
 */
@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

/**
 * A settings row with a toggle switch.
 *
 * Used for boolean on/off settings. Renders as a Material3 ListItem with
 * optional leading icon and trailing Switch.
 *
 * @param title Primary text
 * @param subtitle Secondary description text (optional)
 * @param icon Leading icon (optional - app settings use this, WebAvanue doesn't)
 * @param checked Current toggle state
 * @param enabled Whether the switch is interactive
 * @param onCheckedChange Callback when toggled
 */
@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle) }
        } else null,
        leadingContent = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        },
        modifier = modifier
    )
}

/**
 * A settings row with a slider for numeric range values.
 *
 * Renders title/subtitle header with optional icon, plus a Slider below.
 * The valueLabel (e.g. "1500 ms") appears in the subtitle or as a separate display.
 *
 * @param title Primary text
 * @param subtitle Secondary text showing current value (e.g. "1500 ms")
 * @param icon Leading icon (optional)
 * @param value Current slider value
 * @param valueRange Min..Max range for the slider
 * @param steps Number of discrete steps (0 = continuous)
 * @param valueLabel Optional label displayed next to the title showing current value
 * @param onValueChange Callback when slider moves
 */
@Composable
fun SettingsSliderRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueLabel: String? = null,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (valueLabel != null) {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (icon != null) 40.dp else 0.dp)
        )
    }
}

/**
 * A clickable settings row for navigation or action triggers.
 *
 * Used for items that open sub-screens, dialogs, or system settings.
 * Shows optional current value in primary color and a chevron indicator.
 *
 * @param title Primary text
 * @param subtitle Secondary description text (optional)
 * @param icon Leading icon (optional)
 * @param currentValue Current value displayed below subtitle in primary color (optional)
 * @param onClick Callback when tapped
 */
@Composable
fun SettingsNavigationRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    currentValue: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null || currentValue != null) {
            {
                Column {
                    if (subtitle != null) {
                        Text(subtitle)
                    }
                    if (currentValue != null) {
                        Text(
                            text = currentValue,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else null,
        leadingContent = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        modifier = modifier.clickable(onClick = onClick)
    )
}

/**
 * A dropdown selector for enum-based settings.
 *
 * Renders an ExposedDropdownMenuBox that lets users pick from a list of options.
 * Generic over the option type T with a label mapper for display text.
 *
 * @param T The type of option (typically an enum)
 * @param title Primary text
 * @param subtitle Secondary description (optional)
 * @param icon Leading icon (optional)
 * @param selected Currently selected option
 * @param options All available options to choose from
 * @param optionLabel Maps each option to its display string
 * @param onSelected Callback when a new option is chosen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingsDropdownRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (icon != null) 40.dp else 0.dp, top = 4.dp)
        ) {
            OutlinedTextField(
                value = optionLabel(selected),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * iOS-style grouped card container for settings.
 *
 * Groups related settings items inside a rounded card with consistent
 * surface coloring. Matches the iOS grouped table view aesthetic.
 *
 * @param content Settings rows to render inside the card
 */
@Composable
fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(content = content)
    }
}
