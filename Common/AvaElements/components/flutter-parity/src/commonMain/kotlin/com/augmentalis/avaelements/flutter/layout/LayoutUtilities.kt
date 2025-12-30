package com.augmentalis.avaelements.flutter.layout

import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Spacing

/**
 * Layout utilities for Flutter parity components.
 *
 * This file provides helper functions and utilities for working with layout components,
 * including constraint calculations, alignment helpers, and common layout patterns.
 *
 * @since 2.1.0
 */

/**
 * Calculate the final size after applying BoxFit strategy
 *
 * @param containerWidth The width of the container
 * @param containerHeight The height of the container
 * @param childWidth The intrinsic width of the child
 * @param childHeight The intrinsic height of the child
 * @param fit The BoxFit strategy to apply
 * @return Pair of (scaledWidth, scaledHeight)
 */
fun calculateFittedSize(
    containerWidth: Float,
    containerHeight: Float,
    childWidth: Float,
    childHeight: Float,
    fit: BoxFit
): Pair<Float, Float> {
    val containerAspectRatio = containerWidth / containerHeight
    val childAspectRatio = childWidth / childHeight

    return when (fit) {
        BoxFit.Fill -> {
            // Distort to fill exactly
            Pair(containerWidth, containerHeight)
        }

        BoxFit.Contain -> {
            // Scale to fit inside while maintaining aspect ratio
            if (childAspectRatio > containerAspectRatio) {
                // Child is wider, scale by width
                val scaledHeight = containerWidth / childAspectRatio
                Pair(containerWidth, scaledHeight)
            } else {
                // Child is taller, scale by height
                val scaledWidth = containerHeight * childAspectRatio
                Pair(scaledWidth, containerHeight)
            }
        }

        BoxFit.Cover -> {
            // Scale to cover while maintaining aspect ratio
            if (childAspectRatio > containerAspectRatio) {
                // Child is wider, scale by height
                val scaledWidth = containerHeight * childAspectRatio
                Pair(scaledWidth, containerHeight)
            } else {
                // Child is taller, scale by width
                val scaledHeight = containerWidth / childAspectRatio
                Pair(containerWidth, scaledHeight)
            }
        }

        BoxFit.FitWidth -> {
            // Make width match, let height overflow/underflow
            val scaledHeight = containerWidth / childAspectRatio
            Pair(containerWidth, scaledHeight)
        }

        BoxFit.FitHeight -> {
            // Make height match, let width overflow/underflow
            val scaledWidth = containerHeight * childAspectRatio
            Pair(scaledWidth, containerHeight)
        }

        BoxFit.None -> {
            // No scaling
            Pair(childWidth, childHeight)
        }

        BoxFit.ScaleDown -> {
            // Like None, but scale down if too large
            if (childWidth > containerWidth || childHeight > containerHeight) {
                // Need to scale down - use Contain logic
                if (childAspectRatio > containerAspectRatio) {
                    val scaledHeight = containerWidth / childAspectRatio
                    Pair(containerWidth, scaledHeight)
                } else {
                    val scaledWidth = containerHeight * childAspectRatio
                    Pair(scaledWidth, containerHeight)
                }
            } else {
                // Fits already, no scaling
                Pair(childWidth, childHeight)
            }
        }
    }
}

/**
 * Calculate alignment offset for positioning a child within a parent
 *
 * @param parentSize The size of the parent container
 * @param childSize The size of the child
 * @param alignment The alignment value (-1.0 to 1.0, where -1.0 is start, 0.0 is center, 1.0 is end)
 * @return The offset to apply to the child
 */
fun calculateAlignmentOffset(
    parentSize: Float,
    childSize: Float,
    alignment: Float
): Float {
    val freeSpace = parentSize - childSize
    return freeSpace * ((alignment + 1.0f) / 2.0f)
}

/**
 * Merge two BoxConstraints, taking the tighter constraint for each dimension
 *
 * @param parent Parent constraints
 * @param child Child constraints
 * @return Merged constraints
 */
fun mergeConstraints(parent: BoxConstraints, child: BoxConstraints): BoxConstraints {
    return BoxConstraints(
        minWidth = maxOf(parent.minWidth, child.minWidth),
        maxWidth = minOf(parent.maxWidth, child.maxWidth),
        minHeight = maxOf(parent.minHeight, child.minHeight),
        maxHeight = minOf(parent.maxHeight, child.maxHeight)
    )
}

/**
 * Calculate flex space distribution
 *
 * Given a total available space and a list of flex factors, calculate how much
 * space each flexible child should receive.
 *
 * @param totalSpace The total available space to distribute
 * @param flexFactors List of flex factors for each child
 * @return List of allocated spaces corresponding to each flex factor
 */
fun distributeFlexSpace(totalSpace: Float, flexFactors: List<Int>): List<Float> {
    val totalFlex = flexFactors.sum()
    if (totalFlex == 0) return flexFactors.map { 0f }

    val spacePerFlex = totalSpace / totalFlex
    return flexFactors.map { flex -> spacePerFlex * flex }
}

/**
 * Calculate space distribution for MainAxisAlignment
 *
 * @param totalSpace The total available space
 * @param childCount The number of children
 * @param alignment The main axis alignment
 * @return Triple of (leading space, spacing between items, trailing space)
 */
fun calculateMainAxisSpacing(
    totalSpace: Float,
    childCount: Int,
    alignment: MainAxisAlignment
): Triple<Float, Float, Float> {
    if (childCount == 0 || totalSpace <= 0) {
        return Triple(0f, 0f, 0f)
    }

    return when (alignment) {
        MainAxisAlignment.Start -> Triple(0f, 0f, totalSpace)

        MainAxisAlignment.End -> Triple(totalSpace, 0f, 0f)

        MainAxisAlignment.Center -> {
            val half = totalSpace / 2f
            Triple(half, 0f, half)
        }

        MainAxisAlignment.SpaceBetween -> {
            if (childCount == 1) {
                Triple(0f, 0f, totalSpace)
            } else {
                val spacing = totalSpace / (childCount - 1)
                Triple(0f, spacing, 0f)
            }
        }

        MainAxisAlignment.SpaceAround -> {
            val spacing = totalSpace / childCount
            val halfSpacing = spacing / 2f
            Triple(halfSpacing, spacing, halfSpacing)
        }

        MainAxisAlignment.SpaceEvenly -> {
            val spacing = totalSpace / (childCount + 1)
            Triple(spacing, spacing, spacing)
        }
    }
}

/**
 * Mirror alignment for RTL layout
 *
 * @param alignment Original alignment (-1.0 to 1.0)
 * @param isRtl Whether the layout is right-to-left
 * @return Mirrored alignment if RTL, original otherwise
 */
fun mirrorAlignmentForRtl(alignment: Float, isRtl: Boolean): Float {
    return if (isRtl) -alignment else alignment
}

/**
 * Convert alignment name to AlignmentGeometry
 *
 * Useful for parsing alignment from strings or DSL.
 *
 * @param name Alignment name (e.g., "topLeft", "center", "bottomEnd")
 * @return Corresponding AlignmentGeometry, or Center if not recognized
 */
fun alignmentFromString(name: String): AlignmentGeometry {
    return when (name.lowercase()) {
        "center" -> AlignmentGeometry.Center
        "topleft" -> AlignmentGeometry.TopLeft
        "topcenter" -> AlignmentGeometry.TopCenter
        "topright", "topend" -> AlignmentGeometry.TopEnd
        "centerleft", "centerstart" -> AlignmentGeometry.CenterLeft
        "centerright", "centerend" -> AlignmentGeometry.CenterEnd
        "bottomleft", "bottomstart" -> AlignmentGeometry.BottomLeft
        "bottomcenter" -> AlignmentGeometry.BottomCenter
        "bottomright", "bottomend" -> AlignmentGeometry.BottomEnd
        else -> AlignmentGeometry.Center
    }
}

/**
 * Convert BoxFit name to BoxFit enum
 *
 * Useful for parsing BoxFit from strings or DSL.
 *
 * @param name BoxFit name (e.g., "contain", "cover", "fill")
 * @return Corresponding BoxFit, or Contain if not recognized
 */
fun boxFitFromString(name: String): BoxFit {
    return when (name.lowercase()) {
        "fill" -> BoxFit.Fill
        "contain" -> BoxFit.Contain
        "cover" -> BoxFit.Cover
        "fitwidth" -> BoxFit.FitWidth
        "fitheight" -> BoxFit.FitHeight
        "none" -> BoxFit.None
        "scaledown" -> BoxFit.ScaleDown
        else -> BoxFit.Contain
    }
}

/**
 * Check if constraints allow for flexible sizing
 *
 * @param constraints The constraints to check
 * @return True if there's flexibility (min < max) in either dimension
 */
fun hasFlexibility(constraints: BoxConstraints): Boolean {
    return constraints.minWidth < constraints.maxWidth ||
            constraints.minHeight < constraints.maxHeight
}

/**
 * Constrain size to fit within constraints
 *
 * @param width The desired width
 * @param height The desired height
 * @param constraints The constraints to enforce
 * @return Pair of (constrainedWidth, constrainedHeight)
 */
fun constrainSize(
    width: Float,
    height: Float,
    constraints: BoxConstraints
): Pair<Float, Float> {
    return Pair(
        constraints.constrainWidth(width),
        constraints.constrainHeight(height)
    )
}

/**
 * Create constraints for a flexible child
 *
 * @param availableSpace The space available for this child
 * @param fit The FlexFit strategy (tight or loose)
 * @param crossAxisConstraints The constraints in the cross axis
 * @param isHorizontalFlex Whether this is a horizontal flex (Row) or vertical (Column)
 * @return BoxConstraints for the child
 */
fun createFlexChildConstraints(
    availableSpace: Float,
    fit: FlexFit,
    crossAxisConstraints: Pair<Float, Float>, // (min, max)
    isHorizontalFlex: Boolean
): BoxConstraints {
    return when (isHorizontalFlex) {
        true -> {
            // Horizontal flex (Row): flex controls width, cross axis is height
            when (fit) {
                FlexFit.Tight -> BoxConstraints(
                    minWidth = availableSpace,
                    maxWidth = availableSpace,
                    minHeight = crossAxisConstraints.first,
                    maxHeight = crossAxisConstraints.second
                )
                FlexFit.Loose -> BoxConstraints(
                    minWidth = 0f,
                    maxWidth = availableSpace,
                    minHeight = crossAxisConstraints.first,
                    maxHeight = crossAxisConstraints.second
                )
            }
        }
        false -> {
            // Vertical flex (Column): flex controls height, cross axis is width
            when (fit) {
                FlexFit.Tight -> BoxConstraints(
                    minWidth = crossAxisConstraints.first,
                    maxWidth = crossAxisConstraints.second,
                    minHeight = availableSpace,
                    maxHeight = availableSpace
                )
                FlexFit.Loose -> BoxConstraints(
                    minWidth = crossAxisConstraints.first,
                    maxWidth = crossAxisConstraints.second,
                    minHeight = 0f,
                    maxHeight = availableSpace
                )
            }
        }
    }
}

/**
 * Common spacer sizes
 */
object CommonSpacers {
    val Tiny = SizedBoxComponent(height = Size.dp(4f))
    val Small = SizedBoxComponent(height = Size.dp(8f))
    val Medium = SizedBoxComponent(height = Size.dp(16f))
    val Large = SizedBoxComponent(height = Size.dp(24f))
    val ExtraLarge = SizedBoxComponent(height = Size.dp(32f))

    val TinyHorizontal = SizedBoxComponent(width = Size.dp(4f))
    val SmallHorizontal = SizedBoxComponent(width = Size.dp(8f))
    val MediumHorizontal = SizedBoxComponent(width = Size.dp(16f))
    val LargeHorizontal = SizedBoxComponent(width = Size.dp(24f))
    val ExtraLargeHorizontal = SizedBoxComponent(width = Size.dp(32f))
}

/**
 * Common padding values
 */
object CommonPadding {
    val None = Spacing.Zero
    val Tiny = Spacing.all(4f)
    val Small = Spacing.all(8f)
    val Medium = Spacing.all(16f)
    val Large = Spacing.all(24f)
    val ExtraLarge = Spacing.all(32f)

    // Symmetric padding
    fun horizontal(value: Float) = Spacing.of(left = value, right = value, top = 0f, bottom = 0f)
    fun vertical(value: Float) = Spacing.of(left = 0f, right = 0f, top = value, bottom = value)

    // Edge-specific
    fun only(
        top: Float = 0f,
        right: Float = 0f,
        bottom: Float = 0f,
        left: Float = 0f
    ) = Spacing.of(top = top, right = right, bottom = bottom, left = left)
}

/**
 * Builder functions for common layout patterns
 */
object LayoutBuilders {
    /**
     * Create a horizontal spacer
     */
    fun horizontalSpacer(width: Float) = SizedBoxComponent(width = Size.dp(width))

    /**
     * Create a vertical spacer
     */
    fun verticalSpacer(height: Float) = SizedBoxComponent(height = Size.dp(height))

    /**
     * Create an expanding spacer (fills available space)
     */
    fun expandingSpacer() = ExpandedComponent(flex = 1, child = SizedBoxComponent())

    /**
     * Create a centered box with fixed size
     */
    fun centeredBox(width: Float, height: Float, child: Any) = CenterComponent(
        child = SizedBoxComponent(
            width = Size.dp(width),
            height = Size.dp(height),
            child = child
        )
    )

    /**
     * Create a padded container
     */
    fun paddedContainer(padding: Spacing, child: Any) = PaddingComponent(
        padding = padding,
        child = child
    )

    /**
     * Create a constrained aspect ratio box
     */
    fun aspectRatioBox(aspectRatio: Float, width: Float?, child: Any): Any {
        return if (width != null) {
            SizedBoxComponent(
                width = Size.dp(width),
                height = Size.dp(width / aspectRatio),
                child = child
            )
        } else {
            ConstrainedBoxComponent(
                constraints = BoxConstraints(
                    minWidth = 0f,
                    maxWidth = Float.POSITIVE_INFINITY,
                    minHeight = 0f,
                    maxHeight = Float.POSITIVE_INFINITY
                ),
                child = child
            )
        }
    }
}

/**
 * Extension functions for common operations
 */

/**
 * Check if an alignment is start-aligned
 */
fun AlignmentGeometry.isStart(): Boolean = when (this) {
    is AlignmentGeometry.Custom -> x == -1.0f
    AlignmentGeometry.TopLeft,
    AlignmentGeometry.CenterLeft,
    AlignmentGeometry.BottomLeft -> true
    else -> false
}

/**
 * Check if an alignment is end-aligned
 */
fun AlignmentGeometry.isEnd(): Boolean = when (this) {
    is AlignmentGeometry.Custom -> x == 1.0f
    AlignmentGeometry.TopEnd,
    AlignmentGeometry.CenterEnd,
    AlignmentGeometry.BottomEnd -> true
    else -> false
}

/**
 * Check if an alignment is center-aligned
 */
fun AlignmentGeometry.isCenter(): Boolean = when (this) {
    is AlignmentGeometry.Custom -> x == 0.0f && y == 0.0f
    AlignmentGeometry.Center -> true
    else -> false
}

/**
 * Flip alignment horizontally
 */
fun AlignmentGeometry.flipHorizontal(): AlignmentGeometry = when (this) {
    is AlignmentGeometry.Custom -> AlignmentGeometry.Custom(-x, y)
    AlignmentGeometry.TopLeft -> AlignmentGeometry.TopEnd
    AlignmentGeometry.TopEnd -> AlignmentGeometry.TopLeft
    AlignmentGeometry.CenterLeft -> AlignmentGeometry.CenterEnd
    AlignmentGeometry.CenterEnd -> AlignmentGeometry.CenterLeft
    AlignmentGeometry.BottomLeft -> AlignmentGeometry.BottomEnd
    AlignmentGeometry.BottomEnd -> AlignmentGeometry.BottomLeft
    else -> this
}

/**
 * Flip alignment vertically
 */
fun AlignmentGeometry.flipVertical(): AlignmentGeometry = when (this) {
    is AlignmentGeometry.Custom -> AlignmentGeometry.Custom(x, -y)
    AlignmentGeometry.TopLeft -> AlignmentGeometry.BottomLeft
    AlignmentGeometry.TopCenter -> AlignmentGeometry.BottomCenter
    AlignmentGeometry.TopEnd -> AlignmentGeometry.BottomEnd
    AlignmentGeometry.BottomLeft -> AlignmentGeometry.TopLeft
    AlignmentGeometry.BottomCenter -> AlignmentGeometry.TopCenter
    AlignmentGeometry.BottomEnd -> AlignmentGeometry.TopEnd
    else -> this
}
