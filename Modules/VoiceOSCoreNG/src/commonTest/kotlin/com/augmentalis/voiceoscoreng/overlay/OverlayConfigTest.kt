/**
 * OverlayConfigTest.kt - TDD tests for overlay configuration system
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-01-06
 */
package com.augmentalis.voiceoscoreng.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.BeforeTest

class OverlayConfigTest {

    private lateinit var config: OverlayConfig

    @BeforeTest
    fun setup() {
        // Reset to fresh config before each test
        config = OverlayConfig()
        config.resetToDefaults()
    }

    // ==================== Default Config Creation Tests ====================

    @Test
    fun `default config has Material3Dark theme`() {
        assertEquals("Material3Dark", config.themeName)
    }

    @Test
    fun `default config has large text disabled`() {
        assertFalse(config.largeText)
    }

    @Test
    fun `default config has high contrast disabled`() {
        assertFalse(config.highContrast)
    }

    @Test
    fun `default config has reduced motion disabled`() {
        assertFalse(config.reducedMotion)
    }

    @Test
    fun `default config has numbers enabled`() {
        assertTrue(config.numbersEnabled)
    }

    @Test
    fun `default config has labels enabled`() {
        assertTrue(config.labelsEnabled)
    }

    @Test
    fun `default config has voice feedback enabled`() {
        assertTrue(config.voiceFeedback)
    }

    @Test
    fun `default config has haptic feedback enabled`() {
        assertTrue(config.hapticFeedback)
    }

    @Test
    fun `default config has no custom primary color`() {
        assertNull(config.customPrimaryColor)
    }

    // ==================== Theme Name Tests ====================

    @Test
    fun `can set and get theme name`() {
        config.themeName = "HighContrast"
        assertEquals("HighContrast", config.themeName)
    }

    @Test
    fun `can set theme to Classic`() {
        config.themeName = "Classic"
        assertEquals("Classic", config.themeName)
    }

    @Test
    fun `setting empty theme name uses default`() {
        config.themeName = ""
        assertEquals("Material3Dark", config.themeName)
    }

    // ==================== Large Text Mode Tests ====================

    @Test
    fun `can enable large text mode`() {
        config.largeText = true
        assertTrue(config.largeText)
    }

    @Test
    fun `large text mode applies 25 percent increase to font scale`() {
        config.largeText = true
        assertEquals(1.25f, config.fontScale, 0.01f)
    }

    @Test
    fun `normal mode has font scale of 1`() {
        config.largeText = false
        assertEquals(1.0f, config.fontScale, 0.01f)
    }

    // ==================== High Contrast Mode Tests ====================

    @Test
    fun `can enable high contrast mode`() {
        config.highContrast = true
        assertTrue(config.highContrast)
    }

    @Test
    fun `high contrast mode returns darker background multiplier`() {
        config.highContrast = true
        assertTrue(config.contrastMultiplier > 1.0f)
    }

    @Test
    fun `normal mode has contrast multiplier of 1`() {
        config.highContrast = false
        assertEquals(1.0f, config.contrastMultiplier, 0.01f)
    }

    // ==================== Reduced Motion Mode Tests ====================

    @Test
    fun `can enable reduced motion mode`() {
        config.reducedMotion = true
        assertTrue(config.reducedMotion)
    }

    @Test
    fun `reduced motion mode disables animations`() {
        config.reducedMotion = true
        assertFalse(config.animationsEnabled)
    }

    @Test
    fun `normal mode has animations enabled`() {
        config.reducedMotion = false
        assertTrue(config.animationsEnabled)
    }

    @Test
    fun `reduced motion affects animation duration`() {
        config.reducedMotion = true
        assertEquals(0L, config.animationDurationMs)
    }

    @Test
    fun `normal mode has default animation duration`() {
        config.reducedMotion = false
        assertEquals(DEFAULT_ANIMATION_DURATION_MS, config.animationDurationMs)
    }

    // ==================== Display Settings Tests ====================

    @Test
    fun `can disable numbers`() {
        config.numbersEnabled = false
        assertFalse(config.numbersEnabled)
    }

    @Test
    fun `can disable labels`() {
        config.labelsEnabled = false
        assertFalse(config.labelsEnabled)
    }

    // ==================== Feedback Settings Tests ====================

    @Test
    fun `can disable voice feedback`() {
        config.voiceFeedback = false
        assertFalse(config.voiceFeedback)
    }

    @Test
    fun `can disable haptic feedback`() {
        config.hapticFeedback = false
        assertFalse(config.hapticFeedback)
    }

    // ==================== Custom Primary Color Tests ====================

    @Test
    fun `can set custom primary color`() {
        val color = 0xFF0000FFu.toLong() // Blue
        config.setCustomPrimaryColor(color)
        assertEquals(color, config.customPrimaryColor)
    }

    @Test
    fun `can clear custom primary color`() {
        config.setCustomPrimaryColor(0xFF0000FFu.toLong())
        config.clearCustomPrimaryColor()
        assertNull(config.customPrimaryColor)
    }

    @Test
    fun `has custom color returns true when set`() {
        config.setCustomPrimaryColor(0xFF00FF00u.toLong())
        assertTrue(config.hasCustomPrimaryColor)
    }

    @Test
    fun `has custom color returns false when not set`() {
        assertFalse(config.hasCustomPrimaryColor)
    }

    // ==================== Config Validation Tests ====================

    @Test
    fun `default config is valid`() {
        val result = config.validate()
        assertTrue(result.isValid)
    }

    @Test
    fun `validation returns errors list when invalid`() {
        val result = config.validate()
        assertNotNull(result.errors)
    }

    @Test
    fun `validation returns warnings list`() {
        val result = config.validate()
        assertNotNull(result.warnings)
    }

    @Test
    fun `validation warns when high contrast and custom color both set`() {
        config.highContrast = true
        config.setCustomPrimaryColor(0xFFFF0000u.toLong())
        val result = config.validate()
        assertTrue(result.warnings.isNotEmpty())
        assertTrue(result.warnings.any { it.contains("custom") || it.contains("contrast") })
    }

    // ==================== Export Config Tests ====================

    @Test
    fun `export config returns map with all settings`() {
        val exported = config.exportToMap()

        assertTrue(exported.containsKey("themeName"))
        assertTrue(exported.containsKey("largeText"))
        assertTrue(exported.containsKey("highContrast"))
        assertTrue(exported.containsKey("reducedMotion"))
        assertTrue(exported.containsKey("numbersEnabled"))
        assertTrue(exported.containsKey("labelsEnabled"))
        assertTrue(exported.containsKey("voiceFeedback"))
        assertTrue(exported.containsKey("hapticFeedback"))
    }

    @Test
    fun `export config contains correct values`() {
        config.themeName = "TestTheme"
        config.largeText = true

        val exported = config.exportToMap()

        assertEquals("TestTheme", exported["themeName"])
        assertEquals(true, exported["largeText"])
    }

    @Test
    fun `export config includes custom primary color when set`() {
        val color = 0xFFFF0000u.toLong()
        config.setCustomPrimaryColor(color)

        val exported = config.exportToMap()

        assertEquals(color, exported["customPrimaryColor"])
    }

    @Test
    fun `export config excludes custom primary color when not set`() {
        val exported = config.exportToMap()

        assertFalse(exported.containsKey("customPrimaryColor"))
    }

    // ==================== Import Config Tests ====================

    @Test
    fun `import config from map restores settings`() {
        val importMap = mapOf(
            "themeName" to "ImportedTheme",
            "largeText" to true,
            "highContrast" to true,
            "reducedMotion" to false,
            "numbersEnabled" to false,
            "labelsEnabled" to true,
            "voiceFeedback" to false,
            "hapticFeedback" to true
        )

        config.importFromMap(importMap)

        assertEquals("ImportedTheme", config.themeName)
        assertTrue(config.largeText)
        assertTrue(config.highContrast)
        assertFalse(config.reducedMotion)
        assertFalse(config.numbersEnabled)
        assertTrue(config.labelsEnabled)
        assertFalse(config.voiceFeedback)
        assertTrue(config.hapticFeedback)
    }

    @Test
    fun `import config handles missing keys gracefully`() {
        val partialMap = mapOf(
            "themeName" to "PartialTheme"
        )

        config.importFromMap(partialMap)

        assertEquals("PartialTheme", config.themeName)
        // Other values should remain at defaults
        assertFalse(config.largeText)
    }

    // ==================== Reset to Defaults Tests ====================

    @Test
    fun `reset to defaults restores theme name`() {
        config.themeName = "CustomTheme"
        config.resetToDefaults()
        assertEquals("Material3Dark", config.themeName)
    }

    @Test
    fun `reset to defaults restores accessibility settings`() {
        config.largeText = true
        config.highContrast = true
        config.reducedMotion = true

        config.resetToDefaults()

        assertFalse(config.largeText)
        assertFalse(config.highContrast)
        assertFalse(config.reducedMotion)
    }

    @Test
    fun `reset to defaults clears custom primary color`() {
        config.setCustomPrimaryColor(0xFFFF0000u.toLong())
        config.resetToDefaults()
        assertNull(config.customPrimaryColor)
    }

    @Test
    fun `reset to defaults restores display settings`() {
        config.numbersEnabled = false
        config.labelsEnabled = false

        config.resetToDefaults()

        assertTrue(config.numbersEnabled)
        assertTrue(config.labelsEnabled)
    }

    @Test
    fun `reset to defaults restores feedback settings`() {
        config.voiceFeedback = false
        config.hapticFeedback = false

        config.resetToDefaults()

        assertTrue(config.voiceFeedback)
        assertTrue(config.hapticFeedback)
    }

    // ==================== Copy Tests ====================

    @Test
    fun `copy creates independent instance`() {
        config.themeName = "Original"
        val copy = config.copy()

        copy.themeName = "Modified"

        assertEquals("Original", config.themeName)
        assertEquals("Modified", copy.themeName)
    }

    @Test
    fun `copy preserves all settings`() {
        config.themeName = "CopyTest"
        config.largeText = true
        config.highContrast = true
        config.setCustomPrimaryColor(0xFF00FFFFu.toLong())

        val copy = config.copy()

        assertEquals("CopyTest", copy.themeName)
        assertTrue(copy.largeText)
        assertTrue(copy.highContrast)
        assertEquals(0xFF00FFFFu.toLong(), copy.customPrimaryColor)
    }

    // ==================== String Export Tests ====================

    @Test
    fun `export config string contains theme name`() {
        config.themeName = "TestTheme"
        val exported = config.exportConfigString()
        assertTrue(exported.contains("TestTheme"))
    }

    @Test
    fun `export config string contains accessibility settings`() {
        config.largeText = true
        config.highContrast = true
        val exported = config.exportConfigString()
        assertTrue(exported.contains("Large Text: true") || exported.contains("largeText"))
    }

    companion object {
        const val DEFAULT_ANIMATION_DURATION_MS = 300L
    }
}

// ==================== ConfigValidationResult Tests ====================

class ConfigValidationResultTest {

    @Test
    fun `valid result has isValid true`() {
        val result = ConfigValidationResult(isValid = true)
        assertTrue(result.isValid)
    }

    @Test
    fun `invalid result has isValid false`() {
        val result = ConfigValidationResult(
            isValid = false,
            errors = listOf("Error 1")
        )
        assertFalse(result.isValid)
    }

    @Test
    fun `result contains errors when invalid`() {
        val result = ConfigValidationResult(
            isValid = false,
            errors = listOf("Invalid theme", "Missing setting")
        )
        assertEquals(2, result.errors.size)
    }

    @Test
    fun `result can contain warnings even when valid`() {
        val result = ConfigValidationResult(
            isValid = true,
            warnings = listOf("Consider changing setting")
        )
        assertTrue(result.isValid)
        assertEquals(1, result.warnings.size)
    }

    @Test
    fun `toString formats valid result correctly`() {
        val result = ConfigValidationResult(isValid = true)
        val str = result.toString()
        assertTrue(str.contains("valid") || str.contains("Valid"))
    }

    @Test
    fun `toString formats invalid result with errors`() {
        val result = ConfigValidationResult(
            isValid = false,
            errors = listOf("Test error")
        )
        val str = result.toString()
        assertTrue(str.contains("Test error"))
    }

    @Test
    fun `toString includes warnings`() {
        val result = ConfigValidationResult(
            isValid = true,
            warnings = listOf("Test warning")
        )
        val str = result.toString()
        assertTrue(str.contains("Test warning"))
    }
}
