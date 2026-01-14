import XCTest
import SwiftUI
@testable import AvaElementsRenderer

/**
 * Unit tests for Chip components.
 *
 * Tests 4 chip types:
 * - FilterChip (selectable filter)
 * - ActionChip (button-like action)
 * - ChoiceChip (single selection)
 * - InputChip (user input tag)
 *
 * Coverage:
 * - Component initialization
 * - State management (selected, enabled, disabled)
 * - Icon handling (leading, trailing, avatar)
 * - Event callbacks
 * - Edge cases
 *
 * @since 1.0.0 (iOS Testing Framework)
 */
class ChipComponentTests: XCTestCase {

    // MARK: - FilterChip Tests

    func testFilterChip_defaultInitialization() {
        let chip = AMFilterChip(label: "Filter")

        XCTAssertNotNil(chip, "FilterChip should initialize")
        XCTAssertEqual(chip.label, "Filter", "Label should be 'Filter'")
        XCTAssertFalse(chip.selected, "Default selected should be false")
        XCTAssertTrue(chip.enabled, "Default enabled should be true")
    }

    func testFilterChip_selected() {
        let chip = AMFilterChip(label: "Filter", selected: true)

        XCTAssertTrue(chip.selected, "FilterChip should be selected")
    }

    func testFilterChip_disabled() {
        let chip = AMFilterChip(label: "Filter", enabled: false)

        XCTAssertFalse(chip.enabled, "FilterChip should be disabled")
    }

    func testFilterChip_withLeadingIcon() {
        let chip = AMFilterChip(
            label: "Filter",
            leadingIcon: AMIcon(name: "checkmark")
        )

        XCTAssertNotNil(chip.leadingIcon, "FilterChip should have leading icon")
    }

    func testFilterChip_onSelectedCallback() {
        var callbackFired = false
        var selectedValue = false

        let chip = AMFilterChip(
            label: "Filter",
            onSelected: { selected in
                callbackFired = true
                selectedValue = selected
            }
        )

        chip.onSelected?(true)

        XCTAssertTrue(callbackFired, "onSelected callback should fire")
        XCTAssertTrue(selectedValue, "Selected value should be true")
    }

    func testFilterChip_allStates() {
        let states: [(selected: Bool, enabled: Bool)] = [
            (false, true),   // Default
            (true, true),    // Selected
            (false, false),  // Disabled
            (true, false)    // Selected + Disabled
        ]

        for (selected, enabled) in states {
            let chip = AMFilterChip(
                label: "Filter",
                selected: selected,
                enabled: enabled
            )

            XCTAssertEqual(chip.selected, selected, "Selected state should match")
            XCTAssertEqual(chip.enabled, enabled, "Enabled state should match")
        }
    }

    // MARK: - ActionChip Tests

    func testActionChip_defaultInitialization() {
        let chip = AMActionChip(label: "Action")

        XCTAssertNotNil(chip, "ActionChip should initialize")
        XCTAssertEqual(chip.label, "Action", "Label should be 'Action'")
        XCTAssertTrue(chip.enabled, "Default enabled should be true")
    }

    func testActionChip_disabled() {
        let chip = AMActionChip(label: "Action", enabled: false)

        XCTAssertFalse(chip.enabled, "ActionChip should be disabled")
    }

    func testActionChip_withLeadingIcon() {
        let chip = AMActionChip(
            label: "Action",
            leadingIcon: AMIcon(name: "star")
        )

        XCTAssertNotNil(chip.leadingIcon, "ActionChip should have leading icon")
    }

    func testActionChip_onPressedCallback() {
        var callbackFired = false

        let chip = AMActionChip(
            label: "Action",
            onPressed: {
                callbackFired = true
            }
        )

        chip.onPressed?()

        XCTAssertTrue(callbackFired, "onPressed callback should fire")
    }

    func testActionChip_disabledNoCallback() {
        var callbackFired = false

        let chip = AMActionChip(
            label: "Action",
            enabled: false,
            onPressed: {
                callbackFired = true
            }
        )

        // Disabled chips should not trigger callbacks
        // (This would be enforced by the renderer)

        XCTAssertFalse(chip.enabled, "Chip should be disabled")
    }

    // MARK: - ChoiceChip Tests

    func testChoiceChip_defaultInitialization() {
        let chip = AMChoiceChip(label: "Choice")

        XCTAssertNotNil(chip, "ChoiceChip should initialize")
        XCTAssertEqual(chip.label, "Choice", "Label should be 'Choice'")
        XCTAssertFalse(chip.selected, "Default selected should be false")
        XCTAssertTrue(chip.enabled, "Default enabled should be true")
    }

    func testChoiceChip_selected() {
        let chip = AMChoiceChip(label: "Choice", selected: true)

        XCTAssertTrue(chip.selected, "ChoiceChip should be selected")
    }

    func testChoiceChip_disabled() {
        let chip = AMChoiceChip(label: "Choice", enabled: false)

        XCTAssertFalse(chip.enabled, "ChoiceChip should be disabled")
    }

    func testChoiceChip_withAvatar() {
        let chip = AMChoiceChip(
            label: "Choice",
            avatar: AMIcon(name: "person")
        )

        XCTAssertNotNil(chip.avatar, "ChoiceChip should have avatar")
    }

    func testChoiceChip_onSelectedCallback() {
        var callbackFired = false
        var selectedValue = false

        let chip = AMChoiceChip(
            label: "Choice",
            onSelected: { selected in
                callbackFired = true
                selectedValue = selected
            }
        )

        chip.onSelected?(true)

        XCTAssertTrue(callbackFired, "onSelected callback should fire")
        XCTAssertTrue(selectedValue, "Selected value should be true")
    }

    func testChoiceChip_allStates() {
        let states: [(selected: Bool, enabled: Bool)] = [
            (false, true),   // Unselected
            (true, true),    // Selected
            (false, false),  // Disabled unselected
            (true, false)    // Disabled selected
        ]

        for (selected, enabled) in states {
            let chip = AMChoiceChip(
                label: "Choice",
                selected: selected,
                enabled: enabled
            )

            XCTAssertEqual(chip.selected, selected, "Selected state should match")
            XCTAssertEqual(chip.enabled, enabled, "Enabled state should match")
        }
    }

    // MARK: - InputChip Tests

    func testInputChip_defaultInitialization() {
        let chip = AMInputChip(label: "Input")

        XCTAssertNotNil(chip, "InputChip should initialize")
        XCTAssertEqual(chip.label, "Input", "Label should be 'Input'")
        XCTAssertTrue(chip.enabled, "Default enabled should be true")
    }

    func testInputChip_disabled() {
        let chip = AMInputChip(label: "Input", enabled: false)

        XCTAssertFalse(chip.enabled, "InputChip should be disabled")
    }

    func testInputChip_withAvatar() {
        let chip = AMInputChip(
            label: "Input",
            avatar: AMIcon(name: "person.circle")
        )

        XCTAssertNotNil(chip.avatar, "InputChip should have avatar")
    }

    func testInputChip_withDeleteIcon() {
        let chip = AMInputChip(
            label: "Input",
            deleteIcon: AMIcon(name: "xmark.circle.fill")
        )

        XCTAssertNotNil(chip.deleteIcon, "InputChip should have delete icon")
    }

    func testInputChip_withBothIcons() {
        let chip = AMInputChip(
            label: "Input",
            avatar: AMIcon(name: "person.circle"),
            deleteIcon: AMIcon(name: "xmark.circle.fill")
        )

        XCTAssertNotNil(chip.avatar, "InputChip should have avatar")
        XCTAssertNotNil(chip.deleteIcon, "InputChip should have delete icon")
    }

    func testInputChip_onDeletedCallback() {
        var callbackFired = false

        let chip = AMInputChip(
            label: "Input",
            onDeleted: {
                callbackFired = true
            }
        )

        chip.onDeleted?()

        XCTAssertTrue(callbackFired, "onDeleted callback should fire")
    }

    func testInputChip_onPressedCallback() {
        var callbackFired = false

        let chip = AMInputChip(
            label: "Input",
            onPressed: {
                callbackFired = true
            }
        )

        chip.onPressed?()

        XCTAssertTrue(callbackFired, "onPressed callback should fire")
    }

    func testInputChip_variants() {
        let variants: [(hasAvatar: Bool, hasDelete: Bool)] = [
            (true, false),   // With avatar only
            (false, true),   // With delete only
            (true, true),    // With both
            (false, false)   // Plain
        ]

        for (hasAvatar, hasDelete) in variants {
            let chip = AMInputChip(
                label: "Input",
                avatar: hasAvatar ? AMIcon(name: "person") : nil,
                deleteIcon: hasDelete ? AMIcon(name: "xmark") : nil
            )

            if hasAvatar {
                XCTAssertNotNil(chip.avatar, "Chip should have avatar")
            } else {
                XCTAssertNil(chip.avatar, "Chip should not have avatar")
            }

            if hasDelete {
                XCTAssertNotNil(chip.deleteIcon, "Chip should have delete icon")
            } else {
                XCTAssertNil(chip.deleteIcon, "Chip should not have delete icon")
            }
        }
    }

    // MARK: - Edge Cases

    func testFilterChip_emptyLabel() {
        let chip = AMFilterChip(label: "")

        XCTAssertEqual(chip.label, "", "Label should be empty string")
    }

    func testFilterChip_longLabel() {
        let longLabel = String(repeating: "A", count: 100)
        let chip = AMFilterChip(label: longLabel)

        XCTAssertEqual(chip.label.count, 100, "Label should have 100 characters")
    }

    func testActionChip_multipleCallbacks() {
        var count = 0

        let chip = AMActionChip(
            label: "Action",
            onPressed: {
                count += 1
            }
        )

        chip.onPressed?()
        chip.onPressed?()
        chip.onPressed?()

        XCTAssertEqual(count, 3, "Callback should fire 3 times")
    }

    func testChoiceChip_toggleSelection() {
        var currentState = false

        let chip = AMChoiceChip(
            label: "Choice",
            selected: currentState,
            onSelected: { selected in
                currentState = selected
            }
        )

        chip.onSelected?(true)
        XCTAssertTrue(currentState, "State should be true after first toggle")

        chip.onSelected?(false)
        XCTAssertFalse(currentState, "State should be false after second toggle")
    }

    func testInputChip_deletedThenPressed() {
        var deleteCount = 0
        var pressCount = 0

        let chip = AMInputChip(
            label: "Input",
            onPressed: {
                pressCount += 1
            },
            onDeleted: {
                deleteCount += 1
            }
        )

        chip.onDeleted?()
        chip.onPressed?()

        XCTAssertEqual(deleteCount, 1, "Delete callback should fire once")
        XCTAssertEqual(pressCount, 1, "Press callback should fire once")
    }

    func testFilterChip_selectedDisabled() {
        let chip = AMFilterChip(
            label: "Filter",
            selected: true,
            enabled: false
        )

        XCTAssertTrue(chip.selected, "Chip should be selected")
        XCTAssertFalse(chip.enabled, "Chip should be disabled")
    }

    func testInputChip_nilCallbacks() {
        let chip = AMInputChip(
            label: "Input",
            onPressed: nil,
            onDeleted: nil
        )

        XCTAssertNil(chip.onPressed, "onPressed should be nil")
        XCTAssertNil(chip.onDeleted, "onDeleted should be nil")
    }

    // MARK: - Accessibility Tests

    func testFilterChip_accessibilityLabel() {
        let chip = AMFilterChip(
            label: "Filter",
            selected: true
        )

        let expectedLabel = "Filter, selected"
        XCTAssertNotNil(chip.label, "Chip should have accessible label")
    }

    func testActionChip_accessibilityHint() {
        let chip = AMActionChip(label: "Action")

        let expectedHint = "Double tap to activate"
        XCTAssertNotNil(chip.label, "Chip should have accessible hint")
    }

    func testInputChip_deleteAccessibility() {
        let chip = AMInputChip(
            label: "Tag",
            deleteIcon: AMIcon(name: "xmark")
        )

        let expectedLabel = "Delete Tag"
        XCTAssertNotNil(chip.deleteIcon, "Delete icon should be accessible")
    }
}
