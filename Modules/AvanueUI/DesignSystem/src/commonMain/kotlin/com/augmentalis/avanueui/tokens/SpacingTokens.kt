package com.augmentalis.avanueui.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Unified spacing scale (8dp grid system).
 *
 * md = 16dp (industry standard M3/HIG/Fluent).
 * All spacing is static and universal across themes.
 */
object SpacingTokens {
    val none: Dp = 0.dp
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
    val huge: Dp = 64.dp
    val minTouchTarget: Dp = 48.dp
    val minTouchTargetSpatial: Dp = 60.dp

    fun resolve(id: String): Dp? = when (id) {
        "spacing.none" -> none
        "spacing.xxs" -> xxs
        "spacing.xs" -> xs
        "spacing.sm" -> sm
        "spacing.md" -> md
        "spacing.lg" -> lg
        "spacing.xl" -> xl
        "spacing.xxl" -> xxl
        "spacing.huge" -> huge
        "spacing.minTouchTarget" -> minTouchTarget
        "spacing.minTouchTargetSpatial" -> minTouchTargetSpatial
        else -> null
    }
}
