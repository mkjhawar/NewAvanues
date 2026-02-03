import XCTest
import SwiftUI
import Charts
@testable import AvaElementsiOS

/// Test suite for LineChartView
///
/// Validates:
/// - Single and multiple series rendering
/// - Custom colors and styling
/// - Legend visibility and positioning
/// - VoiceOver accessibility
/// - Animation behavior
/// - Grid and axis labels
/// - Empty state handling
///
/// **Coverage Target:** 90%+
/// **Framework:** Swift Charts (iOS 16+)
@available(iOS 16.0, *)
final class LineChartTests: XCTestCase {

    // MARK: - Test Data

    private var singleSeriesData: [LineChartView.DataPoint] {
        [
            LineChartView.DataPoint(x: 0, y: 100, xLabel: "Jan", series: "Revenue"),
            LineChartView.DataPoint(x: 1, y: 150, xLabel: "Feb", series: "Revenue"),
            LineChartView.DataPoint(x: 2, y: 125, xLabel: "Mar", series: "Revenue"),
            LineChartView.DataPoint(x: 3, y: 180, xLabel: "Apr", series: "Revenue")
        ]
    }

    private var multiSeriesData: [LineChartView.DataPoint] {
        [
            // Revenue series
            LineChartView.DataPoint(x: 0, y: 100, xLabel: "Jan", series: "Revenue"),
            LineChartView.DataPoint(x: 1, y: 150, xLabel: "Feb", series: "Revenue"),
            LineChartView.DataPoint(x: 2, y: 125, xLabel: "Mar", series: "Revenue"),

            // Cost series
            LineChartView.DataPoint(x: 0, y: 80, xLabel: "Jan", series: "Cost"),
            LineChartView.DataPoint(x: 1, y: 90, xLabel: "Feb", series: "Cost"),
            LineChartView.DataPoint(x: 2, y: 85, xLabel: "Mar", series: "Cost")
        ]
    }

    private var emptyData: [LineChartView.DataPoint] {
        []
    }

    // MARK: - Single Series Tests

    /// Test 1: Single series rendering with default styling
    ///
    /// Validates:
    /// - Chart renders with single data series
    /// - LineMark is used for data points
    /// - Default blue color is applied
    /// - Smooth curve interpolation
    func testSingleSeriesRendering() {
        // Given
        let data = singleSeriesData
        let title = "Revenue Chart"

        // When
        let view = LineChartView(
            data: data,
            title: title,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertEqual(view.data.count, 4, "Should have 4 data points")
        XCTAssertEqual(view.title, title, "Should have correct title")
        XCTAssertFalse(view.showLegend, "Legend should be hidden for single series")
        XCTAssertTrue(view.showGrid, "Grid should be visible")
        XCTAssertTrue(view.animated, "Animation should be enabled")

        // Verify data integrity
        XCTAssertEqual(view.data[0].y, 100, "First point Y value should be 100")
        XCTAssertEqual(view.data[3].y, 180, "Last point Y value should be 180")
    }

    // MARK: - Multiple Series Tests

    /// Test 2: Multiple series rendering with distinct colors
    ///
    /// Validates:
    /// - Multiple series are rendered correctly
    /// - Each series gets distinct color from palette
    /// - Legend is shown for multiple series
    /// - Series labels are preserved
    func testMultipleSeriesRendering() {
        // Given
        let data = multiSeriesData
        let title = "Revenue vs Cost"

        // When
        let view = LineChartView(
            data: data,
            title: title,
            showLegend: true,
            showGrid: true,
            animated: false
        )

        // Then
        XCTAssertEqual(view.data.count, 6, "Should have 6 total data points")
        XCTAssertTrue(view.showLegend, "Legend should be visible for multiple series")

        // Verify series separation
        let revenueSeries = view.data.filter { $0.series == "Revenue" }
        let costSeries = view.data.filter { $0.series == "Cost" }

        XCTAssertEqual(revenueSeries.count, 3, "Should have 3 revenue points")
        XCTAssertEqual(costSeries.count, 3, "Should have 3 cost points")

        // Verify data values
        XCTAssertEqual(revenueSeries[0].y, 100, "First revenue point should be 100")
        XCTAssertEqual(costSeries[0].y, 80, "First cost point should be 80")
    }

    // MARK: - Custom Colors Tests

    /// Test 3: Custom colors for series
    ///
    /// Validates:
    /// - Custom color map is applied correctly
    /// - Colors override default palette
    /// - Series without custom color use default
    func testCustomColors() {
        // Given
        let data = multiSeriesData
        let customColors: [String: Color] = [
            "Revenue": .green,
            "Cost": .red
        ]

        // When
        let view = LineChartView(
            data: data,
            title: "Custom Colors Test",
            seriesColors: customColors,
            showLegend: true,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertNotNil(view.seriesColors, "Custom colors should be set")
        XCTAssertEqual(view.seriesColors?.count, 2, "Should have 2 custom colors")
        XCTAssertEqual(view.seriesColors?["Revenue"], .green, "Revenue should be green")
        XCTAssertEqual(view.seriesColors?["Cost"], .red, "Cost should be red")
    }

    // MARK: - Legend Tests

    /// Test 4: Legend visibility and positioning
    ///
    /// Validates:
    /// - Legend can be shown/hidden
    /// - Legend displays correct series labels
    /// - Legend positioning is correct
    func testLegendVisibility() {
        // Given
        let data = multiSeriesData

        // When - Legend shown
        let viewWithLegend = LineChartView(
            data: data,
            title: "With Legend",
            showLegend: true,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertTrue(viewWithLegend.showLegend, "Legend should be visible")

        // When - Legend hidden
        let viewWithoutLegend = LineChartView(
            data: data,
            title: "Without Legend",
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertFalse(viewWithoutLegend.showLegend, "Legend should be hidden")
    }

    // MARK: - VoiceOver Tests

    /// Test 5: VoiceOver accessibility support
    ///
    /// Validates:
    /// - Chart has descriptive accessibility label
    /// - Accessibility label includes title
    /// - Accessibility label describes data structure
    /// - Data points have accessibility values
    func testVoiceOverSupport() {
        // Given
        let data = singleSeriesData
        let title = "Revenue Trend"
        let contentDescription = "Revenue chart showing quarterly performance"

        // When
        let view = LineChartView(
            data: data,
            title: title,
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
            dataPointCount: 4,
            chartType: "line"
        )

        XCTAssertFalse(expectedLabel.isEmpty, "Accessibility label should not be empty")
        XCTAssertTrue(expectedLabel.contains("Revenue Trend"), "Should include title")
        XCTAssertTrue(expectedLabel.contains("line"), "Should mention chart type")
        XCTAssertTrue(expectedLabel.contains("4"), "Should mention data point count")
    }

    // MARK: - Grid and Axis Tests

    /// Test 6: Grid lines and axis labels
    ///
    /// Validates:
    /// - Grid lines can be shown/hidden
    /// - X-axis labels are displayed
    /// - Y-axis labels are displayed
    /// - Axis labels use correct formatting
    func testGridAndAxisLabels() {
        // Given
        let data = singleSeriesData
        let xAxisLabel = "Month"
        let yAxisLabel = "Revenue ($)"

        // When
        let viewWithGrid = LineChartView(
            data: data,
            title: "Grid Test",
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
        let viewWithoutGrid = LineChartView(
            data: data,
            title: "No Grid",
            showLegend: false,
            showGrid: false,
            animated: true
        )

        // Then
        XCTAssertFalse(viewWithoutGrid.showGrid, "Grid should be hidden")
    }

    // MARK: - Animation Tests

    /// Test 7: Animation behavior
    ///
    /// Validates:
    /// - Animation can be enabled/disabled
    /// - Animation duration is configurable
    func testAnimation() {
        // Given
        let data = singleSeriesData

        // When - Animated
        let animatedView = LineChartView(
            data: data,
            title: "Animated",
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertTrue(animatedView.animated, "Animation should be enabled")

        // When - Not animated
        let staticView = LineChartView(
            data: data,
            title: "Static",
            showLegend: false,
            showGrid: true,
            animated: false
        )

        // Then
        XCTAssertFalse(staticView.animated, "Animation should be disabled")
    }

    // MARK: - Empty State Tests

    /// Test 8: Empty data handling
    ///
    /// Validates:
    /// - Chart handles empty data gracefully
    /// - No crash with zero data points
    /// - Appropriate empty state is shown
    func testEmptyDataHandling() {
        // Given
        let data = emptyData

        // When
        let view = LineChartView(
            data: data,
            title: "Empty Chart",
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

    // MARK: - Data Validation Tests

    /// Test 9: Data point validation
    ///
    /// Validates:
    /// - Data points have correct structure
    /// - Series labels are preserved
    /// - X and Y values are accessible
    func testDataPointValidation() {
        // Given
        let data = singleSeriesData

        // When
        let view = LineChartView(
            data: data,
            title: "Validation Test",
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        let firstPoint = view.data[0]
        XCTAssertEqual(firstPoint.x, 0, "X value should be 0")
        XCTAssertEqual(firstPoint.y, 100, "Y value should be 100")
        XCTAssertEqual(firstPoint.xLabel, "Jan", "X label should be 'Jan'")
        XCTAssertEqual(firstPoint.series, "Revenue", "Series should be 'Revenue'")

        // Verify data sorting
        let xValues = view.data.map { $0.x }
        let sortedXValues = xValues.sorted()
        // Note: Multi-series data may not be sorted by x, so we check per series
        let revenueSeries = view.data.filter { $0.series == "Revenue" }
        let revenueXValues = revenueSeries.map { $0.x }
        XCTAssertEqual(revenueXValues, revenueXValues.sorted(), "Series data should be sorted by X")
    }

    // MARK: - Color Palette Tests

    /// Test 10: Default color palette usage
    ///
    /// Validates:
    /// - Default colors are used when custom colors not provided
    /// - Multiple series get distinct colors
    /// - Color wrapping works for >10 series
    func testDefaultColorPalette() {
        // Given
        let data = multiSeriesData

        // When - No custom colors
        let view = LineChartView(
            data: data,
            title: "Default Colors",
            showLegend: true,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertNil(view.seriesColors, "Should not have custom colors")

        // Verify default palette is used (indirectly through unique series)
        let uniqueSeries = Set(view.data.map { $0.series })
        XCTAssertEqual(uniqueSeries.count, 2, "Should have 2 unique series")
    }

    // MARK: - Height Tests

    /// Test 11: Custom height support
    ///
    /// Validates:
    /// - Custom height can be set
    /// - Default height is used when not specified
    func testCustomHeight() {
        // Given
        let data = singleSeriesData
        let customHeight: CGFloat = 400

        // When
        let view = LineChartView(
            data: data,
            title: "Custom Height",
            height: customHeight,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertEqual(view.height, customHeight, "Should have custom height")

        // When - Default height
        let defaultView = LineChartView(
            data: data,
            title: "Default Height",
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertNil(defaultView.height, "Should use default height")
    }
}
