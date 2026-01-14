package com.augmentalis.avaelements.flutter.layout.scrolling

import com.augmentalis.avaelements.core.types.Spacing
import kotlinx.serialization.Serializable

/**
 * A scrollable, 2D array of widgets built on demand.
 *
 * [GridViewBuilder] is an efficient way to build a grid of items because it builds items on demand (lazily).
 * This makes it suitable for grids with a large number of children because the builder is called only
 * for those items that are actually visible.
 *
 * The grid delegate controls the layout of the grid. The most common delegates are
 * [SliverGridDelegateWithFixedCrossAxisCount] (fixed number of columns) and
 * [SliverGridDelegateWithMaxCrossAxisExtent] (maximum tile width).
 *
 * This is equivalent to Flutter's [GridView.builder] constructor.
 *
 * Example with fixed column count:
 * ```kotlin
 * GridViewBuilder(
 *     gridDelegate = SliverGridDelegateWithFixedCrossAxisCount(
 *         crossAxisCount = 3,
 *         crossAxisSpacing = 8f,
 *         mainAxisSpacing = 8f,
 *         childAspectRatio = 1.0f
 *     ),
 *     itemCount = 100,
 *     itemBuilder = { index ->
 *         Container(
 *             color = Colors.Blue,
 *             child = Center(
 *                 child = Text("$index")
 *             )
 *         )
 *     }
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * GridView.builder(
 *   gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
 *     crossAxisCount: 3,
 *     crossAxisSpacing: 8.0,
 *     mainAxisSpacing: 8.0,
 *     childAspectRatio: 1.0,
 *   ),
 *   itemCount: 100,
 *   itemBuilder: (context, index) {
 *     return Container(
 *       color: Colors.blue,
 *       child: Center(
 *         child: Text('$index'),
 *       ),
 *     );
 *   },
 * )
 * ```
 *
 * Example with maximum tile width:
 * ```kotlin
 * GridViewBuilder(
 *     gridDelegate = SliverGridDelegateWithMaxCrossAxisExtent(
 *         maxCrossAxisExtent = 150f,
 *         mainAxisSpacing = 4f,
 *         crossAxisSpacing = 4f,
 *         childAspectRatio = 0.75f
 *     ),
 *     itemCount = 50,
 *     itemBuilder = { index ->
 *         Card(
 *             child = Image(url = "https://example.com/image_$index.jpg")
 *         )
 *     }
 * )
 * ```
 *
 * Performance characteristics:
 * - Lazy loading: Only renders visible items + buffer
 * - Item recycling: Reuses item views for efficiency
 * - Target: 60 FPS scrolling with 10K+ items
 * - Memory: <100 MB for large grids
 *
 * @property gridDelegate Controls the layout of tiles in the grid
 * @property itemCount The total number of items. If null, the grid is infinite.
 * @property itemBuilder Called to build children for the grid
 * @property controller An object that can be used to control the scroll position
 * @property scrollDirection The axis along which the scroll view scrolls
 * @property reverse Whether the scroll view scrolls in the reading direction
 * @property padding The amount of space by which to inset the children
 * @property shrinkWrap Whether the extent of the scroll view should be determined by the contents
 * @property physics How the scroll view should respond to user input
 *
 * @see GridView
 * @see ListViewBuilder
 * @see SliverGrid
 * @since 2.1.0
 */
@Serializable
data class GridViewBuilderComponent(
    val gridDelegate: SliverGridDelegate,
    val itemCount: Int? = null,
    val itemBuilder: String, // Serialized function reference or ID
    val controller: ScrollController? = null,
    val scrollDirection: ScrollDirection = ScrollDirection.Vertical,
    val reverse: Boolean = false,
    val padding: Spacing? = null,
    val shrinkWrap: Boolean = false,
    val physics: ScrollPhysics = ScrollPhysics.AlwaysScrollable
) {
    init {
        itemCount?.let { require(it >= 0) { "itemCount must be non-negative, got $it" } }
    }
}

/**
 * Controls the layout of tiles in a grid
 */
@Serializable
sealed class SliverGridDelegate {
    /**
     * Creates a delegate with a fixed number of tiles in the cross axis
     *
     * @property crossAxisCount The number of children in the cross axis
     * @property mainAxisSpacing The number of pixels between each child along the main axis
     * @property crossAxisSpacing The number of pixels between each child along the cross axis
     * @property childAspectRatio The ratio of the cross-axis to the main-axis extent of each child
     * @property mainAxisExtent If non-null, forces children to have the given extent in the main axis
     */
    @Serializable
    data class WithFixedCrossAxisCount(
        val crossAxisCount: Int,
        val mainAxisSpacing: Float = 0f,
        val crossAxisSpacing: Float = 0f,
        val childAspectRatio: Float = 1.0f,
        val mainAxisExtent: Float? = null
    ) : SliverGridDelegate() {
        init {
            require(crossAxisCount > 0) { "crossAxisCount must be positive, got $crossAxisCount" }
            require(mainAxisSpacing >= 0) { "mainAxisSpacing must be non-negative, got $mainAxisSpacing" }
            require(crossAxisSpacing >= 0) { "crossAxisSpacing must be non-negative, got $crossAxisSpacing" }
            require(childAspectRatio > 0) { "childAspectRatio must be positive, got $childAspectRatio" }
            mainAxisExtent?.let { require(it > 0) { "mainAxisExtent must be positive, got $it" } }
        }
    }

    /**
     * Creates a delegate with tiles that have a maximum cross-axis extent
     *
     * This delegate will select a cross-axis extent that maximizes the number of tiles
     * that fit without exceeding maxCrossAxisExtent.
     *
     * @property maxCrossAxisExtent The maximum extent of tiles in the cross axis
     * @property mainAxisSpacing The number of pixels between each child along the main axis
     * @property crossAxisSpacing The number of pixels between each child along the cross axis
     * @property childAspectRatio The ratio of the cross-axis to the main-axis extent of each child
     * @property mainAxisExtent If non-null, forces children to have the given extent in the main axis
     */
    @Serializable
    data class WithMaxCrossAxisExtent(
        val maxCrossAxisExtent: Float,
        val mainAxisSpacing: Float = 0f,
        val crossAxisSpacing: Float = 0f,
        val childAspectRatio: Float = 1.0f,
        val mainAxisExtent: Float? = null
    ) : SliverGridDelegate() {
        init {
            require(maxCrossAxisExtent > 0) { "maxCrossAxisExtent must be positive, got $maxCrossAxisExtent" }
            require(mainAxisSpacing >= 0) { "mainAxisSpacing must be non-negative, got $mainAxisSpacing" }
            require(crossAxisSpacing >= 0) { "crossAxisSpacing must be non-negative, got $crossAxisSpacing" }
            require(childAspectRatio > 0) { "childAspectRatio must be positive, got $childAspectRatio" }
            mainAxisExtent?.let { require(it > 0) { "mainAxisExtent must be positive, got $it" } }
        }
    }
}
