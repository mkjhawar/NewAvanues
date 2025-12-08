// filename: Universal/AVA/Core/Theme/src/commonMain/kotlin/com/augmentalis/ava/core/theme/DesignTokens.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Universal Design Tokens for AVA Ecosystem
 *
 * Token-based design system combining:
 * - MagicUI's architecture (from VoiceAvanue)
 * - AVA's glassmorphic design language
 *
 * This module can be reused across all AVA projects (AVA, VoiceAvanue, AVAConnect)
 *
 * @author AVA AI Team
 * @version 1.0.0
 */

// ===== COLOR TOKENS =====

/**
 * Core color palette - AVA Ocean Blue Theme
 * Based on MagicUI Design System with deep ocean gradients
 * Reference: LD-magicui-design-system.md
 */
object ColorTokens {
    // ===== OCEAN THEME BASE PALETTE =====

    val DeepOcean = Color(0xFF0A1929)              // Darkest background
    val OceanDepth = Color(0xFF0F172A)             // Primary background
    val OceanMid = Color(0xFF1E293B)               // Mid-level surfaces
    val OceanShallow = Color(0xFF334155)           // Elevated surfaces

    // Gradient colors (Ocean Blue theme)
    val GradientStart = Color(0xFF0A1929)          // Deep ocean (top)
    val GradientMid = Color(0xFF0F172A)            // Ocean depth (middle)
    val GradientEnd = Color(0xFF1E293B)            // Ocean mid (bottom)

    // ===== PRIMARY BRAND COLORS =====

    // Primary accent - CoralBlue (per UI Guidelines)
    val Primary = Color(0xFF3B82F6)                // CoralBlue - Primary
    val OnPrimary = Color(0xFFFFFFFF)              // White text on blue
    val PrimaryContainer = Color(0xFF1E3A5F)       // Darker blue container
    val OnPrimaryContainer = Color(0xFFFFFFFF)     // White text

    // ===== SECONDARY COLORS =====

    val Secondary = Color(0xFF06B6D4)              // TurquoiseCyan
    val OnSecondary = Color(0xFF000000)
    val SecondaryContainer = Color(0x26FFFFFF)     // 15% white glass
    val OnSecondaryContainer = Color(0xFFFFFFFF)

    // ===== TERTIARY COLORS =====

    val Tertiary = Color(0xFF10B981)               // SeafoamGreen
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFF34D399)      // Lighter green
    val OnTertiaryContainer = Color(0xFF000000)

    // ===== ERROR COLORS =====

    val Error = Color(0xFFEF4444)                  // CoralRed
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFF87171)         // Light red
    val OnErrorContainer = Color(0xFF000000)

    // ===== BACKGROUND COLORS =====

    val Background = Color(0xFF0A1929)             // DeepOcean
    val OnBackground = Color(0xFFFFFFFF)

    // ===== SURFACE COLORS =====

    val Surface = Color(0x1AFFFFFF)                // 10% white glass
    val OnSurface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0x26FFFFFF)         // 15% white glass
    val OnSurfaceVariant = Color(0xE6FFFFFF)       // 90% white

    // ===== OUTLINE COLORS =====

    val Outline = Color(0x33FFFFFF)                // 20% white
    val OutlineVariant = Color(0x1AFFFFFF)         // 10% white

    // ===== GLASSMORPHISM COLORS =====

    val GlassUltraLight = Color(0x0DFFFFFF)        // 5% white
    val GlassLight = Color(0x1AFFFFFF)             // 10% white
    val GlassMedium = Color(0x26FFFFFF)            // 15% white
    val GlassHeavy = Color(0x33FFFFFF)             // 20% white
    val GlassDense = Color(0x4DFFFFFF)             // 30% white

    // ===== DARK THEME COLORS (Ocean Blue) =====

    val DarkPrimary = Color(0xFF60A5FA)            // Lighter CoralBlue
    val DarkOnPrimary = Color(0xFF000000)
    val DarkPrimaryContainer = Color(0xFF1E3A5F)   // Dark blue container
    val DarkOnPrimaryContainer = Color(0xFFFFFFFF)

    val DarkSecondary = Color(0xFF22D3EE)          // Lighter TurquoiseCyan
    val DarkOnSecondary = Color(0xFF000000)
    val DarkSecondaryContainer = Color(0x26FFFFFF) // 15% white glass
    val DarkOnSecondaryContainer = Color(0xFFFFFFFF)

    val DarkTertiary = Color(0xFF34D399)           // Lighter SeafoamGreen
    val DarkOnTertiary = Color(0xFF000000)
    val DarkTertiaryContainer = Color(0xFF10B981)
    val DarkOnTertiaryContainer = Color(0xFFFFFFFF)

    val DarkError = Color(0xFFF87171)              // Lighter CoralRed
    val DarkOnError = Color(0xFF000000)
    val DarkErrorContainer = Color(0xFFDC2626)
    val DarkOnErrorContainer = Color(0xFFFFFFFF)

    val DarkBackground = Color(0xFF0A1929)         // DeepOcean
    val DarkOnBackground = Color(0xFFFFFFFF)

    val DarkSurface = Color(0x1AFFFFFF)            // 10% white glass
    val DarkOnSurface = Color(0xFFFFFFFF)
    val DarkSurfaceVariant = Color(0x26FFFFFF)     // 15% white
    val DarkOnSurfaceVariant = Color(0xE6FFFFFF)   // 90% white

    val DarkOutline = Color(0x33FFFFFF)            // 20% white
    val DarkOutlineVariant = Color(0x1AFFFFFF)     // 10% white

    // ===== SEMANTIC COLORS =====

    val Success = Color(0xFF10B981)                // Green
    val SuccessLight = Color(0xFF34D399)
    val SuccessDark = Color(0xFF059669)

    val Warning = Color(0xFFF59E0B)                // Amber
    val WarningLight = Color(0xFFFBBF24)
    val WarningDark = Color(0xFFD97706)

    val Info = Color(0xFF3B82F6)                   // Blue
    val InfoLight = Color(0xFF60A5FA)
    val InfoDark = Color(0xFF2563EB)

    // ===== TEXT COLORS =====

    val TextPrimary = Color(0xFFFFFFFF)            // 100% white
    val TextSecondary = Color(0xE6FFFFFF)          // 90% white
    val TextTertiary = Color(0xB3FFFFFF)           // 70% white
    val TextDisabled = Color(0x61FFFFFF)           // 38% white
    val TextHint = Color(0x99FFFFFF)               // 60% white
    val TextOnAccent = Color(0xFF000000)           // Black on teal
    val TextOnLight = Color(0xFF1A1A1A)            // Dark on light

    // ===== SPECIAL COLORS =====

    val Transparent = Color.Transparent
    val PureWhite = Color(0xFFFFFFFF)
    val PureBlack = Color(0xFF000000)
    val NavigationBackground = Color(0x26000000)   // 15% black
    val ScrimLight = Color(0x52000000)             // 32% black
    val ScrimMedium = Color(0x99000000)            // 60% black
    val ScrimHeavy = Color(0xCC000000)             // 80% black
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

    // Body styles
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

    // Label styles
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
 * Spacing scale for consistent layout (8dp grid system)
 */
object SpacingTokens {
    val None: Dp = 0.dp
    val ExtraSmall: Dp = 4.dp          // 0.5x
    val Small: Dp = 8.dp               // 1x
    val Medium: Dp = 16.dp             // 2x (base unit)
    val Large: Dp = 24.dp              // 3x
    val ExtraLarge: Dp = 32.dp         // 4x
    val ExtraExtraLarge: Dp = 48.dp    // 6x
    val Huge: Dp = 64.dp               // 8x
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
    val ExtraLarge: Dp = 20.dp
    val ExtraExtraLarge: Dp = 24.dp
    val Full: Dp = 999.dp              // Fully rounded (pill)
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
 * Based on MagicUI Design System and AR/VR UI Guidelines
 */
object SizeTokens {
    // Icon sizes
    val IconSmall: Dp = 16.dp
    val IconMedium: Dp = 24.dp
    val IconLarge: Dp = 32.dp
    val IconExtraLarge: Dp = 48.dp

    // Button sizes (optimized for space efficiency)
    val ButtonHeightSmall: Dp = 32.dp
    val ButtonHeightMedium: Dp = 40.dp
    val ButtonHeightLarge: Dp = 48.dp
    val ButtonHeightExtraLarge: Dp = 56.dp
    val ButtonMinWidth: Dp = 88.dp

    // Touch target (accessibility - WCAG AA)
    val MinTouchTarget: Dp = 48.dp
    val MinTouchTargetSpatial: Dp = 60.dp    // For AR/VR (60pt)

    // TextField sizes
    val TextFieldHeight: Dp = 56.dp
    val TextFieldHeightSmall: Dp = 40.dp
    val TextFieldHeightCompact: Dp = 36.dp   // Compact mode

    // App bars
    val AppBarHeight: Dp = 56.dp
    val AppBarHeightCompact: Dp = 48.dp      // Compact mode
    val BottomNavHeight: Dp = 56.dp

    // Chat bubbles
    val ChatBubbleMaxWidth: Dp = 320.dp      // Increased for readability

    // ===== COMMAND BAR / DRAWER SIZES (MagicUI) =====

    // Command bar (floating toolbar)
    val CommandBarHeight: Dp = 64.dp         // Horizontal bar
    val CommandBarItemSize: Dp = 48.dp       // Each command item
    val CommandBarItemGap: Dp = 8.dp         // Gap between items
    val CommandBarIconSize: Dp = 24.dp       // Icon inside item
    val CommandBarCornerRadius: Dp = 32.dp   // Pill shape

    // Voice button (center of command bar)
    val VoiceButtonSize: Dp = 56.dp          // Primary voice FAB
    val VoiceButtonSizeCompact: Dp = 48.dp   // Compact mode

    // Drawer / Command palette items
    val DrawerItemHeight: Dp = 48.dp         // Not 72dp - more compact
    val DrawerItemIconSize: Dp = 24.dp       // Icons in drawer
    val DrawerItemPadding: Dp = 12.dp        // Padding around content
    val DrawerItemGap: Dp = 8.dp             // Gap between items

    // Grid layout for command drawer (5 items in row)
    val DrawerGridItemSize: Dp = 64.dp       // Grid item size
    val DrawerGridGap: Dp = 8.dp             // Gap in grid
}

/**
 * AR/VR Spatial UI sizes
 * Reference: LD-IDEACODE-UI-Guidelines-V2.md
 */
object SpatialSizeTokens {
    // Z-Axis distances (meters from user)
    const val HudDistance: Float = 0.5f           // HUD, alerts
    const val InteractiveDistance: Float = 1.0f  // Active window
    const val PrimaryDistance: Float = 1.5f      // Main content
    const val SecondaryDistance: Float = 2.0f    // Supporting
    const val AmbientDistance: Float = 3.0f      // Background

    // Touch targets for spatial UI
    val SpatialTouchTarget: Dp = 60.dp           // 60pt for spatial

    // Voice command limits
    const val MaxVoiceOptions: Int = 3           // Per prompt
    const val CommandHierarchyLevels: Int = 2    // Flat for voice
}

// ===== RESPONSIVE BREAKPOINTS =====

/**
 * Material 3 responsive breakpoints
 */
object BreakpointTokens {
    val CompactMaxWidth: Dp = 600.dp       // Phones (< 600dp)
    val MediumMinWidth: Dp = 600.dp        // Tablets (600dp - 840dp)
    val ExpandedMinWidth: Dp = 840.dp      // Desktops (> 840dp)
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
