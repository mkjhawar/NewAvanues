package animation

import com.augmentalis.avaelements.flutter.animation.*
import com.augmentalis.avaelements.core.types.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Unit tests for AnimatedPositioned component
 *
 * Tests cover:
 * - Position animation (left, top, right, bottom)
 * - Size constraints
 * - Validation rules
 * - Helper factories
 *
 * Target: 90%+ code coverage
 */
class AnimatedPositionedTest {

    @Test
    fun testAnimatedPositionedWithLeftTop() {
        // Given
        val left = Size.dp(10f)
        val top = Size.dp(20f)

        // When
        val positioned = AnimatedPositioned(
            duration = Duration.milliseconds(300),
            left = left,
            top = top,
            child = "Content"
        )

        // Then
        assertEquals(left, positioned.left)
        assertEquals(top, positioned.top)
    }

    @Test
    fun testAnimatedPositionedWithRightBottom() {
        // Given
        val right = Size.dp(10f)
        val bottom = Size.dp(20f)

        // When
        val positioned = AnimatedPositioned(
            duration = Duration.milliseconds(300),
            right = right,
            bottom = bottom,
            child = "Content"
        )

        // Then
        assertEquals(right, positioned.right)
        assertEquals(bottom, positioned.bottom)
    }

    @Test
    fun testAnimatedPositionedWithSize() {
        // Given
        val width = Size.dp(100f)
        val height = Size.dp(100f)

        // When
        val positioned = AnimatedPositioned(
            duration = Duration.milliseconds(300),
            left = Size.dp(0f),
            top = Size.dp(0f),
            width = width,
            height = height,
            child = "Sized"
        )

        // Then
        assertEquals(width, positioned.width)
        assertEquals(height, positioned.height)
    }

    @Test
    fun testAnimatedPositionedValidation_RequiresPosition() {
        // Should fail with no position specified
        assertFailsWith<IllegalArgumentException> {
            AnimatedPositioned(
                duration = Duration.milliseconds(300),
                child = "No Position"
            )
        }
    }

    @Test
    fun testAnimatedPositionedValidation_LeftRightWithWidth() {
        // Should fail when left, right, and width are all specified
        assertFailsWith<IllegalArgumentException> {
            AnimatedPositioned(
                duration = Duration.milliseconds(300),
                left = Size.dp(0f),
                right = Size.dp(0f),
                width = Size.dp(100f),
                child = "Invalid"
            )
        }
    }

    @Test
    fun testAnimatedPositionedValidation_TopBottomWithHeight() {
        // Should fail when top, bottom, and height are all specified
        assertFailsWith<IllegalArgumentException> {
            AnimatedPositioned(
                duration = Duration.milliseconds(300),
                top = Size.dp(0f),
                bottom = Size.dp(0f),
                height = Size.dp(100f),
                child = "Invalid"
            )
        }
    }

    @Test
    fun testAnimatedPositionedFill() {
        // When
        val filled = AnimatedPositioned.fill(
            duration = Duration.milliseconds(300),
            child = "Fill Parent"
        )

        // Then
        assertEquals(Size.dp(0f), filled.left)
        assertEquals(Size.dp(0f), filled.top)
        assertEquals(Size.dp(0f), filled.right)
        assertEquals(Size.dp(0f), filled.bottom)
    }

    @Test
    fun testAnimatedPositionedFromRect() {
        // Given
        val rect = Rect.fromLTWH(10f, 20f, 100f, 50f)

        // When
        val positioned = AnimatedPositioned.fromRect(
            duration = Duration.milliseconds(300),
            rect = rect,
            child = "From Rect"
        )

        // Then
        assertEquals(Size.dp(10f), positioned.left)
        assertEquals(Size.dp(20f), positioned.top)
        assertEquals(Size.dp(100f), positioned.width)
        assertEquals(Size.dp(50f), positioned.height)
    }

    @Test
    fun testAnimatedPositionedWithCurve() {
        // Given
        val curve = Curve.FastOutSlowIn

        // When
        val positioned = AnimatedPositioned(
            duration = Duration.milliseconds(500),
            curve = curve,
            left = Size.dp(10f),
            top = Size.dp(10f),
            child = "Curved"
        )

        // Then
        assertEquals(curve, positioned.curve)
    }

    @Test
    fun testAnimatedPositionedOnEndCallback() {
        // Given
        var callbackInvoked = false
        val onEnd = { callbackInvoked = true }

        // When
        val positioned = AnimatedPositioned(
            duration = Duration.milliseconds(300),
            left = Size.dp(0f),
            child = "Test",
            onEnd = onEnd
        )

        // Then
        assertNotNull(positioned.onEnd)
        positioned.onEnd?.invoke()
        assertEquals(true, callbackInvoked)
    }

    @Test
    fun testRectFromLTWH() {
        // When
        val rect = Rect.fromLTWH(10f, 20f, 100f, 50f)

        // Then
        assertEquals(10f, rect.left)
        assertEquals(20f, rect.top)
        assertEquals(100f, rect.width)
        assertEquals(50f, rect.height)
        assertEquals(110f, rect.right)
        assertEquals(70f, rect.bottom)
    }

    @Test
    fun testRectFromLTRB() {
        // When
        val rect = Rect.fromLTRB(10f, 20f, 110f, 70f)

        // Then
        assertEquals(10f, rect.left)
        assertEquals(20f, rect.top)
        assertEquals(110f, rect.right)
        assertEquals(70f, rect.bottom)
        assertEquals(100f, rect.width)
        assertEquals(50f, rect.height)
    }

    @Test
    fun testAnimatedPositionedValidLeftRight() {
        // Valid: left and right without width
        AnimatedPositioned(
            duration = Duration.milliseconds(300),
            left = Size.dp(10f),
            right = Size.dp(10f),
            child = "Stretch Horizontal"
        )
    }

    @Test
    fun testAnimatedPositionedValidTopBottom() {
        // Valid: top and bottom without height
        AnimatedPositioned(
            duration = Duration.milliseconds(300),
            top = Size.dp(10f),
            bottom = Size.dp(10f),
            child = "Stretch Vertical"
        )
    }
}
