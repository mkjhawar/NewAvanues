/**
 * AvanueBubble.kt - Unified chat bubble component
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
import com.augmentalis.avanueui.components.glass.BubbleAlign
import com.augmentalis.avanueui.components.glass.GlassBubble
import com.augmentalis.avanueui.components.glass.GlassDefaults
import com.augmentalis.avanueui.components.glass.GlassShapes
import com.augmentalis.avanueui.components.water.WaterSurface
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.MaterialMode

/**
 * Unified chat bubble component for the Avanues ecosystem.
 *
 * Renders as glass, water, cupertino, or mountainview based on [AvanueTheme.materialMode].
 */
@Suppress("DEPRECATION")
@Composable
fun AvanueBubble(
    modifier: Modifier = Modifier,
    align: BubbleAlign = BubbleAlign.START,
    shape: Shape = when (align) {
        BubbleAlign.START -> GlassShapes.bubbleStart
        BubbleAlign.END -> GlassShapes.bubbleEnd
        BubbleAlign.CENTER -> GlassDefaults.shape
    },
    color: Color = AvanueTheme.colors.surface,
    contentColor: Color = AvanueTheme.colors.textPrimary,
    content: @Composable () -> Unit
) {
    when (AvanueTheme.materialMode) {
        MaterialMode.Glass -> GlassBubble(
            modifier = modifier,
            align = align,
            shape = shape,
            color = color,
            contentColor = contentColor,
            content = content
        )
        MaterialMode.Water -> WaterSurface(
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            content = content
        )
        MaterialMode.Cupertino -> {
            val cupertinoShape = RoundedCornerShape(18.dp)
            Surface(
                modifier = modifier,
                shape = cupertinoShape,
                color = color,
                contentColor = contentColor,
                border = BorderStroke(0.33.dp, AvanueTheme.colors.borderSubtle),
                content = content
            )
        }
        MaterialMode.MountainView -> {
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
