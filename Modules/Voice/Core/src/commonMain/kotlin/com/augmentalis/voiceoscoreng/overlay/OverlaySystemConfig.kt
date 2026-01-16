/**
 * OverlaySystemConfig.kt - Configuration data classes for VoiceOSCoreNG overlay system
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-01-06
 *
 * KMP-compatible overlay system configuration using immutable data classes.
 * Provides:
 * - OverlaySystemConfig: Main configuration for overlay display and behavior
 * - OverlayPosition: Enum for 9-point positioning grid
 * - AccessibilityConfig: Nested config for accessibility features
 *
 * Design: Immutable data classes for thread-safety and predictable state management.
 * Use .copy() to create modified configurations.
 */
package com.augmentalis.voiceoscoreng.overlay

/**
 * Defines the position of an overlay on the screen using a 9-point grid.
 *
 * Layout:
 * ```
 * TOP_LEFT     TOP_CENTER     TOP_RIGHT
 * CENTER_LEFT  CENTER         CENTER_RIGHT
 * BOTTOM_LEFT  BOTTOM_CENTER  BOTTOM_RIGHT
 * ```
 */
enum class OverlayPosition {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT
}

/**
 * Accessibility configuration for overlay system.
 *
 * Controls accessibility-related features for users who need:
 * - Larger text for better readability
 * - Higher contrast for visual clarity
 * - Reduced motion for vestibular sensitivity
 *
 * @property largeText Enable larger text sizes (default: false)
 * @property highContrast Enable high contrast mode (default: false)
 * @property reduceMotion Disable or minimize animations (default: false)
 */
data class AccessibilityConfig(
    val largeText: Boolean = false,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false
)

/**
 * Main configuration for the VoiceOSCoreNG overlay system.
 *
 * This immutable data class defines all configurable aspects of overlay behavior
 * and appearance. Use .copy() to create modified versions.
 *
 * Example usage:
 * ```kotlin
 * // Create default config
 * val config = OverlaySystemConfig()
 *
 * // Create config with custom settings
 * val customConfig = OverlaySystemConfig(
 *     enabled = true,
 *     opacity = 0.8f,
 *     position = OverlayPosition.BOTTOM_CENTER,
 *     accessibility = AccessibilityConfig(largeText = true)
 * )
 *
 * // Modify existing config
 * val modified = config.copy(
 *     enabled = false,
 *     autoHideDelay = 3000L
 * )
 * ```
 *
 * @property enabled Whether the overlay system is enabled (default: true)
 * @property opacity Overlay opacity from 0.0 (transparent) to 1.0 (opaque) (default: 1.0)
 * @property animationDuration Duration of show/hide animations in milliseconds (default: 200)
 * @property touchPassthrough Allow touches to pass through the overlay (default: false)
 * @property autoHideDelay Auto-hide delay in milliseconds; 0 means no auto-hide (default: 0)
 * @property position Screen position for the overlay (default: CENTER)
 * @property accessibility Accessibility configuration (default: AccessibilityConfig())
 */
data class OverlaySystemConfig(
    val enabled: Boolean = true,
    val opacity: Float = 1.0f,
    val animationDuration: Long = 200L,
    val touchPassthrough: Boolean = false,
    val autoHideDelay: Long = 0L,
    val position: OverlayPosition = OverlayPosition.CENTER,
    val accessibility: AccessibilityConfig = AccessibilityConfig()
) {
    companion object {
        /**
         * Create a config optimized for accessibility.
         * Enables large text, high contrast, and reduced motion.
         */
        fun forAccessibility(): OverlaySystemConfig = OverlaySystemConfig(
            accessibility = AccessibilityConfig(
                largeText = true,
                highContrast = true,
                reduceMotion = true
            )
        )

        /**
         * Create a config optimized for minimal intrusion.
         * Lower opacity, touch passthrough enabled, auto-hide after 2 seconds.
         */
        fun minimal(): OverlaySystemConfig = OverlaySystemConfig(
            opacity = 0.7f,
            touchPassthrough = true,
            autoHideDelay = 2000L,
            position = OverlayPosition.BOTTOM_CENTER
        )

        /**
         * Create a disabled config.
         */
        fun disabled(): OverlaySystemConfig = OverlaySystemConfig(enabled = false)
    }
}
