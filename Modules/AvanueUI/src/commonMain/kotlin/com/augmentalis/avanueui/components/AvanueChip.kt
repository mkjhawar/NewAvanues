/**
 * AvanueChip.kt - Unified chip component
 *
 * ONE component, theme decides glass/water/plain.
 * Reads AvanueTheme.materialMode to delegate to the correct implementation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.augmentalis.avanueui.components.glass.GlassChip
import com.augmentalis.avanueui.components.glass.GlassShapes
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.MaterialMode
import com.augmentalis.avanueui.water.WaterDefaults
import com.augmentalis.avanueui.water.waterEffect

/**
 * Unified chip component for the Avanues ecosystem.
 *
 * Renders as glass, water, or plain Material3 based on [AvanueTheme.materialMode].
 *
 * @param onClick Click handler
 * @param label Chip label
 * @param modifier Modifier for customization
 * @param enabled Enable/disable chip
 * @param leadingIcon Optional leading icon
 * @param trailingIcon Optional trailing icon
 * @param shape Chip shape
 */
@Suppress("DEPRECATION")
@Composable
fun AvanueChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = GlassShapes.chipShape
) {
    when (AvanueTheme.materialMode) {
        MaterialMode.GLASS -> GlassChip(
            onClick = onClick,
            label = label,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            glass = true,
            shape = shape
        )
        MaterialMode.WATER -> {
            val chipModifier = modifier.waterEffect(
                backgroundColor = AvanueTheme.colors.surfaceElevated,
                shape = shape,
                border = WaterDefaults.border,
                interactive = true
            )
            AssistChip(
                onClick = onClick,
                label = label,
                modifier = chipModifier,
                enabled = enabled,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    labelColor = AvanueTheme.colors.textPrimary
                ),
                shape = shape,
                border = null
            )
        }
        MaterialMode.PLAIN -> {
            AssistChip(
                onClick = onClick,
                label = label,
                modifier = modifier,
                enabled = enabled,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = AvanueTheme.colors.surfaceElevated,
                    labelColor = AvanueTheme.colors.textPrimary
                ),
                shape = shape
            )
        }
    }
}
