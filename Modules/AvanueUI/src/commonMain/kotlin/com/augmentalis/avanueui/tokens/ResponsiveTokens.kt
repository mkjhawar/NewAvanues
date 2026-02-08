package com.augmentalis.avanueui.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * M3 responsive breakpoints, grid configuration, and glass display thresholds.
 * Static and universal across all themes.
 */
object ResponsiveTokens {

    // === Standard M3 Breakpoints ===

    val compactMax: Dp = 599.dp
    val mediumMin: Dp = 600.dp
    val mediumMax: Dp = 839.dp
    val expandedMin: Dp = 840.dp
    val expandedMax: Dp = 1239.dp
    val largeMin: Dp = 1240.dp
    val largeMax: Dp = 1439.dp
    val extraLargeMin: Dp = 1440.dp

    // === Smart Glass Breakpoints ===

    /** Vuzix Blade, smallest monocular displays (480x480). */
    val glassMicroMax: Dp = 479.dp

    /** Vuzix Shield/M400, RealWear HMT (640x360 to 854x480). */
    val glassCompactMin: Dp = 480.dp
    val glassCompactMax: Dp = 853.dp

    /** RealWear Nav520, Vuzix M4000 (854x480 to 1279x720). */
    val glassStandardMin: Dp = 854.dp
    val glassStandardMax: Dp = 1279.dp

    /** XREAL Air, Vuzix Z100 (1280x720+). */
    val glassHdMin: Dp = 1280.dp

    object GridColumns {
        const val compact: Int = 4
        const val medium: Int = 8
        const val expanded: Int = 12
        const val large: Int = 12
    }

    object Margins {
        val compact: Dp = 16.dp
        val medium: Dp = 24.dp
        val expanded: Dp = 24.dp
        val large: Dp = 32.dp
        /** Tighter margins for glass displays. */
        val glass: Dp = 8.dp
    }

    object Gutters {
        val compact: Dp = 16.dp
        val medium: Dp = 24.dp
        val expanded: Dp = 24.dp
        val large: Dp = 24.dp
        /** Tighter gutters for glass displays. */
        val glass: Dp = 8.dp
    }

    object MaxContentWidth {
        val compact: Dp = 360.dp
        val medium: Dp = 600.dp
        val expanded: Dp = 840.dp
        val large: Dp = 1240.dp
        val extraLarge: Dp = 1440.dp
    }
}
