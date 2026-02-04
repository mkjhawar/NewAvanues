package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.magicelements.components.phase3.layout.StickyHeader
import com.augmentalis.magicelements.components.phase3.layout.PullToRefresh
import com.augmentalis.magicelements.renderer.ios.bridge.*

object StickyHeaderMapper {
    fun map(component: StickyHeader, renderChild: (Any) -> SwiftUIComponent): SwiftUIComponent {
        val children = component.content.map { renderChild(it) }

        return SwiftUIComponent(
            type = "VStack",
            props = mapOf(
                "alignment" to "leading",
                "spacing" to 0
            ),
            modifiers = listOf(
                SwiftUIModifier("background", listOf(component.backgroundColor ?: "systemBackground")),
                SwiftUIModifier("shadow", listOf("radius" to component.elevation)),
                SwiftUIModifier("zIndex", listOf(component.zIndex))
            ),
            children = children
        )
    }
}

object PullToRefreshMapper {
    fun map(component: PullToRefresh, renderChild: (Any) -> SwiftUIComponent): SwiftUIComponent {
        val children = component.content.map { renderChild(it) }

        return SwiftUIComponent(
            type = "ScrollView",
            children = listOf(
                SwiftUIComponent(
                    type = "LazyVStack",
                    children = children,
                    modifiers = listOf(
                        SwiftUIModifier("refreshable", emptyList())
                    )
                )
            )
        )
    }
}
