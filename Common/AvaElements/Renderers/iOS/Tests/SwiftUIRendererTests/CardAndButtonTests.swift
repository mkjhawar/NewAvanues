import XCTest
import SwiftUI
@testable import AvaElementsRenderer

/**
 * Unit tests for Card and Button components.
 *
 * Tests 12 components:
 * Cards: Card, OutlinedCard, ElevatedCard
 * Buttons: TextButton, ElevatedButton, FilledButton, OutlinedButton, IconButton, FloatingActionButton
 * Toggle: Switch, Checkbox, Radio
 *
 * @since 1.0.0 (iOS Testing Framework)
 */
class CardAndButtonTests: XCTestCase {

    // MARK: - Card Tests

    func testCard_defaultInitialization() {
        let card = AMCard()

        XCTAssertNotNil(card, "Card should initialize")
        XCTAssertNil(card.child, "Default child should be nil")
    }

    func testCard_withChild() {
        let child = AMText(text: "Card Content")
        let card = AMCard(child: child)

        XCTAssertNotNil(card.child, "Card should have child")
    }

    func testCard_withElevation() {
        let card = AMCard(elevation: 4)

        XCTAssertEqual(card.elevation, 4, "Card elevation should be 4")
    }

    func testCard_withColor() {
        let card = AMCard(color: .blue)

        XCTAssertNotNil(card.color, "Card should have color")
    }

    func testCard_withBorderRadius() {
        let card = AMCard(borderRadius: 12)

        XCTAssertEqual(card.borderRadius, 12, "Border radius should be 12")
    }

    func testCard_onTap() {
        var tapped = false
        let card = AMCard(onTap: { tapped = true })

        card.onTap?()
        XCTAssertTrue(tapped, "onTap callback should fire")
    }

    // MARK: - OutlinedCard Tests

    func testOutlinedCard_defaultInitialization() {
        let card = AMOutlinedCard()

        XCTAssertNotNil(card, "OutlinedCard should initialize")
        XCTAssertEqual(card.borderWidth, 1, "Default border width should be 1")
    }

    func testOutlinedCard_withBorderColor() {
        let card = AMOutlinedCard(borderColor: .red)

        XCTAssertNotNil(card.borderColor, "Border color should not be nil")
    }

    func testOutlinedCard_withChild() {
        let child = AMText(text: "Outlined Content")
        let card = AMOutlinedCard(child: child)

        XCTAssertNotNil(card.child, "OutlinedCard should have child")
    }

    // MARK: - ElevatedCard Tests

    func testElevatedCard_defaultInitialization() {
        let card = AMElevatedCard()

        XCTAssertNotNil(card, "ElevatedCard should initialize")
        XCTAssertEqual(card.elevation, 2, "Default elevation should be 2")
    }

    func testElevatedCard_customElevation() {
        let card = AMElevatedCard(elevation: 8)

        XCTAssertEqual(card.elevation, 8, "Elevation should be 8")
    }

    // MARK: - TextButton Tests

    func testTextButton_defaultInitialization() {
        let button = AMTextButton(text: "Click me")

        XCTAssertNotNil(button, "TextButton should initialize")
        XCTAssertEqual(button.text, "Click me", "Button text should match")
        XCTAssertTrue(button.enabled, "Default enabled should be true")
    }

    func testTextButton_disabled() {
        let button = AMTextButton(text: "Click me", enabled: false)

        XCTAssertFalse(button.enabled, "Button should be disabled")
    }

    func testTextButton_onPressed() {
        var pressed = false
        let button = AMTextButton(
            text: "Click me",
            onPressed: { pressed = true }
        )

        button.onPressed?()
        XCTAssertTrue(pressed, "onPressed callback should fire")
    }

    func testTextButton_withIcon() {
        let button = AMTextButton(
            text: "Click me",
            icon: AMIcon(name: "star")
        )

        XCTAssertNotNil(button.icon, "Button should have icon")
    }

    // MARK: - ElevatedButton Tests

    func testElevatedButton_defaultInitialization() {
        let button = AMElevatedButton(text: "Elevated")

        XCTAssertNotNil(button, "ElevatedButton should initialize")
        XCTAssertEqual(button.elevation, 2, "Default elevation should be 2")
    }

    func testElevatedButton_customElevation() {
        let button = AMElevatedButton(text: "Elevated", elevation: 6)

        XCTAssertEqual(button.elevation, 6, "Elevation should be 6")
    }

    func testElevatedButton_onPressed() {
        var pressed = false
        let button = AMElevatedButton(
            text: "Elevated",
            onPressed: { pressed = true }
        )

        button.onPressed?()
        XCTAssertTrue(pressed, "onPressed callback should fire")
    }

    // MARK: - FilledButton Tests

    func testFilledButton_defaultInitialization() {
        let button = AMFilledButton(text: "Filled")

        XCTAssertNotNil(button, "FilledButton should initialize")
        XCTAssertTrue(button.enabled, "Default enabled should be true")
    }

    func testFilledButton_withColor() {
        let button = AMFilledButton(text: "Filled", color: .blue)

        XCTAssertNotNil(button.color, "Button should have color")
    }

    func testFilledButton_disabled() {
        let button = AMFilledButton(text: "Filled", enabled: false)

        XCTAssertFalse(button.enabled, "Button should be disabled")
    }

    // MARK: - OutlinedButton Tests

    func testOutlinedButton_defaultInitialization() {
        let button = AMOutlinedButton(text: "Outlined")

        XCTAssertNotNil(button, "OutlinedButton should initialize")
        XCTAssertEqual(button.borderWidth, 1, "Default border width should be 1")
    }

    func testOutlinedButton_withBorderColor() {
        let button = AMOutlinedButton(text: "Outlined", borderColor: .blue)

        XCTAssertNotNil(button.borderColor, "Border color should not be nil")
    }

    // MARK: - IconButton Tests

    func testIconButton_defaultInitialization() {
        let button = AMIconButton(icon: AMIcon(name: "star"))

        XCTAssertNotNil(button, "IconButton should initialize")
        XCTAssertNotNil(button.icon, "Icon should not be nil")
        XCTAssertTrue(button.enabled, "Default enabled should be true")
    }

    func testIconButton_disabled() {
        let button = AMIconButton(
            icon: AMIcon(name: "star"),
            enabled: false
        )

        XCTAssertFalse(button.enabled, "IconButton should be disabled")
    }

    func testIconButton_onPressed() {
        var pressed = false
        let button = AMIconButton(
            icon: AMIcon(name: "star"),
            onPressed: { pressed = true }
        )

        button.onPressed?()
        XCTAssertTrue(pressed, "onPressed callback should fire")
    }

    func testIconButton_withSize() {
        let button = AMIconButton(
            icon: AMIcon(name: "star"),
            size: 32
        )

        XCTAssertEqual(button.size, 32, "Icon size should be 32")
    }

    // MARK: - FloatingActionButton Tests

    func testFAB_defaultInitialization() {
        let fab = AMFloatingActionButton(icon: AMIcon(name: "plus"))

        XCTAssertNotNil(fab, "FAB should initialize")
        XCTAssertNotNil(fab.icon, "FAB should have icon")
        XCTAssertTrue(fab.enabled, "Default enabled should be true")
    }

    func testFAB_withLabel() {
        let fab = AMFloatingActionButton(
            icon: AMIcon(name: "plus"),
            label: "Add"
        )

        XCTAssertEqual(fab.label, "Add", "FAB should have label")
    }

    func testFAB_extended() {
        let fab = AMFloatingActionButton(
            icon: AMIcon(name: "plus"),
            label: "Add Item",
            isExtended: true
        )

        XCTAssertTrue(fab.isExtended, "FAB should be extended")
        XCTAssertEqual(fab.label, "Add Item", "Extended FAB should have label")
    }

    func testFAB_onPressed() {
        var pressed = false
        let fab = AMFloatingActionButton(
            icon: AMIcon(name: "plus"),
            onPressed: { pressed = true }
        )

        fab.onPressed?()
        XCTAssertTrue(pressed, "FAB onPressed callback should fire")
    }

    func testFAB_disabled() {
        let fab = AMFloatingActionButton(
            icon: AMIcon(name: "plus"),
            enabled: false
        )

        XCTAssertFalse(fab.enabled, "FAB should be disabled")
    }

    // MARK: - Switch Tests

    func testSwitch_defaultInitialization() {
        let switchControl = AMSwitch(value: false)

        XCTAssertNotNil(switchControl, "Switch should initialize")
        XCTAssertFalse(switchControl.value, "Default value should be false")
        XCTAssertTrue(switchControl.enabled, "Default enabled should be true")
    }

    func testSwitch_onValue() {
        let switchControl = AMSwitch(value: true)

        XCTAssertTrue(switchControl.value, "Switch value should be true")
    }

    func testSwitch_disabled() {
        let switchControl = AMSwitch(value: false, enabled: false)

        XCTAssertFalse(switchControl.enabled, "Switch should be disabled")
    }

    func testSwitch_onChanged() {
        var newValue = false
        let switchControl = AMSwitch(
            value: false,
            onChanged: { value in newValue = value }
        )

        switchControl.onChanged?(true)
        XCTAssertTrue(newValue, "onChanged should update value")
    }

    // MARK: - Checkbox Tests

    func testCheckbox_defaultInitialization() {
        let checkbox = AMCheckbox(value: false)

        XCTAssertNotNil(checkbox, "Checkbox should initialize")
        XCTAssertFalse(checkbox.value, "Default value should be false")
        XCTAssertTrue(checkbox.enabled, "Default enabled should be true")
    }

    func testCheckbox_checked() {
        let checkbox = AMCheckbox(value: true)

        XCTAssertTrue(checkbox.value, "Checkbox should be checked")
    }

    func testCheckbox_tristate() {
        let checkbox = AMCheckbox(tristate: true, value: nil)

        XCTAssertTrue(checkbox.tristate, "Checkbox should support tristate")
        XCTAssertNil(checkbox.value, "Tristate checkbox can have nil value")
    }

    func testCheckbox_onChanged() {
        var newValue: Bool? = false
        let checkbox = AMCheckbox(
            value: false,
            onChanged: { value in newValue = value }
        )

        checkbox.onChanged?(true)
        XCTAssertTrue(newValue == true, "onChanged should update value")
    }

    // MARK: - Radio Tests

    func testRadio_defaultInitialization() {
        let radio = AMRadio(value: 1, groupValue: 1)

        XCTAssertNotNil(radio, "Radio should initialize")
        XCTAssertEqual(radio.value, 1, "Radio value should be 1")
        XCTAssertEqual(radio.groupValue, 1, "Group value should be 1")
    }

    func testRadio_selected() {
        let radio = AMRadio(value: 1, groupValue: 1)

        XCTAssertTrue(radio.isSelected, "Radio should be selected when value == groupValue")
    }

    func testRadio_notSelected() {
        let radio = AMRadio(value: 1, groupValue: 2)

        XCTAssertFalse(radio.isSelected, "Radio should not be selected when value != groupValue")
    }

    func testRadio_onChanged() {
        var selectedValue = 0
        let radio = AMRadio(
            value: 1,
            groupValue: 0,
            onChanged: { value in selectedValue = value }
        )

        radio.onChanged?(1)
        XCTAssertEqual(selectedValue, 1, "onChanged should update group value")
    }

    // MARK: - Edge Cases

    func testButton_emptyText() {
        let button = AMTextButton(text: "")

        XCTAssertEqual(button.text, "", "Button can have empty text")
    }

    func testButton_longText() {
        let longText = String(repeating: "A", count: 100)
        let button = AMTextButton(text: longText)

        XCTAssertEqual(button.text.count, 100, "Button should handle long text")
    }

    func testButton_multipleCallbacks() {
        var count = 0
        let button = AMTextButton(
            text: "Click",
            onPressed: { count += 1 }
        )

        button.onPressed?()
        button.onPressed?()
        button.onPressed?()

        XCTAssertEqual(count, 3, "Callback should fire multiple times")
    }

    func testCard_nestedCards() {
        let innerCard = AMCard(child: AMText(text: "Inner"))
        let outerCard = AMCard(child: innerCard)

        XCTAssertNotNil(outerCard.child, "Outer card should have child")
    }

    func testSwitch_rapidToggle() {
        var finalValue = false
        let switchControl = AMSwitch(
            value: false,
            onChanged: { value in finalValue = value }
        )

        switchControl.onChanged?(true)
        switchControl.onChanged?(false)
        switchControl.onChanged?(true)

        XCTAssertTrue(finalValue, "Final value should be true after rapid toggles")
    }

    func testCheckbox_tristateTransitions() {
        var values: [Bool?] = []
        let checkbox = AMCheckbox(
            tristate: true,
            value: false,
            onChanged: { value in values.append(value) }
        )

        checkbox.onChanged?(true)
        checkbox.onChanged?(nil)
        checkbox.onChanged?(false)

        XCTAssertEqual(values.count, 3, "Should have 3 state transitions")
        XCTAssertEqual(values[0], true, "First transition to true")
        XCTAssertNil(values[1], "Second transition to nil")
        XCTAssertEqual(values[2], false, "Third transition to false")
    }

    func testRadio_groupTransition() {
        var groupValue = 0

        let radio1 = AMRadio(value: 1, groupValue: groupValue, onChanged: { groupValue = $0 })
        let radio2 = AMRadio(value: 2, groupValue: groupValue, onChanged: { groupValue = $0 })
        let radio3 = AMRadio(value: 3, groupValue: groupValue, onChanged: { groupValue = $0 })

        radio1.onChanged?(1)
        XCTAssertEqual(groupValue, 1, "Group value should be 1")

        radio2.onChanged?(2)
        XCTAssertEqual(groupValue, 2, "Group value should be 2")

        radio3.onChanged?(3)
        XCTAssertEqual(groupValue, 3, "Group value should be 3")
    }
}
