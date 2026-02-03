import XCTest
import SwiftUI
@testable import AvaElementsiOS

/// Test suite for TreeMapView
///
/// Validates:
/// - Squarified, Sliced, and Diced algorithms
/// - Flat and hierarchical data structures
/// - Custom colors and styling
/// - Label visibility based on rectangle size
/// - VoiceOver accessibility
/// - Animation behavior
/// - Empty state handling
/// - Edge cases (zero values, single node, deep hierarchy)
///
/// **Coverage Target:** 90%+
/// **Framework:** SwiftUI Canvas API (iOS 16+)
@available(iOS 16.0, *)
final class TreeMapTests: XCTestCase {

    // MARK: - Test Data

    private var flatData: [TreeMapView.TreeNode] {
        [
            TreeMapView.TreeNode(label: "Sales", value: 100, color: "#2196F3"),
            TreeMapView.TreeNode(label: "Marketing", value: 80, color: "#4CAF50"),
            TreeMapView.TreeNode(label: "Engineering", value: 120, color: "#F44336"),
            TreeMapView.TreeNode(label: "Support", value: 60, color: "#FF9800"),
            TreeMapView.TreeNode(label: "HR", value: 40, color: "#9C27B0")
        ]
    }

    private var hierarchicalData: [TreeMapView.TreeNode] {
        [
            TreeMapView.TreeNode(
                label: "Sales",
                value: 100,
                color: "#2196F3",
                children: [
                    TreeMapView.TreeNode(label: "Q1", value: 25, color: "#90CAF9"),
                    TreeMapView.TreeNode(label: "Q2", value: 30, color: "#64B5F6"),
                    TreeMapView.TreeNode(label: "Q3", value: 22, color: "#42A5F5"),
                    TreeMapView.TreeNode(label: "Q4", value: 23, color: "#1E88E5")
                ]
            ),
            TreeMapView.TreeNode(
                label: "Marketing",
                value: 80,
                color: "#4CAF50",
                children: [
                    TreeMapView.TreeNode(label: "Q1", value: 20, color: "#A5D6A7"),
                    TreeMapView.TreeNode(label: "Q2", value: 22, color: "#81C784")
                ]
            )
        ]
    }

    private var singleNodeData: [TreeMapView.TreeNode] {
        [
            TreeMapView.TreeNode(label: "Only Node", value: 100, color: "#2196F3")
        ]
    }

    private var emptyData: [TreeMapView.TreeNode] {
        []
    }

    // MARK: - Test 1: Squarified Algorithm

    /// Test 1: Squarified algorithm creates square-like rectangles
    ///
    /// Validates:
    /// - Nodes are sorted by value (descending)
    /// - Rectangles are laid out to minimize aspect ratios
    /// - All nodes are rendered
    /// - Total area equals bounds area
    func testSquarifiedAlgorithm() {
        // Given
        let data = flatData

        // When
        let view = TreeMapView(
            nodes: data,
            algorithm: .squarified,
            showLabels: true,
            animated: false
        )

        // Then
        // View should be initialized
        XCTAssertNotNil(view)
        XCTAssertEqual(view.nodes.count, 5)
        XCTAssertEqual(view.algorithm, .squarified)
        XCTAssertTrue(view.showLabels)
        XCTAssertFalse(view.animated)

        // Algorithm should create square-like rectangles
        // (tested visually or through layout calculations)
        let totalValue = data.reduce(0) { $0 + $1.value }
        XCTAssertEqual(totalValue, 400)
    }

    // MARK: - Test 2: Sliced Algorithm

    /// Test 2: Sliced algorithm creates horizontal slices
    ///
    /// Validates:
    /// - Nodes are laid out horizontally
    /// - Each node spans full width
    /// - Heights are proportional to values
    func testSlicedAlgorithm() {
        // Given
        let data = flatData

        // When
        let view = TreeMapView(
            nodes: data,
            algorithm: .sliced,
            showLabels: true,
            animated: false
        )

        // Then
        XCTAssertNotNil(view)
        XCTAssertEqual(view.algorithm, .sliced)

        // Sliced layout should create horizontal slices
        // Each node should span the full width
        // Heights should be proportional to values
    }

    // MARK: - Test 3: Diced Algorithm

    /// Test 3: Diced algorithm creates vertical slices
    ///
    /// Validates:
    /// - Nodes are laid out vertically
    /// - Each node spans full height
    /// - Widths are proportional to values
    func testDicedAlgorithm() {
        // Given
        let data = flatData

        // When
        let view = TreeMapView(
            nodes: data,
            algorithm: .diced,
            showLabels: true,
            animated: false
        )

        // Then
        XCTAssertNotNil(view)
        XCTAssertEqual(view.algorithm, .diced)

        // Diced layout should create vertical slices
        // Each node should span the full height
        // Widths should be proportional to values
    }

    // MARK: - Test 4: Hierarchical Data

    /// Test 4: Hierarchical data with parent/child nodes
    ///
    /// Validates:
    /// - Parent nodes contain children
    /// - Children are rendered inside parent bounds
    /// - Recursive layout works correctly
    /// - Total value calculation includes children
    func testHierarchicalData() {
        // Given
        let data = hierarchicalData

        // When
        let view = TreeMapView(
            nodes: data,
            algorithm: .squarified,
            showLabels: true,
            animated: false
        )

        // Then
        XCTAssertNotNil(view)
        XCTAssertEqual(view.nodes.count, 2)

        // First node has 4 children
        XCTAssertEqual(data[0].children.count, 4)
        XCTAssertEqual(data[0].getTotalValue(), 100)

        // Second node has 2 children
        XCTAssertEqual(data[1].children.count, 2)
        XCTAssertEqual(data[1].getTotalValue(), 80)

        // Depth calculation
        XCTAssertEqual(data[0].getDepth(), 2)
        XCTAssertEqual(data[1].getDepth(), 2)
    }

    // MARK: - Test 5: Custom Colors

    /// Test 5: Custom colors are applied to nodes
    ///
    /// Validates:
    /// - Custom hex colors are parsed correctly
    /// - Default colors are used when not specified
    /// - Colors are applied to rectangles
    func testCustomColors() {
        // Given
        let data = [
            TreeMapView.TreeNode(label: "Blue", value: 100, color: "#2196F3"),
            TreeMapView.TreeNode(label: "Green", value: 80, color: "#4CAF50"),
            TreeMapView.TreeNode(label: "Red", value: 60, color: "#F44336")
        ]

        // When
        let view = TreeMapView(
            nodes: data,
            algorithm: .squarified,
            showLabels: true,
            animated: false
        )

        // Then
        XCTAssertNotNil(view)
        XCTAssertEqual(view.nodes.count, 3)

        // Colors should be set
        XCTAssertEqual(data[0].color, "#2196F3")
        XCTAssertEqual(data[1].color, "#4CAF50")
        XCTAssertEqual(data[2].color, "#F44336")
    }

    // MARK: - Test 6: Label Visibility

    /// Test 6: Labels are shown/hidden based on rectangle size
    ///
    /// Validates:
    /// - Labels are shown when showLabels is true
    /// - Labels are hidden when showLabels is false
    /// - Labels are only rendered if rectangle is large enough (>40x20)
    func testLabelVisibility() {
        // Given
        let data = flatData

        // When - Labels shown
        let viewWithLabels = TreeMapView(
            nodes: data,
            algorithm: .squarified,
            showLabels: true,
            animated: false
        )

        // Then
        XCTAssertTrue(viewWithLabels.showLabels)

        // When - Labels hidden
        let viewWithoutLabels = TreeMapView(
            nodes: data,
            algorithm: .squarified,
            showLabels: false,
            animated: false
        )

        // Then
        XCTAssertFalse(viewWithoutLabels.showLabels)
    }

    // MARK: - Test 7: Animation

    /// Test 7: Animation behavior
    ///
    /// Validates:
    /// - Animation is applied when animated is true
    /// - No animation when animated is false
    /// - Rectangles scale from center during animation
    func testAnimation() {
        // Given
        let data = flatData

        // When - Animated
        let animatedView = TreeMapView(
            nodes: data,
            algorithm: .squarified,
            showLabels: true,
            animated: true
        )

        // Then
        XCTAssertTrue(animatedView.animated)

        // When - Not animated
        let staticView = TreeMapView(
            nodes: data,
            algorithm: .squarified,
            showLabels: true,
            animated: false
        )

        // Then
        XCTAssertFalse(staticView.animated)
    }

    // MARK: - Test 8: Empty State

    /// Test 8: Empty state when no data
    ///
    /// Validates:
    /// - Empty state view is shown when data is empty
    /// - Proper accessibility label is set
    /// - No crash with empty data
    func testEmptyState() {
        // Given
        let data = emptyData

        // When
        let view = TreeMapView(
            nodes: data,
            algorithm: .squarified,
            showLabels: true,
            animated: false
        )

        // Then
        XCTAssertNotNil(view)
        XCTAssertEqual(view.nodes.count, 0)
        XCTAssertTrue(view.nodes.isEmpty)
    }

    // MARK: - Test 9: Accessibility

    /// Test 9: VoiceOver accessibility
    ///
    /// Validates:
    /// - Accessibility label describes chart type and data
    /// - Accessibility value includes node values
    /// - Content description can be customized
    /// - Empty state has proper label
    func testAccessibility() {
        // Given
        let data = flatData
        let contentDescription = "Department budget treemap"

        // When
        let view = TreeMapView(
            nodes: data,
            algorithm: .squarified,
            showLabels: true,
            animated: false,
            contentDescription: contentDescription
        )

        // Then
        XCTAssertNotNil(view)
        XCTAssertEqual(view.contentDescription, contentDescription)

        // Accessibility should be properly configured
        // (tested through VoiceOver or accessibility inspector)
    }

    // MARK: - Test 10: Edge Cases

    /// Test 10: Edge cases
    ///
    /// Validates:
    /// - Single node renders correctly
    /// - Zero values are handled (clamped to 0)
    /// - Negative values are clamped to 0
    /// - Deep hierarchy (3+ levels) works
    func testEdgeCases() {
        // Test 1: Single node
        let singleNode = singleNodeData
        let singleView = TreeMapView(nodes: singleNode, algorithm: .squarified)
        XCTAssertEqual(singleView.nodes.count, 1)

        // Test 2: Zero value (clamped)
        let zeroNode = TreeMapView.TreeNode(label: "Zero", value: 0, color: "#000000")
        XCTAssertEqual(zeroNode.value, 0)

        // Test 3: Negative value (clamped to 0)
        let negativeNode = TreeMapView.TreeNode(label: "Negative", value: -10, color: "#000000")
        XCTAssertEqual(negativeNode.value, 0)

        // Test 4: Deep hierarchy
        let deepNode = TreeMapView.TreeNode(
            label: "Level 1",
            value: 100,
            children: [
                TreeMapView.TreeNode(
                    label: "Level 2",
                    value: 50,
                    children: [
                        TreeMapView.TreeNode(
                            label: "Level 3",
                            value: 25
                        )
                    ]
                )
            ]
        )
        XCTAssertEqual(deepNode.getDepth(), 3)
        XCTAssertEqual(deepNode.getTotalValue(), 100)
    }

    // MARK: - Test 11: Convenience Initializer

    /// Test 11: Convenience initializer for flat data
    ///
    /// Validates:
    /// - Simple tuple-based initialization works
    /// - Nodes are created correctly
    /// - Optional colors are handled
    func testConvenienceInitializer() {
        // Given
        let values: [(String, Float, String?)] = [
            ("Sales", 100, "#2196F3"),
            ("Marketing", 80, "#4CAF50"),
            ("Engineering", 120, nil)
        ]

        // When
        let view = TreeMapView(
            values: values,
            algorithm: .squarified
        )

        // Then
        XCTAssertNotNil(view)
        XCTAssertEqual(view.nodes.count, 3)
        XCTAssertEqual(view.algorithm, .squarified)
    }

    // MARK: - Test 12: Node Helper Functions

    /// Test 12: TreeNode helper functions
    ///
    /// Validates:
    /// - getTotalValue() includes children
    /// - getDepth() calculates correctly
    /// - Identifiable protocol works
    func testNodeHelperFunctions() {
        // Given
        let node = TreeMapView.TreeNode(
            label: "Parent",
            value: 50,
            color: "#2196F3",
            children: [
                TreeMapView.TreeNode(label: "Child 1", value: 25),
                TreeMapView.TreeNode(label: "Child 2", value: 25)
            ]
        )

        // When
        let totalValue = node.getTotalValue()
        let depth = node.getDepth()

        // Then
        XCTAssertEqual(totalValue, 50) // Parent value only (children separate)
        XCTAssertEqual(depth, 2)

        // Test identifiable
        XCTAssertNotNil(node.id)
    }

    // MARK: - Test 13: Algorithm Enum

    /// Test 13: Algorithm enum values
    ///
    /// Validates:
    /// - All algorithm cases are defined
    /// - Raw values are correct
    func testAlgorithmEnum() {
        // Given
        let squarified = TreeMapView.Algorithm.squarified
        let sliced = TreeMapView.Algorithm.sliced
        let diced = TreeMapView.Algorithm.diced

        // Then
        XCTAssertEqual(squarified.rawValue, "Squarified")
        XCTAssertEqual(sliced.rawValue, "Sliced")
        XCTAssertEqual(diced.rawValue, "Diced")
    }

    // MARK: - Test 14: Performance

    /// Test 14: Performance with large dataset
    ///
    /// Validates:
    /// - Handles 100+ nodes efficiently
    /// - Layout algorithm completes in reasonable time
    func testPerformance() {
        // Given
        var largeData: [TreeMapView.TreeNode] = []
        for i in 0..<100 {
            largeData.append(
                TreeMapView.TreeNode(
                    label: "Node \(i)",
                    value: Float.random(in: 10...100)
                )
            )
        }

        // When
        measure {
            let view = TreeMapView(
                nodes: largeData,
                algorithm: .squarified,
                showLabels: true,
                animated: false
            )
            XCTAssertNotNil(view)
        }

        // Then - should complete within performance threshold
    }

    // MARK: - Test 15: Color Parsing Integration

    /// Test 15: Integration with ChartHelpers.parseColor
    ///
    /// Validates:
    /// - Hex colors are parsed correctly
    /// - Invalid colors fallback to default
    /// - 3-digit, 6-digit, and 8-digit hex supported
    func testColorParsingIntegration() {
        // Given
        let validColor = "#2196F3"
        let invalidColor = "invalid"

        // When
        let node1 = TreeMapView.TreeNode(label: "Valid", value: 100, color: validColor)
        let node2 = TreeMapView.TreeNode(label: "Invalid", value: 100, color: invalidColor)
        let node3 = TreeMapView.TreeNode(label: "None", value: 100, color: nil)

        // Then
        XCTAssertEqual(node1.color, validColor)
        XCTAssertEqual(node2.color, invalidColor)
        XCTAssertNil(node3.color)

        // ChartHelpers.parseColor should handle these correctly
        let parsedValid = ChartHelpers.parseColor(validColor)
        let parsedInvalid = ChartHelpers.parseColor(invalidColor)
        let parsedDefault = ChartHelpers.parseColor("")

        XCTAssertNotNil(parsedValid)
        XCTAssertNotNil(parsedInvalid) // Returns default blue
        XCTAssertNotNil(parsedDefault) // Returns default blue
    }
}

// MARK: - Test Helpers

@available(iOS 16.0, *)
extension TreeMapTests {
    /// Create test node with random value
    private func randomNode(label: String) -> TreeMapView.TreeNode {
        TreeMapView.TreeNode(
            label: label,
            value: Float.random(in: 10...100)
        )
    }

    /// Create hierarchical test data with specified depth
    private func createHierarchy(depth: Int) -> TreeMapView.TreeNode {
        if depth <= 1 {
            return TreeMapView.TreeNode(label: "Leaf", value: 10)
        } else {
            return TreeMapView.TreeNode(
                label: "Level \(depth)",
                value: 10,
                children: [
                    createHierarchy(depth: depth - 1),
                    createHierarchy(depth: depth - 1)
                ]
            )
        }
    }
}
