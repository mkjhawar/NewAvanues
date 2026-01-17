/**
 * BuiltInComponents.kt - Pre-built component definitions for common widgets
 *
 * Provides factory functions to create common widget definitions programmatically
 * without requiring YAML parsing.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 * Refactored: 2026-01-08 (SRP extraction from ComponentFactory.kt)
 */
package com.augmentalis.voiceoscore

/**
 * Pre-built component definitions for common widgets.
 */
object BuiltInComponents {

    /**
     * Create a simple container definition.
     */
    fun container(
        id: String = "",
        background: String? = null,
        cornerRadius: String? = null,
        padding: String? = null,
        children: List<WidgetDefinition> = emptyList()
    ): WidgetDefinition {
        return WidgetDefinition(
            widget = WidgetType.CONTAINER,
            id = id,
            props = WidgetProps(
                background = background,
                cornerRadius = cornerRadius,
                padding = padding?.let { PaddingValue(all = it) }
            ),
            children = children
        )
    }

    /**
     * Create a text widget definition.
     */
    fun text(
        text: String,
        id: String = "",
        color: String? = null,
        fontSize: String? = null,
        fontWeight: String? = null,
        textAlign: String? = null
    ): WidgetDefinition {
        return WidgetDefinition(
            widget = WidgetType.TEXT,
            id = id,
            props = WidgetProps(
                text = text,
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = textAlign
            )
        )
    }

    /**
     * Create a column layout definition.
     */
    fun column(
        id: String = "",
        spacing: String? = null,
        alignment: String? = null,
        children: List<WidgetDefinition> = emptyList()
    ): WidgetDefinition {
        return WidgetDefinition(
            widget = WidgetType.COLUMN,
            id = id,
            props = WidgetProps(
                spacing = spacing,
                alignment = alignment
            ),
            children = children
        )
    }

    /**
     * Create a row layout definition.
     */
    fun row(
        id: String = "",
        spacing: String? = null,
        alignment: String? = null,
        children: List<WidgetDefinition> = emptyList()
    ): WidgetDefinition {
        return WidgetDefinition(
            widget = WidgetType.ROW,
            id = id,
            props = WidgetProps(
                spacing = spacing,
                alignment = alignment
            ),
            children = children
        )
    }

    /**
     * Create an icon widget definition.
     */
    fun icon(
        icon: String,
        id: String = "",
        size: String? = null,
        color: String? = null
    ): WidgetDefinition {
        return WidgetDefinition(
            widget = WidgetType.ICON,
            id = id,
            props = WidgetProps(
                icon = icon,
                size = size,
                color = color
            )
        )
    }

    /**
     * Create a badge widget definition.
     */
    fun badge(
        number: String,
        id: String = "",
        background: String? = null,
        color: String? = null,
        size: String? = null
    ): WidgetDefinition {
        return WidgetDefinition(
            widget = WidgetType.BADGE,
            id = id,
            props = WidgetProps(
                number = number,
                background = background,
                color = color,
                size = size
            )
        )
    }
}
