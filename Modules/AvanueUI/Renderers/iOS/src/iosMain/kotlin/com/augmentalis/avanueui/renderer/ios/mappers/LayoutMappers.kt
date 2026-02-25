package com.augmentalis.avanueui.renderer.ios.mappers

import com.augmentalis.avanueui.core.*
import com.augmentalis.avanueui.dsl.*
import com.augmentalis.avanueui.renderer.ios.bridge.*

/**
 * Layout Component Mappers
 *
 * Maps AvaElements layout components to SwiftUI layout containers
 */

/**
 * Maps ColumnComponent to SwiftUI VStack
 */
object ColumnMapper {
    fun map(component: ColumnComponent, theme: Theme?, childMapper: (Component) -> SwiftUIView): SwiftUIView {
        // Convert children
        val children = component.children.map { childMapper(it) }

        // Convert alignment
        val alignment = ModifierConverter.toHorizontalAlignment(component.horizontalAlignment)

        // Calculate spacing
        val spacing = ModifierConverter.convertArrangementToSpacing(component.arrangement)

        // Convert modifiers
        val modifiers = ModifierConverter.convert(component.modifiers, theme)

        return SwiftUIView.vStack(
            spacing = spacing,
            alignment = alignment,
            children = children,
            modifiers = modifiers
        ).copy(id = component.id)
    }
}

/**
 * Maps RowComponent to SwiftUI HStack
 */
object RowMapper {
    fun map(component: RowComponent, theme: Theme?, childMapper: (Component) -> SwiftUIView): SwiftUIView {
        // Convert children
        val children = component.children.map { childMapper(it) }

        // Convert alignment
        val alignment = ModifierConverter.toVerticalAlignment(component.verticalAlignment)

        // Calculate spacing
        val spacing = ModifierConverter.convertArrangementToSpacing(component.arrangement)

        // Convert modifiers
        val modifiers = ModifierConverter.convert(component.modifiers, theme)

        return SwiftUIView.hStack(
            spacing = spacing,
            alignment = alignment,
            children = children,
            modifiers = modifiers
        ).copy(id = component.id)
    }
}

/**
 * Maps ContainerComponent to SwiftUI ZStack
 */
object ContainerMapper {
    fun map(component: ContainerComponent, theme: Theme?, childMapper: (Component) -> SwiftUIView): SwiftUIView {
        // Convert child
        val children = component.child?.let { listOf(childMapper(it)) } ?: emptyList()

        // Convert alignment
        val alignment = ModifierConverter.convertAlignment(component.alignment)

        // Convert modifiers
        val modifiers = ModifierConverter.convert(component.modifiers, theme)

        return SwiftUIView.zStack(
            alignment = alignment,
            children = children,
            modifiers = modifiers
        ).copy(id = component.id)
    }
}

/**
 * Maps ScrollViewComponent to SwiftUI ScrollView
 */
object ScrollViewMapper {
    fun map(component: ScrollViewComponent, theme: Theme?, childMapper: (Component) -> SwiftUIView): SwiftUIView {
        // Convert child
        val child = component.child?.let { childMapper(it) }

        // Determine scroll axis
        val axis = when (component.orientation) {
            Orientation.Vertical -> "vertical"
            Orientation.Horizontal -> "horizontal"
        }

        // Convert modifiers
        val modifiers = ModifierConverter.convert(component.modifiers, theme)

        return SwiftUIView(
            type = ViewType.ScrollView,
            id = component.id,
            properties = mapOf(
                "axis" to axis,
                "showsIndicators" to true
            ),
            children = child?.let { listOf(it) } ?: emptyList(),
            modifiers = modifiers
        )
    }
}

/**
 * Maps CardComponent to SwiftUI RoundedRectangle + VStack
 */
object CardMapper {
    fun map(component: CardComponent, theme: Theme?, childMapper: (Component) -> SwiftUIView): SwiftUIView {
        // Convert children
        val children = component.children.map { childMapper(it) }

        // Get theme elevation shadow
        val shadowValue = theme?.elevation?.let { elevation ->
            when (component.elevation) {
                0 -> elevation.level0
                1 -> elevation.level1
                2 -> elevation.level2
                3 -> elevation.level3
                4 -> elevation.level4
                else -> elevation.level5
            }
        }

        // Get theme shape
        val cornerRadius = theme?.shapes?.medium?.topLeft ?: 12f

        // Build modifiers
        val cardModifiers = mutableListOf<SwiftUIModifier>()

        // Add background color
        val backgroundColor = theme?.colorScheme?.surface?.let {
            ModifierConverter.convertColor(it)
        } ?: SwiftUIColor.white
        cardModifiers.add(SwiftUIModifier.background(backgroundColor))

        // Add corner radius
        cardModifiers.add(SwiftUIModifier.cornerRadius(cornerRadius))

        // Add shadow based on elevation
        if (shadowValue != null) {
            cardModifiers.add(SwiftUIModifier.shadow(
                radius = shadowValue.blurRadius,
                x = shadowValue.offsetX,
                y = shadowValue.offsetY
            ))
        }

        // Add component modifiers
        cardModifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Create VStack with children and wrap in styled container
        return SwiftUIView.vStack(
            spacing = 8f,
            alignment = HorizontalAlignment.Leading,
            children = children,
            modifiers = cardModifiers
        ).copy(id = component.id)
    }
}

/**
 * Helper to create a Spacer
 */
object SpacerMapper {
    fun create(minLength: Float? = null): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Spacer,
            properties = minLength?.let { mapOf("minLength" to it) } ?: emptyMap()
        )
    }
}

/**
 * Helper to create a Divider
 */
object DividerMapper {
    fun create(): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Divider,
            properties = emptyMap()
        )
    }
}
