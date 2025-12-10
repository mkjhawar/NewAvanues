// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/theme/ThemeConfig.kt
// created: 2025-11-22
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.ui.theme

import androidx.compose.ui.graphics.Color
import com.augmentalis.ava.preferences.ThemeMode

/**
 * Theme Configuration
 *
 * Defines the complete theme configuration for AVA AI app.
 * Supports theme mode selection and custom accent colors.
 *
 * Features:
 * - Theme mode (Light/Dark/Auto)
 * - Custom accent colors
 * - Material You dynamic color support
 * - Persistence via UserPreferences
 *
 * @param themeMode The theme mode (LIGHT, DARK, AUTO)
 * @param accentColor Custom accent color (null = use default teal)
 * @param useDynamicColor Whether to use Material You dynamic colors (Android 12+)
 *
 * @author AVA AI Team
 * @version 1.0.0
 */
data class ThemeConfig(
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val accentColor: AccentColor = AccentColor.TEAL,
    val useDynamicColor: Boolean = false
) {
    /**
     * Check if dark mode should be active
     *
     * @param systemInDarkMode Whether system is in dark mode
     * @return True if dark mode should be active
     */
    fun isDarkMode(systemInDarkMode: Boolean): Boolean = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> systemInDarkMode
    }
}

/**
 * Predefined accent colors for theme customization
 *
 * Each accent color has light and dark variants for optimal
 * contrast in both theme modes.
 */
enum class AccentColor(
    val displayName: String,
    val lightColor: Color,
    val darkColor: Color,
    val description: String
) {
    TEAL(
        displayName = "Teal",
        lightColor = Color(0xFF03DAC6),
        darkColor = Color(0xFF03DAC6),
        description = "Default AVA teal accent"
    ),
    PURPLE(
        displayName = "Purple",
        lightColor = Color(0xFF7C4DFF),
        darkColor = Color(0xFFB388FF),
        description = "Purple accent matching AVA gradient"
    ),
    BLUE(
        displayName = "Blue",
        lightColor = Color(0xFF2196F3),
        darkColor = Color(0xFF64B5F6),
        description = "Classic blue accent"
    ),
    GREEN(
        displayName = "Green",
        lightColor = Color(0xFF4CAF50),
        darkColor = Color(0xFF81C784),
        description = "Fresh green accent"
    ),
    ORANGE(
        displayName = "Orange",
        lightColor = Color(0xFFFF9800),
        darkColor = Color(0xFFFFB74D),
        description = "Energetic orange accent"
    ),
    PINK(
        displayName = "Pink",
        lightColor = Color(0xFFE91E63),
        darkColor = Color(0xFFF06292),
        description = "Vibrant pink accent"
    );

    /**
     * Get accent color for current theme mode
     *
     * @param isDarkMode Whether dark mode is active
     * @return Appropriate accent color
     */
    fun getColor(isDarkMode: Boolean): Color = if (isDarkMode) darkColor else lightColor

    companion object {
        /**
         * Parse accent color from string
         *
         * @param value String representation
         * @return AccentColor or TEAL as default
         */
        fun fromString(value: String?): AccentColor = when (value?.uppercase()) {
            "PURPLE" -> PURPLE
            "BLUE" -> BLUE
            "GREEN" -> GREEN
            "ORANGE" -> ORANGE
            "PINK" -> PINK
            else -> TEAL
        }
    }
}

/**
 * Theme contrast levels for accessibility
 *
 * Defines contrast ratios for improved accessibility.
 * Higher contrast improves readability for users with visual impairments.
 */
enum class ContrastLevel(
    val displayName: String,
    val multiplier: Float,
    val description: String
) {
    STANDARD(
        displayName = "Standard",
        multiplier = 1.0f,
        description = "Standard contrast (WCAG AA)"
    ),
    MEDIUM(
        displayName = "Medium",
        multiplier = 1.2f,
        description = "Enhanced contrast"
    ),
    HIGH(
        displayName = "High",
        multiplier = 1.5f,
        description = "High contrast (WCAG AAA)"
    );

    companion object {
        fun fromString(value: String?): ContrastLevel = when (value?.uppercase()) {
            "MEDIUM" -> MEDIUM
            "HIGH" -> HIGH
            else -> STANDARD
        }
    }
}
