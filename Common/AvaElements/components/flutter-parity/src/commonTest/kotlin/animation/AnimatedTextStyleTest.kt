package animation

import com.augmentalis.avaelements.flutter.animation.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Unit tests for AnimatedDefaultTextStyle component
 *
 * Tests cover:
 * - Text style animation properties
 * - Font weight, size, color animations
 * - Text alignment and overflow
 * - Validation rules
 *
 * Target: 90%+ code coverage
 */
class AnimatedTextStyleTest {

    @Test
    fun testAnimatedDefaultTextStyleCreation() {
        // Given
        val style = TextStyle(
            fontSize = 24f,
            color = Color.Blue,
            fontWeight = FontWeight.Bold
        )

        // When
        val animated = AnimatedDefaultTextStyle(
            style = style,
            duration = Duration.milliseconds(300),
            child = "Text"
        )

        // Then
        assertEquals(style, animated.style)
        assertEquals(24f, animated.style.fontSize)
        assertEquals(Color.Blue, animated.style.color)
    }

    @Test
    fun testTextStyleWithAllProperties() {
        // When
        val style = TextStyle(
            color = Color.Red,
            fontSize = 18f,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic,
            letterSpacing = 1.5f,
            wordSpacing = 2.0f,
            lineHeight = 1.2f,
            decoration = TextDecoration.Underline,
            decorationColor = Color.Blue,
            decorationStyle = TextDecorationStyle.Solid,
            fontFamily = "Roboto"
        )

        // Then
        assertEquals(Color.Red, style.color)
        assertEquals(18f, style.fontSize)
        assertEquals(FontWeight.SemiBold, style.fontWeight)
        assertEquals(FontStyle.Italic, style.fontStyle)
        assertEquals(1.5f, style.letterSpacing)
        assertEquals(TextDecoration.Underline, style.decoration)
    }

    @Test
    fun testFontWeightValues() {
        // Test all font weight values
        assertEquals(100, FontWeight.Thin.value)
        assertEquals(200, FontWeight.ExtraLight.value)
        assertEquals(300, FontWeight.Light.value)
        assertEquals(400, FontWeight.Normal.value)
        assertEquals(500, FontWeight.Medium.value)
        assertEquals(600, FontWeight.SemiBold.value)
        assertEquals(700, FontWeight.Bold.value)
        assertEquals(800, FontWeight.ExtraBold.value)
        assertEquals(900, FontWeight.Black.value)
    }

    @Test
    fun testTextAlign() {
        // Test different alignments
        val alignments = listOf(
            TextAlign.Left,
            TextAlign.Right,
            TextAlign.Center,
            TextAlign.Justify,
            TextAlign.Start,
            TextAlign.End
        )

        alignments.forEach { align ->
            val animated = AnimatedDefaultTextStyle(
                style = TextStyle(),
                duration = Duration.milliseconds(300),
                textAlign = align,
                child = "Aligned"
            )
            assertEquals(align, animated.textAlign)
        }
    }

    @Test
    fun testTextOverflow() {
        // Test overflow options
        val overflows = listOf(
            TextOverflow.Clip,
            TextOverflow.Fade,
            TextOverflow.Ellipsis,
            TextOverflow.Visible
        )

        overflows.forEach { overflow ->
            val animated = AnimatedDefaultTextStyle(
                style = TextStyle(),
                duration = Duration.milliseconds(300),
                overflow = overflow,
                child = "Overflow"
            )
            assertEquals(overflow, animated.overflow)
        }
    }

    @Test
    fun testTextDecoration() {
        // Test decoration types
        val decorations = listOf(
            TextDecoration.None,
            TextDecoration.Underline,
            TextDecoration.Overline,
            TextDecoration.LineThrough
        )

        decorations.forEach { decoration ->
            val style = TextStyle(decoration = decoration)
            assertEquals(decoration, style.decoration)
        }
    }

    @Test
    fun testTextDecorationStyle() {
        // Test decoration styles
        val styles = listOf(
            TextDecorationStyle.Solid,
            TextDecorationStyle.Double,
            TextDecorationStyle.Dotted,
            TextDecorationStyle.Dashed,
            TextDecorationStyle.Wavy
        )

        styles.forEach { decorationStyle ->
            val style = TextStyle(decorationStyle = decorationStyle)
            assertEquals(decorationStyle, style.decorationStyle)
        }
    }

    @Test
    fun testFontStyle() {
        // Normal
        val normal = TextStyle(fontStyle = FontStyle.Normal)
        assertEquals(FontStyle.Normal, normal.fontStyle)

        // Italic
        val italic = TextStyle(fontStyle = FontStyle.Italic)
        assertEquals(FontStyle.Italic, italic.fontStyle)
    }

    @Test
    fun testMaxLines() {
        // Valid max lines
        val animated = AnimatedDefaultTextStyle(
            style = TextStyle(),
            duration = Duration.milliseconds(300),
            maxLines = 3,
            child = "Multi-line"
        )

        assertEquals(3, animated.maxLines)
    }

    @Test
    fun testMaxLinesValidation() {
        // Should fail with zero or negative max lines
        assertFailsWith<IllegalArgumentException> {
            AnimatedDefaultTextStyle(
                style = TextStyle(),
                duration = Duration.milliseconds(300),
                maxLines = 0,
                child = "Invalid"
            )
        }

        assertFailsWith<IllegalArgumentException> {
            AnimatedDefaultTextStyle(
                style = TextStyle(),
                duration = Duration.milliseconds(300),
                maxLines = -1,
                child = "Invalid"
            )
        }
    }

    @Test
    fun testSoftWrap() {
        // Enabled
        val wrapped = AnimatedDefaultTextStyle(
            style = TextStyle(),
            duration = Duration.milliseconds(300),
            softWrap = true,
            child = "Wrapped"
        )
        assertEquals(true, wrapped.softWrap)

        // Disabled
        val notWrapped = AnimatedDefaultTextStyle(
            style = TextStyle(),
            duration = Duration.milliseconds(300),
            softWrap = false,
            child = "Not Wrapped"
        )
        assertEquals(false, notWrapped.softWrap)
    }

    @Test
    fun testAnimatedDefaultTextStyleCurve() {
        // Given
        val curve = Curve.EaseInOut

        // When
        val animated = AnimatedDefaultTextStyle(
            style = TextStyle(fontSize = 20f),
            duration = Duration.milliseconds(400),
            curve = curve,
            child = "Curved"
        )

        // Then
        assertEquals(curve, animated.curve)
    }

    @Test
    fun testAnimatedDefaultTextStyleOnEndCallback() {
        // Given
        var callbackInvoked = false
        val onEnd = { callbackInvoked = true }

        // When
        val animated = AnimatedDefaultTextStyle(
            style = TextStyle(),
            duration = Duration.milliseconds(300),
            child = "Test",
            onEnd = onEnd
        )

        // Then
        assertNotNull(animated.onEnd)
        animated.onEnd?.invoke()
        assertEquals(true, callbackInvoked)
    }

    @Test
    fun testTextStyleDefault() {
        // When
        val defaultStyle = TextStyle.Default

        // Then
        assertNotNull(defaultStyle)
        assertEquals(null, defaultStyle.color)
        assertEquals(null, defaultStyle.fontSize)
    }
}
