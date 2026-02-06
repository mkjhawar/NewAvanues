package com.avanueui.data

import com.augmentalis.avamagic.components.core.*
import com.augmentalis.avamagic.components.core.ComponentStyle
import com.augmentalis.avamagic.components.core.Size
import com.augmentalis.avamagic.components.core.Color
import com.augmentalis.avamagic.components.core.Spacing
import com.augmentalis.avamagic.components.core.Border
import com.augmentalis.avamagic.components.core.Shadow

/**
 * EmptyState Component
 *
 * An empty state component for displaying a message when no content is available.
 *
 * Features:
 * - Optional icon
 * - Title and description
 * - Optional action button
 *
 * Platform mappings:
 * - Android: Custom empty view
 * - iOS: Empty state view
 * - Web: Empty state layout
 *
 * Usage:
 * ```kotlin
 * EmptyState(
 *     icon = "inbox",
 *     title = "No messages",
 *     description = "You don't have any messages yet",
 *     action = ButtonComponent(
 *         text = "Compose",
 *         onClick = { /* compose */ }
 *     )
 * )
 * ```
 */
data class EmptyStateComponent(
    val type: String = "EmptyState",
    val icon: String? = null,
    val title: String,
    val description: String? = null,
    val action: Component? = null,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    init {
        require(title.isNotBlank()) { "EmptyState title cannot be blank" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}
