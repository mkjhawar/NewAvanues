package com.augmentalis.magicui.components.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MagicIconPicker Component Tests
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class MagicIconPickerTest {

    @Test
    fun `icon picker creation with default values`() {
        val picker = MagicIconPicker(
            selectedIcon = null,
            onIconChange = {}
        )

        assertEquals(null, picker.selectedIcon)
        assertEquals(IconLibrary.MATERIAL, picker.library)
        assertEquals(null, picker.searchQuery)
        assertEquals(null, picker.category)
        assertEquals(IconSize.MEDIUM, picker.iconSize)
        assertEquals(24, picker.columns)
        assertEquals(null, picker.label)
    }

    @Test
    fun `icon picker with all properties`() {
        val picker = MagicIconPicker(
            selectedIcon = "home",
            onIconChange = {},
            library = IconLibrary.FONT_AWESOME,
            searchQuery = "arrow",
            category = "navigation",
            iconSize = IconSize.LARGE,
            columns = 6,
            label = "Select Icon"
        )

        assertEquals("home", picker.selectedIcon)
        assertEquals(IconLibrary.FONT_AWESOME, picker.library)
        assertEquals("arrow", picker.searchQuery)
        assertEquals("navigation", picker.category)
        assertEquals(IconSize.LARGE, picker.iconSize)
        assertEquals(6, picker.columns)
        assertEquals("Select Icon", picker.label)
    }

    @Test
    fun `icon libraries`() {
        val material = MagicIconPicker(null, {}, library = IconLibrary.MATERIAL)
        val fontAwesome = MagicIconPicker(null, {}, library = IconLibrary.FONT_AWESOME)
        val sfSymbols = MagicIconPicker(null, {}, library = IconLibrary.SF_SYMBOLS)
        val custom = MagicIconPicker(null, {}, library = IconLibrary.CUSTOM)

        assertEquals(IconLibrary.MATERIAL, material.library)
        assertEquals(IconLibrary.FONT_AWESOME, fontAwesome.library)
        assertEquals(IconLibrary.SF_SYMBOLS, sfSymbols.library)
        assertEquals(IconLibrary.CUSTOM, custom.library)
    }

    @Test
    fun `icon sizes`() {
        val small = MagicIconPicker(null, {}, iconSize = IconSize.SMALL)
        val medium = MagicIconPicker(null, {}, iconSize = IconSize.MEDIUM)
        val large = MagicIconPicker(null, {}, iconSize = IconSize.LARGE)

        assertEquals(IconSize.SMALL, small.iconSize)
        assertEquals(IconSize.MEDIUM, medium.iconSize)
        assertEquals(IconSize.LARGE, large.iconSize)
    }

    @Test
    fun `icon picker with search query`() {
        val picker = MagicIconPicker(
            selectedIcon = null,
            onIconChange = {},
            searchQuery = "settings"
        )

        assertEquals("settings", picker.searchQuery)
    }

    @Test
    fun `icon picker with category filter`() {
        val picker = MagicIconPicker(
            selectedIcon = null,
            onIconChange = {},
            category = "communication"
        )

        assertEquals("communication", picker.category)
    }

    @Test
    fun `icon picker with custom columns`() {
        val picker = MagicIconPicker(
            selectedIcon = null,
            onIconChange = {},
            columns = 8
        )

        assertEquals(8, picker.columns)
    }

    @Test
    fun `icon picker with label`() {
        val picker = MagicIconPicker(
            selectedIcon = null,
            onIconChange = {},
            label = "Choose an Icon"
        )

        assertEquals("Choose an Icon", picker.label)
    }

    @Test
    fun `icon picker change handler`() {
        var currentIcon: String? = null
        val picker = MagicIconPicker(
            selectedIcon = currentIcon,
            onIconChange = { currentIcon = it }
        )

        picker.onIconChange("star")
        assertEquals("star", currentIcon)

        picker.onIconChange("heart")
        assertEquals("heart", currentIcon)

        picker.onIconChange(null)
        assertEquals(null, currentIcon)
    }

    @Test
    fun `material icons use case`() {
        val commonCategories = listOf(
            "action", "alert", "av", "communication", "content",
            "device", "editor", "file", "hardware", "image",
            "maps", "navigation", "notification", "social", "toggle"
        )

        var selectedIcon: String? = null
        val picker = MagicIconPicker(
            selectedIcon = selectedIcon,
            onIconChange = { selectedIcon = it },
            library = IconLibrary.MATERIAL,
            category = "navigation",
            iconSize = IconSize.MEDIUM,
            columns = 6
        )

        assertEquals(IconLibrary.MATERIAL, picker.library)
        assertEquals("navigation", picker.category)
        assertEquals(6, picker.columns)
    }

    @Test
    fun `search functionality use case`() {
        var searchQuery = ""
        var selectedIcon: String? = null

        val picker = MagicIconPicker(
            selectedIcon = selectedIcon,
            onIconChange = { selectedIcon = it },
            searchQuery = searchQuery,
            library = IconLibrary.FONT_AWESOME
        )

        // Simulate search
        searchQuery = "arrow"
        val pickerWithSearch = picker.copy(searchQuery = searchQuery)
        assertEquals("arrow", pickerWithSearch.searchQuery)
    }

    @Test
    fun `icon selection workflow`() {
        var selectedIcon: String? = null
        val picker = MagicIconPicker(
            selectedIcon = selectedIcon,
            onIconChange = { selectedIcon = it },
            library = IconLibrary.MATERIAL,
            label = "Button Icon"
        )

        // User selects an icon
        picker.onIconChange("check_circle")
        assertEquals("check_circle", selectedIcon)

        // User changes selection
        picker.onIconChange("favorite")
        assertEquals("favorite", selectedIcon)

        // User clears selection
        picker.onIconChange(null)
        assertEquals(null, selectedIcon)
    }

    @Test
    fun `theme builder use case`() {
        val picker = MagicIconPicker(
            selectedIcon = "home",
            onIconChange = {},
            library = IconLibrary.SF_SYMBOLS,
            iconSize = IconSize.LARGE,
            columns = 8,
            label = "App Icon"
        )

        assertEquals("home", picker.selectedIcon)
        assertEquals(IconLibrary.SF_SYMBOLS, picker.library)
        assertEquals(IconSize.LARGE, picker.iconSize)
        assertEquals(8, picker.columns)
    }

    @Test
    fun `custom icon library use case`() {
        val customIcons = listOf(
            "brand_logo", "brand_icon", "custom_1", "custom_2"
        )

        val picker = MagicIconPicker(
            selectedIcon = "brand_logo",
            onIconChange = {},
            library = IconLibrary.CUSTOM,
            iconSize = IconSize.MEDIUM
        )

        assertEquals(IconLibrary.CUSTOM, picker.library)
        assertEquals("brand_logo", picker.selectedIcon)
    }

    private fun MagicIconPicker.copy(
        selectedIcon: String? = this.selectedIcon,
        library: IconLibrary = this.library,
        searchQuery: String? = this.searchQuery,
        category: String? = this.category,
        iconSize: IconSize = this.iconSize,
        columns: Int = this.columns,
        label: String? = this.label
    ) = MagicIconPicker(
        selectedIcon = selectedIcon,
        onIconChange = this.onIconChange,
        library = library,
        searchQuery = searchQuery,
        category = category,
        iconSize = iconSize,
        columns = columns,
        label = label
    )
}
