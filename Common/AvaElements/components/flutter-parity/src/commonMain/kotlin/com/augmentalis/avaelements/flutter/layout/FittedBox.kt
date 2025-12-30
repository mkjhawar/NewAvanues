package com.augmentalis.avaelements.flutter.layout

import kotlinx.serialization.Serializable

/**
 * Scales and positions its child within itself according to [fit].
 *
 * [FittedBox] tries, in order:
 * 1. to fit the child into its own bounds by scaling it
 * 2. to position the scaled child according to the [alignment]
 *
 * This is useful for situations where you have a child that's naturally sized (like an image or text)
 * but you want to force it to fit into a specific space while maintaining its aspect ratio.
 *
 * This is equivalent to Flutter's [FittedBox] widget.
 *
 * Example - Scale down an oversized widget:
 * ```kotlin
 * FittedBox(
 *     fit = BoxFit.Contain,
 *     child = Image(url = "large_logo.png")
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * FittedBox(
 *   fit: BoxFit.contain,
 *   child: Image(url: 'large_logo.png'),
 * )
 * ```
 *
 * Example - Force widget to fill area:
 * ```kotlin
 * SizedBox(
 *     width = Size.dp(200f),
 *     height = Size.dp(100f),
 *     child = FittedBox(
 *         fit = BoxFit.Fill,
 *         child = Text("Stretched Text")
 *     )
 * )
 * ```
 *
 * Example - Center and cover:
 * ```kotlin
 * Container(
 *     width = Size.dp(300f),
 *     height = Size.dp(200f),
 *     child = FittedBox(
 *         fit = BoxFit.Cover,
 *         alignment = AlignmentGeometry.Center,
 *         child = Image(url = "photo.jpg")
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * Container(
 *   width: 300,
 *   height: 200,
 *   child: FittedBox(
 *     fit: BoxFit.cover,
 *     alignment: Alignment.center,
 *     child: Image(url: 'photo.jpg'),
 *   ),
 * )
 * ```
 *
 * @property fit How to inscribe the child into the space allocated during layout
 * @property alignment How to align the child within its parent's bounds when the child is smaller.
 *                     Defaults to [AlignmentGeometry.Center]
 * @property clipBehavior The content will be clipped (or not) according to this option.
 *                        Defaults to [Clip.None]
 * @property child The widget below this widget in the tree
 *
 * @see BoxFit
 * @see Image
 * @see AlignmentGeometry
 * @since 2.1.0
 */
@Serializable
data class FittedBoxComponent(
    val fit: BoxFit = BoxFit.Contain,
    val alignment: AlignmentGeometry = AlignmentGeometry.Center,
    val clipBehavior: Clip = Clip.None,
    val child: Any
)

/**
 * How a box should be inscribed into another box.
 *
 * This corresponds to Flutter's BoxFit enum.
 *
 * See also:
 * - [FittedBox], which uses this to scale and position its child
 * - [Image], which also uses this to determine how an image should be painted onto the canvas
 *
 * @since 2.1.0
 */
@Serializable
enum class BoxFit {
    /**
     * Fill the target box by distorting the source's aspect ratio.
     *
     * This will stretch or squash the content to exactly fill the destination box,
     * potentially changing its aspect ratio.
     */
    Fill,

    /**
     * As large as possible while still containing the source entirely within the target box.
     *
     * The source will be scaled down to fit within the target box while maintaining its aspect ratio.
     * This is the default behavior.
     *
     * If the aspect ratios don't match, there will be gaps on either the top/bottom or left/right.
     */
    Contain,

    /**
     * As small as possible while still covering the entire target box.
     *
     * The source will be scaled up to completely cover the target box while maintaining its aspect ratio.
     * This may result in parts of the source being clipped.
     *
     * If the aspect ratios don't match, the source will be clipped on either the top/bottom or left/right.
     */
    Cover,

    /**
     * Make sure the full width of the source is shown, regardless of whether this means
     * the source overflows the target box vertically.
     *
     * The source's width will match the target's width, but the height may overflow or leave gaps.
     */
    FitWidth,

    /**
     * Make sure the full height of the source is shown, regardless of whether this means
     * the source overflows the target box horizontally.
     *
     * The source's height will match the target's height, but the width may overflow or leave gaps.
     */
    FitHeight,

    /**
     * Align the source within the target box (no scaling) and clip or leave gaps.
     *
     * The source is displayed at its natural/intrinsic size. If it's larger than the target,
     * it will be clipped. If smaller, there will be gaps.
     */
    None,

    /**
     * Align the source within the target box (scaling down if necessary) and clip or leave gaps.
     *
     * This is like [None], but if the source is larger than the target, it will be scaled down
     * to fit (while maintaining aspect ratio). If smaller, it won't be scaled up.
     */
    ScaleDown
}

/**
 * A description of how widgets should be clipped.
 *
 * This corresponds to Flutter's Clip enum.
 *
 * @since 2.1.0
 */
@Serializable
enum class Clip {
    /**
     * No clip at all.
     *
     * This is the fastest option, but may allow content to overflow the widget's bounds.
     */
    None,

    /**
     * Clip to the axis-aligned bounding box of the widget.
     *
     * This is relatively fast and is usually good enough for rectangular content.
     */
    HardEdge,

    /**
     * Clip with anti-aliasing applied for smoother edges.
     *
     * This is slower than [HardEdge] but produces better-looking results,
     * especially for non-rectangular shapes.
     */
    AntiAlias,

    /**
     * Clip with anti-aliasing and save layer for blend modes.
     *
     * This is the slowest option but provides the highest quality clipping,
     * particularly when used with blend modes or transparency.
     */
    AntiAliasWithSaveLayer
}
