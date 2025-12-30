package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.flutter.material.chips.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

// Helper function to extract spacing values
private data class SpacingValues(
    val top: Float,
    val left: Float,
    val bottom: Float,
    val right: Float
)

private fun extractSpacing(spacing: Spacing): SpacingValues {
    return SpacingValues(
        top = spacing.top,
        left = spacing.left,
        bottom = spacing.bottom,
        right = spacing.right
    )
}

/**
 * Flutter Material Chip Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity chip components to SwiftUI equivalents:
 * - FilterChip (MagicFilter) - Selectable filter tag with checkmark
 * - ActionChip (MagicAction) - Button-like action chip
 * - ChoiceChip (MagicChoice) - Single-selection radio-style chip
 * - InputChip (MagicInput) - Deletable input tag
 *
 * SwiftUI Implementation Notes:
 * - Uses Capsule shape for Material chip appearance
 * - Implements selection states with visual feedback
 * - Supports avatars and icons
 * - Accessibility labels for VoiceOver
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// FILTER CHIP (MagicFilter)
// ============================================

/**
 * Maps MagicFilter/FilterChip to SwiftUI toggle-style chip
 *
 * SwiftUI Implementation:
 * ```swift
 * Button(action: { selected.toggle() }) {
 *     HStack(spacing: 4) {
 *         if selected { Image(systemName: "checkmark") }
 *         if let avatar { Image(avatar) }
 *         Text(label)
 *     }
 *     .padding(.horizontal, 12)
 *     .padding(.vertical, 8)
 *     .background(selected ? Color.accentColor : Color.secondary.opacity(0.2))
 *     .clipShape(Capsule())
 * }
 * ```
 */
object FilterChipMapper {
    fun map(component: MagicFilter, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Apply theme colors
        val selectedColor = theme?.colorScheme?.secondaryContainer?.let {
            ModifierConverter.convertColor(it)
        } ?: SwiftUIColor.system("secondarySystemFill")

        val unselectedColor = SwiftUIColor.rgb(0.5f, 0.5f, 0.5f, 0.2f)

        // Background based on selection state
        if (component.selected) {
            modifiers.add(SwiftUIModifier.background(selectedColor))
        } else {
            modifiers.add(SwiftUIModifier.background(unselectedColor))
        }

        // Chip shape
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("clipShape" to "Capsule")
        ))

        // Padding
        modifiers.add(SwiftUIModifier.padding(8f, 12f, 8f, 12f))

        // Opacity for disabled state
        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        // Build children for HStack content
        val children = mutableListOf<SwiftUIView>()

        // Checkmark when selected
        if (component.selected && component.showCheckmark) {
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "systemName" to "checkmark",
                    "size" to 12f
                )
            ))
        }

        // Avatar if present
        component.avatar?.let { avatar ->
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "name" to avatar,
                    "size" to 18f
                )
            ))
        }

        // Label text
        children.add(SwiftUIView.text(
            content = component.label,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Subheadline)
            )
        ))

        return SwiftUIView(
            type = ViewType.Custom("ChipButton"),
            properties = mapOf(
                "selected" to component.selected,
                "enabled" to component.enabled,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf("spacing" to 4f),
                    children = children,
                    modifiers = modifiers
                )
            ),
        )
    }
}

// ============================================
// ACTION CHIP (MagicAction)
// ============================================

/**
 * Maps MagicAction/ActionChip to SwiftUI button-style chip
 *
 * SwiftUI Implementation:
 * ```swift
 * Button(action: onPressed) {
 *     HStack(spacing: 4) {
 *         if let avatar { Image(avatar) }
 *         Text(label)
 *     }
 *     .padding(.horizontal, 12)
 *     .padding(.vertical, 8)
 *     .background(Color.secondary.opacity(0.2))
 *     .clipShape(Capsule())
 * }
 * ```
 */
object ActionChipMapper {
    fun map(component: MagicAction, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Background color
        val backgroundColor = component.backgroundColor?.let {
            parseChipColor(it)
        } ?: SwiftUIColor.rgb(0.5f, 0.5f, 0.5f, 0.2f)

        modifiers.add(SwiftUIModifier.background(backgroundColor))

        // Elevation shadow
        component.elevation?.let { elevation ->
            modifiers.add(SwiftUIModifier.shadow(
                color = component.shadowColor?.let { parseChipColor(it) } ?: SwiftUIColor.rgb(0f, 0f, 0f, 0.15f),
                radius = elevation,
                x = 0f,
                y = elevation / 2
            ))
        }

        // Chip shape
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("clipShape" to "Capsule")
        ))

        // Padding
        val padding = component.padding?.let { extractSpacing(it) }
        if (padding != null) {
            modifiers.add(SwiftUIModifier.padding(padding.top, padding.left, padding.bottom, padding.right))
        } else {
            modifiers.add(SwiftUIModifier.padding(8f, 12f, 8f, 12f))
        }

        // Opacity for disabled state
        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        // Build children for HStack content
        val children = mutableListOf<SwiftUIView>()

        // Avatar if present
        component.avatar?.let { avatar ->
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "name" to avatar,
                    "size" to 18f
                )
            ))
        }

        // Label text
        children.add(SwiftUIView.text(
            content = component.label,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Subheadline)
            )
        ))

        return SwiftUIView(
            type = ViewType.Custom("ChipButton"),
            properties = mapOf(
                "enabled" to component.enabled,
                "accessibilityLabel" to component.getAccessibilityDescription(),
                "tooltip" to (component.tooltip ?: "")
            ),
            children = listOf(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf("spacing" to 4f),
                    children = children,
                    modifiers = modifiers
                )
            ),
        )
    }
}

// ============================================
// CHOICE CHIP (MagicChoice)
// ============================================

/**
 * Maps MagicChoice/ChoiceChip to SwiftUI selectable chip (radio-style)
 *
 * SwiftUI Implementation:
 * ```swift
 * Button(action: { onSelected(!selected) }) {
 *     HStack(spacing: 4) {
 *         if selected && showCheckmark { Image(systemName: "checkmark") }
 *         if let avatar { Image(avatar) }
 *         Text(label)
 *     }
 *     .padding(.horizontal, 12)
 *     .padding(.vertical, 8)
 *     .background(selected ? selectedColor : backgroundColor)
 *     .clipShape(Capsule())
 * }
 * ```
 */
object ChoiceChipMapper {
    fun map(component: MagicChoice, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Background color based on selection state
        val backgroundColor = if (component.selected) {
            component.selectedColor?.let { parseChipColor(it) }
                ?: theme?.colorScheme?.secondaryContainer?.let { ModifierConverter.convertColor(it) }
                ?: SwiftUIColor.system("accentColor")
        } else {
            component.backgroundColor?.let { parseChipColor(it) }
                ?: SwiftUIColor.rgb(0.5f, 0.5f, 0.5f, 0.2f)
        }

        modifiers.add(SwiftUIModifier.background(backgroundColor))

        // Elevation shadow
        component.elevation?.let { elevation ->
            val shadowColor = if (component.selected) {
                component.selectedShadowColor?.let { parseChipColor(it) }
            } else {
                component.shadowColor?.let { parseChipColor(it) }
            } ?: SwiftUIColor.rgb(0f, 0f, 0f, 0.15f)

            modifiers.add(SwiftUIModifier.shadow(
                color = shadowColor,
                radius = elevation,
                x = 0f,
                y = elevation / 2
            ))
        }

        // Chip shape
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("clipShape" to "Capsule")
        ))

        // Padding
        val padding = component.padding?.let { extractSpacing(it) }
        if (padding != null) {
            modifiers.add(SwiftUIModifier.padding(padding.top, padding.left, padding.bottom, padding.right))
        } else {
            modifiers.add(SwiftUIModifier.padding(8f, 12f, 8f, 12f))
        }

        // Opacity for disabled state
        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        // Build children for HStack content
        val children = mutableListOf<SwiftUIView>()

        // Checkmark when selected
        if (component.selected && component.showCheckmark) {
            val checkmarkColor = component.checkmarkColor?.let { parseChipColor(it) }
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "systemName" to "checkmark",
                    "size" to 12f
                ),
                modifiers = checkmarkColor?.let {
                    listOf(SwiftUIModifier.foregroundColor(it))
                } ?: emptyList()
            ))
        }

        // Avatar if present
        component.avatar?.let { avatar ->
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "name" to avatar,
                    "size" to 18f
                )
            ))
        }

        // Label text
        children.add(SwiftUIView.text(
            content = component.label,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Subheadline)
            )
        ))

        return SwiftUIView(
            type = ViewType.Custom("ChipButton"),
            properties = mapOf(
                "selected" to component.selected,
                "enabled" to component.enabled,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf("spacing" to 4f),
                    children = children,
                    modifiers = modifiers
                )
            ),
        )
    }
}

// ============================================
// INPUT CHIP (MagicInput)
// ============================================

/**
 * Maps MagicInput/InputChip to SwiftUI deletable input chip
 *
 * SwiftUI Implementation:
 * ```swift
 * HStack(spacing: 4) {
 *     if let avatar { Image(avatar) }
 *     Text(label)
 *     Button(action: onDeleted) {
 *         Image(systemName: "xmark.circle.fill")
 *             .foregroundColor(.secondary)
 *     }
 * }
 * .padding(.horizontal, 12)
 * .padding(.vertical, 8)
 * .background(selected ? selectedColor : backgroundColor)
 * .clipShape(Capsule())
 * ```
 */
object InputChipMapper {
    fun map(component: MagicInput, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Background color based on selection state
        val backgroundColor = if (component.selected) {
            component.selectedColor?.let { parseChipColor(it) }
                ?: theme?.colorScheme?.secondaryContainer?.let { ModifierConverter.convertColor(it) }
                ?: SwiftUIColor.system("secondarySystemFill")
        } else {
            component.backgroundColor?.let { parseChipColor(it) }
                ?: SwiftUIColor.rgb(0.5f, 0.5f, 0.5f, 0.2f)
        }

        modifiers.add(SwiftUIModifier.background(backgroundColor))

        // Elevation shadow
        component.elevation?.let { elevation ->
            val shadowColor = if (component.selected) {
                component.selectedShadowColor?.let { parseChipColor(it) }
            } else {
                component.shadowColor?.let { parseChipColor(it) }
            } ?: SwiftUIColor.rgb(0f, 0f, 0f, 0.15f)

            modifiers.add(SwiftUIModifier.shadow(
                color = shadowColor,
                radius = elevation,
                x = 0f,
                y = elevation / 2
            ))
        }

        // Chip shape
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("clipShape" to "Capsule")
        ))

        // Padding
        val padding = component.padding?.let { extractSpacing(it) }
        if (padding != null) {
            modifiers.add(SwiftUIModifier.padding(padding.top, padding.left, padding.bottom, padding.right))
        } else {
            modifiers.add(SwiftUIModifier.padding(8f, 12f, 8f, 12f))
        }

        // Opacity for disabled state
        if (!component.enabled || !component.isEnabled) {
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        // Build children for HStack content
        val children = mutableListOf<SwiftUIView>()

        // Checkmark when selected
        if (component.selected && component.showCheckmark) {
            val checkmarkColor = component.checkmarkColor?.let { parseChipColor(it) }
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "systemName" to "checkmark",
                    "size" to 12f
                ),
                modifiers = checkmarkColor?.let {
                    listOf(SwiftUIModifier.foregroundColor(it))
                } ?: emptyList()
            ))
        }

        // Avatar if present
        component.avatar?.let { avatar ->
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "name" to avatar,
                    "size" to 18f
                )
            ))
        }

        // Label text
        children.add(SwiftUIView.text(
            content = component.label,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Subheadline)
            )
        ))

        // Delete button
        if (component.deleteIcon != null) {
            val deleteIconColor = component.deleteIconColor?.let { parseChipColor(it) }
                ?: SwiftUIColor.system("secondaryLabel")

            children.add(SwiftUIView(
                type = ViewType.Custom("DeleteButton"),
                properties = mapOf(
                    "icon" to (component.deleteIcon ?: "xmark.circle.fill"),
                    "tooltip" to component.getDeleteButtonAccessibilityDescription()
                ),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf(
                            "systemName" to "xmark.circle.fill",
                            "size" to 16f
                        ),
                        modifiers = listOf(
                            SwiftUIModifier.foregroundColor(deleteIconColor)
                        )
                    )
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.Custom("InputChip"),
            properties = mapOf(
                "selected" to component.selected,
                "enabled" to (component.enabled && component.isEnabled),
                "hasDeleteAction" to (component.deleteIcon != null),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf("spacing" to 4f),
                    children = children,
                    modifiers = modifiers
                )
            ),
        )
    }
}

// ============================================
// Helper Functions
// ============================================

/**
 * Parse color string to SwiftUIColor for chips
 */
private fun parseChipColor(colorString: String): SwiftUIColor {
    return when {
        colorString.startsWith("#") -> {
            // Hex color - convert to 0-1 float range for rgb()
            val hex = colorString.removePrefix("#")
            val r = hex.substring(0, 2).toInt(16) / 255f
            val g = hex.substring(2, 4).toInt(16) / 255f
            val b = hex.substring(4, 6).toInt(16) / 255f
            val a = if (hex.length == 8) hex.substring(6, 8).toInt(16) / 255f else 1f
            SwiftUIColor.rgb(r, g, b, a)
        }
        colorString.equals("primary", ignoreCase = true) -> SwiftUIColor.primary
        colorString.equals("secondary", ignoreCase = true) -> SwiftUIColor.secondary
        colorString.equals("accent", ignoreCase = true) -> SwiftUIColor.system("accentColor")
        else -> SwiftUIColor.system(colorString)
    }
}
