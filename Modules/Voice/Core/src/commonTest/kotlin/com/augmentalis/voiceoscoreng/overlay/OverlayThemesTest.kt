package com.augmentalis.voiceoscoreng.overlay

import com.augmentalis.voiceoscoreng.features.OverlayThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertContains

/**
 * TDD tests for OverlayThemes
 *
 * Tests the predefined theme collection including:
 * - Theme retrieval by name
 * - Theme name listing
 * - Default theme behavior
 * - Unknown theme fallback
 * - Theme descriptions
 */
class OverlayThemesTest {

    // ==================== getTheme Tests ====================

    @Test
    fun `getTheme with Material3Dark returns dark theme`() {
        val theme = OverlayThemes.getTheme("Material3Dark")

        assertNotNull(theme)
        // Material3Dark should have blue primary
        assertEquals(0xFF2196F3, theme.primaryColor)
        // Dark background
        assertEquals(0xEE1E1E1E, theme.backgroundColor)
        // White text
        assertEquals(0xFFFFFFFF, theme.textPrimaryColor)
    }

    @Test
    fun `getTheme with material3dark lowercase returns dark theme`() {
        val theme = OverlayThemes.getTheme("material3dark")

        assertNotNull(theme)
        assertEquals(0xFF2196F3, theme.primaryColor)
    }

    @Test
    fun `getTheme with default returns Material3Dark`() {
        val theme = OverlayThemes.getTheme("default")
        val material3Dark = OverlayThemes.getTheme("Material3Dark")

        assertEquals(material3Dark.primaryColor, theme.primaryColor)
        assertEquals(material3Dark.backgroundColor, theme.backgroundColor)
    }

    @Test
    fun `getTheme with HighContrast returns high contrast theme`() {
        val theme = OverlayThemes.getTheme("HighContrast")

        assertNotNull(theme)
        // High contrast should have pure black background
        assertEquals(0xFF000000, theme.backgroundColor)
        // Pure white text
        assertEquals(0xFFFFFFFF, theme.textPrimaryColor)
        // WCAG AAA compliance
        assertEquals(7.0f, theme.minimumContrastRatio)
        // Larger touch targets
        assertTrue(theme.badgeSize >= 48f)
    }

    @Test
    fun `getTheme with accessibility alias returns HighContrast theme`() {
        val highContrast = OverlayThemes.getTheme("HighContrast")
        val accessibility = OverlayThemes.getTheme("accessibility")

        assertEquals(highContrast.primaryColor, accessibility.primaryColor)
        assertEquals(highContrast.backgroundColor, accessibility.backgroundColor)
    }

    @Test
    fun `getTheme with Minimalist returns minimalist theme`() {
        val theme = OverlayThemes.getTheme("Minimalist")

        assertNotNull(theme)
        // Grey primary color
        assertEquals(0xFF9E9E9E, theme.primaryColor)
        // Smaller badge
        assertTrue(theme.badgeSize <= 32f)
        // Faster animations
        assertTrue(theme.animationDurationNormal <= 150)
    }

    @Test
    fun `getTheme with minimal alias returns Minimalist theme`() {
        val minimalist = OverlayThemes.getTheme("Minimalist")
        val minimal = OverlayThemes.getTheme("minimal")

        assertEquals(minimalist.primaryColor, minimal.primaryColor)
    }

    @Test
    fun `getTheme with Gaming returns gaming theme`() {
        val theme = OverlayThemes.getTheme("Gaming")

        assertNotNull(theme)
        // Neon green primary
        assertEquals(0xFF00FF41, theme.primaryColor)
        // Sharp corners
        assertTrue(theme.cornerRadiusSmall <= 4f)
    }

    @Test
    fun `getTheme with neon alias returns Gaming theme`() {
        val gaming = OverlayThemes.getTheme("Gaming")
        val neon = OverlayThemes.getTheme("neon")

        assertEquals(gaming.primaryColor, neon.primaryColor)
    }

    @Test
    fun `getTheme with Professional returns professional theme`() {
        val theme = OverlayThemes.getTheme("Professional")

        assertNotNull(theme)
        // Navy blue primary
        assertEquals(0xFF1976D2, theme.primaryColor)
        // Larger padding
        assertTrue(theme.paddingLarge >= 16f)
    }

    @Test
    fun `getTheme with corporate alias returns Professional theme`() {
        val professional = OverlayThemes.getTheme("Professional")
        val corporate = OverlayThemes.getTheme("corporate")

        assertEquals(professional.primaryColor, corporate.primaryColor)
    }

    @Test
    fun `getTheme with Material3Light returns light theme`() {
        val theme = OverlayThemes.getTheme("Material3Light")

        assertNotNull(theme)
        // Light background (white-ish)
        assertTrue(theme.backgroundColor and 0x00FFFFFF >= 0x00F0F0F0)
        // Dark text
        assertEquals(0xFF000000, theme.textPrimaryColor)
    }

    @Test
    fun `getTheme with light alias returns Material3Light theme`() {
        val material3Light = OverlayThemes.getTheme("Material3Light")
        val light = OverlayThemes.getTheme("light")

        assertEquals(material3Light.primaryColor, light.primaryColor)
        assertEquals(material3Light.backgroundColor, light.backgroundColor)
    }

    // ==================== Unknown Theme Tests ====================

    @Test
    fun `getTheme with unknown name returns default theme`() {
        val defaultTheme = OverlayThemes.getDefault()
        val unknownTheme = OverlayThemes.getTheme("SomeUnknownTheme")

        assertEquals(defaultTheme.primaryColor, unknownTheme.primaryColor)
        assertEquals(defaultTheme.backgroundColor, unknownTheme.backgroundColor)
    }

    @Test
    fun `getTheme with empty string returns default theme`() {
        val defaultTheme = OverlayThemes.getDefault()
        val emptyTheme = OverlayThemes.getTheme("")

        assertEquals(defaultTheme.primaryColor, emptyTheme.primaryColor)
    }

    // ==================== getThemeNames Tests ====================

    @Test
    fun `getThemeNames returns all available theme names`() {
        val names = OverlayThemes.getThemeNames()

        assertTrue(names.isNotEmpty())
        assertContains(names, "Material3Dark")
        assertContains(names, "HighContrast")
        assertContains(names, "Minimalist")
        assertContains(names, "Gaming")
        assertContains(names, "Professional")
        assertContains(names, "Material3Light")
    }

    @Test
    fun `getThemeNames returns exactly 6 themes`() {
        val names = OverlayThemes.getThemeNames()

        assertEquals(6, names.size)
    }

    @Test
    fun `getThemeNames does not contain duplicates`() {
        val names = OverlayThemes.getThemeNames()

        assertEquals(names.size, names.toSet().size)
    }

    // ==================== getDefault Tests ====================

    @Test
    fun `getDefault returns Material3Dark`() {
        val defaultTheme = OverlayThemes.getDefault()
        val material3Dark = OverlayThemes.getTheme("Material3Dark")

        assertEquals(material3Dark.primaryColor, defaultTheme.primaryColor)
        assertEquals(material3Dark.backgroundColor, defaultTheme.backgroundColor)
        assertEquals(material3Dark.textPrimaryColor, defaultTheme.textPrimaryColor)
    }

    @Test
    fun `getDefault returns non-null theme`() {
        val defaultTheme = OverlayThemes.getDefault()

        assertNotNull(defaultTheme)
    }

    @Test
    fun `getDefault returns consistent theme across calls`() {
        val first = OverlayThemes.getDefault()
        val second = OverlayThemes.getDefault()

        assertEquals(first.primaryColor, second.primaryColor)
        assertEquals(first.backgroundColor, second.backgroundColor)
    }

    // ==================== getThemeDescriptions Tests ====================

    @Test
    fun `getThemeDescriptions returns descriptions for all themes`() {
        val descriptions = OverlayThemes.getThemeDescriptions()
        val names = OverlayThemes.getThemeNames()

        assertEquals(names.size, descriptions.size)
        names.forEach { name ->
            assertTrue(descriptions.containsKey(name), "Missing description for $name")
        }
    }

    @Test
    fun `getThemeDescriptions has non-empty descriptions`() {
        val descriptions = OverlayThemes.getThemeDescriptions()

        descriptions.values.forEach { description ->
            assertTrue(description.isNotBlank(), "Description should not be blank")
        }
    }

    @Test
    fun `getThemeDescriptions includes accessibility info for HighContrast`() {
        val descriptions = OverlayThemes.getThemeDescriptions()
        val highContrastDesc = descriptions["HighContrast"]

        assertNotNull(highContrastDesc)
        assertTrue(
            highContrastDesc.contains("accessibility", ignoreCase = true) ||
            highContrastDesc.contains("WCAG", ignoreCase = true),
            "HighContrast description should mention accessibility"
        )
    }

    // ==================== LargeText Theme Tests ====================

    @Test
    fun `getTheme with LargeText returns large text theme`() {
        val theme = OverlayThemes.getTheme("LargeText")

        assertNotNull(theme)
        // Large fonts
        assertTrue(theme.bodyFontSize >= 18f)
        assertTrue(theme.titleFontSize >= 20f)
    }

    @Test
    fun `getTheme with largetext lowercase returns LargeText theme`() {
        val largeText = OverlayThemes.getTheme("LargeText")
        val lowercase = OverlayThemes.getTheme("largetext")

        assertEquals(largeText.bodyFontSize, lowercase.bodyFontSize)
    }

    // ==================== ColorblindFriendly Theme Tests ====================

    @Test
    fun `getTheme with ColorblindFriendly returns colorblind-safe theme`() {
        val theme = OverlayThemes.getTheme("ColorblindFriendly")

        assertNotNull(theme)
        // Should have colorblind-safe palette (no red-green conflicts)
        // Success should not be pure green
        assertTrue(theme.statusSuccessColor != 0xFF00FF00)
        // Error should not be pure red
        assertTrue(theme.statusErrorColor != 0xFFFF0000)
    }

    // ==================== Theme Property Consistency Tests ====================

    @Test
    fun `all themes have valid primary colors`() {
        val names = OverlayThemes.getThemeNames()

        names.forEach { name ->
            val theme = OverlayThemes.getTheme(name)
            // Alpha channel should be fully opaque or near-opaque
            val alpha = (theme.primaryColor shr 24) and 0xFF
            assertTrue(alpha >= 0xCC, "Theme $name has low alpha primary color")
        }
    }

    @Test
    fun `all themes have valid background colors`() {
        val names = OverlayThemes.getThemeNames()

        names.forEach { name ->
            val theme = OverlayThemes.getTheme(name)
            // Background should have some alpha
            val alpha = (theme.backgroundColor shr 24) and 0xFF
            assertTrue(alpha > 0, "Theme $name has zero alpha background")
        }
    }

    @Test
    fun `all themes have positive animation durations or zero`() {
        val names = OverlayThemes.getThemeNames()

        names.forEach { name ->
            val theme = OverlayThemes.getTheme(name)
            assertTrue(theme.animationDurationFast >= 0, "Theme $name has negative fast animation")
            assertTrue(theme.animationDurationNormal >= 0, "Theme $name has negative normal animation")
            assertTrue(theme.animationDurationSlow >= 0, "Theme $name has negative slow animation")
        }
    }
}
