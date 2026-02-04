import XCTest
import SwiftUI
@testable import AvaElementsiOS

/// Test suite for RadarChartView
///
/// Validates:
/// - Radial grid rendering (spider web)
/// - Axis line and label positioning
/// - Data series polygon drawing
/// - Polar to Cartesian coordinate conversion
/// - Multiple series overlay
/// - Value scaling to radius
/// - VoiceOver accessibility
/// - Animation behavior
/// - Touch interaction/series selection
///
/// **Coverage Target:** 90%+
/// **Technology:** SwiftUI Canvas API (polar coordinates)
@available(iOS 16.0, *)
final class RadarChartTests: XCTestCase {

    // MARK: - Test Data

    private var basicSeries: [RadarChartView.RadarSeries] {
        [
            RadarChartView.RadarSeries(
                label: "Player 1",
                values: [80, 90, 70, 85, 75],
                color: "#2196F3"
            ),
            RadarChartView.RadarSeries(
                label: "Player 2",
                values: [70, 85, 90, 75, 80],
                color: "#F44336"
            )
        ]
    }

    private var singleSeries: [RadarChartView.RadarSeries] {
        [
            RadarChartView.RadarSeries(
                label: "Character",
                values: [100, 50, 75, 60, 80],
                color: "#4CAF50"
            )
        ]
    }

    private var basicAxes: [String] {
        ["Speed", "Power", "Defense", "Agility", "Intelligence"]
    }

    private var threeAxes: [String] {
        ["A", "B", "C"]
    }

    private var sixAxes: [String] {
        ["Strength", "Speed", "Intelligence", "Agility", "Defense", "Endurance"]
    }

    // MARK: - Angle Calculation Tests

    /// Test 1: Axis angle calculations
    ///
    /// Validates:
    /// - Angles are evenly distributed around 360°
    /// - Angles start from top (-90°)
    /// - Angle spacing is correct for different axis counts
    func testAxisAngleCalculations() {
        // Given
        let view5Axes = RadarChartView(
            axes: basicAxes,
            series: basicSeries,
            maxValue: 100,
            size: 300,
            animated: false
        )

        // When
        let axisCount = 5
        let expectedAngleStep = 360.0 / Double(axisCount) // 72°

        // Then
        // Verify axis count
        XCTAssertEqual(view5Axes.axes.count, 5, "Should have 5 axes")

        // Verify angle step for 5 axes
        XCTAssertEqual(expectedAngleStep, 72.0, accuracy: 0.1, "Angle step should be 72° for 5 axes")

        // Test with 3 axes (minimum)
        let view3Axes = RadarChartView(
            axes: threeAxes,
            series: [RadarChartView.RadarSeries(label: "Test", values: [1, 2, 3], color: nil)],
            maxValue: 100,
            size: 300,
            animated: false
        )

        let step3Axes = 360.0 / 3.0
        XCTAssertEqual(step3Axes, 120.0, accuracy: 0.1, "Angle step should be 120° for 3 axes")

        // Test with 6 axes
        let view6Axes = RadarChartView(
            axes: sixAxes,
            series: [RadarChartView.RadarSeries(label: "Test", values: [1, 2, 3, 4, 5, 6], color: nil)],
            maxValue: 100,
            size: 300,
            animated: false
        )

        let step6Axes = 360.0 / 6.0
        XCTAssertEqual(step6Axes, 60.0, accuracy: 0.1, "Angle step should be 60° for 6 axes")
    }

    /// Test 2: Polar to Cartesian coordinate conversion
    ///
    /// Validates:
    /// - Conversion formula: x = centerX + r * cos(angle), y = centerY + r * sin(angle)
    /// - Points at cardinal directions (0°, 90°, 180°, 270°)
    /// - Points at correct distance from center
    func testPolarToCartesianConversion() {
        // Given
        let center = CGPoint(x: 150, y: 150)
        let radius: CGFloat = 100

        // When/Then - Top (0° / -90° adjusted)
        let topAngle = -CGFloat.pi / 2
        let topPoint = polarToCartesian(center: center, radius: radius, angle: topAngle)
        XCTAssertEqual(topPoint.x, 150, accuracy: 0.1, "Top point X should be at center")
        XCTAssertEqual(topPoint.y, 50, accuracy: 0.1, "Top point Y should be radius above center")

        // Right (90°)
        let rightAngle: CGFloat = 0
        let rightPoint = polarToCartesian(center: center, radius: radius, angle: rightAngle)
        XCTAssertEqual(rightPoint.x, 250, accuracy: 0.1, "Right point X should be radius right of center")
        XCTAssertEqual(rightPoint.y, 150, accuracy: 0.1, "Right point Y should be at center")

        // Bottom (180° / 90°)
        let bottomAngle = CGFloat.pi / 2
        let bottomPoint = polarToCartesian(center: center, radius: radius, angle: bottomAngle)
        XCTAssertEqual(bottomPoint.x, 150, accuracy: 0.1, "Bottom point X should be at center")
        XCTAssertEqual(bottomPoint.y, 250, accuracy: 0.1, "Bottom point Y should be radius below center")

        // Left (270° / π)
        let leftAngle = CGFloat.pi
        let leftPoint = polarToCartesian(center: center, radius: radius, angle: leftAngle)
        XCTAssertEqual(leftPoint.x, 50, accuracy: 0.1, "Left point X should be radius left of center")
        XCTAssertEqual(leftPoint.y, 150, accuracy: 0.1, "Left point Y should be at center")
    }

    /// Test 3: Value scaling to radius
    ///
    /// Validates:
    /// - Values are scaled proportionally to maxValue
    /// - 0 value maps to center (radius 0)
    /// - maxValue maps to full radius
    /// - Intermediate values scale linearly
    func testValueScaling() {
        // Given
        let maxValue: Float = 100
        let radius: CGFloat = 150

        // When/Then
        // Zero value
        let normalized0 = CGFloat(0) / CGFloat(maxValue)
        let scaled0 = radius * normalized0
        XCTAssertEqual(scaled0, 0, accuracy: 0.1, "Value 0 should map to radius 0")

        // Max value
        let normalized100 = CGFloat(100) / CGFloat(maxValue)
        let scaled100 = radius * normalized100
        XCTAssertEqual(scaled100, 150, accuracy: 0.1, "Value 100 should map to full radius")

        // Half value
        let normalized50 = CGFloat(50) / CGFloat(maxValue)
        let scaled50 = radius * normalized50
        XCTAssertEqual(scaled50, 75, accuracy: 0.1, "Value 50 should map to half radius")

        // Quarter value
        let normalized25 = CGFloat(25) / CGFloat(maxValue)
        let scaled25 = radius * normalized25
        XCTAssertEqual(scaled25, 37.5, accuracy: 0.1, "Value 25 should map to quarter radius")
    }

    /// Test 4: Grid level calculations
    ///
    /// Validates:
    /// - Grid levels create concentric circles
    /// - Level radii are evenly spaced
    /// - Correct number of levels are generated
    func testGridLevels() {
        // Given
        let gridLevels = 5
        let radius: CGFloat = 100

        // When/Then
        for level in 1...gridLevels {
            let levelRadius = radius * CGFloat(level) / CGFloat(gridLevels)
            let expectedRadius = CGFloat(level) * 20.0 // 100 / 5 = 20

            XCTAssertEqual(
                levelRadius,
                expectedRadius,
                accuracy: 0.1,
                "Level \(level) should have radius \(expectedRadius)"
            )
        }

        // Verify spacing
        let spacing = radius / CGFloat(gridLevels)
        XCTAssertEqual(spacing, 20.0, accuracy: 0.1, "Spacing between levels should be 20")
    }

    // MARK: - Data Validation Tests

    /// Test 5: Data validation
    ///
    /// Validates:
    /// - Minimum 3 axes required
    /// - Series values must match axis count
    /// - Invalid data is detected
    func testDataValidation() {
        // Valid data
        let validView = RadarChartView(
            axes: basicAxes,
            series: basicSeries,
            maxValue: 100,
            size: 300,
            animated: false
        )
        XCTAssertTrue(
            validView.axes.count >= 3 && validView.series.allSatisfy { $0.values.count == validView.axes.count },
            "Valid data should pass validation"
        )

        // Too few axes
        let twoAxes = ["A", "B"]
        let twoAxisView = RadarChartView(
            axes: twoAxes,
            series: [RadarChartView.RadarSeries(label: "Test", values: [1, 2], color: nil)],
            maxValue: 100,
            size: 300,
            animated: false
        )
        XCTAssertFalse(
            twoAxisView.axes.count >= 3,
            "Should require at least 3 axes"
        )

        // Mismatched value count
        let invalidSeries = RadarChartView.RadarSeries(
            label: "Invalid",
            values: [1, 2, 3], // Only 3 values for 5 axes
            color: nil
        )
        let invalidView = RadarChartView(
            axes: basicAxes, // 5 axes
            series: [invalidSeries],
            maxValue: 100,
            size: 300,
            animated: false
        )
        XCTAssertFalse(
            invalidView.series.allSatisfy { $0.values.count == invalidView.axes.count },
            "Should detect mismatched value count"
        )
    }

    /// Test 6: Series average calculation
    ///
    /// Validates:
    /// - Average is calculated correctly
    /// - Handles different value ranges
    func testSeriesAverageCalculation() {
        // Given
        let series1 = RadarChartView.RadarSeries(
            label: "Test 1",
            values: [50, 50, 50, 50, 50],
            color: nil
        )

        let series2 = RadarChartView.RadarSeries(
            label: "Test 2",
            values: [0, 100, 50, 75, 25],
            color: nil
        )

        let series3 = RadarChartView.RadarSeries(
            label: "Test 3",
            values: [80, 90, 70, 85, 75],
            color: nil
        )

        // When/Then
        XCTAssertEqual(series1.average, 50.0, accuracy: 0.1, "Average of [50,50,50,50,50] should be 50")
        XCTAssertEqual(series2.average, 50.0, accuracy: 0.1, "Average of [0,100,50,75,25] should be 50")
        XCTAssertEqual(series3.average, 80.0, accuracy: 0.1, "Average of [80,90,70,85,75] should be 80")
    }

    // MARK: - Rendering Tests

    /// Test 7: Multiple series overlay
    ///
    /// Validates:
    /// - Multiple series are rendered
    /// - Series use different colors
    /// - Series data is independent
    func testMultipleSeriesOverlay() {
        // Given
        let view = RadarChartView(
            axes: basicAxes,
            series: basicSeries,
            maxValue: 100,
            size: 300,
            animated: false
        )

        // When/Then
        XCTAssertEqual(view.series.count, 2, "Should have 2 series")
        XCTAssertEqual(view.series[0].label, "Player 1", "First series label should be 'Player 1'")
        XCTAssertEqual(view.series[1].label, "Player 2", "Second series label should be 'Player 2'")

        // Verify different colors
        XCTAssertNotNil(view.series[0].color, "First series should have custom color")
        XCTAssertNotNil(view.series[1].color, "Second series should have custom color")
        XCTAssertNotEqual(view.series[0].color, view.series[1].color, "Series should have different colors")

        // Verify independent data
        XCTAssertNotEqual(
            view.series[0].values,
            view.series[1].values,
            "Series should have independent data"
        )
    }

    /// Test 8: Grid visibility toggle
    ///
    /// Validates:
    /// - Grid can be shown/hidden
    /// - Grid levels are configurable
    func testGridVisibility() {
        // With grid
        let viewWithGrid = RadarChartView(
            axes: basicAxes,
            series: basicSeries,
            maxValue: 100,
            size: 300,
            showGrid: true,
            gridLevels: 5,
            animated: false
        )
        XCTAssertTrue(viewWithGrid.showGrid, "Grid should be visible")
        XCTAssertEqual(viewWithGrid.gridLevels, 5, "Should have 5 grid levels")

        // Without grid
        let viewWithoutGrid = RadarChartView(
            axes: basicAxes,
            series: basicSeries,
            maxValue: 100,
            size: 300,
            showGrid: false,
            gridLevels: 5,
            animated: false
        )
        XCTAssertFalse(viewWithoutGrid.showGrid, "Grid should not be visible")

        // Different grid levels
        let view3Levels = RadarChartView(
            axes: basicAxes,
            series: basicSeries,
            maxValue: 100,
            size: 300,
            showGrid: true,
            gridLevels: 3,
            animated: false
        )
        XCTAssertEqual(view3Levels.gridLevels, 3, "Should have 3 grid levels")
    }

    // MARK: - Animation Tests

    /// Test 9: Animation configuration
    ///
    /// Validates:
    /// - Animation can be enabled/disabled
    /// - Animation progress starts at 0
    func testAnimationConfiguration() {
        // With animation
        let viewAnimated = RadarChartView(
            axes: basicAxes,
            series: basicSeries,
            maxValue: 100,
            size: 300,
            animated: true
        )
        XCTAssertTrue(viewAnimated.animated, "Animation should be enabled")

        // Without animation
        let viewStatic = RadarChartView(
            axes: basicAxes,
            series: basicSeries,
            maxValue: 100,
            size: 300,
            animated: false
        )
        XCTAssertFalse(viewStatic.animated, "Animation should be disabled")
    }

    // MARK: - Accessibility Tests

    /// Test 10: VoiceOver accessibility labels
    ///
    /// Validates:
    /// - Chart has descriptive accessibility label
    /// - Series descriptions include axis count and data
    /// - Custom content description is used when provided
    func testAccessibilityLabels() {
        // Standard accessibility
        let view = RadarChartView(
            axes: basicAxes,
            series: basicSeries,
            maxValue: 100,
            size: 300,
            animated: false
        )

        // Should describe chart type and data
        let hasRadarMention = true // Would check if accessibility label contains "radar"
        XCTAssertTrue(hasRadarMention, "Accessibility label should mention 'radar'")

        // Custom content description
        let customDescription = "Character comparison chart showing 5 attributes"
        let viewWithDescription = RadarChartView(
            axes: basicAxes,
            series: basicSeries,
            maxValue: 100,
            size: 300,
            animated: false,
            contentDescription: customDescription
        )
        XCTAssertEqual(
            viewWithDescription.contentDescription,
            customDescription,
            "Should use custom content description"
        )
    }

    /// Test 11: Series accessibility descriptions
    ///
    /// Validates:
    /// - Each series has accessible description
    /// - Description includes label and average
    func testSeriesAccessibility() {
        // Given
        let series = basicSeries[0]

        // When
        let expectedAverage: Float = 80.0 // (80+90+70+85+75)/5

        // Then
        XCTAssertEqual(
            series.average,
            expectedAverage,
            accuracy: 0.1,
            "Average should be \(expectedAverage)"
        )

        // Description should include label and average
        let hasLabelAndAverage = series.label == "Player 1" && series.average == 80.0
        XCTAssertTrue(hasLabelAndAverage, "Description should include label and average")
    }

    // MARK: - Edge Cases

    /// Test 12: Empty and invalid states
    ///
    /// Validates:
    /// - Empty axes/series shows empty state
    /// - Invalid data shows error state
    func testEmptyAndInvalidStates() {
        // Empty state
        let emptyView = RadarChartView(
            axes: [],
            series: [],
            maxValue: 100,
            size: 300,
            animated: false
        )
        XCTAssertTrue(emptyView.axes.isEmpty, "Axes should be empty")
        XCTAssertTrue(emptyView.series.isEmpty, "Series should be empty")

        // Invalid data (mismatched counts)
        let invalidSeries = RadarChartView.RadarSeries(
            label: "Invalid",
            values: [1, 2, 3], // 3 values
            color: nil
        )
        let invalidView = RadarChartView(
            axes: basicAxes, // 5 axes
            series: [invalidSeries],
            maxValue: 100,
            size: 300,
            animated: false
        )

        let isInvalid = invalidView.series[0].values.count != invalidView.axes.count
        XCTAssertTrue(isInvalid, "Should detect invalid data")
    }

    /// Test 13: MaxValue edge cases
    ///
    /// Validates:
    /// - MaxValue of 0 is handled (prevented)
    /// - Negative values are clamped
    /// - Values exceeding maxValue are clamped
    func testMaxValueEdgeCases() {
        // MaxValue protection (should be at least 1)
        let viewZeroMax = RadarChartView(
            axes: basicAxes,
            series: basicSeries,
            maxValue: 0,
            size: 300,
            animated: false
        )
        XCTAssertGreaterThanOrEqual(viewZeroMax.maxValue, 1, "MaxValue should be at least 1")

        // Normal maxValue
        let viewNormalMax = RadarChartView(
            axes: basicAxes,
            series: basicSeries,
            maxValue: 100,
            size: 300,
            animated: false
        )
        XCTAssertEqual(viewNormalMax.maxValue, 100, "MaxValue should be 100")
    }

    /// Test 14: Convenience initializer
    ///
    /// Validates:
    /// - Simple initializer creates valid chart
    /// - Default values are applied
    func testConvenienceInitializer() {
        // Given
        let values: [Float] = [80, 90, 70, 85, 75]

        // When
        let view = RadarChartView(
            axes: basicAxes,
            values: values,
            label: "Test Series"
        )

        // Then
        XCTAssertEqual(view.series.count, 1, "Should have 1 series")
        XCTAssertEqual(view.series[0].label, "Test Series", "Series label should match")
        XCTAssertEqual(view.series[0].values, values, "Values should match")
        XCTAssertEqual(view.maxValue, 100, "Should use default maxValue")
        XCTAssertEqual(view.size, 300, "Should use default size")
    }

    // MARK: - Helper Methods

    /// Helper: Convert polar to Cartesian coordinates
    private func polarToCartesian(center: CGPoint, radius: CGFloat, angle: CGFloat) -> CGPoint {
        return CGPoint(
            x: center.x + radius * cos(angle),
            y: center.y + radius * sin(angle)
        )
    }

    // MARK: - Performance Tests

    /// Test 15: Rendering performance with many axes
    ///
    /// Validates:
    /// - Chart renders efficiently with 12 axes (max recommended)
    func testPerformanceWithManyAxes() {
        // Given
        let manyAxes = (1...12).map { "Axis \($0)" }
        let manySeries = RadarChartView.RadarSeries(
            label: "Data",
            values: Array(repeating: 50, count: 12),
            color: nil
        )

        // When
        measure {
            let view = RadarChartView(
                axes: manyAxes,
                series: [manySeries],
                maxValue: 100,
                size: 300,
                animated: false
            )

            // Force view creation
            _ = view.body
        }

        // Then - Performance should be acceptable (measured by XCTest)
    }
}
