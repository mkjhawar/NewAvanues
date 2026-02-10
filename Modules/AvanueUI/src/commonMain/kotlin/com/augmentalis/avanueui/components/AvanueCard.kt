/**
 * AvanueCard.kt - Unified card component
 *
 * ONE component, theme decides glass/water/plain.
 * Reads AvanueTheme.materialMode to delegate to the correct implementation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components

import androidx.compose.foundation.layout.ColumnScope
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
 * Renders as glass, water, or plain Material3 based on [AvanueTheme.materialMode].
 *
 * @param onClick Optional click handler
 * @param modifier Modifier for customization
 * @param shape Card shape
 * @param content Card content (ColumnScope)
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
        MaterialMode.GLASS -> GlassCard(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            content = content
        )
        MaterialMode.WATER -> WaterCard(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            content = content
        )
        MaterialMode.PLAIN -> {
            if (onClick != null) {
                Card(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    colors = CardDefaults.cardColors(
                        containerColor = AvanueTheme.colors.surface,
                        contentColor = AvanueTheme.colors.textPrimary
                    ),
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
                    content = content
                )
            }
        }
    }
}
