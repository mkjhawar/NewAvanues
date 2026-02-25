package com.avanueui.data

import com.augmentalis.avanueui.core.*
import com.augmentalis.avanueui.core.ComponentStyle
import com.augmentalis.avanueui.core.Size
import com.augmentalis.avanueui.core.Color
import com.augmentalis.avanueui.core.Spacing
import com.augmentalis.avanueui.core.Border
import com.augmentalis.avanueui.core.Shadow

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
    val type: String = "Avatar",
    val source: String? = null,
    val text: String? = null,
    val size: AvatarSize = AvatarSize.Medium,
    val shape: AvatarShape = AvatarShape.Circle,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    init {
        require(source != null || text != null) { "Avatar must have either source or text" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
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
