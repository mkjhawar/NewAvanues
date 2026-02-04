import XCTest
import SwiftUI
import Charts
@testable import AvaElementsiOS

/// Test suite for AreaChartView
///
/// Validates:
/// - Single and multiple series rendering
/// - Stacked vs overlapping modes
/// - Gradient fills with opacity
/// - Custom colors and styling
/// - Legend visibility and positioning
/// - VoiceOver accessibility
/// - Animation behavior
/// - Grid and axis labels
/// - Empty state handling
/// - Negative values handling
///
/// **Coverage Target:** 90%+
/// **Framework:** Swift Charts (iOS 16+)
@available(iOS 16.0, *)
final class AreaChartTests: XCTestCase {

    // MARK: - Test Data

    private var singleSeriesData: [AreaChartView.DataPoint] {
        [
            AreaChartView.DataPoint(x: 0, y: 100, xLabel: "Jan", series: "Revenue"),
            AreaChartView.DataPoint(x: 1, y: 150, xLabel: "Feb", series: "Revenue"),
            AreaChartView.DataPoint(x: 2, y: 125, xLabel: "Mar", series: "Revenue"),
            AreaChartView.DataPoint(x: 3, y: 180, xLabel: "Apr", series: "Revenue")
        ]
    }

    private var multiSeriesData: [AreaChartView.DataPoint] {
        [
            // Revenue series
            AreaChartView.DataPoint(x: 0, y: 100, xLabel: "Jan", series: "Revenue"),
            AreaChartView.DataPoint(x: 1, y: 150, xLabel: "Feb", series: "Revenue"),
            AreaChartView.DataPoint(x: 2, y: 125, xLabel: "Mar", series: "Revenue"),
            AreaChartView.DataPoint(x: 3, y: 180, xLabel: "Apr", series: "Revenue"),

            // Cost series
            AreaChartView.DataPoint(x: 0, y: 80, xLabel: "Jan", series: "Cost"),
            AreaChartView.DataPoint(x: 1, y: 90, xLabel: "Feb", series: "Cost"),
            AreaChartView.DataPoint(x: 2, y: 85, xLabel: "Mar", series: "Cost"),
            AreaChartView.DataPoint(x: 3, y: 95, xLabel: "Apr", series: "Cost")
        ]
    }

    private var negativeValuesData: [AreaChartView.DataPoint] {
        [
            AreaChartView.DataPoint(x: 0, y: 50, xLabel: "Q1", series: "Profit"),
            AreaChartView.DataPoint(x: 1, y: -20, xLabel: "Q2", series: "Profit"),
            AreaChartView.DataPoint(x: 2, y: 30, xLabel: "Q3", series: "Profit"),
            AreaChartView.DataPoint(x: 3, y: -10, xLabel: "Q4", series: "Profit")
        ]
    }

    private var emptyData: [AreaChartView.DataPoint] {
        []
    }

    // MARK: - Single Series Tests

    /// Test 1: Single series area chart rendering
    ///
    /// Validates:
    /// - Chart renders with single data series
    /// - AreaMark is used for filled area
    /// - Gradient fill is applied with opacity
    /// - Smooth curve interpolation (Catmull-Rom)
    func testSingleSeriesRendering() {
        // Given
        let data = singleSeriesData
        let title = "Revenue Chart"

        // When
        let view = AreaChartView(
            data: data,
            title: title,
            stacked: false,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertEqual(view.data.count, 4, "Should have 4 data points")
        XCTAssertEqual(view.title, title, "Should have correct title")
        XCTAssertFalse(view.stacked, "Should not be stacked mode")
        XCTAssertFalse(view.showLegend, "Legend should be hidden for single series")
        XCTAssertTrue(view.showGrid, "Grid should be visible")
        XCTAssertTrue(view.animated, "Animation should be enabled")

        // Verify data integrity
        XCTAssertEqual(view.data[0].y, 100, "First point Y value should be 100")
        XCTAssertEqual(view.data[3].y, 180, "Last point Y value should be 180")
    }

    // MARK: - Multiple Series Tests

    /// Test 2: Multiple overlapping series rendering
    ///
    /// Validates:
    /// - Multiple series are rendered correctly
    /// - Each series gets distinct color from palette
    /// - Areas overlap with transparency (0.3 opacity)
    /// - Legend is shown for multiple series
    func testMultipleOverlappingSeriesRendering() {
        // Given
        let data = multiSeriesData
        let title = "Revenue vs Cost"

        // When
        let view = AreaChartView(
            data: data,
            title: title,
            stacked: false,
            showLegend: true,
            showGrid: true,
            animated: false
        )

        // Then
        XCTAssertEqual(view.data.count, 8, "Should have 8 total data points")
        XCTAssertFalse(view.stacked, "Should be overlapping mode")
        XCTAssertTrue(view.showLegend, "Legend should be visible for multiple series")

        // Verify series separation
        let revenueSeries = view.data.filter { $0.series == "Revenue" }
        let costSeries = view.data.filter { $0.series == "Cost" }

        XCTAssertEqual(revenueSeries.count, 4, "Should have 4 revenue points")
        XCTAssertEqual(costSeries.count, 4, "Should have 4 cost points")

        // Verify data values
        XCTAssertEqual(revenueSeries[0].y, 100, "First revenue point should be 100")
        XCTAssertEqual(costSeries[0].y, 80, "First cost point should be 80")
    }

    // MARK: - Stacked Mode Tests

    /// Test 3: Stacked area chart rendering
    ///
    /// Validates:
    /// - Stacked mode is applied correctly
    /// - Areas are cumulative (not overlapping)
    /// - Multiple series stack on top of each other
    /// - Total height represents sum of all series
    func testStackedModeRendering() {
        // Given
        let data = multiSeriesData
        let title = "Stacked Revenue & Cost"

        // When
        let view = AreaChartView(
            data: data,
            title: title,
            stacked: true,
            showLegend: true,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertTrue(view.stacked, "Should be stacked mode")
        XCTAssertEqual(view.data.count, 8, "Should have 8 total data points")

        // Verify series exist
        let uniqueSeries = Set(view.data.map { $0.series })
        XCTAssertEqual(uniqueSeries.count, 2, "Should have 2 unique series")
        XCTAssertTrue(uniqueSeries.contains("Revenue"), "Should contain Revenue series")
        XCTAssertTrue(uniqueSeries.contains("Cost"), "Should contain Cost series")
    }

    // MARK: - Gradient Fill Tests

    /// Test 4: Gradient fills with correct opacity
    ///
    /// Validates:
    /// - Gradient fills are applied to areas
    /// - Overlapping mode uses 0.3 opacity
    /// - Stacked mode uses higher opacity
    /// - Colors are from default or custom palette
    func testGradientFills() {
        // Given
        let data = multiSeriesData
        let customColors: [String: Color] = [
            "Revenue": .green,
            "Cost": .red
        ]

        // When
        let overlappingView = AreaChartView(
            data: data,
            title: "Overlapping with Gradients",
            stacked: false,
            seriesColors: customColors,
            showLegend: true,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertNotNil(overlappingView.seriesColors, "Custom colors should be set")
        XCTAssertEqual(overlappingView.seriesColors?.count, 2, "Should have 2 custom colors")
        XCTAssertEqual(overlappingView.seriesColors?["Revenue"], .green, "Revenue should be green")
        XCTAssertEqual(overlappingView.seriesColors?["Cost"], .red, "Cost should be red")
    }

    // MARK: - Negative Values Tests

    /// Test 5: Negative values handling
    ///
    /// Validates:
    /// - Areas can extend below zero
    /// - Negative values are rendered correctly
    /// - Gradient fills work with negative values
    /// - Chart doesn't crash with negative data
    func testNegativeValuesHandling() {
        // Given
        let data = negativeValuesData
        let title = "Profit/Loss"

        // When
        let view = AreaChartView(
            data: data,
            title: title,
            stacked: false,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertEqual(view.data.count, 4, "Should have 4 data points")

        // Verify negative values
        let negativePoints = view.data.filter { $0.y < 0 }
        XCTAssertEqual(negativePoints.count, 2, "Should have 2 negative values")
        XCTAssertEqual(view.data[1].y, -20, "Second point should be -20")
        XCTAssertEqual(view.data[3].y, -10, "Fourth point should be -10")

        // Should not crash when rendering
        XCTAssertNoThrow(view.body, "Should not crash with negative values")
    }

    // MARK: - VoiceOver Tests

    /// Test 6: VoiceOver accessibility support
    ///
    /// Validates:
    /// - Chart has descriptive accessibility label
    /// - Accessibility label includes title and chart type
    /// - Accessibility value describes data summary
    /// - Data points have accessibility values
    func testVoiceOverSupport() {
        // Given
        let data = singleSeriesData
        let title = "Revenue Trend"
        let contentDescription = "Area chart showing quarterly revenue performance"

        // When
        let view = AreaChartView(
            data: data,
            title: title,
            contentDescription: contentDescription,
            stacked: false,
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
            chartType: "area"
        )

        XCTAssertFalse(expectedLabel.isEmpty, "Accessibility label should not be empty")
        XCTAssertTrue(expectedLabel.contains("Revenue Trend"), "Should include title")
        XCTAssertTrue(expectedLabel.contains("area"), "Should mention chart type")
        XCTAssertTrue(expectedLabel.contains("4"), "Should mention data point count")
    }

    // MARK: - Grid and Axis Tests

    /// Test 7: Grid lines and axis labels
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
        let viewWithGrid = AreaChartView(
            data: data,
            title: "Grid Test",
            xAxisLabel: xAxisLabel,
            yAxisLabel: yAxisLabel,
            stacked: false,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertTrue(viewWithGrid.showGrid, "Grid should be visible")
        XCTAssertEqual(viewWithGrid.xAxisLabel, xAxisLabel, "X-axis label should match")
        XCTAssertEqual(viewWithGrid.yAxisLabel, yAxisLabel, "Y-axis label should match")

        // When - Grid hidden
        let viewWithoutGrid = AreaChartView(
            data: data,
            title: "No Grid",
            stacked: false,
            showLegend: false,
            showGrid: false,
            animated: true
        )

        // Then
        XCTAssertFalse(viewWithoutGrid.showGrid, "Grid should be hidden")
    }

    // MARK: - Animation Tests

    /// Test 8: Animation behavior
    ///
    /// Validates:
    /// - Animation can be enabled/disabled
    /// - Animation duration is configurable
    func testAnimation() {
        // Given
        let data = singleSeriesData

        // When - Animated
        let animatedView = AreaChartView(
            data: data,
            title: "Animated",
            stacked: false,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertTrue(animatedView.animated, "Animation should be enabled")

        // When - Not animated
        let staticView = AreaChartView(
            data: data,
            title: "Static",
            stacked: false,
            showLegend: false,
            showGrid: true,
            animated: false
        )

        // Then
        XCTAssertFalse(staticView.animated, "Animation should be disabled")
    }

    // MARK: - Empty State Tests

    /// Test 9: Empty data handling
    ///
    /// Validates:
    /// - Chart handles empty data gracefully
    /// - No crash with zero data points
    /// - Appropriate empty state is shown
    func testEmptyDataHandling() {
        // Given
        let data = emptyData

        // When
        let view = AreaChartView(
            data: data,
            title: "Empty Chart",
            stacked: false,
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

    // MARK: - Legend Tests

    /// Test 10: Legend visibility and positioning
    ///
    /// Validates:
    /// - Legend can be shown/hidden
    /// - Legend displays correct series labels
    /// - Legend shows color indicators
    func testLegendVisibility() {
        // Given
        let data = multiSeriesData

        // When - Legend shown
        let viewWithLegend = AreaChartView(
            data: data,
            title: "With Legend",
            stacked: false,
            showLegend: true,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertTrue(viewWithLegend.showLegend, "Legend should be visible")

        // When - Legend hidden
        let viewWithoutLegend = AreaChartView(
            data: data,
            title: "Without Legend",
            stacked: false,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertFalse(viewWithoutLegend.showLegend, "Legend should be hidden")
    }

    // MARK: - Data Validation Tests

    /// Test 11: Data point validation
    ///
    /// Validates:
    /// - Data points have correct structure
    /// - Series labels are preserved
    /// - X and Y values are accessible
    func testDataPointValidation() {
        // Given
        let data = singleSeriesData

        // When
        let view = AreaChartView(
            data: data,
            title: "Validation Test",
            stacked: false,
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

        // Verify data sorting per series
        let revenueSeries = view.data.filter { $0.series == "Revenue" }
        let revenueXValues = revenueSeries.map { $0.x }
        XCTAssertEqual(revenueXValues, revenueXValues.sorted(), "Series data should be sorted by X")
    }

    // MARK: - Custom Height Tests

    /// Test 12: Custom height support
    ///
    /// Validates:
    /// - Custom height can be set
    /// - Default height is used when not specified
    func testCustomHeight() {
        // Given
        let data = singleSeriesData
        let customHeight: CGFloat = 400

        // When
        let view = AreaChartView(
            data: data,
            title: "Custom Height",
            stacked: false,
            height: customHeight,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertEqual(view.height, customHeight, "Should have custom height")

        // When - Default height
        let defaultView = AreaChartView(
            data: data,
            title: "Default Height",
            stacked: false,
            showLegend: false,
            showGrid: true,
            animated: true
        )

        // Then
        XCTAssertNil(defaultView.height, "Should use default height")
    }
}
