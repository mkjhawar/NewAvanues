package com.augmentalis.avamagic.components.foundation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MagicDivider Component Tests
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class MagicDividerTest {

    @Test
    fun `divider creation with default values`() {
        val divider = MagicDivider()

        assertEquals(DividerOrientation.HORIZONTAL, divider.orientation)
        assertEquals(1, divider.thickness)
        assertEquals(null, divider.color)
        assertFalse(divider.inset)
    }

    @Test
    fun `divider with all properties`() {
        val divider = MagicDivider(
            orientation = DividerOrientation.VERTICAL,
            thickness = 2,
            color = "#CCCCCC",
            inset = true
        )

        assertEquals(DividerOrientation.VERTICAL, divider.orientation)
        assertEquals(2, divider.thickness)
        assertEquals("#CCCCCC", divider.color)
        assertTrue(divider.inset)
    }

    @Test
    fun `horizontal divider`() {
        val divider = MagicDivider(
            orientation = DividerOrientation.HORIZONTAL
        )

        assertEquals(DividerOrientation.HORIZONTAL, divider.orientation)
    }

    @Test
    fun `vertical divider`() {
        val divider = MagicDivider(
            orientation = DividerOrientation.VERTICAL
        )

        assertEquals(DividerOrientation.VERTICAL, divider.orientation)
    }

    @Test
    fun `divider with custom thickness`() {
        val thin = MagicDivider(thickness = 1)
        val medium = MagicDivider(thickness = 2)
        val thick = MagicDivider(thickness = 4)

        assertEquals(1, thin.thickness)
        assertEquals(2, medium.thickness)
        assertEquals(4, thick.thickness)
    }

    @Test
    fun `divider with custom color`() {
        val divider = MagicDivider(
            color = "#FF0000"
        )

        assertEquals("#FF0000", divider.color)
    }

    @Test
    fun `divider without color uses theme default`() {
        val divider = MagicDivider()

        assertEquals(null, divider.color)
    }

    @Test
    fun `inset divider`() {
        val divider = MagicDivider(
            inset = true
        )

        assertTrue(divider.inset)
    }

    @Test
    fun `non-inset divider`() {
        val divider = MagicDivider(
            inset = false
        )

        assertFalse(divider.inset)
    }

    @Test
    fun `list item divider use case`() {
        val divider = MagicDivider(
            orientation = DividerOrientation.HORIZONTAL,
            inset = true,
            thickness = 1
        )

        assertEquals(DividerOrientation.HORIZONTAL, divider.orientation)
        assertTrue(divider.inset)
        assertEquals(1, divider.thickness)
    }

    @Test
    fun `sidebar divider use case`() {
        val divider = MagicDivider(
            orientation = DividerOrientation.VERTICAL,
            thickness = 2
        )

        assertEquals(DividerOrientation.VERTICAL, divider.orientation)
        assertEquals(2, divider.thickness)
    }
}
