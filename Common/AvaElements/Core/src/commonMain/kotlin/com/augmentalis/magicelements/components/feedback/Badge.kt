package com.augmentalis.avaelements.components.feedback

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

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
    override val type: String = "Badge",
    val content: String = "",
    val variant: BadgeVariant = BadgeVariant.Default,
    val size: BadgeSize = BadgeSize.Medium,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
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
