package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable

/**
 * A scrolling list that animates items when they are inserted or removed.
 *
 * This widget is useful for animating changes to a list in place, such as adding or
 * removing items. To use it, you must provide an [itemBuilder] that builds the list
 * items and manages their animations.
 *
 * This is equivalent to Flutter's [AnimatedList] widget.
 *
 * Example:
 * ```kotlin
 * AnimatedList(
 *     items = listOf("Item 1", "Item 2", "Item 3"),
 *     itemBuilder = { item, index, animation ->
 *         SlideTransition(
 *             position = Offset(1.0f, 0.0f),
 *             child = ListTile(title = item)
 *         )
 *     },
 *     initialItemCount = 3
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * AnimatedList(
 *   initialItemCount: 3,
 *   itemBuilder: (context, index, animation) {
 *     return SlideTransition(
 *       position: animation.drive(
 *         Tween(begin: Offset(1.0, 0.0), end: Offset.zero),
 *       ),
 *       child: ListTile(title: Text('Item $index')),
 *     );
 *   },
 * )
 * ```
 *
 * Performance considerations:
 * - Animates only visible items for efficiency
 * - Uses GPU-accelerated transitions
 * - Targets 60 FPS for smooth transitions
 * - Recycles list items when scrolling
 *
 * @property items The list of items to display
 * @property itemBuilder Builder function that creates widgets for each item
 * @property initialItemCount The initial number of items in the list
 * @property scrollDirection The axis along which the list scrolls
 * @property reverse Whether the scroll view scrolls in the reading direction
 * @property padding Padding around the list
 * @property primary Whether this is the primary scroll view
 *
 * @see AnimatedListState
 * @see SliverAnimatedList
 * @see ListView
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class AnimatedList(
    val items: List<Any>,
    val itemBuilder: String,
    val initialItemCount: Int,
    val scrollDirection: Axis = Axis.Vertical,
    val reverse: Boolean = false,
    val padding: String? = null,
    val primary: Boolean = false
) {
    init {
        require(initialItemCount >= 0) { "initialItemCount must be non-negative, got $initialItemCount" }
    }

    /**
     * Returns accessibility description for this list.
     */
    fun getAccessibilityDescription(): String {
        return "Animated list with ${items.size} items"
    }

    /**
     * Scroll axis.
     */
    enum class Axis {
        Horizontal,
        Vertical
    }

    companion object {
        /**
         * Default animation duration in milliseconds for item insertion.
         */
        const val DEFAULT_INSERT_DURATION = 300

        /**
         * Default animation duration in milliseconds for item removal.
         */
        const val DEFAULT_REMOVE_DURATION = 300
    }
}
