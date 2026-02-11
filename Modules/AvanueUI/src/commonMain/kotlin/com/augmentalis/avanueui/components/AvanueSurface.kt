/**
 * AvanueSurface.kt - Unified surface component
 *
 * ONE component, theme decides glass/water/cupertino/mountainview.
 * Reads AvanueTheme.materialMode to delegate to the correct implementation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.glass.GlassDefaults
import com.augmentalis.avanueui.components.glass.GlassSurface
import com.augmentalis.avanueui.components.water.WaterSurface
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.MaterialMode

/**
 * Unified surface component for the Avanues ecosystem.
 *
 * Renders as glass, water, cupertino, or mountainview based on [AvanueTheme.materialMode].
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
        MaterialMode.Glass -> GlassSurface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            content = content
        )
        MaterialMode.Water -> WaterSurface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            content = content
        )
        MaterialMode.Cupertino -> {
            val cupertinoShape = RoundedCornerShape(12.dp)
            if (onClick != null) {
                Surface(
                    onClick = onClick,
                    modifier = modifier,
                    shape = cupertinoShape,
                    color = color,
                    contentColor = contentColor,
                    border = BorderStroke(0.33.dp, AvanueTheme.colors.borderSubtle),
                    content = content
                )
            } else {
                Surface(
                    modifier = modifier,
                    shape = cupertinoShape,
                    color = color,
                    contentColor = contentColor,
                    border = BorderStroke(0.33.dp, AvanueTheme.colors.borderSubtle),
                    content = content
                )
            }
        }
        MaterialMode.MountainView -> {
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
