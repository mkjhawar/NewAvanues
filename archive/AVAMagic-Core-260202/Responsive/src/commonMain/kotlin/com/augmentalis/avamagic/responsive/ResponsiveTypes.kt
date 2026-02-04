/**
 * ResponsiveTypes.kt
 * Common responsive types and enums
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-23
 * Version: 1.0.0
 */

package com.augmentalis.avamagic.responsive

/**
 * Material Design 3 responsive breakpoints
 */
enum class Breakpoint {
    XS,  // <600dp width (phones in portrait)
    SM,  // 600-839dp width (tablets in portrait, phones in landscape)
    MD,  // 840-1239dp width (tablets in landscape, small desktops)
    LG,  // 1240-1439dp width (desktops)
    XL   // ≥1440dp width (large desktops)
}

/**
 * Device type classification
 */
enum class DeviceType {
    PHONE,
    TABLET,
    FOLDABLE,
    WEARABLE,
    TV,
    DESKTOP,
    XR,
    UNKNOWN
}

/**
 * Device metrics information
 */
data class DeviceMetrics(
    val widthPixels: Int,
    val heightPixels: Int,
    val widthDp: Float,
    val heightDp: Float,
    val density: Float,
    val densityDpi: Int,
    val diagonalInches: Float
)

/**
 * Foldable device state
 */
data class FoldState(
    val isFoldable: Boolean,
    val hingeAngle: Float,
    val isOpen: Boolean,
    val shouldAvoidCrease: Boolean
)
