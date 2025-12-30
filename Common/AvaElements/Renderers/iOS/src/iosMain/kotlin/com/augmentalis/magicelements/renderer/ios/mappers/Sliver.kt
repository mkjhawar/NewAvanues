package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.flutter.layout.scrolling.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Flutter Sliver Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity sliver components to SwiftUI equivalents.
 *
 * Slivers are scrollable regions that can be combined in CustomScrollView.
 * In SwiftUI, these map to LazyVStack/LazyVGrid components within ScrollView.
 *
 * Components:
 * - SliverList → LazyVStack
 * - SliverGrid → LazyVGrid
 * - SliverFixedExtentList → LazyVStack with fixed height items
 * - SliverAppBar → Collapsing toolbar/navigation bar
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// SLIVER LIST
// ============================================

object SliverListMapper {
    fun map(component: SliverList, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        // Extract children from delegate
        val children = when (val delegate = component.delegate) {
            is SliverChildDelegate.Builder -> {
                // For builder delegate, we'd need to call the builder function
                // For now, return empty list - actual implementation would call builder
                emptyList<SwiftUIView>()
            }
            is SliverChildDelegate.FixedExtent -> {
                // For fixed children, map each child
                delegate.children.map { renderChild(it) }
            }
            else -> emptyList()
        }

        return SwiftUIView(
            type = ViewType.Custom("LazyVStack"),
            properties = mapOf("spacing" to 0f),
            children = children
        )
    }
}

// ============================================
// SLIVER GRID
// ============================================

object SliverGridMapper {
    fun map(component: SliverGrid, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        // Extract children from delegate
        val children = when (val delegate = component.delegate) {
            is SliverChildDelegate.Builder -> {
                emptyList<SwiftUIView>()
            }
            is SliverChildDelegate.FixedExtent -> {
                delegate.children.map { renderChild(it) }
            }
            else -> emptyList()
        }

        // Get grid configuration
        val columns = when (val gridDelegate = component.gridDelegate) {
            is SliverGridDelegate.WithFixedCrossAxisCount -> gridDelegate.crossAxisCount
            is SliverGridDelegate.WithMaxCrossAxisExtent -> {
                // Calculate columns based on max extent (simplified)
                2
            }
            else -> 2
        }

        val spacing = when (val gridDelegate = component.gridDelegate) {
            is SliverGridDelegate.WithFixedCrossAxisCount -> gridDelegate.mainAxisSpacing
            is SliverGridDelegate.WithMaxCrossAxisExtent -> gridDelegate.mainAxisSpacing
            else -> 8f
        }

        return SwiftUIView(
            type = ViewType.Custom("LazyVGrid"),
            properties = mapOf(
                "columns" to columns,
                "spacing" to spacing
            ),
            children = children
        )
    }
}

// ============================================
// SLIVER FIXED EXTENT LIST
// ============================================

object SliverFixedExtentListMapper {
    fun map(component: SliverFixedExtentList, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        // Extract children from delegate
        val children = when (val delegate = component.delegate) {
            is SliverChildDelegate.Builder -> {
                emptyList<SwiftUIView>()
            }
            is SliverChildDelegate.FixedExtent -> {
                delegate.children.map { child ->
                    val childView = renderChild(child)
                    // Add fixed height modifier
                    SwiftUIView(
                        type = childView.type,
                        properties = childView.properties,
                        children = childView.children,
                        modifiers = childView.modifiers + listOf(
                            SwiftUIModifier(ModifierType.Custom, mapOf(
                                "frame" to mapOf("height" to component.itemExtent)
                            ))
                        ),
                        id = childView.id
                    )
                }
            }
            else -> emptyList()
        }

        return SwiftUIView(
            type = ViewType.Custom("LazyVStack"),
            properties = mapOf("spacing" to 0f),
            children = children
        )
    }
}

// ============================================
// SLIVER APP BAR
// ============================================

object SliverAppBarMapper {
    fun map(component: SliverAppBar, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Leading icon/button
        component.leading?.let { leading ->
            children.add(renderChild(leading))
        }

        // Title
        component.title?.let { title ->
            children.add(renderChild(title))
        }

        // Actions
        component.actions?.let { actionsList ->
            if (actionsList.isNotEmpty()) {
                val actionsView = SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf("spacing" to 12f),
                    children = actionsList.map { renderChild(it) }
                )
                children.add(actionsView)
            }
        }

        // Main content
        val primaryColor = theme?.colorScheme?.primary?.let {
            ModifierConverter.convertColor(it)
        } ?: SwiftUIColor.system("systemBlue")
        val toolbar = SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 12f),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(16f),
                SwiftUIModifier.background(primaryColor)
            )
        )

        // If expandedHeight is set, add flexible space
        val expandedHeightValue = component.expandedHeight
        val allChildren = if (expandedHeightValue != null && expandedHeightValue > 0f) {
            // FlexibleSpace equivalent
            val flexibleSpace = SwiftUIView(
                type = ViewType.VStack,
                properties = emptyMap(),
                children = emptyList(),
                modifiers = listOf(
                    SwiftUIModifier(ModifierType.Custom, mapOf(
                        "frame" to mapOf("height" to expandedHeightValue)
                    ))
                )
            )

            // Bottom content (flexibleSpace)
            component.flexibleSpace?.let { flexSpace ->
                // FlexibleSpaceBar is a data class, not a component, so skip rendering it for now
                listOf(flexibleSpace, toolbar)
            } ?: listOf(flexibleSpace, toolbar)
        } else {
            listOf(toolbar)
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("spacing" to 0f),
            children = allChildren,
            modifiers = if (component.pinned) {
                listOf(
                    SwiftUIModifier(ModifierType.Custom, mapOf("sticky" to true))
                )
            } else {
                emptyList()
            }
        )
    }
}
