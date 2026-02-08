package com.augmentalis.avanueui.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation/shadow levels. Static and universal across all themes.
 */
object ElevationTokens {
    val none: Dp = 0.dp
    val xs: Dp = 1.dp
    val sm: Dp = 2.dp
    val md: Dp = 4.dp
    val lg: Dp = 8.dp
    val xl: Dp = 12.dp
    val xxl: Dp = 16.dp

    fun resolve(id: String): Dp? = when (id) {
        "elevation.none" -> none
        "elevation.xs" -> xs
        "elevation.sm" -> sm
        "elevation.md" -> md
        "elevation.lg" -> lg
        "elevation.xl" -> xl
        "elevation.xxl" -> xxl
        else -> null
    }
}
