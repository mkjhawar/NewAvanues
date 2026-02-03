package com.augmentalis.avaelements.flutter.material.advanced

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * CircleAvatar component - Flutter Material parity
 *
 * A circular avatar widget typically used to represent a user with an image, icon, or initials.
 * Follows Material Design 3 specifications for avatar components.
 *
 * **Flutter Equivalent:** `CircleAvatar`
 * **Material Design 3:** https://m3.material.io/components/avatar/overview
 *
 * ## Features
 * - Circular shape with proper clipping
 * - Supports image, icon, or text (initials)
 * - Customizable radius and colors
 * - Background and foreground color support
 * - Material3 theming with proper contrast
 * - Dark mode support
 * - TalkBack accessibility with proper role and description
 * - WCAG 2.1 Level AA compliant (4.5:1 contrast for text)
 * - Minimum 24dp size for touch targets
 *
 * ## Usage Example
 * ```kotlin
 * // Image avatar
 * CircleAvatar(
 *     backgroundImage = "https://example.com/avatar.jpg",
 *     radius = 20f,
 *     contentDescription = "John Doe's profile picture"
 * )
 *
 * // Initials avatar
 * CircleAvatar(
 *     child = Text("JD"),
 *     backgroundColor = "primary",
 *     foregroundColor = "onPrimary",
 *     radius = 20f
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property child Optional child component (typically Text for initials or Icon)
 * @property backgroundImage URL or resource for background image
 * @property backgroundColor Background color (used when no image or as fallback)
 * @property foregroundColor Foreground color (for text/icon)
 * @property radius Radius of the circle (in dp)
 * @property minRadius Minimum radius constraint
 * @property maxRadius Maximum radius constraint
 * @property onBackgroundImageError Fallback when background image fails to load
 * @property semanticsLabel Accessibility label for the avatar
 * @property contentDescription Accessibility description for TalkBack
 * @property onTap Callback invoked when avatar is tapped (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class CircleAvatar(
    override val type: String = "CircleAvatar",
    override val id: String? = null,
    val child: Component? = null,
    val backgroundImage: String? = null,
    val backgroundColor: String? = null,
    val foregroundColor: String? = null,
    val radius: Float? = null,
    val minRadius: Float? = null,
    val maxRadius: Float? = null,
    val onBackgroundImageError: Component? = null,
    val semanticsLabel: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onTap: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective radius considering min/max constraints
     */
    fun getEffectiveRadius(): Float {
        val baseRadius = radius ?: DEFAULT_RADIUS
        val constrainedRadius = when {
            minRadius != null && baseRadius < minRadius -> minRadius
            maxRadius != null && baseRadius > maxRadius -> maxRadius
            else -> baseRadius
        }
        return constrainedRadius
    }

    /**
     * Get effective diameter (2 * radius)
     */
    fun getDiameter(): Float {
        return getEffectiveRadius() * 2
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        return contentDescription ?: semanticsLabel ?: "Avatar"
    }

    /**
     * Check if avatar has image
     */
    fun hasImage(): Boolean {
        return !backgroundImage.isNullOrBlank()
    }

    /**
     * Check if avatar has child content
     */
    fun hasChild(): Boolean {
        return child != null
    }

    /**
     * Get avatar type for accessibility
     */
    fun getAvatarType(): String {
        return when {
            hasImage() -> "image avatar"
            hasChild() -> "avatar"
            else -> "empty avatar"
        }
    }

    companion object {
        /**
         * Default radius (in dp)
         */
        const val DEFAULT_RADIUS = 20f

        /**
         * Minimum recommended radius for accessibility (in dp)
         */
        const val MIN_TOUCH_RADIUS = 12f

        /**
         * Create a simple circle avatar with image
         */
        fun fromImage(
            imageUrl: String,
            radius: Float = DEFAULT_RADIUS,
            contentDescription: String? = null
        ) = CircleAvatar(
            backgroundImage = imageUrl,
            radius = radius,
            contentDescription = contentDescription
        )

        /**
         * Create a circle avatar with initials
         */
        fun fromInitials(
            initials: String,
            backgroundColor: String? = null,
            foregroundColor: String? = null,
            radius: Float = DEFAULT_RADIUS,
            contentDescription: String? = null
        ) = CircleAvatar(
            child = createTextComponent(initials),
            backgroundColor = backgroundColor,
            foregroundColor = foregroundColor,
            radius = radius,
            contentDescription = contentDescription
        )

        /**
         * Create a circle avatar with icon
         */
        fun fromIcon(
            icon: String,
            backgroundColor: String? = null,
            foregroundColor: String? = null,
            radius: Float = DEFAULT_RADIUS,
            contentDescription: String? = null
        ) = CircleAvatar(
            child = createIconComponent(icon),
            backgroundColor = backgroundColor,
            foregroundColor = foregroundColor,
            radius = radius,
            contentDescription = contentDescription
        )

        /**
         * Create a small circle avatar (16dp radius)
         */
        fun small(
            backgroundImage: String? = null,
            child: Component? = null,
            contentDescription: String? = null
        ) = CircleAvatar(
            backgroundImage = backgroundImage,
            child = child,
            radius = 16f,
            contentDescription = contentDescription
        )

        /**
         * Create a large circle avatar (32dp radius)
         */
        fun large(
            backgroundImage: String? = null,
            child: Component? = null,
            contentDescription: String? = null
        ) = CircleAvatar(
            backgroundImage = backgroundImage,
            child = child,
            radius = 32f,
            contentDescription = contentDescription
        )

        /**
         * Helper to create text component for initials
         * Note: This is a placeholder - actual implementation should use proper Text component
         */
        private fun createTextComponent(text: String): Component {
            // This would be replaced with actual Text component from the framework
            return object : Component {
                override val type = "Text"
                override val id: String? = null
                override val style: ComponentStyle? = null
                override val modifiers: List<Modifier> = emptyList()
                override fun render(renderer: Renderer): Any = renderer.render(this)
            }
        }

        /**
         * Helper to create icon component
         * Note: This is a placeholder - actual implementation should use proper Icon component
         */
        private fun createIconComponent(icon: String): Component {
            return object : Component {
                override val type = "Icon"
                override val id: String? = null
                override val style: ComponentStyle? = null
                override val modifiers: List<Modifier> = emptyList()
                override fun render(renderer: Renderer): Any = renderer.render(this)
            }
        }
    }
}
