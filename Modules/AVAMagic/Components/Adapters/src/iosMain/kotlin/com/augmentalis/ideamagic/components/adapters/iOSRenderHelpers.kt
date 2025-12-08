package com.augmentalis.avanues.avamagic.components.adapters

import com.augmentalis.avaelements.core.*

/**
 * iOS Renderer Helper Functions
 *
 * Provides mapping functions to convert IDEAMagic components to iOS-compatible formats.
 * These helpers bridge the gap between cross-platform components and native iOS SwiftUI views.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

/**
 * Map button variant to iOS button style
 */
fun mapButtonStyle(variant: ButtonVariant): String {
    return when (variant) {
        ButtonVariant.FILLED -> "filled"
        ButtonVariant.TONAL -> "tonal"
        ButtonVariant.OUTLINED -> "outlined"
        ButtonVariant.TEXT -> "text"
    }
}

/**
 * Map generic icon name to SF Symbol name
 *
 * Common icon mappings:
 * - "home" → "house.fill"
 * - "settings" → "gearshape.fill"
 * - "search" → "magnifyingglass"
 * - "user" → "person.fill"
 */
fun mapToSFSymbol(iconName: String): String {
    return when (iconName.lowercase()) {
        "home" -> "house.fill"
        "settings" -> "gearshape.fill"
        "search" -> "magnifyingglass"
        "user", "profile" -> "person.fill"
        "menu" -> "line.3.horizontal"
        "close", "x" -> "xmark"
        "check", "checkmark" -> "checkmark"
        "add", "plus" -> "plus"
        "edit", "pencil" -> "pencil"
        "delete", "trash" -> "trash.fill"
        "heart" -> "heart.fill"
        "star" -> "star.fill"
        "calendar" -> "calendar"
        "clock", "time" -> "clock.fill"
        "location", "pin" -> "location.fill"
        "camera" -> "camera.fill"
        "photo", "image" -> "photo.fill"
        "mail", "email" -> "envelope.fill"
        "phone" -> "phone.fill"
        "message", "chat" -> "message.fill"
        "notification", "bell" -> "bell.fill"
        "download" -> "arrow.down.circle.fill"
        "upload" -> "arrow.up.circle.fill"
        "share" -> "square.and.arrow.up"
        "favorite" -> "heart.fill"
        "bookmark" -> "bookmark.fill"
        "info" -> "info.circle.fill"
        "warning" -> "exclamationmark.triangle.fill"
        "error" -> "xmark.circle.fill"
        "success" -> "checkmark.circle.fill"
        else -> iconName  // Fallback to original name
    }
}

/**
 * Map text variant to iOS font style
 */
fun mapTextVariant(variant: TextVariant): String {
    return when (variant) {
        TextVariant.DISPLAY_LARGE -> "largeTitle"
        TextVariant.DISPLAY_MEDIUM -> "title"
        TextVariant.DISPLAY_SMALL -> "title2"
        TextVariant.HEADLINE_LARGE -> "title3"
        TextVariant.HEADLINE_MEDIUM -> "headline"
        TextVariant.HEADLINE_SMALL -> "subheadline"
        TextVariant.BODY_LARGE -> "body"
        TextVariant.BODY_MEDIUM -> "body"
        TextVariant.BODY_SMALL -> "callout"
        TextVariant.LABEL_LARGE -> "caption"
        TextVariant.LABEL_MEDIUM -> "caption2"
        TextVariant.LABEL_SMALL -> "footnote"
    }
}

/**
 * Map alignment to iOS Alignment
 */
fun mapAlignment(align: Alignment): String {
    return when (align) {
        Alignment.START, Alignment.LEFT -> "leading"
        Alignment.CENTER -> "center"
        Alignment.END, Alignment.RIGHT -> "trailing"
        Alignment.TOP -> "top"
        Alignment.BOTTOM -> "bottom"
    }
}

/**
 * Map size to iOS spacing
 */
fun mapSize(size: Size): Double {
    return when (size) {
        Size.SMALL -> 8.0
        Size.MEDIUM -> 16.0
        Size.LARGE -> 24.0
        Size.XLARGE -> 32.0
    }
}

/**
 * Map component list to iOS array
 */
fun mapChildren(children: List<Component>): List<Map<String, Any>> {
    val renderer = iOSRenderer()
    return children.map { child ->
        renderer.render(child) as Map<String, Any>
    }
}

/**
 * Component data structure for iOS bridge
 */
fun createComponentData(
    type: String,
    vararg properties: Pair<String, Any?>
): Map<String, Any?> {
    return mapOf("_type" to type) + properties.toMap().filterValues { it != null }
}

/**
 * Render all remaining component types with proper mapping
 */