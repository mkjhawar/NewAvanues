/**
 * WaterCard.kt - Water-effect card component
 *
 * Zero Material3 elevation (like GlassCard). Depth from water shadow layer.
 * Uses WaterSurface internally with card-appropriate defaults.
 *
 * Usage:
 * ```
 * WaterCard(onClick = { /* ... */ }) {
 *     Column(Modifier.padding(16.dp)) {
 *         Text("Card Title")
 *         Text("Card body content")
 *     }
 * }
 * ```
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components.water

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.water.WaterBorder
import com.augmentalis.avanueui.water.WaterDefaults
import com.augmentalis.avanueui.water.WaterLevel
import com.augmentalis.avanueui.water.WaterShapes
import com.augmentalis.avanueui.water.waterEffect

/**
 * Water-effect card with zero elevation and liquid refraction depth.
 *
 * @param onClick Optional click handler
 * @param modifier Modifier for customization
 * @param shape Card shape
 * @param waterLevel Effect intensity
 * @param border Optional gradient border
 * @param content Card content (ColumnScope)
 */
@Deprecated(
    message = "Use AvanueCard instead. Theme controls glass/water/plain rendering.",
    replaceWith = ReplaceWith(
        "AvanueCard(onClick = onClick, modifier = modifier, shape = shape, content = content)",
        "com.augmentalis.avanueui.components.AvanueCard"
    )
)
@Composable
fun WaterCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: Shape = WaterShapes.default,
    waterLevel: WaterLevel = WaterLevel.REGULAR,
    border: WaterBorder? = WaterDefaults.border,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier.waterEffect(
        backgroundColor = AvanueTheme.colors.surface,
        waterLevel = waterLevel,
        shape = shape,
        border = border,
        interactive = onClick != null
    )

    val zeroElevation = CardDefaults.cardElevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
        focusedElevation = 0.dp,
        hoveredElevation = 0.dp,
        draggedElevation = 0.dp,
        disabledElevation = 0.dp
    )

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
                contentColor = AvanueTheme.colors.textPrimary
            ),
            elevation = zeroElevation,
            content = content
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
                contentColor = AvanueTheme.colors.textPrimary
            ),
            elevation = zeroElevation,
            content = content
        )
    }
}
