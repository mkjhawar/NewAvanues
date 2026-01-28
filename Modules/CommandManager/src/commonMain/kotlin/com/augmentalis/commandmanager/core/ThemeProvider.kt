/**
 * ThemeProvider.kt - Singleton theme provider for VoiceOSCoreNG
 *
 * Thread-safe implementation of IThemeProvider with support for
 * multiple theme variants and observable theme changes.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.OverlayTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Singleton theme provider for VoiceOSCoreNG.
 *
 * Provides centralized theme management with:
 * - Thread-safe theme switching using Mutex
 * - Observable theme changes via StateFlow
 * - Pre-built theme variants for accessibility
 * - Custom theme support
 *
 * Example usage:
 * ```kotlin
 * // Get singleton instance
 * val themeProvider = ThemeProvider.instance
 *
 * // Observe theme changes in Compose
 * val theme by themeProvider.currentTheme.collectAsState()
 *
 * // Switch to high contrast mode
 * themeProvider.setVariant(ThemeVariant.HIGH_CONTRAST)
 *
 * // Set custom theme
 * themeProvider.setCustomTheme(OverlayTheme(primaryColor = 0xFF00BCD4))
 * ```
 */
object ThemeProvider : IThemeProvider {

    /**
     * Singleton instance accessor.
     * Returns this object for explicit singleton pattern usage.
     */
    val instance: ThemeProvider get() = this

    // Thread-safety mutex for state updates
    private val mutex = Mutex()

    // Mutable state flows for internal updates
    private val _currentTheme = MutableStateFlow(OverlayTheme.DEFAULT)
    private val _currentVariant = MutableStateFlow(ThemeVariant.DEFAULT)

    // Track if using custom theme vs variant
    private var isCustomTheme = false

    override val currentTheme: StateFlow<OverlayTheme> = _currentTheme.asStateFlow()
    override val currentVariant: StateFlow<ThemeVariant> = _currentVariant.asStateFlow()

    /**
     * Pre-built theme variants cache.
     * Lazy initialization to avoid overhead until needed.
     */
    private val themeVariants: Map<ThemeVariant, OverlayTheme> by lazy {
        buildThemeVariants()
    }

    override fun setVariant(variant: ThemeVariant) {
        // Use non-suspending synchronization for simple operations
        // This is safe because StateFlow.value setter is atomic
        _currentVariant.value = variant
        _currentTheme.value = getThemeForVariant(variant)
        isCustomTheme = false
    }

    override fun setCustomTheme(theme: OverlayTheme) {
        _currentTheme.value = theme
        isCustomTheme = true
    }

    override fun resetToDefault() {
        _currentVariant.value = ThemeVariant.DEFAULT
        _currentTheme.value = OverlayTheme.DEFAULT
        isCustomTheme = false
    }

    override fun getThemeForVariant(variant: ThemeVariant): OverlayTheme {
        return themeVariants[variant] ?: OverlayTheme.DEFAULT
    }

    /**
     * Suspend function for thread-safe theme switching.
     * Use this when coordinating with other coroutines.
     */
    suspend fun setVariantSafe(variant: ThemeVariant) {
        mutex.withLock {
            _currentVariant.value = variant
            _currentTheme.value = getThemeForVariant(variant)
            isCustomTheme = false
        }
    }

    /**
     * Suspend function for thread-safe custom theme setting.
     * Use this when coordinating with other coroutines.
     */
    suspend fun setCustomThemeSafe(theme: OverlayTheme) {
        mutex.withLock {
            _currentTheme.value = theme
            isCustomTheme = true
        }
    }

    /**
     * Check if currently using a custom theme (not a preset variant).
     */
    fun isUsingCustomTheme(): Boolean = isCustomTheme

    /**
     * Get all available theme variants.
     */
    fun getAvailableVariants(): List<ThemeVariant> = ThemeVariant.entries

    /**
     * Build all theme variant configurations.
     */
    private fun buildThemeVariants(): Map<ThemeVariant, OverlayTheme> = mapOf(
        ThemeVariant.DEFAULT to OverlayTheme.DEFAULT,
        ThemeVariant.HIGH_CONTRAST to buildHighContrastTheme(),
        ThemeVariant.LARGE_TEXT to buildLargeTextTheme(),
        ThemeVariant.COLORBLIND_FRIENDLY to buildColorblindFriendlyTheme(),
        ThemeVariant.REDUCED_MOTION to buildReducedMotionTheme(),
        ThemeVariant.LIGHT to buildLightTheme(),
        ThemeVariant.GAMING to buildGamingTheme(),
        ThemeVariant.MINIMALIST to buildMinimalistTheme()
    )

    /**
     * High contrast theme for maximum visibility (WCAG AAA).
     */
    private fun buildHighContrastTheme(): OverlayTheme = OverlayTheme(
        // Pure black/white for maximum contrast
        backgroundColor = 0xFF000000,
        backdropColor = 0x80000000,
        textPrimaryColor = 0xFFFFFFFF,
        textSecondaryColor = 0xFFFFFFFF,
        textDisabledColor = 0xFFAAAAAA,

        // Bold borders for visibility
        borderColor = 0xFFFFFFFF,
        borderWidthMedium = 3f,
        borderWidthThick = 4f,

        // High contrast status colors
        badgeEnabledWithNameColor = 0xFF00FF00,  // Bright green
        badgeEnabledNoNameColor = 0xFFFFFF00,    // Yellow
        badgeDisabledColor = 0xFF888888,
        statusListeningColor = 0xFF00BFFF,       // Deep sky blue
        statusProcessingColor = 0xFFFFFF00,      // Yellow
        statusSuccessColor = 0xFF00FF00,         // Bright green
        statusErrorColor = 0xFFFF0000,           // Bright red

        // Enhanced focus indicator
        focusIndicatorColor = 0xFFFFFF00,
        focusIndicatorWidth = 4f,

        // WCAG AAA contrast ratio
        minimumContrastRatio = 7.0f
    )

    /**
     * Large text theme for improved readability.
     */
    private fun buildLargeTextTheme(): OverlayTheme = OverlayTheme.DEFAULT.copy(
        // Increased font sizes (25% larger)
        titleFontSize = 20f,
        bodyFontSize = 18f,
        captionFontSize = 15f,
        smallFontSize = 14f,
        badgeFontSize = 18f,
        instructionFontSize = 20f,

        // Larger badges to accommodate bigger text
        badgeSize = 40f,
        badgeNumberSize = 36f,

        // Increased spacing
        paddingMedium = 12f,
        paddingLarge = 20f,
        spacingMedium = 16f,
        spacingLarge = 20f
    )

    /**
     * Colorblind-friendly theme avoiding red/green confusion.
     * Uses blue/orange/yellow as primary differentiators.
     */
    private fun buildColorblindFriendlyTheme(): OverlayTheme = OverlayTheme.DEFAULT.copy(
        // Blue as primary - safe for all colorblind types
        primaryColor = 0xFF0077BB,  // Accessible blue

        // Orange and yellow instead of green/red
        badgeEnabledWithNameColor = 0xFF0077BB,   // Blue - has name
        badgeEnabledNoNameColor = 0xFFEE7733,     // Orange - no name
        badgeDisabledColor = 0xFF888888,          // Gray - disabled

        // Status colors avoiding red/green
        statusListeningColor = 0xFF0077BB,        // Blue
        statusProcessingColor = 0xFFEE7733,       // Orange
        statusSuccessColor = 0xFF009988,          // Teal (distinct from red)
        statusErrorColor = 0xFFCC3311,            // Dark orange-red

        // Focus indicator in accessible blue
        focusIndicatorColor = 0xFF0077BB
    )

    /**
     * Reduced motion theme with all animations disabled.
     */
    private fun buildReducedMotionTheme(): OverlayTheme = OverlayTheme.DEFAULT.copy(
        animationDurationFast = 0,
        animationDurationNormal = 0,
        animationDurationSlow = 0,
        animationEnabled = false
    )

    /**
     * Light theme for daytime/bright environment use.
     */
    private fun buildLightTheme(): OverlayTheme = OverlayTheme(
        primaryColor = 0xFF1976D2,  // Material Blue

        // Light backgrounds
        backgroundColor = 0xFFF5F5F5,
        backdropColor = 0x33000000,  // Subtle dimming
        cardBackgroundColor = 0xFFFFFFFF,
        tooltipBackgroundColor = 0xEE424242,

        // Dark text on light
        textPrimaryColor = 0xFF212121,
        textSecondaryColor = 0xFF757575,
        textDisabledColor = 0xFFBDBDBD,

        // Visible borders
        borderColor = 0xFF424242,
        dividerColor = 0x1F000000,

        // Status colors adjusted for light bg
        badgeEnabledWithNameColor = 0xFF388E3C,   // Darker green
        badgeEnabledNoNameColor = 0xFFF57C00,     // Darker orange
        badgeDisabledColor = 0xFF9E9E9E,
        statusListeningColor = 0xFF1976D2,
        statusProcessingColor = 0xFFF57C00,
        statusSuccessColor = 0xFF388E3C,
        statusErrorColor = 0xFFD32F2F,

        focusIndicatorColor = 0xFF1976D2
    )

    /**
     * Gaming theme with vibrant colors and larger touch targets.
     */
    private fun buildGamingTheme(): OverlayTheme = OverlayTheme(
        primaryColor = 0xFF00E5FF,  // Cyan accent

        // Dark background with subtle glow effect colors
        backgroundColor = 0xF0101020,
        backdropColor = 0x60000000,
        cardBackgroundColor = 0xF0151530,
        tooltipBackgroundColor = 0xF0101020,

        // Bright, vibrant text
        textPrimaryColor = 0xFFFFFFFF,
        textSecondaryColor = 0xFFB0BEC5,
        textDisabledColor = 0xFF607D8B,

        // Neon-style borders
        borderColor = 0xFF00E5FF,
        dividerColor = 0x3000E5FF,

        // Vibrant status colors
        badgeEnabledWithNameColor = 0xFF00E676,   // Neon green
        badgeEnabledNoNameColor = 0xFFFFD740,     // Amber
        badgeDisabledColor = 0xFF607D8B,
        statusListeningColor = 0xFF00E5FF,        // Cyan
        statusProcessingColor = 0xFFFFD740,       // Amber
        statusSuccessColor = 0xFF00E676,          // Neon green
        statusErrorColor = 0xFFFF1744,            // Red accent

        // Larger touch targets for quick gaming response
        badgeSize = 40f,
        badgeNumberSize = 36f,
        minimumTouchTargetSize = 56f,

        // Faster animations for responsiveness
        animationDurationFast = 100,
        animationDurationNormal = 150,
        animationDurationSlow = 200,

        focusIndicatorColor = 0xFF00E5FF,
        focusIndicatorWidth = 2f
    )

    /**
     * Minimalist theme with reduced visual elements.
     */
    private fun buildMinimalistTheme(): OverlayTheme = OverlayTheme(
        primaryColor = 0xFF90A4AE,  // Blue grey

        // Subtle, near-transparent backgrounds
        backgroundColor = 0xCC2C2C2C,
        backdropColor = 0x20000000,
        cardBackgroundColor = 0xCC3C3C3C,
        tooltipBackgroundColor = 0xCC2C2C2C,

        // Muted text colors
        textPrimaryColor = 0xFFE0E0E0,
        textSecondaryColor = 0xFF9E9E9E,
        textDisabledColor = 0xFF616161,

        // Thin, subtle borders
        borderColor = 0xFF757575,
        borderWidthThin = 0.5f,
        borderWidthMedium = 1f,
        borderWidthThick = 1.5f,
        dividerColor = 0x10FFFFFF,

        // Muted status colors
        badgeEnabledWithNameColor = 0xFF66BB6A,
        badgeEnabledNoNameColor = 0xFFFFB74D,
        badgeDisabledColor = 0xFF757575,
        statusListeningColor = 0xFF64B5F6,
        statusProcessingColor = 0xFFFFB74D,
        statusSuccessColor = 0xFF66BB6A,
        statusErrorColor = 0xFFEF5350,

        // Smaller badges
        badgeSize = 28f,
        badgeNumberSize = 24f,
        badgeFontSize = 12f,

        // Reduced spacing
        paddingSmall = 3f,
        paddingMedium = 6f,
        paddingLarge = 12f,
        spacingSmall = 6f,
        spacingMedium = 10f,
        spacingLarge = 14f,

        // Smaller corner radii
        cornerRadiusSmall = 4f,
        cornerRadiusMedium = 6f,
        cornerRadiusLarge = 8f,

        focusIndicatorColor = 0xFF90A4AE,
        focusIndicatorWidth = 2f
    )
}
