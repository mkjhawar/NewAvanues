package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

/**
 * Liquid Glass theme color implementation.
 *
 * Inspired by Apple Vision Pro / visionOS liquid glass aesthetic:
 * - True black background for maximum contrast and OLED efficiency
 * - Cyan-electric primary for futuristic shimmer
 * - Apple system colors for semantic states
 * - Subtle white borders at low opacity for glass edges
 *
 * References: WWDC 2025 Sessions 219/356, Apple HIG visionOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
object LiquidColors : AvanueColorScheme {
    // Primary - Cyan Electric (liquid shimmer)
    override val primary = Color(0xFF00D4FF)
    override val onPrimary = Color.Black
    override val primaryDark = Color(0xFF00A5CC)
    override val primaryLight = Color(0xFF66E5FF)

    // Secondary - Soft Violet
    override val secondary = Color(0xFFA78BFA)
    override val onSecondary = Color.Black

    // Tertiary - Emerald
    override val tertiary = Color(0xFF34D399)

    // Containers - tinted backgrounds for cards/chips (visionOS-inspired)
    override val primaryContainer = Color(0xFF003340)    // Deep cyan (primary@20% on black)
    override val onPrimaryContainer = Color(0xFF99EEFF)  // Light cyan text on container
    override val secondaryContainer = Color(0xFF2E2150)  // Deep violet
    override val onSecondaryContainer = Color(0xFFD4BBFF) // Light violet text on container
    override val tertiaryContainer = Color(0xFF0D3326)   // Deep emerald (tertiary@20% on black)
    override val onTertiaryContainer = Color(0xFF99F6CC) // Light emerald text on container
    override val errorContainer = Color(0xFF3C1111)      // Deep red (Apple system red@20%)
    override val onErrorContainer = Color(0xFFFFB3AD)    // Light red text on container

    // Error - Apple System Red
    override val error = Color(0xFFFF453A)
    override val onError = Color.White

    // Background & Surface - True black + Apple system grays
    override val background = Color(0xFF000000)        // True black (visionOS)
    override val surface = Color(0xFF1C1C1E)           // Apple system gray 6
    override val surfaceElevated = Color(0xFF2C2C2E)   // Apple system gray 5
    override val surfaceVariant = Color(0xFF3A3A3C)    // Apple system gray 4
    override val surfaceInput = Color(0xFF2C2C2E)      // Input fields

    // Text - Apple label hierarchy
    override val textPrimary = Color(0xFFF5F5F7)       // Apple white
    override val textSecondary = Color(0xFFA1A1A6)     // Apple secondary label
    override val textTertiary = Color(0xFF636366)      // Apple tertiary label
    override val textDisabled = Color(0xFF48484A)      // Apple quaternary label
    override val textOnPrimary = Color.Black

    // Icon
    override val iconPrimary = Color(0xFF00D4FF)       // Cyan electric
    override val iconSecondary = Color(0xFFA1A1A6)     // Secondary label
    override val iconDisabled = Color(0xFF48484A)      // Quaternary label

    // Border - Subtle white glass edges
    override val border = Color(0x1FFFFFFF)            // White @ 12%
    override val borderSubtle = Color(0x0FFFFFFF)      // White @ 6%
    override val borderStrong = Color(0x33FFFFFF)      // White @ 20%

    // Semantic - Apple system colors
    override val success = Color(0xFF30D158)           // Apple system green
    override val warning = Color(0xFFFFD60A)           // Apple system yellow
    override val info = Color(0xFF00D4FF)              // Matches primary

    // Special
    override val starActive = Color(0xFFFFD60A)        // Apple system yellow
}
