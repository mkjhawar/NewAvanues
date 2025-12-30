package com.augmentalis.magicui.components.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for MagicUI Input Components
 */
class InputComponentsTest {

    @Test
    fun testSliderCreation() {
        val slider = Slider(id = "test-slider", value = 50f, min = 0f, max = 100f)
        assertEquals("test-slider", slider.id)
        assertEquals(50f, slider.value)
        assertEquals(0f, slider.min)
        assertEquals(100f, slider.max)
        assertTrue(slider.enabled)
    }

    @Test
    fun testSliderCallback() {
        var callbackValue = 0f
        val slider = Slider(id = "callback-slider", value = 50f, onValueChange = { callbackValue = it })
        assertNotNull(slider.onValueChange)
        slider.onValueChange?.invoke(75f)
        assertEquals(75f, callbackValue)
    }

    @Test
    fun testRangeSliderCreation() {
        val rangeSlider = RangeSlider(id = "test-range", valueStart = 20f, valueEnd = 80f, min = 0f, max = 100f)
        assertEquals("test-range", rangeSlider.id)
        assertEquals(20f, rangeSlider.valueStart)
        assertEquals(80f, rangeSlider.valueEnd)
    }

    @Test
    fun testDatePickerCreation() {
        val datePicker = DatePicker(id = "test-date", label = "Select Date")
        assertEquals("test-date", datePicker.id)
        assertEquals("Select Date", datePicker.label)
        assertEquals("Select date", datePicker.placeholder)
        assertTrue(datePicker.enabled)
    }

    @Test
    fun testDateFormatting() {
        val date = Date(2025, 11, 9)
        assertEquals("2025-11-09", date.format(DateFormat.ISO_8601))
        assertEquals("11/09/2025", date.format(DateFormat.US))
        assertEquals("09/11/2025", date.format(DateFormat.EU))
    }

    @Test
    fun testTimeFormatting() {
        val time = Time(14, 30, 0)
        assertEquals("14:30", time.format(is24Hour = true))
        assertEquals("02:30 PM", time.format(is24Hour = false))
    }

    @Test
    fun testRadioGroupCreation() {
        val options = listOf(
            RadioOption("opt1", "Option 1"),
            RadioOption("opt2", "Option 2"),
            RadioOption("opt3", "Option 3")
        )
        val group = RadioGroup(id = "test-group", options = options, selectedIndex = 0)
        assertEquals(3, group.options.size)
        assertEquals(0, group.selectedIndex)
    }

    @Test
    fun testDropdownCreation() {
        val options = listOf(
            DropdownOption("opt1", "Option 1"),
            DropdownOption("opt2", "Option 2")
        )
        val dropdown = Dropdown(id = "test-dropdown", options = options, label = "Choose")
        assertEquals("test-dropdown", dropdown.id)
        assertEquals(2, dropdown.options.size)
        assertFalse(dropdown.searchable)
    }

    @Test
    fun testAutocompleteCreation() {
        val options = listOf(
            AutocompleteOption("opt1", "Apple"),
            AutocompleteOption("opt2", "Banana")
        )
        val autocomplete = Autocomplete(id = "test-autocomplete", options = options, minChars = 2, maxResults = 5)
        assertEquals(2, autocomplete.options.size)
        assertEquals(2, autocomplete.minChars)
        assertEquals(5, autocomplete.maxResults)
    }

    @Test
    fun testFileUploadCreation() {
        val fileUpload = FileUpload(id = "test-upload", multiple = true, maxFiles = 5, maxSize = 10_000_000)
        assertEquals("test-upload", fileUpload.id)
        assertTrue(fileUpload.multiple)
        assertEquals(5, fileUpload.maxFiles)
    }

    @Test
    fun testRatingCreation() {
        val rating = Rating(id = "test-rating", rating = 3.5f, maxRating = 5, allowHalf = true)
        assertEquals("test-rating", rating.id)
        assertEquals(3.5f, rating.rating)
        assertTrue(rating.allowHalf)
    }

    @Test
    fun testSearchBarCreation() {
        val searchBar = SearchBar(id = "test-search", query = "", placeholder = "Search...", debounceMs = 300)
        assertEquals("test-search", searchBar.id)
        assertEquals("", searchBar.query)
        assertEquals(300, searchBar.debounceMs)
    }

    @Test
    fun testDisabledComponents() {
        val slider = Slider(id = "disabled", value = 50f, enabled = false)
        val dropdown = Dropdown(id = "disabled", options = emptyList(), enabled = false)
        assertFalse(slider.enabled)
        assertFalse(dropdown.enabled)
    }
}
