package com.augmentalis.avaelements.flutter.layout.scrolling

import com.augmentalis.avaelements.core.types.Spacing
import kotlinx.serialization.Serializable

/**
 * A scrollable list of widgets arranged linearly, built on demand.
 *
 * [ListViewBuilder] is a more efficient way to build a long list than using [ListView] constructor
 * because it builds items on demand (lazily). This makes it suitable for lists with a large (or infinite)
 * number of children because the builder is called only for those items that are actually visible.
 *
 * The [itemBuilder] callback will be called with indices starting from 0 and increasing as long as
 * [itemBuilder] returns a non-null value for the given index. If [itemCount] is non-null, the list will
 * have exactly that many items.
 *
 * This is equivalent to Flutter's [ListView.builder] constructor.
 *
 * Example with finite item count:
 * ```kotlin
 * ListViewBuilder(
 *     itemCount = 100,
 *     itemBuilder = { index ->
 *         ListTile(
 *             title = Text("Item $index"),
 *             subtitle = Text("Subtitle for item $index")
 *         )
 *     }
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * ListView.builder(
 *   itemCount: 100,
 *   itemBuilder: (context, index) {
 *     return ListTile(
 *       title: Text('Item $index'),
 *       subtitle: Text('Subtitle for item $index'),
 *     );
 *   },
 * )
 * ```
 *
 * Example with infinite scroll:
 * ```kotlin
 * ListViewBuilder(
 *     itemBuilder = { index ->
 *         Container(
 *             padding = Spacing.all(16f),
 *             child = Text("Item $index")
 *         )
 *     }
 * )
 * ```
 *
 * Example with custom scroll controller:
 * ```kotlin
 * val scrollController = ScrollController()
 *
 * ListViewBuilder(
 *     controller = scrollController,
 *     itemCount = 1000,
 *     itemBuilder = { index -> Text("Item $index") }
 * )
 * ```
 *
 * Performance characteristics:
 * - Lazy loading: Only renders visible items + buffer
 * - Item recycling: Reuses item views for efficiency
 * - Target: 60 FPS scrolling with 10K+ items
 * - Memory: <100 MB for large lists
 *
 * @property itemCount The total number of items. If null, the list is infinite.
 * @property itemBuilder Called to build children for the list. Must return a widget for valid indices.
 *                       For infinite lists, should handle any non-negative integer.
 * @property controller An object that can be used to control the position to which this scroll view is scrolled.
 * @property scrollDirection The axis along which the scroll view scrolls.
 * @property reverse Whether the scroll view scrolls in the reading direction.
 * @property padding The amount of space by which to inset the children.
 * @property itemExtent If non-null, forces the children to have the given extent in the scroll direction.
 *                      This can improve performance because it allows the scroll view to estimate
 *                      the scroll offset without having to measure children.
 * @property shrinkWrap Whether the extent of the scroll view in the scroll direction should be determined
 *                      by the contents being viewed. Defaults to false.
 * @property physics How the scroll view should respond to user input.
 *
 * @see ListView
 * @see ListViewSeparated
 * @see CustomScrollView
 * @since 2.1.0
 */
@Serializable
data class ListViewBuilderComponent(
    val itemCount: Int? = null,
    val itemBuilder: String, // Serialized function reference or ID
    val controller: ScrollController? = null,
    val scrollDirection: ScrollDirection = ScrollDirection.Vertical,
    val reverse: Boolean = false,
    val padding: Spacing? = null,
    val itemExtent: Float? = null,
    val shrinkWrap: Boolean = false,
    val physics: ScrollPhysics = ScrollPhysics.AlwaysScrollable
) {
    init {
        itemCount?.let { require(it >= 0) { "itemCount must be non-negative, got $it" } }
        itemExtent?.let { require(it > 0) { "itemExtent must be positive, got $it" } }
    }
}

/**
 * The direction in which a scroll view scrolls
 */
@Serializable
enum class ScrollDirection {
    /**
     * Scroll vertically (default)
     */
    Vertical,

    /**
     * Scroll horizontally
     */
    Horizontal
}

/**
 * How the scroll view should respond to user input
 */
@Serializable
enum class ScrollPhysics {
    /**
     * Always allow the user to scroll (default)
     */
    AlwaysScrollable,

    /**
     * Scroll physics that does not allow the user to scroll
     */
    NeverScrollable,

    /**
     * Scroll physics for environments that allow the scroll offset to reach
     * beyond the bounds of the content, but then bounce back
     */
    Bouncing,

    /**
     * Scroll physics that matches platform conventions (Android uses clamping, iOS uses bouncing)
     */
    Platform
}

/**
 * Controls a scrollable widget
 *
 * This is a simplified version of Flutter's ScrollController.
 */
@Serializable
data class ScrollController(
    val initialScrollOffset: Float = 0f,
    val keepScrollOffset: Boolean = true,
    val debugLabel: String? = null
) {
    init {
        require(initialScrollOffset >= 0) { "initialScrollOffset must be non-negative, got $initialScrollOffset" }
    }
}
