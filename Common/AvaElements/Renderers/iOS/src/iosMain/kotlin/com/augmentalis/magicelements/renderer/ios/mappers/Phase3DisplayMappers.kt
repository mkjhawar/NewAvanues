package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.components.phase3.display.*
import com.augmentalis.avaelements.components.phase3.data.Orientation
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Phase 3 Display Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Phase 3 display components to SwiftUI equivalents:
 * - Badge, Chip, Avatar
 * - Divider, Skeleton, Spinner
 * - ProgressBar, Tooltip
 */

// ============================================
// BADGE
// ============================================

/**
 * Maps Badge component to SwiftUI custom badge view
 */
object BadgeMapper {
    fun map(component: Badge, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Determine badge colors based on variant
        val (bgColor, fgColor) = when (component.variant) {
            "success" -> {
                val bg = theme?.colorScheme?.tertiary?.let { ModifierConverter.convertColor(it) }
                    ?: SwiftUIColor.rgb(76f/255f, 175f/255f, 80f/255f, 1f)
                val fg = SwiftUIColor.white
                bg to fg
            }
            "error" -> {
                val bg = theme?.colorScheme?.error?.let { ModifierConverter.convertColor(it) }
                    ?: SwiftUIColor.rgb(244f/255f, 67f/255f, 54f/255f, 1f)
                val fg = theme?.colorScheme?.onError?.let { ModifierConverter.convertColor(it) }
                    ?: SwiftUIColor.white
                bg to fg
            }
            "warning" -> {
                val bg = SwiftUIColor.rgb(255f/255f, 152f/255f, 0f/255f, 1f)
                val fg = SwiftUIColor.black
                bg to fg
            }
            else -> {
                val bg = theme?.colorScheme?.primary?.let { ModifierConverter.convertColor(it) }
                    ?: SwiftUIColor.rgb(33f/255f, 150f/255f, 243f/255f, 1f)
                val fg = theme?.colorScheme?.onPrimary?.let { ModifierConverter.convertColor(it) }
                    ?: SwiftUIColor.white
                bg to fg
            }
        }

        modifiers.add(SwiftUIModifier.background(bgColor))
        modifiers.add(SwiftUIModifier.foregroundColor(fgColor))
        modifiers.add(SwiftUIModifier.cornerRadius(12f))
        modifiers.add(SwiftUIModifier.padding(4f, 8f, 4f, 8f)) // top, leading, bottom, trailing

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView.text(
            content = component.text,
            modifiers = modifiers
        ).copy(id = component.id)
    }
}

// ============================================
// CHIP
// ============================================

/**
 * Maps Chip component to SwiftUI capsule button
 */
object ChipMapper {
    fun map(component: Chip, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Apply selected state styling
        if (component.selected) {
            theme?.colorScheme?.primary?.let {
                modifiers.add(SwiftUIModifier.background(ModifierConverter.convertColor(it)))
            }
            theme?.colorScheme?.onPrimary?.let {
                modifiers.add(SwiftUIModifier.foregroundColor(ModifierConverter.convertColor(it)))
            }
        } else {
            theme?.colorScheme?.outline?.let {
                modifiers.add(SwiftUIModifier.border(ModifierConverter.convertColor(it), 1f))
            }
            theme?.colorScheme?.onSurface?.let {
                modifiers.add(SwiftUIModifier.foregroundColor(ModifierConverter.convertColor(it)))
            }
        }

        modifiers.add(SwiftUIModifier.cornerRadius(16f))
        modifiers.add(SwiftUIModifier.padding(12f))

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Build properties map excluding null values
        val props = mutableMapOf<String, Any>(
            "label" to component.label,
            "selected" to component.selected,
            "deletable" to component.deletable
        )
        component.icon?.let { props["icon"] = it }
        if (component.onDelete != null) {
            props["onDelete"] = "callback"
        }
        if (component.onClick != null) {
            props["onClick"] = "callback"
        }

        return SwiftUIView(
            type = ViewType.HStack,
            properties = props,
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// AVATAR
// ============================================

/**
 * Maps Avatar component to SwiftUI circular image
 */
object AvatarMapper {
    fun map(component: Avatar, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Determine avatar size from enum
        val size = when (component.size) {
            AvatarSize.Small -> 32f
            AvatarSize.Large -> 64f
            AvatarSize.Medium -> 40f
        }

        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Fixed(size),
            height = SizeValue.Fixed(size)
        ))

        // Apply shape based on component.shape
        when (component.shape) {
            AvatarShape.Circle -> modifiers.add(SwiftUIModifier.custom("clipShape(Circle())"))
            AvatarShape.Square -> {} // No clip needed
            AvatarShape.Rounded -> modifiers.add(SwiftUIModifier.cornerRadius(8f))
        }

        component.style?.backgroundColor?.let {
            modifiers.add(SwiftUIModifier.background(ModifierConverter.convertColor(it)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return component.source?.let { sourceUrl ->
            SwiftUIView(
                type = ViewType.AsyncImage,
                properties = mapOf("url" to sourceUrl),
                modifiers = modifiers,
                id = component.id
            )
        } ?: SwiftUIView.text(
            content = component.text ?: "",
            modifiers = modifiers
        ).copy(id = component.id)
    }
}

// ============================================
// DIVIDER
// ============================================

/**
 * Maps Divider component to SwiftUI Divider (Phase3 enhanced variant)
 */
object Phase3DividerMapper {
    fun map(component: Divider, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        component.style?.backgroundColor?.let {
            modifiers.add(SwiftUIModifier.background(ModifierConverter.convertColor(it)))
        }

        // Use thickness from component (defaults to 1f)
        val thickness = component.thickness

        if (component.orientation == Orientation.Vertical) {
            modifiers.add(SwiftUIModifier.frame(width = SizeValue.Fixed(thickness), height = null))
        } else {
            modifiers.add(SwiftUIModifier.frame(width = null, height = SizeValue.Fixed(thickness)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Divider,
            properties = emptyMap(),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// SKELETON
// ============================================

/**
 * Maps Skeleton component to SwiftUI shimmer effect placeholder
 */
object SkeletonMapper {
    fun map(component: Skeleton, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Use component's width/height if set, or derive from variant
        val width = (component.width as? Size.Fixed)?.value ?: when (component.variant) {
            SkeletonVariant.Text -> 200f
            SkeletonVariant.Circular -> 40f
            else -> null
        }

        val height = (component.height as? Size.Fixed)?.value ?: when (component.variant) {
            SkeletonVariant.Text -> 16f
            SkeletonVariant.Circular -> 40f
            SkeletonVariant.Rectangular -> 100f
            SkeletonVariant.Rounded -> 40f
        }

        if (width != null) {
            modifiers.add(SwiftUIModifier.frame(
                width = SizeValue.Fixed(width),
                height = height?.let { SizeValue.Fixed(it) }
            ))
        } else if (height != null) {
            modifiers.add(SwiftUIModifier.frame(width = null, height = SizeValue.Fixed(height)))
        }

        theme?.colorScheme?.surfaceVariant?.let {
            modifiers.add(SwiftUIModifier.background(ModifierConverter.convertColor(it)))
        }

        if (component.variant == SkeletonVariant.Circular) {
            modifiers.add(SwiftUIModifier.custom("clipShape(Circle())"))
        } else {
            modifiers.add(SwiftUIModifier.cornerRadius(4f))
        }

        // Add shimmer animation
        modifiers.add(SwiftUIModifier.custom("shimmer"))

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Rectangle,
            properties = emptyMap(),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// SPINNER
// ============================================

/**
 * Maps Spinner component to SwiftUI ProgressView
 */
object SpinnerMapper {
    fun map(component: Spinner, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // component.size is an Int representing the size in dp
        val size = component.size.toFloat()

        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Fixed(size),
            height = SizeValue.Fixed(size)
        ))

        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.accentColor(ModifierConverter.convertColor(it)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.ProgressView,
            properties = mapOf("style" to "circular"),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// PROGRESS BAR
// ============================================

/**
 * Maps ProgressBar component to SwiftUI ProgressView (linear)
 */
object ProgressBarMapper {
    fun map(component: ProgressBar, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.accentColor(ModifierConverter.convertColor(it)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.ProgressView,
            properties = mapOf(
                "progress" to component.progress,
                "showLabel" to component.showLabel,
                "style" to "linear"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// TOOLTIP
// ============================================

/**
 * Maps Tooltip component to SwiftUI popover or overlay
 */
object TooltipMapper {
    fun map(component: Tooltip, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.custom("popover"))

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Custom("Tooltip"),
            properties = mapOf(
                "text" to component.text,
                "placement" to component.placement
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}
