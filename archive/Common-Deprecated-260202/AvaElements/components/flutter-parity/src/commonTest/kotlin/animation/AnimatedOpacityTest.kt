package animation

import com.augmentalis.avaelements.flutter.animation.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Unit tests for AnimatedOpacity component
 *
 * Tests cover:
 * - Opacity animation properties
 * - Range validation (0.0 - 1.0)
 * - Duration configuration
 * - Callback handling
 *
 * Target: 90%+ code coverage
 */
class AnimatedOpacityTest {

    @Test
    fun testAnimatedOpacityCreation() {
        // Given
        val opacity = 0.5f
        val duration = Duration.milliseconds(500)

        // When
        val animated = AnimatedOpacity(
            opacity = opacity,
            duration = duration,
            child = "Content"
        )

        // Then
        assertEquals(opacity, animated.opacity)
        assertEquals(duration, animated.duration)
        assertEquals("Content", animated.child)
    }

    @Test
    fun testAnimatedOpacityFullyVisible() {
        // When
        val animated = AnimatedOpacity(
            opacity = 1.0f,
            duration = Duration.milliseconds(300),
            child = "Visible"
        )

        // Then
        assertEquals(1.0f, animated.opacity)
    }

    @Test
    fun testAnimatedOpacityFullyTransparent() {
        // When
        val animated = AnimatedOpacity(
            opacity = 0.0f,
            duration = Duration.milliseconds(300),
            child = "Invisible"
        )

        // Then
        assertEquals(0.0f, animated.opacity)
    }

    @Test
    fun testAnimatedOpacityWithCurve() {
        // Given
        val curve = Curve.EaseInOut

        // When
        val animated = AnimatedOpacity(
            opacity = 0.8f,
            duration = Duration.milliseconds(400),
            curve = curve,
            child = "Content"
        )

        // Then
        assertEquals(curve, animated.curve)
    }

    @Test
    fun testAnimatedOpacityValidation_OpacityRange() {
        // Should fail with opacity < 0.0
        assertFailsWith<IllegalArgumentException> {
            AnimatedOpacity(
                opacity = -0.1f,
                duration = Duration.milliseconds(300),
                child = "Test"
            )
        }

        // Should fail with opacity > 1.0
        assertFailsWith<IllegalArgumentException> {
            AnimatedOpacity(
                opacity = 1.1f,
                duration = Duration.milliseconds(300),
                child = "Test"
            )
        }
    }

    @Test
    fun testAnimatedOpacityValidation_PositiveDuration() {
        // Should fail with zero duration
        assertFailsWith<IllegalArgumentException> {
            AnimatedOpacity(
                opacity = 0.5f,
                duration = Duration.milliseconds(0),
                child = "Test"
            )
        }

        // Should fail with negative duration
        assertFailsWith<IllegalArgumentException> {
            AnimatedOpacity(
                opacity = 0.5f,
                duration = Duration.milliseconds(-100),
                child = "Test"
            )
        }
    }

    @Test
    fun testAnimatedOpacityOnEndCallback() {
        // Given
        var callbackInvoked = false
        val onEnd = { callbackInvoked = true }

        // When
        val animated = AnimatedOpacity(
            opacity = 0.5f,
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
    fun testAnimatedOpacityAlwaysIncludeSemantics() {
        // When
        val animated = AnimatedOpacity(
            opacity = 0.0f,
            duration = Duration.milliseconds(300),
            child = "Hidden but accessible",
            alwaysIncludeSemantics = true
        )

        // Then
        assertEquals(true, animated.alwaysIncludeSemantics)
    }

    @Test
    fun testAnimatedOpacityDefaultDuration() {
        // Then
        assertEquals(200, AnimatedOpacity.DEFAULT_DURATION_MS)
    }

    @Test
    fun testAnimatedOpacityBoundaryValues() {
        // Test exact boundary values
        AnimatedOpacity(
            opacity = 0.0f,
            duration = Duration.milliseconds(100),
            child = "Zero"
        )

        AnimatedOpacity(
            opacity = 1.0f,
            duration = Duration.milliseconds(100),
            child = "One"
        )
    }

    @Test
    fun testAnimatedOpacityTypicalFadeIn() {
        // Simulate a fade-in animation
        val fadeIn = AnimatedOpacity(
            opacity = 1.0f,
            duration = Duration.milliseconds(500),
            curve = Curve.EaseIn,
            child = "Fading In"
        )

        assertEquals(1.0f, fadeIn.opacity)
        assertEquals(Curve.EaseIn, fadeIn.curve)
    }

    @Test
    fun testAnimatedOpacityTypicalFadeOut() {
        // Simulate a fade-out animation
        val fadeOut = AnimatedOpacity(
            opacity = 0.0f,
            duration = Duration.milliseconds(500),
            curve = Curve.EaseOut,
            child = "Fading Out"
        )

        assertEquals(0.0f, fadeOut.opacity)
        assertEquals(Curve.EaseOut, fadeOut.curve)
    }
}
