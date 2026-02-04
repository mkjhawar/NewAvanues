package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.flutter.material.lists.*
import com.augmentalis.avaelements.flutter.material.data.RadioListTile
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Flutter Material List Tile Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity list tile components to SwiftUI equivalents:
 * - ExpansionTile → DisclosureGroup
 * - CheckboxListTile → Toggle with HStack layout
 * - SwitchListTile → Toggle with HStack layout
 * - RadioListTile → Picker with radioGroup style
 *
 * SwiftUI Implementation Notes:
 * - Uses native SwiftUI DisclosureGroup for expansion tiles
 * - Implements list tiles with HStack and control elements
 * - Supports leading/trailing control positioning
 * - Accessibility labels for VoiceOver
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// EXPANSION TILE
// ============================================

/**
 * Maps ExpansionTile to SwiftUI DisclosureGroup
 *
 * SwiftUI Implementation:
 * ```swift
 * DisclosureGroup(
 *     isExpanded: $isExpanded,
 *     content: {
 *         VStack(alignment: .leading, spacing: 0) {
 *             ForEach(children) { child }
 *         }
 *         .padding(childrenPadding)
 *     },
 *     label: {
 *         HStack(spacing: 8) {
 *             if let leading { Image(systemName: leading) }
 *             VStack(alignment: .leading, spacing: 2) {
 *                 Text(title)
 *                 if let subtitle { Text(subtitle).font(.caption) }
 *             }
 *         }
 *     }
 * )
 * .padding(tilePadding)
 * ```
 */
object ExpansionTileMapper {
    fun map(component: ExpansionTile, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Tile padding
        val tilePadding = component.tilePadding
        if (tilePadding != null) {
            modifiers.add(SwiftUIModifier.padding(
                tilePadding.top,
                tilePadding.left,
                tilePadding.bottom,
                tilePadding.right
            ))
        }

        // Background colors
        val bgColor = component.backgroundColor ?: component.collapsedBackgroundColor
        if (bgColor != null) {
            modifiers.add(SwiftUIModifier.background(parseColor(bgColor)))
        }

        // Build label content (HStack with leading icon, title, subtitle)
        val labelChildren = mutableListOf<SwiftUIView>()

        // Leading icon if present
        component.leading?.let { leading ->
            val iconColor = component.iconColor ?: component.collapsedIconColor
            labelChildren.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "systemName" to leading,
                    "size" to 20f
                ),
                modifiers = iconColor?.let {
                    listOf(SwiftUIModifier.foregroundColor(parseColor(it)))
                } ?: emptyList()
            ))
        }

        // Title and subtitle in VStack
        val titleChildren = mutableListOf<SwiftUIView>()

        val textColor = component.textColor ?: component.collapsedTextColor
        titleChildren.add(SwiftUIView.text(
            content = component.title,
            modifiers = listOfNotNull(
                SwiftUIModifier.font(FontStyle.Body),
                textColor?.let { SwiftUIModifier.foregroundColor(parseColor(it)) }
            )
        ))

        component.subtitle?.let { subtitle ->
            titleChildren.add(SwiftUIView.text(
                content = subtitle,
                modifiers = listOfNotNull(
                    SwiftUIModifier.font(FontStyle.Caption),
                    textColor?.let { SwiftUIModifier.foregroundColor(parseColor(it)) }
                )
            ))
        }

        labelChildren.add(SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf(
                "alignment" to "leading",
                "spacing" to 2f
            ),
            children = titleChildren
        ))

        val labelView = SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 8f),
            children = labelChildren
        )

        // Build content (children)
        val childrenViews = component.children.map { renderChild(it) }

        // Map cross-axis alignment
        val alignment = when (component.expandedCrossAxisAlignment) {
            ExpansionTile.CrossAxisAlignment.Start -> "leading"
            ExpansionTile.CrossAxisAlignment.Center -> "center"
            ExpansionTile.CrossAxisAlignment.End -> "trailing"
            ExpansionTile.CrossAxisAlignment.Stretch -> "leading" // Approximate
        }

        val childrenContainer = SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf(
                "alignment" to alignment,
                "spacing" to 0f
            ),
            children = childrenViews,
            modifiers = component.childrenPadding?.let {
                listOf(SwiftUIModifier.padding(it.top, it.left, it.bottom, it.right))
            } ?: emptyList()
        )

        return SwiftUIView(
            type = ViewType.Custom("DisclosureGroup"),
            properties = mapOf(
                "isExpanded" to component.initiallyExpanded,
                "accessibilityLabel" to component.getAccessibilityDescription(component.initiallyExpanded)
            ),
            children = listOf(labelView, childrenContainer),
            modifiers = modifiers,
        )
    }
}

// ============================================
// CHECKBOX LIST TILE
// ============================================

/**
 * Maps CheckboxListTile to SwiftUI HStack with Toggle
 *
 * SwiftUI Implementation:
 * ```swift
 * Toggle(isOn: $value) {
 *     HStack(spacing: 12) {
 *         if controlAffinity == .leading {
 *             // Toggle appears automatically on left in this case
 *         }
 *         if let secondary { Image(systemName: secondary) }
 *         VStack(alignment: .leading, spacing: 2) {
 *             Text(title)
 *             if let subtitle { Text(subtitle).font(.caption) }
 *         }
 *         Spacer()
 *     }
 * }
 * .toggleStyle(.checkbox)
 * .disabled(!enabled)
 * ```
 */
object CheckboxListTileMapper {
    fun map(component: CheckboxListTile, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Toggle style for checkbox appearance
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("toggleStyle" to "checkbox")
        ))

        // Disabled state
        if (!component.enabled) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("disabled" to true)
            ))
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        // Background colors
        val bgColor = if (component.selected) {
            component.selectedTileColor ?: component.tileColor
        } else {
            component.tileColor
        }
        if (bgColor != null) {
            modifiers.add(SwiftUIModifier.background(parseColor(bgColor)))
        }

        // Content padding
        component.contentPadding?.let { padding ->
            modifiers.add(SwiftUIModifier.padding(
                padding.top,
                padding.left,
                padding.bottom,
                padding.right
            ))
        }

        // Build label content
        val labelChildren = mutableListOf<SwiftUIView>()

        // Secondary icon if present
        component.secondary?.let { secondary ->
            labelChildren.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "systemName" to secondary,
                    "size" to 24f
                )
            ))
        }

        // Title and subtitle in VStack
        val titleChildren = mutableListOf<SwiftUIView>()

        titleChildren.add(SwiftUIView.text(
            content = component.title,
            modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
        ))

        component.subtitle?.let { subtitle ->
            titleChildren.add(SwiftUIView.text(
                content = subtitle,
                modifiers = listOf(SwiftUIModifier.font(FontStyle.Caption))
            ))
        }

        labelChildren.add(SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf(
                "alignment" to "leading",
                "spacing" to 2f
            ),
            children = titleChildren
        ))

        // Spacer to push content
        labelChildren.add(SwiftUIView(
            type = ViewType.Spacer,
            properties = emptyMap()
        ))

        val labelView = SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 12f),
            children = labelChildren
        )

        return SwiftUIView(
            type = ViewType.Custom("Toggle"),
            properties = mapOf(
                "isOn" to (component.value ?: false),
                "enabled" to component.enabled,
                "controlAffinity" to component.controlAffinity.name.lowercase(),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(labelView),
            modifiers = modifiers,
        )
    }
}

// ============================================
// SWITCH LIST TILE
// ============================================

/**
 * Maps SwitchListTile to SwiftUI HStack with Toggle
 *
 * SwiftUI Implementation:
 * ```swift
 * Toggle(isOn: $value) {
 *     HStack(spacing: 12) {
 *         if let secondary { Image(systemName: secondary) }
 *         VStack(alignment: .leading, spacing: 2) {
 *             Text(title)
 *             if let subtitle { Text(subtitle).font(.caption) }
 *         }
 *         Spacer()
 *     }
 * }
 * .toggleStyle(.switch)
 * .disabled(!enabled)
 * ```
 */
object SwitchListTileMapper {
    fun map(component: SwitchListTile, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Toggle style for switch appearance
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("toggleStyle" to "switch")
        ))

        // Disabled state
        if (!component.enabled) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("disabled" to true)
            ))
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        // Background colors
        val bgColor = if (component.selected) {
            component.selectedTileColor ?: component.tileColor
        } else {
            component.tileColor
        }
        if (bgColor != null) {
            modifiers.add(SwiftUIModifier.background(parseColor(bgColor)))
        }

        // Tint color for switch
        component.activeColor?.let { color ->
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("tint" to parseColor(color))
            ))
        }

        // Content padding
        component.contentPadding?.let { padding ->
            modifiers.add(SwiftUIModifier.padding(
                padding.top,
                padding.left,
                padding.bottom,
                padding.right
            ))
        }

        // Build label content
        val labelChildren = mutableListOf<SwiftUIView>()

        // Secondary icon if present
        component.secondary?.let { secondary ->
            labelChildren.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "systemName" to secondary,
                    "size" to 24f
                )
            ))
        }

        // Title and subtitle in VStack
        val titleChildren = mutableListOf<SwiftUIView>()

        titleChildren.add(SwiftUIView.text(
            content = component.title,
            modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
        ))

        component.subtitle?.let { subtitle ->
            titleChildren.add(SwiftUIView.text(
                content = subtitle,
                modifiers = listOf(SwiftUIModifier.font(FontStyle.Caption))
            ))
        }

        labelChildren.add(SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf(
                "alignment" to "leading",
                "spacing" to 2f
            ),
            children = titleChildren
        ))

        // Spacer to push content
        labelChildren.add(SwiftUIView(
            type = ViewType.Spacer,
            properties = emptyMap()
        ))

        val labelView = SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 12f),
            children = labelChildren
        )

        return SwiftUIView(
            type = ViewType.Custom("Toggle"),
            properties = mapOf(
                "isOn" to component.value,
                "enabled" to component.enabled,
                "controlAffinity" to component.controlAffinity.name.lowercase(),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(labelView),
            modifiers = modifiers,
        )
    }
}

// ============================================
// RADIO LIST TILE
// ============================================

/**
 * Maps RadioListTile to SwiftUI HStack with custom radio button
 *
 * SwiftUI Implementation:
 * ```swift
 * Button(action: { onChanged(value) }) {
 *     HStack(spacing: 12) {
 *         if controlAffinity == .leading {
 *             Image(systemName: isSelected ? "largecircle.fill.circle" : "circle")
 *         }
 *         if let secondary { Image(systemName: secondary) }
 *         VStack(alignment: .leading, spacing: 2) {
 *             Text(title)
 *             if let subtitle { Text(subtitle).font(.caption) }
 *         }
 *         Spacer()
 *         if controlAffinity == .trailing {
 *             Image(systemName: isSelected ? "largecircle.fill.circle" : "circle")
 *         }
 *     }
 * }
 * .disabled(!enabled)
 * ```
 */
object RadioListTileMapper {
    fun map(component: RadioListTile, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Button style for plain appearance
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("buttonStyle" to "plain")
        ))

        // Disabled state
        if (!component.enabled) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("disabled" to true)
            ))
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        // Background colors
        val bgColor = if (component.selected || component.isSelected) {
            component.selectedTileColor ?: component.tileColor
        } else {
            component.tileColor
        }
        if (bgColor != null) {
            modifiers.add(SwiftUIModifier.background(parseColor(bgColor)))
        }

        // Content padding
        component.contentPadding?.let { padding ->
            modifiers.add(SwiftUIModifier.padding(
                padding.top,
                padding.left,
                padding.bottom,
                padding.right
            ))
        }

        // Build content
        val contentChildren = mutableListOf<SwiftUIView>()

        // Radio button icon (leading)
        if (component.controlAffinity == RadioListTile.ListTileControlAffinity.Leading) {
            val radioIcon = if (component.isSelected) "largecircle.fill.circle" else "circle"
            val radioColor = component.activeColor?.let { parseColor(it) }
                ?: theme?.colorScheme?.primary?.let { ModifierConverter.convertColor(it) }
                ?: SwiftUIColor.system("accentColor")

            contentChildren.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "systemName" to radioIcon,
                    "size" to 20f
                ),
                modifiers = listOf(SwiftUIModifier.foregroundColor(radioColor))
            ))
        }

        // Secondary icon if present
        component.secondary?.let { secondary ->
            contentChildren.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "systemName" to secondary,
                    "size" to 24f
                )
            ))
        }

        // Title and subtitle in VStack
        val titleChildren = mutableListOf<SwiftUIView>()

        titleChildren.add(SwiftUIView.text(
            content = component.title,
            modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
        ))

        component.subtitle?.let { subtitle ->
            titleChildren.add(SwiftUIView.text(
                content = subtitle,
                modifiers = listOf(SwiftUIModifier.font(FontStyle.Caption))
            ))
        }

        contentChildren.add(SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf(
                "alignment" to "leading",
                "spacing" to 2f
            ),
            children = titleChildren
        ))

        // Spacer to push content
        contentChildren.add(SwiftUIView(
            type = ViewType.Spacer,
            properties = emptyMap()
        ))

        // Radio button icon (trailing)
        if (component.controlAffinity == RadioListTile.ListTileControlAffinity.Trailing) {
            val radioIcon = if (component.isSelected) "largecircle.fill.circle" else "circle"
            val radioColor = component.activeColor?.let { parseColor(it) }
                ?: theme?.colorScheme?.primary?.let { ModifierConverter.convertColor(it) }
                ?: SwiftUIColor.system("accentColor")

            contentChildren.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "systemName" to radioIcon,
                    "size" to 20f
                ),
                modifiers = listOf(SwiftUIModifier.foregroundColor(radioColor))
            ))
        }

        val contentView = SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 12f),
            children = contentChildren
        )

        return SwiftUIView(
            type = ViewType.Custom("Button"),
            properties = mapOf(
                "value" to component.value,
                "groupValue" to (component.groupValue ?: ""),
                "isSelected" to component.isSelected,
                "enabled" to component.enabled,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(contentView),
            modifiers = modifiers,
        )
    }
}

// ============================================
// Helper Functions
// ============================================

/**
 * Parse color string to SwiftUIColor
 */
private fun parseColor(colorString: String): SwiftUIColor {
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
