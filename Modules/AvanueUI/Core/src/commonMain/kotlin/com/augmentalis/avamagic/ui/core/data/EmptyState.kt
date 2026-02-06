package com.augmentalis.avamagic.ui.core.data

import com.augmentalis.avamagic.components.core.*

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
    val icon: String? = null,
    val title: String,
    val description: String? = null,
    val action: Any? = null,
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList()
) {
    init {
        require(title.isNotBlank()) { "EmptyState title cannot be blank" }
    }

}
