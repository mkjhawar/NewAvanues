package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.augmentalis.avaelements.flutter.material.chips.FilterChip
import com.augmentalis.avaelements.flutter.material.chips.ActionChip
import com.augmentalis.avaelements.flutter.material.chips.ChoiceChip
import com.augmentalis.avaelements.flutter.material.chips.InputChip
import com.augmentalis.avaelements.renderer.android.IconFromString

/**
 * Android Compose mappers for chip components
 *
 * This file contains renderer functions that map cross-platform chip component models
 * to Material3 Compose implementations on Android.
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Render FilterChip component using Material3
 *
 * Maps FilterChip component to Material3 FilterChip with:
 * - Selection state
 * - Checkmark indicator when selected
 * - Optional leading avatar
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component FilterChip component to render
 */
@Composable
fun FilterChipMapper(component: FilterChip) {
    androidx.compose.material3.FilterChip(
        selected = component.selected,
        onClick = {
            component.onSelected?.invoke(!component.selected)
        },
        label = {
            Text(component.label)
        },
        modifier = Modifier
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        enabled = component.enabled,
        leadingIcon = if (component.selected && component.showCheckmark) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else if (component.avatar != null && !component.selected) {
            {
                IconFromString(
                    iconName = component.avatar,
                    size = FilterChipDefaults.IconSize,
                    contentDescription = null
                )
            }
        } else {
            null
        }
    )
}

/**
 * Render ActionChip component using Material3
 *
 * Maps ActionChip component to Material3 AssistChip:
 * - Compact button-like appearance
 * - Optional leading icon/avatar
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component ActionChip component to render
 */
@Composable
fun ActionChipMapper(component: ActionChip) {
    AssistChip(
        onClick = {
            component.onPressed?.invoke()
        },
        label = {
            Text(component.label)
        },
        modifier = Modifier
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        enabled = component.enabled,
        leadingIcon = if (component.avatar != null) {
            {
                IconFromString(
                    iconName = component.avatar,
                    size = AssistChipDefaults.IconSize,
                    contentDescription = null
                )
            }
        } else {
            null
        }
    )
}

/**
 * Render ChoiceChip component using Material3
 *
 * Maps ChoiceChip component to Material3 FilterChip (single-selection mode):
 * - Single-selection within a group
 * - Visual feedback for selected state
 * - Optional leading icon/avatar
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component ChoiceChip component to render
 */
@Composable
fun ChoiceChipMapper(component: ChoiceChip) {
    androidx.compose.material3.FilterChip(
        selected = component.selected,
        onClick = {
            component.onSelected?.invoke(!component.selected)
        },
        label = {
            Text(component.label)
        },
        modifier = Modifier
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        enabled = component.enabled,
        leadingIcon = if (component.selected && component.showCheckmark) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else if (component.avatar != null && !component.selected) {
            {
                IconFromString(
                    iconName = component.avatar,
                    size = FilterChipDefaults.IconSize,
                    contentDescription = null
                )
            }
        } else {
            null
        }
    )
}

/**
 * Render InputChip component using Material3
 *
 * Maps InputChip component to Material3 InputChip:
 * - Optional leading avatar
 * - Delete/remove action button
 * - Selectable state
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component InputChip component to render
 */
@Composable
fun InputChipMapper(component: InputChip) {
    androidx.compose.material3.InputChip(
        selected = component.selected,
        onClick = {
            component.onPressed?.invoke()
            component.onSelected?.invoke(!component.selected)
        },
        label = {
            Text(component.label)
        },
        modifier = Modifier
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        enabled = component.enabled,
        leadingIcon = if (component.avatar != null) {
            {
                IconFromString(
                    iconName = component.avatar,
                    size = InputChipDefaults.AvatarSize,
                    contentDescription = null
                )
            }
        } else {
            null
        },
        trailingIcon = if (component.onDeleted != null) {
            {
                IconButton(
                    onClick = {
                        component.onDeleted.invoke()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = component.getDeleteButtonAccessibilityDescription()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(InputChipDefaults.IconSize)
                    )
                }
            }
        } else {
            null
        }
    )
}
