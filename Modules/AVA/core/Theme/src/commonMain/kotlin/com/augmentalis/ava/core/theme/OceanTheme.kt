package com.augmentalis.ava.core.theme

import androidx.compose.ui.graphics.Color

/**
 * OceanTheme - Central color definitions for Ocean Blue Glassmorphism theme.
 *
 * Canonical color tokens shared across VoiceOSCore, WebAvanue, and all apps.
 * All components should reference these colors instead of hardcoding values.
 *
 * Color Hierarchy:
 * - Background: Deep slate (#0F172A)
 * - Surface: Slate (#1E293B)
 * - Surface Elevated: Lighter slate (#334155)
 * - Primary: CoralBlue (#3B82F6)
 * - Text: White with varying opacity
 */
object OceanTheme {

    // ========== Background Colors ==========
    val background = Color(0xFF0F172A)
    val surface = Color(0xFF1E293B)
    val surfaceElevated = Color(0xFF334155)
    val surfaceInput = Color(0xFF334155)

    // ========== Primary Colors (CoralBlue) ==========
    val primary = Color(0xFF3B82F6)
    val primaryDark = Color(0xFF2563EB)
    val primaryLight = Color(0xFF60A5FA)

    // ========== Text Colors ==========
    val textPrimary = Color(0xFFE2E8F0)
    val textSecondary = Color(0xFFCBD5E1)
    val textTertiary = Color(0xFF94A3B8)
    val textDisabled = Color(0xFF64748B)
    val textOnPrimary = Color.White

    // ========== Border Colors ==========
    val border = Color(0x33FFFFFF)
    val borderSubtle = Color(0x1AFFFFFF)
    val borderStrong = Color(0x4DFFFFFF)
    val borderFocused = primary

    // ========== State Colors ==========
    val success = Color(0xFF10B981)
    val warning = Color(0xFFF59E0B)
    val error = Color(0xFFEF4444)
    val info = Color(0xFF0EA5E9)

    // ========== Icon Colors ==========
    val iconActive = textPrimary
    val iconInactive = textTertiary
    val iconOnPrimary = Color.White

    // ========== Special Colors ==========
    val starActive = Color(0xFFFFC107)
    val voiceListening = primary
    val loading = primary

    // ========== Glassmorphism Overlays ==========
    val glassLight = Color(0x141E293B)
    val glassMedium = Color(0x1F334155)
    val glassHeavy = Color(0x33334155)
    val glassBorder = Color(0x262563EB)
}
