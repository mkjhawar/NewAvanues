package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.flutter.animation.transitions.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Flutter Transition Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity transition components to SwiftUI equivalents.
 *
 * Components:
 * - FadeTransition → opacity transition
 * - SlideTransition → offset transition
 * - Hero → matchedGeometryEffect
 * - ScaleTransition → scaleEffect transition
 * - RotationTransition → rotationEffect transition
 * - PositionedTransition → position transition
 * - SizeTransition → frame transition
 * - AnimatedCrossFade → crossfade transition
 * - AnimatedSwitcher → transition with content change
 * - DecoratedBoxTransition → decoration transition
 * - AlignTransition → alignment transition
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// FADE TRANSITION
// ============================================

object FadeTransitionMapper {
    fun map(component: FadeTransition, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf("opacity" to component.opacity)),
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "transition" to mapOf("type" to "opacity")
                ))
            )
        )
    }
}

// ============================================
// SLIDE TRANSITION
// ============================================

object SlideTransitionMapper {
    fun map(component: SlideTransition, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        val offsetX = component.position.dx
        val offsetY = component.position.dy

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf<String, Any>(
                    "offset" to mapOf("x" to offsetX, "y" to offsetY)
                )),
                SwiftUIModifier(ModifierType.Custom, mapOf<String, Any>(
                    "transition" to mapOf("type" to "slide")
                ))
            )
        )
    }
}

// ============================================
// HERO
// ============================================

object HeroMapper {
    fun map(component: Hero, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "matchedGeometryEffect" to mapOf("id" to component.tag, "namespace" to "heroAnimation")
                ))
            )
        )
    }
}

// ============================================
// SCALE TRANSITION
// ============================================

object ScaleTransitionMapper {
    fun map(component: ScaleTransition, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf("scaleEffect" to component.scale)),
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "transition" to mapOf("type" to "scale")
                ))
            )
        )
    }
}

// ============================================
// ROTATION TRANSITION
// ============================================

object RotationTransitionMapper {
    fun map(component: RotationTransition, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        // Convert turns to degrees (1 turn = 360 degrees)
        val degrees = component.turns * 360f

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "rotationEffect" to mapOf("degrees" to degrees, "anchor" to "center")
                )),
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "transition" to mapOf("type" to "rotation")
                ))
            )
        )
    }
}

// ============================================
// POSITIONED TRANSITION
// ============================================

object PositionedTransitionMapper {
    fun map(component: PositionedTransition, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Position
        val left = component.rect.left ?: 0f
        val top = component.rect.top ?: 0f
        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "position" to mapOf("x" to left, "y" to top)
        )))

        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "transition" to mapOf("type" to "move")
        )))

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + modifiers
        )
    }
}

// ============================================
// SIZE TRANSITION
// ============================================

object SizeTransitionMapper {
    fun map(component: SizeTransition, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Apply size factor as scale
        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "scaleEffect" to component.sizeFactor
        )))

        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "transition" to mapOf("type" to "scale", "axis" to component.axis.name.lowercase())
        )))

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + modifiers
        )
    }
}

// ============================================
// ANIMATED CROSS FADE
// ============================================

object AnimatedCrossFadeMapper {
    fun map(component: AnimatedCrossFade, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val firstChild = renderChild(component.firstChild)
        val secondChild = renderChild(component.secondChild)

        // Show one or the other based on state
        val activeChild = if (component.crossFadeState == AnimatedCrossFade.CrossFadeState.ShowFirst) firstChild else secondChild

        return SwiftUIView(
            type = activeChild.type,
            properties = activeChild.properties,
            children = activeChild.children,
            modifiers = activeChild.modifiers + listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "transition" to mapOf("type" to "opacity")
                )),
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "animation" to mapOf(
                        "type" to "easeInOut",
                        "duration" to component.duration / 1000.0
                    )
                ))
            )
        )
    }
}

// ============================================
// ANIMATED SWITCHER
// ============================================

object AnimatedSwitcherMapper {
    fun map(component: AnimatedSwitcher, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = component.child?.let { renderChild(it) } ?: SwiftUIView(
            type = ViewType.EmptyView,
            properties = emptyMap()
        )

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "transition" to mapOf("type" to "opacity")
                )),
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "animation" to mapOf(
                        "type" to component.switchInCurve,
                        "duration" to component.duration / 1000.0
                    )
                ))
            )
        )
    }
}

// ============================================
// DECORATED BOX TRANSITION
// ============================================

object DecoratedBoxTransitionMapper {
    fun map(component: DecoratedBoxTransition, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Background color
        component.decoration.color?.let { color ->
            modifiers.add(SwiftUIModifier.background(SwiftUIColor.rgb(
                ((color shr 16) and 0xFF) / 255f,
                ((color shr 8) and 0xFF) / 255f,
                (color and 0xFF) / 255f,
                ((color shr 24) and 0xFF) / 255f
            )))
        }

        // Border (border is a String description, use default width)
        component.decoration.border?.let { border ->
            modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
                "overlay" to mapOf(
                    "shape" to "RoundedRectangle",
                    "cornerRadius" to (component.decoration.borderRadius ?: 0f),
                    "stroke" to "border",
                    "lineWidth" to 1f  // Default border width
                )
            )))
        }

        // Corner radius
        component.decoration.borderRadius?.let { radius ->
            modifiers.add(SwiftUIModifier.cornerRadius(radius))
        }

        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "transition" to mapOf("type" to "opacity")
        )))

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + modifiers
        )
    }
}

// ============================================
// ALIGN TRANSITION
// ============================================

object AlignTransitionMapper {
    fun map(component: AlignTransition, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        val alignment = when (component.alignment) {
            AlignTransition.Alignment.TopLeft -> "topLeading"
            AlignTransition.Alignment.TopCenter -> "top"
            AlignTransition.Alignment.TopRight -> "topTrailing"
            AlignTransition.Alignment.CenterLeft -> "leading"
            AlignTransition.Alignment.Center -> "center"
            AlignTransition.Alignment.CenterRight -> "trailing"
            AlignTransition.Alignment.BottomLeft -> "bottomLeading"
            AlignTransition.Alignment.BottomCenter -> "bottom"
            AlignTransition.Alignment.BottomRight -> "bottomTrailing"
        }

        return SwiftUIView(
            type = ViewType.ZStack,
            properties = emptyMap(),
            children = listOf(childView),
            modifiers = listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf("frame" to mapOf(
                    "maxWidth" to Float.MAX_VALUE,
                    "maxHeight" to Float.MAX_VALUE,
                    "alignment" to alignment
                ))),
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "transition" to mapOf("type" to "move")
                ))
            )
        )
    }
}
