package com.augmentalis.avaelements.core

import com.augmentalis.avaelements.core.types.*
import kotlin.test.*

/**
 * Test suite for AvaElements Type System
 *
 * Tests all core types: Color, Size, Spacing, Font, Border, Shadow, Gradient, etc.
 * Coverage: Type creation, validation, conversions, edge cases
 */
class TypesTest {

    // ==================== Color Tests ====================

    @Test
    fun should_createColor_when_validRgb() {
        // Given/When
        val color = Color(255, 128, 0)

        // Then
        assertEquals(255, color.red)
        assertEquals(128, color.green)
        assertEquals(0, color.blue)
        assertEquals(1.0f, color.alpha)
    }

    @Test
    fun should_createColor_when_validRgba() {
        // Given/When
        val color = Color(255, 128, 0, 0.5f)

        // Then
        assertEquals(255, color.red)
        assertEquals(128, color.green)
        assertEquals(0, color.blue)
        assertEquals(0.5f, color.alpha)
    }

    @Test
    fun should_throwException_when_invalidRedValue() {
        // When/Then
        assertFailsWith<IllegalArgumentException> {
            Color(-1, 0, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            Color(256, 0, 0)
        }
    }

    @Test
    fun should_throwException_when_invalidGreenValue() {
        // When/Then
        assertFailsWith<IllegalArgumentException> {
            Color(0, -1, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            Color(0, 256, 0)
        }
    }

    @Test
    fun should_throwException_when_invalidBlueValue() {
        // When/Then
        assertFailsWith<IllegalArgumentException> {
            Color(0, 0, -1)
        }
        assertFailsWith<IllegalArgumentException> {
            Color(0, 0, 256)
        }
    }

    @Test
    fun should_throwException_when_invalidAlphaValue() {
        // When/Then
        assertFailsWith<IllegalArgumentException> {
            Color(0, 0, 0, -0.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            Color(0, 0, 0, 1.1f)
        }
    }

    @Test
    fun should_parseHexColor_when_6DigitHex() {
        // Given/When
        val color = Color.hex("#FF8000")

        // Then
        assertEquals(255, color.red)
        assertEquals(128, color.green)
        assertEquals(0, color.blue)
        assertEquals(1.0f, color.alpha)
    }

    @Test
    fun should_parseHexColor_when_8DigitHex() {
        // Given/When
        val color = Color.hex("#FF800080")

        // Then
        assertEquals(255, color.red)
        assertEquals(128, color.green)
        assertEquals(0, color.blue)
        assertEquals(0.5019608f, color.alpha, 0.001f) // 128/255 ≈ 0.502
    }

    @Test
    fun should_parseHexColor_when_withoutHashPrefix() {
        // Given/When
        val color = Color.hex("FF8000")

        // Then
        assertEquals(255, color.red)
        assertEquals(128, color.green)
        assertEquals(0, color.blue)
    }

    @Test
    fun should_throwException_when_invalidHexFormat() {
        // When/Then
        assertFailsWith<IllegalArgumentException> {
            Color.hex("#FFF") // Too short
        }
        assertFailsWith<IllegalArgumentException> {
            Color.hex("#FFFFFFFFF") // Too long
        }
    }

    @Test
    fun should_convertToHex_when_rgbaColor() {
        // Given
        val color = Color(255, 128, 0, 0.5f)

        // When
        val hex = color.toHex()

        // Then
        assertTrue(hex.startsWith("#"))
        assertEquals(9, hex.length) // # + 8 hex digits
    }

    @Test
    fun should_providePredefinedColors_when_usingCompanion() {
        // When/Then
        assertEquals(Color(0, 0, 0, 0.0f), Color.Transparent)
        assertEquals(Color(0, 0, 0), Color.Black)
        assertEquals(Color(255, 255, 255), Color.White)
        assertEquals(Color(255, 0, 0), Color.Red)
        assertEquals(Color(0, 255, 0), Color.Green)
        assertEquals(Color(0, 0, 255), Color.Blue)
    }

    // ==================== Size Tests ====================

    @Test
    fun should_createFixedSize_when_valueProvided() {
        // Given/When
        val size = Size.Fixed(100f, Size.Unit.DP)

        // Then
        assertEquals(100f, size.value)
        assertEquals(Size.Unit.DP, size.unit)
    }

    @Test
    fun should_createPercentSize_when_valueProvided() {
        // Given/When
        val size = Size.Percent(50f)

        // Then
        assertEquals(50f, size.value)
    }

    @Test
    fun should_throwException_when_invalidPercentValue() {
        // When/Then
        assertFailsWith<IllegalArgumentException> {
            Size.Percent(-1f)
        }
        assertFailsWith<IllegalArgumentException> {
            Size.Percent(101f)
        }
    }

    @Test
    fun should_acceptValidPercentRange_when_creating() {
        // Given/When
        val size0 = Size.Percent(0f)
        val size50 = Size.Percent(50f)
        val size100 = Size.Percent(100f)

        // Then
        assertEquals(0f, size0.value)
        assertEquals(50f, size50.value)
        assertEquals(100f, size100.value)
    }

    @Test
    fun should_provideAutoSize_when_requested() {
        // Given/When
        val size = Size.Auto

        // Then
        assertTrue(size is Size.Auto)
    }

    @Test
    fun should_provideFillSize_when_requested() {
        // Given/When
        val size = Size.Fill

        // Then
        assertTrue(size is Size.Fill)
    }

    @Test
    fun should_supportDifferentUnits_when_creatingFixedSize() {
        // Given/When
        val dp = Size.Fixed(100f, Size.Unit.DP)
        val pt = Size.Fixed(100f, Size.Unit.PT)
        val px = Size.Fixed(100f, Size.Unit.PX)
        val sp = Size.Fixed(100f, Size.Unit.SP)

        // Then
        assertEquals(Size.Unit.DP, dp.unit)
        assertEquals(Size.Unit.PT, pt.unit)
        assertEquals(Size.Unit.PX, px.unit)
        assertEquals(Size.Unit.SP, sp.unit)
    }

    // ==================== Spacing Tests ====================

    @Test
    fun should_createSpacing_when_allSidesProvided() {
        // Given/When
        val spacing = Spacing(top = 10f, right = 20f, bottom = 30f, left = 40f)

        // Then
        assertEquals(10f, spacing.top)
        assertEquals(20f, spacing.right)
        assertEquals(30f, spacing.bottom)
        assertEquals(40f, spacing.left)
        assertEquals(Size.Unit.DP, spacing.unit)
    }

    @Test
    fun should_createUniformSpacing_when_usingAll() {
        // Given/When
        val spacing = Spacing.all(16f)

        // Then
        assertEquals(16f, spacing.top)
        assertEquals(16f, spacing.right)
        assertEquals(16f, spacing.bottom)
        assertEquals(16f, spacing.left)
    }

    @Test
    fun should_createSymmetricSpacing_when_usingSymmetric() {
        // Given/When
        val spacing = Spacing.symmetric(vertical = 10f, horizontal = 20f)

        // Then
        assertEquals(10f, spacing.top)
        assertEquals(20f, spacing.right)
        assertEquals(10f, spacing.bottom)
        assertEquals(20f, spacing.left)
    }

    @Test
    fun should_createHorizontalSpacing_when_usingHorizontal() {
        // Given/When
        val spacing = Spacing.horizontal(20f)

        // Then
        assertEquals(0f, spacing.top)
        assertEquals(20f, spacing.right)
        assertEquals(0f, spacing.bottom)
        assertEquals(20f, spacing.left)
    }

    @Test
    fun should_createVerticalSpacing_when_usingVertical() {
        // Given/When
        val spacing = Spacing.vertical(10f)

        // Then
        assertEquals(10f, spacing.top)
        assertEquals(0f, spacing.right)
        assertEquals(10f, spacing.bottom)
        assertEquals(0f, spacing.left)
    }

    @Test
    fun should_provideZeroSpacing_when_usingCompanion() {
        // When/Then
        assertEquals(Spacing(0f, 0f, 0f, 0f), Spacing.Zero)
    }

    // ==================== Font Tests ====================

    @Test
    fun should_createFont_when_defaultValues() {
        // Given/When
        val font = Font()

        // Then
        assertEquals("System", font.family)
        assertEquals(16f, font.size)
        assertEquals(Font.Weight.Regular, font.weight)
        assertEquals(Font.Style.Normal, font.style)
    }

    @Test
    fun should_createFont_when_customValues() {
        // Given/When
        val font = Font(
            family = "Arial",
            size = 24f,
            weight = Font.Weight.Bold,
            style = Font.Style.Italic
        )

        // Then
        assertEquals("Arial", font.family)
        assertEquals(24f, font.size)
        assertEquals(Font.Weight.Bold, font.weight)
        assertEquals(Font.Style.Italic, font.style)
    }

    @Test
    fun should_providePredefinedFonts_when_usingCompanion() {
        // When/Then
        assertEquals(Font(), Font.System)
        assertEquals(24f, Font.Title.size)
        assertEquals(Font.Weight.Bold, Font.Title.weight)
        assertEquals(20f, Font.Heading.size)
        assertEquals(16f, Font.Body.size)
        assertEquals(12f, Font.Caption.size)
    }

    @Test
    fun should_supportAllWeights_when_creating() {
        // When/Then
        val weights = Font.Weight.values()
        assertEquals(9, weights.size)
        assertTrue(weights.contains(Font.Weight.Thin))
        assertTrue(weights.contains(Font.Weight.Bold))
        assertTrue(weights.contains(Font.Weight.Black))
    }

    // ==================== Border Tests ====================

    @Test
    fun should_createBorder_when_defaultValues() {
        // Given/When
        val border = Border()

        // Then
        assertEquals(1f, border.width)
        assertEquals(Color.Black, border.color)
        assertEquals(CornerRadius.Zero, border.radius)
        assertEquals(Border.Style.Solid, border.style)
    }

    @Test
    fun should_createBorder_when_customValues() {
        // Given/When
        val border = Border(
            width = 2f,
            color = Color.Red,
            radius = CornerRadius.all(8f),
            style = Border.Style.Dashed
        )

        // Then
        assertEquals(2f, border.width)
        assertEquals(Color.Red, border.color)
        assertEquals(CornerRadius.all(8f), border.radius)
        assertEquals(Border.Style.Dashed, border.style)
    }

    @Test
    fun should_supportAllBorderStyles_when_creating() {
        // When/Then
        val styles = Border.Style.values()
        assertTrue(styles.contains(Border.Style.Solid))
        assertTrue(styles.contains(Border.Style.Dashed))
        assertTrue(styles.contains(Border.Style.Dotted))
        assertTrue(styles.contains(Border.Style.Double))
        assertTrue(styles.contains(Border.Style.None))
    }

    // ==================== CornerRadius Tests ====================

    @Test
    fun should_createCornerRadius_when_allCornersProvided() {
        // Given/When
        val radius = CornerRadius(
            topLeft = 8f,
            topRight = 16f,
            bottomRight = 24f,
            bottomLeft = 32f
        )

        // Then
        assertEquals(8f, radius.topLeft)
        assertEquals(16f, radius.topRight)
        assertEquals(24f, radius.bottomRight)
        assertEquals(32f, radius.bottomLeft)
    }

    @Test
    fun should_createUniformCornerRadius_when_usingAll() {
        // Given/When
        val radius = CornerRadius.all(12f)

        // Then
        assertEquals(12f, radius.topLeft)
        assertEquals(12f, radius.topRight)
        assertEquals(12f, radius.bottomRight)
        assertEquals(12f, radius.bottomLeft)
    }

    @Test
    fun should_providePredefinedRadii_when_usingCompanion() {
        // When/Then
        assertEquals(CornerRadius(0f, 0f, 0f, 0f), CornerRadius.Zero)
        assertEquals(CornerRadius.all(4f), CornerRadius.Small)
        assertEquals(CornerRadius.all(8f), CornerRadius.Medium)
        assertEquals(CornerRadius.all(16f), CornerRadius.Large)
        assertEquals(CornerRadius.all(24f), CornerRadius.ExtraLarge)
    }

    // ==================== Shadow Tests ====================

    @Test
    fun should_createShadow_when_defaultValues() {
        // Given/When
        val shadow = Shadow()

        // Then
        assertEquals(0f, shadow.offsetX)
        assertEquals(4f, shadow.offsetY)
        assertEquals(8f, shadow.blurRadius)
        assertEquals(0f, shadow.spreadRadius)
        assertEquals(Color(0, 0, 0, 0.25f), shadow.color)
    }

    @Test
    fun should_createShadow_when_customValues() {
        // Given/When
        val shadow = Shadow(
            offsetX = 2f,
            offsetY = 4f,
            blurRadius = 12f,
            spreadRadius = 2f,
            color = Color(0, 0, 0, 0.5f)
        )

        // Then
        assertEquals(2f, shadow.offsetX)
        assertEquals(4f, shadow.offsetY)
        assertEquals(12f, shadow.blurRadius)
        assertEquals(2f, shadow.spreadRadius)
        assertEquals(Color(0, 0, 0, 0.5f), shadow.color)
    }

    // ==================== Gradient Tests ====================

    @Test
    fun should_createLinearGradient_when_colorsProvided() {
        // Given/When
        val gradient = Gradient.Linear(
            colors = listOf(
                Gradient.ColorStop(Color.Red, 0f),
                Gradient.ColorStop(Color.Blue, 1f)
            ),
            angle = 90f
        )

        // Then
        assertEquals(2, gradient.colors.size)
        assertEquals(90f, gradient.angle)
        assertEquals(Color.Red, gradient.colors[0].color)
        assertEquals(0f, gradient.colors[0].position)
        assertEquals(Color.Blue, gradient.colors[1].color)
        assertEquals(1f, gradient.colors[1].position)
    }

    @Test
    fun should_createRadialGradient_when_colorsProvided() {
        // Given/When
        val gradient = Gradient.Radial(
            colors = listOf(
                Gradient.ColorStop(Color.Red, 0f),
                Gradient.ColorStop(Color.Blue, 1f)
            ),
            centerX = 0.5f,
            centerY = 0.5f,
            radius = 1.0f
        )

        // Then
        assertEquals(2, gradient.colors.size)
        assertEquals(0.5f, gradient.centerX)
        assertEquals(0.5f, gradient.centerY)
        assertEquals(1.0f, gradient.radius)
    }

    @Test
    fun should_validateColorStopPosition_when_creating() {
        // When/Then
        assertFailsWith<IllegalArgumentException> {
            Gradient.ColorStop(Color.Red, -0.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            Gradient.ColorStop(Color.Red, 1.1f)
        }
    }

    @Test
    fun should_acceptValidColorStopPosition_when_inRange() {
        // Given/When
        val stop1 = Gradient.ColorStop(Color.Red, 0f)
        val stop2 = Gradient.ColorStop(Color.Green, 0.5f)
        val stop3 = Gradient.ColorStop(Color.Blue, 1f)

        // Then
        assertEquals(0f, stop1.position)
        assertEquals(0.5f, stop2.position)
        assertEquals(1f, stop3.position)
    }

    // ==================== Alignment Tests ====================

    @Test
    fun should_provideAllAlignments_when_usingEnum() {
        // When/Then
        val alignments = Alignment.values()
        assertEquals(9, alignments.size)
        assertTrue(alignments.contains(Alignment.Center))
        assertTrue(alignments.contains(Alignment.TopStart))
        assertTrue(alignments.contains(Alignment.BottomEnd))
    }

    @Test
    fun should_provideConvenienceAliases_when_usingCompanion() {
        // When/Then
        assertEquals(Alignment.CenterStart, Alignment.Start)
        assertEquals(Alignment.CenterEnd, Alignment.End)
        assertEquals(Alignment.TopCenter, Alignment.Top)
        assertEquals(Alignment.BottomCenter, Alignment.Bottom)
    }

    // ==================== Arrangement Tests ====================

    @Test
    fun should_provideAllArrangements_when_usingEnum() {
        // When/Then
        val arrangements = Arrangement.values()
        assertEquals(6, arrangements.size)
        assertTrue(arrangements.contains(Arrangement.Start))
        assertTrue(arrangements.contains(Arrangement.Center))
        assertTrue(arrangements.contains(Arrangement.SpaceBetween))
        assertTrue(arrangements.contains(Arrangement.SpaceEvenly))
    }

    // ==================== Constraints Tests ====================

    @Test
    fun should_createConstraints_when_allProvided() {
        // Given/When
        val constraints = Constraints(
            minWidth = Size.Fixed(100f),
            maxWidth = Size.Fixed(500f),
            minHeight = Size.Fixed(50f),
            maxHeight = Size.Fixed(300f)
        )

        // Then
        assertEquals(Size.Fixed(100f), constraints.minWidth)
        assertEquals(Size.Fixed(500f), constraints.maxWidth)
        assertEquals(Size.Fixed(50f), constraints.minHeight)
        assertEquals(Size.Fixed(300f), constraints.maxHeight)
    }

    // ==================== Animation Tests ====================

    @Test
    fun should_createAnimation_when_defaultValues() {
        // Given/When
        val animation = Animation()

        // Then
        assertEquals(300L, animation.duration)
        assertEquals(Animation.Easing.EaseInOut, animation.easing)
        assertEquals(0L, animation.delay)
    }

    @Test
    fun should_createAnimation_when_customValues() {
        // Given/When
        val animation = Animation(
            duration = 500L,
            easing = Animation.Easing.Spring,
            delay = 100L
        )

        // Then
        assertEquals(500L, animation.duration)
        assertEquals(Animation.Easing.Spring, animation.easing)
        assertEquals(100L, animation.delay)
    }

    // ==================== StateConfig Tests ====================

    @Test
    fun should_createStateConfig_when_defaultOnly() {
        // Given/When
        val config = StateConfig(default = Color.Blue)

        // Then
        assertEquals(Color.Blue, config.default)
        assertNull(config.hover)
        assertNull(config.pressed)
    }

    @Test
    fun should_getStateValue_when_stateDefined() {
        // Given
        val config = StateConfig(
            default = Color.Blue,
            hover = Color.Red,
            pressed = Color.Green
        )

        // When/Then
        assertEquals(Color.Blue, config.get(ComponentState.Default))
        assertEquals(Color.Red, config.get(ComponentState.Hover))
        assertEquals(Color.Green, config.get(ComponentState.Pressed))
    }

    @Test
    fun should_fallbackToDefault_when_stateNotDefined() {
        // Given
        val config = StateConfig(
            default = Color.Blue,
            hover = Color.Red
        )

        // When/Then
        assertEquals(Color.Blue, config.get(ComponentState.Default))
        assertEquals(Color.Red, config.get(ComponentState.Hover))
        assertEquals(Color.Blue, config.get(ComponentState.Pressed)) // Falls back
        assertEquals(Color.Blue, config.get(ComponentState.Disabled)) // Falls back
    }
}
