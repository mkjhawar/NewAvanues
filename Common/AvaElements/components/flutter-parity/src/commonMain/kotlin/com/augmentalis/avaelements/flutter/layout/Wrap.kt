package com.augmentalis.avaelements.flutter.layout

import com.augmentalis.avaelements.core.types.Spacing
import kotlinx.serialization.Serializable

/**
 * A widget that displays its children in multiple horizontal or vertical runs.
 *
 * A [Wrap] lays out each child and attempts to place the child adjacent to the previous child in the main axis,
 * as long as there is room. If there is not enough space to fit the child, [Wrap] creates a new run adjacent
 * to the existing children in the cross axis.
 *
 * This is equivalent to Flutter's [Wrap] widget.
 *
 * Example:
 * ```kotlin
 * Wrap(
 *     direction = WrapDirection.Horizontal,
 *     alignment = WrapAlignment.Start,
 *     spacing = Spacing.all(8f),
 *     runSpacing = Spacing.all(4f),
 *     children = listOf(
 *         Chip("Tag 1"),
 *         Chip("Tag 2"),
 *         Chip("Tag 3")
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * Wrap(
 *   direction: Axis.horizontal,
 *   alignment: WrapAlignment.start,
 *   spacing: 8.0,
 *   runSpacing: 4.0,
 *   children: [
 *     Chip(label: Text('Tag 1')),
 *     Chip(label: Text('Tag 2')),
 *     Chip(label: Text('Tag 3')),
 *   ],
 * )
 * ```
 *
 * @property direction The direction to use as the main axis (horizontal or vertical)
 * @property alignment How the children within a run should be aligned along the main axis
 * @property spacing How much space to place between children in a run in the main axis
 * @property runSpacing How much space to place between the runs in the cross axis
 * @property runAlignment How the runs themselves should be placed in the cross axis
 * @property crossAxisAlignment How the children within a run should be aligned relative to each other in the cross axis
 * @property verticalDirection Determines the order to lay children out vertically
 * @property children The widgets to be laid out
 *
 * @see FlowRow
 * @see FlowColumn
 * @since 2.1.0
 */
@Serializable
data class WrapComponent(
    val direction: WrapDirection = WrapDirection.Horizontal,
    val alignment: WrapAlignment = WrapAlignment.Start,
    val spacing: Spacing = Spacing.Zero,
    val runSpacing: Spacing = Spacing.Zero,
    val runAlignment: WrapAlignment = WrapAlignment.Start,
    val crossAxisAlignment: WrapCrossAlignment = WrapCrossAlignment.Start,
    val verticalDirection: VerticalDirection = VerticalDirection.Down,
    val children: List<Any> = emptyList()
)

/**
 * The direction in which a [Wrap] lays out its children
 */
@Serializable
enum class WrapDirection {
    /**
     * Lay out children horizontally first, wrapping to new rows as needed
     */
    Horizontal,

    /**
     * Lay out children vertically first, wrapping to new columns as needed
     */
    Vertical
}

/**
 * How [Wrap] should align children within a run in the main axis
 */
@Serializable
enum class WrapAlignment {
    /**
     * Place children as close to the start of the main axis as possible
     */
    Start,

    /**
     * Place children as close to the end of the main axis as possible
     */
    End,

    /**
     * Place children as close to the middle of the main axis as possible
     */
    Center,

    /**
     * Place free space evenly between children
     */
    SpaceBetween,

    /**
     * Place free space evenly between children and at the ends
     */
    SpaceAround,

    /**
     * Place free space evenly between children and half of that space at the ends
     */
    SpaceEvenly
}

/**
 * How [Wrap] should align children within a run in the cross axis
 */
@Serializable
enum class WrapCrossAlignment {
    /**
     * Place children with their start edge aligned with the start side of the run in the cross axis
     */
    Start,

    /**
     * Place children as close to the end of the run in the cross axis as possible
     */
    End,

    /**
     * Place children as close to the middle of the run in the cross axis as possible
     */
    Center
}

/**
 * The vertical direction in which to lay out children
 */
@Serializable
enum class VerticalDirection {
    /**
     * Lay out children from top to bottom
     */
    Down,

    /**
     * Lay out children from bottom to top
     */
    Up
}
