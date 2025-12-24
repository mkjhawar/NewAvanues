package com.augmentalis.magicui.ui.core.feedback

import com.augmentalis.magicui.components.core.*

/**
 * Tooltip Component
 *
 * A hover tooltip component for displaying contextual information.
 *
 * Features:
 * - Appears on hover or focus
 * - Position control (top/bottom/left/right)
 * - Optional arrow pointing to target
 * - Configurable delay before showing
 * - Auto-positioning to stay on screen
 * - Touch-friendly for mobile
 * - Keyboard accessible
 *
 * Platform mappings:
 * - Android: Custom tooltip view
 * - iOS: Custom tooltip popover
 * - Web: Title attribute or custom tooltip
 *
 * Usage:
 * ```kotlin
 * Tooltip(
 *     content = "Click here to save your work",
 *     position = TooltipPosition.Top,
 *     child = ButtonComponent(...)
 * )
 * ```
 */
data class TooltipComponent(
    val content: String,
    val position: TooltipPosition = TooltipPosition.Top,
    val child: Any,
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList()
) {
    init {
        require(content.isNotBlank()) { "Tooltip content cannot be blank" }
    }

}

/**
 * Tooltip position relative to the child component
 */
enum class TooltipPosition {
    Top,
    Bottom,
    Left,
    Right
}
