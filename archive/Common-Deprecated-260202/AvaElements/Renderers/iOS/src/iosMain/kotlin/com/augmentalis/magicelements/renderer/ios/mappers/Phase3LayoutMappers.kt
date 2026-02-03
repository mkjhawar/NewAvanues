package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.components.phase3.layout.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Phase 3 Layout Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Phase 3 layout components to SwiftUI equivalents:
 * - Grid, Stack
 * - Spacer, Drawer
 * - Tabs
 */

// ============================================
// GRID
// ============================================

/**
 * Maps Grid component to SwiftUI LazyVGrid or LazyHGrid
 */
object GridMapper {
    fun map(component: Grid, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Custom("LazyVGrid"),
            properties = mapOf(
                "columns" to component.columns,
                "gap" to component.gap
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// STACK
// ============================================

/**
 * Maps Stack component to SwiftUI ZStack (layered views)
 */
object StackMapper {
    fun map(component: Stack, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.ZStack,
            properties = mapOf(
                "alignment" to component.alignment
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// SPACER
// ============================================

/**
 * Maps Spacer component to SwiftUI Spacer (Phase3 enhanced variant)
 */
object Phase3SpacerMapper {
    fun map(component: Spacer, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add fixed size if specified (default vertical spacer)
        if (component.size > 0) {
            modifiers.add(SwiftUIModifier.frame(width = null, height = SizeValue.Fixed(component.size.toFloat())))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Spacer,
            properties = emptyMap(),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// DRAWER
// ============================================

/**
 * Maps Drawer component to SwiftUI slide-out navigation
 */
object DrawerMapper {
    fun map(component: Drawer, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Determine drawer edge from position enum
        val edge = when (component.position) {
            DrawerPosition.Right -> "trailing"
            DrawerPosition.Top -> "top"
            DrawerPosition.Bottom -> "bottom"
            else -> "leading" // Left is default
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Render header and footer if present
        val headerView = component.header?.let { renderChild(it) }
        val footerView = component.footer?.let { renderChild(it) }

        // Build properties map excluding null values
        val props = mutableMapOf<String, Any>(
            "isOpen" to component.isOpen,
            "edge" to edge,
            "items" to component.items.map { mapOf("id" to it.id, "icon" to it.icon, "label" to it.label, "badge" to it.badge) }
        )
        headerView?.let { props["header"] = it }
        footerView?.let { props["footer"] = it }

        return SwiftUIView(
            type = ViewType.Custom("Drawer"),
            properties = props,
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// TABS
// ============================================

/**
 * Maps Tabs component to SwiftUI TabView
 */
object TabsMapper {
    fun map(component: Tabs, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.accentColor(ModifierConverter.convertColor(it)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Map tabs to view data with content
        val tabViews = component.tabs.mapNotNull { tab ->
            tab.content?.let { renderChild(it) }
        }

        return SwiftUIView(
            type = ViewType.Custom("TabView"),
            properties = mapOf(
                "selectedIndex" to component.selectedIndex,
                "tabs" to component.tabs.map { mapOf("label" to it.label, "icon" to it.icon) }
            ),
            children = tabViews,
            modifiers = modifiers,
            id = component.id
        )
    }
}
