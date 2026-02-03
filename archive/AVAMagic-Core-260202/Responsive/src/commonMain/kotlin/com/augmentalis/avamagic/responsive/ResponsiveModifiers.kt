/**
 * ResponsiveModifiers.kt
 * Device-aware modifiers for MagicUI components
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-23
 * Version: 1.0.0
 */

package com.augmentalis.avamagic.responsive

import com.augmentalis.avanues.avamagic.components.core.Size
import kotlinx.serialization.Serializable

/**
 * Base class for responsive modifiers
 * These modifiers adapt based on device characteristics
 */
@Serializable
sealed class ResponsiveModifier {

    /**
     * Width based on breakpoint
     * Cascading fallback: xl -> lg -> md -> sm -> xs
     */
    @Serializable
    data class WidthByBreakpoint(
        val xs: Size? = null,
        val sm: Size? = null,
        val md: Size? = null,
        val lg: Size? = null,
        val xl: Size? = null
    ) : ResponsiveModifier() {
        fun resolveFor(breakpoint: Breakpoint): Size {
            return when (breakpoint) {
                Breakpoint.XS -> xs ?: Size.Fill
                Breakpoint.SM -> sm ?: xs ?: Size.Fill
                Breakpoint.MD -> md ?: sm ?: xs ?: Size.Fill
                Breakpoint.LG -> lg ?: md ?: sm ?: xs ?: Size.Fill
                Breakpoint.XL -> xl ?: lg ?: md ?: sm ?: xs ?: Size.Fill
            }
        }
    }

    /**
     * Height based on breakpoint
     */
    @Serializable
    data class HeightByBreakpoint(
        val xs: Size? = null,
        val sm: Size? = null,
        val md: Size? = null,
        val lg: Size? = null,
        val xl: Size? = null
    ) : ResponsiveModifier() {
        fun resolveFor(breakpoint: Breakpoint): Size {
            return when (breakpoint) {
                Breakpoint.XS -> xs ?: Size.Auto
                Breakpoint.SM -> sm ?: xs ?: Size.Auto
                Breakpoint.MD -> md ?: sm ?: xs ?: Size.Auto
                Breakpoint.LG -> lg ?: md ?: sm ?: xs ?: Size.Auto
                Breakpoint.XL -> xl ?: lg ?: md ?: sm ?: xs ?: Size.Auto
            }
        }
    }

    /**
     * Width based on device type
     */
    @Serializable
    data class WidthByDeviceType(
        val phone: Size? = null,
        val tablet: Size? = null,
        val desktop: Size? = null,
        val foldable: Size? = null,
        val wearable: Size? = null,
        val tv: Size? = null,
        val xr: Size? = null,
        val default: Size = Size.Fill
    ) : ResponsiveModifier() {
        fun resolveFor(deviceType: DeviceType): Size {
            return when (deviceType) {
                DeviceType.PHONE -> phone ?: default
                DeviceType.TABLET -> tablet ?: default
                DeviceType.DESKTOP -> desktop ?: default
                DeviceType.FOLDABLE -> foldable ?: default
                DeviceType.WEARABLE -> wearable ?: default
                DeviceType.TV -> tv ?: default
                DeviceType.XR -> xr ?: default
                DeviceType.UNKNOWN -> default
            }
        }
    }

    /**
     * Size based on foldable state
     * Only applies to foldable devices
     */
    @Serializable
    data class FoldableAware(
        val closed: Size,
        val open: Size,
        val avoidCrease: Boolean = true
    ) : ResponsiveModifier()

    /**
     * Size based on orientation
     */
    @Serializable
    data class OrientationAware(
        val portrait: Size,
        val landscape: Size
    ) : ResponsiveModifier()

    /**
     * Padding based on breakpoint (in DP)
     */
    @Serializable
    data class PaddingByBreakpoint(
        val xs: Float? = null,
        val sm: Float? = null,
        val md: Float? = null,
        val lg: Float? = null,
        val xl: Float? = null
    ) : ResponsiveModifier() {
        fun resolveFor(breakpoint: Breakpoint): Float {
            return when (breakpoint) {
                Breakpoint.XS -> xs ?: 8f
                Breakpoint.SM -> sm ?: xs ?: 12f
                Breakpoint.MD -> md ?: sm ?: xs ?: 16f
                Breakpoint.LG -> lg ?: md ?: sm ?: xs ?: 24f
                Breakpoint.XL -> xl ?: lg ?: md ?: sm ?: xs ?: 32f
            }
        }
    }
}
