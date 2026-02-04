package animation

import com.augmentalis.avaelements.flutter.animation.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Unit tests for AnimatedSize component
 */
class AnimatedSizeTest {

    @Test
    fun testAnimatedSizeCreation() {
        val animated = AnimatedSize(
            duration = Duration.milliseconds(300),
            child = "Content"
        )

        assertEquals(Duration.milliseconds(300), animated.duration)
        assertEquals("Content", animated.child)
    }

    @Test
    fun testAnimatedSizeWithAlignment() {
        val alignment = AlignmentGeometry.TopCenter
        val animated = AnimatedSize(
            duration = Duration.milliseconds(300),
            alignment = alignment,
            child = "Aligned"
        )

        assertEquals(alignment, animated.alignment)
    }

    @Test
    fun testAnimatedSizeClipBehavior() {
        val clipBehaviors = listOf(
            Clip.None,
            Clip.HardEdge,
            Clip.AntiAlias,
            Clip.AntiAliasWithSaveLayer
        )

        clipBehaviors.forEach { clip ->
            val animated = AnimatedSize(
                duration = Duration.milliseconds(300),
                clipBehavior = clip,
                child = "Clipped"
            )
            assertEquals(clip, animated.clipBehavior)
        }
    }

    @Test
    fun testAnimatedSizeWithCurve() {
        val curve = Curve.FastOutSlowIn
        val animated = AnimatedSize(
            duration = Duration.milliseconds(300),
            curve = curve,
            child = "Curved"
        )

        assertEquals(curve, animated.curve)
    }

    @Test
    fun testAnimatedSizeValidation() {
        assertFailsWith<IllegalArgumentException> {
            AnimatedSize(
                duration = Duration.milliseconds(-100),
                child = "Invalid"
            )
        }
    }

    @Test
    fun testAnimatedSizeOnEndCallback() {
        var callbackInvoked = false
        val animated = AnimatedSize(
            duration = Duration.milliseconds(300),
            child = "Test",
            onEnd = { callbackInvoked = true }
        )

        animated.onEnd?.invoke()
        assertEquals(true, callbackInvoked)
    }

    @Test
    fun testAnimatedSizeDefaultDuration() {
        assertEquals(200, AnimatedSize.DEFAULT_DURATION_MS)
    }
}
