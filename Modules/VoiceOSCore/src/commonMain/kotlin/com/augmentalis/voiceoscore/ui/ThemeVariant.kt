/**
 * ThemeVariant.kt - Theme variant enumeration for VoiceOSCoreNG
 *
 * Defines all available theme variants for accessibility and user preference.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscore

/**
 * Available theme variants for the VoiceOS overlay system.
 *
 * Each variant is designed for specific use cases:
 * - Accessibility needs (high contrast, large text, colorblind-friendly)
 * - User preferences (light, dark, gaming, minimalist)
 * - Motion sensitivity (reduced motion)
 */
enum class ThemeVariant {
    /**
     * Default dark theme optimized for overlay visibility
     */
    DEFAULT,

    /**
     * High contrast theme meeting WCAG AAA standards (7:1 contrast ratio)
     * Best for users with visual impairments
     */
    HIGH_CONTRAST,

    /**
     * Large text variant with increased font sizes
     * Best for users who need larger text for readability
     */
    LARGE_TEXT,

    /**
     * Colorblind-friendly palette avoiding red/green differentiation
     * Uses blue/orange/yellow as primary status indicators
     */
    COLORBLIND_FRIENDLY,

    /**
     * Reduced motion variant with all animations disabled
     * Best for users with motion sensitivity or vestibular disorders
     */
    REDUCED_MOTION,

    /**
     * Light theme variant for daytime use or bright environments
     * Features light backgrounds with dark text
     */
    LIGHT,

    /**
     * Gaming-focused theme with vibrant colors and larger touch targets
     * Optimized for quick recognition during gameplay
     */
    GAMING,

    /**
     * Minimalist theme with reduced visual elements
     * Smaller badges, subtle colors, less visual noise
     */
    MINIMALIST;

    companion object {
        /**
         * Get theme variant by name (case-insensitive)
         */
        fun fromName(name: String): ThemeVariant? {
            return entries.find { it.name.equals(name, ignoreCase = true) }
        }

        /**
         * Get all accessibility-focused variants
         */
        fun accessibilityVariants(): List<ThemeVariant> = listOf(
            HIGH_CONTRAST,
            LARGE_TEXT,
            COLORBLIND_FRIENDLY,
            REDUCED_MOTION
        )

        /**
         * Get all visual preference variants
         */
        fun preferenceVariants(): List<ThemeVariant> = listOf(
            DEFAULT,
            LIGHT,
            GAMING,
            MINIMALIST
        )
    }
}
