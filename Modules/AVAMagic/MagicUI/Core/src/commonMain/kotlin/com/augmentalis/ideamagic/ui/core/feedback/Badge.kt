package com.augmentalis.magicui.ui.core.feedback

import com.augmentalis.magicui.components.core.*

/**
 * Badge Component
 *
 * A small status badge or chip component.
 *
 * Features:
 * - Text or number content
 * - Color variants (primary/secondary/success/warning/error)
 * - Size variants (small/medium/large)
 * - Dot variant (no text)
 * - Positioned relative to parent (for notifications)
 * - Rounded or square styling
 * - Icon support
 *
 * Platform mappings:
 * - Android: Badge from Material Components
 * - iOS: Custom badge view
 * - Web: Badge element
 *
 * Usage:
 * ```kotlin
 * Badge(
 *     content = "New",
 *     variant = BadgeVariant.Primary,
 *     size = BadgeSize.Small
 * )
 *
 * // Notification badge
 * Badge(
 *     content = "5",
 *     variant = BadgeVariant.Error,
 *     size = BadgeSize.Small
 * )
 * ```
 */
data class BadgeComponent(
    val content: String = "",
    val variant: BadgeVariant = BadgeVariant.Default,
    val size: BadgeSize = BadgeSize.Medium,
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList()
) {
}

/**
 * Badge color variants
 */
enum class BadgeVariant {
    Default,
    Primary,
    Secondary,
    Success,
    Warning,
    Error
}

/**
 * Badge size variants
 */
enum class BadgeSize {
    Small,
    Medium,
    Large
}
