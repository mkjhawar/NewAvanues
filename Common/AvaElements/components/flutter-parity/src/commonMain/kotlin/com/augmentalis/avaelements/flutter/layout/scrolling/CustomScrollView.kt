package com.augmentalis.avaelements.flutter.layout.scrolling

import com.augmentalis.avaelements.core.types.Spacing
import kotlinx.serialization.Serializable

/**
 * A scrollable widget that creates custom scroll effects using slivers.
 *
 * A [CustomScrollView] lets you supply slivers directly to create various scrolling effects,
 * such as lists, grids, and expanding headers. Slivers are scrollable areas that can be
 * combined to create custom scrollable layouts.
 *
 * Unlike [ListView] or [GridView], which use a single type of sliver internally, a
 * [CustomScrollView] allows you to combine multiple types of slivers to create more
 * complex scrolling experiences.
 *
 * This is equivalent to Flutter's [CustomScrollView] widget.
 *
 * Example with app bar and list:
 * ```kotlin
 * CustomScrollView(
 *     slivers = listOf(
 *         SliverAppBar(
 *             expandedHeight = 200f,
 *             flexibleSpace = FlexibleSpaceBar(
 *                 title = Text("Custom Scroll View"),
 *                 background = Image(url = "header.jpg", fit = BoxFit.Cover)
 *             ),
 *             pinned = true
 *         ),
 *         SliverList(
 *             delegate = SliverChildBuilderDelegate(
 *                 builder = { index -> ListTile(title = Text("Item $index")) },
 *                 childCount = 50
 *             )
 *         )
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * CustomScrollView(
 *   slivers: [
 *     SliverAppBar(
 *       expandedHeight: 200.0,
 *       flexibleSpace: FlexibleSpaceBar(
 *         title: Text('Custom Scroll View'),
 *         background: Image.asset('header.jpg', fit: BoxFit.cover),
 *       ),
 *       pinned: true,
 *     ),
 *     SliverList(
 *       delegate: SliverChildBuilderDelegate(
 *         (context, index) => ListTile(title: Text('Item $index')),
 *         childCount: 50,
 *       ),
 *     ),
 *   ],
 * )
 * ```
 *
 * Example with grid and fixed extent list:
 * ```kotlin
 * CustomScrollView(
 *     slivers = listOf(
 *         SliverPadding(
 *             padding = Spacing.all(8f),
 *             sliver = SliverGrid(
 *                 gridDelegate = SliverGridDelegate.WithFixedCrossAxisCount(
 *                     crossAxisCount = 2,
 *                     mainAxisSpacing = 8f,
 *                     crossAxisSpacing = 8f
 *                 ),
 *                 delegate = SliverChildBuilderDelegate(
 *                     builder = { index -> Card(child = Center(child = Text("$index"))) },
 *                     childCount = 10
 *                 )
 *             )
 *         ),
 *         SliverFixedExtentList(
 *             itemExtent = 50f,
 *             delegate = SliverChildBuilderDelegate(
 *                 builder = { index -> ListTile(title = Text("List Item $index")) },
 *                 childCount = 20
 *             )
 *         )
 *     )
 * )
 * ```
 *
 * Example with multiple sections:
 * ```kotlin
 * CustomScrollView(
 *     slivers = listOf(
 *         SliverToBoxAdapter(
 *             child = Container(
 *                 height = 100f,
 *                 color = Colors.Blue,
 *                 child = Center(child = Text("Header", style = TextStyle(color = Colors.White)))
 *             )
 *         ),
 *         SliverList(
 *             delegate = SliverChildBuilderDelegate(
 *                 builder = { index -> ListTile(title = Text("Section 1 - Item $index")) },
 *                 childCount = 5
 *             )
 *         ),
 *         SliverToBoxAdapter(
 *             child = Divider()
 *         ),
 *         SliverGrid(
 *             gridDelegate = SliverGridDelegate.WithMaxCrossAxisExtent(
 *                 maxCrossAxisExtent = 100f
 *             ),
 *             delegate = SliverChildBuilderDelegate(
 *                 builder = { index -> Container(color = Colors.Green) },
 *                 childCount = 6
 *             )
 *         )
 *     )
 * )
 * ```
 *
 * Common sliver types:
 * - [SliverAppBar] - A Material Design app bar that integrates with a [CustomScrollView]
 * - [SliverList] - A sliver that places multiple box children in a linear array
 * - [SliverGrid] - A sliver that places multiple box children in a 2D arrangement
 * - [SliverFixedExtentList] - A sliver list where all items have the same extent
 * - [SliverToBoxAdapter] - A sliver that contains a single box widget
 * - [SliverPadding] - A sliver that adds padding around another sliver
 * - [SliverFillRemaining] - A sliver that fills the remaining space
 *
 * @property slivers The slivers to place inside the viewport
 * @property controller An object that can be used to control the scroll position
 * @property scrollDirection The axis along which the scroll view scrolls
 * @property reverse Whether the scroll view scrolls in the reading direction
 * @property physics How the scroll view should respond to user input
 * @property shrinkWrap Whether the extent of the scroll view should be determined by the contents
 * @property anchor The relative position of the zero scroll offset
 * @property cacheExtent The viewport has an area before and after the visible area to cache items
 * @property semanticChildCount The number of children that will contribute semantic information
 * @property dragStartBehavior Determines how drag start behavior is handled
 *
 * @see SliverList
 * @see SliverGrid
 * @see SliverAppBar
 * @see ListView
 * @see GridView
 * @since 2.1.0
 */
@Serializable
data class CustomScrollViewComponent(
    val slivers: List<SliverComponent>,
    val controller: ScrollController? = null,
    val scrollDirection: ScrollDirection = ScrollDirection.Vertical,
    val reverse: Boolean = false,
    val physics: ScrollPhysics = ScrollPhysics.AlwaysScrollable,
    val shrinkWrap: Boolean = false,
    val anchor: Float = 0f,
    val cacheExtent: Float? = null,
    val semanticChildCount: Int? = null,
    val dragStartBehavior: DragStartBehavior = DragStartBehavior.Start
) {
    init {
        require(slivers.isNotEmpty()) { "slivers list cannot be empty" }
        require(anchor >= 0 && anchor <= 1.0f) { "anchor must be between 0 and 1, got $anchor" }
        cacheExtent?.let { require(it >= 0) { "cacheExtent must be non-negative, got $it" } }
        semanticChildCount?.let { require(it >= 0) { "semanticChildCount must be non-negative, got $it" } }
    }
}

/**
 * Base class for sliver components
 */
@Serializable
sealed class SliverComponent

/**
 * Determines the way that drag start behavior is handled
 */
@Serializable
enum class DragStartBehavior {
    /**
     * Start immediately when the user touches down (default for most platforms)
     */
    Down,

    /**
     * Start when the user has moved beyond a threshold (default for iOS)
     */
    Start
}

/**
 * A sliver that contains a single box widget.
 *
 * [SliverToBoxAdapter] is a simple sliver that takes a single box widget and makes it
 * scrollable within a [CustomScrollView]. This is useful for inserting non-sliver widgets
 * into a sliver-based scrollable.
 *
 * @property child The box widget to display
 *
 * @see CustomScrollView
 * @since 2.1.0
 */
@Serializable
data class SliverToBoxAdapter(
    val child: Any
) : SliverComponent()

/**
 * A sliver that applies padding around another sliver.
 *
 * @property padding The amount of space by which to inset the child sliver
 * @property sliver The sliver to which padding will be applied
 *
 * @see CustomScrollView
 * @since 2.1.0
 */
@Serializable
data class SliverPadding(
    val padding: Spacing,
    val sliver: SliverComponent
) : SliverComponent()

/**
 * A sliver that fills the remaining space in the viewport.
 *
 * [SliverFillRemaining] is useful when you want a sliver to take up all remaining space,
 * such as for a footer that should always be at the bottom of the screen.
 *
 * @property child The widget to display in the remaining space
 * @property hasScrollBody Whether the child has a scrollable body
 * @property fillOverscroll Whether to fill the overscroll area as well
 *
 * @see CustomScrollView
 * @since 2.1.0
 */
@Serializable
data class SliverFillRemaining(
    val child: Any,
    val hasScrollBody: Boolean = true,
    val fillOverscroll: Boolean = false
) : SliverComponent()

/**
 * A delegate that supplies children for slivers.
 *
 * This is a base class for sliver child delegates.
 */
@Serializable
sealed class SliverChildDelegate {
    /**
     * A delegate that supplies children using a builder callback.
     *
     * @property builder A function that builds a child widget for a given index
     * @property childCount The total number of children. If null, the delegate will continue
     *                      calling builder until it returns null.
     * @property addAutomaticKeepAlives Whether to wrap children in AutomaticKeepAlive widgets
     * @property addRepaintBoundaries Whether to wrap children in RepaintBoundary widgets
     * @property addSemanticIndexes Whether to add semantic indexes to children
     *
     * @see SliverList
     * @see SliverGrid
     * @since 2.1.0
     */
    @Serializable
    data class Builder(
        val builder: String, // Serialized function reference
        val childCount: Int? = null,
        val addAutomaticKeepAlives: Boolean = true,
        val addRepaintBoundaries: Boolean = true,
        val addSemanticIndexes: Boolean = true
    ) : SliverChildDelegate() {
        init {
            childCount?.let { require(it >= 0) { "childCount must be non-negative, got $it" } }
        }
    }

    /**
     * A delegate that supplies children using a list.
     *
     * @property children The list of children
     * @property addAutomaticKeepAlives Whether to wrap children in AutomaticKeepAlive widgets
     * @property addRepaintBoundaries Whether to wrap children in RepaintBoundary widgets
     * @property addSemanticIndexes Whether to add semantic indexes to children
     *
     * @see SliverList
     * @see SliverGrid
     * @since 2.1.0
     */
    @Serializable
    data class FixedExtent(
        val children: List<Any>,
        val addAutomaticKeepAlives: Boolean = true,
        val addRepaintBoundaries: Boolean = true,
        val addSemanticIndexes: Boolean = true
    ) : SliverChildDelegate() {
        init {
            require(children.isNotEmpty()) { "children list cannot be empty" }
        }
    }
}
