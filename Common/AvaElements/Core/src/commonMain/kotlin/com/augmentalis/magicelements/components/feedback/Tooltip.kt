package com.augmentalis.avaelements.components.feedback

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

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
    override val type: String = "Tooltip",
    val content: String,
    val position: TooltipPosition = TooltipPosition.Top,
    val child: Component,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    init {
        require(content.isNotBlank()) { "Tooltip content cannot be blank" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
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
