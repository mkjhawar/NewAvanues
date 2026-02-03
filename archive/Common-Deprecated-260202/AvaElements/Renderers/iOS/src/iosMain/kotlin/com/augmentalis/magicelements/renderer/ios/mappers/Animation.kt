package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.flutter.animation.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

// Helper function to extract spacing value
private fun extractSpacing(spacing: Spacing): Float {
    return spacing.top.takeIf { it > 0f } ?: spacing.left.takeIf { it > 0f } ?: 0f
}

// Helper function to convert Flutter Color to SwiftUIColor
private fun convertColor(color: Color): SwiftUIColor {
    return SwiftUIColor.rgb(
        color.red / 255f,
        color.green / 255f,
        color.blue / 255f,
        color.alpha / 255f
    )
}

// Helper function to extract Size value as Float
private fun extractSizeValue(size: Size?): Float? {
    return when (size) {
        is Size.Fixed -> size.value
        else -> null
    }
}

// Helper function to convert AlignmentGeometry to SwiftUI alignment string
private fun convertAlignment(alignment: AlignmentGeometry): String {
    return when (alignment) {
        is AlignmentGeometry.TopLeft -> "topLeading"
        is AlignmentGeometry.TopCenter -> "top"
        is AlignmentGeometry.TopRight -> "topTrailing"
        is AlignmentGeometry.CenterLeft -> "leading"
        is AlignmentGeometry.Center -> "center"
        is AlignmentGeometry.CenterRight -> "trailing"
        is AlignmentGeometry.BottomLeft -> "bottomLeading"
        is AlignmentGeometry.BottomCenter -> "bottom"
        is AlignmentGeometry.BottomRight -> "bottomTrailing"
        is AlignmentGeometry.Custom -> "center" // Custom alignments default to center
    }
}

/**
 * Flutter Animation Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity animation components to SwiftUI equivalents.
 *
 * Components:
 * - AnimatedContainer → withAnimation + frame modifiers
 * - AnimatedOpacity → withAnimation + opacity modifier
 * - AnimatedPositioned → withAnimation + position modifier
 * - AnimatedDefaultTextStyle → withAnimation + font modifier
 * - AnimatedPadding → withAnimation + padding modifier
 * - AnimatedSize → withAnimation + frame modifier
 * - AnimatedAlign → withAnimation + alignment modifier
 * - AnimatedScale → withAnimation + scaleEffect modifier
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// ANIMATED CONTAINER
// ============================================

object AnimatedContainerMapper {
    fun map(component: AnimatedContainer, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = component.child?.let { renderChild(it) }

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Width and height
        val width = extractSizeValue(component.width)
        val height = extractSizeValue(component.height)
        if (width != null || height != null) {
            modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
                "frame" to buildMap {
                    width?.let { put("width", it) }
                    height?.let { put("height", it) }
                }
            )))
        }

        // Padding
        component.padding?.let { padding ->
            modifiers.add(SwiftUIModifier.padding(extractSpacing(padding)))
        }

        // Margin (as additional padding)
        component.margin?.let { margin ->
            modifiers.add(SwiftUIModifier.padding(extractSpacing(margin)))
        }

        // Background color
        component.color?.let { color ->
            modifiers.add(SwiftUIModifier.background(convertColor(color)))
        }

        // Border radius
        component.decoration?.borderRadius?.topLeft?.let { radius ->
            modifiers.add(SwiftUIModifier.cornerRadius(radius))
        }

        // Animation
        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "animation" to mapOf(
                "type" to "easeInOut",
                "duration" to component.duration.milliseconds / 1000.0
            )
        )))

        return if (childView != null) {
            SwiftUIView(
                type = childView.type,
                properties = childView.properties,
                children = childView.children,
                modifiers = childView.modifiers + modifiers,
            )
        } else {
            SwiftUIView(
                type = ViewType.VStack,
                properties = emptyMap(),
                children = emptyList(),
                modifiers = modifiers,
            )
        }
    }
}

// ============================================
// ANIMATED OPACITY
// ============================================

object AnimatedOpacityMapper {
    fun map(component: AnimatedOpacity, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        val modifiers = listOf(
            SwiftUIModifier(ModifierType.Custom, mapOf("opacity" to component.opacity)),
            SwiftUIModifier(ModifierType.Custom, mapOf(
                "animation" to mapOf(
                    "type" to "easeInOut",
                    "duration" to component.duration.milliseconds / 1000.0
                )
            ))
        )

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + modifiers,
        )
    }
}

// ============================================
// ANIMATED POSITIONED
// ============================================

object AnimatedPositionedMapper {
    fun map(component: AnimatedPositioned, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Position offset
        val left = extractSizeValue(component.left) ?: 0f
        val top = extractSizeValue(component.top) ?: 0f
        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "offset" to mapOf("x" to left, "y" to top)
        )))

        // Size
        val width = extractSizeValue(component.width)
        val height = extractSizeValue(component.height)
        if (width != null || height != null) {
            modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
                "frame" to buildMap {
                    width?.let { put("width", it) }
                    height?.let { put("height", it) }
                }
            )))
        }

        // Animation
        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "animation" to mapOf(
                "type" to "easeInOut",
                "duration" to component.duration.milliseconds / 1000.0
            )
        )))

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + modifiers,
        )
    }
}

// ============================================
// ANIMATED DEFAULT TEXT STYLE
// ============================================

object AnimatedDefaultTextStyleMapper {
    fun map(component: AnimatedDefaultTextStyle, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Font size
        component.style.fontSize?.let { size ->
            modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
                "font" to mapOf("size" to size)
            )))
        }

        // Font weight
        component.style.fontWeight?.let { weight ->
            val fontWeight = when (weight.name.lowercase()) {
                "thin", "w100" -> com.augmentalis.avaelements.renderer.ios.bridge.FontWeight.Thin
                "ultralight", "extralight", "w200" -> com.augmentalis.avaelements.renderer.ios.bridge.FontWeight.UltraLight
                "light", "w300" -> com.augmentalis.avaelements.renderer.ios.bridge.FontWeight.Light
                "regular", "normal", "w400" -> com.augmentalis.avaelements.renderer.ios.bridge.FontWeight.Regular
                "medium", "w500" -> com.augmentalis.avaelements.renderer.ios.bridge.FontWeight.Medium
                "semibold", "w600" -> com.augmentalis.avaelements.renderer.ios.bridge.FontWeight.Semibold
                "bold", "w700" -> com.augmentalis.avaelements.renderer.ios.bridge.FontWeight.Bold
                "heavy", "ultrabold", "extrabold", "w800" -> com.augmentalis.avaelements.renderer.ios.bridge.FontWeight.Heavy
                "black", "w900" -> com.augmentalis.avaelements.renderer.ios.bridge.FontWeight.Black
                else -> com.augmentalis.avaelements.renderer.ios.bridge.FontWeight.Regular
            }
            modifiers.add(SwiftUIModifier.fontWeight(fontWeight))
        }

        // Text color
        component.style.color?.let { color ->
            modifiers.add(SwiftUIModifier.foregroundColor(convertColor(color)))
        }

        // Animation
        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "animation" to mapOf(
                "type" to "easeInOut",
                "duration" to component.duration.milliseconds / 1000.0
            )
        )))

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + modifiers,
        )
    }
}

// ============================================
// ANIMATED PADDING
// ============================================

object AnimatedPaddingMapper {
    fun map(component: AnimatedPadding, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        val modifiers = listOf(
            SwiftUIModifier.padding(extractSpacing(component.padding)),
            SwiftUIModifier(ModifierType.Custom, mapOf(
                "animation" to mapOf(
                    "type" to "easeInOut",
                    "duration" to component.duration.milliseconds / 1000.0
                )
            ))
        )

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + modifiers,
        )
    }
}

// ============================================
// ANIMATED SIZE
// ============================================

object AnimatedSizeMapper {
    fun map(component: AnimatedSize, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        val modifiers = mutableListOf<SwiftUIModifier>()

        // AnimatedSize automatically sizes to its child, so we just add animation modifier
        // The actual sizing is handled by the child content
        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "animation" to mapOf(
                "type" to "easeInOut",
                "duration" to component.duration.milliseconds / 1000.0
            )
        )))

        // Add alignment if not default
        if (component.alignment != AlignmentGeometry.TopCenter) {
            modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
                "frame" to mapOf("alignment" to convertAlignment(component.alignment))
            )))
        }

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + modifiers,
        )
    }
}

// ============================================
// ANIMATED ALIGN
// ============================================

object AnimatedAlignMapper {
    fun map(component: AnimatedAlign, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        val alignment = convertAlignment(component.alignment)

        val modifiers = listOf(
            SwiftUIModifier(ModifierType.Custom, mapOf("frame" to mapOf(
                "maxWidth" to Float.MAX_VALUE,
                "maxHeight" to Float.MAX_VALUE,
                "alignment" to alignment
            ))),
            SwiftUIModifier(ModifierType.Custom, mapOf(
                "animation" to mapOf(
                    "type" to "easeInOut",
                    "duration" to component.duration.milliseconds / 1000.0
                )
            ))
        )

        return SwiftUIView(
            type = ViewType.ZStack,
            properties = emptyMap(),
            children = listOf(childView),
            modifiers = modifiers,
        )
    }
}

// ============================================
// ANIMATED SCALE
// ============================================

object AnimatedScaleMapper {
    fun map(component: AnimatedScale, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        val modifiers = listOf(
            SwiftUIModifier(ModifierType.Custom, mapOf("scaleEffect" to component.scale)),
            SwiftUIModifier(ModifierType.Custom, mapOf(
                "animation" to mapOf(
                    "type" to "easeInOut",
                    "duration" to component.duration.milliseconds / 1000.0
                )
            ))
        )

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + modifiers,
        )
    }
}
