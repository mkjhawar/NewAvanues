/**
 * AvanueCard.kt - Unified card component
 *
 * ONE component, theme decides glass/water/cupertino/mountainview.
 * Reads AvanueTheme.materialMode to delegate to the correct implementation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.glass.GlassCard
import com.augmentalis.avanueui.components.glass.GlassDefaults
import com.augmentalis.avanueui.components.water.WaterCard
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.MaterialMode

/**
 * Unified card component for the Avanues ecosystem.
 *
 * Renders as glass, water, cupertino, or mountainview based on [AvanueTheme.materialMode].
 */
@Suppress("DEPRECATION")
@Composable
fun AvanueCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: Shape = GlassDefaults.shape,
    content: @Composable ColumnScope.() -> Unit
) {
    when (AvanueTheme.materialMode) {
        MaterialMode.Glass -> GlassCard(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            content = content
        )
        MaterialMode.Water -> WaterCard(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            content = content
        )
        MaterialMode.Cupertino -> {
            val cupertinoShape = RoundedCornerShape(12.dp)
            if (onClick != null) {
                Card(
                    onClick = onClick,
                    modifier = modifier,
                    shape = cupertinoShape,
                    colors = CardDefaults.cardColors(
                        containerColor = AvanueTheme.colors.surface,
                        contentColor = AvanueTheme.colors.textPrimary
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(0.33.dp, AvanueTheme.colors.borderSubtle),
                    content = content
                )
            } else {
                Card(
                    modifier = modifier,
                    shape = cupertinoShape,
                    colors = CardDefaults.cardColors(
                        containerColor = AvanueTheme.colors.surface,
                        contentColor = AvanueTheme.colors.textPrimary
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(0.33.dp, AvanueTheme.colors.borderSubtle),
                    content = content
                )
            }
        }
        MaterialMode.MountainView -> {
            if (onClick != null) {
                Card(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    colors = CardDefaults.cardColors(
                        containerColor = AvanueTheme.colors.surface,
                        contentColor = AvanueTheme.colors.textPrimary
                    ),
                    elevation = CardDefaults.cardElevation(1.dp),
                    content = content
                )
            } else {
                Card(
                    modifier = modifier,
                    shape = shape,
                    colors = CardDefaults.cardColors(
                        containerColor = AvanueTheme.colors.surface,
                        contentColor = AvanueTheme.colors.textPrimary
                    ),
                    elevation = CardDefaults.cardElevation(1.dp),
                    content = content
                )
            }
        }
    }
}
