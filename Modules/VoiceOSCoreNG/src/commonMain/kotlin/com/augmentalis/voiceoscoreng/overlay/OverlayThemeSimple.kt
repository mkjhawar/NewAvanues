/**
 * OverlayThemeSimple.kt - Simplified overlay theme configuration
 *
 * Provides a streamlined theme data class for overlay UI components
 * with LIGHT, DARK, and HIGH_CONTRAST preset themes.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscoreng.overlay

/**
 * Simple overlay theme configuration
 *
 * Provides basic theming properties for overlay UI including colors,
 * borders, typography, and shadows. Use the companion object presets
 * (LIGHT, DARK, HIGH_CONTRAST) for common themes or create custom
 * themes with specific values.
 *
 * @property name Theme identifier name
 * @property backgroundColor Background color in ARGB format (0xAARRGGBB)
 * @property textColor Primary text color in ARGB format
 * @property accentColor Accent/highlight color in ARGB format
 * @property borderColor Border color in ARGB format
 * @property borderWidth Border width in dp (default: 1)
 * @property cornerRadius Corner radius in dp (default: 8)
 * @property fontSize Base font size in sp (default: 14)
 * @property fontFamily Font family name (default: "default")
 * @property shadowEnabled Whether shadows are enabled (default: true)
 * @property shadowColor Shadow color in ARGB format (default: 25% black)
 */
data class OverlayThemeSimple(
    val name: String,
    val backgroundColor: Long,
    val textColor: Long,
    val accentColor: Long,
    val borderColor: Long,
    val borderWidth: Int = 1,
    val cornerRadius: Int = 8,
    val fontSize: Int = 14,
    val fontFamily: String = "default",
    val shadowEnabled: Boolean = true,
    val shadowColor: Long = 0x40000000L
) {
    companion object {
        /**
         * Light theme with white background and dark text
         */
        val LIGHT = OverlayThemeSimple(
            name = "light",
            backgroundColor = 0xFFFFFFFFL,
            textColor = 0xFF000000L,
            accentColor = 0xFF2196F3L,
            borderColor = 0xFFE0E0E0L
        )

        /**
         * Dark theme with dark background and light text
         */
        val DARK = OverlayThemeSimple(
            name = "dark",
            backgroundColor = 0xFF212121L,
            textColor = 0xFFFFFFFFL,
            accentColor = 0xFF64B5F6L,
            borderColor = 0xFF424242L
        )

        /**
         * High contrast theme for accessibility
         * Features pure black background, white text, yellow accent,
         * and thicker borders for maximum visibility
         */
        val HIGH_CONTRAST = OverlayThemeSimple(
            name = "high_contrast",
            backgroundColor = 0xFF000000L,
            textColor = 0xFFFFFFFFL,
            accentColor = 0xFFFFFF00L,
            borderColor = 0xFFFFFFFFL,
            borderWidth = 2
        )
    }
}
