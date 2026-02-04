package animation

import com.augmentalis.avaelements.flutter.animation.*
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Unit tests for AnimatedContainer component
 *
 * Tests cover:
 * - Component initialization and validation
 * - Property animations (size, color, padding)
 * - Duration and curve configuration
 * - Edge cases and error handling
 *
 * Target: 90%+ code coverage
 */
class AnimatedContainerTest {

    @Test
    fun testAnimatedContainerCreation() {
        // Given
        val duration = Duration.milliseconds(300)
        val color = Color.Blue

        // When
        val container = AnimatedContainer(
            duration = duration,
            color = color,
            width = Size.dp(100f),
            height = Size.dp(100f),
            child = "Text"
        )

        // Then
        assertEquals(duration, container.duration)
        assertEquals(color, container.color)
        assertEquals(Size.dp(100f), container.width)
        assertEquals(Size.dp(100f), container.height)
        assertEquals("Text", container.child)
    }

    @Test
    fun testAnimatedContainerWithPadding() {
        // Given
        val padding = Spacing.all(16f)
        val duration = Duration.milliseconds(200)

        // When
        val container = AnimatedContainer(
            duration = duration,
            padding = padding,
            child = "Content"
        )

        // Then
        assertEquals(padding, container.padding)
        assertEquals(16f, container.padding?.top)
        assertEquals(16f, container.padding?.left)
    }

    @Test
    fun testAnimatedContainerWithDecoration() {
        // Given
        val decoration = BoxDecoration(
            color = Color.Red,
            borderRadius = BorderRadius.circular(8f),
            border = Border.all(BorderSide(Color.Black, 2f))
        )

        // When
        val container = AnimatedContainer(
            duration = Duration.milliseconds(300),
            decoration = decoration,
            child = "Decorated"
        )

        // Then
        assertEquals(decoration, container.decoration)
        assertNotNull(container.decoration?.borderRadius)
        assertEquals(8f, container.decoration?.borderRadius?.topLeft)
    }

    @Test
    fun testAnimatedContainerCurves() {
        // Test different animation curves
        val curves = listOf(
            Curve.Linear,
            Curve.EaseIn,
            Curve.EaseOut,
            Curve.EaseInOut,
            Curve.FastOutSlowIn
        )

        curves.forEach { curve ->
            val container = AnimatedContainer(
                duration = Duration.milliseconds(300),
                curve = curve,
                child = "Test"
            )
            assertEquals(curve, container.curve)
        }
    }

    @Test
    fun testAnimatedContainerValidation_PositiveDuration() {
        // Should fail with zero or negative duration
        assertFailsWith<IllegalArgumentException> {
            AnimatedContainer(
                duration = Duration.milliseconds(0),
                child = "Test"
            )
        }

        assertFailsWith<IllegalArgumentException> {
            AnimatedContainer(
                duration = Duration.milliseconds(-100),
                child = "Test"
            )
        }
    }

    @Test
    fun testAnimatedContainerColorAndDecorationValidation() {
        // Should fail when both color and decoration.color are specified
        assertFailsWith<IllegalArgumentException> {
            AnimatedContainer(
                duration = Duration.milliseconds(300),
                color = Color.Blue,
                decoration = BoxDecoration(color = Color.Red),
                child = "Test"
            )
        }
    }

    @Test
    fun testAnimatedContainerWithMargin() {
        // Given
        val margin = Spacing.of(top = 10f, right = 20f, bottom = 10f, left = 20f)

        // When
        val container = AnimatedContainer(
            duration = Duration.milliseconds(300),
            margin = margin,
            child = "Margined"
        )

        // Then
        assertEquals(margin, container.margin)
        assertEquals(10f, container.margin?.top)
        assertEquals(20f, container.margin?.right)
    }

    @Test
    fun testAnimatedContainerOnEndCallback() {
        // Given
        var callbackInvoked = false
        val onEnd = { callbackInvoked = true }

        // When
        val container = AnimatedContainer(
            duration = Duration.milliseconds(300),
            child = "Test",
            onEnd = onEnd
        )

        // Then
        assertNotNull(container.onEnd)
        container.onEnd?.invoke()
        assertEquals(true, callbackInvoked)
    }

    @Test
    fun testDurationHelpers() {
        // Test Duration.milliseconds
        val duration1 = Duration.milliseconds(500)
        assertEquals(500, duration1.milliseconds)

        // Test Duration.seconds
        val duration2 = Duration.seconds(2)
        assertEquals(2000, duration2.milliseconds)

        // Test Duration.zero
        assertEquals(0, Duration.zero.milliseconds)
    }

    @Test
    fun testColorHelpers() {
        // Test Color.rgb
        val color1 = Color.rgb(255, 100, 50)
        assertEquals(255, color1.red)
        assertEquals(100, color1.green)
        assertEquals(50, color1.blue)
        assertEquals(255, color1.alpha)

        // Test Color.argb
        val color2 = Color.argb(128, 255, 100, 50)
        assertEquals(128, color2.alpha)

        // Test predefined colors
        assertEquals(Color(255, 0, 0), Color.Red)
        assertEquals(Color(0, 255, 0), Color.Green)
        assertEquals(Color(0, 0, 255), Color.Blue)
    }

    @Test
    fun testColorValidation() {
        // Valid colors
        Color(0, 0, 0, 0)
        Color(255, 255, 255, 255)

        // Invalid colors
        assertFailsWith<IllegalArgumentException> {
            Color(-1, 0, 0)
        }

        assertFailsWith<IllegalArgumentException> {
            Color(0, 256, 0)
        }

        assertFailsWith<IllegalArgumentException> {
            Color(0, 0, 0, 300)
        }
    }

    @Test
    fun testAlignmentGeometry() {
        // Test predefined alignments
        val topLeft = AlignmentGeometry.TopLeft
        val center = AlignmentGeometry.Center
        val bottomRight = AlignmentGeometry.BottomRight

        assertNotNull(topLeft)
        assertNotNull(center)
        assertNotNull(bottomRight)

        // Test custom alignment
        val custom = AlignmentGeometry.Custom(0.5f, -0.5f)
        assertEquals(0.5f, custom.x)
        assertEquals(-0.5f, custom.y)
    }

    @Test
    fun testAlignmentValidation() {
        // Valid alignment
        AlignmentGeometry.Custom(0f, 0f)
        AlignmentGeometry.Custom(-1f, 1f)

        // Invalid alignment (out of bounds)
        assertFailsWith<IllegalArgumentException> {
            AlignmentGeometry.Custom(1.5f, 0f)
        }

        assertFailsWith<IllegalArgumentException> {
            AlignmentGeometry.Custom(0f, -1.5f)
        }
    }

    @Test
    fun testBorderRadiusHelpers() {
        // Test circular
        val circular = BorderRadius.circular(10f)
        assertEquals(10f, circular.topLeft)
        assertEquals(10f, circular.topRight)
        assertEquals(10f, circular.bottomRight)
        assertEquals(10f, circular.bottomLeft)

        // Test all
        val all = BorderRadius.all(15f)
        assertEquals(15f, all.topLeft)
        assertEquals(15f, all.bottomRight)
    }

    @Test
    fun testBoxShadow() {
        // Given
        val shadow = BoxShadow(
            color = Color.Black,
            offset = Offset(4f, 4f),
            blurRadius = 8f,
            spreadRadius = 2f
        )

        // Then
        assertEquals(Color.Black, shadow.color)
        assertEquals(4f, shadow.offset.x)
        assertEquals(4f, shadow.offset.y)
        assertEquals(8f, shadow.blurRadius)
        assertEquals(2f, shadow.spreadRadius)
    }

    @Test
    fun testGradientLinear() {
        // Given
        val gradient = Gradient.Linear(
            colors = listOf(Color.Red, Color.Blue),
            begin = AlignmentGeometry.TopCenter,
            end = AlignmentGeometry.BottomCenter
        )

        // Then
        assertEquals(2, gradient.colors.size)
        assertEquals(Color.Red, gradient.colors[0])
        assertEquals(Color.Blue, gradient.colors[1])
    }

    @Test
    fun testGradientRadial() {
        // Given
        val gradient = Gradient.Radial(
            colors = listOf(Color.White, Color.Black),
            center = AlignmentGeometry.Center,
            radius = 0.8f
        )

        // Then
        assertEquals(2, gradient.colors.size)
        assertEquals(0.8f, gradient.radius)
    }

    @Test
    fun testMatrix4Identity() {
        // When
        val identity = Matrix4.identity()

        // Then
        assertEquals(16, identity.values.size)
        assertEquals(1f, identity.values[0])  // [0,0]
        assertEquals(1f, identity.values[5])  // [1,1]
        assertEquals(1f, identity.values[10]) // [2,2]
        assertEquals(1f, identity.values[15]) // [3,3]
    }

    @Test
    fun testMatrix4Validation() {
        // Valid matrix
        Matrix4.identity()

        // Invalid matrix (wrong size)
        assertFailsWith<IllegalArgumentException> {
            Matrix4(listOf(1f, 2f, 3f)) // Only 3 values instead of 16
        }
    }
}
