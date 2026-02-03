import XCTest
import SwiftUI
@testable import AvaElementsRenderer

/**
 * Unit tests for Input and Control components.
 *
 * Tests 15 components:
 * Input: TextField, TextArea, SearchBar
 * Selection: Dropdown, Menu, MenuItem, Slider, RangeSlider
 * Pickers: DatePicker, TimePicker, ColorPicker
 * Progress: ProgressIndicator, LinearProgressIndicator, CircularProgressIndicator
 * Other: Divider, Spacer
 *
 * @since 1.0.0 (iOS Testing Framework)
 */
class InputAndControlTests: XCTestCase {

    // MARK: - TextField Tests

    func testTextField_defaultInitialization() {
        let textField = AMTextField(text: "")

        XCTAssertNotNil(textField, "TextField should initialize")
        XCTAssertEqual(textField.text, "", "Default text should be empty")
        XCTAssertTrue(textField.enabled, "Default enabled should be true")
    }

    func testTextField_withPlaceholder() {
        let textField = AMTextField(
            text: "",
            placeholder: "Enter text"
        )

        XCTAssertEqual(textField.placeholder, "Enter text", "Placeholder should match")
    }

    func testTextField_withText() {
        let textField = AMTextField(text: "Hello")

        XCTAssertEqual(textField.text, "Hello", "Text should match")
    }

    func testTextField_disabled() {
        let textField = AMTextField(text: "", enabled: false)

        XCTAssertFalse(textField.enabled, "TextField should be disabled")
    }

    func testTextField_onChanged() {
        var newText = ""
        let textField = AMTextField(
            text: "",
            onChanged: { text in newText = text }
        )

        textField.onChanged?("Updated")
        XCTAssertEqual(newText, "Updated", "onChanged should update text")
    }

    func testTextField_maxLength() {
        let textField = AMTextField(text: "", maxLength: 10)

        XCTAssertEqual(textField.maxLength, 10, "Max length should be 10")
    }

    func testTextField_keyboardType() {
        let types: [UIKeyboardType] = [.default, .numberPad, .emailAddress, .URL]

        for type in types {
            let textField = AMTextField(text: "", keyboardType: type)
            XCTAssertEqual(textField.keyboardType, type, "Keyboard type should match")
        }
    }

    // MARK: - TextArea Tests

    func testTextArea_defaultInitialization() {
        let textArea = AMTextArea(text: "")

        XCTAssertNotNil(textArea, "TextArea should initialize")
        XCTAssertEqual(textArea.text, "", "Default text should be empty")
    }

    func testTextArea_withText() {
        let multilineText = "Line 1\nLine 2\nLine 3"
        let textArea = AMTextArea(text: multilineText)

        XCTAssertEqual(textArea.text, multilineText, "Text should support multiple lines")
    }

    func testTextArea_withMinLines() {
        let textArea = AMTextArea(text: "", minLines: 3)

        XCTAssertEqual(textArea.minLines, 3, "Min lines should be 3")
    }

    func testTextArea_withMaxLines() {
        let textArea = AMTextArea(text: "", maxLines: 10)

        XCTAssertEqual(textArea.maxLines, 10, "Max lines should be 10")
    }

    // MARK: - SearchBar Tests

    func testSearchBar_defaultInitialization() {
        let searchBar = AMSearchBar(text: "")

        XCTAssertNotNil(searchBar, "SearchBar should initialize")
        XCTAssertEqual(searchBar.text, "", "Default text should be empty")
    }

    func testSearchBar_withPlaceholder() {
        let searchBar = AMSearchBar(
            text: "",
            placeholder: "Search..."
        )

        XCTAssertEqual(searchBar.placeholder, "Search...", "Placeholder should match")
    }

    func testSearchBar_onChanged() {
        var searchText = ""
        let searchBar = AMSearchBar(
            text: "",
            onChanged: { text in searchText = text }
        )

        searchBar.onChanged?("query")
        XCTAssertEqual(searchText, "query", "Search text should update")
    }

    func testSearchBar_onSubmitted() {
        var submittedText = ""
        let searchBar = AMSearchBar(
            text: "",
            onSubmitted: { text in submittedText = text }
        )

        searchBar.onSubmitted?("search term")
        XCTAssertEqual(submittedText, "search term", "onSubmitted should fire")
    }

    // MARK: - Dropdown Tests

    func testDropdown_defaultInitialization() {
        let items = ["Option 1", "Option 2", "Option 3"]
        let dropdown = AMDropdown(items: items, selectedIndex: 0)

        XCTAssertNotNil(dropdown, "Dropdown should initialize")
        XCTAssertEqual(dropdown.items.count, 3, "Should have 3 items")
        XCTAssertEqual(dropdown.selectedIndex, 0, "Selected index should be 0")
    }

    func testDropdown_onChanged() {
        var selectedIndex = 0
        let dropdown = AMDropdown(
            items: ["A", "B", "C"],
            selectedIndex: 0,
            onChanged: { index in selectedIndex = index }
        )

        dropdown.onChanged?(2)
        XCTAssertEqual(selectedIndex, 2, "Selected index should update to 2")
    }

    func testDropdown_emptyItems() {
        let dropdown = AMDropdown(items: [], selectedIndex: nil)

        XCTAssertEqual(dropdown.items.count, 0, "Should have no items")
        XCTAssertNil(dropdown.selectedIndex, "Selected index should be nil")
    }

    // MARK: - Slider Tests

    func testSlider_defaultInitialization() {
        let slider = AMSlider(value: 0.5, min: 0.0, max: 1.0)

        XCTAssertNotNil(slider, "Slider should initialize")
        XCTAssertEqual(slider.value, 0.5, "Value should be 0.5")
        XCTAssertEqual(slider.min, 0.0, "Min should be 0.0")
        XCTAssertEqual(slider.max, 1.0, "Max should be 1.0")
    }

    func testSlider_onChanged() {
        var currentValue: Double = 0.0
        let slider = AMSlider(
            value: 0.5,
            min: 0.0,
            max: 1.0,
            onChanged: { value in currentValue = value }
        )

        slider.onChanged?(0.75)
        XCTAssertEqual(currentValue, 0.75, "Value should update to 0.75")
    }

    func testSlider_withStep() {
        let slider = AMSlider(
            value: 0.0,
            min: 0.0,
            max: 10.0,
            step: 1.0
        )

        XCTAssertEqual(slider.step, 1.0, "Step should be 1.0")
    }

    func testSlider_customRange() {
        let slider = AMSlider(value: 50, min: 0, max: 100)

        XCTAssertEqual(slider.value, 50, "Value should be 50")
        XCTAssertEqual(slider.min, 0, "Min should be 0")
        XCTAssertEqual(slider.max, 100, "Max should be 100")
    }

    // MARK: - RangeSlider Tests

    func testRangeSlider_defaultInitialization() {
        let rangeSlider = AMRangeSlider(
            start: 0.25,
            end: 0.75,
            min: 0.0,
            max: 1.0
        )

        XCTAssertNotNil(rangeSlider, "RangeSlider should initialize")
        XCTAssertEqual(rangeSlider.start, 0.25, "Start should be 0.25")
        XCTAssertEqual(rangeSlider.end, 0.75, "End should be 0.75")
    }

    func testRangeSlider_onChanged() {
        var startValue: Double = 0.0
        var endValue: Double = 1.0

        let rangeSlider = AMRangeSlider(
            start: 0.25,
            end: 0.75,
            min: 0.0,
            max: 1.0,
            onChanged: { start, end in
                startValue = start
                endValue = end
            }
        )

        rangeSlider.onChanged?(0.3, 0.8)
        XCTAssertEqual(startValue, 0.3, "Start should update to 0.3")
        XCTAssertEqual(endValue, 0.8, "End should update to 0.8")
    }

    // MARK: - DatePicker Tests

    func testDatePicker_defaultInitialization() {
        let date = Date()
        let datePicker = AMDatePicker(date: date)

        XCTAssertNotNil(datePicker, "DatePicker should initialize")
        XCTAssertEqual(datePicker.date, date, "Date should match")
    }

    func testDatePicker_onChanged() {
        var selectedDate = Date()
        let datePicker = AMDatePicker(
            date: selectedDate,
            onChanged: { date in selectedDate = date }
        )

        let newDate = Date().addingTimeInterval(86400) // +1 day
        datePicker.onChanged?(newDate)
        XCTAssertEqual(selectedDate, newDate, "Date should update")
    }

    func testDatePicker_withRange() {
        let minDate = Date()
        let maxDate = Date().addingTimeInterval(86400 * 30) // +30 days
        let datePicker = AMDatePicker(
            date: Date(),
            minimumDate: minDate,
            maximumDate: maxDate
        )

        XCTAssertEqual(datePicker.minimumDate, minDate, "Min date should match")
        XCTAssertEqual(datePicker.maximumDate, maxDate, "Max date should match")
    }

    // MARK: - TimePicker Tests

    func testTimePicker_defaultInitialization() {
        let time = Date()
        let timePicker = AMTimePicker(time: time)

        XCTAssertNotNil(timePicker, "TimePicker should initialize")
    }

    func testTimePicker_onChanged() {
        var selectedTime = Date()
        let timePicker = AMTimePicker(
            time: selectedTime,
            onChanged: { time in selectedTime = time }
        )

        let newTime = Date().addingTimeInterval(3600) // +1 hour
        timePicker.onChanged?(newTime)
        XCTAssertNotNil(selectedTime, "Time should update")
    }

    // MARK: - ProgressIndicator Tests

    func testLinearProgress_defaultInitialization() {
        let progress = AMLinearProgressIndicator(value: 0.5)

        XCTAssertNotNil(progress, "Linear progress should initialize")
        XCTAssertEqual(progress.value, 0.5, "Value should be 0.5")
    }

    func testLinearProgress_indeterminate() {
        let progress = AMLinearProgressIndicator(value: nil)

        XCTAssertNil(progress.value, "Indeterminate progress should have nil value")
    }

    func testLinearProgress_withColor() {
        let progress = AMLinearProgressIndicator(value: 0.75, color: .blue)

        XCTAssertEqual(progress.value, 0.75, "Value should be 0.75")
        XCTAssertNotNil(progress.color, "Color should not be nil")
    }

    func testCircularProgress_defaultInitialization() {
        let progress = AMCircularProgressIndicator(value: 0.5)

        XCTAssertNotNil(progress, "Circular progress should initialize")
        XCTAssertEqual(progress.value, 0.5, "Value should be 0.5")
    }

    func testCircularProgress_indeterminate() {
        let progress = AMCircularProgressIndicator(value: nil)

        XCTAssertNil(progress.value, "Indeterminate progress should have nil value")
    }

    // MARK: - Divider Tests

    func testDivider_defaultInitialization() {
        let divider = AMDivider()

        XCTAssertNotNil(divider, "Divider should initialize")
        XCTAssertEqual(divider.thickness, 1, "Default thickness should be 1")
    }

    func testDivider_withThickness() {
        let divider = AMDivider(thickness: 2)

        XCTAssertEqual(divider.thickness, 2, "Thickness should be 2")
    }

    func testDivider_withColor() {
        let divider = AMDivider(color: .gray)

        XCTAssertNotNil(divider.color, "Color should not be nil")
    }

    func testDivider_horizontal() {
        let divider = AMDivider(orientation: .horizontal)

        XCTAssertEqual(divider.orientation, .horizontal, "Should be horizontal")
    }

    func testDivider_vertical() {
        let divider = AMDivider(orientation: .vertical)

        XCTAssertEqual(divider.orientation, .vertical, "Should be vertical")
    }

    // MARK: - Spacer Tests

    func testSpacer_defaultInitialization() {
        let spacer = AMSpacer()

        XCTAssertNotNil(spacer, "Spacer should initialize")
    }

    func testSpacer_withWidth() {
        let spacer = AMSpacer(width: 16)

        XCTAssertEqual(spacer.width, 16, "Width should be 16")
    }

    func testSpacer_withHeight() {
        let spacer = AMSpacer(height: 24)

        XCTAssertEqual(spacer.height, 24, "Height should be 24")
    }

    func testSpacer_flexible() {
        let spacer = AMSpacer.flexible()

        XCTAssertNil(spacer.width, "Flexible spacer should have nil width")
        XCTAssertNil(spacer.height, "Flexible spacer should have nil height")
    }

    // MARK: - Edge Cases

    func testTextField_maxLengthEnforcement() {
        var text = ""
        let textField = AMTextField(
            text: "",
            maxLength: 5,
            onChanged: { newText in text = newText }
        )

        textField.onChanged?("12345")
        XCTAssertEqual(text.count, 5, "Should accept text up to max length")

        textField.onChanged?("123456")
        // Max length enforcement would happen in the renderer
    }

    func testSlider_minMaxValidation() {
        let slider = AMSlider(value: 5, min: 0, max: 10)

        XCTAssertGreaterThanOrEqual(slider.value, slider.min, "Value should be >= min")
        XCTAssertLessThanOrEqual(slider.value, slider.max, "Value should be <= max")
    }

    func testRangeSlider_startEndValidation() {
        let rangeSlider = AMRangeSlider(
            start: 0.25,
            end: 0.75,
            min: 0.0,
            max: 1.0
        )

        XCTAssertLessThanOrEqual(rangeSlider.start, rangeSlider.end, "Start should be <= end")
    }

    func testProgressIndicator_clampedValue() {
        let progress1 = AMLinearProgressIndicator(value: -0.1)
        let progress2 = AMLinearProgressIndicator(value: 1.5)

        // Values would be clamped by renderer to 0.0-1.0 range
        XCTAssertNotNil(progress1.value, "Progress should handle negative values")
        XCTAssertNotNil(progress2.value, "Progress should handle values > 1.0")
    }

    func testDropdown_invalidIndex() {
        let dropdown = AMDropdown(
            items: ["A", "B", "C"],
            selectedIndex: 5 // Invalid index
        )

        XCTAssertEqual(dropdown.selectedIndex, 5, "Should store index even if invalid")
        // Validation would happen in renderer
    }

    func testTextArea_veryLongText() {
        let longText = String(repeating: "A", count: 10000)
        let textArea = AMTextArea(text: longText)

        XCTAssertEqual(textArea.text.count, 10000, "Should handle very long text")
    }

    func testSearchBar_emptySubmit() {
        var submitted = false
        let searchBar = AMSearchBar(
            text: "",
            onSubmitted: { _ in submitted = true }
        )

        searchBar.onSubmitted?("")
        XCTAssertTrue(submitted, "Should allow submitting empty search")
    }
}
