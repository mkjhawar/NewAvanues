/**
 * WaterSurface.kt - Base water-effect surface component
 *
 * The foundational composable for water UI. Applies [Modifier.waterEffect()]
 * with transparent container color (effect via modifier, same pattern as GlassSurface).
 *
 * Usage:
 * ```
 * WaterSurface(shape = WaterShapes.large) {
 *     Text("Hello from water")
 * }
 * ```
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components.water

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.water.WaterBorder
import com.augmentalis.avanueui.water.WaterDefaults
import com.augmentalis.avanueui.water.WaterLevel
import com.augmentalis.avanueui.water.waterEffect

/**
 * Base water-effect surface. Wraps Material3 Surface with water rendering.
 *
 * @param onClick Optional click handler (makes surface interactive with press scale)
 * @param modifier Modifier for customization
 * @param shape Surface shape
 * @param color Base surface color (made translucent by water effect)
 * @param contentColor Content color (text, icons)
 * @param waterLevel Effect intensity
 * @param border Optional gradient border
 * @param content Composable content
 */
@Deprecated(
    message = "Use AvanueSurface instead. Theme controls glass/water/plain rendering.",
    replaceWith = ReplaceWith(
        "AvanueSurface(onClick = onClick, modifier = modifier, shape = shape, content = content)",
        "com.augmentalis.avanueui.components.AvanueSurface"
    )
)
@Composable
fun WaterSurface(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: Shape = WaterDefaults.shape,
    color: Color = AvanueTheme.colors.surface,
    contentColor: Color = AvanueTheme.colors.textPrimary,
    waterLevel: WaterLevel = WaterLevel.REGULAR,
    border: WaterBorder? = WaterDefaults.border,
    content: @Composable () -> Unit
) {
    val surfaceModifier = modifier.waterEffect(
        backgroundColor = color,
        waterLevel = waterLevel,
        shape = shape,
        border = border,
        interactive = onClick != null
    )

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = surfaceModifier,
            shape = shape,
            color = Color.Transparent,
            contentColor = contentColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            content = content
        )
    } else {
        Surface(
            modifier = surfaceModifier,
            shape = shape,
            color = Color.Transparent,
            contentColor = contentColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            content = content
        )
    }
}
