/**
 * VoiceOSCoreNGComposeTheme.kt - Main Compose theme wrapper for VoiceOSCoreNG
 *
 * Provides a composable theme provider that converts the KMP-compatible
 * OverlayTheme to Material3 theming components. Supports dynamic theme
 * switching and accessibility variants.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscoreng.features

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.augmentalis.voiceoscoreng.features.OverlayTheme

// ===== COMPOSITION LOCALS =====

/**
 * CompositionLocal for accessing the current OverlayTheme.
 *
 * Provides access to raw OverlayTheme values when Material3 semantics
 * don't cover specific needs (e.g., badge colors, status colors).
 */
val LocalOverlayTheme = staticCompositionLocalOf { OverlayTheme.DEFAULT }

/**
 * CompositionLocal for VoiceOS-specific extended colors.
 *
 * Provides access to colors not covered by Material3 ColorScheme,
 * such as status indicators and badge states.
 */
val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }

/**
 * Extended color palette for VoiceOS-specific UI elements.
 *
 * These colors complement Material3 ColorScheme with voice UI-specific
 * semantic colors that don't map to standard Material roles.
 */
data class ExtendedColors(
    /** Badge state colors for numbered element selection */
    val badgeEnabledWithName: androidx.compose.ui.graphics.Color = 0xFF4CAF50.toComposeColor(),
    val badgeEnabledNoName: androidx.compose.ui.graphics.Color = 0xFFFF9800.toComposeColor(),
    val badgeDisabled: androidx.compose.ui.graphics.Color = 0xFF9E9E9E.toComposeColor(),

    /** Voice command status colors */
    val statusListening: androidx.compose.ui.graphics.Color = 0xFF2196F3.toComposeColor(),
    val statusProcessing: androidx.compose.ui.graphics.Color = 0xFFFF9800.toComposeColor(),
    val statusSuccess: androidx.compose.ui.graphics.Color = 0xFF4CAF50.toComposeColor(),
    val statusError: androidx.compose.ui.graphics.Color = 0xFFF44336.toComposeColor(),

    /** Overlay-specific colors */
    val backdrop: androidx.compose.ui.graphics.Color = 0x4D000000.toComposeColor(),
    val tooltip: androidx.compose.ui.graphics.Color = 0xEE000000.toComposeColor(),
    val focusIndicator: androidx.compose.ui.graphics.Color = 0xFF2196F3.toComposeColor()
)

/**
 * Create ExtendedColors from OverlayTheme.
 */
fun OverlayTheme.toExtendedColors(): ExtendedColors {
    return ExtendedColors(
        badgeEnabledWithName = badgeEnabledWithNameColor.toComposeColor(),
        badgeEnabledNoName = badgeEnabledNoNameColor.toComposeColor(),
        badgeDisabled = badgeDisabledColor.toComposeColor(),
        statusListening = statusListeningColor.toComposeColor(),
        statusProcessing = statusProcessingColor.toComposeColor(),
        statusSuccess = statusSuccessColor.toComposeColor(),
        statusError = statusErrorColor.toComposeColor(),
        backdrop = backdropColor.toComposeColor(),
        tooltip = tooltipBackgroundColor.toComposeColor(),
        focusIndicator = focusIndicatorColor.toComposeColor()
    )
}

// ===== THEME CONFIGURATION =====

/**
 * Theme mode configuration options.
 */
enum class VoiceOSThemeMode {
    /** Always use dark theme (default for overlays) */
    DARK,
    /** Always use light theme */
    LIGHT,
    /** Follow system preference */
    SYSTEM,
    /** Use Android 12+ dynamic color with OverlayTheme fallback */
    DYNAMIC
}

/**
 * Accessibility mode configuration.
 */
enum class VoiceOSAccessibilityMode {
    /** Standard theme */
    STANDARD,
    /** High contrast for vision impairment */
    HIGH_CONTRAST,
    /** Large text for readability */
    LARGE_TEXT,
    /** Reduced motion for vestibular disorders */
    REDUCED_MOTION
}

// ===== MAIN THEME COMPOSABLE =====

/**
 * VoiceOSCoreNG theme wrapper for Compose UI.
 *
 * Converts OverlayTheme to Material3 theming and provides composition locals
 * for accessing both Material3 and VoiceOS-specific theme values.
 *
 * Usage:
 * ```kotlin
 * VoiceOSCoreNGTheme(
 *     theme = OverlayTheme.DEFAULT,
 *     mode = VoiceOSThemeMode.DARK
 * ) {
 *     // Your composables here
 *     // Access Material3: MaterialTheme.colorScheme.primary
 *     // Access VoiceOS: LocalOverlayTheme.current.badgeSize
 *     // Access Extended: LocalExtendedColors.current.statusListening
 * }
 * ```
 *
 * @param theme The OverlayTheme to apply (default: OverlayTheme.DEFAULT)
 * @param mode Theme mode: DARK, LIGHT, SYSTEM, or DYNAMIC
 * @param accessibilityMode Accessibility variant to apply
 * @param updateStatusBar Whether to update status bar color to match theme
 * @param content The composable content to wrap
 */
@Composable
fun VoiceOSCoreNGTheme(
    theme: OverlayTheme = OverlayTheme.DEFAULT,
    mode: VoiceOSThemeMode = VoiceOSThemeMode.DARK,
    accessibilityMode: VoiceOSAccessibilityMode = VoiceOSAccessibilityMode.STANDARD,
    updateStatusBar: Boolean = true,
    content: @Composable () -> Unit
) {
    // Apply accessibility modifications
    val accessibleTheme = when (accessibilityMode) {
        VoiceOSAccessibilityMode.STANDARD -> theme
        VoiceOSAccessibilityMode.HIGH_CONTRAST -> theme.toHighContrast()
        VoiceOSAccessibilityMode.LARGE_TEXT -> theme.withLargeText()
        VoiceOSAccessibilityMode.REDUCED_MOTION -> theme.withReducedMotion()
    }

    // Determine if dark theme should be used
    val useDarkTheme = when (mode) {
        VoiceOSThemeMode.DARK -> true
        VoiceOSThemeMode.LIGHT -> false
        VoiceOSThemeMode.SYSTEM -> isSystemInDarkTheme()
        VoiceOSThemeMode.DYNAMIC -> isSystemInDarkTheme()
    }

    // Resolve color scheme
    val colorScheme: ColorScheme = when {
        // Use dynamic colors on Android 12+ if requested
        mode == VoiceOSThemeMode.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        // Otherwise use OverlayTheme-derived colors
        useDarkTheme -> accessibleTheme.toColorScheme()
        else -> accessibleTheme.toLightColorScheme()
    }

    // Get typography from theme
    val typography: Typography = accessibleTheme.toTypography()

    // Get extended colors
    val extendedColors = accessibleTheme.toExtendedColors()

    // Update status bar color
    if (updateStatusBar) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as? Activity)?.window
                if (window != null) {
                    window.statusBarColor = colorScheme.background.toArgb()
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !useDarkTheme
                    }
                }
            }
        }
    }

    // Provide theme values through composition
    CompositionLocalProvider(
        LocalOverlayTheme provides accessibleTheme,
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

// ===== CONVENIENCE COMPOSABLES =====

/**
 * Simplified theme for overlay content (always dark, semi-transparent).
 *
 * Use this for popup overlays, badges, and floating UI elements
 * that should maintain consistent appearance.
 */
@Composable
fun VoiceOSOverlayTheme(
    theme: OverlayTheme = OverlayTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    VoiceOSCoreNGTheme(
        theme = theme,
        mode = VoiceOSThemeMode.DARK,
        updateStatusBar = false,
        content = content
    )
}

/**
 * High contrast theme for accessibility.
 *
 * Automatically applies high contrast modifications for users
 * with vision impairments.
 */
@Composable
fun VoiceOSHighContrastTheme(
    theme: OverlayTheme = OverlayTheme.HIGH_CONTRAST,
    content: @Composable () -> Unit
) {
    VoiceOSCoreNGTheme(
        theme = theme,
        mode = VoiceOSThemeMode.DARK,
        accessibilityMode = VoiceOSAccessibilityMode.HIGH_CONTRAST,
        content = content
    )
}

// ===== THEME ACCESSOR OBJECT =====

/**
 * Accessor object for VoiceOS theme values within composables.
 *
 * Provides convenient access to both Material3 and VoiceOS-specific
 * theme values.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun MyComponent() {
 *     val primary = VoiceOSTheme.colorScheme.primary
 *     val statusListening = VoiceOSTheme.extendedColors.statusListening
 *     val badgeSize = VoiceOSTheme.overlayTheme.badgeSize
 * }
 * ```
 */
object VoiceOSTheme {
    /**
     * Access Material3 color scheme.
     */
    val colorScheme: ColorScheme
        @Composable
        get() = MaterialTheme.colorScheme

    /**
     * Access Material3 typography.
     */
    val typography: Typography
        @Composable
        get() = MaterialTheme.typography

    /**
     * Access the raw OverlayTheme for VoiceOS-specific values.
     */
    val overlayTheme: OverlayTheme
        @Composable
        get() = LocalOverlayTheme.current

    /**
     * Access VoiceOS extended colors (status, badges, etc.).
     */
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}
