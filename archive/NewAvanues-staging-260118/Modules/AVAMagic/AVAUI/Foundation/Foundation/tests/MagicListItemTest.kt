package com.augmentalis.avamagic.components.foundation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MagicListItem Component Tests
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class MagicListItemTest {

    @Test
    fun `list item creation with default values`() {
        val item = MagicListItem(
            title = "Item"
        )

        assertEquals("Item", item.title)
        assertEquals(null, item.subtitle)
        assertEquals(null, item.leadingIcon)
        assertEquals(null, item.trailingIcon)
        assertEquals(null, item.onClick)
        assertFalse(item.showDivider)
    }

    @Test
    fun `list item with all properties`() {
        val item = MagicListItem(
            title = "Settings",
            subtitle = "Manage preferences",
            leadingIcon = "gear",
            trailingIcon = "chevron-right",
            onClick = {},
            showDivider = true
        )

        assertEquals("Settings", item.title)
        assertEquals("Manage preferences", item.subtitle)
        assertEquals("gear", item.leadingIcon)
        assertEquals("chevron-right", item.trailingIcon)
        assertTrue(item.onClick != null)
        assertTrue(item.showDivider)
    }

    @Test
    fun `list item with subtitle`() {
        val item = MagicListItem(
            title = "Notifications",
            subtitle = "Push, Email, SMS"
        )

        assertEquals("Notifications", item.subtitle)
    }

    @Test
    fun `list item with leading icon`() {
        val item = MagicListItem(
            title = "Profile",
            leadingIcon = "person"
        )

        assertEquals("person", item.leadingIcon)
    }

    @Test
    fun `list item with trailing icon`() {
        val item = MagicListItem(
            title = "Details",
            trailingIcon = "arrow-right"
        )

        assertEquals("arrow-right", item.trailingIcon)
    }

    @Test
    fun `clickable list item`() {
        var clicked = false
        val item = MagicListItem(
            title = "Click Me",
            onClick = { clicked = true }
        )

        item.onClick?.invoke()
        assertTrue(clicked)
    }

    @Test
    fun `list item with divider`() {
        val item = MagicListItem(
            title = "Item",
            showDivider = true
        )

        assertTrue(item.showDivider)
    }

    @Test
    fun `settings list item use case`() {
        var navigated = false
        val item = MagicListItem(
            title = "Account",
            subtitle = "Email, Password, Security",
            leadingIcon = "person.circle",
            trailingIcon = "chevron.right",
            onClick = { navigated = true },
            showDivider = true
        )

        assertEquals("Account", item.title)
        assertEquals("Email, Password, Security", item.subtitle)
        assertEquals("person.circle", item.leadingIcon)
        assertEquals("chevron.right", item.trailingIcon)
        assertTrue(item.showDivider)
        item.onClick?.invoke()
        assertTrue(navigated)
    }

    @Test
    fun `contact list item use case`() {
        val item = MagicListItem(
            title = "John Doe",
            subtitle = "john@example.com",
            leadingIcon = "person.fill",
            trailingIcon = "phone.fill",
            showDivider = true
        )

        assertEquals("John Doe", item.title)
        assertEquals("john@example.com", item.subtitle)
        assertEquals("person.fill", item.leadingIcon)
        assertEquals("phone.fill", item.trailingIcon)
    }

    @Test
    fun `simple list item use case`() {
        val item = MagicListItem(
            title = "Item 1"
        )

        assertEquals("Item 1", item.title)
        assertEquals(null, item.subtitle)
        assertEquals(null, item.leadingIcon)
        assertEquals(null, item.trailingIcon)
    }
}
