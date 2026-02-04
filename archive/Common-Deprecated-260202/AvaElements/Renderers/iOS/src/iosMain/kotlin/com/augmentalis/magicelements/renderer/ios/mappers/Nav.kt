package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.flutter.material.navigation.*
import com.augmentalis.avaelements.flutter.material.layout.MasonryGrid
import com.augmentalis.avaelements.flutter.material.layout.AspectRatio
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Flutter Material Navigation Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity navigation components to SwiftUI equivalents.
 *
 * Components:
 * - Menu → Menu with buttons
 * - Sidebar → NavigationSplitView or List
 * - NavLink → NavigationLink
 * - ProgressStepper → HStack with step indicators
 * - MenuBar → Toolbar or menu bar items
 * - SubMenu → Menu with nested items
 * - VerticalTabs → List with selection
 * - MasonryGrid → LazyVGrid with flexible columns
 * - AspectRatio → aspectRatio modifier
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// MENU
// ============================================

object MenuMapper {
    fun map(component: Menu, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Menu items
        val menuItems = component.items.map { item ->
            SwiftUIView(
                type = ViewType.Custom("Button"),
                properties = mapOf(
                    "text" to item.label,
                    "icon" to (item.icon ?: ""),
                    "enabled" to item.enabled
                ),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.HStack,
                        properties = mapOf("spacing" to 8f),
                        children = listOfNotNull(
                            item.icon?.let {
                                SwiftUIView(
                                    type = ViewType.Image,
                                    properties = mapOf("systemName" to it, "size" to 16f)
                                )
                            },
                            SwiftUIView.text(
                                content = item.label,
                                modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
                            )
                        )
                    )
                ),
                modifiers = listOf(
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("buttonStyle" to "plain")
                    )
                )
            )
        }

        // Label - use first item's label or ellipsis icon
        val labelView = if (component.items.isNotEmpty()) {
            SwiftUIView.text(
                content = component.items.first().label,
                modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
            )
        } else {
            SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to "ellipsis", "size" to 20f)
            )
        }

        return SwiftUIView(
            type = ViewType.Custom("Menu"),
            properties = emptyMap(),
            children = menuItems + listOf(labelView),
            modifiers = modifiers,
        )
    }
}

// ============================================
// SIDEBAR
// ============================================

object SidebarMapper {
    fun map(component: Sidebar, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Fixed(component.width),
            height = null
        ))
        modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("systemGroupedBackground")))

        val children = mutableListOf<SwiftUIView>()

        // Header if present
        component.headerContent?.let { header ->
            children.add(SwiftUIView.text(
                content = header,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Headline),
                    SwiftUIModifier.padding(16f, 16f, 8f, 16f)
                )
            ))
        }

        // Items
        val itemViews = component.items.map { item ->
            SwiftUIView(
                type = ViewType.Custom("NavigationLink"),
                properties = mapOf(
                    "label" to item.label,
                    "icon" to (item.icon ?: ""),
                    "isSelected" to item.selected
                ),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.HStack,
                        properties = mapOf("spacing" to 12f),
                        children = listOfNotNull(
                            item.icon?.let {
                                SwiftUIView(
                                    type = ViewType.Image,
                                    properties = mapOf("systemName" to it, "size" to 20f)
                                )
                            },
                            SwiftUIView.text(
                                content = item.label,
                                modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
                            )
                        ),
                        modifiers = listOf(
                            SwiftUIModifier.padding(12f, 16f, 12f, 16f)
                        )
                    )
                )
            )
        }

        children.addAll(itemViews)

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 0f),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// NAV LINK
// ============================================

object NavLinkMapper {
    fun map(component: NavLink, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Styling based on active state and custom colors
        if (component.active) {
            val activeColor = component.activeColor?.let { parseColor(it) }
                ?: SwiftUIColor.system("accentColor")
            modifiers.add(SwiftUIModifier.foregroundColor(activeColor))
        } else {
            val inactiveColor = component.inactiveColor?.let { parseColor(it) }
                ?: SwiftUIColor.secondary
            modifiers.add(SwiftUIModifier.foregroundColor(inactiveColor))
        }

        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        val children = mutableListOf<SwiftUIView>()

        // Icon if present
        component.icon?.let { icon ->
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to icon, "size" to 16f)
            ))
        }

        // Label
        children.add(SwiftUIView.text(
            content = component.label,
            modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
        ))

        // Badge if present
        component.badge?.let { badge ->
            children.add(SwiftUIView.text(
                content = badge,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption2),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                    SwiftUIModifier.padding(2f, 6f, 2f, 6f),
                    SwiftUIModifier.background(SwiftUIColor.system("systemRed")),
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("cornerRadius" to 10f)
                    )
                )
            ))
        }

        // Active indicator if selected
        if (component.active) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("underline" to true)
            ))
        }

        return SwiftUIView(
            type = ViewType.Custom("NavigationLink"),
            properties = mapOf(
                "destination" to component.href,
                "isActive" to component.active,
                "enabled" to component.enabled
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
// PROGRESS STEPPER
// ============================================

object ProgressStepperMapper {
    fun map(component: ProgressStepper, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        val stepViews = component.steps.mapIndexed { index, step ->
            val isCompleted = index < component.currentStep
            val isCurrent = index == component.currentStep

            val stepChildren = mutableListOf<SwiftUIView>()

            // Step indicator
            val indicatorColor = when {
                isCompleted -> SwiftUIColor.system("systemGreen")
                isCurrent -> SwiftUIColor.system("accentColor")
                else -> SwiftUIColor.system("tertiaryLabel")
            }

            stepChildren.add(SwiftUIView(
                type = ViewType.ZStack,
                properties = emptyMap(),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.Custom("Circle"),
                        properties = mapOf("size" to 32f),
                        modifiers = listOf(
                            SwiftUIModifier.foregroundColor(indicatorColor)
                        )
                    ),
                    SwiftUIView.text(
                        content = if (isCompleted) "✓" else "${index + 1}",
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Subheadline),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.white)
                        )
                    )
                )
            ))

            // Step label - use 'label' property
            stepChildren.add(SwiftUIView.text(
                content = step.label,
                modifiers = listOf(
                    SwiftUIModifier.font(if (isCurrent) FontStyle.Headline else FontStyle.Subheadline),
                    SwiftUIModifier.foregroundColor(if (isCurrent) SwiftUIColor.primary else SwiftUIColor.secondary)
                )
            ))

            // Step description if present
            step.description?.let { desc ->
                stepChildren.add(SwiftUIView.text(
                    content = desc,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
                    )
                ))
            }

            SwiftUIView(
                type = ViewType.VStack,
                properties = mapOf("spacing" to 8f),
                children = stepChildren
            )
        }

        // Layout based on orientation
        val containerType = when (component.orientation) {
            ProgressStepper.Orientation.Horizontal -> ViewType.HStack
            ProgressStepper.Orientation.Vertical -> ViewType.VStack
        }

        return SwiftUIView(
            type = containerType,
            properties = mapOf("spacing" to 16f),
            children = stepViews,
            modifiers = modifiers,
        )
    }
}

// ============================================
// MENU BAR
// ============================================

object MenuBarMapper {
    fun map(component: MenuBar, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.padding(8f, 16f, 8f, 16f))
        modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("systemBackground")))
        modifiers.add(SwiftUIModifier.shadow(
            color = SwiftUIColor.rgb(0f, 0f, 0f, 0.1f),
            radius = 2f,
            x = 0f,
            y = 1f
        ))
        modifiers.add(SwiftUIModifier.frame(
            width = null,
            height = SizeValue.Fixed(component.height)
        ))

        val children = component.items.map { item ->
            SwiftUIView(
                type = ViewType.Custom("Menu"),
                properties = mapOf(
                    "label" to item.label,
                    "enabled" to item.enabled
                ),
                children = listOf(
                    SwiftUIView.text(
                        content = item.label,
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Body),
                            SwiftUIModifier.padding(8f, 12f, 8f, 12f)
                        )
                    )
                )
            )
        }

        return SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 8f),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// SUB MENU
// ============================================

object SubMenuMapper {
    fun map(component: SubMenu, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Nested menu items
        val menuItems = component.items.map { item ->
            SwiftUIView(
                type = ViewType.Custom("Button"),
                properties = mapOf(
                    "text" to item.label,
                    "icon" to (item.icon ?: ""),
                    "enabled" to item.enabled
                ),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.HStack,
                        properties = mapOf("spacing" to 8f),
                        children = listOfNotNull(
                            item.icon?.let {
                                SwiftUIView(
                                    type = ViewType.Image,
                                    properties = mapOf("systemName" to it, "size" to 16f)
                                )
                            },
                            SwiftUIView.text(
                                content = item.label,
                                modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
                            ),
                            item.shortcut?.let { shortcut ->
                                SwiftUIView.text(
                                    content = shortcut,
                                    modifiers = listOf(
                                        SwiftUIModifier.font(FontStyle.Caption),
                                        SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
                                    )
                                )
                            }
                        )
                    )
                )
            )
        }

        // Label with optional icon
        val labelChildren = mutableListOf<SwiftUIView>()

        component.icon?.let { icon ->
            labelChildren.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to icon, "size" to 16f)
            ))
        }

        labelChildren.add(SwiftUIView.text(
            content = component.label,
            modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
        ))

        val labelView = SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 8f),
            children = labelChildren
        )

        return SwiftUIView(
            type = ViewType.Custom("Menu"),
            properties = mapOf("enabled" to component.enabled),
            children = menuItems + listOf(labelView),
            modifiers = modifiers,
        )
    }
}

// ============================================
// VERTICAL TABS
// ============================================

object VerticalTabsMapper {
    fun map(component: VerticalTabs, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Fixed(component.width),
            height = null
        ))

        val children = mutableListOf<SwiftUIView>()

        // Tab buttons - use tab.selected or compare with selectedTabId
        component.tabs.forEachIndexed { index, tab ->
            val isSelected = tab.selected || tab.id == component.selectedTabId
            val tabModifiers = mutableListOf<SwiftUIModifier>()

            tabModifiers.add(SwiftUIModifier.padding(12f, 16f, 12f, 16f))
            tabModifiers.add(SwiftUIModifier.background(
                if (isSelected) SwiftUIColor.system("secondarySystemFill")
                else SwiftUIColor.system("clear")
            ))

            if (isSelected) {
                tabModifiers.add(SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("cornerRadius" to 8f)
                ))
            }

            val tabChildren = mutableListOf<SwiftUIView>()

            tab.icon?.let { icon ->
                tabChildren.add(SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to icon, "size" to 20f),
                    modifiers = listOf(
                        SwiftUIModifier.foregroundColor(
                            if (isSelected) SwiftUIColor.system("accentColor")
                            else SwiftUIColor.secondary
                        )
                    )
                ))
            }

            if (component.showLabels) {
                tabChildren.add(SwiftUIView.text(
                    content = tab.label,
                    modifiers = listOf(
                        SwiftUIModifier.font(if (isSelected) FontStyle.Headline else FontStyle.Body),
                        SwiftUIModifier.foregroundColor(
                            if (isSelected) SwiftUIColor.primary else SwiftUIColor.secondary
                        )
                    )
                ))
            }

            // Badge if present
            tab.badge?.let { badge ->
                tabChildren.add(SwiftUIView.text(
                    content = badge,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption2),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                        SwiftUIModifier.padding(2f, 6f, 2f, 6f),
                        SwiftUIModifier.background(SwiftUIColor.system("systemRed")),
                        SwiftUIModifier(
                            type = ModifierType.Custom,
                            value = mapOf("cornerRadius" to 10f)
                        )
                    )
                ))
            }

            children.add(SwiftUIView(
                type = ViewType.Custom("Button"),
                properties = mapOf("action" to "selectTab_${tab.id}"),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.HStack,
                        properties = mapOf("spacing" to 12f),
                        children = tabChildren,
                        modifiers = tabModifiers
                    )
                ),
                modifiers = listOf(
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("buttonStyle" to "plain")
                    )
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 4f),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// MASONRY GRID
// ============================================

object MasonryGridMapper {
    fun map(component: MasonryGrid, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Get column count from sealed class
        val columnCount = when (val columns = component.columns) {
            is MasonryGrid.Columns.Fixed -> columns.count
            is MasonryGrid.Columns.Adaptive -> 2 // Default for adaptive
        }

        // Create flexible columns
        val columnsConfig = (1..columnCount).map {
            mapOf("type" to "flexible", "spacing" to component.horizontalSpacing)
        }

        val children = component.items.map { child ->
            renderChild(child)
        }

        return SwiftUIView(
            type = ViewType.Custom("LazyVGrid"),
            properties = mapOf(
                "columns" to columnsConfig,
                "spacing" to component.verticalSpacing
            ),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// ASPECT RATIO
// ============================================

object AspectRatioMapper {
    fun map(component: AspectRatio, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Get ratio value from sealed class
        val ratioValue = component.ratio.value

        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf(
                "aspectRatio" to ratioValue,
                "contentMode" to "fit"
            )
        ))

        // Render child if present
        val childView = component.child?.let { renderChild(it) }
            ?: SwiftUIView(type = ViewType.EmptyView, properties = emptyMap())

        return childView.copy(
            modifiers = childView.modifiers + modifiers,
            id = component.id ?: childView.id
        )
    }
}

// ============================================
// Helper Functions
// ============================================

private fun parseColor(colorString: String): SwiftUIColor {
    return when {
        colorString.startsWith("#") -> {
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
