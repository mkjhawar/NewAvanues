/**
 * OverlayThemeTest.kt - Tests for centralized overlay theme configuration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscoreng.overlay

import com.augmentalis.voiceoscoreng.features.OverlayTheme
import com.augmentalis.voiceoscoreng.features.colorWithAlpha
import com.augmentalis.voiceoscoreng.features.extractAlpha
import com.augmentalis.voiceoscoreng.features.extractRed
import com.augmentalis.voiceoscoreng.features.extractGreen
import com.augmentalis.voiceoscoreng.features.extractBlue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class OverlayThemeTest {

    // ==================== Default Theme Tests ====================

    @Test
    fun `default theme has correct primary color`() {
        val theme = OverlayTheme()
        assertEquals(0xFF2196F3, theme.primaryColor)
    }

    @Test
    fun `default theme has correct background color`() {
        val theme = OverlayTheme()
        assertEquals(0xEE1E1E1E, theme.backgroundColor)
    }

    @Test
    fun `default theme has correct backdrop color`() {
        val theme = OverlayTheme()
        assertEquals(0x4D000000, theme.backdropColor) // Black with 0.3 alpha
    }

    @Test
    fun `default theme has correct text colors`() {
        val theme = OverlayTheme()
        assertEquals(0xFFFFFFFF, theme.textPrimaryColor)
        assertEquals(0xB3FFFFFF, theme.textSecondaryColor) // White with 0.7 alpha
        assertEquals(0xFF808080, theme.textDisabledColor)
    }

    @Test
    fun `default theme has correct border colors`() {
        val theme = OverlayTheme()
        assertEquals(0xFFFFFFFF, theme.borderColor)
        assertEquals(0x1AFFFFFF, theme.dividerColor) // White with 0.1 alpha
    }

    // ==================== Badge Color Tests ====================

    @Test
    fun `default theme has correct badge enabled with name color (green)`() {
        val theme = OverlayTheme()
        assertEquals(0xFF4CAF50, theme.badgeEnabledWithNameColor)
    }

    @Test
    fun `default theme has correct badge enabled no name color (orange)`() {
        val theme = OverlayTheme()
        assertEquals(0xFFFF9800, theme.badgeEnabledNoNameColor)
    }

    @Test
    fun `default theme has correct badge disabled color (grey)`() {
        val theme = OverlayTheme()
        assertEquals(0xFF9E9E9E, theme.badgeDisabledColor)
    }

    // ==================== Status Color Tests ====================

    @Test
    fun `default theme has correct status listening color (blue)`() {
        val theme = OverlayTheme()
        assertEquals(0xFF2196F3, theme.statusListeningColor)
    }

    @Test
    fun `default theme has correct status processing color (orange)`() {
        val theme = OverlayTheme()
        assertEquals(0xFFFF9800, theme.statusProcessingColor)
    }

    @Test
    fun `default theme has correct status success color (green)`() {
        val theme = OverlayTheme()
        assertEquals(0xFF4CAF50, theme.statusSuccessColor)
    }

    @Test
    fun `default theme has correct status error color (red)`() {
        val theme = OverlayTheme()
        assertEquals(0xFFF44336, theme.statusErrorColor)
    }

    // ==================== Typography Tests ====================

    @Test
    fun `default theme has correct title font size`() {
        val theme = OverlayTheme()
        assertEquals(16f, theme.titleFontSize)
    }

    @Test
    fun `default theme has correct body font size`() {
        val theme = OverlayTheme()
        assertEquals(14f, theme.bodyFontSize)
    }

    @Test
    fun `default theme has correct caption font size`() {
        val theme = OverlayTheme()
        assertEquals(12f, theme.captionFontSize)
    }

    @Test
    fun `default theme has correct small font size`() {
        val theme = OverlayTheme()
        assertEquals(11f, theme.smallFontSize)
    }

    @Test
    fun `default theme has correct badge font size`() {
        val theme = OverlayTheme()
        assertEquals(14f, theme.badgeFontSize)
    }

    @Test
    fun `default theme has correct instruction font size`() {
        val theme = OverlayTheme()
        assertEquals(16f, theme.instructionFontSize)
    }

    // ==================== Spacing Tests ====================

    @Test
    fun `default theme has correct padding values`() {
        val theme = OverlayTheme()
        assertEquals(4f, theme.paddingSmall)
        assertEquals(8f, theme.paddingMedium)
        assertEquals(16f, theme.paddingLarge)
        assertEquals(24f, theme.paddingXLarge)
    }

    @Test
    fun `default theme has correct spacing values`() {
        val theme = OverlayTheme()
        assertEquals(4f, theme.spacingTiny)
        assertEquals(8f, theme.spacingSmall)
        assertEquals(12f, theme.spacingMedium)
        assertEquals(16f, theme.spacingLarge)
    }

    @Test
    fun `default theme has correct element offsets`() {
        val theme = OverlayTheme()
        assertEquals(4f, theme.badgeOffsetX)
        assertEquals(4f, theme.badgeOffsetY)
        assertEquals(40f, theme.tooltipOffsetY)
    }

    // ==================== Shape Tests ====================

    @Test
    fun `default theme has correct corner radii`() {
        val theme = OverlayTheme()
        assertEquals(6f, theme.cornerRadiusSmall)
        assertEquals(8f, theme.cornerRadiusMedium)
        assertEquals(12f, theme.cornerRadiusLarge)
        assertEquals(24f, theme.cornerRadiusXLarge)
        assertEquals(9999f, theme.cornerRadiusCircle)
    }

    @Test
    fun `default theme has correct elevations`() {
        val theme = OverlayTheme()
        assertEquals(4f, theme.elevationLow)
        assertEquals(8f, theme.elevationMedium)
        assertEquals(16f, theme.elevationHigh)
    }

    @Test
    fun `default theme has correct border widths`() {
        val theme = OverlayTheme()
        assertEquals(1f, theme.borderWidthThin)
        assertEquals(2f, theme.borderWidthMedium)
        assertEquals(3f, theme.borderWidthThick)
    }

    // ==================== Size Tests ====================

    @Test
    fun `default theme has correct badge sizes`() {
        val theme = OverlayTheme()
        assertEquals(32f, theme.badgeSize)
        assertEquals(28f, theme.badgeNumberSize)
    }

    @Test
    fun `default theme has correct icon sizes`() {
        val theme = OverlayTheme()
        assertEquals(16f, theme.iconSizeSmall)
        assertEquals(24f, theme.iconSizeMedium)
        assertEquals(32f, theme.iconSizeLarge)
    }

    @Test
    fun `default theme has correct menu dimensions`() {
        val theme = OverlayTheme()
        assertEquals(200f, theme.menuMinWidth)
        assertEquals(280f, theme.menuMaxWidth)
    }

    @Test
    fun `default theme has correct tooltip max width`() {
        val theme = OverlayTheme()
        assertEquals(200f, theme.tooltipMaxWidth)
    }

    // ==================== Animation Tests ====================

    @Test
    fun `default theme has correct animation durations`() {
        val theme = OverlayTheme()
        assertEquals(150, theme.animationDurationFast)
        assertEquals(200, theme.animationDurationNormal)
        assertEquals(300, theme.animationDurationSlow)
    }

    @Test
    fun `default theme has animation enabled`() {
        val theme = OverlayTheme()
        assertTrue(theme.animationEnabled)
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun `default theme has correct minimum contrast ratio`() {
        val theme = OverlayTheme()
        assertEquals(4.5f, theme.minimumContrastRatio)
    }

    @Test
    fun `default theme has correct focus indicator width`() {
        val theme = OverlayTheme()
        assertEquals(3f, theme.focusIndicatorWidth)
    }

    @Test
    fun `default theme has correct minimum touch target size`() {
        val theme = OverlayTheme()
        assertEquals(48f, theme.minimumTouchTargetSize)
    }

    // ==================== Alpha Values Tests ====================

    @Test
    fun `default theme has correct alpha values`() {
        val theme = OverlayTheme()
        assertEquals(0.5f, theme.alphaDisabled)
        assertEquals(0.7f, theme.alphaSecondary)
        assertEquals(0.6f, theme.alphaHint)
        assertEquals(0.1f, theme.alphaDivider)
        assertEquals(0.3f, theme.alphaBackdrop)
    }

    // ==================== withLargeText Tests ====================

    @Test
    fun `withLargeText increases title font size`() {
        val theme = OverlayTheme()
        val largeTextTheme = theme.withLargeText()
        assertEquals(20f, largeTextTheme.titleFontSize)
    }

    @Test
    fun `withLargeText increases body font size`() {
        val theme = OverlayTheme()
        val largeTextTheme = theme.withLargeText()
        assertEquals(18f, largeTextTheme.bodyFontSize)
    }

    @Test
    fun `withLargeText increases caption font size`() {
        val theme = OverlayTheme()
        val largeTextTheme = theme.withLargeText()
        assertEquals(16f, largeTextTheme.captionFontSize)
    }

    @Test
    fun `withLargeText increases small font size`() {
        val theme = OverlayTheme()
        val largeTextTheme = theme.withLargeText()
        assertEquals(14f, largeTextTheme.smallFontSize)
    }

    @Test
    fun `withLargeText increases badge font size`() {
        val theme = OverlayTheme()
        val largeTextTheme = theme.withLargeText()
        assertEquals(18f, largeTextTheme.badgeFontSize)
    }

    @Test
    fun `withLargeText increases instruction font size`() {
        val theme = OverlayTheme()
        val largeTextTheme = theme.withLargeText()
        assertEquals(20f, largeTextTheme.instructionFontSize)
    }

    @Test
    fun `withLargeText preserves non-font properties`() {
        val theme = OverlayTheme()
        val largeTextTheme = theme.withLargeText()
        assertEquals(theme.primaryColor, largeTextTheme.primaryColor)
        assertEquals(theme.backgroundColor, largeTextTheme.backgroundColor)
        assertEquals(theme.paddingMedium, largeTextTheme.paddingMedium)
    }

    // ==================== toHighContrast Tests ====================

    @Test
    fun `toHighContrast sets background to black`() {
        val theme = OverlayTheme()
        val highContrastTheme = theme.toHighContrast()
        assertEquals(0xFF000000, highContrastTheme.backgroundColor)
    }

    @Test
    fun `toHighContrast sets text primary to white`() {
        val theme = OverlayTheme()
        val highContrastTheme = theme.toHighContrast()
        assertEquals(0xFFFFFFFF, highContrastTheme.textPrimaryColor)
    }

    @Test
    fun `toHighContrast sets text secondary to white`() {
        val theme = OverlayTheme()
        val highContrastTheme = theme.toHighContrast()
        assertEquals(0xFFFFFFFF, highContrastTheme.textSecondaryColor)
    }

    @Test
    fun `toHighContrast increases border width`() {
        val theme = OverlayTheme()
        val highContrastTheme = theme.toHighContrast()
        assertEquals(3f, highContrastTheme.borderWidthMedium)
    }

    @Test
    fun `toHighContrast increases minimum contrast ratio to AAA`() {
        val theme = OverlayTheme()
        val highContrastTheme = theme.toHighContrast()
        assertEquals(7.0f, highContrastTheme.minimumContrastRatio)
    }

    @Test
    fun `toHighContrast preserves other properties`() {
        val theme = OverlayTheme()
        val highContrastTheme = theme.toHighContrast()
        assertEquals(theme.primaryColor, highContrastTheme.primaryColor)
        assertEquals(theme.titleFontSize, highContrastTheme.titleFontSize)
    }

    // ==================== withReducedMotion Tests ====================

    @Test
    fun `withReducedMotion sets fast animation duration to zero`() {
        val theme = OverlayTheme()
        val reducedMotionTheme = theme.withReducedMotion()
        assertEquals(0, reducedMotionTheme.animationDurationFast)
    }

    @Test
    fun `withReducedMotion sets normal animation duration to zero`() {
        val theme = OverlayTheme()
        val reducedMotionTheme = theme.withReducedMotion()
        assertEquals(0, reducedMotionTheme.animationDurationNormal)
    }

    @Test
    fun `withReducedMotion sets slow animation duration to zero`() {
        val theme = OverlayTheme()
        val reducedMotionTheme = theme.withReducedMotion()
        assertEquals(0, reducedMotionTheme.animationDurationSlow)
    }

    @Test
    fun `withReducedMotion disables animation flag`() {
        val theme = OverlayTheme()
        val reducedMotionTheme = theme.withReducedMotion()
        assertFalse(reducedMotionTheme.animationEnabled)
    }

    @Test
    fun `withReducedMotion preserves non-animation properties`() {
        val theme = OverlayTheme()
        val reducedMotionTheme = theme.withReducedMotion()
        assertEquals(theme.primaryColor, reducedMotionTheme.primaryColor)
        assertEquals(theme.titleFontSize, reducedMotionTheme.titleFontSize)
        assertEquals(theme.paddingMedium, reducedMotionTheme.paddingMedium)
    }

    // ==================== withPrimaryColor Tests ====================

    @Test
    fun `withPrimaryColor updates primary color`() {
        val theme = OverlayTheme()
        val customTheme = theme.withPrimaryColor(0xFFFF5722) // Deep Orange
        assertEquals(0xFFFF5722, customTheme.primaryColor)
    }

    @Test
    fun `withPrimaryColor updates status listening color`() {
        val theme = OverlayTheme()
        val customTheme = theme.withPrimaryColor(0xFFFF5722)
        assertEquals(0xFFFF5722, customTheme.statusListeningColor)
    }

    @Test
    fun `withPrimaryColor updates focus indicator color`() {
        val theme = OverlayTheme()
        val customTheme = theme.withPrimaryColor(0xFFFF5722)
        assertEquals(0xFFFF5722, customTheme.focusIndicatorColor)
    }

    @Test
    fun `withPrimaryColor preserves other colors`() {
        val theme = OverlayTheme()
        val customTheme = theme.withPrimaryColor(0xFFFF5722)
        assertEquals(theme.backgroundColor, customTheme.backgroundColor)
        assertEquals(theme.textPrimaryColor, customTheme.textPrimaryColor)
        assertEquals(theme.statusSuccessColor, customTheme.statusSuccessColor)
    }

    // ==================== Validation Tests ====================

    @Test
    fun `default theme passes validation`() {
        val theme = OverlayTheme()
        val result = theme.validate()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `theme with small badge size fails validation`() {
        val theme = OverlayTheme(badgeSize = 20f) // Below 48dp minimum
        val result = theme.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Badge size") })
    }

    @Test
    fun `theme with small body font fails validation`() {
        val theme = OverlayTheme(bodyFontSize = 10f) // Below 12sp minimum
        val result = theme.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Body font size") })
    }

    @Test
    fun `theme with low contrast fails validation`() {
        // Dark text on dark background
        val theme = OverlayTheme(
            textPrimaryColor = 0xFF333333,
            backgroundColor = 0xFF1E1E1E
        )
        val result = theme.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("contrast") })
    }

    @Test
    fun `validation result toString shows success for valid theme`() {
        val theme = OverlayTheme()
        val result = theme.validate()
        assertTrue(result.toString().contains("passes"))
    }

    @Test
    fun `validation result toString shows errors for invalid theme`() {
        val theme = OverlayTheme(bodyFontSize = 8f)
        val result = theme.validate()
        assertTrue(result.toString().contains("failed"))
    }

    // ==================== Data Class Tests ====================

    @Test
    fun `theme equality works correctly`() {
        val theme1 = OverlayTheme()
        val theme2 = OverlayTheme()
        assertEquals(theme1, theme2)
    }

    @Test
    fun `themes with different colors are not equal`() {
        val theme1 = OverlayTheme()
        val theme2 = OverlayTheme(primaryColor = 0xFFFF0000)
        assertNotEquals(theme1, theme2)
    }

    @Test
    fun `copy preserves unchanged properties`() {
        val original = OverlayTheme()
        val copied = original.copy(primaryColor = 0xFFFF0000)
        assertEquals(0xFFFF0000, copied.primaryColor)
        assertEquals(original.backgroundColor, copied.backgroundColor)
        assertEquals(original.titleFontSize, copied.titleFontSize)
    }

    // ==================== Chained Modifier Tests ====================

    @Test
    fun `modifiers can be chained`() {
        val theme = OverlayTheme()
            .withLargeText()
            .toHighContrast()
            .withPrimaryColor(0xFFFF5722)

        // Large text
        assertEquals(20f, theme.titleFontSize)
        // High contrast
        assertEquals(0xFF000000, theme.backgroundColor)
        assertEquals(7.0f, theme.minimumContrastRatio)
        // Primary color
        assertEquals(0xFFFF5722, theme.primaryColor)
    }

    @Test
    fun `chained modifiers with reduced motion`() {
        val theme = OverlayTheme()
            .withLargeText()
            .withReducedMotion()

        assertEquals(20f, theme.titleFontSize)
        assertEquals(0, theme.animationDurationNormal)
        assertFalse(theme.animationEnabled)
    }

    // ==================== Color Helper Tests ====================

    @Test
    fun `colorWithAlpha creates correct color`() {
        // White with 50% alpha should be 0x80FFFFFF
        val result = colorWithAlpha(0xFFFFFFFF, 0.5f)
        assertEquals(0x80FFFFFF, result)
    }

    @Test
    fun `colorWithAlpha handles zero alpha`() {
        val result = colorWithAlpha(0xFFFF0000, 0.0f)
        assertEquals(0x00FF0000, result)
    }

    @Test
    fun `colorWithAlpha handles full alpha`() {
        val result = colorWithAlpha(0xFFFF0000, 1.0f)
        assertEquals(0xFFFF0000, result)
    }

    @Test
    fun `extractAlpha returns correct alpha value`() {
        val alpha = extractAlpha(0x80FFFFFF)
        assertEquals(0.5f, alpha, 0.01f)
    }

    @Test
    fun `extractRed returns correct red value`() {
        val red = extractRed(0xFFFF8800)
        assertEquals(1.0f, red, 0.01f)
    }

    @Test
    fun `extractGreen returns correct green value`() {
        val green = extractGreen(0xFF00FF00)
        assertEquals(1.0f, green, 0.01f)
    }

    @Test
    fun `extractBlue returns correct blue value`() {
        val blue = extractBlue(0xFF0000FF)
        assertEquals(1.0f, blue, 0.01f)
    }

    // ==================== Companion Object Tests ====================

    @Test
    fun `DEFAULT is accessible`() {
        val theme = OverlayTheme.DEFAULT
        assertEquals(0xFF2196F3, theme.primaryColor)
    }

    @Test
    fun `DARK theme has dark background`() {
        val theme = OverlayTheme.DARK
        assertEquals(0xFF121212, theme.backgroundColor)
    }

    @Test
    fun `HIGH_CONTRAST theme has high contrast values`() {
        val theme = OverlayTheme.HIGH_CONTRAST
        assertEquals(0xFF000000, theme.backgroundColor)
        assertEquals(7.0f, theme.minimumContrastRatio)
    }
}
