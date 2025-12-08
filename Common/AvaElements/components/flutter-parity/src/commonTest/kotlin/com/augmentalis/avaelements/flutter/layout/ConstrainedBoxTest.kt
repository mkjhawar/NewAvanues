package com.augmentalis.avaelements.flutter.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for ConstrainedBoxComponent and BoxConstraints
 *
 * Covers:
 * - Min/max width and height constraints
 * - Tight, loose, and custom constraints
 * - Constraint validation
 * - Constraint checking utilities
 * - Edge cases (zero, infinity)
 */
class ConstrainedBoxTest {

    // ======= BoxConstraints Tests =======

    @Test
    fun `test default box constraints`() {
        val constraints = BoxConstraints()

        assertEquals(0f, constraints.minWidth)
        assertEquals(Float.POSITIVE_INFINITY, constraints.maxWidth)
        assertEquals(0f, constraints.minHeight)
        assertEquals(Float.POSITIVE_INFINITY, constraints.maxHeight)
    }

    @Test
    fun `test box constraints with custom values`() {
        val constraints = BoxConstraints(
            minWidth = 100f,
            maxWidth = 300f,
            minHeight = 50f,
            maxHeight = 200f
        )

        assertEquals(100f, constraints.minWidth)
        assertEquals(300f, constraints.maxWidth)
        assertEquals(50f, constraints.minHeight)
        assertEquals(200f, constraints.maxHeight)
    }

    @Test
    fun `test box constraints validation - negative min width throws`() {
        assertFailsWith<IllegalArgumentException> {
            BoxConstraints(minWidth = -1f)
        }
    }

    @Test
    fun `test box constraints validation - negative min height throws`() {
        assertFailsWith<IllegalArgumentException> {
            BoxConstraints(minHeight = -1f)
        }
    }

    @Test
    fun `test box constraints validation - min greater than max width throws`() {
        assertFailsWith<IllegalArgumentException> {
            BoxConstraints(minWidth = 300f, maxWidth = 100f)
        }
    }

    @Test
    fun `test box constraints validation - min greater than max height throws`() {
        assertFailsWith<IllegalArgumentException> {
            BoxConstraints(minHeight = 300f, maxHeight = 100f)
        }
    }

    @Test
    fun `test tight constraints factory`() {
        val constraints = BoxConstraints.tight(200f, 150f)

        assertEquals(200f, constraints.minWidth)
        assertEquals(200f, constraints.maxWidth)
        assertEquals(150f, constraints.minHeight)
        assertEquals(150f, constraints.maxHeight)
        assertTrue(constraints.isTight)
    }

    @Test
    fun `test tight width constraints`() {
        val constraints = BoxConstraints.tightWidth(200f)

        assertEquals(200f, constraints.minWidth)
        assertEquals(200f, constraints.maxWidth)
        assertEquals(0f, constraints.minHeight)
        assertEquals(Float.POSITIVE_INFINITY, constraints.maxHeight)
        assertTrue(constraints.hasTightWidth)
    }

    @Test
    fun `test tight height constraints`() {
        val constraints = BoxConstraints.tightHeight(150f)

        assertEquals(0f, constraints.minWidth)
        assertEquals(Float.POSITIVE_INFINITY, constraints.maxWidth)
        assertEquals(150f, constraints.minHeight)
        assertEquals(150f, constraints.maxHeight)
        assertTrue(constraints.hasTightHeight)
    }

    @Test
    fun `test loose constraints factory`() {
        val constraints = BoxConstraints.loose(300f, 200f)

        assertEquals(0f, constraints.minWidth)
        assertEquals(300f, constraints.maxWidth)
        assertEquals(0f, constraints.minHeight)
        assertEquals(200f, constraints.maxHeight)
    }

    @Test
    fun `test expand constraints factory`() {
        val constraints = BoxConstraints.expand(500f, 400f)

        assertEquals(500f, constraints.minWidth)
        assertEquals(500f, constraints.maxWidth)
        assertEquals(400f, constraints.minHeight)
        assertEquals(400f, constraints.maxHeight)
    }

    @Test
    fun `test expand constraints with infinity`() {
        val constraints = BoxConstraints.expand()

        assertEquals(Float.POSITIVE_INFINITY, constraints.minWidth)
        assertEquals(Float.POSITIVE_INFINITY, constraints.maxWidth)
        assertEquals(Float.POSITIVE_INFINITY, constraints.minHeight)
        assertEquals(Float.POSITIVE_INFINITY, constraints.maxHeight)
    }

    @Test
    fun `test is tight width`() {
        val tight = BoxConstraints(minWidth = 100f, maxWidth = 100f)
        val loose = BoxConstraints(minWidth = 50f, maxWidth = 150f)

        assertTrue(tight.hasTightWidth)
        assert(!loose.hasTightWidth)
    }

    @Test
    fun `test is tight height`() {
        val tight = BoxConstraints(minHeight = 100f, maxHeight = 100f)
        val loose = BoxConstraints(minHeight = 50f, maxHeight = 150f)

        assertTrue(tight.hasTightHeight)
        assert(!loose.hasTightHeight)
    }

    @Test
    fun `test is tight`() {
        val tight = BoxConstraints.tight(100f, 100f)
        val loosish = BoxConstraints(minWidth = 100f, maxWidth = 100f, minHeight = 50f, maxHeight = 150f)

        assertTrue(tight.isTight)
        assert(!loosish.isTight)
    }

    @Test
    fun `test is satisfied by - valid size`() {
        val constraints = BoxConstraints(
            minWidth = 100f,
            maxWidth = 300f,
            minHeight = 50f,
            maxHeight = 200f
        )

        assertTrue(constraints.isSatisfiedBy(200f, 100f))
    }

    @Test
    fun `test is satisfied by - too small`() {
        val constraints = BoxConstraints(
            minWidth = 100f,
            maxWidth = 300f,
            minHeight = 50f,
            maxHeight = 200f
        )

        assert(!constraints.isSatisfiedBy(50f, 100f)) // Width too small
    }

    @Test
    fun `test is satisfied by - too large`() {
        val constraints = BoxConstraints(
            minWidth = 100f,
            maxWidth = 300f,
            minHeight = 50f,
            maxHeight = 200f
        )

        assert(!constraints.isSatisfiedBy(200f, 250f)) // Height too large
    }

    @Test
    fun `test constrain width`() {
        val constraints = BoxConstraints(minWidth = 100f, maxWidth = 300f)

        assertEquals(100f, constraints.constrainWidth(50f)) // Too small
        assertEquals(200f, constraints.constrainWidth(200f)) // Just right
        assertEquals(300f, constraints.constrainWidth(400f)) // Too large
    }

    @Test
    fun `test constrain height`() {
        val constraints = BoxConstraints(minHeight = 50f, maxHeight = 200f)

        assertEquals(50f, constraints.constrainHeight(25f)) // Too small
        assertEquals(100f, constraints.constrainHeight(100f)) // Just right
        assertEquals(200f, constraints.constrainHeight(300f)) // Too large
    }

    // ======= ConstrainedBoxComponent Tests =======

    @Test
    fun `test constrained box with min constraints`() {
        val constraints = BoxConstraints(minWidth = 200f, minHeight = 100f)
        val constrainedBox = ConstrainedBoxComponent(
            constraints = constraints,
            child = "Test"
        )

        assertEquals(constraints, constrainedBox.constraints)
        assertEquals("Test", constrainedBox.child)
    }

    @Test
    fun `test constrained box with max constraints`() {
        val constraints = BoxConstraints(maxWidth = 300f, maxHeight = 200f)
        val constrainedBox = ConstrainedBoxComponent(
            constraints = constraints,
            child = "Test"
        )

        assertEquals(constraints, constrainedBox.constraints)
    }

    @Test
    fun `test constrained box with tight constraints`() {
        val constraints = BoxConstraints.tight(150f, 150f)
        val constrainedBox = ConstrainedBoxComponent(
            constraints = constraints,
            child = "Test"
        )

        assertTrue(constrainedBox.constraints.isTight)
    }

    @Test
    fun `test constrained box with loose constraints`() {
        val constraints = BoxConstraints.loose(300f, 200f)
        val constrainedBox = ConstrainedBoxComponent(
            constraints = constraints,
            child = "Test"
        )

        assertEquals(0f, constrainedBox.constraints.minWidth)
        assertEquals(300f, constrainedBox.constraints.maxWidth)
    }

    @Test
    fun `test constrained box serialization preserves properties`() {
        val constraints = BoxConstraints(
            minWidth = 100f,
            maxWidth = 300f,
            minHeight = 50f,
            maxHeight = 200f
        )
        val constrainedBox = ConstrainedBoxComponent(
            constraints = constraints,
            child = "TestChild"
        )

        assertNotNull(constrainedBox)
        assertEquals(100f, constrainedBox.constraints.minWidth)
        assertEquals(300f, constrainedBox.constraints.maxWidth)
        assertEquals(50f, constrainedBox.constraints.minHeight)
        assertEquals(200f, constrainedBox.constraints.maxHeight)
        assertEquals("TestChild", constrainedBox.child)
    }
}
