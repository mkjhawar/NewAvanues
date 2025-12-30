package animation

import com.augmentalis.avaelements.flutter.animation.*
import com.augmentalis.avaelements.core.types.Spacing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Unit tests for AnimatedPadding component
 */
class AnimatedPaddingTest {

    @Test
    fun testAnimatedPaddingCreation() {
        val padding = Spacing.all(16f)
        val animated = AnimatedPadding(
            padding = padding,
            duration = Duration.milliseconds(300),
            child = "Content"
        )

        assertEquals(padding, animated.padding)
        assertEquals(16f, animated.padding.top)
    }

    @Test
    fun testAnimatedPaddingAsymmetric() {
        val padding = Spacing.of(top = 10f, right = 20f, bottom = 10f, left = 20f)
        val animated = AnimatedPadding(
            padding = padding,
            duration = Duration.milliseconds(300),
            child = "Asymmetric"
        )

        assertEquals(10f, animated.padding.top)
        assertEquals(20f, animated.padding.right)
        assertEquals(10f, animated.padding.bottom)
        assertEquals(20f, animated.padding.left)
    }

    @Test
    fun testAnimatedPaddingWithCurve() {
        val curve = Curve.EaseInOut
        val animated = AnimatedPadding(
            padding = Spacing.all(8f),
            duration = Duration.milliseconds(250),
            curve = curve,
            child = "Curved"
        )

        assertEquals(curve, animated.curve)
    }

    @Test
    fun testAnimatedPaddingValidation() {
        assertFailsWith<IllegalArgumentException> {
            AnimatedPadding(
                padding = Spacing.all(16f),
                duration = Duration.milliseconds(0),
                child = "Invalid"
            )
        }
    }

    @Test
    fun testAnimatedPaddingOnEndCallback() {
        var callbackInvoked = false
        val animated = AnimatedPadding(
            padding = Spacing.all(16f),
            duration = Duration.milliseconds(300),
            child = "Test",
            onEnd = { callbackInvoked = true }
        )

        animated.onEnd?.invoke()
        assertEquals(true, callbackInvoked)
    }

    @Test
    fun testAnimatedPaddingDefaultDuration() {
        assertEquals(200, AnimatedPadding.DEFAULT_DURATION_MS)
    }
}
