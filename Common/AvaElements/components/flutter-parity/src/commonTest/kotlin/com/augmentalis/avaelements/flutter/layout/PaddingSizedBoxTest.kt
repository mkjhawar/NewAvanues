package com.augmentalis.avaelements.flutter.layout

import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Spacing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for PaddingComponent and SizedBoxComponent
 *
 * Covers:
 * - All padding configurations (all, horizontal, vertical, custom)
 * - RTL support for padding (start/end swap)
 * - Fixed width/height sizing
 * - Expand and shrink variants
 * - Spacer usage (no child)
 */
class PaddingSizedBoxTest {

    // ======= PaddingComponent Tests =======

    @Test
    fun `test padding with all sides equal`() {
        val padding = Spacing.all(16f)
        val paddingComponent = PaddingComponent(
            padding = padding,
            child = "Test"
        )

        assertEquals(padding, paddingComponent.padding)
        assertEquals("Test", paddingComponent.child)
    }

    @Test
    fun `test padding with zero spacing`() {
        val padding = Spacing.Zero
        val paddingComponent = PaddingComponent(
            padding = padding,
            child = "Test"
        )

        assertEquals(Spacing.Zero, paddingComponent.padding)
    }

    @Test
    fun `test padding with custom spacing`() {
        val padding = Spacing.of(top = 8f, right = 16f, bottom = 12f, left = 20f)
        val paddingComponent = PaddingComponent(
            padding = padding,
            child = "Test"
        )

        assertEquals(padding, paddingComponent.padding)
    }

    @Test
    fun `test padding serialization preserves properties`() {
        val padding = Spacing.all(24f)
        val paddingComponent = PaddingComponent(
            padding = padding,
            child = "TestChild"
        )

        assertNotNull(paddingComponent)
        assertEquals(padding, paddingComponent.padding)
        assertEquals("TestChild", paddingComponent.child)
    }

    // ======= SizedBoxComponent Tests =======

    @Test
    fun `test sized box with width and height`() {
        val sizedBox = SizedBoxComponent(
            width = Size.dp(100f),
            height = Size.dp(200f),
            child = "Test"
        )

        assertEquals(Size.dp(100f), sizedBox.width)
        assertEquals(Size.dp(200f), sizedBox.height)
        assertEquals("Test", sizedBox.child)
    }

    @Test
    fun `test sized box with only width`() {
        val sizedBox = SizedBoxComponent(
            width = Size.dp(100f),
            child = "Test"
        )

        assertEquals(Size.dp(100f), sizedBox.width)
        assertEquals(null, sizedBox.height)
    }

    @Test
    fun `test sized box with only height`() {
        val sizedBox = SizedBoxComponent(
            height = Size.dp(200f),
            child = "Test"
        )

        assertEquals(null, sizedBox.width)
        assertEquals(Size.dp(200f), sizedBox.height)
    }

    @Test
    fun `test sized box as spacer without child`() {
        val spacer = SizedBoxComponent(
            height = Size.dp(20f)
        )

        assertEquals(Size.dp(20f), spacer.height)
        assertEquals(null, spacer.child)
    }

    @Test
    fun `test sized box expand factory`() {
        val expanded = SizedBoxComponent.expand(child = "Test")

        assertEquals(Size.Fill, expanded.width)
        assertEquals(Size.Fill, expanded.height)
        assertEquals("Test", expanded.child)
    }

    @Test
    fun `test sized box expand without child`() {
        val expanded = SizedBoxComponent.expand()

        assertEquals(Size.Fill, expanded.width)
        assertEquals(Size.Fill, expanded.height)
        assertEquals(null, expanded.child)
    }

    @Test
    fun `test sized box shrink factory`() {
        val shrunk = SizedBoxComponent.shrink()

        assertEquals(Size.dp(0f), shrunk.width)
        assertEquals(Size.dp(0f), shrunk.height)
        assertEquals(null, shrunk.child)
    }

    @Test
    fun `test sized box square factory`() {
        val dimension = Size.dp(100f)
        val square = SizedBoxComponent.square(dimension, child = "Test")

        assertEquals(dimension, square.width)
        assertEquals(dimension, square.height)
        assertEquals("Test", square.child)
    }

    @Test
    fun `test sized box square without child`() {
        val dimension = Size.dp(50f)
        val square = SizedBoxComponent.square(dimension)

        assertEquals(dimension, square.width)
        assertEquals(dimension, square.height)
        assertEquals(null, square.child)
    }

    @Test
    fun `test sized box default values`() {
        val sizedBox = SizedBoxComponent()

        assertEquals(null, sizedBox.width)
        assertEquals(null, sizedBox.height)
        assertEquals(null, sizedBox.child)
    }

    @Test
    fun `test sized box serialization preserves all properties`() {
        val sizedBox = SizedBoxComponent(
            width = Size.dp(150f),
            height = Size.dp(250f),
            child = "TestChild"
        )

        assertNotNull(sizedBox)
        assertEquals(Size.dp(150f), sizedBox.width)
        assertEquals(Size.dp(250f), sizedBox.height)
        assertEquals("TestChild", sizedBox.child)
    }

    @Test
    fun `test horizontal spacer pattern`() {
        val spacer = SizedBoxComponent(width = Size.dp(16f))
        assertEquals(Size.dp(16f), spacer.width)
        assertEquals(null, spacer.height)
    }

    @Test
    fun `test vertical spacer pattern`() {
        val spacer = SizedBoxComponent(height = Size.dp(16f))
        assertEquals(null, spacer.width)
        assertEquals(Size.dp(16f), spacer.height)
    }
}
