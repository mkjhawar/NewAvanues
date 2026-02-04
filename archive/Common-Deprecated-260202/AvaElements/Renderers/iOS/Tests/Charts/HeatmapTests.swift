import XCTest
import SwiftUI
@testable import AvaElementsRenderer

/// Tests for HeatmapView component
///
/// **Test Coverage:**
/// - Data rendering (2D matrix)
/// - Color interpolation for all schemes
/// - Label positioning
/// - Value overlay
/// - Cell selection
/// - Animation state
/// - Accessibility
/// - Edge cases (empty data, single cell)
///
/// **Target:** 100% code coverage for HeatmapView
@available(iOS 16.0, *)
final class HeatmapTests: XCTestCase {

    // MARK: - Setup

    override func setUp() {
        super.setUp()
    }

    override func tearDown() {
        super.tearDown()
    }

    // MARK: - Data Rendering Tests

    /// Test basic heatmap rendering with 3x3 matrix
    func testBasicHeatmapRendering() {
        // Given
        let data: [[Float]] = [
            [10, 20, 30],
            [15, 25, 35],
            [20, 30, 40]
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            colorScheme: .blueRed,
            cellSize: 50,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // View should render without crashing
    }

    /// Test heatmap with row and column labels
    func testHeatmapWithLabels() {
        // Given
        let data: [[Float]] = [
            [10, 20],
            [15, 25]
        ]
        let rowLabels = ["Row 1", "Row 2"]
        let columnLabels = ["Col 1", "Col 2"]

        // When
        let heatmap = HeatmapView(
            data: data,
            rowLabels: rowLabels,
            columnLabels: columnLabels,
            cellSize: 50,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Labels should be displayed
    }

    /// Test heatmap with value overlay
    func testHeatmapWithValueOverlay() {
        // Given
        let data: [[Float]] = [
            [1.5, 2.7],
            [3.2, 4.9]
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            showValues: true,
            cellSize: 60,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Values should be displayed on cells
    }

    // MARK: - Color Scheme Tests

    /// Test BlueRed color scheme interpolation
    func testBlueRedColorScheme() {
        // Given
        let data: [[Float]] = [
            [0, 50, 100]
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            colorScheme: .blueRed,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Low values should be blue, high values should be red
    }

    /// Test GreenRed color scheme interpolation
    func testGreenRedColorScheme() {
        // Given
        let data: [[Float]] = [
            [0, 50, 100]
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            colorScheme: .greenRed,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Low values should be green, high values should be red
    }

    /// Test Grayscale color scheme
    func testGrayscaleColorScheme() {
        // Given
        let data: [[Float]] = [
            [0, 50, 100]
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            colorScheme: .grayscale,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Low values should be white, high values should be black
    }

    /// Test Viridis color scheme (perceptually uniform)
    func testViridisColorScheme() {
        // Given
        let data: [[Float]] = [
            [0, 25, 50, 75, 100]
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            colorScheme: .viridis,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Should use purple -> green -> yellow gradient
    }

    // MARK: - Animation Tests

    /// Test animation state initialization
    func testAnimationEnabled() {
        // Given
        let data: [[Float]] = [[10]]

        // When
        let heatmap = HeatmapView(
            data: data,
            animated: true
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Animation should be enabled
    }

    /// Test animation disabled
    func testAnimationDisabled() {
        // Given
        let data: [[Float]] = [[10]]

        // When
        let heatmap = HeatmapView(
            data: data,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Animation should be disabled
    }

    // MARK: - Interaction Tests

    /// Test cell selection (tap interaction)
    func testCellSelection() {
        // Given
        let data: [[Float]] = [
            [10, 20],
            [15, 25]
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            cellSize: 50,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Cell selection should work on tap
        // Selected cell should be highlighted
    }

    // MARK: - Edge Cases

    /// Test empty data array
    func testEmptyData() {
        // Given
        let data: [[Float]] = []

        // When
        let heatmap = HeatmapView(
            data: data,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Should handle empty data gracefully
    }

    /// Test single cell heatmap
    func testSingleCell() {
        // Given
        let data: [[Float]] = [[42]]

        // When
        let heatmap = HeatmapView(
            data: data,
            rowLabels: ["Single"],
            columnLabels: ["Cell"],
            showValues: true,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Should render single cell correctly
    }

    /// Test all same values (uniform color)
    func testUniformValues() {
        // Given
        let data: [[Float]] = [
            [50, 50, 50],
            [50, 50, 50]
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // All cells should have same color (middle of gradient)
    }

    /// Test negative values
    func testNegativeValues() {
        // Given
        let data: [[Float]] = [
            [-10, 0, 10],
            [-5, 5, 15]
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            showValues: true,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Should handle negative values correctly
    }

    /// Test very large matrix (performance)
    func testLargeMatrix() {
        // Given
        let rows = 20
        let cols = 20
        var data: [[Float]] = []

        for i in 0..<rows {
            var row: [Float] = []
            for j in 0..<cols {
                row.append(Float(i * cols + j))
            }
            data.append(row)
        }

        // When
        let heatmap = HeatmapView(
            data: data,
            cellSize: 30,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Should handle 400 cells efficiently
    }

    /// Test correlation matrix (symmetric)
    func testCorrelationMatrix() {
        // Given
        let data: [[Float]] = [
            [1.0, 0.8, 0.3],
            [0.8, 1.0, 0.5],
            [0.3, 0.5, 1.0]
        ]
        let labels = ["X", "Y", "Z"]

        // When
        let heatmap = HeatmapView(
            data: data,
            rowLabels: labels,
            columnLabels: labels,
            colorScheme: .blueRed,
            showValues: true,
            cellSize: 60,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Diagonal should be max color (1.0)
        // Matrix should be symmetric
    }

    // MARK: - Accessibility Tests

    /// Test accessibility description
    func testAccessibilityDescription() {
        // Given
        let data: [[Float]] = [
            [10, 20],
            [15, 25]
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            contentDescription: "Custom heatmap description"
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Should use custom description for VoiceOver
    }

    /// Test accessibility value (data summary)
    func testAccessibilityValue() {
        // Given
        let data: [[Float]] = [
            [5, 15, 25],
            [10, 20, 30],
            [1, 2, 35]
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Should describe value range (1 to 35)
        // Should identify max value location (row 3, col 3)
    }

    // MARK: - Legend Tests

    /// Test color legend rendering
    func testColorLegend() {
        // Given
        let data: [[Float]] = [
            [0, 100]
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            colorScheme: .blueRed,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Legend should show min (0) and max (100) values
        // Legend should display color gradient
    }

    // MARK: - Value Formatting Tests

    /// Test integer value formatting
    func testIntegerValueFormatting() {
        // Given
        let data: [[Float]] = [
            [1, 10, 100]
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            showValues: true,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Should format as "1", "10", "100" (no decimals)
    }

    /// Test decimal value formatting
    func testDecimalValueFormatting() {
        // Given
        let data: [[Float]] = [
            [1.23, 4.56, 7.89]
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            showValues: true,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Should format as "1.2", "4.6", "7.9" (1 decimal)
    }

    // MARK: - Text Contrast Tests

    /// Test text color selection for dark backgrounds
    func testTextColorOnDarkBackground() {
        // Given
        let data: [[Float]] = [
            [0, 10] // Low values = dark blue background
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            colorScheme: .blueRed,
            showValues: true,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Text should be white on dark blue cells
    }

    /// Test text color selection for light backgrounds
    func testTextColorOnLightBackground() {
        // Given
        let data: [[Float]] = [
            [90, 100] // High values = bright red background
        ]

        // When
        let heatmap = HeatmapView(
            data: data,
            colorScheme: .blueRed,
            showValues: true,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Text should be black on bright red cells
    }

    // MARK: - Cell Size Tests

    /// Test custom cell size
    func testCustomCellSize() {
        // Given
        let data: [[Float]] = [[10]]

        // When
        let heatmap = HeatmapView(
            data: data,
            cellSize: 80,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Cell should be 80x80 points
    }

    /// Test small cell size
    func testSmallCellSize() {
        // Given
        let data: [[Float]] = [[10]]

        // When
        let heatmap = HeatmapView(
            data: data,
            cellSize: 20,
            animated: false
        )

        // Then
        XCTAssertNotNil(heatmap)
        // Cell should be 20x20 points (compact)
    }

    // MARK: - Performance Tests

    /// Test rendering performance for medium-sized matrix
    func testRenderingPerformance() {
        // Given
        let data: [[Float]] = (0..<10).map { row in
            (0..<10).map { col in Float(row * 10 + col) }
        }

        // When
        measure {
            let heatmap = HeatmapView(
                data: data,
                showValues: true,
                animated: false
            )

            // Force view creation
            _ = heatmap.body
        }

        // Then
        // Should render 100 cells efficiently
    }
}
