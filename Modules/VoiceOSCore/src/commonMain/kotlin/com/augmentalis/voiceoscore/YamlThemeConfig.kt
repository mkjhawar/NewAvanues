/**
 * YamlThemeConfig.kt - Data classes for YAML theme structure
 *
 * Defines the complete data model for YAML theme files.
 * Uses kotlinx.serialization annotations for potential future use with kaml.
 * Currently works with manual YAML parsing for KMP compatibility.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscore

import kotlinx.serialization.Serializable

/**
 * Root configuration parsed from YAML theme file
 */
@Serializable
data class YamlThemeConfig(
    val theme: ThemeMetadata = ThemeMetadata(),
    val colors: ColorConfig = ColorConfig(),
    val typography: TypographyConfig = TypographyConfig(),
    val spacing: SpacingConfig = SpacingConfig(),
    val shapes: ShapeConfig = ShapeConfig(),
    val elevation: ElevationConfig = ElevationConfig(),
    val borders: BorderConfig = BorderConfig(),
    val sizes: SizeConfig = SizeConfig(),
    val animations: AnimationConfig = AnimationConfig(),
    val accessibility: ThemeAccessibilityConfig = ThemeAccessibilityConfig(),
    val opacity: OpacityConfig = OpacityConfig(),
    val text: TextConfig = TextConfig(),
    val variants: Map<String, YamlThemeVariant> = emptyMap()
)

/**
 * Theme metadata section
 */
@Serializable
data class ThemeMetadata(
    val name: String = "VoiceOSCoreNGTheme",
    val version: String = "1.0.0",
    val inherit: String? = null,
    val platform: String = "all"
)

/**
 * Color definitions - all values in ARGB hex format "0xAARRGGBB"
 */
@Serializable
data class ColorConfig(
    // Primary brand colors
    val primary: String = "0xFF2196F3",
    val primaryVariant: String = "0xFF1976D2",
    val onPrimary: String = "0xFFFFFFFF",

    // Background colors
    val background: String = "0xEE1E1E1E",
    val backgroundSolid: String = "0xFF121212",
    val backdrop: String = "0x4D000000",
    val surface: String = "0xEE1E1E1E",
    val surfaceVariant: String = "0xFF2C2C2C",

    // Text colors
    val textPrimary: String = "0xFFFFFFFF",
    val textSecondary: String = "0xB3FFFFFF",
    val textDisabled: String = "0xFF808080",
    val textHint: String = "0x99FFFFFF",

    // Border/divider colors
    val border: String = "0xFFFFFFFF",
    val divider: String = "0x1AFFFFFF",

    // Badge state colors
    val badgeEnabledWithName: String = "0xFF4CAF50",
    val badgeEnabledNoName: String = "0xFFFF9800",
    val badgeDisabled: String = "0xFF9E9E9E",

    // Status/feedback colors
    val statusListening: String = "0xFF2196F3",
    val statusProcessing: String = "0xFFFF9800",
    val statusSuccess: String = "0xFF4CAF50",
    val statusError: String = "0xFFF44336",

    // Confidence level colors
    val confidenceHigh: String = "0xFF4CAF50",
    val confidenceMedium: String = "0xFFFFEB3B",
    val confidenceLow: String = "0xFFFF9800",
    val confidenceReject: String = "0xFFF44336",

    // Card/tooltip backgrounds
    val cardBackground: String = "0xEE1E1E1E",
    val tooltipBackground: String = "0xEE000000",

    // Focus indicator
    val focusIndicator: String = "0xFF2196F3"
)

/**
 * Typography configuration - sizes in sp
 */
@Serializable
data class TypographyConfig(
    val titleSize: Int = 16,
    val bodySize: Int = 14,
    val captionSize: Int = 12,
    val smallSize: Int = 11,
    val badgeSize: Int = 14,
    val instructionSize: Int = 16,
    val fontWeightRegular: Int = 400,
    val fontWeightMedium: Int = 500,
    val fontWeightBold: Int = 700
)

/**
 * Spacing configuration - values in dp
 */
@Serializable
data class SpacingConfig(
    // Padding values
    val paddingSmall: Int = 4,
    val paddingMedium: Int = 8,
    val paddingLarge: Int = 16,
    val paddingXLarge: Int = 24,

    // Component spacing
    val spacingTiny: Int = 4,
    val spacingSmall: Int = 8,
    val spacingMedium: Int = 12,
    val spacingLarge: Int = 16,

    // Badge/element offsets
    val badgeOffsetX: Int = 4,
    val badgeOffsetY: Int = 4,
    val tooltipOffsetY: Int = 40
)

/**
 * Shape configuration - radii in dp
 */
@Serializable
data class ShapeConfig(
    val cornerRadiusSmall: Int = 6,
    val cornerRadiusMedium: Int = 8,
    val cornerRadiusLarge: Int = 12,
    val cornerRadiusXLarge: Int = 24,
    val cornerRadiusCircle: Int = 9999
)

/**
 * Elevation configuration - values in dp
 */
@Serializable
data class ElevationConfig(
    val low: Int = 4,
    val medium: Int = 8,
    val high: Int = 16
)

/**
 * Border configuration - widths in dp
 */
@Serializable
data class BorderConfig(
    val thin: Int = 1,
    val medium: Int = 2,
    val thick: Int = 3
)

/**
 * Size configuration - values in dp
 */
@Serializable
data class SizeConfig(
    // Badge dimensions
    val badgeSize: Int = 32,
    val badgeNumberSize: Int = 28,
    val badgeRadius: Int = 16,

    // Icon sizes
    val iconSmall: Int = 16,
    val iconMedium: Int = 24,
    val iconLarge: Int = 32,

    // Menu dimensions
    val menuMinWidth: Int = 200,
    val menuMaxWidth: Int = 280,
    val tooltipMaxWidth: Int = 200
)

/**
 * Animation configuration - durations in ms
 */
@Serializable
data class AnimationConfig(
    val durationFast: Int = 150,
    val durationNormal: Int = 200,
    val durationSlow: Int = 300,
    val enabled: Boolean = true,
    val easingDefault: String = "easeInOut",
    val easingEnter: String = "easeOut",
    val easingExit: String = "easeIn"
)

/**
 * Theme-level accessibility configuration
 * Note: Distinct from ComponentDefinition.AccessibilityConfig which handles component-level accessibility
 */
@Serializable
data class ThemeAccessibilityConfig(
    val minimumContrastRatio: Float = 4.5f,
    val focusIndicatorWidth: Int = 3,
    val minimumTouchTarget: Int = 48,
    val confidenceHighThreshold: Float = 0.8f,
    val confidenceMediumThreshold: Float = 0.6f,
    val confidenceLowThreshold: Float = 0.4f
)

/**
 * Opacity configuration - values from 0.0 to 1.0
 */
@Serializable
data class OpacityConfig(
    val disabled: Float = 0.5f,
    val secondary: Float = 0.7f,
    val hint: Float = 0.6f,
    val divider: Float = 0.1f,
    val backdrop: Float = 0.3f,
    val tooltip: Float = 0.9333f
)

/**
 * Text defaults configuration
 */
@Serializable
data class TextConfig(
    val instructionDefault: String = "Say a number to select",
    val instructionMultiple: String = "Say number or command name",
    val listeningPrompt: String = "Listening...",
    val processingPrompt: String = "Processing...",
    val successPrompt: String = "Done!",
    val errorPrompt: String = "Error occurred"
)

/**
 * Theme variant - allows partial overrides of parent theme
 */
@Serializable
data class YamlThemeVariant(
    val inherit: String? = null,
    val colors: Map<String, String> = emptyMap(),
    val typography: Map<String, Int> = emptyMap(),
    val spacing: Map<String, Int> = emptyMap(),
    val shapes: Map<String, Int> = emptyMap(),
    val elevation: Map<String, Int> = emptyMap(),
    val borders: Map<String, Int> = emptyMap(),
    val sizes: Map<String, Int> = emptyMap(),
    val animations: Map<String, String> = emptyMap(),
    val accessibility: Map<String, String> = emptyMap()
)
