/**
 * ResponsiveContext.kt
 * Responsive context for device-aware UI components
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-23
 * Version: 1.0.0
 */

package com.augmentalis.avamagic.responsive

import androidx.compose.runtime.*

/**
 * Responsive context providing device information to composables
 */
data class ResponsiveContext(
    val breakpoint: Breakpoint,
    val deviceType: DeviceType,
    val metrics: DeviceMetrics,
    val foldState: FoldState?
) {
    val isPhone: Boolean get() = deviceType == DeviceType.PHONE
    val isTablet: Boolean get() = deviceType == DeviceType.TABLET
    val isFoldable: Boolean get() = foldState?.isFoldable == true
    val isWearable: Boolean get() = deviceType == DeviceType.WEARABLE
    val isTV: Boolean get() = deviceType == DeviceType.TV
    val isDesktop: Boolean get() = deviceType == DeviceType.DESKTOP
    val isXR: Boolean get() = deviceType == DeviceType.XR

    val isPortrait: Boolean get() = metrics.heightPixels > metrics.widthPixels
    val isLandscape: Boolean get() = metrics.widthPixels > metrics.heightPixels

    val isCompact: Boolean get() = breakpoint == Breakpoint.XS
    val isMedium: Boolean get() = breakpoint == Breakpoint.SM || breakpoint == Breakpoint.MD
    val isExpanded: Boolean get() = breakpoint == Breakpoint.LG || breakpoint == Breakpoint.XL
}

/**
 * CompositionLocal for ResponsiveContext
 */
val LocalResponsiveContext = staticCompositionLocalOf<ResponsiveContext?> { null }

/**
 * Remember a responsive context (platform-specific implementation)
 */
@Composable
expect fun rememberResponsiveContext(): ResponsiveContext

/**
 * Provide responsive context to composable tree
 */
@Composable
fun ResponsiveProvider(
    context: ResponsiveContext,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalResponsiveContext provides context,
        content = content
    )
}

/**
 * Provide responsive context using platform-default provider
 */
@Composable
fun ResponsiveProvider(
    content: @Composable () -> Unit
) {
    val context = rememberResponsiveContext()
    CompositionLocalProvider(
        LocalResponsiveContext provides context,
        content = content
    )
}

/**
 * Get current responsive context or throw if not provided
 */
@Composable
fun requireResponsiveContext(): ResponsiveContext {
    return LocalResponsiveContext.current
        ?: error("ResponsiveContext not provided. Wrap your composable in ResponsiveProvider.")
}

/**
 * Get current responsive context or null
 */
@Composable
fun currentResponsiveContext(): ResponsiveContext? {
    return LocalResponsiveContext.current
}
