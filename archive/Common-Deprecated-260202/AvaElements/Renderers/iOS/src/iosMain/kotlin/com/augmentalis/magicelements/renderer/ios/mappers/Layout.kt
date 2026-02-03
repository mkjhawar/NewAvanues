package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.flutter.layout.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Flutter Layout Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity layout components to SwiftUI equivalents:
 * - AlignComponent, CenterComponent
 * - ConstrainedBoxComponent, SizedBoxComponent
 * - ExpandedComponent, FlexibleComponent
 * - FlexComponent, PaddingComponent
 * - FittedBoxComponent, WrapComponent
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// ALIGN
// ============================================

/**
 * Maps AlignComponent to SwiftUI frame with alignment
 *
 * SwiftUI Implementation:
 * ```swift
 * childView
 *     .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
 * ```
 */
object AlignMapper {
    fun map(component: AlignComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Map AlignmentGeometry to SwiftUI alignment
        val alignment = mapAlignment(component.alignment)

        // Apply size factors if specified
        val widthSize = component.widthFactor?.let { SizeValue.Fixed(it) }
        val heightSize = component.heightFactor?.let { SizeValue.Fixed(it) }

        // If no size factors, expand to fill available space with alignment
        if (widthSize == null && heightSize == null) {
            modifiers.add(SwiftUIModifier.frame(
                width = SizeValue.Infinity,
                height = SizeValue.Infinity,
                alignment = alignment
            ))
        } else {
            modifiers.add(SwiftUIModifier.frame(
                width = widthSize,
                height = heightSize,
                alignment = alignment
            ))
        }

        return childView.copy(
            modifiers = childView.modifiers + modifiers
        )
    }

    fun mapAlignment(alignment: AlignmentGeometry): ZStackAlignment {
        return when (alignment) {
            is AlignmentGeometry.Center -> ZStackAlignment.Center
            is AlignmentGeometry.Custom -> {
                when {
                    alignment.x < -0.5f && alignment.y < -0.5f -> ZStackAlignment.TopLeading
                    alignment.x > 0.5f && alignment.y < -0.5f -> ZStackAlignment.TopTrailing
                    alignment.y < -0.5f -> ZStackAlignment.Top
                    alignment.x < -0.5f && alignment.y > 0.5f -> ZStackAlignment.BottomLeading
                    alignment.x > 0.5f && alignment.y > 0.5f -> ZStackAlignment.BottomTrailing
                    alignment.y > 0.5f -> ZStackAlignment.Bottom
                    alignment.x < -0.5f -> ZStackAlignment.Leading
                    alignment.x > 0.5f -> ZStackAlignment.Trailing
                    else -> ZStackAlignment.Center
                }
            }
        }
    }
}

// ============================================
// CENTER
// ============================================

/**
 * Maps CenterComponent to SwiftUI frame with center alignment
 *
 * SwiftUI Implementation:
 * ```swift
 * childView
 *     .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
 * ```
 */
object CenterMapper {
    fun map(component: CenterComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Apply size factors if specified, otherwise expand to fill
        val widthSize = component.widthFactor?.let { SizeValue.Fixed(it) } ?: SizeValue.Infinity
        val heightSize = component.heightFactor?.let { SizeValue.Fixed(it) } ?: SizeValue.Infinity

        modifiers.add(SwiftUIModifier.frame(
            width = widthSize,
            height = heightSize,
            alignment = ZStackAlignment.Center
        ))

        return childView.copy(
            modifiers = childView.modifiers + modifiers
        )
    }
}

// ============================================
// CONSTRAINED BOX
// ============================================

/**
 * Maps ConstrainedBoxComponent to SwiftUI frame with constraints
 *
 * SwiftUI Implementation:
 * ```swift
 * childView
 *     .frame(minWidth: 100, maxWidth: 300, minHeight: 50, maxHeight: 200)
 * ```
 */
object ConstrainedBoxMapper {
    fun map(component: ConstrainedBoxComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)
        val modifiers = mutableListOf<SwiftUIModifier>()
        val constraints = component.constraints

        // Build frame modifier with constraints using extended frame
        modifiers.add(SwiftUIModifier.frame(
            minWidth = if (constraints.minWidth > 0f) SizeValue.Fixed(constraints.minWidth) else null,
            maxWidth = if (constraints.maxWidth < Float.POSITIVE_INFINITY) SizeValue.Fixed(constraints.maxWidth) else null,
            minHeight = if (constraints.minHeight > 0f) SizeValue.Fixed(constraints.minHeight) else null,
            maxHeight = if (constraints.maxHeight < Float.POSITIVE_INFINITY) SizeValue.Fixed(constraints.maxHeight) else null
        ))

        return childView.copy(
            modifiers = childView.modifiers + modifiers
        )
    }
}

// ============================================
// SIZED BOX
// ============================================

/**
 * Maps SizedBoxComponent to SwiftUI frame with exact size
 *
 * SwiftUI Implementation:
 * ```swift
 * childView
 *     .frame(width: 200, height: 100)
 * ```
 */
object SizedBoxMapper {
    fun map(component: SizedBoxComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Convert Size to SizeValue
        val widthValue = component.width?.let { convertSize(it) }
        val heightValue = component.height?.let { convertSize(it) }

        modifiers.add(SwiftUIModifier.frame(
            width = widthValue,
            height = heightValue
        ))

        // If child exists, render it and add frame modifier
        val child = component.child
        return if (child != null) {
            val childView = renderChild(child)
            childView.copy(
                modifiers = childView.modifiers + modifiers
            )
        } else {
            // Empty SizedBox - just a Spacer with frame
            SwiftUIView(
                type = ViewType.Spacer,
                properties = emptyMap(),
                modifiers = modifiers
            )
        }
    }

    private fun convertSize(size: Size): SizeValue {
        return when (size) {
            is Size.Fixed -> SizeValue.Fixed(size.value)
            is Size.Fill -> SizeValue.Infinity
            is Size.Auto -> SizeValue.Ideal
            is Size.Percent -> SizeValue.Fixed(size.value) // Approximate
        }
    }
}

// ============================================
// EXPANDED
// ============================================

/**
 * Maps ExpandedComponent to SwiftUI Spacer with layoutPriority
 *
 * SwiftUI Implementation:
 * ```swift
 * childView
 *     .frame(maxWidth: .infinity)
 *     .layoutPriority(Double(flex))
 * ```
 */
object ExpandedMapper {
    fun map(component: ExpandedComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Expanded fills available space
        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Infinity,
            height = null
        ))

        // Apply layout priority based on flex factor
        if (component.flex != 1) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("layoutPriority" to component.flex.toDouble())
            ))
        }

        return childView.copy(
            modifiers = childView.modifiers + modifiers
        )
    }
}

// ============================================
// FLEXIBLE
// ============================================

/**
 * Maps FlexibleComponent to SwiftUI with optional expansion
 *
 * SwiftUI Implementation for Tight fit:
 * ```swift
 * childView
 *     .frame(maxWidth: .infinity)
 *     .layoutPriority(Double(flex))
 * ```
 *
 * For Loose fit, child keeps natural size but can shrink if needed
 */
object FlexibleMapper {
    fun map(component: FlexibleComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)
        val modifiers = mutableListOf<SwiftUIModifier>()

        when (component.fit) {
            FlexFit.Tight -> {
                // Like Expanded - fill available space
                modifiers.add(SwiftUIModifier.frame(
                    width = SizeValue.Infinity,
                    height = null
                ))
            }
            FlexFit.Loose -> {
                // Keep natural size, but allow shrinking
                // No specific frame modifier needed
            }
        }

        // Apply layout priority based on flex factor
        if (component.flex != 1) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("layoutPriority" to component.flex.toDouble())
            ))
        }

        return childView.copy(
            modifiers = childView.modifiers + modifiers
        )
    }
}

// ============================================
// FLEX
// ============================================

/**
 * Maps FlexComponent to SwiftUI HStack or VStack
 *
 * SwiftUI Implementation:
 * ```swift
 * HStack(alignment: .center, spacing: 8) {
 *     children...
 * }
 * ```
 */
object FlexMapper {
    fun map(component: FlexComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val viewType = when (component.direction) {
            FlexDirection.Horizontal -> ViewType.HStack
            FlexDirection.Vertical -> ViewType.VStack
        }

        // Map alignment
        val alignment = when (component.crossAxisAlignment) {
            CrossAxisAlignment.Start -> "top" // or "leading" for VStack
            CrossAxisAlignment.End -> "bottom" // or "trailing" for VStack
            CrossAxisAlignment.Center -> "center"
            CrossAxisAlignment.Stretch -> "center" // SwiftUI doesn't have stretch, approximate
            CrossAxisAlignment.Baseline -> "firstTextBaseline"
        }

        // Map main axis alignment to SwiftUI spacing behavior
        // SwiftUI uses Spacer() for spacing control, this is simplified
        val spacing = when (component.mainAxisAlignment) {
            MainAxisAlignment.SpaceBetween -> -1f // Special marker for Spacer between
            MainAxisAlignment.SpaceAround -> -2f
            MainAxisAlignment.SpaceEvenly -> -3f
            else -> 0f
        }

        // Render children
        val children = component.children.map { renderChild(it) }

        return SwiftUIView(
            type = viewType,
            properties = mapOf(
                "alignment" to alignment,
                "spacing" to spacing,
                "mainAxisAlignment" to component.mainAxisAlignment.name
            ),
            children = children
        )
    }
}

// ============================================
// PADDING
// ============================================

/**
 * Maps PaddingComponent to SwiftUI padding modifier
 *
 * SwiftUI Implementation:
 * ```swift
 * childView
 *     .padding(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
 * ```
 */
object PaddingMapper {
    fun map(component: PaddingComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)
        val padding = component.padding

        val modifiers = mutableListOf<SwiftUIModifier>()
        if (padding != null) {
            modifiers.add(SwiftUIModifier.padding(
                top = padding.top,
                leading = padding.left,
                bottom = padding.bottom,
                trailing = padding.right
            ))
        }

        return childView.copy(
            modifiers = childView.modifiers + modifiers
        )
    }
}

// ============================================
// FITTED BOX
// ============================================

/**
 * Maps FittedBoxComponent to SwiftUI scaledToFit/scaledToFill
 *
 * SwiftUI Implementation:
 * ```swift
 * childView
 *     .scaledToFit() // or .scaledToFill()
 *     .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
 *     .clipped()
 * ```
 */
object FittedBoxMapper {
    fun map(component: FittedBoxComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Map BoxFit to SwiftUI content mode
        val contentMode = when (component.fit) {
            com.augmentalis.avaelements.flutter.layout.BoxFit.Fill -> "fill" // scaleToFill
            com.augmentalis.avaelements.flutter.layout.BoxFit.Contain -> "fit" // scaledToFit
            com.augmentalis.avaelements.flutter.layout.BoxFit.Cover -> "fill" // scaledToFill + clipped
            com.augmentalis.avaelements.flutter.layout.BoxFit.FitWidth -> "fitWidth"
            com.augmentalis.avaelements.flutter.layout.BoxFit.FitHeight -> "fitHeight"
            com.augmentalis.avaelements.flutter.layout.BoxFit.None -> "none"
            com.augmentalis.avaelements.flutter.layout.BoxFit.ScaleDown -> "scaleDown"
        }

        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("contentMode" to contentMode)
        ))

        // Map alignment
        val alignment = AlignMapper.mapAlignment(component.alignment)

        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Infinity,
            height = SizeValue.Infinity,
            alignment = alignment
        ))

        // Add clipping if needed
        if (component.clipBehavior != Clip.None || component.fit == com.augmentalis.avaelements.flutter.layout.BoxFit.Cover) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("clipped" to true)
            ))
        }

        return childView.copy(
            modifiers = childView.modifiers + modifiers
        )
    }
}

// ============================================
// WRAP
// ============================================

/**
 * Maps WrapComponent to SwiftUI LazyVGrid with flexible columns
 *
 * SwiftUI Implementation:
 * ```swift
 * LazyVGrid(columns: [GridItem(.flexible())], spacing: 8) {
 *     ForEach(children) { child in
 *         child
 *     }
 * }
 * ```
 *
 * Note: SwiftUI doesn't have a direct equivalent to Flutter's Wrap.
 * We approximate it with LazyVGrid for horizontal wrapping.
 */
object WrapMapper {
    fun map(component: WrapComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val children = component.children.map { renderChild(it) }

        val viewType = when (component.direction) {
            WrapDirection.Horizontal -> ViewType.Custom("LazyVGrid")
            WrapDirection.Vertical -> ViewType.Custom("LazyHGrid")
        }

        // Map alignment
        val alignment = when (component.alignment) {
            WrapAlignment.Start -> "leading"
            WrapAlignment.End -> "trailing"
            WrapAlignment.Center -> "center"
            WrapAlignment.SpaceBetween -> "leading" // Approximate
            WrapAlignment.SpaceAround -> "center"
            WrapAlignment.SpaceEvenly -> "center"
        }

        return SwiftUIView(
            type = viewType,
            properties = mapOf(
                "columns" to listOf(mapOf("type" to "flexible")),
                "spacing" to component.spacing.top, // Use top spacing as uniform spacing
                "runSpacing" to component.runSpacing.top,
                "alignment" to alignment
            ),
            children = children
        )
    }
}
