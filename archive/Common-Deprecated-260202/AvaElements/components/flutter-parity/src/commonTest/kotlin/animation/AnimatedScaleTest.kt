package animation

import com.augmentalis.avaelements.flutter.animation.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Unit tests for AnimatedScale component
 */
class AnimatedScaleTest {

    @Test
    fun testAnimatedScaleCreation() {
        val animated = AnimatedScale(
            scale = 1.5f,
            duration = Duration.milliseconds(300),
            child = "Content"
        )

        assertEquals(1.5f, animated.scale)
        assertEquals(Duration.milliseconds(300), animated.duration)
    }

    @Test
    fun testAnimatedScaleNormalSize() {
        val animated = AnimatedScale(
            scale = 1.0f,
            duration = Duration.milliseconds(300),
            child = "Normal"
        )

        assertEquals(1.0f, animated.scale)
    }

    @Test
    fun testAnimatedScaleHalfSize() {
        val animated = AnimatedScale(
            scale = 0.5f,
            duration = Duration.milliseconds(300),
            child = "Half"
        )

        assertEquals(0.5f, animated.scale)
    }

    @Test
    fun testAnimatedScaleDoubleSize() {
        val animated = AnimatedScale(
            scale = 2.0f,
            duration = Duration.milliseconds(300),
            child = "Double"
        )

        assertEquals(2.0f, animated.scale)
    }

    @Test
    fun testAnimatedScaleZero() {
        val animated = AnimatedScale(
            scale = 0.0f,
            duration = Duration.milliseconds(300),
            child = "Zero"
        )

        assertEquals(0.0f, animated.scale)
    }

    @Test
    fun testAnimatedScaleValidation() {
        assertFailsWith<IllegalArgumentException> {
            AnimatedScale(
                scale = -0.5f,
                duration = Duration.milliseconds(300),
                child = "Negative"
            )
        }
    }

    @Test
    fun testAnimatedScaleWithAlignment() {
        val alignment = AlignmentGeometry.TopLeft
        val animated = AnimatedScale(
            scale = 1.5f,
            duration = Duration.milliseconds(300),
            alignment = alignment,
            child = "Aligned"
        )

        assertEquals(alignment, animated.alignment)
    }

    @Test
    fun testAnimatedScaleFilterQuality() {
        val qualities = listOf(
            FilterQuality.None,
            FilterQuality.Low,
            FilterQuality.Medium,
            FilterQuality.High
        )

        qualities.forEach { quality ->
            val animated = AnimatedScale(
                scale = 2.0f,
                duration = Duration.milliseconds(300),
                filterQuality = quality,
                child = "Filtered"
            )
            assertEquals(quality, animated.filterQuality)
        }
    }

    @Test
    fun testAnimatedScaleWithCurve() {
        val curve = Curve.EaseInOut
        val animated = AnimatedScale(
            scale = 1.2f,
            duration = Duration.milliseconds(300),
            curve = curve,
            child = "Curved"
        )

        assertEquals(curve, animated.curve)
    }

    @Test
    fun testAnimatedScaleOnEndCallback() {
        var callbackInvoked = false
        val animated = AnimatedScale(
            scale = 1.5f,
            duration = Duration.milliseconds(300),
            child = "Test",
            onEnd = { callbackInvoked = true }
        )

        animated.onEnd?.invoke()
        assertEquals(true, callbackInvoked)
    }

    @Test
    fun testAnimatedScaleDefaultDuration() {
        assertEquals(200, AnimatedScale.DEFAULT_DURATION_MS)
    }
}
