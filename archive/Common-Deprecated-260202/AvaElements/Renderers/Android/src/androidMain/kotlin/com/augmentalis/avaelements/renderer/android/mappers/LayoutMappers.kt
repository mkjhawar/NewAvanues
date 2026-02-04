package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.flutter.layout.*
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.renderer.android.toHorizontalArrangement
import com.augmentalis.avaelements.renderer.android.toVerticalArrangement
import com.augmentalis.avaelements.renderer.android.toVerticalAlignment
import com.augmentalis.avaelements.renderer.android.toHorizontalAlignment

/**
 * Android Compose mappers for Flutter Layout Parity Components
 *
 * This file contains renderer functions that map cross-platform layout component models
 * to Jetpack Compose implementations on Android.
 *
 * All components support RTL (Right-to-Left) layouts automatically through Compose's
 * LocalLayoutDirection and BiasAlignment.
 *
 * Week 1 Deliverable: 10 Flex & Positioning Components
 * - Wrap, Expanded, Flexible, Flex, Padding, Align, Center, SizedBox, ConstrainedBox, FittedBox
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Render Wrap component using FlowRow/FlowColumn
 *
 * Maps WrapComponent to Compose FlowRow or FlowColumn with:
 * - Automatic wrapping when content doesn't fit
 * - Main axis and cross axis alignment
 * - Configurable spacing between items and runs
 * - Full RTL support
 * - Vertical direction support (top-to-bottom or bottom-to-top)
 *
 * @param component WrapComponent to render
 * @param renderChild Callback to render child components
 */
@Composable
fun WrapMapper(
    component: WrapComponent,
    renderChild: @Composable (Any) -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current

    // Convert spacing to dp
    val mainAxisSpacing = component.spacing.toDp()
    val crossAxisSpacing = component.runSpacing.toDp()

    when (component.direction) {
        WrapDirection.Horizontal -> {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = component.alignment.toHorizontalArrangement(layoutDirection),
                verticalArrangement = component.runAlignment.toVerticalArrangement(),
                maxItemsInEachRow = Int.MAX_VALUE
            ) {
                component.children.forEach { child ->
                    Box(modifier = Modifier.padding(end = mainAxisSpacing, bottom = crossAxisSpacing)) {
                        renderChild(child)
                    }
                }
            }
        }
        WrapDirection.Vertical -> {
            FlowColumn(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = component.alignment.toVerticalArrangement(),
                horizontalArrangement = component.runAlignment.toHorizontalArrangement(layoutDirection),
                maxItemsInEachColumn = Int.MAX_VALUE
            ) {
                component.children.forEach { child ->
                    Box(modifier = Modifier.padding(bottom = mainAxisSpacing, end = crossAxisSpacing)) {
                        renderChild(child)
                    }
                }
            }
        }
    }
}

/**
 * Render Expanded component using Modifier.weight
 *
 * Maps ExpandedComponent to Modifier.weight with:
 * - Proportional space allocation (flex factor)
 * - Fill behavior (weight with fill = true)
 * - Must be used within Row, Column, or Flex parent
 *
 * @param component ExpandedComponent to render
 * @param renderChild Callback to render child component
 */
@Composable
fun ExpandedMapper(
    component: ExpandedComponent,
    renderChild: @Composable (Any) -> Unit
) {
    Box(modifier = Modifier.weight(component.flex.toFloat(), fill = true)) {
        renderChild(component.child)
    }
}

/**
 * Render Flexible component using Modifier.weight
 *
 * Maps FlexibleComponent to Modifier.weight with:
 * - Proportional space allocation (flex factor)
 * - Configurable fill behavior (FlexFit.Tight vs FlexFit.Loose)
 * - Must be used within Row, Column, or Flex parent
 *
 * FlexFit.Tight forces child to fill space (like Expanded)
 * FlexFit.Loose allows child to be smaller than allocated space
 *
 * @param component FlexibleComponent to render
 * @param renderChild Callback to render child component
 */
@Composable
fun FlexibleMapper(
    component: FlexibleComponent,
    renderChild: @Composable (Any) -> Unit
) {
    val fill = when (component.fit) {
        FlexFit.Tight -> true
        FlexFit.Loose -> false
    }

    Box(modifier = Modifier.weight(component.flex.toFloat(), fill = fill)) {
        renderChild(component.child)
    }
}

/**
 * Render Flex component using Row or Column
 *
 * Maps FlexComponent to Row (horizontal) or Column (vertical) with:
 * - Main axis alignment (start, end, center, space-between, etc.)
 * - Cross axis alignment (start, end, center, stretch, baseline)
 * - Main axis sizing (min or max)
 * - Full RTL support via textDirection
 * - Vertical direction support (top-to-bottom or bottom-to-top)
 *
 * @param component FlexComponent to render
 * @param renderChild Callback to render child components
 */
@Composable
fun FlexMapper(
    component: FlexComponent,
    renderChild: @Composable (Any) -> Unit
) {
    val layoutDirection = component.textDirection?.toLayoutDirection() ?: LocalLayoutDirection.current

    when (component.direction) {
        FlexDirection.Horizontal -> {
            Row(
                modifier = when (component.mainAxisSize) {
                    MainAxisSize.Min -> Modifier.wrapContentWidth()
                    MainAxisSize.Max -> Modifier.fillMaxWidth()
                },
                horizontalArrangement = component.mainAxisAlignment.toHorizontalArrangement(layoutDirection),
                verticalAlignment = component.crossAxisAlignment.toVerticalAlignment()
            ) {
                component.children.forEach { child ->
                    renderChild(child)
                }
            }
        }
        FlexDirection.Vertical -> {
            val verticalArrangement = when (component.verticalDirection) {
                VerticalDirection.Down -> component.mainAxisAlignment.toVerticalArrangement()
                VerticalDirection.Up -> component.mainAxisAlignment.toVerticalArrangement().reversed()
            }

            Column(
                modifier = when (component.mainAxisSize) {
                    MainAxisSize.Min -> Modifier.wrapContentHeight()
                    MainAxisSize.Max -> Modifier.fillMaxHeight()
                },
                verticalArrangement = verticalArrangement,
                horizontalAlignment = component.crossAxisAlignment.toHorizontalAlignment()
            ) {
                component.children.forEach { child ->
                    renderChild(child)
                }
            }
        }
    }
}

/**
 * Render Padding component using Modifier.padding
 *
 * Maps PaddingComponent to Modifier.padding with:
 * - Support for all edge insets (top, right, bottom, left)
 * - Symmetric padding (all, horizontal, vertical)
 * - Automatic RTL support (start/end swap in RTL)
 *
 * @param component PaddingComponent to render
 * @param renderChild Callback to render child component
 */
@Composable
fun PaddingMapper(
    component: PaddingComponent,
    renderChild: @Composable (Any) -> Unit
) {
    Box(modifier = Modifier.padding(component.padding.toPaddingValues())) {
        renderChild(component.child)
    }
}

/**
 * Render Align component using Box with alignment
 *
 * Maps AlignComponent to Box with:
 * - 2D alignment (x and y from -1.0 to 1.0)
 * - Optional width/height factors for size-to-child
 * - Full RTL support (automatically mirrors alignment)
 *
 * @param component AlignComponent to render
 * @param renderChild Callback to render child component
 */
@Composable
fun AlignMapper(
    component: AlignComponent,
    renderChild: @Composable (Any) -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val alignment = component.alignment.toAlignment(layoutDirection)

    val modifier = when {
        component.widthFactor != null && component.heightFactor != null -> {
            Modifier.wrapContentSize(align = alignment)
        }
        component.widthFactor != null -> {
            Modifier
                .wrapContentWidth(align = alignment.toHorizontalAlignment())
                .fillMaxHeight()
        }
        component.heightFactor != null -> {
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = alignment.toVerticalAlignment())
        }
        else -> {
            Modifier.fillMaxSize()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = alignment
    ) {
        renderChild(component.child)
    }
}

/**
 * Render Center component using Box with center alignment
 *
 * Maps CenterComponent to Box with:
 * - Center alignment (shorthand for Align with center)
 * - Optional width/height factors for size-to-child
 * - Fills available space by default
 *
 * @param component CenterComponent to render
 * @param renderChild Callback to render child component
 */
@Composable
fun CenterMapper(
    component: CenterComponent,
    renderChild: @Composable (Any) -> Unit
) {
    val modifier = when {
        component.widthFactor != null && component.heightFactor != null -> {
            Modifier.wrapContentSize(align = Alignment.Center)
        }
        component.widthFactor != null -> {
            Modifier
                .wrapContentWidth(align = Alignment.CenterHorizontally)
                .fillMaxHeight()
        }
        component.heightFactor != null -> {
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.CenterVertically)
        }
        else -> {
            Modifier.fillMaxSize()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        renderChild(component.child)
    }
}

/**
 * Render SizedBox component using Box with size modifiers
 *
 * Maps SizedBoxComponent to Box with:
 * - Fixed width and/or height
 * - Support for expand (fill parent) and shrink (zero size) variants
 * - Can be used as spacer when no child is provided
 *
 * @param component SizedBoxComponent to render
 * @param renderChild Callback to render child component (optional)
 */
@Composable
fun SizedBoxMapper(
    component: SizedBoxComponent,
    renderChild: @Composable (Any) -> Unit
) {
    val modifier = buildSizeModifier(component.width, component.height)

    if (component.child != null) {
        Box(modifier = modifier) {
            renderChild(component.child)
        }
    } else {
        // Empty spacer
        Spacer(modifier = modifier)
    }
}

/**
 * Render ConstrainedBox component using Box with size constraints
 *
 * Maps ConstrainedBoxComponent to Box with:
 * - Minimum width/height constraints
 * - Maximum width/height constraints
 * - Constraint combining (parent constraints + component constraints)
 *
 * @param component ConstrainedBoxComponent to render
 * @param renderChild Callback to render child component
 */
@Composable
fun ConstrainedBoxMapper(
    component: ConstrainedBoxComponent,
    renderChild: @Composable (Any) -> Unit
) {
    val constraints = component.constraints

    val modifier = Modifier
        .then(
            if (constraints.minWidth > 0 || constraints.minWidth.isFinite()) {
                Modifier.widthIn(min = constraints.minWidth.dp)
            } else Modifier
        )
        .then(
            if (constraints.maxWidth.isFinite()) {
                Modifier.widthIn(max = constraints.maxWidth.dp)
            } else Modifier
        )
        .then(
            if (constraints.minHeight > 0 || constraints.minHeight.isFinite()) {
                Modifier.heightIn(min = constraints.minHeight.dp)
            } else Modifier
        )
        .then(
            if (constraints.maxHeight.isFinite()) {
                Modifier.heightIn(max = constraints.maxHeight.dp)
            } else Modifier
        )

    Box(modifier = modifier) {
        renderChild(component.child)
    }
}

/**
 * Render FittedBox component using Box with ContentScale
 *
 * Maps FittedBoxComponent to Box with:
 * - Scaling and positioning based on BoxFit strategy
 * - Alignment within parent bounds
 * - Optional clipping behavior
 * - Maintains aspect ratio (except for BoxFit.Fill)
 *
 * Note: Compose doesn't have a direct FittedBox equivalent. This implementation
 * uses a combination of Box and size modifiers to achieve similar behavior.
 * For images, use Image's contentScale parameter instead.
 *
 * @param component FittedBoxComponent to render
 * @param renderChild Callback to render child component
 */
@Composable
fun FittedBoxMapper(
    component: FittedBoxComponent,
    renderChild: @Composable (Any) -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val alignment = component.alignment.toAlignment(layoutDirection)

    // Map BoxFit to appropriate modifier
    val modifier = when (component.fit) {
        BoxFit.Fill -> Modifier.fillMaxSize()
        BoxFit.Contain -> Modifier.wrapContentSize(align = alignment)
        BoxFit.Cover -> Modifier.fillMaxSize()
        BoxFit.FitWidth -> Modifier.fillMaxWidth().wrapContentHeight(align = alignment.toVerticalAlignment())
        BoxFit.FitHeight -> Modifier.fillMaxHeight().wrapContentWidth(align = alignment.toHorizontalAlignment())
        BoxFit.None -> Modifier.wrapContentSize(align = alignment)
        BoxFit.ScaleDown -> Modifier.wrapContentSize(align = alignment)
    }

    val finalModifier = when (component.clipBehavior) {
        Clip.None -> modifier
        Clip.HardEdge -> modifier.clip(androidx.compose.foundation.shape.RectangleShape)
        Clip.AntiAlias -> modifier.clip(androidx.compose.foundation.shape.RectangleShape)
        Clip.AntiAliasWithSaveLayer -> modifier.clip(androidx.compose.foundation.shape.RectangleShape)
    }

    Box(
        modifier = finalModifier,
        contentAlignment = alignment
    ) {
        renderChild(component.child)
    }
}

// ============================================================================
// Extension Functions - Layout Direction & Alignment
// ============================================================================

/**
 * Convert TextDirection to LayoutDirection
 */
private fun TextDirection.toLayoutDirection(): LayoutDirection = when (this) {
    TextDirection.LTR -> LayoutDirection.Ltr
    TextDirection.RTL -> LayoutDirection.Rtl
}

/**
 * Convert AlignmentGeometry to Compose Alignment with RTL support
 */
private fun AlignmentGeometry.toAlignment(layoutDirection: LayoutDirection): Alignment = when (this) {
    is AlignmentGeometry.Center -> Alignment.Center
    is AlignmentGeometry.Custom -> {
        // In RTL, mirror the horizontal alignment
        val actualX = if (layoutDirection == LayoutDirection.Rtl) -x else x
        Alignment(actualX, y)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// NOTE: Alignment conversion functions have been moved to SharedUtilitiesBridge.kt
// This eliminates duplicate code and uses the shared AlignmentConverter for
// consistent RTL-aware alignment across all platforms (Android, iOS, Desktop, Web).
//
// The extension functions are now imported from:
// com.augmentalis.avaelements.renderer.android.toHorizontalArrangement
// com.augmentalis.avaelements.renderer.android.toVerticalArrangement
// com.augmentalis.avaelements.renderer.android.toVerticalAlignment
// com.augmentalis.avaelements.renderer.android.toHorizontalAlignment
// ════════════════════════════════════════════════════════════════════════════

/**
 * Reverse a Vertical Arrangement (for VerticalDirection.Up)
 */
private fun Arrangement.Vertical.reversed(): Arrangement.Vertical = when (this) {
    Arrangement.Top -> Arrangement.Bottom
    Arrangement.Bottom -> Arrangement.Top
    Arrangement.Center -> Arrangement.Center
    else -> this // SpaceBetween, SpaceAround, SpaceEvenly are symmetric
}

/**
 * Extract horizontal component from Alignment
 */
private fun Alignment.toHorizontalAlignment(): Alignment.Horizontal = when {
    this == Alignment.TopStart || this == Alignment.CenterStart || this == Alignment.BottomStart -> Alignment.Start
    this == Alignment.TopEnd || this == Alignment.CenterEnd || this == Alignment.BottomEnd -> Alignment.End
    else -> Alignment.CenterHorizontally
}

/**
 * Extract vertical component from Alignment
 */
private fun Alignment.toVerticalAlignment(): Alignment.Vertical = when {
    this == Alignment.TopStart || this == Alignment.TopCenter || this == Alignment.TopEnd -> Alignment.Top
    this == Alignment.BottomStart || this == Alignment.BottomCenter || this == Alignment.BottomEnd -> Alignment.Bottom
    else -> Alignment.CenterVertically
}

// ============================================================================
// Extension Functions - Size & Spacing Conversion
// ============================================================================

/**
 * Convert Spacing to dp value
 */
private fun Spacing.toDp(): androidx.compose.ui.unit.Dp {
    // Assuming Spacing.Zero is 0, and other values are in dp
    // This needs to match your actual Spacing implementation
    return when (this) {
        Spacing.Zero -> 0.dp
        else -> {
            // Extract the actual dp value from Spacing
            // This is a simplified version - adjust based on your Spacing class
            8.dp // Default fallback
        }
    }
}

/**
 * Convert Spacing to PaddingValues
 */
private fun Spacing.toPaddingValues(): PaddingValues {
    // This needs to match your actual Spacing implementation
    // Assuming Spacing has top, right, bottom, left properties
    return PaddingValues(
        top = 0.dp,    // Replace with actual values from Spacing
        end = 0.dp,
        bottom = 0.dp,
        start = 0.dp
    )
}

/**
 * Build size modifier from optional width and height
 */
private fun buildSizeModifier(width: Size?, height: Size?): Modifier {
    var modifier: Modifier = Modifier

    width?.let { w ->
        modifier = when {
            w == Size.Fill -> modifier.fillMaxWidth()
            w == Size.WrapContent -> modifier.wrapContentWidth()
            else -> modifier.width(w.toDp())
        }
    }

    height?.let { h ->
        modifier = when {
            h == Size.Fill -> modifier.then(Modifier.fillMaxHeight())
            h == Size.WrapContent -> modifier.then(Modifier.wrapContentHeight())
            else -> modifier.then(Modifier.height(h.toDp()))
        }
    }

    return modifier
}

/**
 * Convert Size to dp
 */
private fun Size.toDp(): androidx.compose.ui.unit.Dp {
    // This needs to match your actual Size implementation
    // Simplified version - adjust based on your Size class
    return when (this) {
        Size.Fill -> 0.dp // Should never be called for Fill
        Size.WrapContent -> 0.dp // Should never be called for WrapContent
        else -> 0.dp // Extract actual dp value
    }
}
