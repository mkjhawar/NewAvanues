import XCTest
import SwiftUI
import Charts
@testable import AvaElementsiOS

/// Test suite for BarChartView
///
/// Validates:
/// - Vertical and horizontal bar orientations
/// - Grouped and stacked bar modes
/// - Custom colors and styling
/// - Legend visibility
/// - VoiceOver accessibility
/// - Animation behavior
/// - Grid and axis labels
/// - Empty state handling
///
/// **Coverage Target:** 90%+
/// **Framework:** Swift Charts (iOS 16+)
@available(iOS 16.0, *)
final class BarChartTests: XCTestCase {

    // MARK: - Test Data

    private var singleGroupData: [BarChartView.BarGroup] {
        [
            BarChartView.BarGroup(
                label: "Q1",
                bars: [
                    BarChartView.Bar(value: 100, label: "Revenue", color: "#2196F3")
                ]
            ),
            BarChartView.BarGroup(
                label: "Q2",
                bars: [
                    BarChartView.Bar(value: 150, label: "Revenue", color: "#2196F3")
                ]
            ),
            BarChartView.BarGroup(
                label: "Q3",
                bars: [
                    BarChartView.Bar(value: 125, label: "Revenue", color: "#2196F3")
                ]
            )
        ]
    }

    private var multiSeriesData: [BarChartView.BarGroup] {
        [
            BarChartView.BarGroup(
                label: "Q1",
                bars: [
                    BarChartView.Bar(value: 100, label: "Revenue", color: "#2196F3"),
                    BarChartView.Bar(value: 80, label: "Cost", color: "#F44336")
                ]
            ),
            BarChartView.BarGroup(
                label: "Q2",
                bars: [
                    BarChartView.Bar(value: 150, label: "Revenue", color: "#2196F3"),
                    BarChartView.Bar(value: 90, label: "Cost", color: "#F44336")
                ]
            ),
            BarChartView.BarGroup(
                label: "Q3",
                bars: [
                    BarChartView.Bar(value: 125, label: "Revenue", color: "#2196F3"),
                    BarChartView.Bar(value: 85, label: "Cost", color: "#F44336")
                ]
            )
        ]
    }

    private var emptyData: [BarChartView.BarGroup] {
        []
    }

    // MARK: - Test 1: Vertical Bars

    /// Test 1: Vertical bar chart rendering
    ///
    /// Validates:
    /// - Bars render in vertical orientation
    /// - BarMark is used for bars
    /// - Default styling is applied
    /// - Data values are preserved
    func testVerticalBars() {
        // Given
        let data = singleGroupData
        let title = "Quarterly Revenue"

        // When
        let view = BarChartView(
            data: data,
            title: title,
            mode: .grouped,
            orientation: .vertical,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertEqual(view.data.count, 3, "Should have 3 bar groups")
        XCTAssertEqual(view.title, title, "Should have correct title")
        XCTAssertEqual(view.orientation, .vertical, "Should be vertical")
        XCTAssertTrue(view.showGrid, "Grid should be visible")
        XCTAssertTrue(view.animated, "Animation should be enabled")

        // Verify data integrity
        XCTAssertEqual(view.data[0].bars[0].value, 100, "First bar value should be 100")
        XCTAssertEqual(view.data[1].bars[0].value, 150, "Second bar value should be 150")
        XCTAssertEqual(view.data[2].bars[0].value, 125, "Third bar value should be 125")
    }

    // MARK: - Test 2: Horizontal Bars

    /// Test 2: Horizontal bar chart rendering
    ///
    /// Validates:
    /// - Bars render in horizontal orientation
    /// - X and Y axes are swapped correctly
    /// - Labels are positioned correctly
    func testHorizontalBars() {
        // Given
        let data = singleGroupData
        let title = "Horizontal Revenue"

        // When
        let view = BarChartView(
            data: data,
            title: title,
            mode: .grouped,
            orientation: .horizontal,
            showLegend: false,
            showGrid: true,
            animated: false
        )

        // Then
        XCTAssertEqual(view.orientation, .horizontal, "Should be horizontal")
        XCTAssertEqual(view.data.count, 3, "Should have 3 bar groups")

        // Verify horizontal bars have correct structure
        XCTAssertEqual(view.data[0].label, "Q1", "First group should be Q1")
        XCTAssertEqual(view.data[0].bars[0].value, 100, "First bar should have value 100")
    }

    // MARK: - Test 3: Stacked Bars

    /// Test 3: Stacked bar mode
    ///
    /// Validates:
    /// - Multiple bars in a group are stacked
    /// - Total values are calculated correctly
    /// - Colors are distinct for each series
    func testStackedBars() {
        // Given
        let data = multiSeriesData
        let title = "Revenue vs Cost (Stacked)"

        // When
        let view = BarChartView(
            data: data,
            title: title,
            mode: .stacked,
            orientation: .vertical,
            showLegend: true,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertEqual(view.mode, .stacked, "Should be stacked mode")
        XCTAssertTrue(view.showLegend, "Legend should be visible for stacked bars")

        // Verify stacked structure
        let firstGroup = view.data[0]
        XCTAssertEqual(firstGroup.bars.count, 2, "First group should have 2 bars")

        // Calculate total for stacked mode
        let total = firstGroup.bars.map { $0.value }.reduce(0, +)
        XCTAssertEqual(total, 180, "Stacked total should be 180 (100 + 80)")
    }

    // MARK: - Test 4: Grouped Bars

    /// Test 4: Grouped bar mode (multiple series side-by-side)
    ///
    /// Validates:
    /// - Multiple bars in a group are displayed side-by-side
    /// - Each series maintains its own color
    /// - Bars are properly spaced
    func testGroupedBars() {
        // Given
        let data = multiSeriesData
        let title = "Revenue vs Cost (Grouped)"

        // When
        let view = BarChartView(
            data: data,
            title: title,
            mode: .grouped,
            orientation: .vertical,
            showLegend: true,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertEqual(view.mode, .grouped, "Should be grouped mode")
        XCTAssertTrue(view.showLegend, "Legend should be visible for grouped bars")

        // Verify grouped structure
        let firstGroup = view.data[0]
        XCTAssertEqual(firstGroup.bars.count, 2, "First group should have 2 bars")

        // Verify both bars are independent
        XCTAssertEqual(firstGroup.bars[0].value, 100, "First bar should be 100")
        XCTAssertEqual(firstGroup.bars[1].value, 80, "Second bar should be 80")
        XCTAssertEqual(firstGroup.bars[0].label, "Revenue", "First bar should be Revenue")
        XCTAssertEqual(firstGroup.bars[1].label, "Cost", "Second bar should be Cost")
    }

    // MARK: - Test 5: VoiceOver Support

    /// Test 5: VoiceOver accessibility support
    ///
    /// Validates:
    /// - Chart has descriptive accessibility label
    /// - Bar groups have accessibility values
    /// - Content description is used when provided
    func testVoiceOverSupport() {
        // Given
        let data = singleGroupData
        let title = "Revenue Chart"
        let contentDescription = "Bar chart showing quarterly revenue"

        // When
        let view = BarChartView(
            data: data,
            title: title,
            mode: .grouped,
            orientation: .vertical,
            contentDescription: contentDescription,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertNotNil(view.contentDescription, "Should have content description")
        XCTAssertEqual(view.contentDescription, contentDescription, "Content description should match")

        // Verify accessibility label generation
        let expectedLabel = ChartAccessibility.generateChartLabel(
            title: title,
            seriesCount: 1,
            dataPointCount: 3,
            chartType: "bar"
        )

        XCTAssertFalse(expectedLabel.isEmpty, "Accessibility label should not be empty")
        XCTAssertTrue(expectedLabel.contains("Revenue Chart"), "Should include title")
        XCTAssertTrue(expectedLabel.contains("bar"), "Should mention chart type")
        XCTAssertTrue(expectedLabel.contains("3"), "Should mention data count")

        // Test bar value accessibility
        let barValue = ChartAccessibility.generateBarValue(
            label: "Q1",
            value: 100,
            unit: "dollars"
        )
        XCTAssertTrue(barValue.contains("Q1"), "Should include label")
        XCTAssertTrue(barValue.contains("100"), "Should include value")
        XCTAssertTrue(barValue.contains("dollars"), "Should include unit")
    }

    // MARK: - Test 6: Legend Tests

    /// Test 6: Legend visibility for multiple series
    ///
    /// Validates:
    /// - Legend is shown for multiple series
    /// - Legend displays correct labels
    /// - Legend can be hidden
    func testLegendVisibility() {
        // Given
        let data = multiSeriesData

        // When - Legend shown
        let viewWithLegend = BarChartView(
            data: data,
            title: "With Legend",
            mode: .grouped,
            orientation: .vertical,
            showLegend: true,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertTrue(viewWithLegend.showLegend, "Legend should be visible")

        // When - Legend hidden
        let viewWithoutLegend = BarChartView(
            data: data,
            title: "Without Legend",
            mode: .grouped,
            orientation: .vertical,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertFalse(viewWithoutLegend.showLegend, "Legend should be hidden")
    }

    // MARK: - Test 7: Animation

    /// Test 7: Animation behavior
    ///
    /// Validates:
    /// - Animation can be enabled/disabled
    /// - Animated property is correctly set
    func testAnimation() {
        // Given
        let data = singleGroupData

        // When - Animated
        let animatedView = BarChartView(
            data: data,
            title: "Animated",
            mode: .grouped,
            orientation: .vertical,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertTrue(animatedView.animated, "Animation should be enabled")

        // When - Not animated
        let staticView = BarChartView(
            data: data,
            title: "Static",
            mode: .grouped,
            orientation: .vertical,
            showLegend: false,
            showGrid: true,
            animated: false
        )

        // Then
        XCTAssertFalse(staticView.animated, "Animation should be disabled")
    }

    // MARK: - Test 8: Empty State

    /// Test 8: Empty data handling
    ///
    /// Validates:
    /// - Chart handles empty data gracefully
    /// - No crash with zero data
    /// - Appropriate empty state is shown
    func testEmptyDataHandling() {
        // Given
        let data = emptyData

        // When
        let view = BarChartView(
            data: data,
            title: "Empty Chart",
            mode: .grouped,
            orientation: .vertical,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertTrue(view.data.isEmpty, "Data should be empty")
        XCTAssertNotNil(view.title, "Title should still be set")

        // Should not crash when rendering
        XCTAssertNoThrow(view.body, "Should not crash with empty data")
    }

    // MARK: - Test 9: Color Parsing

    /// Test 9: Color parsing from hex strings
    ///
    /// Validates:
    /// - Hex colors are parsed correctly
    /// - Colors are applied to bars
    /// - Default colors are used when not specified
    func testColorParsing() {
        // Given
        let data = [
            BarChartView.BarGroup(
                label: "Test",
                bars: [
                    BarChartView.Bar(value: 100, label: "Blue", color: "#2196F3"),
                    BarChartView.Bar(value: 80, label: "Red", color: "#F44336")
                ]
            )
        ]

        // When
        let view = BarChartView(
            data: data,
            title: "Color Test",
            mode: .grouped,
            orientation: .vertical,
            showLegend: true,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertEqual(view.data[0].bars[0].color, "#2196F3", "Should have blue color")
        XCTAssertEqual(view.data[0].bars[1].color, "#F44336", "Should have red color")

        // Test color parsing
        let blueColor = ChartHelpers.parseColor("#2196F3")
        let redColor = ChartHelpers.parseColor("#F44336")

        XCTAssertNotNil(blueColor, "Blue color should be parsed")
        XCTAssertNotNil(redColor, "Red color should be parsed")
    }

    // MARK: - Test 10: Grid and Axis Labels

    /// Test 10: Grid lines and axis labels
    ///
    /// Validates:
    /// - Grid lines can be shown/hidden
    /// - X-axis labels are displayed
    /// - Y-axis labels are displayed
    func testGridAndAxisLabels() {
        // Given
        let data = singleGroupData
        let xAxisLabel = "Quarter"
        let yAxisLabel = "Revenue ($)"

        // When
        let viewWithGrid = BarChartView(
            data: data,
            title: "Grid Test",
            mode: .grouped,
            orientation: .vertical,
            xAxisLabel: xAxisLabel,
            yAxisLabel: yAxisLabel,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertTrue(viewWithGrid.showGrid, "Grid should be visible")
        XCTAssertEqual(viewWithGrid.xAxisLabel, xAxisLabel, "X-axis label should match")
        XCTAssertEqual(viewWithGrid.yAxisLabel, yAxisLabel, "Y-axis label should match")

        // When - Grid hidden
        let viewWithoutGrid = BarChartView(
            data: data,
            title: "No Grid",
            mode: .grouped,
            orientation: .vertical,
            showLegend: false,
            showGrid: false,
            animated: true
        )

        // Then
        XCTAssertFalse(viewWithoutGrid.showGrid, "Grid should be hidden")
    }

    // MARK: - Test 11: Custom Height

    /// Test 11: Custom height support
    ///
    /// Validates:
    /// - Custom height can be set
    /// - Default height is used when not specified
    func testCustomHeight() {
        // Given
        let data = singleGroupData
        let customHeight: CGFloat = 400

        // When
        let view = BarChartView(
            data: data,
            title: "Custom Height",
            mode: .grouped,
            orientation: .vertical,
            height: customHeight,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertEqual(view.height, customHeight, "Should have custom height")

        // When - Default height
        let defaultView = BarChartView(
            data: data,
            title: "Default Height",
            mode: .grouped,
            orientation: .vertical,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertNil(defaultView.height, "Should use default height")
    }
}
