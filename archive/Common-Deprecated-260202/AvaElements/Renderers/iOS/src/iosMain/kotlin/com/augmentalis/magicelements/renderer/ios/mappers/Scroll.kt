package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.flutter.layout.scrolling.*
import com.augmentalis.avaelements.flutter.material.advanced.IndexedStack
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Flutter Scrolling Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity scrolling components to SwiftUI equivalents.
 *
 * Components:
 * - ListViewBuilderComponent → LazyVStack with ForEach
 * - GridViewBuilderComponent → LazyVGrid with ForEach
 * - ListViewSeparatedComponent → LazyVStack with dividers
 * - PageViewComponent → TabView with PageTabViewStyle
 * - ReorderableListViewComponent → List with onMove
 * - CustomScrollViewComponent → ScrollView with custom content
 * - IndexedStack → ZStack with conditional visibility
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// LIST VIEW BUILDER
// ============================================

object ListViewBuilderMapper {
    fun map(component: ListViewBuilderComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val registry = com.augmentalis.magicelements.renderer.ios.registry.ItemBuilderRegistryHolder.getRegistry()
        val itemCount = component.itemCount ?: 100
        val isHorizontal = component.scrollDirection == ScrollDirection.Horizontal

        val children = if (registry.hasBuilder(component.itemBuilder)) {
            (0 until itemCount).mapNotNull { index ->
                registry.resolveBuilder(component.itemBuilder, index)?.let { renderChild(it) }
            }
        } else {
            // Fallback: create placeholder items when no builder registered
            (0 until minOf(itemCount, 10)).map { index ->
                SwiftUIView.text(
                    content = "Item $index (builder: ${component.itemBuilder})",
                    modifiers = listOf(
                        SwiftUIModifier(ModifierType.Custom, mapOf("padding" to 8f))
                    )
                )
            }
        }

        return SwiftUIView(
            type = ViewType.Custom("ScrollView"),
            properties = mapOf(
                "axes" to if (isHorizontal) "horizontal" else "vertical"
            ),
            children = listOf(
                SwiftUIView(
                    type = ViewType.Custom(if (isHorizontal) "LazyHStack" else "LazyVStack"),
                    properties = mapOf("spacing" to 8f),
                    children = children
                )
            )
        )
    }
}

// ============================================
// GRID VIEW BUILDER
// ============================================

object GridViewBuilderMapper {
    fun map(component: GridViewBuilderComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val registry = com.augmentalis.magicelements.renderer.ios.registry.ItemBuilderRegistryHolder.getRegistry()
        val itemCount = component.itemCount ?: 100

        val columns = when (val delegate = component.gridDelegate) {
            is SliverGridDelegate.WithFixedCrossAxisCount -> delegate.crossAxisCount
            is SliverGridDelegate.WithMaxCrossAxisExtent -> 2  // Simplified
            else -> 2
        }

        val spacing = when (val delegate = component.gridDelegate) {
            is SliverGridDelegate.WithFixedCrossAxisCount -> delegate.mainAxisSpacing
            is SliverGridDelegate.WithMaxCrossAxisExtent -> delegate.mainAxisSpacing
            else -> 8f
        }

        val children = if (registry.hasBuilder(component.itemBuilder)) {
            (0 until itemCount).mapNotNull { index ->
                registry.resolveBuilder(component.itemBuilder, index)?.let { renderChild(it) }
            }
        } else {
            // Fallback: create placeholder items when no builder registered
            (0 until minOf(itemCount, 10)).map { index ->
                SwiftUIView.text(
                    content = "Item $index (builder: ${component.itemBuilder})",
                    modifiers = listOf(
                        SwiftUIModifier(ModifierType.Custom, mapOf("padding" to 8f))
                    )
                )
            }
        }

        return SwiftUIView(
            type = ViewType.Custom("ScrollView"),
            properties = emptyMap(),
            children = listOf(
                SwiftUIView(
                    type = ViewType.Custom("LazyVGrid"),
                    properties = mapOf(
                        "columns" to columns,
                        "spacing" to spacing
                    ),
                    children = children
                )
            )
        )
    }
}

// ============================================
// LIST VIEW SEPARATED
// ============================================

object ListViewSeparatedMapper {
    fun map(component: ListViewSeparatedComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val registry = com.augmentalis.magicelements.renderer.ios.registry.ItemBuilderRegistryHolder.getRegistry()
        val itemCount = component.itemCount ?: 100

        val children = mutableListOf<SwiftUIView>()

        if (registry.hasBuilder(component.itemBuilder)) {
            for (index in 0 until itemCount) {
                // Add item
                registry.resolveBuilder(component.itemBuilder, index)?.let { item ->
                    children.add(renderChild(item))
                }

                // Add separator (except after last item)
                if (index < itemCount - 1) {
                    val separator = registry.resolveSeparator(component.separatorBuilder, index)?.let { sep ->
                        renderChild(sep)
                    } ?: SwiftUIView(
                        type = ViewType.Custom("Divider"),
                        properties = emptyMap(),
                        modifiers = listOf(
                            SwiftUIModifier(ModifierType.Custom, mapOf("padding" to mapOf("horizontal" to 16f)))
                        )
                    )
                    children.add(separator)
                }
            }
        } else {
            // Fallback: create placeholder items when no builder registered
            for (index in 0 until minOf(itemCount, 10)) {
                children.add(
                    SwiftUIView.text(
                        content = "Item $index (builder: ${component.itemBuilder})",
                        modifiers = listOf(
                            SwiftUIModifier(ModifierType.Custom, mapOf("padding" to 8f))
                        )
                    )
                )
                if (index < minOf(itemCount, 10) - 1) {
                    children.add(
                        SwiftUIView(
                            type = ViewType.Custom("Divider"),
                            properties = emptyMap()
                        )
                    )
                }
            }
        }

        return SwiftUIView(
            type = ViewType.Custom("ScrollView"),
            properties = emptyMap(),
            children = listOf(
                SwiftUIView(
                    type = ViewType.Custom("LazyVStack"),
                    properties = mapOf("spacing" to 0f),
                    children = children
                )
            )
        )
    }
}

// ============================================
// PAGE VIEW
// ============================================

object PageViewMapper {
    fun map(component: PageViewComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        // Map children if present
        val children = component.children?.map { renderChild(it) } ?: emptyList()

        return SwiftUIView(
            type = ViewType.Custom("TabView"),
            properties = mapOf("selection" to (component.controller?.initialPage ?: 0)),
            children = children,
            modifiers = listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf("tabViewStyle" to "page")),
                SwiftUIModifier(ModifierType.Custom, mapOf("indexViewStyle" to "page"))
            )
        )
    }
}

// ============================================
// REORDERABLE LIST VIEW
// ============================================

object ReorderableListViewMapper {
    fun map(component: ReorderableListViewComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val registry = com.augmentalis.magicelements.renderer.ios.registry.ItemBuilderRegistryHolder.getRegistry()
        val itemCount = component.itemCount ?: 100

        val children = if (registry.hasBuilder(component.itemBuilder)) {
            (0 until itemCount).mapNotNull { index ->
                registry.resolveBuilder(component.itemBuilder, index)?.let { renderChild(it) }
            }
        } else {
            // Fallback: create placeholder items when no builder registered
            (0 until minOf(itemCount, 10)).map { index ->
                SwiftUIView.text(
                    content = "Item $index (builder: ${component.itemBuilder})",
                    modifiers = listOf(
                        SwiftUIModifier(ModifierType.Custom, mapOf("padding" to 8f))
                    )
                )
            }
        }

        return SwiftUIView(
            type = ViewType.Custom("List"),
            properties = emptyMap(),
            children = children,
            modifiers = listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf("onMove" to "handleReorder")),
                SwiftUIModifier(ModifierType.Custom, mapOf("listStyle" to "plain"))
            )
        )
    }
}

// ============================================
// CUSTOM SCROLL VIEW
// ============================================

object CustomScrollViewMapper {
    fun map(component: CustomScrollViewComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        val sliverViews = component.slivers.map { renderChild(it) }

        return SwiftUIView(
            type = ViewType.Custom("ScrollView"),
            properties = emptyMap(),
            children = listOf(
                SwiftUIView(
                    type = ViewType.VStack,
                    properties = mapOf("spacing" to 0f),
                    children = sliverViews
                )
            )
        )
    }
}

// ============================================
// INDEXED STACK
// ============================================

object IndexedStackMapper {
    fun map(component: IndexedStack, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val children = component.children.mapIndexed { index, child ->
            val childView = renderChild(child)
            // Add opacity modifier to hide non-active children
            if (index == component.index) {
                childView
            } else {
                SwiftUIView(
                    type = childView.type,
                    properties = childView.properties,
                    children = childView.children,
                    modifiers = childView.modifiers + listOf(
                        SwiftUIModifier(ModifierType.Custom, mapOf("opacity" to 0f)),
                        SwiftUIModifier(ModifierType.Custom, mapOf("allowsHitTesting" to false))
                    ),
                    id = childView.id
                )
            }
        }

        return SwiftUIView(
            type = ViewType.ZStack,
            properties = emptyMap(),
            children = children
        )
    }
}
