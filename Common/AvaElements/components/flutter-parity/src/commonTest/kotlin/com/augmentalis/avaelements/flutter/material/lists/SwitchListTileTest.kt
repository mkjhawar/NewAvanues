package com.augmentalis.avaelements.flutter.material.lists

import kotlin.test.*

/**
 * Comprehensive unit tests for SwitchListTile component
 *
 * @since 3.0.0-flutter-parity
 */
class SwitchListTileTest {

    @Test
    fun `create SwitchListTile with default values`() {
        val tile = SwitchListTile(title = "Dark Mode")

        assertEquals("SwitchListTile", tile.type)
        assertEquals("Dark Mode", tile.title)
        assertFalse(tile.value)
        assertTrue(tile.enabled)
        assertEquals(SwitchListTile.ListTileControlAffinity.Trailing, tile.controlAffinity)
    }

    @Test
    fun `SwitchListTile onChanged callback updates state`() {
        var switchState = false
        val tile = SwitchListTile(
            title = "Enable feature",
            onChanged = { switchState = it }
        )

        tile.onChanged?.invoke(true)
        assertTrue(switchState)

        tile.onChanged?.invoke(false)
        assertFalse(switchState)
    }

    @Test
    fun `SwitchListTile accessibility description includes state`() {
        val on = SwitchListTile(title = "Test", value = true)
        assertEquals("Test, on", on.getAccessibilityDescription())

        val off = SwitchListTile(title = "Test", value = false)
        assertEquals("Test, off", off.getAccessibilityDescription())
    }

    @Test
    fun `SwitchListTile on factory creates ON tile`() {
        val tile = SwitchListTile.on("Enabled")

        assertTrue(tile.value)
        assertEquals("Enabled", tile.title)
    }

    @Test
    fun `SwitchListTile off factory creates OFF tile`() {
        val tile = SwitchListTile.off("Disabled")

        assertFalse(tile.value)
        assertEquals("Disabled", tile.title)
    }

    @Test
    fun `SwitchListTile withSubtitle factory includes subtitle`() {
        val tile = SwitchListTile.withSubtitle(
            title = "WiFi",
            subtitle = "Connected to network",
            value = true
        )

        assertEquals("WiFi", tile.title)
        assertEquals("Connected to network", tile.subtitle)
        assertTrue(tile.value)
    }

    @Test
    fun `SwitchListTile supports leading and trailing positions`() {
        val leading = SwitchListTile(
            title = "Test",
            controlAffinity = SwitchListTile.ListTileControlAffinity.Leading
        )
        assertEquals(SwitchListTile.ListTileControlAffinity.Leading, leading.controlAffinity)

        val trailing = SwitchListTile(
            title = "Test",
            controlAffinity = SwitchListTile.ListTileControlAffinity.Trailing
        )
        assertEquals(SwitchListTile.ListTileControlAffinity.Trailing, trailing.controlAffinity)
    }

    @Test
    fun `SwitchListTile disabled state prevents interaction`() {
        val tile = SwitchListTile(
            title = "Test",
            enabled = false
        )

        assertFalse(tile.enabled)
    }

    @Test
    fun `SwitchListTile supports custom contentDescription`() {
        val tile = SwitchListTile(
            title = "Test",
            contentDescription = "Custom description",
            value = true
        )

        assertEquals("Custom description, on", tile.getAccessibilityDescription())
    }

    @Test
    fun `SwitchListTile toggle behavior`() {
        var state = false
        val tile = SwitchListTile(
            title = "Feature",
            value = state,
            onChanged = { state = it }
        )

        // Simulate toggle
        tile.onChanged?.invoke(!tile.value)
        assertTrue(state)

        tile.onChanged?.invoke(!state)
        assertFalse(state)
    }
}
