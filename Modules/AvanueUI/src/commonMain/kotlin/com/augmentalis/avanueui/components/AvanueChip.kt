/**
 * AvanueChip.kt - Unified chip component
 *
 * ONE component, theme decides glass/water/cupertino/mountainview.
 * Reads AvanueTheme.materialMode to delegate to the correct implementation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.glass.GlassChip
import com.augmentalis.avanueui.components.glass.GlassShapes
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.MaterialMode
import com.augmentalis.avanueui.water.WaterDefaults
import com.augmentalis.avanueui.water.waterEffect

/**
 * Unified chip component for the Avanues ecosystem.
 *
 * Renders as glass, water, cupertino, or mountainview based on [AvanueTheme.materialMode].
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
        MaterialMode.Glass -> GlassChip(
            onClick = onClick,
            label = label,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            glass = true,
            shape = shape
        )
        MaterialMode.Water -> {
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
                    containerColor = Color.Transparent,
                    labelColor = AvanueTheme.colors.textPrimary
                ),
                shape = shape,
                border = null
            )
        }
        MaterialMode.Cupertino -> {
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
                shape = RoundedCornerShape(8.dp),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = enabled,
                    borderColor = AvanueTheme.colors.borderSubtle,
                    borderWidth = 0.33.dp
                )
            )
        }
        MaterialMode.MountainView -> {
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
