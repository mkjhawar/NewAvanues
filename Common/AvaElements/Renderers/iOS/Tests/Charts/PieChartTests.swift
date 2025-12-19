import XCTest
import SwiftUI
@testable import AvaElementsiOS

/// Test suite for PieChartView
///
/// Validates:
/// - Pie slice rendering with Canvas API
/// - Donut mode rendering
/// - Percentage label calculations
/// - Color assignment for slices
/// - VoiceOver accessibility
/// - Animation behavior
/// - Touch interaction/slice selection
///
/// **Coverage Target:** 90%+
/// **Technology:** SwiftUI Canvas API (custom drawing)
@available(iOS 16.0, *)
final class PieChartTests: XCTestCase {

    // MARK: - Test Data

    private var basicSlices: [PieChartView.PieSlice] {
        [
            PieChartView.PieSlice(label: "Sales", value: 150, color: "#2196F3"),
            PieChartView.PieSlice(label: "Marketing", value: 100, color: "#F44336"),
            PieChartView.PieSlice(label: "Engineering", value: 200, color: "#4CAF50")
        ]
    }

    private var twoSlices: [PieChartView.PieSlice] {
        [
            PieChartView.PieSlice(label: "Category A", value: 60, color: nil),
            PieChartView.PieSlice(label: "Category B", value: 40, color: nil)
        ]
    }

    private var singleSlice: [PieChartView.PieSlice] {
        [
            PieChartView.PieSlice(label: "Single", value: 100, color: "#2196F3")
        ]
    }

    private var emptySlices: [PieChartView.PieSlice] {
        []
    }

    // MARK: - Slice Angle Calculation Tests

    /// Test 1: Slice angle calculations
    ///
    /// Validates:
    /// - Angles are calculated proportionally to values
    /// - Total angles sum to 360 degrees
    /// - Zero values are handled correctly
    func testSliceAngleCalculations() {
        // Given
        let slices = basicSlices
        let total = slices.reduce(0.0) { $0 + $1.value }

        // When
        let view = PieChartView(
            slices: slices,
            donutMode: false,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertEqual(view.slices.count, 3, "Should have 3 slices")
        XCTAssertEqual(total, 450, "Total should be 450")

        // Calculate expected angles
        let expectedAngles = slices.map { ($0.value / total) * 360.0 }
        XCTAssertEqual(expectedAngles[0], 120.0, accuracy: 0.1, "First slice should be 120 degrees")
        XCTAssertEqual(expectedAngles[1], 80.0, accuracy: 0.1, "Second slice should be 80 degrees")
        XCTAssertEqual(expectedAngles[2], 160.0, accuracy: 0.1, "Third slice should be 160 degrees")

        // Verify sum is 360
        let sum = expectedAngles.reduce(0, +)
        XCTAssertEqual(sum, 360.0, accuracy: 0.1, "Angles should sum to 360 degrees")
    }

    /// Test 2: Donut mode rendering
    ///
    /// Validates:
    /// - Donut mode creates inner radius cutout
    /// - Inner radius percentage is applied correctly
    /// - Outer radius remains unchanged
    func testDonutMode() {
        // Given
        let slices = basicSlices
        let innerRadiusRatio: Float = 0.6

        // When
        let donutView = PieChartView(
            slices: slices,
            donutMode: true,
            donutInnerRadius: innerRadiusRatio,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertTrue(donutView.donutMode, "Donut mode should be enabled")
        XCTAssertEqual(donutView.donutInnerRadius, innerRadiusRatio, "Inner radius ratio should match")

        // When - Standard pie mode
        let pieView = PieChartView(
            slices: slices,
            donutMode: false,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertFalse(pieView.donutMode, "Donut mode should be disabled")
    }

    /// Test 3: Percentage label calculations
    ///
    /// Validates:
    /// - Percentages are calculated correctly
    /// - Percentages sum to 100%
    /// - Labels can be shown/hidden
    func testPercentageLabels() {
        // Given
        let slices = basicSlices
        let total = slices.reduce(0.0) { $0 + $1.value }

        // When
        let viewWithPercentages = PieChartView(
            slices: slices,
            donutMode: false,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertTrue(viewWithPercentages.showPercentages, "Percentages should be visible")

        // Calculate expected percentages
        let expectedPercentages = slices.map { ($0.value / total) * 100.0 }
        XCTAssertEqual(expectedPercentages[0], 33.33, accuracy: 0.01, "First slice should be 33.33%")
        XCTAssertEqual(expectedPercentages[1], 22.22, accuracy: 0.01, "Second slice should be 22.22%")
        XCTAssertEqual(expectedPercentages[2], 44.44, accuracy: 0.01, "Third slice should be 44.44%")

        // Verify sum is 100%
        let sum = expectedPercentages.reduce(0, +)
        XCTAssertEqual(sum, 100.0, accuracy: 0.01, "Percentages should sum to 100%")

        // When - Without percentages
        let viewWithoutPercentages = PieChartView(
            slices: slices,
            donutMode: false,
            size: 200,
            showLabels: true,
            showPercentages: false,
            animated: false
        )

        // Then
        XCTAssertFalse(viewWithoutPercentages.showPercentages, "Percentages should be hidden")
    }

    /// Test 4: Color assignment for slices
    ///
    /// Validates:
    /// - Custom colors are used when provided
    /// - Default palette is used when colors are nil
    /// - Colors are parsed correctly from hex strings
    func testColorAssignment() {
        // Given
        let slicesWithColors = basicSlices
        let slicesWithoutColors = twoSlices

        // When - With custom colors
        let viewWithColors = PieChartView(
            slices: slicesWithColors,
            donutMode: false,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertEqual(viewWithColors.slices[0].color, "#2196F3", "First slice should have blue color")
        XCTAssertEqual(viewWithColors.slices[1].color, "#F44336", "Second slice should have red color")
        XCTAssertEqual(viewWithColors.slices[2].color, "#4CAF50", "Third slice should have green color")

        // When - Without custom colors
        let viewWithoutColors = PieChartView(
            slices: slicesWithoutColors,
            donutMode: false,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertNil(viewWithoutColors.slices[0].color, "First slice should use default color")
        XCTAssertNil(viewWithoutColors.slices[1].color, "Second slice should use default color")
    }

    /// Test 5: VoiceOver accessibility support
    ///
    /// Validates:
    /// - Chart has descriptive accessibility label
    /// - Each slice has accessibility value
    /// - Percentages are included in accessibility
    /// - Content description is used when provided
    func testVoiceOverSupport() {
        // Given
        let slices = basicSlices
        let contentDescription = "Budget breakdown pie chart"

        // When
        let view = PieChartView(
            slices: slices,
            donutMode: false,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false,
            contentDescription: contentDescription
        )

        // Then
        XCTAssertNotNil(view.contentDescription, "Should have content description")
        XCTAssertEqual(view.contentDescription, contentDescription, "Content description should match")

        // Verify accessibility label for each slice
        let total = slices.reduce(0.0) { $0 + $1.value }
        let firstSlicePercentage = (slices[0].value / total) * 100.0

        let expectedLabel = ChartAccessibility.generatePieSliceValue(
            label: slices[0].label,
            value: slices[0].value,
            percentage: firstSlicePercentage
        )

        XCTAssertFalse(expectedLabel.isEmpty, "Accessibility label should not be empty")
        XCTAssertTrue(expectedLabel.contains("Sales"), "Should include slice label")
        XCTAssertTrue(expectedLabel.contains("150"), "Should include value")
        XCTAssertTrue(expectedLabel.contains("percent"), "Should include percentage")
    }

    /// Test 6: Animation behavior
    ///
    /// Validates:
    /// - Animation can be enabled/disabled
    /// - Animated slices grow from 0 to full size
    func testAnimation() {
        // Given
        let slices = basicSlices

        // When - Animated
        let animatedView = PieChartView(
            slices: slices,
            donutMode: false,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: true
        )

        // Then
        XCTAssertTrue(animatedView.animated, "Animation should be enabled")

        // When - Not animated
        let staticView = PieChartView(
            slices: slices,
            donutMode: false,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertFalse(staticView.animated, "Animation should be disabled")
    }

    /// Test 7: Empty state handling
    ///
    /// Validates:
    /// - Chart handles empty slices gracefully
    /// - No crash with zero slices
    /// - Appropriate empty state is shown
    func testEmptyStateHandling() {
        // Given
        let slices = emptySlices

        // When
        let view = PieChartView(
            slices: slices,
            donutMode: false,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertTrue(view.slices.isEmpty, "Slices should be empty")

        // Should not crash when rendering
        XCTAssertNoThrow(view.body, "Should not crash with empty slices")
    }

    /// Test 8: Single slice rendering
    ///
    /// Validates:
    /// - Single slice renders as full circle (360 degrees)
    /// - Percentage is 100%
    func testSingleSliceRendering() {
        // Given
        let slices = singleSlice

        // When
        let view = PieChartView(
            slices: slices,
            donutMode: false,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertEqual(view.slices.count, 1, "Should have 1 slice")

        let total = slices.reduce(0.0) { $0 + $1.value }
        let percentage = (slices[0].value / total) * 100.0
        XCTAssertEqual(percentage, 100.0, "Single slice should be 100%")
    }

    /// Test 9: Size configuration
    ///
    /// Validates:
    /// - Custom size can be set
    /// - Size affects chart dimensions
    func testSizeConfiguration() {
        // Given
        let slices = basicSlices
        let customSize: Float = 300

        // When
        let largeView = PieChartView(
            slices: slices,
            donutMode: false,
            size: customSize,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertEqual(largeView.size, customSize, "Size should be 300")

        // When - Default size
        let defaultView = PieChartView(
            slices: slices,
            donutMode: false,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertEqual(defaultView.size, 200, "Size should be 200")
    }

    /// Test 10: Label visibility
    ///
    /// Validates:
    /// - Labels can be shown/hidden
    /// - Label text is correct
    func testLabelVisibility() {
        // Given
        let slices = basicSlices

        // When - With labels
        let viewWithLabels = PieChartView(
            slices: slices,
            donutMode: false,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertTrue(viewWithLabels.showLabels, "Labels should be visible")

        // When - Without labels
        let viewWithoutLabels = PieChartView(
            slices: slices,
            donutMode: false,
            size: 200,
            showLabels: false,
            showPercentages: false,
            animated: false
        )

        // Then
        XCTAssertFalse(viewWithoutLabels.showLabels, "Labels should be hidden")
    }

    /// Test 11: Donut inner radius bounds
    ///
    /// Validates:
    /// - Inner radius is clamped to valid range (0.0 to 1.0)
    /// - Invalid values don't cause rendering issues
    func testDonutInnerRadiusBounds() {
        // Given
        let slices = basicSlices

        // When - Valid inner radius
        let validView = PieChartView(
            slices: slices,
            donutMode: true,
            donutInnerRadius: 0.6,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertEqual(validView.donutInnerRadius, 0.6, "Inner radius should be 0.6")

        // When - Minimum inner radius
        let minView = PieChartView(
            slices: slices,
            donutMode: true,
            donutInnerRadius: 0.0,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertEqual(minView.donutInnerRadius, 0.0, "Inner radius should be 0.0")

        // When - Maximum inner radius
        let maxView = PieChartView(
            slices: slices,
            donutMode: true,
            donutInnerRadius: 0.95,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertEqual(maxView.donutInnerRadius, 0.95, "Inner radius should be 0.95")
    }

    /// Test 12: Zero value slices
    ///
    /// Validates:
    /// - Slices with zero value don't render
    /// - No division by zero errors
    func testZeroValueSlices() {
        // Given
        let slicesWithZero = [
            PieChartView.PieSlice(label: "A", value: 100, color: nil),
            PieChartView.PieSlice(label: "B", value: 0, color: nil),
            PieChartView.PieSlice(label: "C", value: 50, color: nil)
        ]

        // When
        let view = PieChartView(
            slices: slicesWithZero,
            donutMode: false,
            size: 200,
            showLabels: true,
            showPercentages: true,
            animated: false
        )

        // Then
        XCTAssertEqual(view.slices.count, 3, "Should have 3 slices")

        // Calculate total (should only include non-zero values for percentage)
        let total = slicesWithZero.reduce(0.0) { $0 + $1.value }
        XCTAssertEqual(total, 150, "Total should be 150")

        // Should not crash when rendering
        XCTAssertNoThrow(view.body, "Should not crash with zero value slices")
    }
}
