package com.augmentalis.avaelements.flutter.layout.scrolling

import com.augmentalis.avaelements.core.types.Spacing
import kotlinx.serialization.Serializable

/**
 * A list whose items the user can interactively reorder by dragging.
 *
 * This widget allows the user to reorder list items by long-pressing on an item and then
 * dragging it to a new position in the list. During the drag, a visual indicator shows
 * where the item will be placed when the user releases it.
 *
 * All list items must have a unique key.
 *
 * This is equivalent to Flutter's [ReorderableListView] widget.
 *
 * Example:
 * ```kotlin
 * val items = mutableListOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")
 *
 * ReorderableListView(
 *     itemCount = items.size,
 *     itemBuilder = { index ->
 *         ListTile(
 *             key = Key(items[index]),
 *             title = Text(items[index]),
 *             trailing = Icon(Icons.DragHandle)
 *         )
 *     },
 *     onReorder = { oldIndex, newIndex ->
 *         if (oldIndex < newIndex) {
 *             newIndex -= 1
 *         }
 *         val item = items.removeAt(oldIndex)
 *         items.add(newIndex, item)
 *     }
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * ReorderableListView(
 *   children: items.map((item) => ListTile(
 *     key: Key(item),
 *     title: Text(item),
 *     trailing: Icon(Icons.drag_handle),
 *   )).toList(),
 *   onReorder: (oldIndex, newIndex) {
 *     if (oldIndex < newIndex) {
 *       newIndex -= 1;
 *     }
 *     final item = items.removeAt(oldIndex);
 *     items.insert(newIndex, item);
 *   },
 * )
 * ```
 *
 * Example with builder:
 * ```kotlin
 * ReorderableListView.builder(
 *     itemCount = tasks.size,
 *     itemBuilder = { index ->
 *         TaskCard(
 *             key = Key(tasks[index].id),
 *             task = tasks[index]
 *         )
 *     },
 *     onReorder = { oldIndex, newIndex ->
 *         reorderTasks(oldIndex, newIndex)
 *     }
 * )
 * ```
 *
 * @property itemCount The number of items in the list
 * @property itemBuilder Called to build each item. Each item must have a unique key.
 * @property onReorder Called when an item is reordered. Parameters are the old and new indices.
 * @property controller An object that can be used to control the scroll position
 * @property scrollDirection The axis along which the list scrolls (typically vertical)
 * @property reverse Whether the list scrolls in the reading direction
 * @property padding The amount of space by which to inset the children
 * @property shrinkWrap Whether the extent of the scroll view should be determined by the contents
 * @property physics How the scroll view should respond to user input
 * @property proxyDecorator A callback that returns the widget to display while dragging.
 *                          If null, the original widget is displayed with reduced opacity.
 * @property buildDefaultDragHandles Whether to wrap each child in a drag handle. Defaults to true.
 * @property onReorderStart Called when the user starts dragging an item
 * @property onReorderEnd Called when the user releases a dragged item
 *
 * @see ListViewBuilder
 * @see DragHandle
 * @since 2.1.0
 */
@Serializable
data class ReorderableListViewComponent(
    val itemCount: Int,
    val itemBuilder: String, // Serialized function reference
    val onReorder: String, // Serialized callback reference (oldIndex, newIndex) -> Unit
    val controller: ScrollController? = null,
    val scrollDirection: ScrollDirection = ScrollDirection.Vertical,
    val reverse: Boolean = false,
    val padding: Spacing? = null,
    val shrinkWrap: Boolean = false,
    val physics: ScrollPhysics = ScrollPhysics.AlwaysScrollable,
    val proxyDecorator: String? = null, // Serialized function reference
    val buildDefaultDragHandles: Boolean = true,
    val onReorderStart: String? = null, // Serialized callback reference
    val onReorderEnd: String? = null // Serialized callback reference
) {
    init {
        require(itemCount >= 0) { "itemCount must be non-negative, got $itemCount" }
    }
}

/**
 * A widget that marks a region of a [ReorderableListView] as a place where the user can
 * grab to drag the item.
 *
 * This is typically used as a trailing icon in list items.
 *
 * @property child The widget below this widget in the tree
 *
 * @see ReorderableListView
 * @since 2.1.0
 */
@Serializable
data class ReorderableDragStartListener(
    val index: Int,
    val child: Any
) {
    init {
        require(index >= 0) { "index must be non-negative, got $index" }
    }
}
