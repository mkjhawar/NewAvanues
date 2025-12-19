package com.augmentalis.avaelements.flutter.layout.scrolling

import kotlinx.serialization.Serializable

/**
 * A scrollable list that works page by page.
 *
 * Each child of a page view is forced to be the same size as the viewport. You can use a
 * [PageController] to control which page is visible in the view.
 *
 * This is equivalent to Flutter's [PageView] widget.
 *
 * Example with builder:
 * ```kotlin
 * PageView(
 *     itemCount = 5,
 *     itemBuilder = { index ->
 *         Container(
 *             color = colors[index],
 *             child = Center(
 *                 child = Text("Page $index", style = TextStyle(fontSize = 24f))
 *             )
 *         )
 *     }
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * PageView.builder(
 *   itemCount: 5,
 *   itemBuilder: (context, index) {
 *     return Container(
 *       color: colors[index],
 *       child: Center(
 *         child: Text('Page $index', style: TextStyle(fontSize: 24)),
 *       ),
 *     );
 *   },
 * )
 * ```
 *
 * Example with page controller:
 * ```kotlin
 * val pageController = PageController(initialPage = 1, viewportFraction = 0.8f)
 *
 * PageView(
 *     controller = pageController,
 *     itemCount = 10,
 *     itemBuilder = { index ->
 *         Card(
 *             margin = Spacing.all(8f),
 *             child = Center(child = Text("Page $index"))
 *         )
 *     }
 * )
 * ```
 *
 * Example with children:
 * ```kotlin
 * PageView(
 *     children = listOf(
 *         OnboardingPage1(),
 *         OnboardingPage2(),
 *         OnboardingPage3()
 *     )
 * )
 * ```
 *
 * @property controller An object that can be used to control the position to which this page view is scrolled
 * @property scrollDirection The axis along which the page view scrolls
 * @property reverse Whether the page view scrolls in the reading direction
 * @property physics How the page view should respond to user input
 * @property pageSnapping Set to false to disable page snapping, useful for custom scroll behavior
 * @property onPageChanged Called whenever the page changes
 * @property children The list of children widgets. Either use this or itemBuilder + itemCount
 * @property itemBuilder Called to build pages on demand. Use with itemCount for lazy loading
 * @property itemCount The number of pages. Use with itemBuilder for lazy loading
 * @property padEnds Whether to add padding to both ends of the page view
 * @property allowImplicitScrolling Whether the page view should implicitly scroll when focused
 *
 * @see PageController
 * @see TabBarView
 * @since 2.1.0
 */
@Serializable
data class PageViewComponent(
    val controller: PageController? = null,
    val scrollDirection: ScrollDirection = ScrollDirection.Horizontal,
    val reverse: Boolean = false,
    val physics: ScrollPhysics = ScrollPhysics.Platform,
    val pageSnapping: Boolean = true,
    val onPageChanged: String? = null, // Serialized callback reference
    val children: List<Any>? = null,
    val itemBuilder: String? = null, // Serialized function reference
    val itemCount: Int? = null,
    val padEnds: Boolean = true,
    val allowImplicitScrolling: Boolean = false
) {
    init {
        // Must have either children or itemBuilder + itemCount
        require((children != null) xor (itemBuilder != null)) {
            "Must provide either children or itemBuilder, not both"
        }

        if (itemBuilder != null) {
            require(itemCount != null || itemCount == -1) {
                "itemBuilder requires itemCount to be specified (use -1 for infinite)"
            }
        }

        itemCount?.let {
            if (it != -1) {
                require(it >= 0) { "itemCount must be non-negative or -1 for infinite, got $it" }
            }
        }
    }
}

/**
 * A controller for [PageView].
 *
 * A page controller lets you manipulate which page is visible in a [PageView].
 * In addition to being able to control the pixel offset of the content inside the [PageView],
 * a [PageController] also lets you control the offset in terms of pages, which are increments
 * of the viewport size.
 *
 * @property initialPage The page to show when first creating the [PageView]
 * @property keepPage Save the current page with [PageStorage] and restore it if the PageView is rebuilt
 * @property viewportFraction The fraction of the viewport that each page should occupy.
 *                           Defaults to 1.0, which means each page fills the viewport in the scrolling direction.
 *                           Values less than 1.0 can be used to create a preview effect.
 *
 * @see PageView
 * @since 2.1.0
 */
@Serializable
data class PageController(
    val initialPage: Int = 0,
    val keepPage: Boolean = true,
    val viewportFraction: Float = 1.0f
) {
    init {
        require(initialPage >= 0) { "initialPage must be non-negative, got $initialPage" }
        require(viewportFraction > 0 && viewportFraction <= 1.0f) {
            "viewportFraction must be between 0 (exclusive) and 1.0 (inclusive), got $viewportFraction"
        }
    }
}
