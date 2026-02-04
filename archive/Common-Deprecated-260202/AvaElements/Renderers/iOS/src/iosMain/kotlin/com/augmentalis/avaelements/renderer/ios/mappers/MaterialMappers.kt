package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.flutter.material.chips.*
import com.augmentalis.avaelements.flutter.material.lists.*
import com.augmentalis.avaelements.flutter.material.advanced.*

/**
 * iOS SwiftUI Mappers for Flutter Material Parity Components
 *
 * This file maps cross-platform Flutter Material components to iOS SwiftUI
 * bridge representations. The SwiftUI bridge models are consumed by Swift
 * code to render native iOS UI.
 *
 * Architecture:
 * Flutter Component → iOS Mapper → SwiftUIView Bridge → Swift → Native SwiftUI
 *
 * Components Implemented:
 * - Chips: FilterChip, InputChip, ActionChip, ChoiceChip
 * - Lists: ExpansionTile, CheckboxListTile, SwitchListTile
 * - Buttons: FilledButton
 * - Advanced: PopupMenuButton, CircleAvatar, RichText, SelectableText,
 *             VerticalDivider, FadeInImage, RefreshIndicator, IndexedStack
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Maps FilterChip to SwiftUI custom chip view
 *
 * SwiftUI doesn't have native chips, so we create a custom HStack with:
 * - Rounded capsule background
 * - Selection state (filled vs outlined)
 * - Optional checkmark icon when selected
 * - Optional leading avatar
 *
 * Visual parity with Material Design 3 chips maintained
 */
object FilterChipMapper {
    fun map(
        component: FilterChip,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Add checkmark if selected
        if (component.selected && component.showCheckmark) {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to "checkmark"),
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(14f),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                    )
                )
            )
        }
        // Add avatar if present and not selected
        else {
            val avatarIcon = component.avatar
            if (avatarIcon != null && !component.selected) {
                children.add(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to avatarIcon),
                        modifiers = listOf(
                            SwiftUIModifier.fontSize(14f)
                        )
                    )
                )
            }
        }

        // Add label text
        children.add(
            SwiftUIView.text(
                content = component.label,
                modifiers = listOf(
                    SwiftUIModifier.fontSize(14f)
                )
            )
        )

        val backgroundColor = if (component.selected) {
            SwiftUIColor.primary.copy(
                value = RGBValue(0.2f, 0.5f, 1.0f, 0.2f)
            )
        } else {
            SwiftUIColor.system("systemGray6")
        }

        val modifiers = mutableListOf<SwiftUIModifier>()
        modifiers.add(SwiftUIModifier.padding(12f, 16f, 12f, 16f))
        modifiers.add(SwiftUIModifier.background(backgroundColor))
        modifiers.add(SwiftUIModifier.cornerRadius(16f))

        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.disabled(true))
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        return SwiftUIView(
            type = ViewType.FilterChip,
            id = component.id,
            properties = mapOf(
                "label" to component.label,
                "selected" to component.selected,
                "enabled" to component.enabled,
                "avatar" to (component.avatar ?: ""),
                "showCheckmark" to component.showCheckmark,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = modifiers
        )
    }
}

/**
 * Maps InputChip to SwiftUI custom chip view with delete button
 *
 * Features:
 * - Optional leading avatar
 * - Delete button (X icon) on trailing edge
 * - Selection state
 * - Material Design 3 visual styling
 */
object InputChipMapper {
    fun map(
        component: InputChip,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Add avatar if present
        val avatarIcon = component.avatar
        if (avatarIcon != null) {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to avatarIcon),
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(14f)
                    )
                )
            )
        }

        // Add label
        children.add(
            SwiftUIView.text(
                content = component.label,
                modifiers = listOf(SwiftUIModifier.fontSize(14f))
            )
        )

        // Add delete button if callback provided
        if (component.onDeleted != null) {
            children.add(
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf(
                        "label" to "",
                        "action" to "delete"
                    ),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "xmark.circle.fill"),
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(14f),
                                SwiftUIModifier.foregroundColor(
                                    SwiftUIColor.system("systemGray")
                                )
                            )
                        )
                    )
                )
            )
        }

        val backgroundColor = if (component.selected) {
            SwiftUIColor.primary.copy(
                value = RGBValue(0.2f, 0.5f, 1.0f, 0.2f)
            )
        } else {
            SwiftUIColor.system("systemGray6")
        }

        return SwiftUIView(
            type = ViewType.InputChip,
            id = component.id,
            properties = mapOf(
                "label" to component.label,
                "selected" to component.selected,
                "enabled" to component.enabled,
                "avatar" to (component.avatar ?: ""),
                "hasDeleteAction" to (component.onDeleted != null),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(12f, 16f, 12f, 16f),
                SwiftUIModifier.background(backgroundColor),
                SwiftUIModifier.cornerRadius(16f),
                if (!component.enabled) SwiftUIModifier.opacity(0.5f)
                else SwiftUIModifier.opacity(1.0f)
            )
        )
    }
}

/**
 * Maps ActionChip to SwiftUI button-styled chip
 *
 * Similar to a compact button with chip appearance.
 * Uses AssistChip visual style from Material Design 3.
 */
object ActionChipMapper {
    fun map(
        component: ActionChip,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Add avatar if present
        val avatarIcon = component.avatar
        if (avatarIcon != null) {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to avatarIcon),
                    modifiers = listOf(SwiftUIModifier.fontSize(14f))
                )
            )
        }

        // Add label
        children.add(
            SwiftUIView.text(
                content = component.label,
                modifiers = listOf(SwiftUIModifier.fontSize(14f))
            )
        )

        return SwiftUIView(
            type = ViewType.ActionChip,
            id = component.id,
            properties = mapOf(
                "label" to component.label,
                "enabled" to component.enabled,
                "avatar" to (component.avatar ?: ""),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(12f, 16f, 12f, 16f),
                SwiftUIModifier.background(SwiftUIColor.system("systemGray6")),
                SwiftUIModifier.cornerRadius(16f),
                SwiftUIModifier.border(
                    SwiftUIColor.system("systemGray4"),
                    1f
                ),
                if (!component.enabled) SwiftUIModifier.opacity(0.5f)
                else SwiftUIModifier.opacity(1.0f)
            )
        )
    }
}

/**
 * Maps ChoiceChip to SwiftUI selectable chip
 *
 * For single-selection within a group (radio-button-like behavior).
 * Visual distinction between selected and unselected states.
 */
object ChoiceChipMapper {
    fun map(
        component: ChoiceChip,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Add checkmark if selected and showCheckmark is true
        if (component.selected && component.showCheckmark) {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to "checkmark"),
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(14f),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                    )
                )
            )
        }
        // Add avatar if present and not selected
        else {
            val avatarIcon = component.avatar
            if (avatarIcon != null && !component.selected) {
                children.add(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to avatarIcon),
                        modifiers = listOf(SwiftUIModifier.fontSize(14f))
                    )
                )
            }
        }

        // Add label
        children.add(
            SwiftUIView.text(
                content = component.label,
                modifiers = listOf(SwiftUIModifier.fontSize(14f))
            )
        )

        val backgroundColor = if (component.selected) {
            SwiftUIColor.primary
        } else {
            SwiftUIColor.system("systemGray6")
        }

        val textColor = if (component.selected) {
            SwiftUIColor.white
        } else {
            SwiftUIColor.primary
        }

        return SwiftUIView(
            type = ViewType.ChoiceChip,
            id = component.id,
            properties = mapOf(
                "label" to component.label,
                "selected" to component.selected,
                "enabled" to component.enabled,
                "avatar" to (component.avatar ?: ""),
                "showCheckmark" to component.showCheckmark,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(12f, 16f, 12f, 16f),
                SwiftUIModifier.background(backgroundColor),
                SwiftUIModifier.foregroundColor(textColor),
                SwiftUIModifier.cornerRadius(16f),
                if (!component.enabled) SwiftUIModifier.opacity(0.5f)
                else SwiftUIModifier.opacity(1.0f)
            )
        )
    }
}

/**
 * Maps ExpansionTile to SwiftUI DisclosureGroup
 *
 * SwiftUI's DisclosureGroup provides native expand/collapse with:
 * - Smooth animation (200ms like Material)
 * - Rotating disclosure indicator
 * - Accessibility support
 */
object ExpansionTileMapper {
    fun map(
        component: ExpansionTile,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val childViews = component.children.map { renderChild(it) }

        return SwiftUIView(
            type = ViewType.ExpansionTile,
            id = component.id,
            properties = mapOf(
                "title" to component.title,
                "subtitle" to (component.subtitle ?: ""),
                "leading" to (component.leading ?: ""),
                "initiallyExpanded" to component.initiallyExpanded,
                "accessibilityLabel" to component.getAccessibilityDescription(false)
            ),
            children = childViews,
            modifiers = emptyList()
        )
    }
}

/**
 * Maps CheckboxListTile to SwiftUI custom list row with Toggle
 *
 * Creates a horizontal stack with:
 * - Optional leading/trailing checkbox (Toggle in iOS)
 * - Title and optional subtitle
 * - Tap gesture to toggle state
 */
object CheckboxListTileMapper {
    fun map(
        component: CheckboxListTile,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        val isLeading = component.controlAffinity ==
            CheckboxListTile.ListTileControlAffinity.Leading

        // Create checkbox toggle
        val toggle = SwiftUIView(
            type = ViewType.Toggle,
            properties = mapOf(
                "isOn" to (component.value ?: false),
                "label" to ""
            ),
            modifiers = listOf(
                SwiftUIModifier.disabled(!component.enabled)
            )
        )

        // Add leading checkbox if applicable
        if (isLeading) {
            children.add(toggle)
        }

        // Add text content
        val textStack = mutableListOf<SwiftUIView>()
        textStack.add(
            SwiftUIView.text(
                content = component.title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.fontWeight(FontWeight.Medium)
                )
            )
        )
        val subtitleText = component.subtitle
        if (subtitleText != null) {
            textStack.add(
                SwiftUIView.text(
                    content = subtitleText,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.foregroundColor(
                            SwiftUIColor.system("secondaryLabel")
                        )
                    )
                )
            )
        }

        children.add(
            SwiftUIView.vStack(
                spacing = 4f,
                alignment = HorizontalAlignment.Leading,
                children = textStack
            )
        )

        // Add trailing checkbox if applicable
        if (!isLeading) {
            children.add(toggle)
        }

        return SwiftUIView(
            type = ViewType.CheckboxListTile,
            id = component.id,
            properties = mapOf(
                "title" to component.title,
                "subtitle" to (component.subtitle ?: ""),
                "value" to (component.value ?: false),
                "enabled" to component.enabled,
                "tristate" to component.tristate,
                "controlAffinity" to component.controlAffinity.name,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(16f)
            )
        )
    }
}

/**
 * Maps SwitchListTile to SwiftUI list row with Toggle
 *
 * Similar to CheckboxListTile but uses iOS-native Toggle (switch) styling
 */
object SwitchListTileMapper {
    fun map(
        component: SwitchListTile,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        val isLeading = component.controlAffinity ==
            SwitchListTile.ListTileControlAffinity.Leading

        // Create switch toggle
        val toggle = SwiftUIView(
            type = ViewType.Toggle,
            properties = mapOf(
                "isOn" to component.value,
                "label" to ""
            ),
            modifiers = listOf(
                SwiftUIModifier.disabled(!component.enabled)
            )
        )

        // Add leading switch if applicable
        if (isLeading) {
            children.add(toggle)
        }

        // Add text content
        val textStack = mutableListOf<SwiftUIView>()
        textStack.add(
            SwiftUIView.text(
                content = component.title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.fontWeight(FontWeight.Medium)
                )
            )
        )
        val subtitleText = component.subtitle
        if (subtitleText != null) {
            textStack.add(
                SwiftUIView.text(
                    content = subtitleText,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.foregroundColor(
                            SwiftUIColor.system("secondaryLabel")
                        )
                    )
                )
            )
        }

        children.add(
            SwiftUIView.vStack(
                spacing = 4f,
                alignment = HorizontalAlignment.Leading,
                children = textStack
            )
        )

        // Add trailing switch if applicable
        if (!isLeading) {
            children.add(toggle)
        }

        return SwiftUIView(
            type = ViewType.SwitchListTile,
            id = component.id,
            properties = mapOf(
                "title" to component.title,
                "subtitle" to (component.subtitle ?: ""),
                "value" to component.value,
                "enabled" to component.enabled,
                "controlAffinity" to component.controlAffinity.name,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(16f)
            )
        )
    }
}

/**
 * Maps FilledButton to SwiftUI Button with filled style
 *
 * Material 3 filled button maps to iOS borderedProminent button style
 * with custom Material-like appearance
 */
object FilledButtonMapper {
    fun map(
        component: FilledButton,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        val isLeadingIcon = component.iconPosition == FilledButton.IconPosition.Leading
        val buttonIcon = component.icon

        // Add leading icon if applicable
        if (buttonIcon != null && isLeadingIcon) {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to buttonIcon),
                    modifiers = listOf(SwiftUIModifier.fontSize(16f))
                )
            )
        }

        // Add button text
        children.add(
            SwiftUIView.text(
                content = component.text,
                modifiers = listOf(
                    SwiftUIModifier.fontWeight(FontWeight.Semibold)
                )
            )
        )

        // Add trailing icon if applicable
        if (buttonIcon != null && !isLeadingIcon) {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to buttonIcon),
                    modifiers = listOf(SwiftUIModifier.fontSize(16f))
                )
            )
        }

        val modifiers = mutableListOf<SwiftUIModifier>()
        modifiers.add(SwiftUIModifier.padding(16f, 24f, 16f, 24f))
        modifiers.add(SwiftUIModifier.background(SwiftUIColor.primary))
        modifiers.add(SwiftUIModifier.foregroundColor(SwiftUIColor.white))
        modifiers.add(SwiftUIModifier.cornerRadius(8f))

        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.disabled(true))
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        return SwiftUIView(
            type = ViewType.FilledButton,
            id = component.id,
            properties = mapOf(
                "text" to component.text,
                "enabled" to component.enabled,
                "icon" to (component.icon ?: ""),
                "iconPosition" to component.iconPosition.name,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = modifiers
        )
    }
}

/**
 * Maps PopupMenuButton to SwiftUI Menu
 *
 * SwiftUI Menu provides native popup menu with:
 * - Automatic positioning
 * - Keyboard navigation
 * - VoiceOver support
 */
object PopupMenuButtonMapper {
    fun map(
        component: PopupMenuButton,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val menuItems = component.items.map { item ->
            SwiftUIView(
                type = ViewType.Button,
                properties = mapOf(
                    "label" to item.text,
                    "action" to "menuItem_${item.value}",
                    "enabled" to item.enabled,
                    "icon" to (item.icon ?: "")
                )
            )
        }

        return SwiftUIView(
            type = ViewType.PopupMenu,
            id = component.id,
            properties = mapOf(
                "enabled" to component.enabled,
                "tooltip" to (component.tooltip ?: ""),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = menuItems,
            modifiers = emptyList()
        )
    }
}

/**
 * Maps CircleAvatar to SwiftUI Circle with Image/AsyncImage
 *
 * Creates circular clipped image view with:
 * - Customizable radius
 * - Background color
 * - Optional image or child content
 */
object CircleAvatarMapper {
    fun map(
        component: CircleAvatar,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val radius = component.getEffectiveRadius()
        val bgImage = component.backgroundImage
        val childComponent = component.child

        val content = if (bgImage != null) {
            // Use AsyncImage for remote images
            SwiftUIView(
                type = ViewType.Image,
                properties = mapOf(
                    "url" to bgImage,
                    "placeholder" to ""
                ),
                modifiers = listOf(
                    SwiftUIModifier.frame(
                        SizeValue.Fixed(radius * 2),
                        SizeValue.Fixed(radius * 2),
                        ZStackAlignment.Center
                    )
                )
            )
        } else if (childComponent != null) {
            renderChild(childComponent)
        } else {
            SwiftUIView(
                type = ViewType.EmptyView,
                properties = emptyMap()
            )
        }

        return SwiftUIView(
            type = ViewType.CircleAvatar,
            id = component.id,
            properties = mapOf(
                "radius" to radius,
                "backgroundColor" to (component.backgroundColor ?: ""),
                "hasImage" to (component.backgroundImage != null),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(content),
            modifiers = listOf(
                SwiftUIModifier.frame(
                    SizeValue.Fixed(radius * 2),
                    SizeValue.Fixed(radius * 2),
                    ZStackAlignment.Center
                ),
                SwiftUIModifier.background(
                    SwiftUIColor.system("systemGray5")
                ),
                SwiftUIModifier.cornerRadius(radius)
            )
        )
    }
}

/**
 * Maps RichText to SwiftUI Text with AttributedString
 *
 * Supports multiple text styles within a single text block:
 * - Font size, weight, style
 * - Colors
 * - Letter spacing
 */
object RichTextMapper {
    fun map(
        component: RichText,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        // SwiftUI will handle AttributedString in Swift layer
        return SwiftUIView(
            type = ViewType.RichText,
            id = component.id,
            properties = mapOf(
                "spans" to component.spans.map { span ->
                    mapOf(
                        "text" to span.text,
                        "color" to (span.style?.color ?: ""),
                        "fontSize" to (span.style?.fontSize ?: 14.0),
                        "fontWeight" to (span.style?.fontWeight ?: "normal"),
                        "fontStyle" to (span.style?.fontStyle ?: "normal"),
                        "letterSpacing" to (span.style?.letterSpacing ?: 0.0)
                    )
                },
                "textAlign" to component.textAlign.name,
                "overflow" to component.overflow.name,
                "maxLines" to (component.maxLines ?: Int.MAX_VALUE),
                "softWrap" to component.softWrap,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            modifiers = emptyList()
        )
    }
}

/**
 * Maps SelectableText to SwiftUI Text with .textSelection(.enabled)
 *
 * Enables text selection with iOS-native selection handles and copy support
 */
object SelectableTextMapper {
    fun map(
        component: SelectableText,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add font styling if present
        val textStyle = component.textStyle
        if (textStyle != null) {
            val styleFontSize = textStyle.fontSize
            if (styleFontSize != null) {
                modifiers.add(SwiftUIModifier.fontSize(styleFontSize))
            }
            val styleFontWeight = textStyle.fontWeight
            if (styleFontWeight != null) {
                val fontWeightValue = when (styleFontWeight) {
                    "bold" -> FontWeight.Bold
                    "normal" -> FontWeight.Regular
                    else -> FontWeight.Regular
                }
                modifiers.add(SwiftUIModifier.fontWeight(fontWeightValue))
            }
        }

        return SwiftUIView(
            type = ViewType.SelectableText,
            id = component.id,
            properties = mapOf(
                "text" to component.text,
                "textAlign" to component.textAlign.name,
                "maxLines" to (component.maxLines ?: Int.MAX_VALUE),
                "minLines" to (component.minLines ?: 1),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            modifiers = modifiers
        )
    }
}

/**
 * Maps VerticalDivider to SwiftUI Divider in vertical orientation
 *
 * SwiftUI Divider is horizontal by default, so we use a Rectangle
 * with frame constraints for vertical divider
 */
object VerticalDividerMapper {
    fun map(
        component: VerticalDivider,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val width = component.thickness
        val height = component.width ?: 40f

        return SwiftUIView(
            type = ViewType.VerticalDivider,
            id = component.id,
            properties = mapOf(
                "thickness" to width,
                "height" to height,
                "indent" to component.indent,
                "endIndent" to component.endIndent,
                "color" to (component.color ?: "")
            ),
            modifiers = listOf(
                SwiftUIModifier.frame(
                    SizeValue.Fixed(width),
                    SizeValue.Fixed(height),
                    ZStackAlignment.Center
                ),
                SwiftUIModifier.background(
                    SwiftUIColor.system("separator")
                ),
                SwiftUIModifier.padding(
                    component.indent, 0f, component.endIndent, 0f
                )
            )
        )
    }
}

/**
 * Maps FadeInImage to SwiftUI AsyncImage with fade transition
 *
 * Uses AsyncImage for remote image loading with:
 * - Placeholder during loading
 * - Fade-in animation when loaded
 * - Error state handling
 */
object FadeInImageMapper {
    fun map(
        component: FadeInImage,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()
        val imgWidth = component.width
        val imgHeight = component.height

        // Add size constraints if specified
        if (imgWidth != null && imgHeight != null) {
            modifiers.add(
                SwiftUIModifier.frame(
                    SizeValue.Fixed(imgWidth),
                    SizeValue.Fixed(imgHeight),
                    ZStackAlignment.Center
                )
            )
        } else if (imgWidth != null) {
            modifiers.add(
                SwiftUIModifier.frame(
                    SizeValue.Fixed(imgWidth),
                    null,
                    ZStackAlignment.Center
                )
            )
        } else if (imgHeight != null) {
            modifiers.add(
                SwiftUIModifier.frame(
                    null,
                    SizeValue.Fixed(imgHeight),
                    ZStackAlignment.Center
                )
            )
        }

        return SwiftUIView(
            type = ViewType.FadeInImage,
            id = component.id,
            properties = mapOf(
                "imageUrl" to component.image,
                "placeholderImage" to (component.placeholder ?: ""),
                "fadeDuration" to (component.fadeInDuration ?: 300),
                "accessibilityLabel" to (component.getAccessibilityDescription() ?: "")
            ),
            modifiers = modifiers
        )
    }
}

/**
 * Maps RefreshIndicator to SwiftUI RefreshControl
 *
 * Uses iOS native pull-to-refresh gesture with UIRefreshControl
 */
object RefreshIndicatorMapper {
    fun map(
        component: RefreshIndicator,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        // Child content will be wrapped in scrollable view with refresh control
        val children = component.child?.let { listOf(renderChild(it)) } ?: emptyList()

        return SwiftUIView(
            type = ViewType.RefreshControl,
            id = component.id,
            properties = mapOf(
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = emptyList()
        )
    }
}

/**
 * Maps IndexedStack to SwiftUI conditional rendering
 *
 * Shows single child by index while keeping all children in memory.
 * Uses ZStack with opacity to implement this behavior.
 */
object IndexedStackMapper {
    fun map(
        component: IndexedStack,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = component.children.mapIndexed { index, child ->
            val childView = renderChild(child)
            // Add opacity modifier based on whether this is the selected index
            childView.copy(
                modifiers = childView.modifiers + listOf(
                    SwiftUIModifier.opacity(
                        if (index == component.index) 1.0f else 0.0f
                    )
                )
            )
        }

        val alignment = when (component.alignment) {
            IndexedStack.Alignment.TopStart -> ZStackAlignment.TopLeading
            IndexedStack.Alignment.TopCenter -> ZStackAlignment.Top
            IndexedStack.Alignment.TopEnd -> ZStackAlignment.TopTrailing
            IndexedStack.Alignment.CenterStart -> ZStackAlignment.Leading
            IndexedStack.Alignment.Center -> ZStackAlignment.Center
            IndexedStack.Alignment.CenterEnd -> ZStackAlignment.Trailing
            IndexedStack.Alignment.BottomStart -> ZStackAlignment.BottomLeading
            IndexedStack.Alignment.BottomCenter -> ZStackAlignment.Bottom
            IndexedStack.Alignment.BottomEnd -> ZStackAlignment.BottomTrailing
        }

        return SwiftUIView(
            type = ViewType.IndexedStack,
            id = component.id,
            properties = mapOf(
                "index" to component.index,
                "alignment" to alignment.name,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = emptyList()
        )
    }
}
