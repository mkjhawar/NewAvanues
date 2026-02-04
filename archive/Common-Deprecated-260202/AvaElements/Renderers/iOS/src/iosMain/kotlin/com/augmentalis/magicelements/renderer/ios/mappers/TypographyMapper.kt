package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.magicelements.components.phase3.display.*
import com.augmentalis.magicelements.renderer.ios.bridge.*

object HeadingTextMapper {
    fun map(component: HeadingText): SwiftUIComponent {
        val font = when (component.level) {
            HeadingLevel.H1 -> "largeTitle"
            HeadingLevel.H2 -> "title"
            HeadingLevel.H3 -> "title2"
            HeadingLevel.H4 -> "title3"
            HeadingLevel.H5 -> "headline"
            HeadingLevel.H6 -> "subheadline"
        }

        val alignment = when (component.textAlign) {
            TextAlignment.Start -> "leading"
            TextAlignment.Center -> "center"
            TextAlignment.End -> "trailing"
            TextAlignment.Justify -> "leading"
        }

        return SwiftUIComponent(
            type = "Text",
            props = mapOf("content" to component.text),
            modifiers = listOfNotNull(
                SwiftUIModifier("font", listOf(font)),
                component.fontWeight?.let { SwiftUIModifier("fontWeight", listOf(it)) },
                component.color?.let { SwiftUIModifier("foregroundColor", listOf(it)) },
                SwiftUIModifier("multilineTextAlignment", listOf(alignment)),
                component.maxLines?.let { SwiftUIModifier("lineLimit", listOf(it)) }
            )
        )
    }
}

object DisplayTextMapper {
    fun map(component: DisplayText): SwiftUIComponent {
        val fontSize = when (component.size) {
            DisplaySize.Small -> 32
            DisplaySize.Medium -> 48
            DisplaySize.Large -> 64
            DisplaySize.XLarge -> 80
        }

        return SwiftUIComponent(
            type = "Text",
            props = mapOf("content" to component.text),
            modifiers = listOf(
                SwiftUIModifier("font", listOf("system(size: $fontSize, weight: .bold)")),
                SwiftUIModifier("foregroundColor", listOf(component.color ?: "primary"))
            )
        )
    }
}

object BodyTextMapper {
    fun map(component: BodyText): SwiftUIComponent {
        val font = when (component.size) {
            BodySize.Small -> "footnote"
            BodySize.Medium -> "body"
            BodySize.Large -> "title3"
        }

        return SwiftUIComponent(
            type = "Text",
            props = mapOf("content" to component.text),
            modifiers = listOfNotNull(
                SwiftUIModifier("font", listOf(font)),
                component.color?.let { SwiftUIModifier("foregroundColor", listOf(it)) },
                component.lineHeight?.let { SwiftUIModifier("lineSpacing", listOf(it)) }
            )
        )
    }
}

object BorderDecoratorMapper {
    fun map(component: BorderDecorator, renderChild: (Any) -> SwiftUIComponent): SwiftUIComponent {
        val children = component.content.map { renderChild(it) }

        return SwiftUIComponent(
            type = "VStack",
            children = children,
            modifiers = listOf(
                SwiftUIModifier("overlay", listOf(
                    "RoundedRectangle(cornerRadius: ${component.radius})" to "stroke(${component.color}, lineWidth: ${component.width})"
                )),
                SwiftUIModifier("cornerRadius", listOf(component.radius))
            )
        )
    }
}
