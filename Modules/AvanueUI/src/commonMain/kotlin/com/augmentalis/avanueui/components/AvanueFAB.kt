/**
 * AvanueFAB.kt - Unified floating action button component
 *
 * ONE component, theme decides glass/water/plain.
 * Reads AvanueTheme.materialMode to delegate to the correct implementation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.augmentalis.avanueui.components.glass.GlassFloatingActionButton
import com.augmentalis.avanueui.components.glass.GlassShapes
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.MaterialMode
import com.augmentalis.avanueui.water.WaterDefaults
import com.augmentalis.avanueui.water.waterEffect

/**
 * Unified floating action button for the Avanues ecosystem.
 *
 * Renders as glass, water, or plain Material3 based on [AvanueTheme.materialMode].
 *
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param shape FAB shape
 * @param content FAB content (icon)
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
        MaterialMode.GLASS -> GlassFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            content = content
        )
        MaterialMode.WATER -> {
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
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = AvanueTheme.colors.textOnPrimary,
                content = content
            )
        }
        MaterialMode.PLAIN -> {
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
