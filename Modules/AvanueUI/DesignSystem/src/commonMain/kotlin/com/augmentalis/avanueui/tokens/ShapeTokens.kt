package com.augmentalis.avanueui.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Corner radius scale. Static and universal across all themes.
 */
object ShapeTokens {
    val none: Dp = 0.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val full: Dp = 9999.dp

    fun resolve(id: String): Dp? = when (id) {
        "shape.none" -> none
        "shape.xs" -> xs
        "shape.sm" -> sm
        "shape.md" -> md
        "shape.lg" -> lg
        "shape.xl" -> xl
        "shape.xxl" -> xxl
        "shape.full" -> full
        else -> null
    }
}
