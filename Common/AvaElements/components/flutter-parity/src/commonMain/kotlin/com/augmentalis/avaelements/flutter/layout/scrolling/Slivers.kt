package com.augmentalis.avaelements.flutter.layout.scrolling

import kotlinx.serialization.Serializable

/**
 * A sliver that places multiple box children in a linear array along the main axis.
 *
 * [SliverList] is a sliver that lays its children out linearly, like [ListView], but it is
 * designed to be used inside a [CustomScrollView] along with other slivers. This allows you
 * to create complex scrolling layouts with different types of content.
 *
 * This is equivalent to Flutter's [SliverList] widget.
 *
 * Example with builder:
 * ```kotlin
 * SliverList(
 *     delegate = SliverChildDelegate.Builder(
 *         builder = { index ->
 *             ListTile(
 *                 title = Text("Item $index"),
 *                 subtitle = Text("Description for item $index")
 *             )
 *         },
 *         childCount = 50
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * SliverList(
 *   delegate: SliverChildBuilderDelegate(
 *     (context, index) {
 *       return ListTile(
 *         title: Text('Item $index'),
 *         subtitle: Text('Description for item $index'),
 *       );
 *     },
 *     childCount: 50,
 *   ),
 * )
 * ```
 *
 * Example with fixed children:
 * ```kotlin
 * SliverList(
 *     delegate = SliverChildDelegate.FixedExtent(
 *         children = listOf(
 *             ListTile(title = Text("First")),
 *             ListTile(title = Text("Second")),
 *             ListTile(title = Text("Third"))
 *         )
 *     )
 * )
 * ```
 *
 * Example in CustomScrollView:
 * ```kotlin
 * CustomScrollView(
 *     slivers = listOf(
 *         SliverAppBar(
 *             title = Text("My List"),
 *             pinned = true
 *         ),
 *         SliverList(
 *             delegate = SliverChildDelegate.Builder(
 *                 builder = { index -> Card(child = Text("Card $index")) },
 *                 childCount = 100
 *             )
 *         )
 *     )
 * )
 * ```
 *
 * Performance characteristics:
 * - Lazy loading: Only renders visible items + buffer
 * - Item recycling: Reuses item views for efficiency
 * - Target: 60 FPS scrolling with 10K+ items
 * - Memory: <100 MB for large lists
 *
 * @property delegate A delegate that provides the children for the sliver
 *
 * @see SliverGrid
 * @see SliverFixedExtentList
 * @see CustomScrollView
 * @see ListView
 * @since 2.1.0
 */
@Serializable
data class SliverList(
    val delegate: SliverChildDelegate
) : SliverComponent()

/**
 * A sliver that places multiple box children in a 2D arrangement.
 *
 * [SliverGrid] is a sliver that lays its children out in a grid, like [GridView], but it is
 * designed to be used inside a [CustomScrollView] along with other slivers.
 *
 * This is equivalent to Flutter's [SliverGrid] widget.
 *
 * Example with fixed column count:
 * ```kotlin
 * SliverGrid(
 *     gridDelegate = SliverGridDelegate.WithFixedCrossAxisCount(
 *         crossAxisCount = 3,
 *         mainAxisSpacing = 8f,
 *         crossAxisSpacing = 8f,
 *         childAspectRatio = 1.0f
 *     ),
 *     delegate = SliverChildDelegate.Builder(
 *         builder = { index ->
 *             Container(
 *                 color = Colors.Blue,
 *                 child = Center(child = Text("$index"))
 *             )
 *         },
 *         childCount = 50
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * SliverGrid(
 *   gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
 *     crossAxisCount: 3,
 *     mainAxisSpacing: 8.0,
 *     crossAxisSpacing: 8.0,
 *     childAspectRatio: 1.0,
 *   ),
 *   delegate: SliverChildBuilderDelegate(
 *     (context, index) {
 *       return Container(
 *         color: Colors.blue,
 *         child: Center(child: Text('$index')),
 *       );
 *     },
 *     childCount: 50,
 *   ),
 * )
 * ```
 *
 * Example with maximum tile width:
 * ```kotlin
 * SliverGrid(
 *     gridDelegate = SliverGridDelegate.WithMaxCrossAxisExtent(
 *         maxCrossAxisExtent = 150f,
 *         mainAxisSpacing = 4f,
 *         crossAxisSpacing = 4f
 *     ),
 *     delegate = SliverChildDelegate.Builder(
 *         builder = { index -> ImageCard(imageUrl = "image_$index.jpg") },
 *         childCount = 100
 *     )
 * )
 * ```
 *
 * Example in CustomScrollView with header:
 * ```kotlin
 * CustomScrollView(
 *     slivers = listOf(
 *         SliverToBoxAdapter(
 *             child = Container(
 *                 padding = Spacing.all(16f),
 *                 child = Text("Photo Gallery", style = TextStyle(fontSize = 24f))
 *             )
 *         ),
 *         SliverGrid(
 *             gridDelegate = SliverGridDelegate.WithFixedCrossAxisCount(
 *                 crossAxisCount = 2,
 *                 crossAxisSpacing = 8f,
 *                 mainAxisSpacing = 8f
 *             ),
 *             delegate = SliverChildDelegate.Builder(
 *                 builder = { index -> PhotoTile(photo = photos[index]) },
 *                 childCount = photos.size
 *             )
 *         )
 *     )
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
 * @property delegate A delegate that provides the children for the sliver
 *
 * @see SliverList
 * @see CustomScrollView
 * @see GridView
 * @since 2.1.0
 */
@Serializable
data class SliverGrid(
    val gridDelegate: SliverGridDelegate,
    val delegate: SliverChildDelegate
) : SliverComponent()

/**
 * A sliver that places multiple box children in a linear array with a fixed extent.
 *
 * [SliverFixedExtentList] is more efficient than [SliverList] when all items have the
 * same extent (height for vertical scrolling, width for horizontal). By knowing the exact
 * extent ahead of time, the framework can optimize scrolling calculations.
 *
 * This is equivalent to Flutter's [SliverFixedExtentList] widget.
 *
 * Example:
 * ```kotlin
 * SliverFixedExtentList(
 *     itemExtent = 50f,
 *     delegate = SliverChildDelegate.Builder(
 *         builder = { index ->
 *             Container(
 *                 height = 50f,
 *                 padding = Spacing.symmetric(horizontal = 16f),
 *                 child = Align(
 *                     alignment = Alignment.CenterLeft,
 *                     child = Text("Item $index")
 *                 )
 *             )
 *         },
 *         childCount = 1000
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * SliverFixedExtentList(
 *   itemExtent: 50.0,
 *   delegate: SliverChildBuilderDelegate(
 *     (context, index) {
 *       return Container(
 *         height: 50.0,
 *         padding: EdgeInsets.symmetric(horizontal: 16.0),
 *         child: Align(
 *           alignment: Alignment.centerLeft,
 *           child: Text('Item $index'),
 *         ),
 *       );
 *     },
 *     childCount: 1000,
 *   ),
 * )
 * ```
 *
 * Performance benefits:
 * - More efficient than [SliverList] for fixed-height items
 * - Faster scroll position calculations
 * - Reduced layout computation
 * - Better scroll bar positioning
 *
 * @property itemExtent The extent (height for vertical, width for horizontal) that each item will have
 * @property delegate A delegate that provides the children for the sliver
 *
 * @see SliverList
 * @see CustomScrollView
 * @since 2.1.0
 */
@Serializable
data class SliverFixedExtentList(
    val itemExtent: Float,
    val delegate: SliverChildDelegate
) : SliverComponent() {
    init {
        require(itemExtent > 0) { "itemExtent must be positive, got $itemExtent" }
    }
}

/**
 * A sliver that contains a Material Design app bar.
 *
 * [SliverAppBar] is a Material Design app bar that integrates with a [CustomScrollView].
 * It can expand and collapse, pin itself to the top, float, and more.
 *
 * This is equivalent to Flutter's [SliverAppBar] widget.
 *
 * Example with expanding header:
 * ```kotlin
 * SliverAppBar(
 *     expandedHeight = 200f,
 *     flexibleSpace = FlexibleSpaceBar(
 *         title = Text("Title"),
 *         background = Image(url = "header.jpg", fit = BoxFit.Cover)
 *     ),
 *     pinned = true
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * SliverAppBar(
 *   expandedHeight: 200.0,
 *   flexibleSpace: FlexibleSpaceBar(
 *     title: Text('Title'),
 *     background: Image.asset('header.jpg', fit: BoxFit.cover),
 *   ),
 *   pinned: true,
 * )
 * ```
 *
 * Example with floating app bar:
 * ```kotlin
 * SliverAppBar(
 *     title = Text("Floating App Bar"),
 *     floating = true,
 *     snap = true,
 *     actions = listOf(
 *         IconButton(icon = Icon(Icons.Search), onPressed = "onSearch"),
 *         IconButton(icon = Icon(Icons.More), onPressed = "onMore")
 *     )
 * )
 * ```
 *
 * @property title The primary widget displayed in the app bar
 * @property leading A widget to display before the title
 * @property actions Widgets to display in a row after the title widget
 * @property flexibleSpace The flexible space bar to use for the app bar
 * @property bottom Additional widget to display at the bottom of the app bar
 * @property expandedHeight The height of the app bar when it is fully expanded
 * @property floating Whether the app bar should become visible as soon as the user scrolls up
 * @property pinned Whether the app bar should remain visible at the start of the scroll view
 * @property snap If true and [floating] is true, the app bar will snap into view
 * @property stretch Whether the app bar should stretch to fill the over-scroll area
 * @property elevation The z-coordinate at which to place this app bar
 * @property backgroundColor The color to use for the app bar's background
 * @property foregroundColor The color to use for the app bar's foreground (text, icons)
 * @property forceElevated Whether to show the shadow appropriate for the elevation even if the content is not scrolled under the AppBar
 *
 * @see CustomScrollView
 * @see AppBar
 * @since 2.1.0
 */
@Serializable
data class SliverAppBar(
    val title: Any? = null,
    val leading: Any? = null,
    val actions: List<Any>? = null,
    val flexibleSpace: FlexibleSpaceBar? = null,
    val bottom: Any? = null,
    val expandedHeight: Float? = null,
    val floating: Boolean = false,
    val pinned: Boolean = false,
    val snap: Boolean = false,
    val stretch: Boolean = false,
    val elevation: Float = 4f,
    val backgroundColor: String? = null,
    val foregroundColor: String? = null,
    val forceElevated: Boolean = false
) : SliverComponent() {
    init {
        expandedHeight?.let { require(it > 0) { "expandedHeight must be positive, got $it" } }
        require(elevation >= 0) { "elevation must be non-negative, got $elevation" }
        if (snap) {
            require(floating) { "snap requires floating to be true" }
        }
    }
}

/**
 * A flexible space bar for use in a [SliverAppBar].
 *
 * The flexible space bar provides a smooth transition between an expanded app bar and
 * a collapsed app bar. It typically contains a title and/or background image.
 *
 * @property title The title to display in the flexible space
 * @property background A widget to display behind the title
 * @property centerTitle Whether to center the title horizontally
 * @property titlePadding The padding for the title
 * @property collapseMode How the title should collapse
 * @property stretchModes The modes to use when the flexible space stretches
 *
 * @see SliverAppBar
 * @since 2.1.0
 */
@Serializable
data class FlexibleSpaceBar(
    val title: Any? = null,
    val background: Any? = null,
    val centerTitle: Boolean = false,
    val titlePadding: String? = null, // Serialized EdgeInsets
    val collapseMode: CollapseMode = CollapseMode.Parallax,
    val stretchModes: List<StretchMode> = listOf(StretchMode.ZoomBackground)
)

/**
 * How a flexible space bar should collapse
 */
@Serializable
enum class CollapseMode {
    /**
     * The background widget will scroll in a parallax fashion (default)
     */
    Parallax,

    /**
     * The background widget will act as normal with no parallax effect
     */
    Pin,

    /**
     * The background widget will not scroll at all
     */
    None
}

/**
 * How a flexible space bar should stretch beyond its normal bounds
 */
@Serializable
enum class StretchMode {
    /**
     * Zooms the background widget when over-scrolling (default)
     */
    ZoomBackground,

    /**
     * Blurs the background widget when over-scrolling
     */
    BlurBackground,

    /**
     * Fades the title when over-scrolling
     */
    FadeTitle
}
