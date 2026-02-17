/**
 * AvanueFAB.kt - Unified floating action button component
 *
 * ONE component, theme decides glass/water/cupertino/mountainview.
 * Reads AvanueTheme.materialMode to delegate to the correct implementation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.glass.GlassFloatingActionButton
import com.augmentalis.avanueui.components.glass.GlassShapes
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.MaterialMode
import com.augmentalis.avanueui.water.WaterDefaults
import com.augmentalis.avanueui.water.waterEffect

/**
 * Unified floating action button for the Avanues ecosystem.
 *
 * Renders as glass, water, cupertino, or mountainview based on [AvanueTheme.materialMode].
 */
@Suppress("DEPRECATION")
@Composable
fun AvanueFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapes.fabShape,
    content: @Composable () -> Unit
) {
    when (AvanueTheme.materialMode) {
        MaterialMode.Glass -> GlassFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            content = content
        )
        MaterialMode.Water -> {
            val fabModifier = modifier.waterEffect(
                backgroundColor = AvanueTheme.colors.primary,
                shape = shape,
                border = WaterDefaults.border,
                interactive = true
            )
            FloatingActionButton(
                onClick = onClick,
                modifier = fabModifier,
                shape = shape,
                containerColor = Color.Transparent,
                contentColor = AvanueTheme.colors.textOnPrimary,
                content = content
            )
        }
        MaterialMode.Cupertino -> {
            FloatingActionButton(
                onClick = onClick,
                modifier = modifier,
                shape = RoundedCornerShape(16.dp),
                containerColor = AvanueTheme.colors.primary,
                contentColor = AvanueTheme.colors.textOnPrimary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                content = content
            )
        }
        MaterialMode.MountainView -> {
            FloatingActionButton(
                onClick = onClick,
                modifier = modifier,
                shape = shape,
                containerColor = AvanueTheme.colors.primary,
                contentColor = AvanueTheme.colors.textOnPrimary,
                content = content
            )
        }
    }
}
