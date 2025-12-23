package com.augmentalis.avanues.avamagic.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design Tokens for IDEAMagic Design System
 * Material 3 inspired tokens with custom extensions
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

// ===== COLOR TOKENS =====

/**
 * Core color palette based on Material 3
 */
object ColorTokens {
    // Primary colors
    val Primary = Color(0xFF6750A4)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFEADDFF)
    val OnPrimaryContainer = Color(0xFF21005D)

    // Secondary colors
    val Secondary = Color(0xFF625B71)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFE8DEF8)
    val OnSecondaryContainer = Color(0xFF1D192B)

    // Tertiary colors
    val Tertiary = Color(0xFF7D5260)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFFFD8E4)
    val OnTertiaryContainer = Color(0xFF31111D)

    // Error colors
    val Error = Color(0xFFB3261E)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFF9DEDC)
    val OnErrorContainer = Color(0xFF410E0B)

    // Background colors
    val Background = Color(0xFFFFFBFE)
    val OnBackground = Color(0xFF1C1B1F)

    // Surface colors
    val Surface = Color(0xFFFFFBFE)
    val OnSurface = Color(0xFF1C1B1F)
    val SurfaceVariant = Color(0xFFE7E0EC)
    val OnSurfaceVariant = Color(0xFF49454F)

    // Outline colors
    val Outline = Color(0xFF79747E)
    val OutlineVariant = Color(0xFFCAC4D0)

    // Dark theme colors
    val DarkPrimary = Color(0xFFD0BCFF)
    val DarkOnPrimary = Color(0xFF381E72)
    val DarkPrimaryContainer = Color(0xFF4F378B)
    val DarkOnPrimaryContainer = Color(0xFFEADDFF)

    val DarkSecondary = Color(0xFFCCC2DC)
    val DarkOnSecondary = Color(0xFF332D41)
    val DarkSecondaryContainer = Color(0xFF4A4458)
    val DarkOnSecondaryContainer = Color(0xFFE8DEF8)

    val DarkTertiary = Color(0xFFEFB8C8)
    val DarkOnTertiary = Color(0xFF492532)
    val DarkTertiaryContainer = Color(0xFF633B48)
    val DarkOnTertiaryContainer = Color(0xFFFFD8E4)

    val DarkError = Color(0xFFF2B8B5)
    val DarkOnError = Color(0xFF601410)
    val DarkErrorContainer = Color(0xFF8C1D18)
    val DarkOnErrorContainer = Color(0xFFF9DEDC)

    val DarkBackground = Color(0xFF1C1B1F)
    val DarkOnBackground = Color(0xFFE6E1E5)

    val DarkSurface = Color(0xFF1C1B1F)
    val DarkOnSurface = Color(0xFFE6E1E5)
    val DarkSurfaceVariant = Color(0xFF49454F)
    val DarkOnSurfaceVariant = Color(0xFFCAC4D0)

    val DarkOutline = Color(0xFF938F99)
    val DarkOutlineVariant = Color(0xFF49454F)
}

// ===== TYPOGRAPHY TOKENS =====

/**
 * Typography scale based on Material 3
 */
object TypographyTokens {
    // Display styles (largest)
    val DisplayLarge = TextStyle(
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.25).sp
    )

    val DisplayMedium = TextStyle(
        fontSize = 45.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    val DisplaySmall = TextStyle(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    // Headline styles
    val HeadlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    val HeadlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    val HeadlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    // Title styles
    val TitleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    val TitleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.15.sp
    )

    val TitleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    )

    // Body styles (most common)
    val BodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp
    )

    val BodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.25.sp
    )

    val BodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp
    )

    // Label styles (buttons, chips)
    val LabelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    )

    val LabelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )

    val LabelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
}

// ===== SPACING TOKENS =====

/**
 * Spacing scale for consistent layout
 */
object SpacingTokens {
    val None: Dp = 0.dp
    val ExtraSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 16.dp
    val Large: Dp = 24.dp
    val ExtraLarge: Dp = 32.dp
    val ExtraExtraLarge: Dp = 48.dp
    val Huge: Dp = 64.dp
}

// ===== SHAPE TOKENS =====

/**
 * Corner radius values for consistent shapes
 */
object ShapeTokens {
    val None: Dp = 0.dp
    val ExtraSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val ExtraLarge: Dp = 28.dp
    val Full: Dp = 9999.dp  // Fully rounded
}

// ===== ELEVATION TOKENS =====

/**
 * Elevation values for shadows and depth
 */
object ElevationTokens {
    val Level0: Dp = 0.dp
    val Level1: Dp = 1.dp
    val Level2: Dp = 3.dp
    val Level3: Dp = 6.dp
    val Level4: Dp = 8.dp
    val Level5: Dp = 12.dp
}

// ===== SIZE TOKENS =====

/**
 * Common size values for UI components
 */
object SizeTokens {
    // Icon sizes
    val IconSmall: Dp = 16.dp
    val IconMedium: Dp = 24.dp
    val IconLarge: Dp = 32.dp
    val IconExtraLarge: Dp = 48.dp

    // Button sizes
    val ButtonHeightSmall: Dp = 32.dp
    val ButtonHeightMedium: Dp = 40.dp
    val ButtonHeightLarge: Dp = 48.dp
    val ButtonHeightExtraLarge: Dp = 56.dp

    // Touch target (accessibility)
    val MinTouchTarget: Dp = 48.dp

    // TextField sizes
    val TextFieldHeight: Dp = 56.dp
    val TextFieldHeightSmall: Dp = 40.dp
}

// ===== ANIMATION TOKENS =====

/**
 * Animation duration and easing values
 */
object AnimationTokens {
    const val DurationShort: Int = 150
    const val DurationMedium: Int = 300
    const val DurationLong: Int = 500
    const val DurationExtraLong: Int = 1000
}

// ===== RESPONSIVE TOKENS =====

/**
 * Responsive breakpoint values and device-specific tokens
 * Based on Material Design 3 breakpoint system
 */
object ResponsiveTokens {
    // Breakpoint thresholds (in DP)
    val BreakpointXS: Dp = 0.dp      // <600dp width (phones in portrait)
    val BreakpointSM: Dp = 600.dp    // 600-839dp width (tablets in portrait, phones in landscape)
    val BreakpointMD: Dp = 840.dp    // 840-1239dp width (tablets in landscape, small desktops)
    val BreakpointLG: Dp = 1240.dp   // 1240-1439dp width (desktops)
    val BreakpointXL: Dp = 1440.dp   // ≥1440dp width (large desktops)

    // Device-specific spacing
    object SpacingByDevice {
        // Phone spacing
        val PhoneCompact: Dp = 8.dp
        val PhoneMedium: Dp = 16.dp
        val PhoneLarge: Dp = 24.dp

        // Tablet spacing
        val TabletCompact: Dp = 16.dp
        val TabletMedium: Dp = 24.dp
        val TabletLarge: Dp = 32.dp

        // Desktop spacing
        val DesktopCompact: Dp = 24.dp
        val DesktopMedium: Dp = 32.dp
        val DesktopLarge: Dp = 48.dp
    }

    // Content max widths by breakpoint
    object MaxContentWidth {
        val XS: Dp = 360.dp   // Phone portrait
        val SM: Dp = 600.dp   // Phone landscape / small tablet
        val MD: Dp = 840.dp   // Tablet
        val LG: Dp = 1240.dp  // Desktop
        val XL: Dp = 1440.dp  // Large desktop
    }

    // Grid columns by breakpoint
    object GridColumns {
        const val XS: Int = 4    // Phone
        const val SM: Int = 8    // Tablet portrait
        const val MD: Int = 12   // Tablet landscape
        const val LG: Int = 12   // Desktop
        const val XL: Int = 12   // Large desktop
    }

    // Margin/Gutter sizes by breakpoint
    object MarginsByBreakpoint {
        val XS: Dp = 16.dp
        val SM: Dp = 24.dp
        val MD: Dp = 24.dp
        val LG: Dp = 32.dp
        val XL: Dp = 32.dp
    }

    object GuttersByBreakpoint {
        val XS: Dp = 16.dp
        val SM: Dp = 24.dp
        val MD: Dp = 24.dp
        val LG: Dp = 24.dp
        val XL: Dp = 24.dp
    }
}
