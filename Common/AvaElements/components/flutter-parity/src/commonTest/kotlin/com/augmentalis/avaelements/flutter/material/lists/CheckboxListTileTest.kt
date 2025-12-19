package com.augmentalis.avaelements.flutter.material.lists

import kotlin.test.*

/**
 * Comprehensive unit tests for CheckboxListTile component
 *
 * @since 3.0.0-flutter-parity
 */
class CheckboxListTileTest {

    @Test
    fun `create CheckboxListTile with default values`() {
        val tile = CheckboxListTile(title = "Enable notifications")

        assertEquals("CheckboxListTile", tile.type)
        assertEquals("Enable notifications", tile.title)
        assertEquals(false, tile.value)
        assertTrue(tile.enabled)
        assertFalse(tile.tristate)
        assertEquals(CheckboxListTile.ListTileControlAffinity.Leading, tile.controlAffinity)
    }

    @Test
    fun `CheckboxListTile onChanged callback updates state`() {
        var checkedState: Boolean? = false
        val tile = CheckboxListTile(
            title = "Accept terms",
            onChanged = { checkedState = it }
        )

        tile.onChanged?.invoke(true)
        assertEquals(true, checkedState)

        tile.onChanged?.invoke(false)
        assertEquals(false, checkedState)
    }

    @Test
    fun `CheckboxListTile tristate supports three states`() {
        var state: Boolean? = null
        val tile = CheckboxListTile(
            title = "Select all",
            value = null,
            tristate = true,
            onChanged = { state = it }
        )

        assertTrue(tile.tristate)
        assertNull(tile.value)

        tile.onChanged?.invoke(true)
        assertEquals(true, state)

        tile.onChanged?.invoke(false)
        assertEquals(false, state)

        tile.onChanged?.invoke(null)
        assertNull(state)
    }

    @Test
    fun `CheckboxListTile accessibility description includes state`() {
        val checked = CheckboxListTile(title = "Test", value = true)
        assertEquals("Test, checked", checked.getAccessibilityDescription())

        val unchecked = CheckboxListTile(title = "Test", value = false)
        assertEquals("Test, unchecked", unchecked.getAccessibilityDescription())

        val indeterminate = CheckboxListTile(title = "Test", value = null, tristate = true)
        assertEquals("Test, indeterminate", indeterminate.getAccessibilityDescription())
    }

    @Test
    fun `CheckboxListTile checked factory creates checked tile`() {
        val tile = CheckboxListTile.checked("Enabled")

        assertEquals(true, tile.value)
        assertEquals("Enabled", tile.title)
    }

    @Test
    fun `CheckboxListTile unchecked factory creates unchecked tile`() {
        val tile = CheckboxListTile.unchecked("Disabled")

        assertEquals(false, tile.value)
        assertEquals("Disabled", tile.title)
    }

    @Test
    fun `CheckboxListTile indeterminate factory creates tristate tile`() {
        val tile = CheckboxListTile.indeterminate("Partial")

        assertNull(tile.value)
        assertTrue(tile.tristate)
        assertEquals("Partial", tile.title)
    }

    @Test
    fun `CheckboxListTile withSubtitle factory includes subtitle`() {
        val tile = CheckboxListTile.withSubtitle(
            title = "Notifications",
            subtitle = "Receive alerts",
            value = true
        )

        assertEquals("Notifications", tile.title)
        assertEquals("Receive alerts", tile.subtitle)
        assertEquals(true, tile.value)
    }

    @Test
    fun `CheckboxListTile supports leading and trailing positions`() {
        val leading = CheckboxListTile(
            title = "Test",
            controlAffinity = CheckboxListTile.ListTileControlAffinity.Leading
        )
        assertEquals(CheckboxListTile.ListTileControlAffinity.Leading, leading.controlAffinity)

        val trailing = CheckboxListTile(
            title = "Test",
            controlAffinity = CheckboxListTile.ListTileControlAffinity.Trailing
        )
        assertEquals(CheckboxListTile.ListTileControlAffinity.Trailing, trailing.controlAffinity)
    }

    @Test
    fun `CheckboxListTile disabled state prevents interaction`() {
        val tile = CheckboxListTile(
            title = "Test",
            enabled = false
        )

        assertFalse(tile.enabled)
    }

    @Test
    fun `CheckboxListTile supports custom contentDescription`() {
        val tile = CheckboxListTile(
            title = "Test",
            contentDescription = "Custom description",
            value = true
        )

        assertEquals("Custom description, checked", tile.getAccessibilityDescription())
    }
}
