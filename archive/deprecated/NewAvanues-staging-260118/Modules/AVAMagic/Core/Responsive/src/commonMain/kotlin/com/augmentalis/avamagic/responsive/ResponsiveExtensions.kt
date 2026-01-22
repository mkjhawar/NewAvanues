/**
 * ResponsiveExtensions.kt
 * Helper functions and composables for responsive design
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-23
 * Version: 1.0.0
 */

package com.augmentalis.avamagic.responsive

import androidx.compose.runtime.Composable
import com.augmentalis.avamagic.deviceinfo.*

/**
 * Select a value based on current breakpoint
 * Cascading fallback: xl -> lg -> md -> sm -> xs -> default
 */
@Composable
fun <T> responsive(
    xs: T? = null,
    sm: T? = null,
    md: T? = null,
    lg: T? = null,
    xl: T? = null,
    default: T
): T {
    val context = LocalResponsiveContext.current ?: return default
    return when (context.breakpoint) {
        Breakpoint.XS -> xs ?: default
        Breakpoint.SM -> sm ?: xs ?: default
        Breakpoint.MD -> md ?: sm ?: xs ?: default
        Breakpoint.LG -> lg ?: md ?: sm ?: xs ?: default
        Breakpoint.XL -> xl ?: lg ?: md ?: sm ?: xs ?: default
    }
}

/**
 * Check if current device is a phone
 */
@Composable
fun isPhone(): Boolean {
    return LocalResponsiveContext.current?.isPhone ?: false
}

/**
 * Check if current device is a tablet
 */
@Composable
fun isTablet(): Boolean {
    return LocalResponsiveContext.current?.isTablet ?: false
}

/**
 * Check if current device is foldable
 */
@Composable
fun isFoldable(): Boolean {
    return LocalResponsiveContext.current?.isFoldable ?: false
}

/**
 * Check if current device is wearable
 */
@Composable
fun isWearable(): Boolean {
    return LocalResponsiveContext.current?.isWearable ?: false
}

/**
 * Check if current device is TV
 */
@Composable
fun isTV(): Boolean {
    return LocalResponsiveContext.current?.isTV ?: false
}

/**
 * Check if current device is desktop
 */
@Composable
fun isDesktop(): Boolean {
    return LocalResponsiveContext.current?.isDesktop ?: false
}

/**
 * Check if current device is XR
 */
@Composable
fun isXR(): Boolean {
    return LocalResponsiveContext.current?.isXR ?: false
}

/**
 * Check if display is in portrait orientation
 */
@Composable
fun isPortrait(): Boolean {
    return LocalResponsiveContext.current?.isPortrait ?: true
}

/**
 * Check if display is in landscape orientation
 */
@Composable
fun isLandscape(): Boolean {
    return LocalResponsiveContext.current?.isLandscape ?: false
}

/**
 * Check if current breakpoint is compact (XS)
 */
@Composable
fun isCompact(): Boolean {
    return LocalResponsiveContext.current?.isCompact ?: true
}

/**
 * Check if current breakpoint is medium (SM/MD)
 */
@Composable
fun isMedium(): Boolean {
    return LocalResponsiveContext.current?.isMedium ?: false
}

/**
 * Check if current breakpoint is expanded (LG/XL)
 */
@Composable
fun isExpanded(): Boolean {
    return LocalResponsiveContext.current?.isExpanded ?: false
}

/**
 * Get current breakpoint
 */
@Composable
fun currentBreakpoint(): Breakpoint {
    return LocalResponsiveContext.current?.breakpoint ?: Breakpoint.XS
}

/**
 * Get current device type
 */
@Composable
fun currentDeviceType(): DeviceType {
    return LocalResponsiveContext.current?.deviceType ?: DeviceType.UNKNOWN
}

/**
 * Get current device metrics
 */
@Composable
fun currentDeviceMetrics(): DeviceMetrics? {
    return LocalResponsiveContext.current?.metrics
}

/**
 * Get current fold state (foldable devices only)
 */
@Composable
fun currentFoldState(): FoldState? {
    return LocalResponsiveContext.current?.foldState
}

/**
 * Check if crease should be avoided (foldable devices)
 */
@Composable
fun shouldAvoidCrease(): Boolean {
    return LocalResponsiveContext.current?.foldState?.shouldAvoidCrease ?: false
}

/**
 * Conditional composable based on device type
 */
@Composable
fun OnPhone(content: @Composable () -> Unit) {
    if (isPhone()) {
        content()
    }
}

@Composable
fun OnTablet(content: @Composable () -> Unit) {
    if (isTablet()) {
        content()
    }
}

@Composable
fun OnDesktop(content: @Composable () -> Unit) {
    if (isDesktop()) {
        content()
    }
}

@Composable
fun OnFoldable(content: @Composable () -> Unit) {
    if (isFoldable()) {
        content()
    }
}

/**
 * Conditional composable based on breakpoint
 */
@Composable
fun OnCompact(content: @Composable () -> Unit) {
    if (isCompact()) {
        content()
    }
}

@Composable
fun OnMedium(content: @Composable () -> Unit) {
    if (isMedium()) {
        content()
    }
}

@Composable
fun OnExpanded(content: @Composable () -> Unit) {
    if (isExpanded()) {
        content()
    }
}

/**
 * Conditional composable based on orientation
 */
@Composable
fun OnPortrait(content: @Composable () -> Unit) {
    if (isPortrait()) {
        content()
    }
}

@Composable
fun OnLandscape(content: @Composable () -> Unit) {
    if (isLandscape()) {
        content()
    }
}
