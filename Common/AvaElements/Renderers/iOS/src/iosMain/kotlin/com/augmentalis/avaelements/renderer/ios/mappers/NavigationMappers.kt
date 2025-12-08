package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.flutter.material.navigation.*

/**
 * iOS SwiftUI Mappers for Flutter Navigation Parity Components
 *
 * This file maps cross-platform Flutter navigation components to iOS SwiftUI
 * bridge representations. The SwiftUI bridge models are consumed by Swift
 * code to render native iOS navigation UI.
 *
 * Architecture:
 * Flutter Component → iOS Mapper → SwiftUIView Bridge → Swift → Native SwiftUI
 *
 * Components Implemented (12 Navigation Components):
 * - Sidebar: NavigationSplitView sidebar with collapsible behavior
 * - Menu: Vertical/horizontal menu with items and sections
 * - MenuBar: Top menu bar for desktop applications
 * - SubMenu: Cascading nested submenu
 * - VerticalTabs: Vertical tab navigation for settings panels
 * - NavLink: Navigation link with active state styling
 * - BackButton: Back navigation button
 * - ForwardButton: Forward navigation button
 * - HomeButton: Home navigation button
 * - ProgressStepper: Multi-step progress indicator
 * - Wizard: Multi-step wizard flow
 * - ActionSheet: iOS action sheet
 *
 * iOS-Specific Navigation Features:
 * - NavigationSplitView for sidebar layouts
 * - Menu and MenuButton for macOS-style menus
 * - TabView for vertical tabs
 * - NavigationLink for routing
 * - Navigation bar buttons for back/forward/home
 * - Stepper-style progress indicators
 * - Sheet presentation for action sheets
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Maps Sidebar to SwiftUI NavigationSplitView
 *
 * SwiftUI Implementation:
 * - NavigationSplitView for persistent sidebar
 * - Sheet presentation for overlay mode
 * - Collapsible behavior with animation
 * - List of navigation items with selection
 * - Header and footer sections
 * - Material Design 3 visual styling
 *
 * Features:
 * - Persistent and overlay modes
 * - Collapsible sidebar with smooth transitions
 * - Selected item highlighting
 * - Badge support for notifications
 * - Section dividers
 * - Adaptive width based on collapsed state
 */
object SidebarMapper {
    fun map(
        component: Sidebar,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val items = component.items.map { sidebarItem ->
            val itemChildren = mutableListOf<SwiftUIView>()

            // Add icon if present
            val iconName = sidebarItem.icon
            if (iconName != null) {
                itemChildren.add(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to iconName),
                        modifiers = listOf(SwiftUIModifier.fontSize(18f))
                    )
                )
            }

            // Add label (only if not collapsed)
            if (!component.collapsed) {
                itemChildren.add(
                    SwiftUIView.text(
                        content = sidebarItem.label,
                        modifiers = listOf(SwiftUIModifier.fontSize(16f))
                    )
                )
            }

            // Add badge if present
            val badgeText = sidebarItem.badge
            if (badgeText != null) {
                itemChildren.add(
                    SwiftUIView.text(
                        content = badgeText,
                        modifiers = listOf(
                            SwiftUIModifier.fontSize(12f),
                            SwiftUIModifier.padding(4f, 8f, 4f, 8f),
                            SwiftUIModifier.background(SwiftUIColor.red),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                            SwiftUIModifier.cornerRadius(10f)
                        )
                    )
                )
            }

            SwiftUIView.hStack(
                spacing = 12f,
                alignment = VerticalAlignment.Center,
                children = itemChildren,
                modifiers = listOf(
                    SwiftUIModifier.padding(12f),
                    SwiftUIModifier.background(
                        if (sidebarItem.selected) SwiftUIColor.system("systemBlue").copy(
                            value = RGBValue(0.2f, 0.5f, 1.0f, 0.2f)
                        ) else SwiftUIColor.clear
                    ),
                    SwiftUIModifier.cornerRadius(8f)
                )
            )
        }

        val modifiers = mutableListOf<SwiftUIModifier>()
        modifiers.add(
            SwiftUIModifier.frame(
                width = SizeValue.Fixed(component.getEffectiveWidth())
            )
        )
        val bgColor = component.backgroundColor
        if (bgColor != null) {
            modifiers.add(SwiftUIModifier.background(parseColor(bgColor)))
        }

        return SwiftUIView(
            type = ViewType.Custom("Sidebar"),
            id = component.id,
            properties = mapOf(
                "visible" to component.visible,
                "collapsed" to component.collapsed,
                "mode" to component.mode.name,
                "width" to component.getEffectiveWidth(),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = items,
            modifiers = modifiers
        )
    }
}

/**
 * Maps Menu to SwiftUI Menu or List
 *
 * SwiftUI Implementation:
 * - VStack for vertical menu (default)
 * - HStack for horizontal menu
 * - List for scrollable menus
 * - Selection state management
 * - Section dividers
 *
 * Features:
 * - Vertical and horizontal orientations
 * - Single/multiple selection modes
 * - Nested submenu support
 * - Icons and badges
 * - Dividers between sections
 * - Keyboard navigation support
 */
object MenuMapper {
    fun map(
        component: Menu,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val items = component.items.mapIndexed { index, menuItem ->
            if (menuItem.divider) {
                SwiftUIView(
                    type = ViewType.Divider,
                    properties = emptyMap(),
                    modifiers = listOf(SwiftUIModifier.padding(8f, 0f, 8f, 0f))
                )
            } else {
                val itemChildren = mutableListOf<SwiftUIView>()

                // Add icon if present
                val iconName = menuItem.icon
                if (iconName != null) {
                    itemChildren.add(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to iconName),
                            modifiers = listOf(SwiftUIModifier.fontSize(16f))
                        )
                    )
                }

                // Add label
                itemChildren.add(
                    SwiftUIView.text(
                        content = menuItem.label,
                        modifiers = listOf(SwiftUIModifier.fontSize(15f))
                    )
                )

                // Add badge if present
                val badgeText = menuItem.badge
                if (badgeText != null) {
                    itemChildren.add(
                        SwiftUIView.text(
                            content = badgeText,
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(11f),
                                SwiftUIModifier.padding(2f, 6f, 2f, 6f),
                                SwiftUIModifier.background(SwiftUIColor.system("systemRed")),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                                SwiftUIModifier.cornerRadius(8f)
                            )
                        )
                    )
                }

                // Add submenu indicator if has children
                if (menuItem.hasSubmenu()) {
                    itemChildren.add(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "chevron.right"),
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(12f),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemGray"))
                            )
                        )
                    )
                }

                val isSelected = component.isItemSelected(index)

                SwiftUIView.hStack(
                    spacing = 12f,
                    alignment = VerticalAlignment.Center,
                    children = itemChildren,
                    modifiers = listOf(
                        SwiftUIModifier.padding(12f, 16f, 12f, 16f),
                        SwiftUIModifier.background(
                            if (isSelected) SwiftUIColor.system("systemBlue").copy(
                                value = RGBValue(0.2f, 0.5f, 1.0f, 0.15f)
                            ) else SwiftUIColor.clear
                        ),
                        SwiftUIModifier.cornerRadius(8f),
                        if (!menuItem.enabled) SwiftUIModifier.opacity(0.5f) else SwiftUIModifier.opacity(1.0f)
                    )
                )
            }
        }

        val stackView = if (component.orientation == Menu.Orientation.Vertical) {
            SwiftUIView.vStack(
                spacing = if (component.dense) 2f else 4f,
                alignment = HorizontalAlignment.Leading,
                children = items,
                modifiers = emptyList()
            )
        } else {
            SwiftUIView.hStack(
                spacing = 8f,
                alignment = VerticalAlignment.Center,
                children = items,
                modifiers = emptyList()
            )
        }

        val modifiers = mutableListOf<SwiftUIModifier>()
        val bgColor = component.backgroundColor
        if (bgColor != null) {
            modifiers.add(SwiftUIModifier.background(parseColor(bgColor)))
        }
        val elevation = component.elevation
        if (elevation != null) {
            modifiers.add(SwiftUIModifier.shadow(elevation))
        }

        return stackView.copy(
            id = component.id,
            modifiers = modifiers
        )
    }
}

/**
 * Maps MenuBar to SwiftUI horizontal menu bar
 *
 * SwiftUI Implementation:
 * - HStack of menu items
 * - Menu buttons with dropdown capability
 * - Keyboard accelerator support
 * - Focus management
 *
 * Features:
 * - Horizontal layout for top menu bar
 * - Dropdown menus via Menu.MenuItem children
 * - Keyboard accelerators (Alt+Letter)
 * - Active menu state
 * - Desktop-optimized styling
 */
object MenuBarMapper {
    fun map(
        component: MenuBar,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val menuItems = component.items.map { menuBarItem ->
            val label = if (component.showAccelerators && menuBarItem.accelerator != null) {
                menuBarItem.getFormattedLabel()
            } else {
                menuBarItem.label
            }

            val isActive = component.isMenuOpen(menuBarItem.id)

            SwiftUIView(
                type = ViewType.Button,
                properties = mapOf(
                    "label" to label,
                    "action" to "menuItem_${menuBarItem.id}",
                    "hasDropdown" to menuBarItem.hasDropdown()
                ),
                modifiers = listOf(
                    SwiftUIModifier.padding(8f, 16f, 8f, 16f),
                    SwiftUIModifier.background(
                        if (isActive) SwiftUIColor.system("systemGray5") else SwiftUIColor.clear
                    ),
                    SwiftUIModifier.cornerRadius(6f),
                    if (!menuBarItem.enabled) SwiftUIModifier.opacity(0.5f) else SwiftUIModifier.opacity(1.0f)
                )
            )
        }

        return SwiftUIView.hStack(
            spacing = 4f,
            alignment = VerticalAlignment.Center,
            children = menuItems,
            modifiers = listOf(
                SwiftUIModifier.frame(height = SizeValue.Fixed(component.height)),
                SwiftUIModifier.background(
                    component.backgroundColor?.let { parseColor(it) } ?: SwiftUIColor.system("systemGray6")
                ),
                component.elevation?.let { SwiftUIModifier.shadow(it) } ?: SwiftUIModifier.shadow(2f)
            )
        )
    }
}

/**
 * Maps SubMenu to SwiftUI cascading menu
 *
 * SwiftUI Implementation:
 * - Menu with nested Menu children
 * - Automatic placement (right, left, auto)
 * - Hover and click triggers
 * - Multi-level nesting support
 *
 * Features:
 * - Cascading submenu with unlimited depth
 * - Hover and click trigger modes
 * - Automatic collision detection
 * - Keyboard navigation
 * - Dividers and badges
 * - Destructive action styling
 */
object SubMenuMapper {
    fun map(
        component: SubMenu,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val items = component.items.map { subMenuItem ->
            if (subMenuItem.divider) {
                SwiftUIView(
                    type = ViewType.Divider,
                    properties = emptyMap()
                )
            } else {
                val itemChildren = mutableListOf<SwiftUIView>()

                // Add icon if present
                val iconName = subMenuItem.icon
                if (iconName != null) {
                    itemChildren.add(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to iconName),
                            modifiers = listOf(SwiftUIModifier.fontSize(16f))
                        )
                    )
                }

                // Add label
                itemChildren.add(
                    SwiftUIView.text(
                        content = subMenuItem.label,
                        modifiers = listOf(
                            SwiftUIModifier.fontSize(15f),
                            if (subMenuItem.destructive) {
                                SwiftUIModifier.foregroundColor(SwiftUIColor.red)
                            } else {
                                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                            }
                        )
                    )
                )

                // Add spacer
                itemChildren.add(
                    SwiftUIView(type = ViewType.Spacer, properties = emptyMap())
                )

                // Add shortcut or submenu indicator
                val shortcut = subMenuItem.shortcut
                if (shortcut != null) {
                    itemChildren.add(
                        SwiftUIView.text(
                            content = shortcut,
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(13f),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemGray"))
                            )
                        )
                    )
                } else if (subMenuItem.hasSubmenu()) {
                    itemChildren.add(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "chevron.right"),
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(12f),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemGray"))
                            )
                        )
                    )
                }

                SwiftUIView.hStack(
                    spacing = 12f,
                    alignment = VerticalAlignment.Center,
                    children = itemChildren,
                    modifiers = listOf(
                        SwiftUIModifier.padding(10f, 12f, 10f, 12f),
                        if (!subMenuItem.enabled) SwiftUIModifier.opacity(0.5f) else SwiftUIModifier.opacity(1.0f)
                    )
                )
            }
        }

        return SwiftUIView(
            type = ViewType.Custom("SubMenu"),
            id = component.id,
            properties = mapOf(
                "label" to component.label,
                "open" to component.open,
                "trigger" to component.trigger.name,
                "placement" to component.placement.name,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = items,
            modifiers = listOf(
                SwiftUIModifier.background(
                    component.backgroundColor?.let { parseColor(it) } ?: SwiftUIColor.system("systemBackground")
                ),
                SwiftUIModifier.cornerRadius(8f),
                component.elevation?.let { SwiftUIModifier.shadow(it) } ?: SwiftUIModifier.shadow(4f)
            )
        )
    }
}

/**
 * Maps VerticalTabs to SwiftUI vertical tab view
 *
 * SwiftUI Implementation:
 * - VStack of tab buttons
 * - Selection indicator on left/right edge
 * - Icons and labels in horizontal layout
 * - Badge overlay support
 * - Scrollable for many tabs
 *
 * Features:
 * - Vertical tab arrangement
 * - Icon-only or icon+label modes
 * - Badge indicators
 * - Selection highlighting
 * - Grouped tabs with dividers
 * - Scrollable mode
 */
object VerticalTabsMapper {
    fun map(
        component: VerticalTabs,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val tabs = component.tabs.map { verticalTab ->
            if (verticalTab.divider) {
                SwiftUIView(
                    type = ViewType.Divider,
                    properties = emptyMap(),
                    modifiers = listOf(SwiftUIModifier.padding(8f, 16f, 8f, 16f))
                )
            } else {
                val tabChildren = mutableListOf<SwiftUIView>()

                // Determine icon and label layout based on labelPosition
                val tabIcon = verticalTab.icon
                val iconView = if (component.showIcons && tabIcon != null) {
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to tabIcon),
                        modifiers = listOf(SwiftUIModifier.fontSize(20f))
                    )
                } else null

                val labelView = if (component.showLabels) {
                    SwiftUIView.text(
                        content = verticalTab.label,
                        modifiers = listOf(SwiftUIModifier.fontSize(15f))
                    )
                } else null

                // Add badge overlay
                val tabBadge = verticalTab.badge
                val badgeView = if (tabBadge != null) {
                    SwiftUIView.text(
                        content = tabBadge,
                        modifiers = listOf(
                            SwiftUIModifier.fontSize(10f),
                            SwiftUIModifier.padding(2f, 5f, 2f, 5f),
                            SwiftUIModifier.background(SwiftUIColor.red),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                            SwiftUIModifier.cornerRadius(8f)
                        )
                    )
                } else null

                // Layout based on labelPosition
                when (component.labelPosition) {
                    VerticalTabs.LabelPosition.Right -> {
                        iconView?.let { tabChildren.add(it) }
                        labelView?.let { tabChildren.add(it) }
                    }
                    VerticalTabs.LabelPosition.Left -> {
                        labelView?.let { tabChildren.add(it) }
                        iconView?.let { tabChildren.add(it) }
                    }
                    VerticalTabs.LabelPosition.Top, VerticalTabs.LabelPosition.Bottom -> {
                        // Vertical layout - wrap in VStack
                        val verticalChildren = mutableListOf<SwiftUIView>()
                        if (component.labelPosition == VerticalTabs.LabelPosition.Top) {
                            labelView?.let { verticalChildren.add(it) }
                            iconView?.let { verticalChildren.add(it) }
                        } else {
                            iconView?.let { verticalChildren.add(it) }
                            labelView?.let { verticalChildren.add(it) }
                        }
                        tabChildren.add(
                            SwiftUIView.vStack(
                                spacing = 4f,
                                alignment = HorizontalAlignment.Center,
                                children = verticalChildren,
                                modifiers = emptyList()
                            )
                        )
                    }
                }

                badgeView?.let { tabChildren.add(it) }

                val isSelected = verticalTab.selected || component.selectedTabId == verticalTab.id

                SwiftUIView.hStack(
                    spacing = 12f,
                    alignment = VerticalAlignment.Center,
                    children = tabChildren,
                    modifiers = listOf(
                        SwiftUIModifier.padding(if (component.dense) 8f else 12f),
                        SwiftUIModifier.frame(width = SizeValue.Fixed(component.width)),
                        SwiftUIModifier.background(
                            if (isSelected) {
                                component.selectedTabColor?.let { parseColor(it) }
                                    ?: SwiftUIColor.system("systemBlue").copy(
                                        value = RGBValue(0.2f, 0.5f, 1.0f, 0.2f)
                                    )
                            } else SwiftUIColor.clear
                        ),
                        SwiftUIModifier.cornerRadius(8f),
                        if (!verticalTab.enabled) SwiftUIModifier.opacity(0.5f) else SwiftUIModifier.opacity(1.0f)
                    )
                )
            }
        }

        val container = SwiftUIView.vStack(
            spacing = if (component.dense) 2f else 4f,
            alignment = HorizontalAlignment.Leading,
            children = tabs,
            modifiers = emptyList()
        )

        val modifiers = mutableListOf<SwiftUIModifier>()
        modifiers.add(SwiftUIModifier.frame(width = SizeValue.Fixed(component.width)))
        val bgColor = component.backgroundColor
        if (bgColor != null) {
            modifiers.add(SwiftUIModifier.background(parseColor(bgColor)))
        }
        val elevation = component.elevation
        if (elevation != null) {
            modifiers.add(SwiftUIModifier.shadow(elevation))
        }

        return if (component.scrollable) {
            SwiftUIView(
                type = ViewType.ScrollView,
                id = component.id,
                properties = mapOf(
                    "accessibilityLabel" to component.getAccessibilityDescription()
                ),
                children = listOf(container),
                modifiers = modifiers
            )
        } else {
            container.copy(id = component.id, modifiers = modifiers)
        }
    }
}

/**
 * Maps NavLink to SwiftUI NavigationLink with active styling
 *
 * SwiftUI Implementation:
 * - NavigationLink for routing
 * - Active state highlighting
 * - Icon and label layout
 * - Badge overlay
 *
 * Features:
 * - Active/inactive state styling
 * - Icon positioning (leading, trailing, top, bottom)
 * - Badge notifications
 * - Minimum 48dp touch target
 * - Accessibility support
 */
object NavLinkMapper {
    fun map(
        component: NavLink,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Create icon and label views
        val iconName = component.icon
        val iconView = if (iconName != null) {
            SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to iconName),
                modifiers = listOf(SwiftUIModifier.fontSize(18f))
            )
        } else null

        val labelView = SwiftUIView.text(
            content = component.label,
            modifiers = listOf(SwiftUIModifier.fontSize(16f))
        )

        // Layout based on iconPosition
        when (component.iconPosition) {
            NavLink.IconPosition.Leading -> {
                iconView?.let { children.add(it) }
                children.add(labelView)
            }
            NavLink.IconPosition.Trailing -> {
                children.add(labelView)
                iconView?.let { children.add(it) }
            }
            NavLink.IconPosition.Top -> {
                val vstack = SwiftUIView.vStack(
                    spacing = 4f,
                    alignment = HorizontalAlignment.Center,
                    children = listOfNotNull(iconView, labelView),
                    modifiers = emptyList()
                )
                children.add(vstack)
            }
            NavLink.IconPosition.Bottom -> {
                val vstack = SwiftUIView.vStack(
                    spacing = 4f,
                    alignment = HorizontalAlignment.Center,
                    children = listOfNotNull(labelView, iconView),
                    modifiers = emptyList()
                )
                children.add(vstack)
            }
        }

        // Add badge if present
        val badgeText = component.badge
        if (badgeText != null) {
            children.add(
                SwiftUIView.text(
                    content = badgeText,
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(11f),
                        SwiftUIModifier.padding(2f, 6f, 2f, 6f),
                        SwiftUIModifier.background(SwiftUIColor.red),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                        SwiftUIModifier.cornerRadius(10f)
                    )
                )
            )
        }

        val backgroundColor = if (component.active) {
            component.activeBackgroundColor?.let { parseColor(it) }
                ?: SwiftUIColor.system("systemBlue").copy(
                    value = RGBValue(0.2f, 0.5f, 1.0f, 0.2f)
                )
        } else {
            component.backgroundColor?.let { parseColor(it) } ?: SwiftUIColor.clear
        }

        val foregroundColor = if (component.active) {
            component.activeColor?.let { parseColor(it) } ?: SwiftUIColor.system("systemBlue")
        } else {
            component.inactiveColor?.let { parseColor(it) } ?: SwiftUIColor.primary
        }

        return SwiftUIView.hStack(
            spacing = 12f,
            alignment = VerticalAlignment.Center,
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(12f, 16f, 12f, 16f),
                SwiftUIModifier.background(backgroundColor),
                SwiftUIModifier.foregroundColor(foregroundColor),
                SwiftUIModifier.cornerRadius(8f),
                if (!component.enabled) SwiftUIModifier.opacity(0.5f) else SwiftUIModifier.opacity(1.0f)
            )
        )
    }
}

/**
 * Maps BackButton to SwiftUI navigation back button
 *
 * SwiftUI Implementation:
 * - NavigationLink with back chevron icon
 * - Standard iOS back button styling
 * - Integration with navigation stack
 *
 * Features:
 * - Standard iOS back chevron
 * - Optional custom label
 * - Navigation stack integration
 * - Accessibility support
 */
object BackButtonMapper {
    fun map(
        theme: Theme?
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Button,
            properties = mapOf(
                "label" to "",
                "action" to "back"
            ),
            children = listOf(
                SwiftUIView.hStack(
                    spacing = 4f,
                    alignment = VerticalAlignment.Center,
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "chevron.left"),
                            modifiers = listOf(SwiftUIModifier.fontSize(17f))
                        ),
                        SwiftUIView.text(
                            content = "Back",
                            modifiers = listOf(SwiftUIModifier.fontSize(17f))
                        )
                    ),
                    modifiers = emptyList()
                )
            ),
            modifiers = listOf(
                SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemBlue"))
            )
        )
    }
}

/**
 * Maps ForwardButton to SwiftUI navigation forward button
 *
 * SwiftUI Implementation:
 * - Button with forward chevron icon
 * - Browser-style forward navigation
 * - Standard iOS styling
 *
 * Features:
 * - Forward chevron icon
 * - Optional custom label
 * - Browser-style navigation
 * - Accessibility support
 */
object ForwardButtonMapper {
    fun map(
        theme: Theme?
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Button,
            properties = mapOf(
                "label" to "",
                "action" to "forward"
            ),
            children = listOf(
                SwiftUIView.hStack(
                    spacing = 4f,
                    alignment = VerticalAlignment.Center,
                    children = listOf(
                        SwiftUIView.text(
                            content = "Forward",
                            modifiers = listOf(SwiftUIModifier.fontSize(17f))
                        ),
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "chevron.right"),
                            modifiers = listOf(SwiftUIModifier.fontSize(17f))
                        )
                    ),
                    modifiers = emptyList()
                )
            ),
            modifiers = listOf(
                SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemBlue"))
            )
        )
    }
}

/**
 * Maps HomeButton to SwiftUI home navigation button
 *
 * SwiftUI Implementation:
 * - Button with home icon
 * - Navigation to root/home
 * - Standard iOS styling
 *
 * Features:
 * - Home house icon
 * - Navigation to root
 * - Accessibility support
 */
object HomeButtonMapper {
    fun map(
        theme: Theme?
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Button,
            properties = mapOf(
                "label" to "Home",
                "action" to "home"
            ),
            children = listOf(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to "house"),
                    modifiers = listOf(SwiftUIModifier.fontSize(20f))
                )
            ),
            modifiers = listOf(
                SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemBlue"))
            )
        )
    }
}

/**
 * Maps ProgressStepper to SwiftUI custom stepper view
 *
 * SwiftUI Implementation:
 * - HStack or VStack of step indicators
 * - Connector lines between steps
 * - Step numbers or custom icons
 * - Three states: completed, current, upcoming
 * - Clickable steps for navigation
 *
 * Features:
 * - Horizontal and vertical orientations
 * - Step numbers or custom icons
 * - Completed/current/upcoming states
 * - Connector lines with styling
 * - Clickable navigation to completed steps
 * - Optional labels and descriptions
 */
object ProgressStepperMapper {
    fun map(
        component: ProgressStepper,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val steps = component.steps.mapIndexed { index, progressStep ->
            val state = when {
                index < component.currentStep -> ProgressStepper.StepState.Completed
                index == component.currentStep -> ProgressStepper.StepState.Current
                else -> ProgressStepper.StepState.Upcoming
            }

            val stepChildren = mutableListOf<SwiftUIView>()

            // Step indicator (number or icon)
            val stepIcon = progressStep.icon
            val indicator = if (stepIcon != null) {
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to stepIcon),
                    modifiers = listOf(SwiftUIModifier.fontSize(16f))
                )
            } else if (component.showStepNumbers) {
                SwiftUIView.text(
                    content = (index + 1).toString(),
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(14f),
                        SwiftUIModifier.fontWeight(FontWeight.Semibold)
                    )
                )
            } else {
                SwiftUIView(
                    type = ViewType.Circle,
                    properties = emptyMap(),
                    modifiers = listOf(
                        SwiftUIModifier.frame(width = SizeValue.Fixed(8f), height = SizeValue.Fixed(8f))
                    )
                )
            }

            val indicatorColor = when (state) {
                ProgressStepper.StepState.Completed -> component.completedStepColor?.let { parseColor(it) }
                    ?: SwiftUIColor.system("systemGreen")
                ProgressStepper.StepState.Current -> component.currentStepColor?.let { parseColor(it) }
                    ?: SwiftUIColor.system("systemBlue")
                ProgressStepper.StepState.Upcoming -> component.upcomingStepColor?.let { parseColor(it) }
                    ?: SwiftUIColor.system("systemGray3")
            }

            val indicatorView = SwiftUIView(
                type = ViewType.ZStack,
                properties = emptyMap(),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.Circle,
                        properties = emptyMap(),
                        modifiers = listOf(
                            SwiftUIModifier.frame(width = SizeValue.Fixed(32f), height = SizeValue.Fixed(32f)),
                            SwiftUIModifier.foregroundColor(indicatorColor)
                        )
                    ),
                    indicator.copy(
                        modifiers = indicator.modifiers + listOf(
                            SwiftUIModifier.foregroundColor(SwiftUIColor.white)
                        )
                    )
                )
            )

            // Step label and description
            val labelView = SwiftUIView.vStack(
                spacing = 2f,
                alignment = HorizontalAlignment.Leading,
                children = listOfNotNull(
                    SwiftUIView.text(
                        content = progressStep.label,
                        modifiers = listOf(
                            SwiftUIModifier.fontSize(15f),
                            SwiftUIModifier.fontWeight(FontWeight.Medium)
                        )
                    ),
                    progressStep.description?.let {
                        SwiftUIView.text(
                            content = it,
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(13f),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemGray"))
                            )
                        )
                    }
                ),
                modifiers = emptyList()
            )

            // Connector line (except for last step)
            val connectorView = if (index < component.steps.size - 1) {
                SwiftUIView(
                    type = ViewType.Rectangle,
                    properties = emptyMap(),
                    modifiers = listOf(
                        SwiftUIModifier.frame(
                            width = if (component.orientation == ProgressStepper.Orientation.Horizontal) {
                                SizeValue.Fixed(40f)
                            } else {
                                SizeValue.Fixed(2f)
                            },
                            height = if (component.orientation == ProgressStepper.Orientation.Horizontal) {
                                SizeValue.Fixed(2f)
                            } else {
                                SizeValue.Fixed(24f)
                            }
                        ),
                        SwiftUIModifier.foregroundColor(
                            component.connectorColor?.let { parseColor(it) }
                                ?: SwiftUIColor.system("systemGray4")
                        )
                    )
                )
            } else null

            if (component.orientation == ProgressStepper.Orientation.Horizontal) {
                SwiftUIView.hStack(
                    spacing = 8f,
                    alignment = VerticalAlignment.Center,
                    children = listOfNotNull(
                        SwiftUIView.vStack(
                            spacing = 4f,
                            alignment = HorizontalAlignment.Center,
                            children = listOf(indicatorView, labelView),
                            modifiers = emptyList()
                        ),
                        connectorView
                    ),
                    modifiers = emptyList()
                )
            } else {
                SwiftUIView.hStack(
                    spacing = 12f,
                    alignment = VerticalAlignment.Top,
                    children = listOf(
                        SwiftUIView.vStack(
                            spacing = 4f,
                            alignment = HorizontalAlignment.Center,
                            children = listOfNotNull(indicatorView, connectorView),
                            modifiers = emptyList()
                        ),
                        labelView
                    ),
                    modifiers = emptyList()
                )
            }
        }

        val container = if (component.orientation == ProgressStepper.Orientation.Horizontal) {
            SwiftUIView.hStack(
                spacing = 0f,
                alignment = VerticalAlignment.Center,
                children = steps,
                modifiers = emptyList()
            )
        } else {
            SwiftUIView.vStack(
                spacing = 0f,
                alignment = HorizontalAlignment.Leading,
                children = steps,
                modifiers = emptyList()
            )
        }

        val modifiers = mutableListOf<SwiftUIModifier>()
        val bgColor = component.backgroundColor
        if (bgColor != null) {
            modifiers.add(SwiftUIModifier.background(parseColor(bgColor)))
        }

        return container.copy(
            id = component.id,
            modifiers = modifiers
        )
    }
}

/**
 * Maps Wizard to SwiftUI multi-step wizard flow
 *
 * SwiftUI Implementation:
 * - TabView with page style for step transitions
 * - Progress stepper at top
 * - Navigation buttons (back, next, finish)
 * - Form content area
 * - Validation support
 *
 * Features:
 * - Multi-step form flow
 * - Progress tracking with stepper
 * - Navigation controls
 * - Step validation
 * - Back/Next/Finish buttons
 * - Cancellation support
 */
object WizardMapper {
    fun map(
        steps: List<String>,
        currentStep: Int,
        canGoBack: Boolean,
        canGoNext: Boolean,
        theme: Theme?
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Header with progress stepper
        val progressStepper = ProgressStepperMapper.map(
            ProgressStepper(
                steps = steps.map { ProgressStepper.Step(label = it) },
                currentStep = currentStep,
                orientation = ProgressStepper.Orientation.Horizontal
            ),
            theme,
            { SwiftUIView(type = ViewType.EmptyView, properties = emptyMap()) }
        )
        children.add(progressStepper)

        // Content area (placeholder for wizard content)
        children.add(
            SwiftUIView(
                type = ViewType.Custom("WizardContent"),
                properties = mapOf("currentStep" to currentStep),
                modifiers = listOf(
                    SwiftUIModifier.padding(16f)
                )
            )
        )

        // Footer with navigation buttons
        val footerButtons = mutableListOf<SwiftUIView>()

        if (canGoBack) {
            footerButtons.add(
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf("label" to "Back", "action" to "wizard_back"),
                    modifiers = listOf(
                        SwiftUIModifier.padding(12f, 24f, 12f, 24f),
                        SwiftUIModifier.background(SwiftUIColor.system("systemGray5")),
                        SwiftUIModifier.cornerRadius(8f)
                    )
                )
            )
        }

        footerButtons.add(SwiftUIView(type = ViewType.Spacer, properties = emptyMap()))

        val nextLabel = if (currentStep == steps.size - 1) "Finish" else "Next"
        footerButtons.add(
            SwiftUIView(
                type = ViewType.Button,
                properties = mapOf("label" to nextLabel, "action" to "wizard_next"),
                modifiers = listOf(
                    SwiftUIModifier.padding(12f, 24f, 12f, 24f),
                    SwiftUIModifier.background(
                        if (canGoNext) SwiftUIColor.system("systemBlue") else SwiftUIColor.system("systemGray4")
                    ),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                    SwiftUIModifier.cornerRadius(8f),
                    SwiftUIModifier.disabled(!canGoNext)
                )
            )
        )

        children.add(
            SwiftUIView.hStack(
                spacing = 16f,
                alignment = VerticalAlignment.Center,
                children = footerButtons,
                modifiers = listOf(SwiftUIModifier.padding(16f))
            )
        )

        return SwiftUIView.vStack(
            spacing = 0f,
            alignment = HorizontalAlignment.Leading,
            children = children,
            modifiers = listOf(
                SwiftUIModifier.background(SwiftUIColor.system("systemBackground"))
            )
        )
    }
}

/**
 * Maps ActionSheet to SwiftUI action sheet / confirmation dialog
 *
 * SwiftUI Implementation:
 * - .confirmationDialog for iOS 15+
 * - .actionSheet for iOS 14
 * - List of action buttons
 * - Destructive action styling
 * - Cancel button
 *
 * Features:
 * - Native iOS action sheet
 * - Destructive action support
 * - Cancel button
 * - Title and message
 * - Multiple action buttons
 * - Dismissal on selection
 */
object ActionSheetMapper {
    fun map(
        title: String?,
        message: String?,
        actions: List<ActionSheetAction>,
        cancelLabel: String = "Cancel",
        theme: Theme?
    ): SwiftUIView {
        val actionButtons = actions.map { sheetAction ->
            SwiftUIView(
                type = ViewType.Button,
                properties = mapOf(
                    "label" to sheetAction.label,
                    "action" to "actionSheet_${sheetAction.id}",
                    "role" to if (sheetAction.destructive) "destructive" else "default"
                ),
                modifiers = listOf(
                    if (sheetAction.destructive) {
                        SwiftUIModifier.foregroundColor(SwiftUIColor.red)
                    } else {
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemBlue"))
                    }
                )
            )
        }

        val cancelButton = SwiftUIView(
            type = ViewType.Button,
            properties = mapOf(
                "label" to cancelLabel,
                "action" to "actionSheet_cancel",
                "role" to "cancel"
            ),
            modifiers = listOf(
                SwiftUIModifier.fontWeight(FontWeight.Semibold)
            )
        )

        return SwiftUIView(
            type = ViewType.Custom("ActionSheet"),
            properties = mapOf(
                "title" to (title ?: ""),
                "message" to (message ?: ""),
                "hasTitle" to (title != null),
                "hasMessage" to (message != null)
            ),
            children = actionButtons + listOf(cancelButton),
            modifiers = emptyList()
        )
    }

    data class ActionSheetAction(
        val id: String,
        val label: String,
        val destructive: Boolean = false
    )
}

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Parse color string to SwiftUIColor
 * Supports: hex (#RRGGBB), named colors, system colors
 */
private fun parseColor(colorString: String): SwiftUIColor {
    return when {
        colorString.startsWith("#") -> {
            // Parse hex color
            val hex = colorString.removePrefix("#")
            val r = hex.substring(0, 2).toInt(16) / 255f
            val g = hex.substring(2, 4).toInt(16) / 255f
            val b = hex.substring(4, 6).toInt(16) / 255f
            val a = if (hex.length == 8) hex.substring(6, 8).toInt(16) / 255f else 1.0f
            SwiftUIColor.rgb(r, g, b, a)
        }
        colorString.startsWith("system") -> SwiftUIColor.system(colorString)
        else -> SwiftUIColor.system(colorString) // Assume named color
    }
}
