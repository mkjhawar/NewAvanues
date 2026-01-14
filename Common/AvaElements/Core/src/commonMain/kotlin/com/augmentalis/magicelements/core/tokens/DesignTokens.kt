package com.augmentalis.avaelements.core.tokens

import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

/**
 * Design Tokens - Atomic Design Values
 *
 * The foundational values that define your design system.
 * These are platform-agnostic and map to native equivalents.
 *
 * Think of tokens as the "periodic table" of your design:
 * - Instead of random colors, you have a color system
 * - Instead of arbitrary spacing, you have a spacing scale
 * - Instead of inconsistent sizes, you have a size scale
 *
 * Benefits:
 * - Consistency across iOS/Android/Web/XR
 * - Easy theme switching
 * - Accessible color combinations
 * - Professional design system
 */
data class DesignTokens(
    /** Color system */
    val color: ColorTokens,

    /** Spacing/padding/margin scale */
    val spacing: SpacingTokens,

    /** Typography system */
    val typography: TypographyTokens,

    /** Border radius scale */
    val radius: RadiusTokens,

    /** Shadow/elevation system */
    val elevation: ElevationTokens,

    /** Motion/animation timings */
    val motion: MotionTokens,

    /** Breakpoints for responsive design */
    val breakpoints: BreakpointTokens,

    /** Z-index layering */
    val zIndex: ZIndexTokens
)

// ============================================
// COLOR TOKENS
// ============================================

/**
 * Color token system
 *
 * Organized by role (primary, secondary, error) not arbitrary names.
 * Each role has a full scale (50-900) plus semantic shortcuts.
 */
data class ColorTokens(
    /** Primary brand color */
    val primary: ColorScale,

    /** Secondary/accent color */
    val secondary: ColorScale,

    /** Tertiary color (optional) */
    val tertiary: ColorScale? = null,

    /** Error/danger color */
    val error: ColorScale,

    /** Warning color */
    val warning: ColorScale,

    /** Info color */
    val info: ColorScale,

    /** Success color */
    val success: ColorScale,

    /** Neutral/gray scale */
    val neutral: ColorScale,

    /** Surface colors */
    val surface: SurfaceColors,

    /** Text colors */
    val text: TextColors,

    /** Border colors */
    val border: BorderColors
)

/**
 * Color scale (50-900)
 *
 * Like Material Design color palettes.
 * 50 = lightest, 900 = darkest
 */
data class ColorScale(
    val shade50: Color,
    val shade100: Color,
    val shade200: Color,
    val shade300: Color,
    val shade400: Color,
    val shade500: Color,  // Base/main
    val shade600: Color,
    val shade700: Color,
    val shade800: Color,
    val shade900: Color,

    // Semantic shortcuts
    val main: Color = shade500,
    val light: Color = shade300,
    val dark: Color = shade700,
    val contrastText: Color? = null  // Text color on this background
)

/**
 * Surface colors (backgrounds)
 */
data class SurfaceColors(
    val background: Color,       // Main app background
    val surface: Color,           // Cards, sheets
    val surfaceVariant: Color,    // Alternate surface
    val surfaceTint: Color? = null,
    val inverseSurface: Color? = null
)

/**
 * Text colors
 */
data class TextColors(
    val primary: Color,           // Main text
    val secondary: Color,         // Secondary text (60% opacity)
    val disabled: Color,          // Disabled text (38% opacity)
    val hint: Color? = null,      // Placeholder/hint text
    val inverse: Color? = null    // Text on inverse surface
)

/**
 * Border colors
 */
data class BorderColors(
    val default: Color,
    val subtle: Color,
    val focus: Color,
    val error: Color? = null
)

// ============================================
// SPACING TOKENS
// ============================================

/**
 * Spacing scale
 *
 * Use multiples of a base unit (4px or 8px).
 * Material Design uses 8px, iOS uses 8px, most systems use 4/8px.
 */
data class SpacingTokens(
    val unit: Float = 8f,  // Base unit (8px)

    // Named scales
    val none: Float = 0f,
    val xs: Float = unit * 0.5f,   // 4px
    val sm: Float = unit,          // 8px
    val md: Float = unit * 2,      // 16px
    val lg: Float = unit * 3,      // 24px
    val xl: Float = unit * 4,      // 32px
    val xxl: Float = unit * 6,     // 48px
    val xxxl: Float = unit * 8,    // 64px

    // Semantic shortcuts
    val paddingSmall: Float = sm,
    val paddingMedium: Float = md,
    val paddingLarge: Float = lg,
    val gapSmall: Float = sm,
    val gapMedium: Float = md,
    val gapLarge: Float = lg
) {
    /** Get spacing by multiplier */
    fun spacing(multiplier: Float): Float = unit * multiplier

    /** Get spacing by multiplier (integer) */
    fun spacing(multiplier: Int): Float = unit * multiplier
}

// ============================================
// TYPOGRAPHY TOKENS
// ============================================

/**
 * Typography scale
 *
 * Organized by purpose, not arbitrary names.
 * Based on Material Design type scale.
 */
data class TypographyTokens(
    /** Display styles (very large) */
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,

    /** Headline styles */
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,

    /** Title styles */
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,

    /** Body text */
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,

    /** Labels (buttons, etc) */
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
)

/**
 * Text style definition
 */
data class TextStyle(
    val fontSize: Float,      // In SP (Android) / Points (iOS) / px (Web)
    val lineHeight: Float,    // Line height
    val fontWeight: FontWeight,
    val letterSpacing: Float = 0f,
    val fontFamily: String? = null
)

enum class FontWeight(val value: Int) {
    THIN(100),
    EXTRA_LIGHT(200),
    LIGHT(300),
    NORMAL(400),
    MEDIUM(500),
    SEMI_BOLD(600),
    BOLD(700),
    EXTRA_BOLD(800),
    BLACK(900)
}

// ============================================
// RADIUS TOKENS
// ============================================

/**
 * Border radius scale
 */
data class RadiusTokens(
    val none: Float = 0f,
    val xs: Float = 2f,
    val sm: Float = 4f,
    val md: Float = 8f,
    val lg: Float = 12f,
    val xl: Float = 16f,
    val xxl: Float = 24f,
    val full: Float = 9999f,  // Fully rounded (pill shape)

    // Semantic shortcuts
    val button: Float = md,
    val card: Float = lg,
    val dialog: Float = xl,
    val input: Float = sm
)

// ============================================
// ELEVATION TOKENS
// ============================================

/**
 * Elevation/shadow system
 *
 * Maps to:
 * - Android: elevation in dp
 * - iOS: shadow properties
 * - Web: box-shadow
 * - XR: depth layers
 */
data class ElevationTokens(
    val level0: Elevation,   // No elevation (flat)
    val level1: Elevation,   // Raised slightly
    val level2: Elevation,
    val level3: Elevation,
    val level4: Elevation,   // Modal/dialog
    val level5: Elevation    // Highest (dropdown, tooltip)
)

data class Elevation(
    val level: Int,
    val shadowColor: Color = Color(0, 0, 0, 0.2f),
    val offsetX: Float = 0f,
    val offsetY: Float,
    val blurRadius: Float,
    val spreadRadius: Float = 0f
)

// ============================================
// MOTION TOKENS
// ============================================

/**
 * Animation/transition timings
 *
 * Material Design uses:
 * - Fast: 100ms
 * - Normal: 300ms
 * - Slow: 500ms
 */
data class MotionTokens(
    val durationFast: Long = 100,      // Quick transitions
    val durationNormal: Long = 300,    // Standard transitions
    val durationSlow: Long = 500,      // Deliberate transitions
    val durationPageTransition: Long = 400,

    val easingStandard: String = "cubic-bezier(0.4, 0.0, 0.2, 1)",
    val easingDecelerate: String = "cubic-bezier(0.0, 0.0, 0.2, 1)",
    val easingAccelerate: String = "cubic-bezier(0.4, 0.0, 1, 1)",
    val easingSharp: String = "cubic-bezier(0.4, 0.0, 0.6, 1)"
)

// ============================================
// BREAKPOINT TOKENS
// ============================================

/**
 * Responsive breakpoints
 *
 * For responsive layouts across devices.
 */
data class BreakpointTokens(
    val xs: Int = 0,      // Phone (portrait)
    val sm: Int = 600,    // Phone (landscape), small tablet
    val md: Int = 960,    // Tablet
    val lg: Int = 1280,   // Desktop
    val xl: Int = 1920    // Large desktop
)

// ============================================
// Z-INDEX TOKENS
// ============================================

/**
 * Z-index layering
 *
 * Prevents z-index wars with consistent layering.
 */
data class ZIndexTokens(
    val base: Int = 0,
    val dropdown: Int = 1000,
    val sticky: Int = 1100,
    val modal: Int = 1300,
    val popover: Int = 1400,
    val toast: Int = 1500,
    val tooltip: Int = 1600
)
