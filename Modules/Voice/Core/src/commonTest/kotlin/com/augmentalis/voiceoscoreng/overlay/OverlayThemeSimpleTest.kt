/**
 * OverlayThemeSimpleTest.kt - TDD Tests for OverlayTheme data class
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscoreng.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals

/**
 * TDD tests for the simple OverlayTheme data class in the overlay package.
 *
 * This tests a streamlined theme configuration with:
 * - LIGHT, DARK, and HIGH_CONTRAST preset themes
 * - Custom theme creation
 * - Default values for optional properties
 */
class OverlayThemeSimpleTest {

    // ==================== LIGHT Theme Tests ====================

    @Test
    fun `LIGHT theme has correct name`() {
        val theme = OverlayThemeSimple.LIGHT
        assertEquals("light", theme.name)
    }

    @Test
    fun `LIGHT theme has white background color`() {
        val theme = OverlayThemeSimple.LIGHT
        assertEquals(0xFFFFFFFFL, theme.backgroundColor)
    }

    @Test
    fun `LIGHT theme has black text color`() {
        val theme = OverlayThemeSimple.LIGHT
        assertEquals(0xFF000000L, theme.textColor)
    }

    @Test
    fun `LIGHT theme has blue accent color`() {
        val theme = OverlayThemeSimple.LIGHT
        assertEquals(0xFF2196F3L, theme.accentColor)
    }

    @Test
    fun `LIGHT theme has light gray border color`() {
        val theme = OverlayThemeSimple.LIGHT
        assertEquals(0xFFE0E0E0L, theme.borderColor)
    }

    @Test
    fun `LIGHT theme has default border width of 1`() {
        val theme = OverlayThemeSimple.LIGHT
        assertEquals(1, theme.borderWidth)
    }

    @Test
    fun `LIGHT theme has default corner radius of 8`() {
        val theme = OverlayThemeSimple.LIGHT
        assertEquals(8, theme.cornerRadius)
    }

    @Test
    fun `LIGHT theme has default font size of 14`() {
        val theme = OverlayThemeSimple.LIGHT
        assertEquals(14, theme.fontSize)
    }

    @Test
    fun `LIGHT theme has default font family`() {
        val theme = OverlayThemeSimple.LIGHT
        assertEquals("default", theme.fontFamily)
    }

    @Test
    fun `LIGHT theme has shadow enabled by default`() {
        val theme = OverlayThemeSimple.LIGHT
        assertTrue(theme.shadowEnabled)
    }

    @Test
    fun `LIGHT theme has correct shadow color`() {
        val theme = OverlayThemeSimple.LIGHT
        assertEquals(0x40000000L, theme.shadowColor)
    }

    // ==================== DARK Theme Tests ====================

    @Test
    fun `DARK theme has correct name`() {
        val theme = OverlayThemeSimple.DARK
        assertEquals("dark", theme.name)
    }

    @Test
    fun `DARK theme has dark gray background color`() {
        val theme = OverlayThemeSimple.DARK
        assertEquals(0xFF212121L, theme.backgroundColor)
    }

    @Test
    fun `DARK theme has white text color`() {
        val theme = OverlayThemeSimple.DARK
        assertEquals(0xFFFFFFFFL, theme.textColor)
    }

    @Test
    fun `DARK theme has light blue accent color`() {
        val theme = OverlayThemeSimple.DARK
        assertEquals(0xFF64B5F6L, theme.accentColor)
    }

    @Test
    fun `DARK theme has dark gray border color`() {
        val theme = OverlayThemeSimple.DARK
        assertEquals(0xFF424242L, theme.borderColor)
    }

    @Test
    fun `DARK theme uses default border width`() {
        val theme = OverlayThemeSimple.DARK
        assertEquals(1, theme.borderWidth)
    }

    // ==================== HIGH_CONTRAST Theme Tests ====================

    @Test
    fun `HIGH_CONTRAST theme has correct name`() {
        val theme = OverlayThemeSimple.HIGH_CONTRAST
        assertEquals("high_contrast", theme.name)
    }

    @Test
    fun `HIGH_CONTRAST theme has black background color`() {
        val theme = OverlayThemeSimple.HIGH_CONTRAST
        assertEquals(0xFF000000L, theme.backgroundColor)
    }

    @Test
    fun `HIGH_CONTRAST theme has white text color`() {
        val theme = OverlayThemeSimple.HIGH_CONTRAST
        assertEquals(0xFFFFFFFFL, theme.textColor)
    }

    @Test
    fun `HIGH_CONTRAST theme has yellow accent color`() {
        val theme = OverlayThemeSimple.HIGH_CONTRAST
        assertEquals(0xFFFFFF00L, theme.accentColor)
    }

    @Test
    fun `HIGH_CONTRAST theme has white border color`() {
        val theme = OverlayThemeSimple.HIGH_CONTRAST
        assertEquals(0xFFFFFFFFL, theme.borderColor)
    }

    @Test
    fun `HIGH_CONTRAST theme has thicker border width of 2`() {
        val theme = OverlayThemeSimple.HIGH_CONTRAST
        assertEquals(2, theme.borderWidth)
    }

    // ==================== Custom Theme Creation Tests ====================

    @Test
    fun `can create custom theme with all parameters`() {
        val customTheme = OverlayThemeSimple(
            name = "custom",
            backgroundColor = 0xFF123456L,
            textColor = 0xFFABCDEFL,
            accentColor = 0xFF789012L,
            borderColor = 0xFF345678L,
            borderWidth = 3,
            cornerRadius = 16,
            fontSize = 18,
            fontFamily = "monospace",
            shadowEnabled = false,
            shadowColor = 0x80FFFFFFL
        )

        assertEquals("custom", customTheme.name)
        assertEquals(0xFF123456L, customTheme.backgroundColor)
        assertEquals(0xFFABCDEFL, customTheme.textColor)
        assertEquals(0xFF789012L, customTheme.accentColor)
        assertEquals(0xFF345678L, customTheme.borderColor)
        assertEquals(3, customTheme.borderWidth)
        assertEquals(16, customTheme.cornerRadius)
        assertEquals(18, customTheme.fontSize)
        assertEquals("monospace", customTheme.fontFamily)
        assertEquals(false, customTheme.shadowEnabled)
        assertEquals(0x80FFFFFFL, customTheme.shadowColor)
    }

    @Test
    fun `custom theme uses defaults for optional properties`() {
        val customTheme = OverlayThemeSimple(
            name = "minimal",
            backgroundColor = 0xFFCCCCCCL,
            textColor = 0xFF333333L,
            accentColor = 0xFF0000FFL,
            borderColor = 0xFF999999L
        )

        // Required values
        assertEquals("minimal", customTheme.name)
        assertEquals(0xFFCCCCCCL, customTheme.backgroundColor)
        assertEquals(0xFF333333L, customTheme.textColor)
        assertEquals(0xFF0000FFL, customTheme.accentColor)
        assertEquals(0xFF999999L, customTheme.borderColor)

        // Default values
        assertEquals(1, customTheme.borderWidth)
        assertEquals(8, customTheme.cornerRadius)
        assertEquals(14, customTheme.fontSize)
        assertEquals("default", customTheme.fontFamily)
        assertTrue(customTheme.shadowEnabled)
        assertEquals(0x40000000L, customTheme.shadowColor)
    }

    // ==================== Data Class Behavior Tests ====================

    @Test
    fun `themes with same values are equal`() {
        val theme1 = OverlayThemeSimple.LIGHT
        val theme2 = OverlayThemeSimple(
            name = "light",
            backgroundColor = 0xFFFFFFFFL,
            textColor = 0xFF000000L,
            accentColor = 0xFF2196F3L,
            borderColor = 0xFFE0E0E0L
        )
        assertEquals(theme1, theme2)
    }

    @Test
    fun `themes with different values are not equal`() {
        val theme1 = OverlayThemeSimple.LIGHT
        val theme2 = OverlayThemeSimple.DARK
        assertNotEquals(theme1, theme2)
    }

    @Test
    fun `copy creates theme with modified properties`() {
        val original = OverlayThemeSimple.LIGHT
        val modified = original.copy(name = "modified_light", fontSize = 20)

        assertEquals("modified_light", modified.name)
        assertEquals(20, modified.fontSize)
        // Preserved values
        assertEquals(original.backgroundColor, modified.backgroundColor)
        assertEquals(original.textColor, modified.textColor)
        assertEquals(original.accentColor, modified.accentColor)
    }

    @Test
    fun `copy preserves unmodified properties`() {
        val original = OverlayThemeSimple.DARK
        val copied = original.copy(borderWidth = 4)

        assertEquals(4, copied.borderWidth)
        assertEquals(original.name, copied.name)
        assertEquals(original.backgroundColor, copied.backgroundColor)
        assertEquals(original.textColor, copied.textColor)
        assertEquals(original.accentColor, copied.accentColor)
        assertEquals(original.borderColor, copied.borderColor)
        assertEquals(original.cornerRadius, copied.cornerRadius)
        assertEquals(original.fontSize, copied.fontSize)
        assertEquals(original.fontFamily, copied.fontFamily)
        assertEquals(original.shadowEnabled, copied.shadowEnabled)
        assertEquals(original.shadowColor, copied.shadowColor)
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun `borderWidth can be zero`() {
        val theme = OverlayThemeSimple(
            name = "no_border",
            backgroundColor = 0xFFFFFFFFL,
            textColor = 0xFF000000L,
            accentColor = 0xFF2196F3L,
            borderColor = 0xFFE0E0E0L,
            borderWidth = 0
        )
        assertEquals(0, theme.borderWidth)
    }

    @Test
    fun `cornerRadius can be zero for sharp corners`() {
        val theme = OverlayThemeSimple(
            name = "sharp",
            backgroundColor = 0xFFFFFFFFL,
            textColor = 0xFF000000L,
            accentColor = 0xFF2196F3L,
            borderColor = 0xFFE0E0E0L,
            cornerRadius = 0
        )
        assertEquals(0, theme.cornerRadius)
    }

    @Test
    fun `fontSize can be large for accessibility`() {
        val theme = OverlayThemeSimple(
            name = "large_text",
            backgroundColor = 0xFFFFFFFFL,
            textColor = 0xFF000000L,
            accentColor = 0xFF2196F3L,
            borderColor = 0xFFE0E0E0L,
            fontSize = 32
        )
        assertEquals(32, theme.fontSize)
    }

    @Test
    fun `transparent shadow color works`() {
        val theme = OverlayThemeSimple(
            name = "no_shadow",
            backgroundColor = 0xFFFFFFFFL,
            textColor = 0xFF000000L,
            accentColor = 0xFF2196F3L,
            borderColor = 0xFFE0E0E0L,
            shadowColor = 0x00000000L
        )
        assertEquals(0x00000000L, theme.shadowColor)
    }
}
