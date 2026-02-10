/**
 * AvanueIconButton.kt - Unified icon button component
 *
 * ONE component, theme decides glass/water/plain.
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
 * Renders as glass, water, or plain Material3 based on [AvanueTheme.materialMode].
 *
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param enabled Enable/disable button
 * @param content Button content (icon)
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
        MaterialMode.GLASS -> GlassIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            glass = true,
            content = content
        )
        MaterialMode.WATER -> {
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
        MaterialMode.PLAIN -> {
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
