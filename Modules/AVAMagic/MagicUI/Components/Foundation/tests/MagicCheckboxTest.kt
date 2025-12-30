package com.augmentalis.magicui.components.foundation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MagicCheckbox Component Tests
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class MagicCheckboxTest {

    @Test
    fun `checkbox creation with default values`() {
        val checkbox = MagicCheckbox(
            checked = false,
            onCheckedChange = {}
        )

        assertFalse(checkbox.checked)
        assertTrue(checkbox.enabled)
        assertEquals(null, checkbox.label)
        assertEquals(CheckboxState.UNCHECKED, checkbox.state)
    }

    @Test
    fun `checkbox with all properties`() {
        val checkbox = MagicCheckbox(
            checked = true,
            onCheckedChange = {},
            label = "Accept Terms",
            enabled = false,
            state = CheckboxState.CHECKED
        )

        assertTrue(checkbox.checked)
        assertFalse(checkbox.enabled)
        assertEquals("Accept Terms", checkbox.label)
        assertEquals(CheckboxState.CHECKED, checkbox.state)
    }

    @Test
    fun `checked checkbox`() {
        val checkbox = MagicCheckbox(
            checked = true,
            onCheckedChange = {}
        )

        assertTrue(checkbox.checked)
        assertEquals(CheckboxState.CHECKED, checkbox.state)
    }

    @Test
    fun `unchecked checkbox`() {
        val checkbox = MagicCheckbox(
            checked = false,
            onCheckedChange = {}
        )

        assertFalse(checkbox.checked)
        assertEquals(CheckboxState.UNCHECKED, checkbox.state)
    }

    @Test
    fun `indeterminate checkbox`() {
        val checkbox = MagicCheckbox(
            checked = false,
            onCheckedChange = {},
            state = CheckboxState.INDETERMINATE
        )

        assertEquals(CheckboxState.INDETERMINATE, checkbox.state)
    }

    @Test
    fun `checkbox with label`() {
        val checkbox = MagicCheckbox(
            checked = false,
            onCheckedChange = {},
            label = "Remember me"
        )

        assertEquals("Remember me", checkbox.label)
    }

    @Test
    fun `disabled checkbox`() {
        val checkbox = MagicCheckbox(
            checked = false,
            onCheckedChange = {},
            enabled = false
        )

        assertFalse(checkbox.enabled)
    }

    @Test
    fun `checkbox change handler`() {
        var currentState = false
        val checkbox = MagicCheckbox(
            checked = currentState,
            onCheckedChange = { currentState = it }
        )

        checkbox.onCheckedChange(true)
        assertTrue(currentState)

        checkbox.onCheckedChange(false)
        assertFalse(currentState)
    }

    @Test
    fun `checkbox state transitions`() {
        val unchecked = MagicCheckbox(false, {}, state = CheckboxState.UNCHECKED)
        val checked = MagicCheckbox(true, {}, state = CheckboxState.CHECKED)
        val indeterminate = MagicCheckbox(false, {}, state = CheckboxState.INDETERMINATE)

        assertEquals(CheckboxState.UNCHECKED, unchecked.state)
        assertEquals(CheckboxState.CHECKED, checked.state)
        assertEquals(CheckboxState.INDETERMINATE, indeterminate.state)
    }
}
