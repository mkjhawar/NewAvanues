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

package com.augmentalis.avanueui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import com.augmentalis.avanueui.theme.AvanueTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        color = AvanueTheme.colors.primary,
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
                    tint = AvanueTheme.colors.textSecondary
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
                    tint = AvanueTheme.colors.textSecondary,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AvanueTheme.colors.textSecondary
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
                            color = AvanueTheme.colors.primary,
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
                    tint = AvanueTheme.colors.textSecondary
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
                    tint = AvanueTheme.colors.textSecondary,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AvanueTheme.colors.textSecondary
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
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
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
 * A settings row with an editable text field.
 *
 * Used for string or numeric settings that the user types in (e.g., hostnames,
 * ports, paths). Renders title/subtitle header with an OutlinedTextField below.
 *
 * @param title Primary text
 * @param subtitle Secondary description text (optional)
 * @param icon Leading icon (optional)
 * @param value Current text value
 * @param placeholder Placeholder text when empty (optional)
 * @param onValueChange Callback when text changes
 * @param singleLine Whether the field is single-line (default true)
 */
@Composable
fun SettingsTextFieldRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    value: String,
    placeholder: String = "",
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
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
                    tint = AvanueTheme.colors.textSecondary,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AvanueTheme.colors.textSecondary
                    )
                }
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, color = AvanueTheme.colors.textDisabled) }
            } else null,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (icon != null) 40.dp else 0.dp, top = 4.dp)
        )
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
            containerColor = AvanueTheme.colors.surface
        )
    ) {
        Column(content = content)
    }
}

/**
 * Curated accent color presets matching the SpatialVoice aesthetic.
 */
val defaultAccentPresets: List<Color> = listOf(
    Color(0xFF007AFF), // System Blue (default)
    Color(0xFF5856D6), // Indigo
    Color(0xFFAF52DE), // Purple
    Color(0xFFFF2D55), // Pink
    Color(0xFFFF3B30), // Red
    Color(0xFFFF9500), // Orange
    Color(0xFF34C759), // Green
    Color(0xFF00C7BE), // Teal
    Color(0xFF5AC8FA), // Light Blue
    Color(0xFFFFCC00), // Yellow
)

/**
 * A settings row showing a color swatch that opens a preset color picker dialog.
 *
 * Used for module accent color customization. Shows the current color as a
 * filled circle; tapping opens a dialog with curated preset colors.
 *
 * @param title Primary text
 * @param subtitle Secondary description text (optional)
 * @param icon Leading icon (optional)
 * @param color Current selected color
 * @param useThemeColor Whether "Use Theme" is selected (no custom override)
 * @param onColorSelected Called when a preset color is picked (isCustom = true)
 * @param onUseTheme Called when user selects "Use Theme Color"
 * @param presetColors Available preset colors to choose from
 */
@Composable
fun SettingsColorRow(
    title: String,
    subtitle: String = "",
    icon: ImageVector? = null,
    color: Color,
    useThemeColor: Boolean = false,
    onColorSelected: (Color) -> Unit,
    onUseTheme: () -> Unit,
    presetColors: List<Color> = defaultAccentPresets,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle.isNotEmpty()) {
            {
                Text(
                    text = if (useThemeColor) "Using theme color" else subtitle,
                    color = AvanueTheme.colors.textSecondary
                )
            }
        } else null,
        leadingContent = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AvanueTheme.colors.textSecondary
                )
            }
        } else null,
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, AvanueTheme.colors.border, CircleShape)
                    .clickable { showDialog = true }
            )
        },
        modifier = modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Choose Accent Color") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // "Use Theme" option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onUseTheme()
                                showDialog = false
                            }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AvanueTheme.colors.primary)
                                .then(
                                    if (useThemeColor) Modifier.border(3.dp, AvanueTheme.colors.textPrimary, CircleShape)
                                    else Modifier
                                )
                        )
                        Text(
                            text = "Use Theme Color",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }

                    // Preset colors grid (5 per row)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        presetColors.chunked(5).forEach { rowColors ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rowColors.forEach { preset ->
                                    val isSelected = !useThemeColor && color == preset
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(preset)
                                            .then(
                                                if (isSelected) Modifier.border(3.dp, AvanueTheme.colors.textPrimary, CircleShape)
                                                else Modifier.border(1.dp, AvanueTheme.colors.borderSubtle, CircleShape)
                                            )
                                            .clickable {
                                                onColorSelected(preset)
                                                showDialog = false
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
