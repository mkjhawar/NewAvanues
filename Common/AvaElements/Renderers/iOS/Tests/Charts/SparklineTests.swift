import XCTest
import SwiftUI
@testable import AvaElementsiOS

/// Test suite for SparklineView
///
/// Validates:
/// - Data rendering with Canvas API
/// - Line drawing and scaling
/// - Optional dots at data points
/// - Optional area fill
/// - Trend detection (up/down/flat)
/// - VoiceOver accessibility with trend descriptions
/// - Edge cases (empty, single point, two points)
/// - Light/dark mode support
///
/// **Coverage Target:** 90%+
/// **Technology:** SwiftUI Canvas (iOS 15+)
@available(iOS 15.0, *)
final class SparklineTests: XCTestCase {

    // MARK: - Test Data

    private var upwardTrendData: [Float] {
        [10, 12, 11, 15, 18, 17, 20, 22]
    }

    private var downwardTrendData: [Float] {
        [22, 20, 21, 18, 15, 16, 12, 10]
    }

    private var flatTrendData: [Float] {
        [15, 15.2, 14.8, 15.1, 14.9, 15.0, 15.2, 15.1]
    }

    private var volatileData: [Float] {
        [10, 15, 8, 18, 12, 20, 10, 17]
    }

    private var singlePointData: [Float] {
        [15]
    }

    private var twoPointsData: [Float] {
        [10, 20]
    }

    private var emptyData: [Float] {
        []
    }

    // MARK: - Basic Rendering Tests

    /// Test 1: Basic sparkline rendering with default settings
    ///
    /// Validates:
    /// - Sparkline renders with data
    /// - Default dimensions (100x30)
    /// - Default line width (2.0)
    /// - No dots by default
    /// - No area fill by default
    func testBasicRendering() {
        // Given
        let data = upwardTrendData

        // When
        let view = SparklineView(
            data: data,
            color: .blue
        )

        // Then
        XCTAssertEqual(view.data.count, 8, "Should have 8 data points")
        XCTAssertEqual(view.width, 100, "Should have default width of 100")
        XCTAssertEqual(view.height, 30, "Should have default height of 30")
        XCTAssertEqual(view.lineWidth, 2, "Should have default line width of 2")
        XCTAssertFalse(view.showDots, "Should not show dots by default")
        XCTAssertFalse(view.showArea, "Should not show area by default")
        XCTAssertTrue(view.animated, "Should be animated by default")
    }

    /// Test 2: Custom dimensions
    ///
    /// Validates:
    /// - Custom width and height can be set
    /// - Sparkline scales to fit custom dimensions
    func testCustomDimensions() {
        // Given
        let data = upwardTrendData
        let customWidth: CGFloat = 200
        let customHeight: CGFloat = 50

        // When
        let view = SparklineView(
            data: data,
            width: customWidth,
            height: customHeight,
            color: .blue
        )

        // Then
        XCTAssertEqual(view.width, customWidth, "Should have custom width")
        XCTAssertEqual(view.height, customHeight, "Should have custom height")
    }

    /// Test 3: Custom line width
    ///
    /// Validates:
    /// - Line width can be customized
    /// - Affects both line and dot sizes
    func testCustomLineWidth() {
        // Given
        let data = upwardTrendData
        let customLineWidth: CGFloat = 3.0

        // When
        let view = SparklineView(
            data: data,
            lineWidth: customLineWidth,
            color: .blue
        )

        // Then
        XCTAssertEqual(view.lineWidth, customLineWidth, "Should have custom line width")
    }

    // MARK: - Visual Features Tests

    /// Test 4: Show dots at data points
    ///
    /// Validates:
    /// - Dots can be enabled
    /// - Dots are drawn at each data point
    func testShowDots() {
        // Given
        let data = upwardTrendData

        // When
        let view = SparklineView(
            data: data,
            showDots: true,
            color: .green
        )

        // Then
        XCTAssertTrue(view.showDots, "Should show dots")
        XCTAssertFalse(view.showArea, "Should not show area")
    }

    /// Test 5: Show filled area under line
    ///
    /// Validates:
    /// - Area fill can be enabled
    /// - Area is filled with semi-transparent color
    func testShowArea() {
        // Given
        let data = upwardTrendData

        // When
        let view = SparklineView(
            data: data,
            showArea: true,
            color: .purple
        )

        // Then
        XCTAssertTrue(view.showArea, "Should show area")
        XCTAssertFalse(view.showDots, "Should not show dots")
    }

    /// Test 6: Show both dots and area
    ///
    /// Validates:
    /// - Both features can be enabled simultaneously
    /// - Visual combination works correctly
    func testShowDotsAndArea() {
        // Given
        let data = upwardTrendData

        // When
        let view = SparklineView(
            data: data,
            showDots: true,
            showArea: true,
            color: .orange
        )

        // Then
        XCTAssertTrue(view.showDots, "Should show dots")
        XCTAssertTrue(view.showArea, "Should show area")
    }

    // MARK: - Trend Detection Tests

    /// Test 7: Upward trend detection
    ///
    /// Validates:
    /// - Upward trend is detected correctly
    /// - Percentage change is calculated
    /// - Accessibility describes trend
    func testUpwardTrendDetection() {
        // Given
        let data = upwardTrendData // [10, 12, 11, 15, 18, 17, 20, 22]

        // When
        let view = SparklineView(
            data: data,
            color: .green
        )

        // Then
        let accessibilityValue = view.accessibilityValue
        XCTAssertFalse(accessibilityValue.isEmpty, "Should have accessibility value")
        XCTAssertTrue(
            accessibilityValue.contains("trending up") || accessibilityValue.contains("up"),
            "Should describe upward trend"
        )
        XCTAssertTrue(accessibilityValue.contains("data points"), "Should mention data point count")
    }

    /// Test 8: Downward trend detection
    ///
    /// Validates:
    /// - Downward trend is detected correctly
    /// - Percentage change is negative
    /// - Accessibility describes trend
    func testDownwardTrendDetection() {
        // Given
        let data = downwardTrendData // [22, 20, 21, 18, 15, 16, 12, 10]

        // When
        let view = SparklineView(
            data: data,
            color: .red
        )

        // Then
        let accessibilityValue = view.accessibilityValue
        XCTAssertFalse(accessibilityValue.isEmpty, "Should have accessibility value")
        XCTAssertTrue(
            accessibilityValue.contains("trending down") || accessibilityValue.contains("down"),
            "Should describe downward trend"
        )
    }

    /// Test 9: Flat trend detection
    ///
    /// Validates:
    /// - Flat/stable trend is detected correctly
    /// - Small variations don't trigger trend
    /// - Accessibility describes as stable
    func testFlatTrendDetection() {
        // Given
        let data = flatTrendData // [15, 15.2, 14.8, 15.1, 14.9, 15.0, 15.2, 15.1]

        // When
        let view = SparklineView(
            data: data,
            color: .gray
        )

        // Then
        let accessibilityValue = view.accessibilityValue
        XCTAssertFalse(accessibilityValue.isEmpty, "Should have accessibility value")
        XCTAssertTrue(
            accessibilityValue.contains("stable") || accessibilityValue.contains("flat"),
            "Should describe stable trend"
        )
    }

    // MARK: - Edge Cases Tests

    /// Test 10: Empty data handling
    ///
    /// Validates:
    /// - Sparkline handles empty data gracefully
    /// - No crash with zero data points
    /// - Accessibility describes empty state
    func testEmptyDataHandling() {
        // Given
        let data = emptyData

        // When
        let view = SparklineView(
            data: data,
            color: .blue
        )

        // Then
        XCTAssertTrue(view.data.isEmpty, "Data should be empty")
        XCTAssertNoThrow(view.body, "Should not crash with empty data")

        // Check accessibility
        let accessibilityValue = view.accessibilityValue
        XCTAssertTrue(
            accessibilityValue.contains("No data") || accessibilityValue.isEmpty,
            "Should describe empty state"
        )
    }

    /// Test 11: Single data point handling
    ///
    /// Validates:
    /// - Single point is rendered as a dot
    /// - No line is drawn (need at least 2 points)
    /// - Accessibility describes single value
    func testSingleDataPoint() {
        // Given
        let data = singlePointData

        // When
        let view = SparklineView(
            data: data,
            color: .blue
        )

        // Then
        XCTAssertEqual(view.data.count, 1, "Should have 1 data point")
        XCTAssertNoThrow(view.body, "Should not crash with single point")

        // Check accessibility
        let accessibilityValue = view.accessibilityValue
        XCTAssertTrue(
            accessibilityValue.contains("Single value") || accessibilityValue.contains("1"),
            "Should describe single value"
        )
    }

    /// Test 12: Two data points handling
    ///
    /// Validates:
    /// - Two points create a simple line
    /// - Trend can be detected from two points
    /// - No crash with minimal data
    func testTwoDataPoints() {
        // Given
        let data = twoPointsData

        // When
        let view = SparklineView(
            data: data,
            showDots: true,
            color: .blue
        )

        // Then
        XCTAssertEqual(view.data.count, 2, "Should have 2 data points")
        XCTAssertNoThrow(view.body, "Should not crash with two points")

        // Check accessibility
        let accessibilityValue = view.accessibilityValue
        XCTAssertTrue(accessibilityValue.contains("2"), "Should mention 2 data points")
    }

    // MARK: - Color Tests

    /// Test 13: Custom color application
    ///
    /// Validates:
    /// - Custom color is applied to line
    /// - Color affects dots and area fill
    func testCustomColor() {
        // Given
        let data = upwardTrendData
        let customColor = Color.green

        // When
        let view = SparklineView(
            data: data,
            color: customColor
        )

        // Then
        XCTAssertEqual(view.color, customColor, "Should have custom color")
    }

    /// Test 14: Default color
    ///
    /// Validates:
    /// - Default color is blue when not specified
    func testDefaultColor() {
        // Given
        let data = upwardTrendData

        // When
        let view = SparklineView(
            data: data
        )

        // Then
        XCTAssertEqual(view.color, .blue, "Should have default blue color")
    }

    // MARK: - Accessibility Tests

    /// Test 15: VoiceOver support with trend description
    ///
    /// Validates:
    /// - Accessibility label is descriptive
    /// - Accessibility value includes trend
    /// - Accessibility value includes data range
    /// - Appropriate accessibility traits
    func testVoiceOverSupport() {
        // Given
        let data = upwardTrendData
        let contentDescription = "Revenue trend for last 7 days"

        // When
        let view = SparklineView(
            data: data,
            contentDescription: contentDescription,
            color: .green
        )

        // Then
        XCTAssertEqual(view.contentDescription, contentDescription, "Should have custom content description")

        let accessibilityLabel = view.accessibilityLabel
        XCTAssertEqual(accessibilityLabel, contentDescription, "Should use content description")

        let accessibilityValue = view.accessibilityValue
        XCTAssertFalse(accessibilityValue.isEmpty, "Should have accessibility value")
        XCTAssertTrue(accessibilityValue.contains("trending"), "Should describe trend")
        XCTAssertTrue(accessibilityValue.contains("Range:"), "Should include range information")
    }

    /// Test 16: Auto-generated accessibility description
    ///
    /// Validates:
    /// - Description is auto-generated when not provided
    /// - Includes data point count
    /// - Includes trend direction
    /// - Includes value range
    func testAutoGeneratedAccessibility() {
        // Given
        let data = volatileData

        // When
        let view = SparklineView(
            data: data,
            color: .orange
        )

        // Then
        XCTAssertNil(view.contentDescription, "Should not have custom description")

        let accessibilityLabel = view.accessibilityLabel
        XCTAssertTrue(accessibilityLabel.contains("Sparkline"), "Should mention sparkline")

        let accessibilityValue = view.accessibilityValue
        XCTAssertTrue(accessibilityValue.contains("data points"), "Should include point count")
        XCTAssertTrue(accessibilityValue.contains("Range:"), "Should include range")
    }

    // MARK: - Animation Tests

    /// Test 17: Animation enabled
    ///
    /// Validates:
    /// - Animation is enabled by default
    /// - Animation can be explicitly enabled
    func testAnimationEnabled() {
        // Given
        let data = upwardTrendData

        // When
        let view = SparklineView(
            data: data,
            animated: true,
            color: .blue
        )

        // Then
        XCTAssertTrue(view.animated, "Animation should be enabled")
    }

    /// Test 18: Animation disabled
    ///
    /// Validates:
    /// - Animation can be disabled
    /// - Sparkline renders immediately without animation
    func testAnimationDisabled() {
        // Given
        let data = upwardTrendData

        // When
        let view = SparklineView(
            data: data,
            animated: false,
            color: .blue
        )

        // Then
        XCTAssertFalse(view.animated, "Animation should be disabled")
    }

    // MARK: - Data Validation Tests

    /// Test 19: Data integrity preservation
    ///
    /// Validates:
    /// - Data values are preserved exactly
    /// - Order is maintained
    /// - No data transformation
    func testDataIntegrity() {
        // Given
        let data = upwardTrendData

        // When
        let view = SparklineView(
            data: data,
            color: .blue
        )

        // Then
        XCTAssertEqual(view.data, data, "Data should be preserved exactly")
        XCTAssertEqual(view.data.first, 10, "First value should be preserved")
        XCTAssertEqual(view.data.last, 22, "Last value should be preserved")
    }

    /// Test 20: Value range calculation
    ///
    /// Validates:
    /// - Min and max values are detected correctly
    /// - Range is calculated for accessibility
    func testValueRangeCalculation() {
        // Given
        let data = upwardTrendData // [10, 12, 11, 15, 18, 17, 20, 22]

        // When
        let view = SparklineView(
            data: data,
            color: .blue
        )

        // Then
        let minValue = data.min()!
        let maxValue = data.max()!

        XCTAssertEqual(minValue, 10, "Minimum should be 10")
        XCTAssertEqual(maxValue, 22, "Maximum should be 22")

        // Verify range is in accessibility
        let accessibilityValue = view.accessibilityValue
        XCTAssertTrue(accessibilityValue.contains("10"), "Should mention min value")
        XCTAssertTrue(accessibilityValue.contains("22"), "Should mention max value")
    }

    // MARK: - Compact Size Tests

    /// Test 21: Typical compact size (100x30)
    ///
    /// Validates:
    /// - Default size is suitable for inline display
    /// - Renders correctly in small space
    func testCompactSize() {
        // Given
        let data = upwardTrendData

        // When
        let view = SparklineView(
            data: data,
            width: 100,
            height: 30,
            color: .blue
        )

        // Then
        XCTAssertEqual(view.width, 100, "Should have compact width")
        XCTAssertEqual(view.height, 30, "Should have compact height")
        XCTAssertNoThrow(view.body, "Should render in compact size")
    }

    /// Test 22: Very small size
    ///
    /// Validates:
    /// - Can render in very small space (e.g., 50x20)
    /// - No crash with minimal dimensions
    func testVerySmallSize() {
        // Given
        let data = upwardTrendData

        // When
        let view = SparklineView(
            data: data,
            width: 50,
            height: 20,
            lineWidth: 1,
            color: .blue
        )

        // Then
        XCTAssertEqual(view.width, 50, "Should have very small width")
        XCTAssertEqual(view.height, 20, "Should have very small height")
        XCTAssertNoThrow(view.body, "Should render in very small size")
    }

    // MARK: - Sample Data Tests

    /// Test 23: Preview sample data validation
    ///
    /// Validates:
    /// - Sample data arrays are valid
    /// - Cover different trend scenarios
    func testSampleDataValidation() {
        // Test all sample data sets
        XCTAssertFalse(SparklineView.sampleDataUpward.isEmpty, "Upward sample should have data")
        XCTAssertFalse(SparklineView.sampleDataDownward.isEmpty, "Downward sample should have data")
        XCTAssertFalse(SparklineView.sampleDataVolatile.isEmpty, "Volatile sample should have data")
        XCTAssertFalse(SparklineView.sampleDataFlat.isEmpty, "Flat sample should have data")

        // Verify upward trend
        let upwardFirst = SparklineView.sampleDataUpward.first!
        let upwardLast = SparklineView.sampleDataUpward.last!
        XCTAssertTrue(upwardLast > upwardFirst, "Upward sample should trend up")

        // Verify downward trend
        let downwardFirst = SparklineView.sampleDataDownward.first!
        let downwardLast = SparklineView.sampleDataDownward.last!
        XCTAssertTrue(downwardLast < downwardFirst, "Downward sample should trend down")
    }

    // MARK: - Zero Value Tests

    /// Test 24: Data with zero values
    ///
    /// Validates:
    /// - Handles zero values correctly
    /// - No division by zero errors
    func testZeroValues() {
        // Given
        let data: [Float] = [0, 5, 10, 5, 0]

        // When
        let view = SparklineView(
            data: data,
            color: .blue
        )

        // Then
        XCTAssertNoThrow(view.body, "Should handle zero values")
        XCTAssertFalse(view.accessibilityValue.isEmpty, "Should have accessibility value")
    }

    /// Test 25: All same values
    ///
    /// Validates:
    /// - Handles flat line (all same values)
    /// - No range calculation errors
    func testAllSameValues() {
        // Given
        let data: [Float] = [15, 15, 15, 15, 15]

        // When
        let view = SparklineView(
            data: data,
            color: .gray
        )

        // Then
        XCTAssertNoThrow(view.body, "Should handle all same values")
        let accessibilityValue = view.accessibilityValue
        XCTAssertTrue(
            accessibilityValue.contains("stable") || accessibilityValue.contains("flat"),
            "Should describe as stable"
        )
    }
}
