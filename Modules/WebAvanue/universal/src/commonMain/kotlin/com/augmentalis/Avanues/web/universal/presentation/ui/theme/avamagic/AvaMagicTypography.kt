package com.augmentalis.webavanue.ui.screen.theme.avamagic

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.augmentalis.webavanue.ui.screen.theme.abstraction.AppTypography

/**
 * AvaMagicTypography - Avanues system-wide typography
 *
 * This typography will be provided by Avanues system, ensuring consistent
 * text styles across ALL apps in the Avanues ecosystem.
 *
 * When an app runs inside Avanues ecosystem, this typography overrides the
 * app's unique typography.
 *
 * Features:
 * - Custom AvaMagic font stack (when available)
 * - Optimized for voice interaction (larger text, better readability)
 * - Consistent with Avanues branding
 * - Accessibility-first design
 *
 * Current Status:
 * - Architecture in place
 * - Using placeholder type scale (slightly larger for voice-first)
 * - Will query AvanuesThemeService when available
 *
 * Future Implementation:
 * ```kotlin
 * class AvaMagicTypography : AppTypography {
 *     private val systemTypography = AvanuesThemeService.getCurrentTypography()
 *     override val bodyLarge = systemTypography.bodyLarge  // System decides!
 * }
 * ```
 */
class AvaMagicTypography : AppTypography {

    // Display styles (bolder for AvaMagic branding)
    override val displayLarge = TextStyle(
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.Bold
    )
    override val displayMedium = TextStyle(
        fontSize = 45.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.Bold
    )
    override val displaySmall = TextStyle(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.Bold
    )

    // Headline styles
    override val headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold
    )
    override val headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold
    )
    override val headlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold
    )

    // Title styles
    override val titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium
    )
    override val titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium
    )
    override val titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    )

    // Body styles (slightly larger for voice-first UI readability)
    override val bodyLarge = TextStyle(
        fontSize = 17.sp,  // +1sp for better voice UI readability
        lineHeight = 26.sp,
        fontWeight = FontWeight.Normal
    )
    override val bodyMedium = TextStyle(
        fontSize = 15.sp,  // +1sp
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal
    )
    override val bodySmall = TextStyle(
        fontSize = 13.sp,  // +1sp
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal
    )

    // Label styles
    override val labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    )
    override val labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    )
    override val labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    )
}
