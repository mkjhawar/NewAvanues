/**
 * AvanueIconButton.kt - Unified icon button component
 *
 * ONE component, theme decides glass/water/cupertino/mountainview.
 * Reads AvanueTheme.materialMode to delegate to the correct implementation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.glass.GlassIconButton
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.MaterialMode
import com.augmentalis.avanueui.water.WaterDefaults
import com.augmentalis.avanueui.water.waterEffect

/**
 * Unified icon button for the Avanues ecosystem.
 *
 * Renders as glass, water, cupertino, or mountainview based on [AvanueTheme.materialMode].
 */
@Suppress("DEPRECATION")
@Composable
fun AvanueIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    when (AvanueTheme.materialMode) {
        MaterialMode.Glass -> GlassIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            glass = true,
            content = content
        )
        MaterialMode.Water -> {
            val iconShape = RoundedCornerShape(8.dp)
            val iconModifier = modifier.waterEffect(
                backgroundColor = AvanueTheme.colors.surfaceElevated,
                shape = iconShape,
                border = WaterDefaults.border,
                interactive = true
            )
            IconButton(
                onClick = onClick,
                modifier = iconModifier,
                enabled = enabled,
                content = content
            )
        }
        MaterialMode.Cupertino -> {
            IconButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = AvanueTheme.colors.iconPrimary
                ),
                content = content
            )
        }
        MaterialMode.MountainView -> {
            IconButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = IconButtonDefaults.iconButtonColors(),
                content = content
            )
        }
    }
}
