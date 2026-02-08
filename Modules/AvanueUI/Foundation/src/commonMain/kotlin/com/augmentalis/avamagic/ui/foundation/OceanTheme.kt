/**
 * OceanTheme.kt - Central color definitions for Ocean Blue Glassmorphism theme
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * Updated: 2026-01-19 (Migrated to AvaUI/Foundation)
 *
 * Originally from: Avanues/Web/common/webavanue/universal
 *
 * UI Guidelines Reference: ld-ui-guidelines-v1.md
 *
 * This object provides consistent color tokens across all UI components.
 * All components should reference these colors instead of hardcoding values.
 *
 * Color Hierarchy:
 * - Background: Deep slate (#0F172A)
 * - Surface: Slate (#1E293B)
 * - Surface Elevated: Lighter slate (#334155)
 * - Primary: CoralBlue (#3B82F6)
 * - Text: White with varying opacity
 */
package com.augmentalis.avamagic.ui.foundation

import androidx.compose.ui.graphics.Color

object OceanTheme {

    // ========== Background Colors ==========
    /** Deep slate - main app background */
    val background = Color(0xFF0F172A)

    /** Slate surface - cards, bars, panels */
    val surface = Color(0xFF1E293B)

    /** Elevated surface - modals, dialogs, elevated cards */
    val surfaceElevated = Color(0xFF334155)

    /** Input background - text fields, search boxes */
    val surfaceInput = Color(0xFF334155)

    // ========== Primary Colors (CoralBlue) ==========
    /** Primary accent - buttons, links, active states */
    val primary = Color(0xFF3B82F6)

    /** Primary darker - pressed states */
    val primaryDark = Color(0xFF2563EB)

    /** Primary lighter - hover states */
    val primaryLight = Color(0xFF60A5FA)

    // ========== Text Colors ==========
    /** Primary text - headers, important content (White 90%) */
    val textPrimary = Color(0xFFE2E8F0)

    /** Secondary text - body content (White 80%) */
    val textSecondary = Color(0xFFCBD5E1)

    /** Tertiary text - hints, placeholders (White 60%) */
    val textTertiary = Color(0xFF94A3B8)

    /** Disabled text (White 40%) */
    val textDisabled = Color(0xFF64748B)

    /** On primary - text on primary colored buttons */
    val textOnPrimary = Color.White

    // ========== Border Colors ==========
    /** Default border (White 20%) */
    val border = Color(0x33FFFFFF)

    /** Subtle border (White 10%) */
    val borderSubtle = Color(0x1AFFFFFF)

    /** Strong border (White 30%) */
    val borderStrong = Color(0x4DFFFFFF)

    /** Focused border - input focus state */
    val borderFocused = primary

    // ========== State Colors ==========
    /** Success - SeafoamGreen */
    val success = Color(0xFF10B981)

    /** Warning - Amber */
    val warning = Color(0xFFF59E0B)

    /** Error - CoralRed */
    val error = Color(0xFFEF4444)

    /** Info - Sky blue */
    val info = Color(0xFF0EA5E9)

    // ========== Icon Colors ==========
    /** Active icon */
    val iconActive = textPrimary

    /** Inactive/disabled icon */
    val iconInactive = textTertiary

    /** Icon on colored background */
    val iconOnPrimary = Color.White

    // ========== Special Colors ==========
    /** Star/favorite active color (Gold) */
    val starActive = Color(0xFFFFC107)

    /** Voice listening indicator */
    val voiceListening = primary

    /** Loading/progress indicator */
    val loading = primary

    // ========== Glassmorphism Overlays (white-based for dark backgrounds) ==========
    /** Light glass (10% white) */
    val glassLight = Color(0x1AFFFFFF)

    /** Medium glass (15% white) */
    val glassMedium = Color(0x26FFFFFF)

    /** Heavy glass (22% white) */
    val glassHeavy = Color(0x38FFFFFF)

    /** Glass border */
    val glassBorder = Color(0x262563EB)
}
