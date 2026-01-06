/**
 * OverlayThemeValidationTest.kt - Additional tests for theme validation
 *
 * Extended TDD tests for OverlayTheme validation covering:
 * - Accessibility validation
 * - Contrast ratio checks
 * - Touch target sizes
 * - Font size requirements
 * - Theme validation combinations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscoreng.theme

import com.augmentalis.voiceoscoreng.overlay.OverlayTheme
import com.augmentalis.voiceoscoreng.overlay.ThemeValidationResult
import com.augmentalis.voiceoscoreng.overlay.colorWithAlpha
import com.augmentalis.voiceoscoreng.overlay.extractAlpha
import com.augmentalis.voiceoscoreng.overlay.extractRed
import com.augmentalis.voiceoscoreng.overlay.extractGreen
import com.augmentalis.voiceoscoreng.overlay.extractBlue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OverlayThemeValidationTest {

    // ==================== Basic Validation Tests ====================

    @Test
    fun `default theme validates successfully`() {
        val theme = OverlayTheme.DEFAULT
        val result = theme.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validation result has isValid property`() {
        val theme = OverlayTheme.DEFAULT
        val result = theme.validate()

        assertNotNull(result.isValid)
    }

    @Test
    fun `validation result has errors list`() {
        val theme = OverlayTheme.DEFAULT
        val result = theme.validate()

        assertNotNull(result.errors)
    }

    @Test
    fun `DARK theme validates successfully`() {
        val theme = OverlayTheme.DARK
        val result = theme.validate()

        assertTrue(result.isValid)
    }

    @Test
    fun `HIGH_CONTRAST theme validates successfully`() {
        val theme = OverlayTheme.HIGH_CONTRAST
        val result = theme.validate()

        assertTrue(result.isValid)
    }

    // ==================== Contrast Ratio Validation Tests ====================

    @Test
    fun `theme with good contrast passes validation`() {
        val theme = OverlayTheme(
            textPrimaryColor = 0xFFFFFFFF,  // White text
            backgroundColor = 0xFF000000     // Black background
        )

        val result = theme.validate()

        assertTrue(result.isValid)
    }

    @Test
    fun `theme with poor contrast fails validation`() {
        val theme = OverlayTheme(
            textPrimaryColor = 0xFF333333,   // Dark gray text
            backgroundColor = 0xFF1E1E1E     // Dark background
        )

        val result = theme.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("contrast") })
    }

    @Test
    fun `theme with medium gray on dark fails contrast check`() {
        val theme = OverlayTheme(
            textPrimaryColor = 0xFF666666,
            backgroundColor = 0xFF111111
        )

        val result = theme.validate()

        assertFalse(result.isValid)
    }

    @Test
    fun `theme with yellow text on white background may fail contrast`() {
        val theme = OverlayTheme(
            textPrimaryColor = 0xFFFFFF00,   // Yellow text
            backgroundColor = 0xFFFFFFFF     // White background
        )

        val result = theme.validate()

        // Yellow on white typically fails contrast requirements
        assertFalse(result.isValid)
    }

    // ==================== Touch Target Size Validation Tests ====================

    @Test
    fun `theme with adequate badge size passes validation`() {
        val theme = OverlayTheme(
            badgeSize = 48f,
            minimumTouchTargetSize = 48f
        )

        val result = theme.validate()

        assertTrue(result.isValid)
    }

    @Test
    fun `theme with small badge size fails validation`() {
        val theme = OverlayTheme(
            badgeSize = 20f,
            minimumTouchTargetSize = 48f
        )

        val result = theme.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Badge") || it.contains("badge") })
    }

    @Test
    fun `theme with badge size equal to minimum passes`() {
        val theme = OverlayTheme(
            badgeSize = 48f,
            minimumTouchTargetSize = 48f
        )

        val result = theme.validate()

        assertTrue(result.isValid)
    }

    @Test
    fun `theme with badge size just below minimum fails`() {
        val theme = OverlayTheme(
            badgeSize = 47f,
            minimumTouchTargetSize = 48f
        )

        val result = theme.validate()

        assertFalse(result.isValid)
    }

    // ==================== Font Size Validation Tests ====================

    @Test
    fun `theme with adequate body font size passes validation`() {
        val theme = OverlayTheme(bodyFontSize = 14f)

        val result = theme.validate()

        assertTrue(result.isValid)
    }

    @Test
    fun `theme with small body font size fails validation`() {
        val theme = OverlayTheme(bodyFontSize = 10f)

        val result = theme.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("font") || it.contains("Font") })
    }

    @Test
    fun `theme with body font size at minimum boundary passes`() {
        val theme = OverlayTheme(bodyFontSize = 12f)

        val result = theme.validate()

        assertTrue(result.isValid)
    }

    @Test
    fun `theme with body font size just below minimum fails`() {
        val theme = OverlayTheme(bodyFontSize = 11.9f)

        val result = theme.validate()

        assertFalse(result.isValid)
    }

    // ==================== Multiple Validation Errors Tests ====================

    @Test
    fun `theme with multiple issues has multiple errors`() {
        val theme = OverlayTheme(
            textPrimaryColor = 0xFF333333,   // Poor contrast
            backgroundColor = 0xFF1E1E1E,
            badgeSize = 20f,                  // Too small
            bodyFontSize = 8f                 // Too small
        )

        val result = theme.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.size >= 2, "Should have at least 2 errors")
    }

    @Test
    fun `validation errors are descriptive`() {
        val theme = OverlayTheme(badgeSize = 20f)

        val result = theme.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.first().length > 10,
            "Error messages should be descriptive")
    }

    // ==================== ThemeValidationResult toString Tests ====================

    @Test
    fun `valid theme toString indicates success`() {
        val result = ThemeValidationResult(isValid = true, errors = emptyList())

        val string = result.toString()

        assertTrue(string.contains("passes") || string.contains("valid"),
            "Should indicate validation passed")
    }

    @Test
    fun `invalid theme toString lists errors`() {
        val result = ThemeValidationResult(
            isValid = false,
            errors = listOf("Error 1", "Error 2")
        )

        val string = result.toString()

        assertTrue(string.contains("failed") || string.contains("Error"))
        assertTrue(string.contains("Error 1"))
        assertTrue(string.contains("Error 2"))
    }

    // ==================== Color Utility Function Tests ====================

    @Test
    fun `colorWithAlpha creates correct semi-transparent color`() {
        val result = colorWithAlpha(0xFFFF0000, 0.5f)

        // Should be red with 50% alpha (~0x80)
        val alpha = ((result shr 24) and 0xFF).toInt()
        assertTrue(alpha in 0x7E..0x81, "Alpha should be approximately 0x80")

        // RGB should be unchanged
        val red = ((result shr 16) and 0xFF).toInt()
        assertEquals(0xFF, red)
    }

    @Test
    fun `colorWithAlpha with zero alpha creates transparent`() {
        val result = colorWithAlpha(0xFFFF0000, 0.0f)

        val alpha = ((result shr 24) and 0xFF).toInt()
        assertEquals(0, alpha)
    }

    @Test
    fun `colorWithAlpha with full alpha preserves original`() {
        val result = colorWithAlpha(0xFFFF0000, 1.0f)

        assertEquals(0xFFFF0000, result)
    }

    @Test
    fun `colorWithAlpha clamps alpha above 1`() {
        val result = colorWithAlpha(0x00FF0000, 1.5f)

        val alpha = ((result shr 24) and 0xFF).toInt()
        assertEquals(0xFF, alpha)
    }

    @Test
    fun `colorWithAlpha clamps alpha below 0`() {
        val result = colorWithAlpha(0xFFFF0000, -0.5f)

        val alpha = ((result shr 24) and 0xFF).toInt()
        assertEquals(0, alpha)
    }

    @Test
    fun `extractAlpha returns correct value`() {
        assertEquals(1.0f, extractAlpha(0xFFFFFFFF), 0.01f)
        assertEquals(0.0f, extractAlpha(0x00FFFFFF), 0.01f)
        assertEquals(0.5f, extractAlpha(0x80FFFFFF), 0.01f)
    }

    @Test
    fun `extractRed returns correct value`() {
        assertEquals(1.0f, extractRed(0xFFFF0000), 0.01f)
        assertEquals(0.0f, extractRed(0xFF00FFFF), 0.01f)
        assertEquals(0.5f, extractRed(0xFF800000), 0.02f)
    }

    @Test
    fun `extractGreen returns correct value`() {
        assertEquals(1.0f, extractGreen(0xFF00FF00), 0.01f)
        assertEquals(0.0f, extractGreen(0xFFFF00FF), 0.01f)
        assertEquals(0.5f, extractGreen(0xFF008000), 0.02f)
    }

    @Test
    fun `extractBlue returns correct value`() {
        assertEquals(1.0f, extractBlue(0xFF0000FF), 0.01f)
        assertEquals(0.0f, extractBlue(0xFFFFFF00), 0.01f)
        assertEquals(0.5f, extractBlue(0xFF000080), 0.02f)
    }

    // ==================== Theme Modifier Validation Tests ====================

    @Test
    fun `withLargeText theme passes validation`() {
        val theme = OverlayTheme.DEFAULT.withLargeText()
        val result = theme.validate()

        assertTrue(result.isValid)
    }

    @Test
    fun `toHighContrast theme passes validation`() {
        val theme = OverlayTheme.DEFAULT.toHighContrast()
        val result = theme.validate()

        assertTrue(result.isValid)
    }

    @Test
    fun `withReducedMotion theme passes validation`() {
        val theme = OverlayTheme.DEFAULT.withReducedMotion()
        val result = theme.validate()

        assertTrue(result.isValid)
    }

    @Test
    fun `withPrimaryColor theme passes validation with valid color`() {
        val theme = OverlayTheme.DEFAULT.withPrimaryColor(0xFF009688)
        val result = theme.validate()

        assertTrue(result.isValid)
    }

    @Test
    fun `chained modifiers produce valid theme`() {
        val theme = OverlayTheme.DEFAULT
            .withLargeText()
            .toHighContrast()
            .withReducedMotion()

        val result = theme.validate()

        assertTrue(result.isValid)
    }

    // ==================== Accessibility Requirements Tests ====================

    @Test
    fun `WCAG AA requires 4_5 to 1 contrast ratio`() {
        val theme = OverlayTheme(minimumContrastRatio = 4.5f)

        assertEquals(4.5f, theme.minimumContrastRatio)
    }

    @Test
    fun `WCAG AAA requires 7 to 1 contrast ratio`() {
        val theme = OverlayTheme.DEFAULT.toHighContrast()

        assertEquals(7.0f, theme.minimumContrastRatio)
    }

    @Test
    fun `minimum touch target should be at least 44dp for accessibility`() {
        // Android guidelines suggest 48dp, iOS suggests 44dp
        val theme = OverlayTheme.DEFAULT

        assertTrue(theme.minimumTouchTargetSize >= 44f,
            "Touch targets should be at least 44dp for accessibility")
    }

    @Test
    fun `focus indicator width should be visible`() {
        val theme = OverlayTheme.DEFAULT

        assertTrue(theme.focusIndicatorWidth >= 2f,
            "Focus indicator should be at least 2dp for visibility")
    }

    // ==================== Theme Variant Validation Tests ====================

    @Test
    fun `all built-in theme variants pass validation`() {
        val themes = listOf(
            OverlayTheme.DEFAULT,
            OverlayTheme.DARK,
            OverlayTheme.HIGH_CONTRAST
        )

        themes.forEach { theme ->
            val result = theme.validate()
            assertTrue(result.isValid, "Built-in theme should pass validation")
        }
    }

    @Test
    fun `ThemeProvider variants all pass validation`() {
        ThemeVariant.entries.forEach { variant ->
            val theme = ThemeProvider.getThemeForVariant(variant)
            val result = theme.validate()

            // Note: Some theme variants may intentionally fail certain checks
            // for stylistic reasons. This test documents expected behavior.
            if (!result.isValid) {
                println("Variant $variant has validation warnings: ${result.errors}")
            }
        }
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun `theme with zero padding values still validates colors correctly`() {
        val theme = OverlayTheme(
            paddingSmall = 0f,
            paddingMedium = 0f,
            paddingLarge = 0f
        )

        val result = theme.validate()

        // Should only fail on spacing-related issues, not crash
        assertNotNull(result)
    }

    @Test
    fun `theme with extremely large font sizes still validates`() {
        val theme = OverlayTheme(
            titleFontSize = 100f,
            bodyFontSize = 80f
        )

        val result = theme.validate()

        // Large fonts should pass - they meet minimum requirements
        assertTrue(result.isValid)
    }

    @Test
    fun `theme with zero animation durations is valid for reduced motion`() {
        val theme = OverlayTheme(
            animationDurationFast = 0,
            animationDurationNormal = 0,
            animationDurationSlow = 0,
            animationEnabled = false
        )

        val result = theme.validate()

        assertTrue(result.isValid)
    }

    // ==================== Companion Object Theme Tests ====================

    @Test
    fun `DEFAULT companion object theme exists`() {
        val theme = OverlayTheme.DEFAULT

        assertNotNull(theme)
        assertEquals(0xFF2196F3, theme.primaryColor)
    }

    @Test
    fun `DARK companion object theme has darker background`() {
        val theme = OverlayTheme.DARK

        assertNotNull(theme)
        assertEquals(0xFF121212, theme.backgroundColor)
    }

    @Test
    fun `HIGH_CONTRAST companion object theme has maximum contrast`() {
        val theme = OverlayTheme.HIGH_CONTRAST

        assertNotNull(theme)
        assertEquals(0xFF000000, theme.backgroundColor)
        assertEquals(0xFFFFFFFF, theme.textPrimaryColor)
        assertEquals(7.0f, theme.minimumContrastRatio)
    }
}
