package com.augmentalis.avaelements.flutter.material.cards

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * FeatureCard component - Flutter Material parity
 *
 * A Material Design 3 card for highlighting product features with icon, title, and description.
 * Commonly used in landing pages, feature showcases, and marketing materials.
 *
 * **Web Equivalent:** `FeatureCard` (MUI)
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Large prominent icon or image
 * - Title and description text
 * - Optional action button/link
 * - Horizontal or vertical layout
 * - Icon position control (top, left, right)
 * - Customizable icon size and color
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * FeatureCard(
 *     icon = "rocket",
 *     title = "Fast Performance",
 *     description = "Lightning-fast loading times with optimized rendering",
 *     iconColor = "#FF6B35",
 *     actionText = "Learn More",
 *     onActionPressed = {
 *         // Navigate to details
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property icon Icon name/resource for the feature
 * @property iconSize Size of the icon in dp
 * @property iconColor Optional icon color override
 * @property title Feature title
 * @property description Feature description text
 * @property actionText Optional action button text
 * @property actionIcon Optional icon for action button
 * @property layout Layout direction (vertical/horizontal)
 * @property iconPosition Position of icon relative to content
 * @property contentDescription Accessibility description for TalkBack
 * @property onPressed Callback invoked when card is pressed (not serialized)
 * @property onActionPressed Callback invoked when action button is pressed (not serialized)
 * @property style Optional card style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class FeatureCard(
    override val type: String = "FeatureCard",
    override val id: String? = null,
    val icon: String,
    val iconSize: Float = 48f,
    val iconColor: String? = null,
    val title: String,
    val description: String,
    val actionText: String? = null,
    val actionIcon: String? = null,
    val layout: Layout = Layout.Vertical,
    val iconPosition: IconPosition = IconPosition.Top,
    val contentDescription: String? = null,
    @Transient
    val onPressed: (() -> Unit)? = null,
    @Transient
    val onActionPressed: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        return contentDescription ?: "Feature: $title, $description"
    }

    /**
     * Layout direction
     */
    enum class Layout {
        /** Vertical layout with icon above text */
        Vertical,

        /** Horizontal layout with icon beside text */
        Horizontal
    }

    /**
     * Icon position relative to content
     */
    enum class IconPosition {
        /** Icon at top (vertical layout) */
        Top,

        /** Icon on left side */
        Left,

        /** Icon on right side */
        Right,

        /** Icon at bottom (vertical layout) */
        Bottom
    }

    companion object {
        /**
         * Create a simple feature card with icon and text
         */
        fun simple(
            icon: String,
            title: String,
            description: String,
            onPressed: (() -> Unit)? = null
        ) = FeatureCard(
            icon = icon,
            title = title,
            description = description,
            onPressed = onPressed
        )

        /**
         * Create a feature card with action button
         */
        fun withAction(
            icon: String,
            title: String,
            description: String,
            actionText: String,
            onActionPressed: (() -> Unit)? = null
        ) = FeatureCard(
            icon = icon,
            title = title,
            description = description,
            actionText = actionText,
            onActionPressed = onActionPressed
        )
    }
}
