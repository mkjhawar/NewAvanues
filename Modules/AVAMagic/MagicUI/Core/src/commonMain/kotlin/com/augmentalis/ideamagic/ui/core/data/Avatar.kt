package com.augmentalis.magicui.ui.core.data

import com.augmentalis.magicui.components.core.*

/**
 * Avatar Component
 *
 * An avatar component for displaying user profile pictures or initials.
 *
 * Features:
 * - Image or text (initials) display
 * - Multiple sizes (small, medium, large)
 * - Multiple shapes (circle, square, rounded)
 * - Fallback to initials if no image
 *
 * Platform mappings:
 * - Android: ImageView with custom shape
 * - iOS: UIImageView with corner radius
 * - Web: Image with CSS styling
 *
 * Usage:
 * ```kotlin
 * Avatar(
 *     source = "https://example.com/avatar.jpg",
 *     text = "JD",  // fallback
 *     size = AvatarSize.Medium,
 *     shape = AvatarShape.Circle
 * )
 * ```
 */
data class AvatarComponent(
    val source: String? = null,
    val text: String? = null,
    val size: AvatarSize = AvatarSize.Medium,
    val shape: AvatarShape = AvatarShape.Circle,
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList()
) {
    init {
        require(source != null || text != null) { "Avatar must have either source or text" }
    }

}

/**
 * Avatar size presets
 */
enum class AvatarSize {
    Small,   // 32dp
    Medium,  // 48dp
    Large    // 64dp
}

/**
 * Avatar shape options
 */
enum class AvatarShape {
    Circle,
    Square,
    Rounded  // Rounded corners
}
