/**
 * ThemeProviderTest.kt - Tests for ThemeProvider functionality
 *
 * TDD tests for ThemeProvider covering:
 * - Theme variant switching
 * - Custom theme setting
 * - Theme reset
 * - Available variants
 * - Thread-safe access patterns
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
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ThemeProviderTest {

    // ==================== Current Theme Tests ====================

    @Test
    fun `currentTheme returns non-null value`() {
        val theme = ThemeProvider.currentTheme.value

        assertNotNull(theme)
    }

    @Test
    fun `initial currentTheme is DEFAULT`() {
        ThemeProvider.resetToDefault()

        val theme = ThemeProvider.currentTheme.value

        assertEquals(OverlayTheme.DEFAULT.primaryColor, theme.primaryColor)
        assertEquals(OverlayTheme.DEFAULT.backgroundColor, theme.backgroundColor)
    }

    @Test
    fun `currentVariant returns non-null value`() {
        val variant = ThemeProvider.currentVariant.value

        assertNotNull(variant)
    }

    @Test
    fun `initial currentVariant is DEFAULT after reset`() {
        ThemeProvider.resetToDefault()

        val variant = ThemeProvider.currentVariant.value

        assertEquals(ThemeVariant.DEFAULT, variant)
    }

    // ==================== setVariant Tests ====================

    @Test
    fun `setVariant HIGH_CONTRAST updates currentVariant`() {
        ThemeProvider.setVariant(ThemeVariant.HIGH_CONTRAST)

        assertEquals(ThemeVariant.HIGH_CONTRAST, ThemeProvider.currentVariant.value)

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    @Test
    fun `setVariant HIGH_CONTRAST updates currentTheme with high contrast values`() {
        ThemeProvider.setVariant(ThemeVariant.HIGH_CONTRAST)

        val theme = ThemeProvider.currentTheme.value

        // High contrast should have pure black background
        assertEquals(0xFF000000, theme.backgroundColor)
        // WCAG AAA contrast ratio
        assertEquals(7.0f, theme.minimumContrastRatio)

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    @Test
    fun `setVariant LARGE_TEXT updates theme with larger fonts`() {
        ThemeProvider.setVariant(ThemeVariant.LARGE_TEXT)

        val theme = ThemeProvider.currentTheme.value

        // Should have larger font sizes than default
        assertTrue(theme.titleFontSize > OverlayTheme.DEFAULT.titleFontSize)
        assertTrue(theme.bodyFontSize > OverlayTheme.DEFAULT.bodyFontSize)

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    @Test
    fun `setVariant COLORBLIND_FRIENDLY updates theme with colorblind-safe colors`() {
        ThemeProvider.setVariant(ThemeVariant.COLORBLIND_FRIENDLY)

        val theme = ThemeProvider.currentTheme.value

        // Success color should not be pure green
        assertNotEquals(0xFF00FF00, theme.statusSuccessColor)
        // Error color should not be pure red
        assertNotEquals(0xFFFF0000, theme.statusErrorColor)

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    @Test
    fun `setVariant REDUCED_MOTION disables animations`() {
        ThemeProvider.setVariant(ThemeVariant.REDUCED_MOTION)

        val theme = ThemeProvider.currentTheme.value

        assertEquals(0, theme.animationDurationFast)
        assertEquals(0, theme.animationDurationNormal)
        assertEquals(0, theme.animationDurationSlow)
        assertFalse(theme.animationEnabled)

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    @Test
    fun `setVariant LIGHT updates theme with light colors`() {
        ThemeProvider.setVariant(ThemeVariant.LIGHT)

        val theme = ThemeProvider.currentTheme.value

        // Light theme should have light background (high RGB values)
        val bgRed = ((theme.backgroundColor shr 16) and 0xFF).toInt()
        val bgGreen = ((theme.backgroundColor shr 8) and 0xFF).toInt()
        val bgBlue = (theme.backgroundColor and 0xFF).toInt()

        assertTrue(bgRed > 200 || bgGreen > 200 || bgBlue > 200,
            "Light theme should have bright background")

        // Text should be dark
        val textRed = ((theme.textPrimaryColor shr 16) and 0xFF).toInt()
        val textGreen = ((theme.textPrimaryColor shr 8) and 0xFF).toInt()
        val textBlue = (theme.textPrimaryColor and 0xFF).toInt()

        assertTrue(textRed < 100 && textGreen < 100 && textBlue < 100,
            "Light theme should have dark text")

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    @Test
    fun `setVariant GAMING updates theme with gaming colors`() {
        ThemeProvider.setVariant(ThemeVariant.GAMING)

        val theme = ThemeProvider.currentTheme.value

        // Gaming theme should have larger touch targets for fast response
        assertTrue(theme.minimumTouchTargetSize >= 48f)
        // Should have fast animations
        assertTrue(theme.animationDurationFast <= 150)

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    @Test
    fun `setVariant MINIMALIST updates theme with minimal styling`() {
        ThemeProvider.setVariant(ThemeVariant.MINIMALIST)

        val theme = ThemeProvider.currentTheme.value

        // Minimalist should have smaller badges
        assertTrue(theme.badgeSize < OverlayTheme.DEFAULT.badgeSize)
        // Smaller padding
        assertTrue(theme.paddingLarge <= OverlayTheme.DEFAULT.paddingLarge)

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    @Test
    fun `setVariant marks theme as not custom`() {
        // First set a custom theme
        ThemeProvider.setCustomTheme(OverlayTheme(primaryColor = 0xFFFF0000))
        assertTrue(ThemeProvider.isUsingCustomTheme())

        // Then set a variant
        ThemeProvider.setVariant(ThemeVariant.HIGH_CONTRAST)

        assertFalse(ThemeProvider.isUsingCustomTheme())

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    // ==================== setCustomTheme Tests ====================

    @Test
    fun `setCustomTheme updates currentTheme`() {
        val customTheme = OverlayTheme(primaryColor = 0xFFFF5722)

        ThemeProvider.setCustomTheme(customTheme)

        assertEquals(0xFFFF5722, ThemeProvider.currentTheme.value.primaryColor)

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    @Test
    fun `setCustomTheme marks theme as custom`() {
        ThemeProvider.resetToDefault()
        assertFalse(ThemeProvider.isUsingCustomTheme())

        val customTheme = OverlayTheme(primaryColor = 0xFFFF5722)
        ThemeProvider.setCustomTheme(customTheme)

        assertTrue(ThemeProvider.isUsingCustomTheme())

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    @Test
    fun `setCustomTheme preserves all custom properties`() {
        val customTheme = OverlayTheme(
            primaryColor = 0xFFFF0000,
            backgroundColor = 0xFF111111,
            titleFontSize = 24f,
            animationDurationNormal = 500,
            minimumContrastRatio = 6.0f
        )

        ThemeProvider.setCustomTheme(customTheme)

        val result = ThemeProvider.currentTheme.value
        assertEquals(0xFFFF0000, result.primaryColor)
        assertEquals(0xFF111111, result.backgroundColor)
        assertEquals(24f, result.titleFontSize)
        assertEquals(500, result.animationDurationNormal)
        assertEquals(6.0f, result.minimumContrastRatio)

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    // ==================== resetToDefault Tests ====================

    @Test
    fun `resetToDefault restores DEFAULT variant`() {
        ThemeProvider.setVariant(ThemeVariant.HIGH_CONTRAST)
        ThemeProvider.resetToDefault()

        assertEquals(ThemeVariant.DEFAULT, ThemeProvider.currentVariant.value)
    }

    @Test
    fun `resetToDefault restores DEFAULT theme`() {
        ThemeProvider.setVariant(ThemeVariant.HIGH_CONTRAST)
        ThemeProvider.resetToDefault()

        val theme = ThemeProvider.currentTheme.value
        assertEquals(OverlayTheme.DEFAULT.primaryColor, theme.primaryColor)
        assertEquals(OverlayTheme.DEFAULT.backgroundColor, theme.backgroundColor)
    }

    @Test
    fun `resetToDefault clears custom theme flag`() {
        ThemeProvider.setCustomTheme(OverlayTheme(primaryColor = 0xFFFF0000))
        assertTrue(ThemeProvider.isUsingCustomTheme())

        ThemeProvider.resetToDefault()

        assertFalse(ThemeProvider.isUsingCustomTheme())
    }

    // ==================== getThemeForVariant Tests ====================

    @Test
    fun `getThemeForVariant returns theme without changing current`() {
        ThemeProvider.resetToDefault()
        val currentBefore = ThemeProvider.currentTheme.value

        val highContrastTheme = ThemeProvider.getThemeForVariant(ThemeVariant.HIGH_CONTRAST)

        // Current should not change
        assertEquals(currentBefore.primaryColor, ThemeProvider.currentTheme.value.primaryColor)
        // But we should get high contrast theme
        assertEquals(0xFF000000, highContrastTheme.backgroundColor)
    }

    @Test
    fun `getThemeForVariant DEFAULT returns default theme`() {
        val theme = ThemeProvider.getThemeForVariant(ThemeVariant.DEFAULT)

        assertEquals(OverlayTheme.DEFAULT.primaryColor, theme.primaryColor)
        assertEquals(OverlayTheme.DEFAULT.backgroundColor, theme.backgroundColor)
    }

    @Test
    fun `getThemeForVariant returns correct theme for each variant`() {
        ThemeVariant.entries.forEach { variant ->
            val theme = ThemeProvider.getThemeForVariant(variant)
            assertNotNull(theme, "Theme for $variant should not be null")
        }
    }

    // ==================== getAvailableVariants Tests ====================

    @Test
    fun `getAvailableVariants returns all variants`() {
        val variants = ThemeProvider.getAvailableVariants()

        assertEquals(ThemeVariant.entries.size, variants.size)
        ThemeVariant.entries.forEach { variant ->
            assertTrue(variants.contains(variant), "Should contain $variant")
        }
    }

    @Test
    fun `getAvailableVariants includes DEFAULT`() {
        val variants = ThemeProvider.getAvailableVariants()

        assertTrue(variants.contains(ThemeVariant.DEFAULT))
    }

    @Test
    fun `getAvailableVariants includes accessibility variants`() {
        val variants = ThemeProvider.getAvailableVariants()

        assertTrue(variants.contains(ThemeVariant.HIGH_CONTRAST))
        assertTrue(variants.contains(ThemeVariant.LARGE_TEXT))
        assertTrue(variants.contains(ThemeVariant.COLORBLIND_FRIENDLY))
        assertTrue(variants.contains(ThemeVariant.REDUCED_MOTION))
    }

    // ==================== isAnimationEnabled Tests ====================

    @Test
    fun `isAnimationEnabled returns true for DEFAULT variant`() {
        ThemeProvider.resetToDefault()

        assertTrue(ThemeProvider.isAnimationEnabled())
    }

    @Test
    fun `isAnimationEnabled returns false for REDUCED_MOTION variant`() {
        ThemeProvider.setVariant(ThemeVariant.REDUCED_MOTION)

        assertFalse(ThemeProvider.isAnimationEnabled())

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    // ==================== instance Tests ====================

    @Test
    fun `instance returns same singleton`() {
        val instance1 = ThemeProvider.instance
        val instance2 = ThemeProvider.instance

        assertEquals(instance1, instance2)
    }

    @Test
    fun `instance is same as ThemeProvider object`() {
        assertEquals(ThemeProvider, ThemeProvider.instance)
    }

    // ==================== isUsingCustomTheme Tests ====================

    @Test
    fun `isUsingCustomTheme returns false initially after reset`() {
        ThemeProvider.resetToDefault()

        assertFalse(ThemeProvider.isUsingCustomTheme())
    }

    @Test
    fun `isUsingCustomTheme returns true after setCustomTheme`() {
        ThemeProvider.setCustomTheme(OverlayTheme())

        assertTrue(ThemeProvider.isUsingCustomTheme())

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    @Test
    fun `isUsingCustomTheme returns false after setVariant`() {
        ThemeProvider.setCustomTheme(OverlayTheme())
        ThemeProvider.setVariant(ThemeVariant.DEFAULT)

        assertFalse(ThemeProvider.isUsingCustomTheme())
    }

    // ==================== Theme Variant Property Tests ====================

    @Test
    fun `HIGH_CONTRAST variant has WCAG AAA contrast ratio`() {
        val theme = ThemeProvider.getThemeForVariant(ThemeVariant.HIGH_CONTRAST)

        assertTrue(theme.minimumContrastRatio >= 7.0f,
            "High contrast should meet WCAG AAA (7:1)")
    }

    @Test
    fun `LARGE_TEXT variant has larger fonts than DEFAULT`() {
        val defaultTheme = ThemeProvider.getThemeForVariant(ThemeVariant.DEFAULT)
        val largeTextTheme = ThemeProvider.getThemeForVariant(ThemeVariant.LARGE_TEXT)

        assertTrue(largeTextTheme.titleFontSize > defaultTheme.titleFontSize)
        assertTrue(largeTextTheme.bodyFontSize > defaultTheme.bodyFontSize)
        assertTrue(largeTextTheme.captionFontSize > defaultTheme.captionFontSize)
    }

    @Test
    fun `all variants have valid primary color`() {
        ThemeVariant.entries.forEach { variant ->
            val theme = ThemeProvider.getThemeForVariant(variant)
            val alpha = ((theme.primaryColor shr 24) and 0xFF).toInt()
            assertTrue(alpha >= 0xCC, "Variant $variant should have opaque primary color")
        }
    }

    @Test
    fun `all variants have valid background color`() {
        ThemeVariant.entries.forEach { variant ->
            val theme = ThemeProvider.getThemeForVariant(variant)
            val alpha = ((theme.backgroundColor shr 24) and 0xFF).toInt()
            assertTrue(alpha > 0, "Variant $variant should have some background alpha")
        }
    }

    @Test
    fun `all variants have positive or zero animation durations`() {
        ThemeVariant.entries.forEach { variant ->
            val theme = ThemeProvider.getThemeForVariant(variant)
            assertTrue(theme.animationDurationFast >= 0,
                "Variant $variant should have non-negative fast animation")
            assertTrue(theme.animationDurationNormal >= 0,
                "Variant $variant should have non-negative normal animation")
            assertTrue(theme.animationDurationSlow >= 0,
                "Variant $variant should have non-negative slow animation")
        }
    }

    // ==================== Thread Safety Tests (Basic) ====================

    @Test
    fun `multiple setVariant calls in sequence work correctly`() {
        ThemeProvider.setVariant(ThemeVariant.HIGH_CONTRAST)
        ThemeProvider.setVariant(ThemeVariant.LARGE_TEXT)
        ThemeProvider.setVariant(ThemeVariant.DEFAULT)

        assertEquals(ThemeVariant.DEFAULT, ThemeProvider.currentVariant.value)

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    @Test
    fun `setVariant then setCustomTheme works correctly`() {
        ThemeProvider.setVariant(ThemeVariant.HIGH_CONTRAST)
        val customTheme = OverlayTheme(primaryColor = 0xFFFF0000)
        ThemeProvider.setCustomTheme(customTheme)

        assertEquals(0xFFFF0000, ThemeProvider.currentTheme.value.primaryColor)
        assertTrue(ThemeProvider.isUsingCustomTheme())

        // Cleanup
        ThemeProvider.resetToDefault()
    }

    @Test
    fun `setCustomTheme then setVariant works correctly`() {
        val customTheme = OverlayTheme(primaryColor = 0xFFFF0000)
        ThemeProvider.setCustomTheme(customTheme)
        ThemeProvider.setVariant(ThemeVariant.HIGH_CONTRAST)

        assertEquals(ThemeVariant.HIGH_CONTRAST, ThemeProvider.currentVariant.value)
        assertEquals(0xFF000000, ThemeProvider.currentTheme.value.backgroundColor)
        assertFalse(ThemeProvider.isUsingCustomTheme())

        // Cleanup
        ThemeProvider.resetToDefault()
    }
}
