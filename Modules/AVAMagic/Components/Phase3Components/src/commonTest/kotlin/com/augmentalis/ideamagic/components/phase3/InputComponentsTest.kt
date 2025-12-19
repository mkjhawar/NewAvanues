package com.augmentalis.avaelements.phase3

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for Phase 3 Input Components
 *
 * Tests cover:
 * - Component creation and initialization
 * - Property validation
 * - Event handling
 * - State management
 * - Edge cases
 */
class InputComponentsTest {

    // ==================== Slider Tests ====================

    @Test
    fun testSliderCreation() {
        val slider = Slider(
            id = "test-slider",
            value = 50f,
            min = 0f,
            max = 100f
        )

        assertEquals("test-slider", slider.id)
        assertEquals(50f, slider.value)
        assertEquals(0f, slider.min)
        assertEquals(100f, slider.max)
        assertTrue(slider.enabled)
    }

    @Test
    fun testSliderWithStep() {
        val slider = Slider(
            id = "stepped-slider",
            value = 25f,
            min = 0f,
            max = 100f,
            step = 5f
        )

        assertEquals(5f, slider.step)
    }

    @Test
    fun testSliderCallback() {
        var callbackValue = 0f
        val slider = Slider(
            id = "callback-slider",
            value = 50f,
            onValueChange = { callbackValue = it }
        )

        assertNotNull(slider.onValueChange)
        slider.onValueChange?.invoke(75f)
        assertEquals(75f, callbackValue)
    }

    // ==================== RangeSlider Tests ====================

    @Test
    fun testRangeSliderCreation() {
        val rangeSlider = RangeSlider(
            id = "test-range",
            valueStart = 20f,
            valueEnd = 80f,
            min = 0f,
            max = 100f
        )

        assertEquals("test-range", rangeSlider.id)
        assertEquals(20f, rangeSlider.valueStart)
        assertEquals(80f, rangeSlider.valueEnd)
    }

    @Test
    fun testRangeSliderCallback() {
        var start = 0f
        var end = 0f
        val rangeSlider = RangeSlider(
            id = "callback-range",
            valueStart = 25f,
            valueEnd = 75f,
            onValuesChange = { s, e ->
                start = s
                end = e
            }
        )

        rangeSlider.onValuesChange?.invoke(30f, 70f)
        assertEquals(30f, start)
        assertEquals(70f, end)
    }

    // ==================== DatePicker Tests ====================

    @Test
    fun testDatePickerCreation() {
        val datePicker = DatePicker(
            id = "test-date",
            label = "Select Date"
        )

        assertEquals("test-date", datePicker.id)
        assertEquals("Select Date", datePicker.label)
        assertEquals("Select date", datePicker.placeholder)
        assertTrue(datePicker.enabled)
    }

    @Test
    fun testDatePickerWithDate() {
        val date = Date(2025, 11, 9)
        val datePicker = DatePicker(
            id = "date-with-value",
            selectedDate = date
        )

        assertEquals(date, datePicker.selectedDate)
    }

    @Test
    fun testDateFormatting() {
        val date = Date(2025, 11, 9)

        assertEquals("2025-11-09", date.format(DateFormat.ISO_8601))
        assertEquals("11/09/2025", date.format(DateFormat.US))
        assertEquals("09/11/2025", date.format(DateFormat.EU))
    }

    // ==================== TimePicker Tests ====================

    @Test
    fun testTimePickerCreation() {
        val timePicker = TimePicker(
            id = "test-time",
            is24Hour = true
        )

        assertEquals("test-time", timePicker.id)
        assertTrue(timePicker.is24Hour)
        assertTrue(timePicker.enabled)
    }

    @Test
    fun testTimeFormatting() {
        val time = Time(14, 30, 0)

        assertEquals("14:30", time.format(is24Hour = true))
        assertEquals("02:30 PM", time.format(is24Hour = false))
    }

    @Test
    fun testTimeMidnight() {
        val midnight = Time(0, 0, 0)

        assertEquals("00:00", midnight.format(is24Hour = true))
        assertEquals("12:00 AM", midnight.format(is24Hour = false))
    }

    // ==================== RadioButton Tests ====================

    @Test
    fun testRadioButtonCreation() {
        val radio = RadioButton(
            id = "test-radio",
            selected = false,
            label = "Option 1"
        )

        assertEquals("test-radio", radio.id)
        assertFalse(radio.selected)
        assertEquals("Option 1", radio.label)
    }

    @Test
    fun testRadioButtonCallback() {
        var wasSelected = false
        val radio = RadioButton(
            id = "callback-radio",
            selected = false,
            onSelected = { wasSelected = true }
        )

        radio.onSelected?.invoke()
        assertTrue(wasSelected)
    }

    // ==================== RadioGroup Tests ====================

    @Test
    fun testRadioGroupCreation() {
        val options = listOf(
            RadioOption("opt1", "Option 1"),
            RadioOption("opt2", "Option 2"),
            RadioOption("opt3", "Option 3")
        )

        val group = RadioGroup(
            id = "test-group",
            options = options,
            selectedIndex = 0
        )

        assertEquals(3, group.options.size)
        assertEquals(0, group.selectedIndex)
    }

    @Test
    fun testRadioGroupCallback() {
        var selectedIdx = -1
        val options = listOf(
            RadioOption("opt1", "Option 1"),
            RadioOption("opt2", "Option 2")
        )

        val group = RadioGroup(
            id = "callback-group",
            options = options,
            onSelectionChanged = { selectedIdx = it }
        )

        group.onSelectionChanged?.invoke(1)
        assertEquals(1, selectedIdx)
    }

    // ==================== Dropdown Tests ====================

    @Test
    fun testDropdownCreation() {
        val options = listOf(
            DropdownOption("opt1", "Option 1"),
            DropdownOption("opt2", "Option 2")
        )

        val dropdown = Dropdown(
            id = "test-dropdown",
            options = options,
            label = "Choose"
        )

        assertEquals("test-dropdown", dropdown.id)
        assertEquals(2, dropdown.options.size)
        assertEquals("Choose", dropdown.label)
        assertFalse(dropdown.searchable)
    }

    @Test
    fun testDropdownSearchable() {
        val dropdown = Dropdown(
            id = "searchable-dropdown",
            options = emptyList(),
            searchable = true
        )

        assertTrue(dropdown.searchable)
    }

    // ==================== Autocomplete Tests ====================

    @Test
    fun testAutocompleteCreation() {
        val options = listOf(
            AutocompleteOption("opt1", "Apple"),
            AutocompleteOption("opt2", "Banana"),
            AutocompleteOption("opt3", "Cherry")
        )

        val autocomplete = Autocomplete(
            id = "test-autocomplete",
            options = options,
            minChars = 2,
            maxResults = 5
        )

        assertEquals(3, autocomplete.options.size)
        assertEquals(2, autocomplete.minChars)
        assertEquals(5, autocomplete.maxResults)
    }

    @Test
    fun testAutocompleteWithDescription() {
        val option = AutocompleteOption(
            id = "opt1",
            label = "Apple",
            description = "A fruit"
        )

        assertEquals("Apple", option.label)
        assertEquals("A fruit", option.description)
    }

    // ==================== FileUpload Tests ====================

    @Test
    fun testFileUploadCreation() {
        val fileUpload = FileUpload(
            id = "test-upload",
            multiple = true,
            maxFiles = 5,
            maxSize = 10_000_000 // 10 MB
        )

        assertEquals("test-upload", fileUpload.id)
        assertTrue(fileUpload.multiple)
        assertEquals(5, fileUpload.maxFiles)
        assertEquals(10_000_000, fileUpload.maxSize)
    }

    @Test
    fun testFileInfoCreation() {
        val fileInfo = FileInfo(
            id = "file1",
            name = "document.pdf",
            size = 1024,
            mimeType = "application/pdf",
            uri = "content://...",
            lastModified = 1234567890
        )

        assertEquals("document.pdf", fileInfo.name)
        assertEquals(1024, fileInfo.size)
        assertEquals("application/pdf", fileInfo.mimeType)
    }

    // ==================== ImagePicker Tests ====================

    @Test
    fun testImagePickerCreation() {
        val imagePicker = ImagePicker(
            id = "test-image-picker",
            multiple = true,
            maxImages = 3,
            allowCamera = true,
            allowGallery = true
        )

        assertEquals("test-image-picker", imagePicker.id)
        assertTrue(imagePicker.multiple)
        assertEquals(3, imagePicker.maxImages)
        assertTrue(imagePicker.allowCamera)
        assertTrue(imagePicker.allowGallery)
    }

    @Test
    fun testImageInfoCreation() {
        val imageInfo = ImageInfo(
            id = "img1",
            name = "photo.jpg",
            size = 2048,
            uri = "content://...",
            width = 1920,
            height = 1080
        )

        assertEquals("photo.jpg", imageInfo.name)
        assertEquals(1920, imageInfo.width)
        assertEquals(1080, imageInfo.height)
    }

    // ==================== Rating Tests ====================

    @Test
    fun testRatingCreation() {
        val rating = Rating(
            id = "test-rating",
            rating = 3.5f,
            maxRating = 5,
            allowHalf = true
        )

        assertEquals("test-rating", rating.id)
        assertEquals(3.5f, rating.rating)
        assertEquals(5, rating.maxRating)
        assertTrue(rating.allowHalf)
    }

    @Test
    fun testRatingSize() {
        val small = Rating(
            id = "small",
            rating = 4f,
            size = RatingSize.Small
        )

        val large = Rating(
            id = "large",
            rating = 4f,
            size = RatingSize.Large
        )

        assertEquals(RatingSize.Small, small.size)
        assertEquals(RatingSize.Large, large.size)
    }

    // ==================== SearchBar Tests ====================

    @Test
    fun testSearchBarCreation() {
        val searchBar = SearchBar(
            id = "test-search",
            query = "",
            placeholder = "Search items...",
            debounceMs = 300
        )

        assertEquals("test-search", searchBar.id)
        assertEquals("", searchBar.query)
        assertEquals("Search items...", searchBar.placeholder)
        assertEquals(300, searchBar.debounceMs)
    }

    @Test
    fun testSearchBarCallbacks() {
        var queryChangedValue = ""
        var searchValue = ""
        var clearCalled = false

        val searchBar = SearchBar(
            id = "callback-search",
            query = "test",
            onQueryChanged = { queryChangedValue = it },
            onSearch = { searchValue = it },
            onClear = { clearCalled = true }
        )

        searchBar.onQueryChanged?.invoke("new query")
        assertEquals("new query", queryChangedValue)

        searchBar.onSearch?.invoke("search term")
        assertEquals("search term", searchValue)

        searchBar.onClear?.invoke()
        assertTrue(clearCalled)
    }

    @Test
    fun testSearchBarIcons() {
        val searchBar = SearchBar(
            id = "icon-search",
            showSearchIcon = true,
            showClearButton = true
        )

        assertTrue(searchBar.showSearchIcon)
        assertTrue(searchBar.showClearButton)
    }

    // ==================== Edge Cases ====================

    @Test
    fun testDisabledComponents() {
        val slider = Slider(id = "disabled", value = 50f, enabled = false)
        val dropdown = Dropdown(id = "disabled", options = emptyList(), enabled = false)
        val rating = Rating(id = "disabled", rating = 3f, enabled = false)

        assertFalse(slider.enabled)
        assertFalse(dropdown.enabled)
        assertFalse(rating.enabled)
    }

    @Test
    fun testEmptyOptions() {
        val dropdown = Dropdown(id = "empty", options = emptyList())
        val autocomplete = Autocomplete(id = "empty", options = emptyList())

        assertTrue(dropdown.options.isEmpty())
        assertTrue(autocomplete.options.isEmpty())
    }

    @Test
    fun testMaximumValues() {
        val slider = Slider(id = "max", value = 100f, min = 0f, max = 100f)
        val rating = Rating(id = "max", rating = 5f, maxRating = 5)

        assertEquals(100f, slider.value)
        assertEquals(5f, rating.rating)
    }
}
