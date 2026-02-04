import XCTest
import SwiftUI
@testable import AvaElementsRenderer

/**
 * Unit tests for Layout components.
 *
 * Tests 8 layout components:
 * - Row (HStack)
 * - Column (VStack)
 * - Stack (ZStack)
 * - Container
 * - Center
 * - Padding
 * - SizedBox
 * - Expanded/Flexible
 *
 * Coverage:
 * - Component initialization
 * - Property mapping
 * - Child rendering
 * - Alignment and spacing
 * - Edge cases
 *
 * @since 1.0.0 (iOS Testing Framework)
 */
class LayoutComponentTests: XCTestCase {

    // MARK: - Row Tests (HStack)

    func testRow_defaultInitialization() {
        let row = AMRow(children: [])

        XCTAssertNotNil(row, "Row should initialize")
        XCTAssertEqual(row.children.count, 0, "Row should have no children")
        XCTAssertEqual(row.mainAxisAlignment, .start, "Default main axis alignment should be start")
        XCTAssertEqual(row.crossAxisAlignment, .center, "Default cross axis alignment should be center")
    }

    func testRow_withChildren() {
        let child1 = AMText(text: "Child 1")
        let child2 = AMText(text: "Child 2")
        let row = AMRow(children: [child1, child2])

        XCTAssertEqual(row.children.count, 2, "Row should have 2 children")
    }

    func testRow_mainAxisAlignment() {
        let alignments: [MainAxisAlignment] = [.start, .end, .center, .spaceBetween, .spaceAround, .spaceEvenly]

        for alignment in alignments {
            let row = AMRow(
                mainAxisAlignment: alignment,
                children: []
            )

            XCTAssertEqual(row.mainAxisAlignment, alignment, "Row should have correct main axis alignment")
        }
    }

    func testRow_crossAxisAlignment() {
        let alignments: [CrossAxisAlignment] = [.start, .end, .center, .stretch, .baseline]

        for alignment in alignments {
            let row = AMRow(
                crossAxisAlignment: alignment,
                children: []
            )

            XCTAssertEqual(row.crossAxisAlignment, alignment, "Row should have correct cross axis alignment")
        }
    }

    func testRow_spacing() {
        let row = AMRow(
            mainAxisSize: .max,
            mainAxisAlignment: .start,
            crossAxisAlignment: .center,
            children: []
        )

        XCTAssertEqual(row.mainAxisSize, .max, "Row should have max main axis size")
    }

    // MARK: - Column Tests (VStack)

    func testColumn_defaultInitialization() {
        let column = AMColumn(children: [])

        XCTAssertNotNil(column, "Column should initialize")
        XCTAssertEqual(column.children.count, 0, "Column should have no children")
        XCTAssertEqual(column.mainAxisAlignment, .start, "Default main axis alignment should be start")
        XCTAssertEqual(column.crossAxisAlignment, .center, "Default cross axis alignment should be center")
    }

    func testColumn_withChildren() {
        let child1 = AMText(text: "Child 1")
        let child2 = AMText(text: "Child 2")
        let child3 = AMText(text: "Child 3")
        let column = AMColumn(children: [child1, child2, child3])

        XCTAssertEqual(column.children.count, 3, "Column should have 3 children")
    }

    func testColumn_mainAxisAlignment() {
        let alignments: [MainAxisAlignment] = [.start, .end, .center, .spaceBetween, .spaceAround, .spaceEvenly]

        for alignment in alignments {
            let column = AMColumn(
                mainAxisAlignment: alignment,
                children: []
            )

            XCTAssertEqual(column.mainAxisAlignment, alignment, "Column should have correct main axis alignment")
        }
    }

    func testColumn_crossAxisAlignment() {
        let alignments: [CrossAxisAlignment] = [.start, .end, .center, .stretch]

        for alignment in alignments {
            let column = AMColumn(
                crossAxisAlignment: alignment,
                children: []
            )

            XCTAssertEqual(column.crossAxisAlignment, alignment, "Column should have correct cross axis alignment")
        }
    }

    // MARK: - Stack Tests (ZStack)

    func testStack_defaultInitialization() {
        let stack = AMStack(children: [])

        XCTAssertNotNil(stack, "Stack should initialize")
        XCTAssertEqual(stack.children.count, 0, "Stack should have no children")
        XCTAssertEqual(stack.alignment, .center, "Default alignment should be center")
    }

    func testStack_withChildren() {
        let child1 = AMContainer(width: 100, height: 100, color: .blue)
        let child2 = AMContainer(width: 50, height: 50, color: .red)
        let stack = AMStack(children: [child1, child2])

        XCTAssertEqual(stack.children.count, 2, "Stack should have 2 children")
    }

    func testStack_alignment() {
        let alignments: [Alignment] = [.topLeading, .top, .topTrailing, .leading, .center, .trailing, .bottomLeading, .bottom, .bottomTrailing]

        for alignment in alignments {
            let stack = AMStack(
                alignment: alignment,
                children: []
            )

            XCTAssertEqual(stack.alignment, alignment, "Stack should have correct alignment")
        }
    }

    // MARK: - Container Tests

    func testContainer_defaultInitialization() {
        let container = AMContainer()

        XCTAssertNotNil(container, "Container should initialize")
        XCTAssertNil(container.width, "Default width should be nil")
        XCTAssertNil(container.height, "Default height should be nil")
    }

    func testContainer_withDimensions() {
        let container = AMContainer(width: 200, height: 150)

        XCTAssertEqual(container.width, 200, "Container width should be 200")
        XCTAssertEqual(container.height, 150, "Container height should be 150")
    }

    func testContainer_withColor() {
        let container = AMContainer(color: .blue)

        XCTAssertNotNil(container.color, "Container should have color")
    }

    func testContainer_withPadding() {
        let container = AMContainer(padding: EdgeInsets(top: 10, leading: 20, bottom: 10, trailing: 20))

        XCTAssertNotNil(container.padding, "Container should have padding")
        XCTAssertEqual(container.padding?.top, 10, "Top padding should be 10")
        XCTAssertEqual(container.padding?.leading, 20, "Leading padding should be 20")
    }

    func testContainer_withChild() {
        let child = AMText(text: "Child")
        let container = AMContainer(child: child)

        XCTAssertNotNil(container.child, "Container should have child")
    }

    func testContainer_withBorder() {
        let container = AMContainer(
            borderWidth: 2,
            borderColor: .red,
            borderRadius: 8
        )

        XCTAssertEqual(container.borderWidth, 2, "Border width should be 2")
        XCTAssertNotNil(container.borderColor, "Border color should not be nil")
        XCTAssertEqual(container.borderRadius, 8, "Border radius should be 8")
    }

    // MARK: - Center Tests

    func testCenter_defaultInitialization() {
        let center = AMCenter()

        XCTAssertNotNil(center, "Center should initialize")
        XCTAssertNil(center.child, "Default child should be nil")
    }

    func testCenter_withChild() {
        let child = AMText(text: "Centered")
        let center = AMCenter(child: child)

        XCTAssertNotNil(center.child, "Center should have child")
    }

    // MARK: - Padding Tests

    func testPadding_uniformPadding() {
        let child = AMText(text: "Padded")
        let padding = AMPadding(padding: 16, child: child)

        XCTAssertEqual(padding.padding, 16, "Padding should be 16")
        XCTAssertNotNil(padding.child, "Padding should have child")
    }

    func testPadding_asymmetricPadding() {
        let child = AMText(text: "Padded")
        let edgeInsets = EdgeInsets(top: 10, leading: 20, bottom: 30, trailing: 40)
        let padding = AMPadding(edgeInsets: edgeInsets, child: child)

        XCTAssertEqual(padding.edgeInsets?.top, 10, "Top padding should be 10")
        XCTAssertEqual(padding.edgeInsets?.leading, 20, "Leading padding should be 20")
        XCTAssertEqual(padding.edgeInsets?.bottom, 30, "Bottom padding should be 30")
        XCTAssertEqual(padding.edgeInsets?.trailing, 40, "Trailing padding should be 40")
    }

    func testPadding_horizontalVertical() {
        let child = AMText(text: "Padded")
        let padding = AMPadding(horizontal: 16, vertical: 8, child: child)

        XCTAssertEqual(padding.horizontal, 16, "Horizontal padding should be 16")
        XCTAssertEqual(padding.vertical, 8, "Vertical padding should be 8")
    }

    // MARK: - SizedBox Tests

    func testSizedBox_defaultInitialization() {
        let sizedBox = AMSizedBox()

        XCTAssertNotNil(sizedBox, "SizedBox should initialize")
        XCTAssertNil(sizedBox.width, "Default width should be nil")
        XCTAssertNil(sizedBox.height, "Default height should be nil")
    }

    func testSizedBox_withDimensions() {
        let sizedBox = AMSizedBox(width: 100, height: 100)

        XCTAssertEqual(sizedBox.width, 100, "Width should be 100")
        XCTAssertEqual(sizedBox.height, 100, "Height should be 100")
    }

    func testSizedBox_withChild() {
        let child = AMText(text: "Sized")
        let sizedBox = AMSizedBox(width: 200, height: 150, child: child)

        XCTAssertNotNil(sizedBox.child, "SizedBox should have child")
        XCTAssertEqual(sizedBox.width, 200, "Width should be 200")
        XCTAssertEqual(sizedBox.height, 150, "Height should be 150")
    }

    func testSizedBox_expand() {
        let sizedBox = AMSizedBox.expand()

        XCTAssertEqual(sizedBox.width, .infinity, "Width should be infinity")
        XCTAssertEqual(sizedBox.height, .infinity, "Height should be infinity")
    }

    func testSizedBox_shrink() {
        let sizedBox = AMSizedBox.shrink()

        XCTAssertEqual(sizedBox.width, 0, "Width should be 0")
        XCTAssertEqual(sizedBox.height, 0, "Height should be 0")
    }

    func testSizedBox_square() {
        let sizedBox = AMSizedBox.square(dimension: 50)

        XCTAssertEqual(sizedBox.width, 50, "Width should be 50")
        XCTAssertEqual(sizedBox.height, 50, "Height should be 50")
    }

    // MARK: - Expanded/Flexible Tests

    func testExpanded_defaultInitialization() {
        let child = AMText(text: "Expanded")
        let expanded = AMExpanded(child: child)

        XCTAssertNotNil(expanded, "Expanded should initialize")
        XCTAssertEqual(expanded.flex, 1, "Default flex should be 1")
    }

    func testExpanded_withFlex() {
        let child = AMText(text: "Expanded")
        let expanded = AMExpanded(flex: 2, child: child)

        XCTAssertEqual(expanded.flex, 2, "Flex should be 2")
        XCTAssertNotNil(expanded.child, "Expanded should have child")
    }

    func testFlexible_defaultInitialization() {
        let child = AMText(text: "Flexible")
        let flexible = AMFlexible(child: child)

        XCTAssertNotNil(flexible, "Flexible should initialize")
        XCTAssertEqual(flexible.flex, 1, "Default flex should be 1")
        XCTAssertEqual(flexible.fit, .loose, "Default fit should be loose")
    }

    func testFlexible_withFlex() {
        let child = AMText(text: "Flexible")
        let flexible = AMFlexible(flex: 3, fit: .tight, child: child)

        XCTAssertEqual(flexible.flex, 3, "Flex should be 3")
        XCTAssertEqual(flexible.fit, .tight, "Fit should be tight")
    }

    // MARK: - Edge Cases

    func testRow_emptyChildren() {
        let row = AMRow(children: [])

        XCTAssertEqual(row.children.count, 0, "Row with empty children should have count 0")
    }

    func testColumn_singleChild() {
        let child = AMText(text: "Single")
        let column = AMColumn(children: [child])

        XCTAssertEqual(column.children.count, 1, "Column should have exactly 1 child")
    }

    func testStack_manyChildren() {
        let children = (1...10).map { AMText(text: "Child \($0)") }
        let stack = AMStack(children: children)

        XCTAssertEqual(stack.children.count, 10, "Stack should have 10 children")
    }

    func testContainer_nilValues() {
        let container = AMContainer(
            width: nil,
            height: nil,
            color: nil,
            padding: nil,
            child: nil
        )

        XCTAssertNil(container.width, "Width should be nil")
        XCTAssertNil(container.height, "Height should be nil")
        XCTAssertNil(container.color, "Color should be nil")
        XCTAssertNil(container.padding, "Padding should be nil")
        XCTAssertNil(container.child, "Child should be nil")
    }

    func testSizedBox_infiniteWidth() {
        let sizedBox = AMSizedBox(width: .infinity, height: 100)

        XCTAssertEqual(sizedBox.width, .infinity, "Width should be infinity")
        XCTAssertEqual(sizedBox.height, 100, "Height should be 100")
    }

    func testSizedBox_zeroHeight() {
        let sizedBox = AMSizedBox(width: 100, height: 0)

        XCTAssertEqual(sizedBox.width, 100, "Width should be 100")
        XCTAssertEqual(sizedBox.height, 0, "Height should be 0")
    }
}
