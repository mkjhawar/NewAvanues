package com.augmentalis.avanueui.display

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.augmentalis.avanueui.theme.AvanueTheme

/**
 * Display-aware utilities for adaptive UI.
 *
 * The density override in [AvanueTheme] scales everything down on glass displays.
 * Touch/gesture targets need a PHYSICAL minimum that counteracts this scaling.
 */
object DisplayUtils {

    /**
     * Minimum touch/gesture target for the current display profile.
     *
     * Returns a dp value that, after density scaling, results in at least
     * [DisplayProfile.minTouchTarget] physical dp on screen.
     *
     * Usage: `Modifier.size(DisplayUtils.minTouchTarget)`
     */
    val minTouchTarget: Dp
        @Composable get() {
            val profile = AvanueTheme.displayProfile
            val scale = profile.densityScale
            // Counteract density scaling to maintain physical minimum
            return profile.minTouchTarget / scale
        }

    /**
     * Whether the current display is a smart glass form factor.
     */
    val isGlass: Boolean
        @Composable get() = AvanueTheme.displayProfile.isGlass

    /**
     * Current layout strategy based on display profile.
     */
    val layoutStrategy: LayoutStrategy
        @Composable get() = AvanueTheme.displayProfile.layoutStrategy
}
