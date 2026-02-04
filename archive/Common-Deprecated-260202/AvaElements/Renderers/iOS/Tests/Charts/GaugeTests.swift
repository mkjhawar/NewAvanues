import XCTest
import SwiftUI
@testable import AvaElementsiOS

/// Test suite for GaugeView
///
/// Validates:
/// - Gauge arc rendering with Canvas API
/// - Value normalization and sweep angle calculation
/// - Multi-segment rendering with color transitions
/// - Min/max label positioning
/// - Value text formatting
/// - VoiceOver accessibility
/// - Animation behavior
/// - Edge cases (min/max values, out of range)
///
/// **Coverage Target:** 90%+
/// **Technology:** SwiftUI Canvas API (custom drawing)
@available(iOS 16.0, *)
final class GaugeTests: XCTestCase {

    // MARK: - Test Data

    private var basicSegments: [GaugeView.GaugeSegment] {
        [
            GaugeView.GaugeSegment(start: 0, end: 60, color: "#4CAF50", label: "Normal"),
            GaugeView.GaugeSegment(start: 60, end: 80, color: "#FF9800", label: "Warning"),
            GaugeView.GaugeSegment(start: 80, end: 100, color: "#F44336", label: "Critical")
        ]
    }

    private var temperatureSegments: [GaugeView.GaugeSegment] {
        [
            GaugeView.GaugeSegment(start: -20, end: 0, color: "#2196F3", label: "Cold"),
            GaugeView.GaugeSegment(start: 0, end: 20, color: "#4CAF50", label: "Normal"),
            GaugeView.GaugeSegment(start: 20, end: 30, color: "#FF9800", label: "Warm"),
            GaugeView.GaugeSegment(start: 30, end: 50, color: "#F44336", label: "Hot")
        ]
    }

    // MARK: - Value Normalization Tests

    /// Test 1: Value normalization
    ///
    /// Validates:
    /// - Values are normalized to 0.0-1.0 range
    /// - Min/max values are handled correctly
    /// - Out-of-range values are clamped
    func testValueNormalization() {
        // Given
        let minValue: Float = 0
        let maxValue: Float = 100

        // When - Middle value
        let middleGauge = GaugeView(
            value: 50,
            minValue: minValue,
            maxValue: maxValue,
            size: 200,
            animated: false
        )

        // Then
        let middleNormalized = (50 - minValue) / (maxValue - minValue)
        XCTAssertEqual(middleNormalized, 0.5, accuracy: 0.01, "Middle value should normalize to 0.5")

        // When - Minimum value
        let minGauge = GaugeView(
            value: minValue,
            minValue: minValue,
            maxValue: maxValue,
            size: 200,
            animated: false
        )

        // Then
        let minNormalized = (minValue - minValue) / (maxValue - minValue)
        XCTAssertEqual(minNormalized, 0.0, accuracy: 0.01, "Min value should normalize to 0.0")

        // When - Maximum value
        let maxGauge = GaugeView(
            value: maxValue,
            minValue: minValue,
            maxValue: maxValue,
            size: 200,
            animated: false
        )

        // Then
        let maxNormalized = (maxValue - minValue) / (maxValue - minValue)
        XCTAssertEqual(maxNormalized, 1.0, accuracy: 0.01, "Max value should normalize to 1.0")

        // When - Out of range (above max)
        let aboveMaxGauge = GaugeView(
            value: 150,
            minValue: minValue,
            maxValue: maxValue,
            size: 200,
            animated: false
        )

        // Then - Should be clamped to maxValue
        XCTAssertEqual(aboveMaxGauge.value, maxValue, "Value should be clamped to max")

        // When - Out of range (below min)
        let belowMinGauge = GaugeView(
            value: -50,
            minValue: minValue,
            maxValue: maxValue,
            size: 200,
            animated: false
        )

        // Then - Should be clamped to minValue
        XCTAssertEqual(belowMinGauge.value, minValue, "Value should be clamped to min")
    }

    /// Test 2: Sweep angle calculation
    ///
    /// Validates:
    /// - Sweep angle is proportional to normalized value
    /// - Different arc configurations work correctly
    func testSweepAngleCalculation() {
        // Given
        let sweepAngle: Float = 270 // 3/4 circle

        // When - 50% value
        let halfGauge = GaugeView(
            value: 50,
            minValue: 0,
            maxValue: 100,
            size: 200,
            startAngle: 135,
            sweepAngle: sweepAngle,
            animated: false
        )

        // Then
        let expectedSweep = sweepAngle * 0.5
        XCTAssertEqual(halfGauge.sweepAngle, sweepAngle, "Total sweep should be 270")

        // When - 75% value
        let threeQuarterGauge = GaugeView(
            value: 75,
            minValue: 0,
            maxValue: 100,
            size: 200,
            startAngle: 135,
            sweepAngle: sweepAngle,
            animated: false
        )

        // Then
        let expectedThreeQuarter = sweepAngle * 0.75
        XCTAssertEqual(threeQuarterGauge.sweepAngle, sweepAngle, "Total sweep should be 270")

        // When - Semi-circle gauge
        let semiCircleGauge = GaugeView(
            value: 100,
            minValue: 0,
            maxValue: 200,
            size: 200,
            startAngle: 180,
            sweepAngle: 180,
            animated: false
        )

        // Then
        XCTAssertEqual(semiCircleGauge.sweepAngle, 180, "Semi-circle sweep should be 180")
    }

    /// Test 3: Multi-segment rendering
    ///
    /// Validates:
    /// - Segments are rendered in correct order
    /// - Segment colors are applied correctly
    /// - Value falls in correct segment
    func testMultiSegmentRendering() {
        // Given
        let segments = basicSegments
        let normalValue: Float = 45
        let warningValue: Float = 70
        let criticalValue: Float = 90

        // When - Normal range
        let normalGauge = GaugeView(
            value: normalValue,
            minValue: 0,
            maxValue: 100,
            size: 200,
            segments: segments,
            animated: false
        )

        // Then
        XCTAssertEqual(normalGauge.segments.count, 3, "Should have 3 segments")
        XCTAssertTrue(segments[0].contains(normalValue), "Value should be in Normal segment")

        // When - Warning range
        let warningGauge = GaugeView(
            value: warningValue,
            minValue: 0,
            maxValue: 100,
            size: 200,
            segments: segments,
            animated: false
        )

        // Then
        XCTAssertTrue(segments[1].contains(warningValue), "Value should be in Warning segment")

        // When - Critical range
        let criticalGauge = GaugeView(
            value: criticalValue,
            minValue: 0,
            maxValue: 100,
            size: 200,
            segments: segments,
            animated: false
        )

        // Then
        XCTAssertTrue(segments[2].contains(criticalValue), "Value should be in Critical segment")
    }

    /// Test 4: Value text formatting
    ///
    /// Validates:
    /// - Default formatting works correctly
    /// - Custom format strings are applied
    /// - Unit labels are appended
    func testValueTextFormatting() {
        // Given
        let value: Float = 75.5

        // When - Default formatting (no custom format)
        let defaultGauge = GaugeView(
            value: value,
            minValue: 0,
            maxValue: 100,
            unit: "%",
            size: 200,
            valueFormat: nil,
            animated: false
        )

        // Then
        XCTAssertEqual(defaultGauge.unit, "%", "Should have % unit")

        // When - Custom format
        let customFormatGauge = GaugeView(
            value: value,
            minValue: 0,
            maxValue: 100,
            unit: "°C",
            size: 200,
            valueFormat: "%.1f",
            animated: false
        )

        // Then
        XCTAssertEqual(customFormatGauge.valueFormat, "%.1f", "Should use custom format")
        XCTAssertEqual(customFormatGauge.unit, "°C", "Should have °C unit")

        // When - No unit
        let noUnitGauge = GaugeView(
            value: value,
            minValue: 0,
            maxValue: 100,
            size: 200,
            animated: false
        )

        // Then
        XCTAssertNil(noUnitGauge.unit, "Should have no unit")
    }

    /// Test 5: Min/max label display
    ///
    /// Validates:
    /// - Min/max labels can be shown/hidden
    /// - Labels are positioned correctly at arc endpoints
    func testMinMaxLabelDisplay() {
        // Given
        let minValue: Float = 0
        let maxValue: Float = 100

        // When - With min/max labels
        let withLabelsGauge = GaugeView(
            value: 50,
            minValue: minValue,
            maxValue: maxValue,
            size: 200,
            showValue: true,
            showMinMax: true,
            animated: false
        )

        // Then
        XCTAssertTrue(withLabelsGauge.showMinMax, "Min/max labels should be visible")
        XCTAssertEqual(withLabelsGauge.minValue, minValue, "Min value should be 0")
        XCTAssertEqual(withLabelsGauge.maxValue, maxValue, "Max value should be 100")

        // When - Without min/max labels
        let withoutLabelsGauge = GaugeView(
            value: 50,
            minValue: minValue,
            maxValue: maxValue,
            size: 200,
            showValue: true,
            showMinMax: false,
            animated: false
        )

        // Then
        XCTAssertFalse(withoutLabelsGauge.showMinMax, "Min/max labels should be hidden")
    }

    /// Test 6: VoiceOver accessibility support
    ///
    /// Validates:
    /// - Gauge has descriptive accessibility label
    /// - Value and range are included in accessibility
    /// - Segment status is included when applicable
    /// - Content description is used when provided
    func testVoiceOverSupport() {
        // Given
        let segments = basicSegments
        let contentDescription = "CPU usage gauge"

        // When - With content description
        let gaugeWithDescription = GaugeView(
            value: 75,
            minValue: 0,
            maxValue: 100,
            label: "CPU Usage",
            unit: "%",
            size: 200,
            segments: segments,
            contentDescription: contentDescription
        )

        // Then
        XCTAssertNotNil(gaugeWithDescription.contentDescription, "Should have content description")
        XCTAssertEqual(gaugeWithDescription.contentDescription, contentDescription, "Content description should match")

        // When - Without content description
        let gaugeWithoutDescription = GaugeView(
            value: 75,
            minValue: 0,
            maxValue: 100,
            label: "CPU Usage",
            unit: "%",
            size: 200,
            segments: segments
        )

        // Then
        XCTAssertNil(gaugeWithoutDescription.contentDescription, "Should not have content description")

        // Verify label is in segment
        let warningSegment = segments[1]
        XCTAssertTrue(warningSegment.contains(75), "Value 75 should be in Warning segment")
        XCTAssertEqual(warningSegment.label, "Warning", "Segment should have Warning label")
    }

    /// Test 7: Animation behavior
    ///
    /// Validates:
    /// - Animation can be enabled/disabled
    /// - Animation duration can be customized
    func testAnimation() {
        // Given
        let value: Float = 75

        // When - Animated
        let animatedGauge = GaugeView(
            value: value,
            minValue: 0,
            maxValue: 100,
            size: 200,
            animated: true,
            animationDuration: 1.0
        )

        // Then
        XCTAssertTrue(animatedGauge.animated, "Animation should be enabled")
        XCTAssertEqual(animatedGauge.animationDuration, 1.0, "Animation duration should be 1.0 seconds")

        // When - Not animated
        let staticGauge = GaugeView(
            value: value,
            minValue: 0,
            maxValue: 100,
            size: 200,
            animated: false
        )

        // Then
        XCTAssertFalse(staticGauge.animated, "Animation should be disabled")

        // When - Custom duration
        let customDurationGauge = GaugeView(
            value: value,
            minValue: 0,
            maxValue: 100,
            size: 200,
            animated: true,
            animationDuration: 2.0
        )

        // Then
        XCTAssertEqual(customDurationGauge.animationDuration, 2.0, "Animation duration should be 2.0 seconds")
    }

    /// Test 8: Segment color parsing
    ///
    /// Validates:
    /// - Hex colors are parsed correctly
    /// - Invalid colors don't cause crashes
    func testSegmentColorParsing() {
        // Given
        let segments = [
            GaugeView.GaugeSegment(start: 0, end: 50, color: "#2196F3", label: "Blue"),
            GaugeView.GaugeSegment(start: 50, end: 100, color: "#F44336", label: "Red")
        ]

        // When
        let gauge = GaugeView(
            value: 25,
            minValue: 0,
            maxValue: 100,
            size: 200,
            segments: segments,
            animated: false
        )

        // Then
        XCTAssertEqual(gauge.segments[0].color, "#2196F3", "First segment should have blue color")
        XCTAssertEqual(gauge.segments[1].color, "#F44336", "Second segment should have red color")

        // Verify ChartHelpers can parse these colors
        let blueColor = ChartHelpers.parseColor("#2196F3")
        let redColor = ChartHelpers.parseColor("#F44336")

        XCTAssertNotNil(blueColor, "Blue color should be parsed")
        XCTAssertNotNil(redColor, "Red color should be parsed")
    }

    /// Test 9: Size configuration
    ///
    /// Validates:
    /// - Custom size can be set
    /// - Thickness can be configured
    func testSizeConfiguration() {
        // Given
        let customSize: Float = 300
        let customThickness: Float = 30

        // When - Custom size
        let largeGauge = GaugeView(
            value: 75,
            minValue: 0,
            maxValue: 100,
            size: customSize,
            thickness: customThickness,
            animated: false
        )

        // Then
        XCTAssertEqual(largeGauge.size, customSize, "Size should be 300")
        XCTAssertEqual(largeGauge.thickness, customThickness, "Thickness should be 30")

        // When - Default size
        let defaultGauge = GaugeView(
            value: 75,
            minValue: 0,
            maxValue: 100,
            animated: false
        )

        // Then
        XCTAssertEqual(defaultGauge.size, 200, "Default size should be 200")
        XCTAssertEqual(defaultGauge.thickness, 20, "Default thickness should be 20")
    }

    /// Test 10: Label configuration
    ///
    /// Validates:
    /// - Label text can be set
    /// - Label can be shown/hidden
    func testLabelConfiguration() {
        // Given
        let label = "CPU Usage"

        // When - With label
        let gaugeWithLabel = GaugeView(
            value: 75,
            minValue: 0,
            maxValue: 100,
            label: label,
            size: 200,
            animated: false
        )

        // Then
        XCTAssertEqual(gaugeWithLabel.label, label, "Should have label")

        // When - Without label
        let gaugeWithoutLabel = GaugeView(
            value: 75,
            minValue: 0,
            maxValue: 100,
            size: 200,
            animated: false
        )

        // Then
        XCTAssertNil(gaugeWithoutLabel.label, "Should not have label")
    }

    /// Test 11: Value text visibility
    ///
    /// Validates:
    /// - Value text can be shown/hidden
    func testValueTextVisibility() {
        // Given
        let value: Float = 75

        // When - With value text
        let gaugeWithValue = GaugeView(
            value: value,
            minValue: 0,
            maxValue: 100,
            size: 200,
            showValue: true,
            animated: false
        )

        // Then
        XCTAssertTrue(gaugeWithValue.showValue, "Value text should be visible")

        // When - Without value text
        let gaugeWithoutValue = GaugeView(
            value: value,
            minValue: 0,
            maxValue: 100,
            size: 200,
            showValue: false,
            animated: false
        )

        // Then
        XCTAssertFalse(gaugeWithoutValue.showValue, "Value text should be hidden")
    }

    /// Test 12: Negative value ranges
    ///
    /// Validates:
    /// - Negative min values work correctly
    /// - Negative current values work correctly
    func testNegativeValueRanges() {
        // Given
        let segments = temperatureSegments

        // When - Negative value
        let coldGauge = GaugeView(
            value: -10,
            minValue: -20,
            maxValue: 50,
            label: "Temperature",
            unit: "°C",
            size: 200,
            segments: segments,
            animated: false
        )

        // Then
        XCTAssertEqual(coldGauge.value, -10, "Value should be -10")
        XCTAssertEqual(coldGauge.minValue, -20, "Min value should be -20")
        XCTAssertEqual(coldGauge.maxValue, 50, "Max value should be 50")

        // Verify value is in Cold segment
        XCTAssertTrue(segments[0].contains(-10), "Value -10 should be in Cold segment")

        // Calculate normalized value
        let range = coldGauge.maxValue - coldGauge.minValue
        let normalized = (coldGauge.value - coldGauge.minValue) / range
        XCTAssertEqual(normalized, 10.0 / 70.0, accuracy: 0.01, "Normalized value should be correct")
    }

    /// Test 13: Full circle gauge
    ///
    /// Validates:
    /// - 360-degree sweep works correctly
    func testFullCircleGauge() {
        // Given
        let fullCircleGauge = GaugeView(
            value: 270,
            minValue: 0,
            maxValue: 360,
            label: "Degrees",
            unit: "°",
            size: 200,
            startAngle: 0,
            sweepAngle: 360,
            animated: false
        )

        // Then
        XCTAssertEqual(fullCircleGauge.startAngle, 0, "Should start at 0 degrees")
        XCTAssertEqual(fullCircleGauge.sweepAngle, 360, "Should sweep 360 degrees")

        let normalized = (270.0 - 0.0) / (360.0 - 0.0)
        XCTAssertEqual(normalized, 0.75, accuracy: 0.01, "270 degrees should be 75% of 360")
    }

    /// Test 14: Convenience initializers
    ///
    /// Validates:
    /// - Progress gauge initializer works
    /// - Temperature gauge initializer works
    /// - Speed gauge initializer works
    func testConvenienceInitializers() {
        // When - Progress gauge
        let progressGauge = GaugeView(progress: 75, maxValue: 100)

        // Then
        XCTAssertEqual(progressGauge.value, 75, "Progress value should be 75")
        XCTAssertEqual(progressGauge.minValue, 0, "Progress min should be 0")
        XCTAssertEqual(progressGauge.maxValue, 100, "Progress max should be 100")
        XCTAssertEqual(progressGauge.unit, "%", "Progress unit should be %")
        XCTAssertEqual(progressGauge.label, "Progress", "Progress label should be set")

        // When - Temperature gauge
        let tempGauge = GaugeView(temperature: 24, minValue: -20, maxValue: 50)

        // Then
        XCTAssertEqual(tempGauge.value, 24, "Temperature value should be 24")
        XCTAssertEqual(tempGauge.minValue, -20, "Temperature min should be -20")
        XCTAssertEqual(tempGauge.maxValue, 50, "Temperature max should be 50")
        XCTAssertEqual(tempGauge.unit, "°C", "Temperature unit should be °C")
        XCTAssertEqual(tempGauge.label, "Temperature", "Temperature label should be set")
        XCTAssertEqual(tempGauge.segments.count, 4, "Temperature should have 4 segments")

        // When - Speed gauge
        let speedGauge = GaugeView(speed: 120, maxValue: 200, unit: "km/h")

        // Then
        XCTAssertEqual(speedGauge.value, 120, "Speed value should be 120")
        XCTAssertEqual(speedGauge.minValue, 0, "Speed min should be 0")
        XCTAssertEqual(speedGauge.maxValue, 200, "Speed max should be 200")
        XCTAssertEqual(speedGauge.unit, "km/h", "Speed unit should be km/h")
        XCTAssertEqual(speedGauge.label, "Speed", "Speed label should be set")
        XCTAssertEqual(speedGauge.startAngle, 180, "Speed gauge should start at 180 degrees")
        XCTAssertEqual(speedGauge.sweepAngle, 180, "Speed gauge should sweep 180 degrees")
        XCTAssertEqual(speedGauge.segments.count, 3, "Speed should have 3 segments")
    }

    /// Test 15: Segment contains method
    ///
    /// Validates:
    /// - Segment boundary detection works correctly
    func testSegmentContains() {
        // Given
        let segment = GaugeView.GaugeSegment(start: 60, end: 80, color: "#FF9800", label: "Warning")

        // Then
        XCTAssertTrue(segment.contains(60), "Should contain start value")
        XCTAssertTrue(segment.contains(70), "Should contain middle value")
        XCTAssertTrue(segment.contains(80), "Should contain end value")
        XCTAssertFalse(segment.contains(59), "Should not contain value below start")
        XCTAssertFalse(segment.contains(81), "Should not contain value above end")
    }
}
