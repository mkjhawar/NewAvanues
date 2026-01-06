/**
 * YamlThemeParserTest.kt - Tests for YAML theme parsing
 *
 * TDD tests for the YamlThemeParser covering:
 * - Valid YAML parsing
 * - Missing optional fields
 * - Theme variants
 * - Color parsing (hex string to Long)
 * - Default values
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscoreng.theme

import com.augmentalis.voiceoscoreng.features.OverlayTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YamlThemeParserTest {

    // ==================== Valid YAML Parsing Tests ====================

    @Test
    fun `parse valid YAML with colors returns Success`() {
        val yaml = """
            name: CustomTheme
            colors:
              primary: "#FF5722"
              background: "#1E1E1E"
              textPrimary: "#FFFFFF"
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(0xFFFF5722, result.theme.primaryColor)
        assertEquals(0xFF1E1E1E, result.theme.backgroundColor)
        assertEquals(0xFFFFFFFF, result.theme.textPrimaryColor)
    }

    @Test
    fun `parse valid YAML with typography returns Success`() {
        val yaml = """
            typography:
              titleSize: 20
              bodySize: 16
              captionSize: 12
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(20f, result.theme.titleFontSize)
        assertEquals(16f, result.theme.bodyFontSize)
        assertEquals(12f, result.theme.captionFontSize)
    }

    @Test
    fun `parse valid YAML with spacing returns Success`() {
        val yaml = """
            spacing:
              paddingSmall: 4
              paddingMedium: 8
              paddingLarge: 16
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(4f, result.theme.paddingSmall)
        assertEquals(8f, result.theme.paddingMedium)
        assertEquals(16f, result.theme.paddingLarge)
    }

    @Test
    fun `parse valid YAML with shapes returns Success`() {
        val yaml = """
            shapes:
              cornerRadiusSmall: 4
              cornerRadiusMedium: 8
              cornerRadiusLarge: 12
              badgeSize: 36
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(4f, result.theme.cornerRadiusSmall)
        assertEquals(8f, result.theme.cornerRadiusMedium)
        assertEquals(12f, result.theme.cornerRadiusLarge)
        assertEquals(36f, result.theme.badgeSize)
    }

    @Test
    fun `parse valid YAML with animation returns Success`() {
        val yaml = """
            animation:
              durationFast: 100
              durationNormal: 200
              durationSlow: 300
              enabled: true
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(100, result.theme.animationDurationFast)
        assertEquals(200, result.theme.animationDurationNormal)
        assertEquals(300, result.theme.animationDurationSlow)
        assertTrue(result.theme.animationEnabled)
    }

    @Test
    fun `parse valid YAML with accessibility returns Success`() {
        val yaml = """
            accessibility:
              minimumContrastRatio: 7.0
              focusIndicatorWidth: 4
              minimumTouchTargetSize: 48
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(7.0f, result.theme.minimumContrastRatio)
        assertEquals(4f, result.theme.focusIndicatorWidth)
        assertEquals(48f, result.theme.minimumTouchTargetSize)
    }

    @Test
    fun `parse complete YAML theme returns all values`() {
        val yaml = """
            name: CompleteTheme
            colors:
              primary: "#2196F3"
              background: "#000000"
              textPrimary: "#FFFFFF"
              statusSuccess: "#4CAF50"
              statusError: "#F44336"
            typography:
              titleSize: 18
              bodySize: 14
            spacing:
              paddingLarge: 24
            shapes:
              cornerRadiusLarge: 16
            animation:
              durationNormal: 250
              enabled: true
            accessibility:
              minimumContrastRatio: 4.5
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(0xFF2196F3, result.theme.primaryColor)
        assertEquals(0xFF000000, result.theme.backgroundColor)
        assertEquals(0xFFFFFFFF, result.theme.textPrimaryColor)
        assertEquals(0xFF4CAF50, result.theme.statusSuccessColor)
        assertEquals(0xFFF44336, result.theme.statusErrorColor)
        assertEquals(18f, result.theme.titleFontSize)
        assertEquals(14f, result.theme.bodyFontSize)
        assertEquals(24f, result.theme.paddingLarge)
        assertEquals(16f, result.theme.cornerRadiusLarge)
        assertEquals(250, result.theme.animationDurationNormal)
        assertTrue(result.theme.animationEnabled)
        assertEquals(4.5f, result.theme.minimumContrastRatio)
    }

    // ==================== Missing Optional Fields Tests ====================

    @Test
    fun `parse YAML with missing colors uses base theme defaults`() {
        val yaml = """
            typography:
              titleSize: 20
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        // Should use default colors
        assertEquals(OverlayTheme.DEFAULT.primaryColor, result.theme.primaryColor)
        assertEquals(OverlayTheme.DEFAULT.backgroundColor, result.theme.backgroundColor)
        // But should have custom typography
        assertEquals(20f, result.theme.titleFontSize)
    }

    @Test
    fun `parse YAML with partial colors uses defaults for missing`() {
        val yaml = """
            colors:
              primary: "#FF0000"
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(0xFFFF0000, result.theme.primaryColor)
        // Other colors should be defaults
        assertEquals(OverlayTheme.DEFAULT.backgroundColor, result.theme.backgroundColor)
        assertEquals(OverlayTheme.DEFAULT.textPrimaryColor, result.theme.textPrimaryColor)
    }

    @Test
    fun `parse YAML with empty sections uses all defaults`() {
        val yaml = """
            name: MinimalTheme
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(OverlayTheme.DEFAULT.primaryColor, result.theme.primaryColor)
        assertEquals(OverlayTheme.DEFAULT.titleFontSize, result.theme.titleFontSize)
        assertEquals(OverlayTheme.DEFAULT.paddingMedium, result.theme.paddingMedium)
    }

    @Test
    fun `parse YAML with custom base theme uses those defaults`() {
        val customBase = OverlayTheme(
            primaryColor = 0xFF00FF00,
            titleFontSize = 24f
        )

        val yaml = """
            colors:
              background: "#111111"
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml, customBase)

        assertIs<ThemeParseResult.Success>(result)
        // Should use custom base primary color (not in YAML)
        assertEquals(0xFF00FF00, result.theme.primaryColor)
        // Should use custom base font size (not in YAML)
        assertEquals(24f, result.theme.titleFontSize)
        // Should use YAML background color
        assertEquals(0xFF111111, result.theme.backgroundColor)
    }

    // ==================== Theme Variants Tests ====================

    @Test
    fun `parse YAML with variants returns theme and variants`() {
        val yaml = """
            name: BaseTheme
            colors:
              primary: "#2196F3"
              background: "#1E1E1E"
            variants:
              highContrast:
                colors:
                  background: "#000000"
                  textPrimary: "#FFFFFF"
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(0xFF2196F3, result.theme.primaryColor)
        assertEquals(0xFF1E1E1E, result.theme.backgroundColor)

        assertTrue(result.variants.containsKey("highContrast"))
        val highContrast = result.variants["highContrast"]!!
        assertEquals(0xFF000000, highContrast.backgroundColor)
        assertEquals(0xFFFFFFFF, highContrast.textPrimaryColor)
        // Variant inherits base theme primary color
        assertEquals(0xFF2196F3, highContrast.primaryColor)
    }

    @Test
    fun `parse YAML with multiple variants returns all variants`() {
        val yaml = """
            name: MultiVariantTheme
            colors:
              primary: "#2196F3"
            variants:
              light:
                colors:
                  background: "#FFFFFF"
                  textPrimary: "#000000"
              dark:
                colors:
                  background: "#121212"
                  textPrimary: "#FFFFFF"
              highContrast:
                colors:
                  background: "#000000"
                  textPrimary: "#FFFF00"
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(3, result.variants.size)
        assertTrue(result.variants.containsKey("light"))
        assertTrue(result.variants.containsKey("dark"))
        assertTrue(result.variants.containsKey("highContrast"))

        assertEquals(0xFFFFFFFF, result.variants["light"]!!.backgroundColor)
        assertEquals(0xFF121212, result.variants["dark"]!!.backgroundColor)
        assertEquals(0xFF000000, result.variants["highContrast"]!!.backgroundColor)
    }

    @Test
    fun `variant inherits base theme values not specified`() {
        val yaml = """
            colors:
              primary: "#FF5722"
            typography:
              titleSize: 22
            spacing:
              paddingLarge: 20
            variants:
              large:
                typography:
                  titleSize: 28
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        val largeVariant = result.variants["large"]!!

        // Should inherit base theme values
        assertEquals(0xFFFF5722, largeVariant.primaryColor)
        assertEquals(20f, largeVariant.paddingLarge)
        // Should override specified values
        assertEquals(28f, largeVariant.titleFontSize)
    }

    // ==================== Color Parsing Tests ====================

    @Test
    fun `parseColor with hash RGB format adds full alpha`() {
        val color = YamlThemeParser.parseColor("#FF5722")

        assertNotNull(color)
        assertEquals(0xFFFF5722, color)
    }

    @Test
    fun `parseColor with hash ARGB format preserves alpha`() {
        val color = YamlThemeParser.parseColor("#80FF5722")

        assertNotNull(color)
        assertEquals(0x80FF5722, color)
    }

    @Test
    fun `parseColor with 0x RGB format adds full alpha`() {
        val color = YamlThemeParser.parseColor("0xFF5722")

        assertNotNull(color)
        assertEquals(0xFFFF5722, color)
    }

    @Test
    fun `parseColor with 0x ARGB format preserves alpha`() {
        val color = YamlThemeParser.parseColor("0x80FF5722")

        assertNotNull(color)
        assertEquals(0x80FF5722, color)
    }

    @Test
    fun `parseColor case insensitive for hex prefix`() {
        val color1 = YamlThemeParser.parseColor("0XFF5722")
        val color2 = YamlThemeParser.parseColor("0xff5722")

        assertNotNull(color1)
        assertNotNull(color2)
        assertEquals(0xFFFF5722, color1)
        assertEquals(0xFFFF5722, color2)
    }

    @Test
    fun `parseColor case insensitive for hex digits`() {
        val lower = YamlThemeParser.parseColor("#aabbcc")
        val upper = YamlThemeParser.parseColor("#AABBCC")
        val mixed = YamlThemeParser.parseColor("#AaBbCc")

        assertNotNull(lower)
        assertNotNull(upper)
        assertNotNull(mixed)
        assertEquals(lower, upper)
        assertEquals(lower, mixed)
    }

    @Test
    fun `parseColor returns null for invalid format`() {
        assertNull(YamlThemeParser.parseColor("FF5722"))  // No prefix
        assertNull(YamlThemeParser.parseColor("#12345"))  // Wrong length
        assertNull(YamlThemeParser.parseColor("#1234567890"))  // Too long
        assertNull(YamlThemeParser.parseColor("#GGGGGG"))  // Invalid hex
        assertNull(YamlThemeParser.parseColor(""))  // Empty
        assertNull(YamlThemeParser.parseColor("rgb(255,87,34)"))  // Wrong format
    }

    @Test
    fun `parseColor handles whitespace`() {
        val color = YamlThemeParser.parseColor("  #FF5722  ")

        assertNotNull(color)
        assertEquals(0xFFFF5722, color)
    }

    @Test
    fun `colorToHexString produces correct format`() {
        val hex = YamlThemeParser.colorToHexString(0xFFFF5722)

        assertEquals("#FFFF5722", hex)
    }

    @Test
    fun `colorToHexString pads with zeros`() {
        val hex = YamlThemeParser.colorToHexString(0x00000001)

        assertEquals("#00000001", hex)
    }

    @Test
    fun `colorToHexString handles full alpha`() {
        val hex = YamlThemeParser.colorToHexString(0xFF2196F3)

        assertEquals("#FF2196F3", hex)
    }

    @Test
    fun `colorToHexString handles semi-transparent`() {
        val hex = YamlThemeParser.colorToHexString(0x80000000)

        assertEquals("#80000000", hex)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `parse empty YAML returns Error`() {
        val result = YamlThemeParser.parse("")

        assertIs<ThemeParseResult.Error>(result)
        assertTrue(result.message.contains("Empty"))
    }

    @Test
    fun `parse blank YAML returns Error`() {
        val result = YamlThemeParser.parse("   \n   \n   ")

        assertIs<ThemeParseResult.Error>(result)
    }

    @Test
    fun `parse YAML with invalid color generates warning`() {
        val yaml = """
            colors:
              primary: "not-a-color"
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertTrue(result.warnings.isNotEmpty())
        assertTrue(result.warnings.any { it.contains("primary") })
        // Should fall back to default
        assertEquals(OverlayTheme.DEFAULT.primaryColor, result.theme.primaryColor)
    }

    @Test
    fun `parse YAML preserves valid values even with warnings`() {
        val yaml = """
            colors:
              primary: "invalid"
              background: "#000000"
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertTrue(result.warnings.isNotEmpty())
        // Invalid primary falls back to default
        assertEquals(OverlayTheme.DEFAULT.primaryColor, result.theme.primaryColor)
        // Valid background is applied
        assertEquals(0xFF000000, result.theme.backgroundColor)
    }

    // ==================== Default Values Tests ====================

    @Test
    fun `default values applied when YAML is incomplete`() {
        val yaml = """
            colors:
              primary: "#FF0000"
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        val theme = result.theme

        // Only primary color should be changed
        assertEquals(0xFFFF0000, theme.primaryColor)

        // Everything else should be default
        assertEquals(OverlayTheme.DEFAULT.backgroundColor, theme.backgroundColor)
        assertEquals(OverlayTheme.DEFAULT.textPrimaryColor, theme.textPrimaryColor)
        assertEquals(OverlayTheme.DEFAULT.titleFontSize, theme.titleFontSize)
        assertEquals(OverlayTheme.DEFAULT.bodyFontSize, theme.bodyFontSize)
        assertEquals(OverlayTheme.DEFAULT.paddingMedium, theme.paddingMedium)
        assertEquals(OverlayTheme.DEFAULT.cornerRadiusMedium, theme.cornerRadiusMedium)
        assertEquals(OverlayTheme.DEFAULT.animationDurationNormal, theme.animationDurationNormal)
        assertEquals(OverlayTheme.DEFAULT.minimumContrastRatio, theme.minimumContrastRatio)
    }

    @Test
    fun `all status colors use defaults when not specified`() {
        val yaml = """
            name: NoStatusColors
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        val theme = result.theme

        assertEquals(OverlayTheme.DEFAULT.statusListeningColor, theme.statusListeningColor)
        assertEquals(OverlayTheme.DEFAULT.statusProcessingColor, theme.statusProcessingColor)
        assertEquals(OverlayTheme.DEFAULT.statusSuccessColor, theme.statusSuccessColor)
        assertEquals(OverlayTheme.DEFAULT.statusErrorColor, theme.statusErrorColor)
    }

    @Test
    fun `all badge colors use defaults when not specified`() {
        val yaml = """
            name: NoBadgeColors
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        val theme = result.theme

        assertEquals(OverlayTheme.DEFAULT.badgeEnabledWithNameColor, theme.badgeEnabledWithNameColor)
        assertEquals(OverlayTheme.DEFAULT.badgeEnabledNoNameColor, theme.badgeEnabledNoNameColor)
        assertEquals(OverlayTheme.DEFAULT.badgeDisabledColor, theme.badgeDisabledColor)
    }

    @Test
    fun `animation enabled defaults to true`() {
        val yaml = """
            animation:
              durationNormal: 300
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertTrue(result.theme.animationEnabled)
    }

    @Test
    fun `animation can be explicitly disabled`() {
        val yaml = """
            animation:
              enabled: false
              durationNormal: 0
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(false, result.theme.animationEnabled)
        assertEquals(0, result.theme.animationDurationNormal)
    }

    // ==================== YAML Comments Tests ====================

    @Test
    fun `parse YAML ignores comments`() {
        val yaml = """
            # This is a comment
            colors:
              # Primary color for the theme
              primary: "#2196F3"
              # Background color
              background: "#1E1E1E"
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(0xFF2196F3, result.theme.primaryColor)
        assertEquals(0xFF1E1E1E, result.theme.backgroundColor)
    }

    // ==================== Numeric Value Parsing Tests ====================

    @Test
    fun `parse integer values for float fields`() {
        val yaml = """
            typography:
              titleSize: 20
              bodySize: 14
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(20f, result.theme.titleFontSize)
        assertEquals(14f, result.theme.bodyFontSize)
    }

    @Test
    fun `parse float values for float fields`() {
        val yaml = """
            typography:
              titleSize: 20.5
              bodySize: 14.5
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(20.5f, result.theme.titleFontSize)
        assertEquals(14.5f, result.theme.bodyFontSize)
    }

    @Test
    fun `parse string numeric values`() {
        val yaml = """
            typography:
              titleSize: "20"
            spacing:
              paddingLarge: "16"
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(20f, result.theme.titleFontSize)
        assertEquals(16f, result.theme.paddingLarge)
    }

    // ==================== Alpha Value Tests ====================

    @Test
    fun `parse alpha values from accessibility section`() {
        val yaml = """
            accessibility:
              alphaDisabled: 0.5
              alphaSecondary: 0.7
              alphaHint: 0.6
        """.trimIndent()

        val result = YamlThemeParser.parse(yaml)

        assertIs<ThemeParseResult.Success>(result)
        assertEquals(0.5f, result.theme.alphaDisabled)
        assertEquals(0.7f, result.theme.alphaSecondary)
        assertEquals(0.6f, result.theme.alphaHint)
    }
}
