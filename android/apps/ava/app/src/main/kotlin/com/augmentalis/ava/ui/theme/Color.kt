// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/theme/Color.kt
// created: 2025-11-04 14:10:00 -0800
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * AVA Theme Colors - Centralized Color System
 *
 * Based on AVAConnect glassmorphic design:
 * - Purple gradient background (#667eea → #764ba2)
 * - Teal accent (#03DAC6)
 * - Semi-transparent glass panels
 * - Dark overlays for depth
 *
 * To change the entire app's color scheme, simply modify the colors in this file.
 */
object AvaColors {

    // ==================== PRIMARY BRAND COLORS ====================

    /**
     * Primary gradient colors - Purple spectrum
     * Used for backgrounds, brand elements, and primary surfaces
     */
    val GradientStart = Color(0xFF667EEA)      // Light purple-blue
    val GradientEnd = Color(0xFF764BA2)        // Deep purple
    val GradientMid = Color(0xFF6E69C7)        // Interpolated middle

    /**
     * Accent color - Teal
     * Used for primary actions, buttons, links, and interactive elements
     */
    val AccentPrimary = Color(0xFF03DAC6)      // Bright teal
    val AccentDark = Color(0xFF018786)         // Darker teal for pressed states
    val AccentLight = Color(0xFF64FFDA)        // Lighter teal for highlights

    // ==================== GLASS MORPHISM ====================

    /**
     * Semi-transparent glass panel colors
     * Apply these with backdrop blur for glassmorphic effect
     */
    val GlassUltraLight = Color(0x0DFFFFFF)    // 5% white - Subtle overlay
    val GlassLight = Color(0x1AFFFFFF)         // 10% white - Light panels
    val GlassMedium = Color(0x26FFFFFF)        // 15% white - Standard panels
    val GlassHeavy = Color(0x33FFFFFF)         // 20% white - Emphasized panels
    val GlassDense = Color(0x4DFFFFFF)         // 30% white - Dense overlays

    /**
     * Dark glass variants for dark mode
     */
    val GlassDarkLight = Color(0x1A000000)     // 10% black
    val GlassDarkMedium = Color(0x33000000)    // 20% black
    val GlassDarkHeavy = Color(0x4D000000)     // 30% black

    // ==================== TEXT COLORS ====================

    /**
     * Text colors with various opacity levels
     */
    val TextPrimary = Color(0xFFFFFFFF)        // 100% white - Primary text
    val TextSecondary = Color(0xE6FFFFFF)      // 90% white - Secondary text
    val TextTertiary = Color(0xB3FFFFFF)       // 70% white - Tertiary text
    val TextDisabled = Color(0x61FFFFFF)       // 38% white - Disabled text
    val TextHint = Color(0x99FFFFFF)           // 60% white - Placeholder text

    /**
     * Text colors for dark backgrounds
     */
    val TextOnAccent = Color(0xFF000000)       // Black text on teal
    val TextOnLight = Color(0xFF1A1A1A)        // Dark gray text on light backgrounds

    // ==================== SEMANTIC COLORS ====================

    /**
     * Status and semantic colors
     */
    val Success = Color(0xFF10B981)            // Green - Success states
    val Warning = Color(0xFFF59E0B)            // Amber - Warning states
    val Error = Color(0xFFEF4444)              // Red - Error states
    val Info = Color(0xFF3B82F6)               // Blue - Info states

    /**
     * Success variants
     */
    val SuccessLight = Color(0xFF34D399)
    val SuccessDark = Color(0xFF059669)

    /**
     * Warning variants
     */
    val WarningLight = Color(0xFFFBBF24)
    val WarningDark = Color(0xFFD97706)

    /**
     * Error variants
     */
    val ErrorLight = Color(0xFFF87171)
    val ErrorDark = Color(0xFFDC2626)

    // ==================== UI ELEMENT COLORS ====================

    /**
     * Borders and dividers
     */
    val BorderLight = Color(0x1AFFFFFF)        // 10% white
    val BorderMedium = Color(0x33FFFFFF)       // 20% white
    val BorderHeavy = Color(0x4DFFFFFF)        // 30% white
    val Divider = Color(0x1AFFFFFF)            // 10% white

    /**
     * Overlays and shadows
     */
    val OverlayLight = Color(0x0D000000)       // 5% black
    val OverlayMedium = Color(0x33000000)      // 20% black
    val OverlayHeavy = Color(0x80000000)       // 50% black
    val ShadowColor = Color(0x40000000)        // 25% black

    /**
     * Shimmer and loading states
     */
    val ShimmerBase = Color(0x1AFFFFFF)        // 10% white
    val ShimmerHighlight = Color(0x33FFFFFF)   // 20% white

    // ==================== SPECIAL COLORS ====================

    /**
     * Special purpose colors
     */
    val Transparent = Color.Transparent
    val PureWhite = Color(0xFFFFFFFF)
    val PureBlack = Color(0xFF000000)
    val NavigationBackground = Color(0x26000000)  // 15% black for bottom nav

    /**
     * Scrim for modals and dialogs
     */
    val ScrimLight = Color(0x52000000)         // 32% black
    val ScrimMedium = Color(0x99000000)        // 60% black
    val ScrimHeavy = Color(0xCC000000)         // 80% black
}
