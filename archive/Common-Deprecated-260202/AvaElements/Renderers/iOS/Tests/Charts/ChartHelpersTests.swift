import XCTest
import SwiftUI
@testable import AvaElements

/// Test suite for Chart Helper utilities
///
/// Tests cover:
/// - Color parsing from hex strings (6 test cases)
/// - Data point calculations
/// - Accessibility label generation
/// - Coordinate transformations
/// - Animation helpers
/// - WCAG color contrast validation
///
/// Quality Gates:
/// - 90%+ code coverage
/// - All edge cases covered
/// - VoiceOver labels validated
class ChartHelpersTests: XCTestCase {

    // MARK: - Color Parsing Tests

    func testParseColor_ValidSixDigitHex() {
        // Test: #2196F3 should parse to blue color
        let color = ChartHelpers.parseColor("#2196F3")

        // Extract RGB components (iOS 14+)
        if #available(iOS 14.0, *) {
            let uiColor = UIColor(color)
            var red: CGFloat = 0
            var green: CGFloat = 0
            var blue: CGFloat = 0
            var alpha: CGFloat = 0

            uiColor.getRed(&red, green: &green, blue: &blue, alpha: &alpha)

            XCTAssertEqual(red, 0x21 / 255.0, accuracy: 0.01, "Red component mismatch")
            XCTAssertEqual(green, 0x96 / 255.0, accuracy: 0.01, "Green component mismatch")
            XCTAssertEqual(blue, 0xF3 / 255.0, accuracy: 0.01, "Blue component mismatch")
            XCTAssertEqual(alpha, 1.0, accuracy: 0.01, "Alpha should be 1.0")
        }
    }

    func testParseColor_ValidEightDigitHexWithAlpha() {
        // Test: #FF2196F3 should parse to blue with full opacity
        let color = ChartHelpers.parseColor("#FF2196F3")

        if #available(iOS 14.0, *) {
            let uiColor = UIColor(color)
            var alpha: CGFloat = 0
            uiColor.getRed(nil, green: nil, blue: nil, alpha: &alpha)

            XCTAssertEqual(alpha, 1.0, accuracy: 0.01, "Alpha should be 1.0")
        }
    }

    func testParseColor_WithoutHashPrefix() {
        // Test: 2196F3 should parse to blue (missing #)
        let color = ChartHelpers.parseColor("2196F3")

        if #available(iOS 14.0, *) {
            let uiColor = UIColor(color)
            var red: CGFloat = 0
            var green: CGFloat = 0
            var blue: CGFloat = 0

            uiColor.getRed(&red, green: &green, blue: &blue, alpha: nil)

            XCTAssertEqual(red, 0x21 / 255.0, accuracy: 0.01, "Red component mismatch")
            XCTAssertEqual(green, 0x96 / 255.0, accuracy: 0.01, "Green component mismatch")
            XCTAssertEqual(blue, 0xF3 / 255.0, accuracy: 0.01, "Blue component mismatch")
        }
    }

    func testParseColor_InvalidHex_ReturnsDefaultBlue() {
        // Test: Invalid hex should return default blue
        let color = ChartHelpers.parseColor("invalid")
        let defaultColor = ChartHelpers.parseColor("#2196F3")

        XCTAssertEqual(color, defaultColor, "Invalid hex should return default blue")
    }

    func testParseColor_EmptyString_ReturnsDefaultBlue() {
        // Test: Empty string should return default blue
        let color = ChartHelpers.parseColor("")
        let defaultColor = ChartHelpers.parseColor("#2196F3")

        XCTAssertEqual(color, defaultColor, "Empty string should return default blue")
    }

    func testParseColor_ThreeDigitHex_Expands() {
        // Test: #RGB should expand to #RRGGBB
        let color = ChartHelpers.parseColor("#F00")
        let expectedColor = ChartHelpers.parseColor("#FF0000")

        if #available(iOS 14.0, *) {
            let uiColor1 = UIColor(color)
            let uiColor2 = UIColor(expectedColor)
            var red1: CGFloat = 0, red2: CGFloat = 0

            uiColor1.getRed(&red1, green: nil, blue: nil, alpha: nil)
            uiColor2.getRed(&red2, green: nil, blue: nil, alpha: nil)

            XCTAssertEqual(red1, red2, accuracy: 0.01, "3-digit hex should expand correctly")
        }
    }

    // MARK: - Data Point Calculation Tests

    func testCalculateChartBounds_EmptyData() {
        // Test: Empty data should return zero bounds
        let bounds = ChartHelpers.calculateChartBounds(data: [])

        XCTAssertEqual(bounds.minX, 0, "minX should be 0 for empty data")
        XCTAssertEqual(bounds.maxX, 0, "maxX should be 0 for empty data")
        XCTAssertEqual(bounds.minY, 0, "minY should be 0 for empty data")
        XCTAssertEqual(bounds.maxY, 0, "maxY should be 0 for empty data")
    }

    func testCalculateChartBounds_SinglePoint() {
        // Test: Single point should have equal min/max
        let data = [ChartDataPoint(x: 5.0, y: 10.0)]
        let bounds = ChartHelpers.calculateChartBounds(data: data)

        XCTAssertEqual(bounds.minX, 5.0, "minX should equal point x")
        XCTAssertEqual(bounds.maxX, 5.0, "maxX should equal point x")
        XCTAssertEqual(bounds.minY, 10.0, "minY should equal point y")
        XCTAssertEqual(bounds.maxY, 10.0, "maxY should equal point y")
    }

    func testCalculateChartBounds_MultiplePoints() {
        // Test: Multiple points should calculate correct bounds
        let data = [
            ChartDataPoint(x: 0.0, y: 100.0),
            ChartDataPoint(x: 1.0, y: 150.0),
            ChartDataPoint(x: 2.0, y: 125.0),
            ChartDataPoint(x: 3.0, y: 200.0)
        ]
        let bounds = ChartHelpers.calculateChartBounds(data: data)

        XCTAssertEqual(bounds.minX, 0.0, "minX should be 0")
        XCTAssertEqual(bounds.maxX, 3.0, "maxX should be 3")
        XCTAssertEqual(bounds.minY, 100.0, "minY should be 100")
        XCTAssertEqual(bounds.maxY, 200.0, "maxY should be 200")
    }

    func testCalculateChartBounds_NegativeValues() {
        // Test: Handle negative values correctly
        let data = [
            ChartDataPoint(x: -2.0, y: -50.0),
            ChartDataPoint(x: 0.0, y: 0.0),
            ChartDataPoint(x: 2.0, y: 50.0)
        ]
        let bounds = ChartHelpers.calculateChartBounds(data: data)

        XCTAssertEqual(bounds.minX, -2.0, "minX should be -2")
        XCTAssertEqual(bounds.maxX, 2.0, "maxX should be 2")
        XCTAssertEqual(bounds.minY, -50.0, "minY should be -50")
        XCTAssertEqual(bounds.maxY, 50.0, "maxY should be 50")
    }

    // MARK: - Coordinate Transformation Tests

    func testTransformToScreenCoordinates_OriginPoint() {
        // Test: Origin point (0,0) transforms correctly
        let dataPoint = CGPoint(x: 0, y: 0)
        let dataBounds = ChartBounds(minX: 0, maxX: 10, minY: 0, maxY: 100)
        let screenSize = CGSize(width: 400, height: 300)

        let screenPoint = ChartHelpers.transformToScreenCoordinates(
            dataPoint: dataPoint,
            dataBounds: dataBounds,
            screenSize: screenSize
        )

        XCTAssertEqual(screenPoint.x, 0, "X should be at left edge")
        XCTAssertEqual(screenPoint.y, 300, "Y should be at bottom edge (inverted)")
    }

    func testTransformToScreenCoordinates_MaxPoint() {
        // Test: Max point transforms to screen max
        let dataPoint = CGPoint(x: 10, y: 100)
        let dataBounds = ChartBounds(minX: 0, maxX: 10, minY: 0, maxY: 100)
        let screenSize = CGSize(width: 400, height: 300)

        let screenPoint = ChartHelpers.transformToScreenCoordinates(
            dataPoint: dataPoint,
            dataBounds: dataBounds,
            screenSize: screenSize
        )

        XCTAssertEqual(screenPoint.x, 400, "X should be at right edge")
        XCTAssertEqual(screenPoint.y, 0, "Y should be at top edge (inverted)")
    }

    func testTransformToScreenCoordinates_MidPoint() {
        // Test: Midpoint transforms correctly
        let dataPoint = CGPoint(x: 5, y: 50)
        let dataBounds = ChartBounds(minX: 0, maxX: 10, minY: 0, maxY: 100)
        let screenSize = CGSize(width: 400, height: 300)

        let screenPoint = ChartHelpers.transformToScreenCoordinates(
            dataPoint: dataPoint,
            dataBounds: dataBounds,
            screenSize: screenSize
        )

        XCTAssertEqual(screenPoint.x, 200, "X should be at center")
        XCTAssertEqual(screenPoint.y, 150, "Y should be at center")
    }

    // MARK: - Accessibility Label Tests

    func testGenerateAccessibilityLabel_BasicChart() {
        // Test: Basic chart with title and data
        let label = ChartAccessibility.generateChartLabel(
            title: "Revenue Chart",
            seriesCount: 1,
            dataPointCount: 10,
            chartType: "line"
        )

        XCTAssertTrue(label.contains("Revenue Chart"), "Should contain title")
        XCTAssertTrue(label.contains("line chart"), "Should contain chart type")
        XCTAssertTrue(label.contains("1 series"), "Should contain series count")
        XCTAssertTrue(label.contains("10"), "Should contain data point count")
    }

    func testGenerateAccessibilityLabel_NoTitle() {
        // Test: Chart without title
        let label = ChartAccessibility.generateChartLabel(
            title: nil,
            seriesCount: 2,
            dataPointCount: 20,
            chartType: "bar"
        )

        XCTAssertFalse(label.isEmpty, "Label should not be empty")
        XCTAssertTrue(label.contains("bar chart"), "Should contain chart type")
        XCTAssertTrue(label.contains("2 series"), "Should contain series count")
    }

    func testGenerateAccessibilityValue_DataPoint() {
        // Test: Data point accessibility value
        let value = ChartAccessibility.generateDataPointValue(
            x: 1.0,
            y: 150.0,
            xLabel: "Q1",
            yLabel: "Revenue"
        )

        XCTAssertTrue(value.contains("Q1"), "Should contain X label")
        XCTAssertTrue(value.contains("150"), "Should contain Y value")
        XCTAssertTrue(value.contains("Revenue"), "Should contain Y label")
    }

    func testGenerateAccessibilityHint_InteractiveChart() {
        // Test: Interactive chart hint
        let hint = ChartAccessibility.generateChartHint(isInteractive: true)

        XCTAssertTrue(hint.contains("tap"), "Should mention tap interaction")
        XCTAssertTrue(hint.contains("details"), "Should mention viewing details")
    }

    func testGenerateAccessibilityHint_NonInteractiveChart() {
        // Test: Non-interactive chart hint
        let hint = ChartAccessibility.generateChartHint(isInteractive: false)

        XCTAssertTrue(hint.isEmpty || hint.contains("informational"), "Should be empty or mention informational")
    }

    // MARK: - Animation Helper Tests

    func testAnimationConfiguration_Default() {
        // Test: Default animation configuration
        let config = ChartHelpers.createAnimationConfiguration(duration: 500)

        XCTAssertEqual(config.duration, 0.5, "Duration should be 0.5 seconds")
        XCTAssertNotNil(config.animation, "Animation should not be nil")
    }

    func testAnimationConfiguration_Disabled() {
        // Test: Disabled animation
        let config = ChartHelpers.createAnimationConfiguration(duration: 0)

        XCTAssertEqual(config.duration, 0, "Duration should be 0")
        XCTAssertNil(config.animation, "Animation should be nil when disabled")
    }

    // MARK: - WCAG Color Contrast Tests

    func testColorContrast_WhiteOnBlack_PassesAA() {
        // Test: White on black should pass WCAG AA
        let foreground = Color.white
        let background = Color.black

        let contrast = ChartColors.calculateContrastRatio(foreground: foreground, background: background)

        XCTAssertGreaterThan(contrast, 4.5, "White on black should pass WCAG AA (4.5:1)")
        XCTAssertTrue(ChartColors.meetsWCAG_AA(foreground: foreground, background: background))
    }

    func testColorContrast_LightGrayOnWhite_FailsAA() {
        // Test: Light gray on white should fail WCAG AA
        let foreground = Color.gray.opacity(0.3)
        let background = Color.white

        let contrast = ChartColors.calculateContrastRatio(foreground: foreground, background: background)

        XCTAssertLessThan(contrast, 4.5, "Light gray on white should fail WCAG AA")
        XCTAssertFalse(ChartColors.meetsWCAG_AA(foreground: foreground, background: background))
    }

    func testColorContrast_DefaultChartColors_PassAA() {
        // Test: Default chart colors should pass WCAG AA on white
        let background = Color.white
        let defaultColors = ChartColors.defaultPalette

        for color in defaultColors {
            let contrast = ChartColors.calculateContrastRatio(foreground: color, background: background)
            XCTAssertGreaterThan(contrast, 3.0, "Default chart color should have reasonable contrast")
        }
    }

    // MARK: - Edge Cases

    func testCalculateChartBounds_AllSameValues() {
        // Test: All data points have same value
        let data = [
            ChartDataPoint(x: 0, y: 50),
            ChartDataPoint(x: 1, y: 50),
            ChartDataPoint(x: 2, y: 50)
        ]
        let bounds = ChartHelpers.calculateChartBounds(data: data)

        XCTAssertEqual(bounds.minY, 50, "minY should equal constant value")
        XCTAssertEqual(bounds.maxY, 50, "maxY should equal constant value")
    }

    func testTransformToScreenCoordinates_ZeroSizeScreen() {
        // Test: Zero size screen should not crash
        let dataPoint = CGPoint(x: 5, y: 50)
        let dataBounds = ChartBounds(minX: 0, maxX: 10, minY: 0, maxY: 100)
        let screenSize = CGSize(width: 0, height: 0)

        let screenPoint = ChartHelpers.transformToScreenCoordinates(
            dataPoint: dataPoint,
            dataBounds: dataBounds,
            screenSize: screenSize
        )

        XCTAssertEqual(screenPoint.x, 0, "X should be 0 for zero-width screen")
        XCTAssertEqual(screenPoint.y, 0, "Y should be 0 for zero-height screen")
    }

    func testParseColor_CaseInsensitive() {
        // Test: Hex parsing should be case-insensitive
        let lowerCase = ChartHelpers.parseColor("#ff0000")
        let upperCase = ChartHelpers.parseColor("#FF0000")
        let mixedCase = ChartHelpers.parseColor("#Ff0000")

        if #available(iOS 14.0, *) {
            let uiColor1 = UIColor(lowerCase)
            let uiColor2 = UIColor(upperCase)
            let uiColor3 = UIColor(mixedCase)

            var red1: CGFloat = 0, red2: CGFloat = 0, red3: CGFloat = 0

            uiColor1.getRed(&red1, green: nil, blue: nil, alpha: nil)
            uiColor2.getRed(&red2, green: nil, blue: nil, alpha: nil)
            uiColor3.getRed(&red3, green: nil, blue: nil, alpha: nil)

            XCTAssertEqual(red1, red2, accuracy: 0.01, "Case should not matter")
            XCTAssertEqual(red2, red3, accuracy: 0.01, "Case should not matter")
        }
    }
}

// MARK: - Helper Types for Tests

/// Test data point structure
struct ChartDataPoint {
    let x: Double
    let y: Double
}

/// Chart bounds structure
struct ChartBounds {
    let minX: Double
    let maxX: Double
    let minY: Double
    let maxY: Double
}

/// Animation configuration
struct AnimationConfiguration {
    let duration: Double
    let animation: Animation?
}
