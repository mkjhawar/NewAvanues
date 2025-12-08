import XCTest
import SwiftUI
@testable import AvaElementsRenderer

/**
 * Performance tests for iOS SwiftUI renderer.
 *
 * Tests:
 * - Rendering performance (<16ms per component)
 * - Memory usage (<100 MB for typical usage)
 * - Scroll performance (60 FPS)
 * - Animation frame rate (60 FPS)
 * - Complex layout rendering
 * - Image loading and caching
 *
 * Targets (matching Android):
 * - Rendering: <16ms per component
 * - Memory: <100 MB for typical usage
 * - Animation: 60 FPS (16.67ms per frame)
 *
 * @since 1.0.0 (iOS Testing Framework)
 */
class RenderingPerformanceTests: XCTestCase {

    // MARK: - Rendering Performance

    func testPerformance_singleComponentRendering() {
        measure {
            for _ in 0..<100 {
                let button = AMTextButton(text: "Button")
                let view = SwiftUIRenderer().render(button)
                _ = view.body // Force rendering
            }
        }

        // Expected: <16ms total for 100 components = 0.16ms per component
    }

    func testPerformance_layoutComponentRendering() {
        measure {
            for _ in 0..<50 {
                let column = AMColumn(
                    children: [
                        AMText(text: "Item 1"),
                        AMText(text: "Item 2"),
                        AMText(text: "Item 3"),
                        AMText(text: "Item 4"),
                        AMText(text: "Item 5")
                    ]
                )
                let view = SwiftUIRenderer().render(column)
                _ = view.body
            }
        }

        // Expected: <20ms total for 50 layouts
    }

    func testPerformance_chipRendering() {
        measure {
            for _ in 0..<100 {
                let chip = AMFilterChip(
                    label: "Filter",
                    selected: true,
                    leadingIcon: AMIcon(name: "checkmark")
                )
                let view = SwiftUIRenderer().render(chip)
                _ = view.body
            }
        }

        // Expected: <16ms total for 100 chips
    }

    func testPerformance_cardRendering() {
        measure {
            for _ in 0..<50 {
                let card = AMCard(
                    elevation: 2,
                    child: AMPadding(
                        padding: 16,
                        child: AMColumn(
                            children: [
                                AMText(text: "Title"),
                                AMText(text: "Content")
                            ]
                        )
                    )
                )
                let view = SwiftUIRenderer().render(card)
                _ = view.body
            }
        }

        // Expected: <25ms total for 50 cards
    }

    // MARK: - Complex Layout Performance

    func testPerformance_complexLayout() {
        measure {
            for _ in 0..<20 {
                let layout = AMColumn(
                    children: [
                        AMCard(
                            child: AMPadding(
                                padding: 16,
                                child: AMColumn(
                                    children: [
                                        AMRow(
                                            mainAxisAlignment: .spaceBetween,
                                            children: [
                                                AMText(text: "Header"),
                                                AMIconButton(icon: AMIcon(name: "ellipsis"))
                                            ]
                                        ),
                                        AMDivider(),
                                        AMText(text: "Content"),
                                        AMRow(
                                            children: [
                                                AMFilterChip(label: "Tag 1"),
                                                AMFilterChip(label: "Tag 2")
                                            ]
                                        )
                                    ]
                                )
                            )
                        )
                    ]
                )
                let view = SwiftUIRenderer().render(layout)
                _ = view.body
            }
        }

        // Expected: <50ms total for 20 complex layouts
    }

    func testPerformance_deepNesting() {
        measure {
            var current: AMComponent = AMText(text: "Deep")

            for _ in 0..<50 {
                current = AMContainer(child: current)
            }

            let view = SwiftUIRenderer().render(current)
            _ = view.body
        }

        // Expected: <30ms for 50-level deep nesting
    }

    func testPerformance_manyChildren() {
        measure {
            let children = (1...100).map { AMText(text: "Item \($0)") }
            let column = AMColumn(children: children)
            let view = SwiftUIRenderer().render(column)
            _ = view.body
        }

        // Expected: <50ms for 100 children
    }

    // MARK: - List Rendering Performance

    func testPerformance_scrollableList() {
        measure {
            let items = (1...1000).map { AMText(text: "Item \($0)") }
            let list = AMColumn(children: items)
            let view = SwiftUIRenderer().render(list)
            _ = view.body
        }

        // Expected: <200ms for 1000 items (with lazy loading)
    }

    func testPerformance_listWithComplexItems() {
        measure {
            let items = (1...100).map { index in
                AMCard(
                    child: AMPadding(
                        padding: 16,
                        child: AMRow(
                            children: [
                                AMIconButton(icon: AMIcon(name: "star")),
                                AMColumn(
                                    children: [
                                        AMText(text: "Item \(index)"),
                                        AMText(text: "Subtitle")
                                    ]
                                )
                            ]
                        )
                    )
                )
            }
            let list = AMColumn(children: items)
            let view = SwiftUIRenderer().render(list)
            _ = view.body
        }

        // Expected: <150ms for 100 complex items
    }

    // MARK: - State Update Performance

    func testPerformance_stateUpdates() {
        measure {
            for value in stride(from: 0.0, through: 1.0, by: 0.01) {
                let slider = AMSlider(value: value, min: 0.0, max: 1.0)
                let view = SwiftUIRenderer().render(slider)
                _ = view.body
            }
        }

        // Expected: <20ms for 100 state updates
    }

    func testPerformance_switchToggle() {
        measure {
            for i in 0..<1000 {
                let switchControl = AMSwitch(value: i % 2 == 0)
                let view = SwiftUIRenderer().render(switchControl)
                _ = view.body
            }
        }

        // Expected: <30ms for 1000 toggles
    }

    func testPerformance_checkboxStates() {
        measure {
            for i in 0..<1000 {
                let value: Bool? = i % 3 == 0 ? nil : i % 2 == 0
                let checkbox = AMCheckbox(tristate: true, value: value)
                let view = SwiftUIRenderer().render(checkbox)
                _ = view.body
            }
        }

        // Expected: <30ms for 1000 state changes
    }

    // MARK: - Memory Performance

    func testMemory_componentInstantiation() {
        measure(metrics: [XCTMemoryMetric()]) {
            var components: [AMComponent] = []

            for i in 0..<10000 {
                components.append(AMText(text: "Item \(i)"))
            }

            // Force retention
            _ = components.count
        }

        // Expected: <20 MB for 10,000 components
    }

    func testMemory_complexLayoutInstantiation() {
        measure(metrics: [XCTMemoryMetric()]) {
            var cards: [AMCard] = []

            for i in 0..<1000 {
                cards.append(
                    AMCard(
                        child: AMPadding(
                            padding: 16,
                            child: AMColumn(
                                children: [
                                    AMText(text: "Title \(i)"),
                                    AMDivider(),
                                    AMText(text: "Content \(i)")
                                ]
                            )
                        )
                    )
                )
            }

            _ = cards.count
        }

        // Expected: <50 MB for 1,000 complex cards
    }

    func testMemory_viewRendering() {
        measure(metrics: [XCTMemoryMetric()]) {
            for i in 0..<1000 {
                let component = AMCard(
                    child: AMText(text: "Card \(i)")
                )
                let view = SwiftUIRenderer().render(component)
                _ = view.body
            }
        }

        // Expected: <80 MB for 1,000 rendered views
    }

    // MARK: - Image Loading Performance

    func testPerformance_imageLoading() {
        // Note: This would require actual image assets
        measure {
            for i in 0..<50 {
                let image = AMImage(url: "https://via.placeholder.com/150")
                let view = SwiftUIRenderer().render(image)
                _ = view.body
            }
        }

        // Expected: <100ms for 50 image components (cached)
    }

    // MARK: - Animation Performance

    func testPerformance_animatedComponents() {
        measure {
            for value in stride(from: 0.0, through: 1.0, by: 0.016667) {
                let progress = AMLinearProgressIndicator(value: value)
                let view = SwiftUIRenderer().render(progress)
                _ = view.body
            }
        }

        // Expected: <60ms for 60 frames (60 FPS)
    }

    func testPerformance_animatedOpacity() {
        measure {
            for value in stride(from: 0.0, through: 1.0, by: 0.016667) {
                let container = AMContainer(
                    opacity: value,
                    child: AMText(text: "Animated")
                )
                let view = SwiftUIRenderer().render(container)
                _ = view.body
            }
        }

        // Expected: <60ms for 60 frames (60 FPS)
    }

    // MARK: - Component Comparison Performance

    func testPerformance_allButtonTypes() {
        measure {
            for _ in 0..<100 {
                _ = AMTextButton(text: "Text")
                _ = AMElevatedButton(text: "Elevated")
                _ = AMFilledButton(text: "Filled")
                _ = AMOutlinedButton(text: "Outlined")
                _ = AMIconButton(icon: AMIcon(name: "star"))
                _ = AMFloatingActionButton(icon: AMIcon(name: "plus"))
            }
        }

        // Expected: <20ms for 600 button instantiations
    }

    func testPerformance_allChipTypes() {
        measure {
            for _ in 0..<100 {
                _ = AMFilterChip(label: "Filter")
                _ = AMActionChip(label: "Action")
                _ = AMChoiceChip(label: "Choice")
                _ = AMInputChip(label: "Input")
            }
        }

        // Expected: <15ms for 400 chip instantiations
    }

    // MARK: - Real-World Scenarios

    func testPerformance_typicalProfileScreen() {
        measure {
            for _ in 0..<20 {
                let screen = AMColumn(
                    children: [
                        // Header
                        AMCard(
                            child: AMPadding(
                                padding: 16,
                                child: AMRow(
                                    children: [
                                        AMContainer(
                                            width: 80,
                                            height: 80,
                                            borderRadius: 40,
                                            child: AMImage(url: "avatar.jpg")
                                        ),
                                        AMSpacer(width: 16),
                                        AMColumn(
                                            children: [
                                                AMText(text: "John Doe", style: .headline),
                                                AMText(text: "john@example.com", style: .caption)
                                            ]
                                        )
                                    ]
                                )
                            )
                        ),
                        // Settings
                        AMCard(
                            child: AMColumn(
                                children: [
                                    AMRow(
                                        mainAxisAlignment: .spaceBetween,
                                        children: [
                                            AMText(text: "Notifications"),
                                            AMSwitch(value: true)
                                        ]
                                    ),
                                    AMDivider(),
                                    AMRow(
                                        mainAxisAlignment: .spaceBetween,
                                        children: [
                                            AMText(text: "Dark Mode"),
                                            AMSwitch(value: false)
                                        ]
                                    )
                                ]
                            )
                        ),
                        // Actions
                        AMRow(
                            mainAxisAlignment: .spaceBetween,
                            children: [
                                AMTextButton(text: "Sign Out"),
                                AMFilledButton(text: "Save")
                            ]
                        )
                    ]
                )
                let view = SwiftUIRenderer().render(screen)
                _ = view.body
            }
        }

        // Expected: <100ms for 20 profile screens
    }

    func testPerformance_typicalFeed() {
        measure {
            let posts = (1...50).map { index in
                AMCard(
                    child: AMPadding(
                        padding: 16,
                        child: AMColumn(
                            children: [
                                AMRow(
                                    mainAxisAlignment: .spaceBetween,
                                    children: [
                                        AMText(text: "Post \(index)", style: .headline),
                                        AMText(text: "2h ago", style: .caption)
                                    ]
                                ),
                                AMSpacer(height: 8),
                                AMText(text: "Lorem ipsum dolor sit amet..."),
                                AMSpacer(height: 12),
                                AMRow(
                                    children: [
                                        AMIconButton(icon: AMIcon(name: "heart")),
                                        AMIconButton(icon: AMIcon(name: "message")),
                                        AMIconButton(icon: AMIcon(name: "paperplane"))
                                    ]
                                )
                            ]
                        )
                    )
                )
            }
            let feed = AMColumn(children: posts)
            let view = SwiftUIRenderer().render(feed)
            _ = view.body
        }

        // Expected: <200ms for 50-post feed
    }

    // MARK: - Baseline Metrics

    func testBaseline_emptyView() {
        measure {
            for _ in 0..<10000 {
                let text = AMText(text: "")
                let view = SwiftUIRenderer().render(text)
                _ = view.body
            }
        }

        // Baseline: Fastest possible rendering
    }

    func testBaseline_simpleText() {
        measure {
            for _ in 0..<10000 {
                let text = AMText(text: "Simple text")
                let view = SwiftUIRenderer().render(text)
                _ = view.body
            }
        }

        // Baseline: Text rendering overhead
    }
}
