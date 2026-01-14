package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.flutter.material.advanced.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Flutter Material Button Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity button components to SwiftUI equivalents:
 * - FilledButton → Button with .buttonStyle(.borderedProminent)
 * - CloseButton → Button with xmark icon
 * - LoadingButton → Button with ProgressView
 * - PopupMenuButton → Menu with Button
 *
 * SwiftUI Implementation Notes:
 * - Uses native SwiftUI Button and ButtonStyle
 * - Implements Material 3 visual states
 * - Supports icons and loading indicators
 * - Accessibility labels for VoiceOver
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// FILLED BUTTON
// ============================================

/**
 * Maps FilledButton to SwiftUI Button with borderedProminent style
 *
 * SwiftUI Implementation:
 * ```swift
 * Button(action: onPressed) {
 *     HStack(spacing: 8) {
 *         if let icon, iconPosition == .leading { Image(systemName: icon) }
 *         Text(text)
 *         if let icon, iconPosition == .trailing { Image(systemName: icon) }
 *     }
 * }
 * .buttonStyle(.borderedProminent)
 * .disabled(!enabled)
 * ```
 */
object FilledButtonMapper {
    fun map(component: FilledButton, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Button style
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("buttonStyle" to "borderedProminent")
        ))

        // Disabled state
        if (!component.enabled) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("disabled" to true)
            ))
        }

        // Apply theme tint if available
        theme?.colorScheme?.primary?.let { primary ->
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("tint" to ModifierConverter.convertColor(primary))
            ))
        }

        // Build button content
        val children = mutableListOf<SwiftUIView>()
        val icon = component.icon

        // Leading icon
        if (icon != null && component.iconPosition == FilledButton.IconPosition.Leading) {
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "systemName" to icon,
                    "size" to 16f
                )
            ))
        }

        // Text
        children.add(SwiftUIView.text(
            content = component.text,
            modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
        ))

        // Trailing icon
        if (icon != null && component.iconPosition == FilledButton.IconPosition.Trailing) {
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "systemName" to icon,
                    "size" to 16f
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.Custom("Button"),
            properties = mapOf(
                "enabled" to component.enabled,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf("spacing" to 8f),
                    children = children
                )
            ),
            modifiers = modifiers,
        )
    }
}

// ============================================
// CLOSE BUTTON
// ============================================

/**
 * Maps CloseButton to SwiftUI Button with xmark icon
 *
 * SwiftUI Implementation:
 * ```swift
 * Button(action: onPressed) {
 *     Image(systemName: "xmark")
 *         .font(.system(size: iconSize))
 * }
 * .buttonStyle(.plain)
 * .disabled(!enabled)
 * ```
 */
object CloseButtonMapper {
    fun map(component: CloseButton, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Button style
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

        // Minimum touch target (48dp)
        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Fixed(48f),
            height = SizeValue.Fixed(48f)
        ))

        // Icon size based on button size
        val iconSize = component.getIconSizeInPixels().toFloat()

        return SwiftUIView(
            type = ViewType.Custom("Button"),
            properties = mapOf(
                "enabled" to component.enabled,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf(
                        "systemName" to "xmark",
                        "size" to iconSize
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("label"))
                    )
                )
            ),
            modifiers = modifiers,
        )
    }
}

// ============================================
// LOADING BUTTON
// ============================================

/**
 * Maps LoadingButton to SwiftUI Button with ProgressView
 *
 * SwiftUI Implementation:
 * ```swift
 * Button(action: onPressed) {
 *     HStack(spacing: 8) {
 *         if loading && loadingPosition == .start {
 *             ProgressView()
 *         }
 *         if !loading || loadingPosition != .center {
 *             Text(displayText)
 *         }
 *         if loading && loadingPosition == .center {
 *             ProgressView()
 *         }
 *         if loading && loadingPosition == .end {
 *             ProgressView()
 *         }
 *     }
 * }
 * .disabled(loading || !enabled)
 * ```
 */
object LoadingButtonMapper {
    fun map(component: LoadingButton, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Button style
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("buttonStyle" to "borderedProminent")
        ))

        // Disabled state (loading or explicitly disabled)
        if (component.isDisabled()) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("disabled" to true)
            ))
        }

        // Build button content
        val children = mutableListOf<SwiftUIView>()

        // Loading indicator at start
        if (component.loading && component.loadingPosition == LoadingButton.LoadingPosition.Start) {
            children.add(createProgressView())
        }

        // Text (hidden if loading at center)
        if (!component.loading || component.loadingPosition != LoadingButton.LoadingPosition.Center) {
            children.add(SwiftUIView.text(
                content = component.getDisplayText(),
                modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
            ))
        }

        // Loading indicator at center
        if (component.loading && component.loadingPosition == LoadingButton.LoadingPosition.Center) {
            children.add(createProgressView())
        }

        // Loading indicator at end
        if (component.loading && component.loadingPosition == LoadingButton.LoadingPosition.End) {
            children.add(createProgressView())
        }

        return SwiftUIView(
            type = ViewType.Custom("Button"),
            properties = mapOf(
                "enabled" to !component.isDisabled(),
                "loading" to component.loading,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf("spacing" to 8f),
                    children = children
                )
            ),
            modifiers = modifiers,
        )
    }

    private fun createProgressView(): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Custom("ProgressView"),
            properties = mapOf(
                "style" to "circular",
                "tint" to "white"
            ),
            modifiers = listOf(
                SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("scaleEffect" to 0.8f)
                )
            )
        )
    }
}

// ============================================
// POPUP MENU BUTTON
// ============================================

/**
 * Maps PopupMenuButton to SwiftUI Menu
 *
 * SwiftUI Implementation:
 * ```swift
 * Menu {
 *     ForEach(items) { item in
 *         Button(item.text, action: { onSelected(item.value) })
 *     }
 * } label: {
 *     Image(systemName: icon)
 * }
 * ```
 */
object PopupMenuButtonMapper {
    fun map(component: PopupMenuButton, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Disabled state
        if (!component.enabled) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("disabled" to true)
            ))
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        // Build menu items
        val menuItems = component.items.map { item ->
            SwiftUIView(
                type = ViewType.Custom("MenuItem"),
                properties = mapOf(
                    "value" to item.value,
                    "text" to item.text,
                    "icon" to (item.icon ?: ""),
                    "enabled" to item.enabled
                )
            )
        }

        // Button label content
        val child = component.child
        val labelContent = if (child != null) {
            renderChild(child)
        } else {
            SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "systemName" to (component.icon ?: "ellipsis"),
                    "size" to (component.iconSize ?: 24f)
                )
            )
        }

        return SwiftUIView(
            type = ViewType.Custom("Menu"),
            properties = mapOf(
                "enabled" to component.enabled,
                "position" to component.position.name.lowercase(),
                "tooltip" to (component.tooltip ?: ""),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = menuItems + labelContent,
            modifiers = modifiers,
        )
    }
}

// ============================================
// Additional Button Type Mappers
// These map common button variants to SwiftUI styles
// ============================================

/**
 * Common button mapper for Elevated, Outlined, Text, and Tonal buttons
 * These are mapped using the same base Button component with different styles
 */
object CommonButtonMapper {

    /**
     * Map ElevatedButton style
     */
    fun mapElevatedStyle(): List<SwiftUIModifier> {
        return listOf(
            SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("buttonStyle" to "bordered")
            ),
            SwiftUIModifier.shadow(
                color = SwiftUIColor.rgb(0f, 0f, 0f, 0.15f),
                radius = 3f,
                x = 0f,
                y = 2f
            )
        )
    }

    /**
     * Map OutlinedButton style
     */
    fun mapOutlinedStyle(theme: Theme?): List<SwiftUIModifier> {
        val borderColor = theme?.colorScheme?.outline?.let {
            ModifierConverter.convertColor(it)
        } ?: SwiftUIColor.system("separator")

        return listOf(
            SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("buttonStyle" to "bordered")
            ),
            SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf(
                    "overlay" to mapOf(
                        "type" to "RoundedRectangle",
                        "cornerRadius" to 8f,
                        "stroke" to borderColor
                    )
                )
            )
        )
    }

    /**
     * Map TextButton style
     */
    fun mapTextStyle(): List<SwiftUIModifier> {
        return listOf(
            SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("buttonStyle" to "plain")
            )
        )
    }

    /**
     * Map FilledTonalButton style
     */
    fun mapFilledTonalStyle(theme: Theme?): List<SwiftUIModifier> {
        val containerColor = theme?.colorScheme?.secondaryContainer?.let {
            ModifierConverter.convertColor(it)
        } ?: SwiftUIColor.system("secondarySystemFill")

        return listOf(
            SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("buttonStyle" to "borderedProminent")
            ),
            SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("tint" to containerColor)
            )
        )
    }

    /**
     * Create a generic button view with specified style modifiers
     */
    fun createButtonView(
        text: String,
        icon: String?,
        iconPosition: String,
        enabled: Boolean,
        styleModifiers: List<SwiftUIModifier>,
        accessibilityLabel: String,
        id: String?
    ): SwiftUIView {
        val modifiers = styleModifiers.toMutableList()

        if (!enabled) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("disabled" to true)
            ))
        }

        val children = mutableListOf<SwiftUIView>()

        // Leading icon
        if (icon != null && iconPosition == "leading") {
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to icon, "size" to 16f)
            ))
        }

        // Text
        children.add(SwiftUIView.text(content = text, modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))))

        // Trailing icon
        if (icon != null && iconPosition == "trailing") {
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to icon, "size" to 16f)
            ))
        }

        return SwiftUIView(
            type = ViewType.Custom("Button"),
            properties = mapOf(
                "enabled" to enabled,
                "accessibilityLabel" to accessibilityLabel
            ),
            children = listOf(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf("spacing" to 8f),
                    children = children
                )
            ),
            modifiers = modifiers,
            id = id
        )
    }
}

// ============================================
// FAB (Floating Action Button)
// ============================================

/**
 * Maps FloatingActionButton to SwiftUI circular elevated button
 */
object FloatingActionButtonMapper {
    fun map(
        icon: String,
        size: Float = 56f,
        enabled: Boolean = true,
        theme: Theme?,
        id: String? = null
    ): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Primary color background
        val backgroundColor = theme?.colorScheme?.primaryContainer?.let {
            ModifierConverter.convertColor(it)
        } ?: SwiftUIColor.system("accentColor")

        modifiers.add(SwiftUIModifier.background(backgroundColor))
        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Fixed(size),
            height = SizeValue.Fixed(size)
        ))
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("clipShape" to "Circle")
        ))
        modifiers.add(SwiftUIModifier.shadow(
            color = SwiftUIColor.rgb(0f, 0f, 0f, 0.2f),
            radius = 6f,
            x = 0f,
            y = 3f
        ))

        if (!enabled) {
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        val iconColor = theme?.colorScheme?.onPrimaryContainer?.let {
            ModifierConverter.convertColor(it)
        } ?: SwiftUIColor.white

        return SwiftUIView(
            type = ViewType.Custom("Button"),
            properties = mapOf(
                "enabled" to enabled,
                "accessibilityLabel" to "Floating action button"
            ),
            children = listOf(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf(
                        "systemName" to icon,
                        "size" to (size * 0.4f)
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.foregroundColor(iconColor)
                    )
                )
            ),
            modifiers = modifiers,
            id = id
        )
    }
}

// ============================================
// Icon Button
// ============================================

/**
 * Maps IconButton to SwiftUI circular button with icon
 */
object IconButtonMapper {
    fun map(
        icon: String,
        size: Float = 48f,
        enabled: Boolean = true,
        selected: Boolean = false,
        theme: Theme?,
        contentDescription: String = "",
        id: String? = null
    ): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("buttonStyle" to "plain")
        ))
        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Fixed(size),
            height = SizeValue.Fixed(size)
        ))

        if (selected) {
            val selectedColor = theme?.colorScheme?.secondaryContainer?.let {
                ModifierConverter.convertColor(it)
            } ?: SwiftUIColor.system("secondarySystemFill")
            modifiers.add(SwiftUIModifier.background(selectedColor))
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("clipShape" to "Circle")
            ))
        }

        if (!enabled) {
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        val iconColor = if (selected) {
            theme?.colorScheme?.onSecondaryContainer?.let { ModifierConverter.convertColor(it) }
                ?: SwiftUIColor.primary
        } else {
            theme?.colorScheme?.onSurface?.let { ModifierConverter.convertColor(it) }
                ?: SwiftUIColor.system("label")
        }

        return SwiftUIView(
            type = ViewType.Custom("Button"),
            properties = mapOf(
                "enabled" to enabled,
                "selected" to selected,
                "accessibilityLabel" to contentDescription
            ),
            children = listOf(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf(
                        "systemName" to icon,
                        "size" to (size * 0.5f)
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.foregroundColor(iconColor)
                    )
                )
            ),
            modifiers = modifiers,
            id = id
        )
    }
}

// ============================================
// Segmented Button
// ============================================

/**
 * Maps SegmentedButton to SwiftUI Picker with segmented style
 */
object SegmentedButtonMapper {
    fun map(
        segments: List<String>,
        selectedIndex: Int,
        enabled: Boolean = true,
        theme: Theme?,
        id: String? = null
    ): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("pickerStyle" to "segmented")
        ))

        if (!enabled) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("disabled" to true)
            ))
        }

        val segmentViews = segments.mapIndexed { index, text ->
            SwiftUIView(
                type = ViewType.Text,
                properties = mapOf(
                    "text" to text,
                    "tag" to index
                )
            )
        }

        return SwiftUIView(
            type = ViewType.Custom("Picker"),
            properties = mapOf(
                "selection" to selectedIndex,
                "enabled" to enabled,
                "accessibilityLabel" to "Segmented control"
            ),
            children = segmentViews,
            modifiers = modifiers,
            id = id
        )
    }
}

// ============================================
// Button Bar
// ============================================

/**
 * Maps ButtonBar to SwiftUI HStack with buttons
 */
object ButtonBarMapper {
    fun map(
        buttons: List<SwiftUIView>,
        alignment: String = "trailing",
        spacing: Float = 8f,
        theme: Theme?,
        id: String? = null
    ): SwiftUIView {
        val frameAlignment = when (alignment) {
            "leading" -> ZStackAlignment.Leading
            "center" -> ZStackAlignment.Center
            else -> ZStackAlignment.Trailing
        }

        return SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf(
                "alignment" to alignment,
                "spacing" to spacing
            ),
            children = buttons,
            modifiers = listOf(
                SwiftUIModifier.frame(width = SizeValue.Infinity, height = null, alignment = frameAlignment)
            ),
            id = id
        )
    }
}
