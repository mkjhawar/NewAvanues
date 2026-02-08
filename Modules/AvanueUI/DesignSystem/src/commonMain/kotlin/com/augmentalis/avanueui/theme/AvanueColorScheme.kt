package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

/**
 * Theme-variable color scheme interface.
 * All color roles that change between themes (Ocean, Sunset, etc.).
 */
interface AvanueColorScheme {
    // Primary
    val primary: Color
    val onPrimary: Color
    val primaryDark: Color
    val primaryLight: Color

    // Secondary
    val secondary: Color
    val onSecondary: Color

    // Tertiary
    val tertiary: Color

    // Error
    val error: Color
    val onError: Color

    // Background & Surface
    val background: Color
    val surface: Color
    val surfaceElevated: Color
    val surfaceVariant: Color
    val surfaceInput: Color

    // Text
    val textPrimary: Color
    val textSecondary: Color
    val textTertiary: Color
    val textDisabled: Color
    val textOnPrimary: Color

    // Icon
    val iconPrimary: Color
    val iconSecondary: Color
    val iconDisabled: Color

    // Border
    val border: Color
    val borderSubtle: Color
    val borderStrong: Color

    // Semantic
    val success: Color
    val warning: Color
    val info: Color

    // Special
    val starActive: Color

    fun resolve(id: String): Color? = when (id) {
        "color.primary" -> primary
        "color.onPrimary" -> onPrimary
        "color.primaryDark" -> primaryDark
        "color.primaryLight" -> primaryLight
        "color.secondary" -> secondary
        "color.onSecondary" -> onSecondary
        "color.tertiary" -> tertiary
        "color.error" -> error
        "color.onError" -> onError
        "color.background" -> background
        "color.surface" -> surface
        "color.surfaceElevated" -> surfaceElevated
        "color.surfaceVariant" -> surfaceVariant
        "color.surfaceInput" -> surfaceInput
        "color.textPrimary" -> textPrimary
        "color.textSecondary" -> textSecondary
        "color.textTertiary" -> textTertiary
        "color.textDisabled" -> textDisabled
        "color.textOnPrimary" -> textOnPrimary
        "color.iconPrimary" -> iconPrimary
        "color.iconSecondary" -> iconSecondary
        "color.iconDisabled" -> iconDisabled
        "color.border" -> border
        "color.borderSubtle" -> borderSubtle
        "color.borderStrong" -> borderStrong
        "color.success" -> success
        "color.warning" -> warning
        "color.info" -> info
        "color.starActive" -> starActive
        else -> null
    }
}
