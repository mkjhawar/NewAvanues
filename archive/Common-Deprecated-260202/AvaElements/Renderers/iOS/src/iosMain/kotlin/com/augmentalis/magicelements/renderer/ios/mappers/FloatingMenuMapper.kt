package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.magicelements.components.phase3.navigation.FloatingMenu
import com.augmentalis.magicelements.components.phase3.navigation.FloatingMenuPosition
import com.augmentalis.magicelements.renderer.ios.bridge.*

object FloatingMenuMapper {
    fun map(component: FloatingMenu): SwiftUIComponent {
        val alignment = when (component.position) {
            FloatingMenuPosition.BottomRight -> "bottomTrailing"
            FloatingMenuPosition.BottomLeft -> "bottomLeading"
            FloatingMenuPosition.BottomCenter -> "bottom"
            FloatingMenuPosition.TopRight -> "topTrailing"
            FloatingMenuPosition.TopLeft -> "topLeading"
        }

        val menuItems = component.items.reversed().mapIndexed { index, item ->
            SwiftUIComponent(
                type = "Button",
                props = mapOf(
                    "systemImage" to item.icon,
                    "label" to (item.label ?: "")
                ),
                modifiers = listOf(
                    SwiftUIModifier("buttonStyle", listOf("bordered")),
                    SwiftUIModifier("background", listOf(item.backgroundColor ?: "accentColor")),
                    SwiftUIModifier("clipShape", listOf("Circle")),
                    SwiftUIModifier("offset", listOf("y" to if (component.isOpen) -((index + 1) * (56 + component.spacing)) else 0)),
                    SwiftUIModifier("opacity", listOf(if (component.isOpen) 1.0 else 0.0)),
                    SwiftUIModifier("animation", listOf("spring"))
                )
            )
        }

        val mainButton = SwiftUIComponent(
            type = "Button",
            props = mapOf(
                "systemImage" to if (component.isOpen) component.mainIconOpen else component.mainIcon
            ),
            modifiers = listOf(
                SwiftUIModifier("font", listOf("system(size: 24)")),
                SwiftUIModifier("frame", listOf("width" to 56, "height" to 56)),
                SwiftUIModifier("background", listOf(component.backgroundColor ?: "accentColor")),
                SwiftUIModifier("foregroundColor", listOf(component.iconColor ?: "white")),
                SwiftUIModifier("clipShape", listOf("Circle")),
                SwiftUIModifier("shadow", listOf("radius" to 4)),
                SwiftUIModifier("rotationEffect", listOf(if (component.isOpen) 45 else 0))
            )
        )

        return SwiftUIComponent(
            type = "ZStack",
            props = mapOf("alignment" to alignment),
            children = menuItems + listOf(mainButton),
            modifiers = listOf(
                SwiftUIModifier("frame", listOf("maxWidth" to ".infinity", "maxHeight" to ".infinity")),
                SwiftUIModifier("padding", listOf(16))
            )
        )
    }
}
