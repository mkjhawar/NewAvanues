// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/ui/WindowSizeUtils.kt
// created: 2025-11-06
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Window size utilities for adaptive layouts
 *
 * Detects orientation and screen size to enable responsive UI
 */

enum class WindowSize {
    COMPACT,  // Phone portrait
    MEDIUM,   // Phone landscape, small tablet
    EXPANDED  // Large tablet, foldable
}

enum class Orientation {
    PORTRAIT,
    LANDSCAPE
}

data class WindowSizeClass(
    val widthSize: WindowSize,
    val heightSize: WindowSize,
    val orientation: Orientation
) {
    val isLandscape: Boolean get() = orientation == Orientation.LANDSCAPE
    val isPortrait: Boolean get() = orientation == Orientation.PORTRAIT
    val isExpandedWidth: Boolean get() = widthSize == WindowSize.EXPANDED
    val isMediumOrExpandedWidth: Boolean get() = widthSize in listOf(WindowSize.MEDIUM, WindowSize.EXPANDED)
}

/**
 * Calculate window size class based on current configuration
 */
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current

    return remember(configuration) {
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val orientation = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Orientation.LANDSCAPE
        } else {
            Orientation.PORTRAIT
        }

        val widthSize = when {
            screenWidth < 600 -> WindowSize.COMPACT
            screenWidth < 840 -> WindowSize.MEDIUM
            else -> WindowSize.EXPANDED
        }

        val heightSize = when {
            screenHeight < 480 -> WindowSize.COMPACT
            screenHeight < 900 -> WindowSize.MEDIUM
            else -> WindowSize.EXPANDED
        }

        WindowSizeClass(
            widthSize = widthSize,
            heightSize = heightSize,
            orientation = orientation
        )
    }
}
