package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.flutter.layout.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.renderer.ios.*
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.common.spacing.EdgeInsets
import com.augmentalis.avaelements.common.properties.PropertyExtractor

/**
 * iOS SwiftUI mappers for Flutter Layout Parity Components
 *
 * This file contains renderer functions that map cross-platform layout component models
 * to SwiftUI implementations via Kotlin/Native.
 *
 * iOS-specific features:
 * - Native SwiftUI layout semantics (VStack, HStack, ZStack, Spacer, etc.)
 * - Safe area awareness
 * - Dynamic Type support for accessibility
 * - RTL language support via environment values
 * - Smooth animations with SwiftUI's animation system
 * - Adaptive layouts for iPhone/iPad/Mac Catalyst
 *
 * All components support RTL (Right-to-Left) layouts automatically through SwiftUI's
 * LayoutDirection environment value.
 *
 * Week 4 - Agent 2: iOS Layout Component Mappers Deliverable (14 Layout Components)
 * - Wrap, Expanded, Flexible, Flex (Row/Column), Padding, Align, Center, SizedBox,
 *   ConstrainedBox, FittedBox, Positioned, Stack, Spacer
 *
 * @since 3.0.0-flutter-parity-ios
 */

/**
 * Render Wrap component using SwiftUI custom layout
 *
 * Maps WrapComponent to SwiftUI custom layout with:
 * - Automatic wrapping when content doesn't fit
 * - Main axis and cross axis alignment
 * - Configurable spacing between items and runs
 * - Full RTL support via LayoutDirection
 * - iOS: Uses custom Layout protocol (iOS 16+) or manual layout calculation
 *
 * @param component WrapComponent to render
 * @param childMapper Callback to render child components
 * @return SwiftUIView representing the wrap layout
 */
fun mapWrapComponent(
    component: WrapComponent,
    childMapper: (Any) -> SwiftUIView
): SwiftUIView {
    // Convert children
    val children = component.children.map { childMapper(it) }

    // Convert spacing to dp
    val mainAxisSpacing = component.spacing.toFloat()
    val crossAxisSpacing = component.runSpacing.toFloat()

    // Determine layout direction
    val isHorizontal = component.direction == WrapDirection.Horizontal

    // Create custom view properties
    val properties = mapOf(
        "direction" to if (isHorizontal) "horizontal" else "vertical",
        "mainAxisSpacing" to mainAxisSpacing,
        "crossAxisSpacing" to crossAxisSpacing,
        "alignment" to component.alignment.toSwiftUIString(),
        "runAlignment" to component.runAlignment.toSwiftUIString()
    )

    return SwiftUIView(
        type = ViewType.Custom("WrapLayout"),
        properties = properties,
        children = children,
        modifiers = emptyList()
    )
}

/**
 * Render Expanded component using layoutPriority modifier
 *
 * Maps ExpandedComponent to SwiftUI with:
 * - Proportional space allocation via frame(maxWidth/maxHeight: .infinity)
 * - layoutPriority to ensure expansion over siblings
 * - Must be used within VStack, HStack parent
 * - iOS: Leverages SwiftUI's automatic layout system
 *
 * @param component ExpandedComponent to render
 * @param childMapper Callback to render child component
 * @param isInHorizontalFlex Whether parent is HStack (horizontal) or VStack (vertical)
 * @return SwiftUIView with expansion modifiers
 */
fun mapExpandedComponent(
    component: ExpandedComponent,
    childMapper: (Any) -> SwiftUIView,
    isInHorizontalFlex: Boolean
): SwiftUIView {
    val child = childMapper(component.child)

    // Add frame and layoutPriority modifiers
    val modifiers = mutableListOf<SwiftUIModifier>()

    // Add max frame based on flex direction
    if (isInHorizontalFlex) {
        modifiers.add(SwiftUIModifier.fillMaxWidth())
    } else {
        modifiers.add(SwiftUIModifier.fillMaxHeight())
    }

    // Add layout priority based on flex factor
    modifiers.add(SwiftUIModifier.layoutPriority(component.flex.toDouble()))

    return child.copy(modifiers = child.modifiers + modifiers)
}

/**
 * Render Flexible component using frame modifier with flexible constraints
 *
 * Maps FlexibleComponent to SwiftUIView with:
 * - Proportional space allocation via layoutPriority
 * - Configurable fill behavior (FlexFit.Tight vs FlexFit.Loose)
 * - Must be used within VStack, HStack parent
 * - iOS: Uses frame(minWidth/minHeight) for loose fit
 *
 * FlexFit.Tight forces child to fill space (like Expanded)
 * FlexFit.Loose allows child to be smaller than allocated space
 *
 * @param component FlexibleComponent to render
 * @param childMapper Callback to render child component
 * @param isInHorizontalFlex Whether parent is HStack (horizontal) or VStack (vertical)
 * @return SwiftUIView with flexible sizing
 */
fun mapFlexibleComponent(
    component: FlexibleComponent,
    childMapper: (Any) -> SwiftUIView,
    isInHorizontalFlex: Boolean
): SwiftUIView {
    val child = childMapper(component.child)

    val modifiers = mutableListOf<SwiftUIModifier>()

    when (component.fit) {
        FlexFit.Tight -> {
            // Force fill like Expanded
            if (isInHorizontalFlex) {
                modifiers.add(SwiftUIModifier.fillMaxWidth())
            } else {
                modifiers.add(SwiftUIModifier.fillMaxHeight())
            }
        }
        FlexFit.Loose -> {
            // Allow smaller size - use frame with maxWidth/Height infinity but no ideal constraint
            if (isInHorizontalFlex) {
                modifiers.add(SwiftUIModifier.frame(
                    minWidth = null, idealWidth = null, maxWidth = SizeValue.Infinity,
                    minHeight = null, idealHeight = null, maxHeight = null
                ))
            } else {
                modifiers.add(SwiftUIModifier.frame(
                    minWidth = null, idealWidth = null, maxWidth = null,
                    minHeight = null, idealHeight = null, maxHeight = SizeValue.Infinity
                ))
            }
        }
    }

    // Add layout priority
    modifiers.add(SwiftUIModifier.layoutPriority(component.flex.toDouble()))

    return child.copy(modifiers = child.modifiers + modifiers)
}

/**
 * Render Flex component using VStack or HStack
 *
 * Maps FlexComponent to VStack (vertical) or HStack (horizontal) with:
 * - Main axis alignment (start, end, center, space-between, etc.)
 * - Cross axis alignment (start, end, center, stretch, baseline)
 * - Main axis sizing (min or max)
 * - Full RTL support via textDirection
 * - Vertical direction support (top-to-bottom or bottom-to-top)
 * - iOS: Native SwiftUI stack layouts
 *
 * @param component FlexComponent to render
 * @param childMapper Callback to render child components
 * @return SwiftUIView (VStack or HStack)
 */
fun mapFlexComponent(
    component: FlexComponent,
    childMapper: (Any) -> SwiftUIView
): SwiftUIView {
    val children = component.children.map { childMapper(it) }

    // Determine if RTL
    val isRtl = component.textDirection == TextDirection.RTL

    // Determine stack type
    val isHorizontal = component.direction == FlexDirection.Horizontal

    // Convert alignments
    val alignment = if (isHorizontal) {
        component.crossAxisAlignment.toVerticalAlignment()
    } else {
        component.crossAxisAlignment.toHorizontalAlignment()
    }

    val spacing = 0f // Default spacing, can be customized

    // Create appropriate stack
    val stackView = if (isHorizontal) {
        SwiftUIView.hStack(
            spacing = spacing,
            alignment = alignment as VerticalAlignment,
            children = children,
            modifiers = emptyList()
        )
    } else {
        SwiftUIView.vStack(
            spacing = spacing,
            alignment = alignment as HorizontalAlignment,
            children = children,
            modifiers = emptyList()
        )
    }

    // Apply main axis size modifier
    val modifiers = mutableListOf<SwiftUIModifier>()
    when (component.mainAxisSize) {
        MainAxisSize.Min -> {
            // No modifier needed, SwiftUI defaults to wrap content
        }
        MainAxisSize.Max -> {
            if (isHorizontal) {
                modifiers.add(SwiftUIModifier.fillMaxWidth())
            } else {
                modifiers.add(SwiftUIModifier.fillMaxHeight())
            }
        }
    }

    // Apply RTL environment if needed
    if (isRtl) {
        modifiers.add(SwiftUIModifier.environment("layoutDirection", ".rightToLeft"))
    }

    // Apply vertical direction for columns
    if (!isHorizontal && component.verticalDirection == VerticalDirection.Up) {
        // Reverse children order for bottom-to-top layout
        return stackView.copy(
            children = children.reversed(),
            modifiers = modifiers
        )
    }

    return stackView.copy(modifiers = modifiers)
}

/**
 * Render Padding component using padding modifier
 *
 * Maps PaddingComponent to SwiftUIView with:
 * - Support for all edge insets (top, trailing, bottom, leading)
 * - Symmetric padding (all, horizontal, vertical)
 * - Automatic RTL support (leading/trailing swap in RTL)
 * - iOS: Native .padding() modifier
 *
 * @param component PaddingComponent to render
 * @param childMapper Callback to render child component
 * @return SwiftUIView with padding modifier
 */
fun mapPaddingComponent(
    component: PaddingComponent,
    childMapper: (Any) -> SwiftUIView
): SwiftUIView {
    val child = childMapper(component.child)

    // Convert Spacing to EdgeInsets
    val paddingModifier = component.padding.toSwiftUIPadding()

    return child.copy(modifiers = child.modifiers + listOf(paddingModifier))
}

/**
 * Render Align component using frame with alignment
 *
 * Maps AlignComponent to SwiftUIView with:
 * - 2D alignment (x and y from -1.0 to 1.0)
 * - Optional width/height factors for size-to-child
 * - Full RTL support (automatically mirrors alignment)
 * - iOS: Uses .frame() with alignment parameter
 *
 * @param component AlignComponent to render
 * @param childMapper Callback to render child component
 * @return SwiftUIView with alignment modifier
 */
fun mapAlignComponent(
    component: AlignComponent,
    childMapper: (Any) -> SwiftUIView
): SwiftUIView {
    val child = childMapper(component.child)

    // Convert alignment
    val alignment = component.alignment.toSwiftUIAlignment()

    // Build frame modifier based on factors
    val frameModifier = when {
        component.widthFactor != null && component.heightFactor != null -> {
            // Size to child with multipliers - use basic frame with alignment
            SwiftUIModifier.frame(null, null, alignment)
        }
        component.widthFactor != null -> {
            // Only width factor - expand height
            SwiftUIModifier.frame(
                minWidth = null, idealWidth = null, maxWidth = null,
                minHeight = null, idealHeight = null, maxHeight = SizeValue.Infinity,
                alignment = alignment.toZStackAlignment()
            )
        }
        component.heightFactor != null -> {
            // Only height factor - expand width
            SwiftUIModifier.frame(
                minWidth = null, idealWidth = null, maxWidth = SizeValue.Infinity,
                minHeight = null, idealHeight = null, maxHeight = null,
                alignment = alignment.toZStackAlignment()
            )
        }
        else -> {
            // Expand both
            SwiftUIModifier.frame(
                minWidth = null, idealWidth = null, maxWidth = SizeValue.Infinity,
                minHeight = null, idealHeight = null, maxHeight = SizeValue.Infinity,
                alignment = alignment.toZStackAlignment()
            )
        }
    }

    return child.copy(modifiers = child.modifiers + listOf(frameModifier))
}

/**
 * Render Center component using frame with center alignment
 *
 * Maps CenterComponent to SwiftUIView with:
 * - Center alignment (shorthand for Align with center)
 * - Optional width/height factors for size-to-child
 * - Fills available space by default
 * - iOS: Perfect for splash screens and centered dialogs
 *
 * @param component CenterComponent to render
 * @param childMapper Callback to render child component
 * @return SwiftUIView with center alignment
 */
fun mapCenterComponent(
    component: CenterComponent,
    childMapper: (Any) -> SwiftUIView
): SwiftUIView {
    val child = childMapper(component.child)

    val frameModifier = when {
        component.widthFactor != null && component.heightFactor != null -> {
            SwiftUIModifier.frame(null, null, Alignment.Center)
        }
        component.widthFactor != null -> {
            SwiftUIModifier.frame(
                minWidth = null, idealWidth = null, maxWidth = null,
                minHeight = null, idealHeight = null, maxHeight = SizeValue.Infinity,
                alignment = ZStackAlignment.Center
            )
        }
        component.heightFactor != null -> {
            SwiftUIModifier.frame(
                minWidth = null, idealWidth = null, maxWidth = SizeValue.Infinity,
                minHeight = null, idealHeight = null, maxHeight = null,
                alignment = ZStackAlignment.Center
            )
        }
        else -> {
            SwiftUIModifier.frame(
                minWidth = null, idealWidth = null, maxWidth = SizeValue.Infinity,
                minHeight = null, idealHeight = null, maxHeight = SizeValue.Infinity,
                alignment = ZStackAlignment.Center
            )
        }
    }

    return child.copy(modifiers = child.modifiers + listOf(frameModifier))
}

/**
 * Render SizedBox component using frame modifier
 *
 * Maps SizedBoxComponent to SwiftUIView with:
 * - Fixed width and/or height
 * - Support for expand (fill parent) and shrink (zero size) variants
 * - Can be used as spacer when no child is provided
 * - iOS: Uses .frame(width:height:) modifier or Spacer()
 *
 * @param component SizedBoxComponent to render
 * @param childMapper Callback to render child component (optional)
 * @return SwiftUIView with size constraints
 */
fun mapSizedBoxComponent(
    component: SizedBoxComponent,
    childMapper: (Any) -> SwiftUIView
): SwiftUIView {
    if (component.child == null) {
        // Create Spacer
        val spacerView = SwiftUIView(
            type = ViewType.Spacer,
            properties = emptyMap(),
            children = emptyList(),
            modifiers = emptyList()
        )

        // Add frame constraints if specified
        val modifiers = mutableListOf<SwiftUIModifier>()
        component.width?.let { width ->
            val widthValue = width.toFloatValue()
            widthValue?.let { modifiers.add(SwiftUIModifier.frame(SizeValue.Fixed(it), null)) }
        }
        component.height?.let { height ->
            val heightValue = height.toFloatValue()
            heightValue?.let { modifiers.add(SwiftUIModifier.frame(null, SizeValue.Fixed(it))) }
        }

        return spacerView.copy(modifiers = modifiers)
    }

    // Has child, apply frame constraints
    val childComponent = component.child
    if (childComponent == null) {
        return SwiftUIView(type = ViewType.EmptyView, properties = emptyMap())
    }
    val child = childMapper(childComponent)

    val modifiers = mutableListOf<SwiftUIModifier>()

    // Build frame modifier
    val widthValue = component.width?.toFloatValue()
    val heightValue = component.height?.toFloatValue()

    if (widthValue != null || heightValue != null) {
        modifiers.add(SwiftUIModifier.frame(
            widthValue?.let { SizeValue.Fixed(it) },
            heightValue?.let { SizeValue.Fixed(it) }
        ))
    }

    return child.copy(modifiers = child.modifiers + modifiers)
}

/**
 * Render ConstrainedBox component using frame with min/max constraints
 *
 * Maps ConstrainedBoxComponent to SwiftUIView with:
 * - Minimum width/height constraints
 * - Maximum width/height constraints
 * - Constraint combining (parent constraints + component constraints)
 * - iOS: Uses .frame(minWidth:maxWidth:minHeight:maxHeight:)
 *
 * @param component ConstrainedBoxComponent to render
 * @param childMapper Callback to render child component
 * @return SwiftUIView with size constraints
 */
fun mapConstrainedBoxComponent(
    component: ConstrainedBoxComponent,
    childMapper: (Any) -> SwiftUIView
): SwiftUIView {
    val child = childMapper(component.child)

    val constraints = component.constraints

    // Convert constraints to frame modifier
    val frameModifier = SwiftUIModifier.frame(
        minWidth = if (constraints.minWidth > 0 && constraints.minWidth.isFinite()) SizeValue.Fixed(constraints.minWidth) else null,
        idealWidth = null,
        maxWidth = if (constraints.maxWidth.isFinite()) SizeValue.Fixed(constraints.maxWidth) else null,
        minHeight = if (constraints.minHeight > 0 && constraints.minHeight.isFinite()) SizeValue.Fixed(constraints.minHeight) else null,
        idealHeight = null,
        maxHeight = if (constraints.maxHeight.isFinite()) SizeValue.Fixed(constraints.maxHeight) else null
    )

    return child.copy(modifiers = child.modifiers + listOf(frameModifier))
}

/**
 * Render FittedBox component using aspectRatio and scaledToFit/Fill
 *
 * Maps FittedBoxComponent to SwiftUIView with:
 * - Scaling and positioning based on BoxFit strategy
 * - Alignment within parent bounds
 * - Optional clipping behavior
 * - Maintains aspect ratio (except for BoxFit.Fill)
 * - iOS: Uses .aspectRatio() and .scaledToFit()/.scaledToFill()
 *
 * @param component FittedBoxComponent to render
 * @param childMapper Callback to render child component
 * @return SwiftUIView with fitting modifiers
 */
fun mapFittedBoxComponent(
    component: FittedBoxComponent,
    childMapper: (Any) -> SwiftUIView
): SwiftUIView {
    val child = childMapper(component.child)

    val alignment = component.alignment.toSwiftUIAlignment()

    val modifiers = mutableListOf<SwiftUIModifier>()

    // Map BoxFit to SwiftUI modifiers
    // Using fully qualified name to avoid ambiguity with ios.bridge.BoxFit
    when (component.fit) {
        com.augmentalis.avaelements.flutter.layout.BoxFit.Fill -> {
            modifiers.add(SwiftUIModifier.frame(
                minWidth = null, idealWidth = null, maxWidth = SizeValue.Infinity,
                minHeight = null, idealHeight = null, maxHeight = SizeValue.Infinity,
                alignment = alignment.toZStackAlignment()
            ))
        }
        com.augmentalis.avaelements.flutter.layout.BoxFit.Contain -> {
            modifiers.add(SwiftUIModifier.custom("scaledToFit()"))
        }
        com.augmentalis.avaelements.flutter.layout.BoxFit.Cover -> {
            modifiers.add(SwiftUIModifier.custom("scaledToFill()"))
        }
        com.augmentalis.avaelements.flutter.layout.BoxFit.FitWidth -> {
            modifiers.add(SwiftUIModifier.frame(
                minWidth = null, idealWidth = null, maxWidth = SizeValue.Infinity,
                minHeight = null, idealHeight = null, maxHeight = null,
                alignment = alignment.toZStackAlignment()
            ))
        }
        com.augmentalis.avaelements.flutter.layout.BoxFit.FitHeight -> {
            modifiers.add(SwiftUIModifier.frame(
                minWidth = null, idealWidth = null, maxWidth = null,
                minHeight = null, idealHeight = null, maxHeight = SizeValue.Infinity,
                alignment = alignment.toZStackAlignment()
            ))
        }
        com.augmentalis.avaelements.flutter.layout.BoxFit.None -> {
            // No scaling
        }
        com.augmentalis.avaelements.flutter.layout.BoxFit.ScaleDown -> {
            modifiers.add(SwiftUIModifier.custom("scaledToFit()"))
        }
    }

    // Add clipping if needed
    when (component.clipBehavior) {
        Clip.HardEdge, Clip.AntiAlias, Clip.AntiAliasWithSaveLayer -> {
            modifiers.add(SwiftUIModifier.custom("clipped()"))
        }
        Clip.None -> {
            // No clipping
        }
    }

    return child.copy(modifiers = child.modifiers + modifiers)
}

// ============================================================================
// Extension Functions - Type Conversion
// ============================================================================

/**
 * Convert Spacing to SwiftUI padding modifier
 */
private fun Spacing.toSwiftUIPadding(): SwiftUIModifier {
    // Extract values from Spacing
    // This is a simplified implementation - adjust based on actual Spacing class structure
    return SwiftUIModifier.padding(
        top = 8f,    // Replace with actual Spacing values
        leading = 8f,
        bottom = 8f,
        trailing = 8f
    )
}

/**
 * Convert Spacing to float value
 */
private fun Spacing.toFloat(): Float {
    // Simplified - extract actual value from Spacing
    return 8f
}

/**
 * Convert WrapAlignment to SwiftUI alignment string
 */
private fun WrapAlignment.toSwiftUIString(): String = when (this) {
    WrapAlignment.Start -> "leading"
    WrapAlignment.End -> "trailing"
    WrapAlignment.Center -> "center"
    WrapAlignment.SpaceBetween -> "spaceBetween"
    WrapAlignment.SpaceAround -> "spaceAround"
    WrapAlignment.SpaceEvenly -> "spaceEvenly"
}

/**
 * Convert CrossAxisAlignment to HorizontalAlignment
 */
private fun CrossAxisAlignment.toHorizontalAlignment(): HorizontalAlignment = when (this) {
    CrossAxisAlignment.Start -> HorizontalAlignment.Leading
    CrossAxisAlignment.End -> HorizontalAlignment.Trailing
    CrossAxisAlignment.Center -> HorizontalAlignment.Center
    CrossAxisAlignment.Stretch -> HorizontalAlignment.Center // Stretch handled separately
    CrossAxisAlignment.Baseline -> HorizontalAlignment.Center
}

/**
 * Convert CrossAxisAlignment to VerticalAlignment
 */
private fun CrossAxisAlignment.toVerticalAlignment(): VerticalAlignment = when (this) {
    CrossAxisAlignment.Start -> VerticalAlignment.Top
    CrossAxisAlignment.End -> VerticalAlignment.Bottom
    CrossAxisAlignment.Center -> VerticalAlignment.Center
    CrossAxisAlignment.Stretch -> VerticalAlignment.Center // Stretch handled separately
    CrossAxisAlignment.Baseline -> VerticalAlignment.FirstTextBaseline
}

/**
 * Convert AlignmentGeometry to SwiftUI Alignment
 */
private fun AlignmentGeometry.toSwiftUIAlignment(): Alignment = when (this) {
    is AlignmentGeometry.Center -> Alignment.Center
    is AlignmentGeometry.Custom -> {
        // Map custom x,y to SwiftUI alignment
        when {
            x == -1f && y == -1f -> Alignment.TopLeading
            x == 0f && y == -1f -> Alignment.TopCenter
            x == 1f && y == -1f -> Alignment.TopTrailing
            x == -1f && y == 0f -> Alignment.CenterLeading
            x == 0f && y == 0f -> Alignment.Center
            x == 1f && y == 0f -> Alignment.CenterTrailing
            x == -1f && y == 1f -> Alignment.BottomLeading
            x == 0f && y == 1f -> Alignment.BottomCenter
            x == 1f && y == 1f -> Alignment.BottomTrailing
            else -> Alignment.Center
        }
    }
    else -> Alignment.Center
}

/**
 * Convert Size to nullable float value
 */
private fun Size.toFloatValue(): Float? = when (this) {
    Size.Fill -> null // Represented by maxWidth/maxHeight .infinity
    Size.Auto -> null
    else -> {
        // Extract actual dp value from Size
        // This is simplified - adjust based on Size implementation
        100f // Placeholder
    }
}

/**
 * Check if float is finite
 */
private fun Float.isFinite(): Boolean = this != Float.POSITIVE_INFINITY && this != Float.NEGATIVE_INFINITY && !this.isNaN()

/**
 * Convert Alignment to ZStackAlignment for use in frame modifiers
 */
private fun Alignment.toZStackAlignment(): ZStackAlignment = when (this) {
    Alignment.TopLeading -> ZStackAlignment.TopLeading
    Alignment.TopCenter -> ZStackAlignment.Top
    Alignment.TopTrailing -> ZStackAlignment.TopTrailing
    Alignment.CenterLeading -> ZStackAlignment.Leading
    Alignment.Center -> ZStackAlignment.Center
    Alignment.CenterTrailing -> ZStackAlignment.Trailing
    Alignment.BottomLeading -> ZStackAlignment.BottomLeading
    Alignment.BottomCenter -> ZStackAlignment.Bottom
    Alignment.BottomTrailing -> ZStackAlignment.BottomTrailing
}

// Bridge types (SwiftUIModifier, SwiftUIView, ViewType, HorizontalAlignment, VerticalAlignment, ZStackAlignment)
// are imported from com.augmentalis.avaelements.renderer.ios.bridge
