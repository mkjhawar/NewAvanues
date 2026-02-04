package com.augmentalis.avaelements.flutter.material.navigation

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * NavLink component - Flutter Material parity
 *
 * A navigation link with active state styling, typically used in navigation bars,
 * sidebars, or breadcrumbs. Supports icons, badges, and router integration.
 *
 * **Flutter Equivalent:** Custom navigation link widget
 * **Material Design 3:** https://m3.material.io/components/navigation-bar/overview
 *
 * ## Features
 * - Active state highlighting
 * - Icon + label support
 * - Optional badge/counter for notifications
 * - Router integration ready (href property)
 * - Keyboard navigation support
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 * - Minimum 48dp touch target
 *
 * ## Usage Example
 * ```kotlin
 * NavLink(
 *     label = "Dashboard",
 *     href = "/dashboard",
 *     icon = "dashboard",
 *     active = true,
 *     badge = "5",
 *     onClick = { /* Navigate to dashboard */ }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property label Display text for the link
 * @property href Target route/URL for navigation
 * @property icon Optional icon identifier
 * @property active Whether this link represents the current page/section
 * @property badge Optional badge text (e.g., notification count "5", "New")
 * @property enabled Whether the link is enabled for interaction
 * @property iconPosition Position of icon relative to label
 * @property activeColor Color when link is active
 * @property inactiveColor Color when link is inactive
 * @property backgroundColor Background color of the link
 * @property activeBackgroundColor Background color when active
 * @property shape Custom shape for the link container
 * @property contentPadding Custom padding for the link
 * @property contentDescription Accessibility description for TalkBack
 * @property onClick Callback invoked when link is clicked (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class NavLink(
    override val type: String = "NavLink",
    override val id: String? = null,
    val label: String,
    val href: String,
    val icon: String? = null,
    val active: Boolean = false,
    val badge: String? = null,
    val enabled: Boolean = true,
    val iconPosition: IconPosition = IconPosition.Leading,
    val activeColor: String? = null,
    val inactiveColor: String? = null,
    val backgroundColor: String? = null,
    val activeBackgroundColor: String? = null,
    val shape: String? = null,
    val contentPadding: com.augmentalis.avaelements.core.types.Spacing? = null,
    val contentDescription: String? = null,
    @Transient
    val onClick: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Icon position relative to label
     */
    enum class IconPosition {
        /** Icon appears before the label (left in LTR) */
        Leading,

        /** Icon appears after the label (right in LTR) */
        Trailing,

        /** Icon appears above the label */
        Top,

        /** Icon appears below the label */
        Bottom
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: label
        val parts = mutableListOf(base)
        if (active) parts.add("current page")
        if (badge != null) parts.add("badge: $badge")
        if (!enabled) parts.add("disabled")
        return parts.joinToString(", ")
    }

    /**
     * Check if link has badge
     */
    fun hasBadge(): Boolean = badge != null && badge.isNotBlank()

    /**
     * Check if link has icon
     */
    fun hasIcon(): Boolean = icon != null && icon.isNotBlank()

    companion object {
        /**
         * Create an active navigation link
         */
        fun active(
            label: String,
            href: String,
            icon: String? = null,
            onClick: (() -> Unit)? = null
        ) = NavLink(
            label = label,
            href = href,
            icon = icon,
            active = true,
            onClick = onClick
        )

        /**
         * Create an inactive navigation link
         */
        fun inactive(
            label: String,
            href: String,
            icon: String? = null,
            onClick: (() -> Unit)? = null
        ) = NavLink(
            label = label,
            href = href,
            icon = icon,
            active = false,
            onClick = onClick
        )

        /**
         * Create a navigation link with badge
         */
        fun withBadge(
            label: String,
            href: String,
            badge: String,
            active: Boolean = false,
            onClick: (() -> Unit)? = null
        ) = NavLink(
            label = label,
            href = href,
            badge = badge,
            active = active,
            onClick = onClick
        )

        /**
         * Create a breadcrumb link
         */
        fun breadcrumb(
            label: String,
            href: String,
            active: Boolean = false,
            onClick: (() -> Unit)? = null
        ) = NavLink(
            label = label,
            href = href,
            active = active,
            icon = null,
            onClick = onClick
        )
    }
}
