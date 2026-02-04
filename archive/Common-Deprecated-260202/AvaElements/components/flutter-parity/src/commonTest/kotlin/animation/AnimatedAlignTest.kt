package animation

import com.augmentalis.avaelements.flutter.animation.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Unit tests for AnimatedAlign component
 */
class AnimatedAlignTest {

    @Test
    fun testAnimatedAlignCreation() {
        val alignment = AlignmentGeometry.Center
        val animated = AnimatedAlign(
            alignment = alignment,
            duration = Duration.milliseconds(400),
            child = "Content"
        )

        assertEquals(alignment, animated.alignment)
        assertEquals(Duration.milliseconds(400), animated.duration)
    }

    @Test
    fun testAnimatedAlignAllPositions() {
        val alignments = listOf(
            AlignmentGeometry.TopLeft,
            AlignmentGeometry.TopCenter,
            AlignmentGeometry.TopRight,
            AlignmentGeometry.CenterLeft,
            AlignmentGeometry.Center,
            AlignmentGeometry.CenterRight,
            AlignmentGeometry.BottomLeft,
            AlignmentGeometry.BottomCenter,
            AlignmentGeometry.BottomRight
        )

        alignments.forEach { align ->
            val animated = AnimatedAlign(
                alignment = align,
                duration = Duration.milliseconds(300),
                child = "Test"
            )
            assertEquals(align, animated.alignment)
        }
    }

    @Test
    fun testAnimatedAlignCustomAlignment() {
        val custom = AlignmentGeometry.Custom(0.5f, -0.5f)
        val animated = AnimatedAlign(
            alignment = custom,
            duration = Duration.milliseconds(300),
            child = "Custom"
        )

        assertEquals(custom, animated.alignment)
    }

    @Test
    fun testAnimatedAlignWithFactors() {
        val animated = AnimatedAlign(
            alignment = AlignmentGeometry.Center,
            duration = Duration.milliseconds(300),
            widthFactor = 0.8f,
            heightFactor = 0.6f,
            child = "Factored"
        )

        assertEquals(0.8f, animated.widthFactor)
        assertEquals(0.6f, animated.heightFactor)
    }

    @Test
    fun testAnimatedAlignFactorValidation() {
        assertFailsWith<IllegalArgumentException> {
            AnimatedAlign(
                alignment = AlignmentGeometry.Center,
                duration = Duration.milliseconds(300),
                widthFactor = -0.5f,
                child = "Invalid"
            )
        }
    }

    @Test
    fun testAnimatedAlignWithCurve() {
        val curve = Curve.EaseInOut
        val animated = AnimatedAlign(
            alignment = AlignmentGeometry.Center,
            duration = Duration.milliseconds(400),
            curve = curve,
            child = "Curved"
        )

        assertEquals(curve, animated.curve)
    }

    @Test
    fun testAnimatedAlignOnEndCallback() {
        var callbackInvoked = false
        val animated = AnimatedAlign(
            alignment = AlignmentGeometry.Center,
            duration = Duration.milliseconds(300),
            child = "Test",
            onEnd = { callbackInvoked = true }
        )

        animated.onEnd?.invoke()
        assertEquals(true, callbackInvoked)
    }

    @Test
    fun testAnimatedAlignDefaultDuration() {
        assertEquals(200, AnimatedAlign.DEFAULT_DURATION_MS)
    }
}
