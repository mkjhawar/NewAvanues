/**
 * AvanueSurface.kt - Unified surface component
 *
 * ONE component, theme decides glass/water/plain.
 * Reads AvanueTheme.materialMode to delegate to the correct implementation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.glass.GlassDefaults
import com.augmentalis.avanueui.components.glass.GlassSurface
import com.augmentalis.avanueui.components.water.WaterSurface
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.MaterialMode

/**
 * Unified surface component for the Avanues ecosystem.
 *
 * Renders as glass, water, or plain Material3 based on [AvanueTheme.materialMode].
 *
 * @param onClick Optional click handler (makes surface interactive)
 * @param modifier Modifier for customization
 * @param shape Surface shape
 * @param color Base surface color
 * @param contentColor Content color (text, icons)
 * @param content Composable content
 */
@Suppress("DEPRECATION")
@Composable
fun AvanueSurface(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: Shape = GlassDefaults.shape,
    color: Color = AvanueTheme.colors.surface,
    contentColor: Color = AvanueTheme.colors.textPrimary,
    content: @Composable () -> Unit
) {
    when (AvanueTheme.materialMode) {
        MaterialMode.GLASS -> GlassSurface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            content = content
        )
        MaterialMode.WATER -> WaterSurface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            content = content
        )
        MaterialMode.PLAIN -> {
            if (onClick != null) {
                Surface(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    color = color,
                    contentColor = contentColor,
                    content = content
                )
            } else {
                Surface(
                    modifier = modifier,
                    shape = shape,
                    color = color,
                    contentColor = contentColor,
                    content = content
                )
            }
        }
    }
}
