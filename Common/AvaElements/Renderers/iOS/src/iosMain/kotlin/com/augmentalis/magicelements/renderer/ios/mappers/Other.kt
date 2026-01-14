package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.flutter.material.advanced.*
import com.augmentalis.avaelements.flutter.animation.transitions.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Flutter Other Component Mappers for iOS SwiftUI
 *
 * Maps miscellaneous AvaElements Flutter-parity components to SwiftUI equivalents.
 *
 * Components:
 * - FadeInImage → AsyncImage with placeholder
 * - CircleAvatar → Circle with AsyncImage
 * - SelectableText → Text with textSelection enabled
 * - VerticalDivider → Vertical divider line
 * - EndDrawer → Trailing sidebar
 * - AnimatedList → List with insertion/removal animations
 * - AnimatedModalBarrier → Semi-transparent barrier with fade
 * - DefaultTextStyleTransition → Text style transition
 * - RelativePositionedTransition → Relative position transition
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// FADE IN IMAGE
// ============================================

object FadeInImageMapper {
    fun map(component: FadeInImage, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Custom("AsyncImage"),
            properties = mapOf(
                "url" to component.image,
                "placeholderUrl" to component.placeholder
            ),
            modifiers = listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "transition" to mapOf("type" to "opacity")
                )),
                component.width?.let { width ->
                    component.height?.let { height ->
                        SwiftUIModifier(ModifierType.Custom, mapOf(
                            "frame" to mapOf("width" to width, "height" to height)
                        ))
                    }
                },
                SwiftUIModifier(ModifierType.Custom, mapOf("contentMode" to component.fit))
            ).filterNotNull(),
        )
    }
}

// ============================================
// CIRCLE AVATAR
// ============================================

object CircleAvatarMapper {
    fun map(component: CircleAvatar, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Background circle
        children.add(SwiftUIView(
            type = ViewType.Custom("Circle"),
            properties = emptyMap(),
            modifiers = listOf(
                SwiftUIModifier.foregroundColor(
                    component.backgroundColor?.let { SwiftUIColor.system(it) } ?: SwiftUIColor.system("systemGray")
                )
            )
        ))

        // Content (image or text)
        if (component.backgroundImage != null) {
            children.add(SwiftUIView(
                type = ViewType.Custom("AsyncImage"),
                properties = mapOf("url" to (component.backgroundImage as Any)),
                modifiers = listOf(
                    SwiftUIModifier(ModifierType.Custom, mapOf("clipShape" to "Circle"))
                )
            ))
        } else if (component.child != null) {
            // Render child content (usually text initials)
            children.add(SwiftUIView.text(
                content = component.child.toString(),
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Subheadline),
                    SwiftUIModifier.fontWeight(FontWeight.Semibold),
                    SwiftUIModifier.foregroundColor(
                        component.foregroundColor?.let { SwiftUIColor.system(it) } ?: SwiftUIColor.system("white")
                    )
                )
            ))
        }

        val effectiveRadius = component.getEffectiveRadius()
        return SwiftUIView(
            type = ViewType.ZStack,
            properties = emptyMap(),
            children = children,
            modifiers = listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "frame" to mapOf(
                        "width" to (effectiveRadius * 2f),
                        "height" to (effectiveRadius * 2f)
                    )
                ))
            ),
        )
    }
}

// ============================================
// SELECTABLE TEXT
// ============================================

object SelectableTextMapper {
    fun map(component: SelectableText, theme: Theme?): SwiftUIView {
        return SwiftUIView.text(
            content = component.text,
            modifiers = listOf(
                component.textStyle?.fontSize?.let { size ->
                    SwiftUIModifier(ModifierType.Custom, mapOf("font" to mapOf("size" to size)))
                },
                component.textStyle?.fontWeight?.let { weight ->
                    // Convert string fontWeight to FontWeight enum
                    val fontWeight = when (weight.lowercase()) {
                        "thin", "w100" -> FontWeight.Thin
                        "ultralight", "extralight", "w200" -> FontWeight.UltraLight
                        "light", "w300" -> FontWeight.Light
                        "regular", "normal", "w400" -> FontWeight.Regular
                        "medium", "w500" -> FontWeight.Medium
                        "semibold", "w600" -> FontWeight.Semibold
                        "bold", "w700" -> FontWeight.Bold
                        "heavy", "ultrabold", "extrabold", "w800" -> FontWeight.Heavy
                        "black", "w900" -> FontWeight.Black
                        else -> FontWeight.Regular
                    }
                    SwiftUIModifier.fontWeight(fontWeight)
                },
                component.textStyle?.color?.let { color ->
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system(color))
                },
                SwiftUIModifier(ModifierType.Custom, mapOf("textSelection" to "enabled"))
            ).filterNotNull()
        )
    }
}

// ============================================
// VERTICAL DIVIDER
// ============================================

object VerticalDividerMapper {
    fun map(component: VerticalDivider, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Custom("Divider"),
            properties = emptyMap(),
            modifiers = listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "frame" to mapOf(
                        "width" to component.thickness,
                        "maxHeight" to Float.MAX_VALUE
                    )
                )),
                component.indent.let { indent ->
                    if (indent > 0f) {
                        SwiftUIModifier(ModifierType.Custom, mapOf("padding" to mapOf("top" to indent)))
                    } else null
                },
                component.endIndent.let { endIndent ->
                    if (endIndent > 0f) {
                        SwiftUIModifier(ModifierType.Custom, mapOf("padding" to mapOf("bottom" to endIndent)))
                    } else null
                },
                component.color?.let { color ->
                    SwiftUIModifier.background(SwiftUIColor.system(color))
                } ?: SwiftUIModifier.background(SwiftUIColor.system("separator"))
            ).filterNotNull(),
        )
    }
}

// ============================================
// END DRAWER
// ============================================

object EndDrawerMapper {
    fun map(component: EndDrawer, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Drawer content (single child, not children list)
        component.child?.let { child ->
            children.add(renderChild(child))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 0f),
            children = children,
            modifiers = listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "frame" to mapOf(
                        "width" to component.width,
                        "maxHeight" to Float.MAX_VALUE,
                        "alignment" to "trailing"
                    )
                )),
                SwiftUIModifier.background(SwiftUIColor.system("systemBackground")),
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "offset" to mapOf("x" to component.width, "y" to 0f)  // Start off-screen
                )),
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "animation" to mapOf("type" to "easeInOut", "duration" to 0.3)
                ))
            )
        )
    }
}

// ============================================
// ANIMATED LIST
// ============================================

object AnimatedListMapper {
    fun map(component: AnimatedList, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val registry = com.augmentalis.magicelements.renderer.ios.registry.ItemBuilderRegistryHolder.getRegistry()
        val itemCount = component.items.size

        val children = if (component.itemBuilder != null && registry.hasBuilder(component.itemBuilder!!)) {
            // Use builder pattern from registry
            (0 until itemCount).mapNotNull { index ->
                registry.resolveBuilder(component.itemBuilder!!, index)?.let { item ->
                    val childView = renderChild(item)
                    // Add animation modifiers to each child
                    SwiftUIView(
                        type = childView.type,
                        properties = childView.properties,
                        children = childView.children,
                        modifiers = childView.modifiers + listOf(
                            SwiftUIModifier(ModifierType.Custom, mapOf(
                                "transition" to mapOf("type" to "slide", "edge" to "top")
                            ))
                        ),
                        id = childView.id
                    )
                }
            }
        } else {
            // Fallback: render items directly
            component.items.map { item ->
                val childView = renderChild(item)
                // Add animation modifiers to each child
                SwiftUIView(
                    type = childView.type,
                    properties = childView.properties,
                    children = childView.children,
                    modifiers = childView.modifiers + listOf(
                        SwiftUIModifier(ModifierType.Custom, mapOf(
                            "transition" to mapOf("type" to "slide", "edge" to "top")
                        ))
                    ),
                    id = childView.id
                )
            }
        }

        return SwiftUIView(
            type = ViewType.Custom("LazyVStack"),
            properties = mapOf("spacing" to 0f),
            children = children,
            modifiers = listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "animation" to mapOf("type" to "easeInOut", "duration" to 0.3)
                ))
            )
        )
    }
}

// ============================================
// ANIMATED MODAL BARRIER
// ============================================

object AnimatedModalBarrierMapper {
    fun map(component: AnimatedModalBarrier, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Custom("Rectangle"),
            properties = emptyMap(),
            modifiers = listOf(
                SwiftUIModifier.foregroundColor(
                    SwiftUIColor.rgb(
                        component.getRed() / 255f,
                        component.getGreen() / 255f,
                        component.getBlue() / 255f,
                        component.getOpacity()
                    )
                ),
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "frame" to mapOf(
                        "maxWidth" to Float.MAX_VALUE,
                        "maxHeight" to Float.MAX_VALUE
                    )
                )),
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "transition" to mapOf("type" to "opacity")
                )),
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "animation" to mapOf("type" to "easeInOut", "duration" to 0.2)
                )),
                SwiftUIModifier(ModifierType.Custom, mapOf("ignoresSafeArea" to true))
            ),
        )
    }
}

// ============================================
// DEFAULT TEXT STYLE TRANSITION
// ============================================

object DefaultTextStyleTransitionMapper {
    fun map(component: DefaultTextStyleTransition, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Font size
        component.style?.fontSize?.let { size ->
            modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf("font" to mapOf("size" to size))))
        }

        // Font weight - convert from component FontWeight to SwiftUI FontWeight
        component.style?.fontWeight?.let { weight ->
            val fontWeight = when (weight) {
                DefaultTextStyleTransition.FontWeight.Thin -> FontWeight.Thin
                DefaultTextStyleTransition.FontWeight.ExtraLight -> FontWeight.UltraLight
                DefaultTextStyleTransition.FontWeight.Light -> FontWeight.Light
                DefaultTextStyleTransition.FontWeight.Normal -> FontWeight.Regular
                DefaultTextStyleTransition.FontWeight.Medium -> FontWeight.Medium
                DefaultTextStyleTransition.FontWeight.SemiBold -> FontWeight.Semibold
                DefaultTextStyleTransition.FontWeight.Bold -> FontWeight.Bold
                DefaultTextStyleTransition.FontWeight.ExtraBold -> FontWeight.Heavy
                DefaultTextStyleTransition.FontWeight.Black -> FontWeight.Black
            }
            modifiers.add(SwiftUIModifier.fontWeight(fontWeight))
        }

        // Text color - convert Long color to RGB
        component.style?.color?.let { color ->
            val swiftColor = SwiftUIColor.rgb(
                ((color shr 16) and 0xFF) / 255f,
                ((color shr 8) and 0xFF) / 255f,
                (color and 0xFF) / 255f,
                ((color shr 24) and 0xFF) / 255f
            )
            modifiers.add(SwiftUIModifier.foregroundColor(swiftColor))
        }

        // Animation
        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "transition" to mapOf("type" to "opacity")
        )))

        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "animation" to mapOf("type" to "easeInOut", "duration" to 0.3)
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
// RELATIVE POSITIONED TRANSITION
// ============================================

object RelativePositionedTransitionMapper {
    fun map(component: RelativePositionedTransition, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val childView = renderChild(component.child)

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Relative position (as fraction of parent size)
        val relativeX = component.rect?.left ?: 0f
        val relativeY = component.rect?.top ?: 0f

        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "position" to mapOf("x" to relativeX, "y" to relativeY)
        )))

        // Size - using component.size (Pair<Float, Float>)
        val width = component.size.first
        val height = component.size.second
        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "frame" to mapOf("width" to width, "height" to height)
        )))

        // Animation
        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "transition" to mapOf("type" to "move")
        )))

        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf(
            "animation" to mapOf("type" to "easeInOut", "duration" to 0.3)
        )))

        return SwiftUIView(
            type = childView.type,
            properties = childView.properties,
            children = childView.children,
            modifiers = childView.modifiers + modifiers,
        )
    }
}
