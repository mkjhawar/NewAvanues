package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.components.phase3.navigation.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Phase 3 Navigation Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Phase 3 navigation components to SwiftUI equivalents:
 * - AppBar, BottomNav
 * - Breadcrumb, Pagination
 */

// ============================================
// APP BAR
// ============================================

/**
 * Maps AppBar component to SwiftUI NavigationBar
 */
object AppBarMapper {
    fun map(component: AppBar, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Apply theme colors
        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.background(ModifierConverter.convertColor(it)))
        }

        theme?.colorScheme?.onPrimary?.let {
            modifiers.add(SwiftUIModifier.foregroundColor(ModifierConverter.convertColor(it)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Map AppBarActions to SwiftUI button views
        val actionViews = component.actions.map { action ->
            SwiftUIView(
                type = ViewType.Button,
                properties = mapOf(
                    "systemImage" to action.icon,
                    "label" to (action.label ?: ""),
                    "action" to "appbar_action_${action.icon}"
                )
            )
        }

        return SwiftUIView(
            type = ViewType.Custom("NavigationBar"),
            properties = mapOf(
                "title" to component.title,
                "navigationIcon" to (component.navigationIcon ?: ""),
                "hasNavigationAction" to (component.onNavigationClick != null),
                "elevation" to component.elevation
            ),
            children = actionViews,
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// BOTTOM NAVIGATION
// ============================================

/**
 * Maps BottomNav component to SwiftUI TabBar
 */
object BottomNavMapper {
    fun map(component: BottomNav, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Apply theme colors
        theme?.colorScheme?.surface?.let {
            modifiers.add(SwiftUIModifier.background(ModifierConverter.convertColor(it)))
        }

        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.accentColor(ModifierConverter.convertColor(it)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Custom("TabBar"),
            properties = mapOf(
                "items" to component.items.map { mapOf("icon" to it.icon, "label" to it.label, "badge" to it.badge) },
                "selectedIndex" to component.selectedIndex,
                "onSelectionChange" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// BREADCRUMB
// ============================================

/**
 * Maps Breadcrumb component to SwiftUI horizontal navigation trail
 */
object BreadcrumbMapper {
    fun map(component: Breadcrumb, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        theme?.colorScheme?.onSurface?.let {
            modifiers.add(SwiftUIModifier.foregroundColor(ModifierConverter.convertColor(it)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf(
                "items" to component.items,
                "separator" to (component.separator ?: "/"),
                "onItemClick" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// PAGINATION
// ============================================

/**
 * Maps Pagination component to SwiftUI page control
 */
object PaginationMapper {
    fun map(component: Pagination, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.accentColor(ModifierConverter.convertColor(it)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf(
                "currentPage" to component.currentPage,
                "totalPages" to component.totalPages,
                "showFirstLast" to component.showFirstLast,
                "showPrevNext" to component.showPrevNext,
                "onPageChange" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}
