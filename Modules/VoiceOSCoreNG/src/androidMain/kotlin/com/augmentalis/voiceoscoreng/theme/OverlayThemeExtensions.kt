/**
 * OverlayThemeExtensions.kt - Extension functions to convert OverlayTheme to Compose types
 *
 * Provides seamless conversion between the KMP-compatible OverlayTheme and
 * Android Compose UI types (Color, ColorScheme, Typography).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscoreng.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceoscoreng.overlay.OverlayTheme

// ===== COLOR CONVERSION EXTENSIONS =====

/**
 * Convert a Long color (ARGB format: 0xAARRGGBB) to Compose Color.
 *
 * @return Compose Color with alpha, red, green, blue components extracted
 */
fun Long.toComposeColor(): Color {
    val alpha = ((this shr 24) and 0xFF).toInt()
    val red = ((this shr 16) and 0xFF).toInt()
    val green = ((this shr 8) and 0xFF).toInt()
    val blue = (this and 0xFF).toInt()
    return Color(red, green, blue, alpha)
}

/**
 * Convert an Int color (ARGB format: 0xAARRGGBB) to Compose Color.
 * Useful for inline color literals like 0xFF4CAF50 which are Int in Kotlin.
 *
 * @return Compose Color with alpha, red, green, blue components extracted
 */
fun Int.toComposeColor(): Color {
    return this.toLong().toComposeColor()
}

/**
 * Convert Compose Color to Long (ARGB format: 0xAARRGGBB).
 *
 * @return Long color value in ARGB format
 */
fun Color.toLongColor(): Long {
    val alpha = (this.alpha * 255).toLong()
    val red = (this.red * 255).toLong()
    val green = (this.green * 255).toLong()
    val blue = (this.blue * 255).toLong()
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}

// ===== OVERLAY THEME COLOR ACCESSORS =====

/**
 * Extension properties to access OverlayTheme colors as Compose Color.
 */
val OverlayTheme.primaryColorCompose: Color get() = primaryColor.toComposeColor()
val OverlayTheme.backgroundColorCompose: Color get() = backgroundColor.toComposeColor()
val OverlayTheme.backdropColorCompose: Color get() = backdropColor.toComposeColor()
val OverlayTheme.textPrimaryColorCompose: Color get() = textPrimaryColor.toComposeColor()
val OverlayTheme.textSecondaryColorCompose: Color get() = textSecondaryColor.toComposeColor()
val OverlayTheme.textDisabledColorCompose: Color get() = textDisabledColor.toComposeColor()
val OverlayTheme.borderColorCompose: Color get() = borderColor.toComposeColor()
val OverlayTheme.dividerColorCompose: Color get() = dividerColor.toComposeColor()
val OverlayTheme.badgeEnabledWithNameColorCompose: Color get() = badgeEnabledWithNameColor.toComposeColor()
val OverlayTheme.badgeEnabledNoNameColorCompose: Color get() = badgeEnabledNoNameColor.toComposeColor()
val OverlayTheme.badgeDisabledColorCompose: Color get() = badgeDisabledColor.toComposeColor()
val OverlayTheme.statusListeningColorCompose: Color get() = statusListeningColor.toComposeColor()
val OverlayTheme.statusProcessingColorCompose: Color get() = statusProcessingColor.toComposeColor()
val OverlayTheme.statusSuccessColorCompose: Color get() = statusSuccessColor.toComposeColor()
val OverlayTheme.statusErrorColorCompose: Color get() = statusErrorColor.toComposeColor()
val OverlayTheme.cardBackgroundColorCompose: Color get() = cardBackgroundColor.toComposeColor()
val OverlayTheme.tooltipBackgroundColorCompose: Color get() = tooltipBackgroundColor.toComposeColor()
val OverlayTheme.focusIndicatorColorCompose: Color get() = focusIndicatorColor.toComposeColor()

// ===== COLOR SCHEME CONVERSION =====

/**
 * Convert OverlayTheme to Material3 ColorScheme (dark theme).
 *
 * Maps OverlayTheme colors to appropriate Material3 semantic color roles:
 * - primary: Main accent color for buttons, FABs, switches
 * - secondary: Secondary accent for less prominent components
 * - tertiary: Third accent color for contrast
 * - background/surface: Background colors
 * - onPrimary/onBackground/onSurface: Content colors (text, icons)
 * - error: Error states
 *
 * @return Material3 dark ColorScheme configured with OverlayTheme colors
 */
fun OverlayTheme.toColorScheme(): ColorScheme {
    return darkColorScheme(
        // Primary colors
        primary = primaryColor.toComposeColor(),
        onPrimary = Color.White,
        primaryContainer = primaryColor.toComposeColor().copy(alpha = 0.3f),
        onPrimaryContainer = textPrimaryColor.toComposeColor(),

        // Secondary colors (using listening color as secondary accent)
        secondary = statusListeningColor.toComposeColor(),
        onSecondary = Color.White,
        secondaryContainer = statusListeningColor.toComposeColor().copy(alpha = 0.3f),
        onSecondaryContainer = textPrimaryColor.toComposeColor(),

        // Tertiary colors (using success color)
        tertiary = statusSuccessColor.toComposeColor(),
        onTertiary = Color.White,
        tertiaryContainer = statusSuccessColor.toComposeColor().copy(alpha = 0.3f),
        onTertiaryContainer = textPrimaryColor.toComposeColor(),

        // Background colors
        background = backgroundColor.toComposeColor(),
        onBackground = textPrimaryColor.toComposeColor(),

        // Surface colors
        surface = cardBackgroundColor.toComposeColor(),
        onSurface = textPrimaryColor.toComposeColor(),
        surfaceVariant = cardBackgroundColor.toComposeColor().copy(alpha = 0.8f),
        onSurfaceVariant = textSecondaryColor.toComposeColor(),

        // Surface container colors (M3 tonal surfaces)
        surfaceContainer = cardBackgroundColor.toComposeColor(),
        surfaceContainerHigh = cardBackgroundColor.toComposeColor().copy(alpha = 0.9f),
        surfaceContainerHighest = cardBackgroundColor.toComposeColor(),
        surfaceContainerLow = backgroundColor.toComposeColor(),
        surfaceContainerLowest = backgroundColor.toComposeColor().copy(alpha = 0.9f),

        // Inverse colors
        inverseSurface = textPrimaryColor.toComposeColor(),
        inverseOnSurface = backgroundColor.toComposeColor(),
        inversePrimary = primaryColor.toComposeColor().copy(alpha = 0.8f),

        // Error colors
        error = statusErrorColor.toComposeColor(),
        onError = Color.White,
        errorContainer = statusErrorColor.toComposeColor().copy(alpha = 0.3f),
        onErrorContainer = textPrimaryColor.toComposeColor(),

        // Outline colors
        outline = borderColor.toComposeColor().copy(alpha = 0.5f),
        outlineVariant = dividerColor.toComposeColor(),

        // Scrim for modal overlays
        scrim = backdropColor.toComposeColor()
    )
}

/**
 * Create a light ColorScheme variant from OverlayTheme.
 *
 * Useful for UI that needs light mode support while maintaining
 * VoiceOS brand colors.
 *
 * @return Material3 light ColorScheme configured with inverted brightness
 */
fun OverlayTheme.toLightColorScheme(): ColorScheme {
    return lightColorScheme(
        primary = primaryColor.toComposeColor(),
        onPrimary = Color.White,
        primaryContainer = primaryColor.toComposeColor().copy(alpha = 0.2f),
        onPrimaryContainer = Color(0xFF1E1E1E),

        secondary = statusListeningColor.toComposeColor(),
        onSecondary = Color.White,
        secondaryContainer = statusListeningColor.toComposeColor().copy(alpha = 0.2f),
        onSecondaryContainer = Color(0xFF1E1E1E),

        tertiary = statusSuccessColor.toComposeColor(),
        onTertiary = Color.White,
        tertiaryContainer = statusSuccessColor.toComposeColor().copy(alpha = 0.2f),
        onTertiaryContainer = Color(0xFF1E1E1E),

        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF1E1E1E),

        surface = Color.White,
        onSurface = Color(0xFF1E1E1E),
        surfaceVariant = Color(0xFFF5F5F5),
        onSurfaceVariant = Color(0xFF666666),

        error = statusErrorColor.toComposeColor(),
        onError = Color.White,
        errorContainer = statusErrorColor.toComposeColor().copy(alpha = 0.2f),
        onErrorContainer = Color(0xFF1E1E1E),

        outline = Color(0xFFDDDDDD),
        outlineVariant = Color(0xFFEEEEEE),

        scrim = Color.Black.copy(alpha = 0.3f)
    )
}

// ===== TYPOGRAPHY CONVERSION =====

/**
 * Convert OverlayTheme to Material3 Typography.
 *
 * Maps OverlayTheme font sizes to Material3 type scale:
 * - displayLarge/Medium/Small: Not used (headings too large for overlays)
 * - headlineLarge/Medium/Small: Title font size
 * - titleLarge/Medium/Small: Title font size
 * - bodyLarge/Medium/Small: Body/caption font sizes
 * - labelLarge/Medium/Small: Small/badge font sizes
 *
 * @return Material3 Typography configured with OverlayTheme font sizes
 */
fun OverlayTheme.toTypography(): Typography {
    return Typography(
        // Display styles (using title for consistency in overlays)
        displayLarge = TextStyle(
            fontSize = titleFontSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default
        ),
        displayMedium = TextStyle(
            fontSize = titleFontSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default
        ),
        displaySmall = TextStyle(
            fontSize = titleFontSize.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Default
        ),

        // Headline styles
        headlineLarge = TextStyle(
            fontSize = titleFontSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default
        ),
        headlineMedium = TextStyle(
            fontSize = titleFontSize.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Default
        ),
        headlineSmall = TextStyle(
            fontSize = bodyFontSize.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Default
        ),

        // Title styles
        titleLarge = TextStyle(
            fontSize = titleFontSize.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Default
        ),
        titleMedium = TextStyle(
            fontSize = bodyFontSize.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Default
        ),
        titleSmall = TextStyle(
            fontSize = captionFontSize.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Default
        ),

        // Body styles
        bodyLarge = TextStyle(
            fontSize = bodyFontSize.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Default
        ),
        bodyMedium = TextStyle(
            fontSize = captionFontSize.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Default
        ),
        bodySmall = TextStyle(
            fontSize = smallFontSize.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Default
        ),

        // Label styles (used for badges, buttons)
        labelLarge = TextStyle(
            fontSize = badgeFontSize.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Default
        ),
        labelMedium = TextStyle(
            fontSize = captionFontSize.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Default
        ),
        labelSmall = TextStyle(
            fontSize = smallFontSize.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Default
        )
    )
}

// ===== DIMENSION CONVERSION EXTENSIONS =====

/**
 * Convert Float (dp value) to Compose Dp.
 */
val Float.dpCompose: Dp get() = this.dp

/**
 * Extension properties for OverlayTheme spacing as Compose Dp.
 */
val OverlayTheme.paddingSmallDp: Dp get() = paddingSmall.dp
val OverlayTheme.paddingMediumDp: Dp get() = paddingMedium.dp
val OverlayTheme.paddingLargeDp: Dp get() = paddingLarge.dp
val OverlayTheme.paddingXLargeDp: Dp get() = paddingXLarge.dp

val OverlayTheme.spacingTinyDp: Dp get() = spacingTiny.dp
val OverlayTheme.spacingSmallDp: Dp get() = spacingSmall.dp
val OverlayTheme.spacingMediumDp: Dp get() = spacingMedium.dp
val OverlayTheme.spacingLargeDp: Dp get() = spacingLarge.dp

val OverlayTheme.cornerRadiusSmallDp: Dp get() = cornerRadiusSmall.dp
val OverlayTheme.cornerRadiusMediumDp: Dp get() = cornerRadiusMedium.dp
val OverlayTheme.cornerRadiusLargeDp: Dp get() = cornerRadiusLarge.dp
val OverlayTheme.cornerRadiusXLargeDp: Dp get() = cornerRadiusXLarge.dp

val OverlayTheme.elevationLowDp: Dp get() = elevationLow.dp
val OverlayTheme.elevationMediumDp: Dp get() = elevationMedium.dp
val OverlayTheme.elevationHighDp: Dp get() = elevationHigh.dp

val OverlayTheme.badgeSizeDp: Dp get() = badgeSize.dp
val OverlayTheme.iconSizeSmallDp: Dp get() = iconSizeSmall.dp
val OverlayTheme.iconSizeMediumDp: Dp get() = iconSizeMedium.dp
val OverlayTheme.iconSizeLargeDp: Dp get() = iconSizeLarge.dp

val OverlayTheme.minimumTouchTargetSizeDp: Dp get() = minimumTouchTargetSize.dp
