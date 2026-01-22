package com.augmentalis.avamagic.components.foundation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MagicTextField Component Tests
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class MagicTextFieldTest {

    @Test
    fun `text field creation with default values`() {
        val textField = MagicTextField(
            value = "",
            onValueChange = {}
        )

        assertEquals("", textField.value)
        assertEquals(null, textField.label)
        assertEquals(null, textField.placeholder)
        assertTrue(textField.enabled)
        assertFalse(textField.readOnly)
        assertEquals(TextFieldType.TEXT, textField.type)
        assertEquals(null, textField.error)
        assertEquals(null, textField.helperText)
        assertEquals(null, textField.leadingIcon)
        assertEquals(null, textField.trailingIcon)
        assertEquals(1, textField.maxLines)
    }

    @Test
    fun `text field with all properties`() {
        val textField = MagicTextField(
            value = "John",
            onValueChange = {},
            label = "Name",
            placeholder = "Enter your name",
            enabled = false,
            readOnly = true,
            type = TextFieldType.EMAIL,
            error = "Invalid email",
            helperText = "We'll never share your email",
            leadingIcon = "person",
            trailingIcon = "check",
            maxLines = 3
        )

        assertEquals("John", textField.value)
        assertEquals("Name", textField.label)
        assertEquals("Enter your name", textField.placeholder)
        assertFalse(textField.enabled)
        assertTrue(textField.readOnly)
        assertEquals(TextFieldType.EMAIL, textField.type)
        assertEquals("Invalid email", textField.error)
        assertEquals("We'll never share your email", textField.helperText)
        assertEquals("person", textField.leadingIcon)
        assertEquals("check", textField.trailingIcon)
        assertEquals(3, textField.maxLines)
    }

    @Test
    fun `text field types`() {
        val text = MagicTextField("", {}, type = TextFieldType.TEXT)
        val email = MagicTextField("", {}, type = TextFieldType.EMAIL)
        val password = MagicTextField("", {}, type = TextFieldType.PASSWORD)
        val number = MagicTextField("", {}, type = TextFieldType.NUMBER)
        val phone = MagicTextField("", {}, type = TextFieldType.PHONE)
        val url = MagicTextField("", {}, type = TextFieldType.URL)

        assertEquals(TextFieldType.TEXT, text.type)
        assertEquals(TextFieldType.EMAIL, email.type)
        assertEquals(TextFieldType.PASSWORD, password.type)
        assertEquals(TextFieldType.NUMBER, number.type)
        assertEquals(TextFieldType.PHONE, phone.type)
        assertEquals(TextFieldType.URL, url.type)
    }

    @Test
    fun `text field with label`() {
        val textField = MagicTextField(
            value = "",
            onValueChange = {},
            label = "Email"
        )

        assertEquals("Email", textField.label)
    }

    @Test
    fun `text field with placeholder`() {
        val textField = MagicTextField(
            value = "",
            onValueChange = {},
            placeholder = "example@email.com"
        )

        assertEquals("example@email.com", textField.placeholder)
    }

    @Test
    fun `disabled text field`() {
        val textField = MagicTextField(
            value = "",
            onValueChange = {},
            enabled = false
        )

        assertFalse(textField.enabled)
    }

    @Test
    fun `read-only text field`() {
        val textField = MagicTextField(
            value = "Read Only Value",
            onValueChange = {},
            readOnly = true
        )

        assertTrue(textField.readOnly)
    }

    @Test
    fun `text field with error`() {
        val textField = MagicTextField(
            value = "invalid",
            onValueChange = {},
            error = "Invalid format"
        )

        assertEquals("Invalid format", textField.error)
    }

    @Test
    fun `text field with helper text`() {
        val textField = MagicTextField(
            value = "",
            onValueChange = {},
            helperText = "Min 8 characters"
        )

        assertEquals("Min 8 characters", textField.helperText)
    }

    @Test
    fun `text field with leading icon`() {
        val textField = MagicTextField(
            value = "",
            onValueChange = {},
            leadingIcon = "magnifyingglass"
        )

        assertEquals("magnifyingglass", textField.leadingIcon)
    }

    @Test
    fun `text field with trailing icon`() {
        val textField = MagicTextField(
            value = "Valid",
            onValueChange = {},
            trailingIcon = "checkmark"
        )

        assertEquals("checkmark", textField.trailingIcon)
    }

    @Test
    fun `multiline text field`() {
        val textField = MagicTextField(
            value = "Line 1\nLine 2",
            onValueChange = {},
            maxLines = 5
        )

        assertEquals(5, textField.maxLines)
    }

    @Test
    fun `text field value change handler`() {
        var currentValue = ""
        val textField = MagicTextField(
            value = currentValue,
            onValueChange = { currentValue = it }
        )

        textField.onValueChange("New Value")
        assertEquals("New Value", currentValue)
    }

    @Test
    fun `email input use case`() {
        val textField = MagicTextField(
            value = "",
            onValueChange = {},
            label = "Email",
            placeholder = "your@email.com",
            type = TextFieldType.EMAIL,
            leadingIcon = "envelope",
            helperText = "We'll send verification email"
        )

        assertEquals(TextFieldType.EMAIL, textField.type)
        assertEquals("envelope", textField.leadingIcon)
        assertEquals("We'll send verification email", textField.helperText)
    }

    @Test
    fun `password input use case`() {
        val textField = MagicTextField(
            value = "",
            onValueChange = {},
            label = "Password",
            type = TextFieldType.PASSWORD,
            leadingIcon = "lock",
            helperText = "Min 8 characters"
        )

        assertEquals(TextFieldType.PASSWORD, textField.type)
        assertEquals("lock", textField.leadingIcon)
    }

    @Test
    fun `search input use case`() {
        val textField = MagicTextField(
            value = "",
            onValueChange = {},
            placeholder = "Search...",
            leadingIcon = "magnifyingglass",
            trailingIcon = "xmark.circle"
        )

        assertEquals("magnifyingglass", textField.leadingIcon)
        assertEquals("xmark.circle", textField.trailingIcon)
    }
}
