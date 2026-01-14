package com.augmentalis.avaelements.flutter.layout.scrolling

import com.augmentalis.avaelements.core.types.Spacing
import kotlinx.serialization.Serializable

/**
 * A scrollable list of widgets separated by separator widgets, built on demand.
 *
 * [ListViewSeparated] is a variant of [ListViewBuilder] that automatically adds separator
 * widgets between items. This is commonly used for dividers between list items.
 *
 * The [itemBuilder] callback will be called with indices starting from 0 and increasing.
 * The [separatorBuilder] will be called with indices starting from 0, and the separator
 * at index i will be placed between item i and item i+1.
 *
 * This is equivalent to Flutter's [ListView.separated] constructor.
 *
 * Example with dividers:
 * ```kotlin
 * ListViewSeparated(
 *     itemCount = 20,
 *     itemBuilder = { index ->
 *         ListTile(
 *             title = Text("Item $index"),
 *             subtitle = Text("Description for item $index")
 *         )
 *     },
 *     separatorBuilder = { index ->
 *         Divider(height = Size.dp(1f))
 *     }
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * ListView.separated(
 *   itemCount: 20,
 *   itemBuilder: (context, index) {
 *     return ListTile(
 *       title: Text('Item $index'),
 *       subtitle: Text('Description for item $index'),
 *     );
 *   },
 *   separatorBuilder: (context, index) {
 *     return Divider(height: 1.0);
 *   },
 * )
 * ```
 *
 * Example with variable separators:
 * ```kotlin
 * ListViewSeparated(
 *     itemCount = 10,
 *     itemBuilder = { index ->
 *         Container(
 *             padding = Spacing.all(16f),
 *             child = Text("Item $index")
 *         )
 *     },
 *     separatorBuilder = { index ->
 *         // Different separators for even/odd indices
 *         if (index % 2 == 0) {
 *             Divider(color = Colors.Blue)
 *         } else {
 *             SizedBox(height = Size.dp(8f))
 *         }
 *     }
 * )
 * ```
 *
 * Note: The [separatorBuilder] is called n-1 times for n items. The last item will not have
 * a separator after it.
 *
 * @property itemCount The total number of items (excluding separators). Must be non-null and non-negative.
 * @property itemBuilder Called to build children for the list.
 * @property separatorBuilder Called to build separators between items. Called with indices from 0 to itemCount-2.
 * @property controller An object that can be used to control the scroll position.
 * @property scrollDirection The axis along which the scroll view scrolls.
 * @property reverse Whether the scroll view scrolls in the reading direction.
 * @property padding The amount of space by which to inset the children.
 * @property shrinkWrap Whether the extent of the scroll view should be determined by the contents.
 * @property physics How the scroll view should respond to user input.
 *
 * @see ListViewBuilder
 * @see Divider
 * @since 2.1.0
 */
@Serializable
data class ListViewSeparatedComponent(
    val itemCount: Int,
    val itemBuilder: String, // Serialized function reference or ID
    val separatorBuilder: String, // Serialized function reference or ID
    val controller: ScrollController? = null,
    val scrollDirection: ScrollDirection = ScrollDirection.Vertical,
    val reverse: Boolean = false,
    val padding: Spacing? = null,
    val shrinkWrap: Boolean = false,
    val physics: ScrollPhysics = ScrollPhysics.AlwaysScrollable
) {
    init {
        require(itemCount >= 0) { "itemCount must be non-negative, got $itemCount" }
    }
}
