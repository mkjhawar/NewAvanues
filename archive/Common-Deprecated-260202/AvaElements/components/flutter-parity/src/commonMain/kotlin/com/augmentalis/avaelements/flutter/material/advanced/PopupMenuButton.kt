package com.augmentalis.avaelements.flutter.material.advanced

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * PopupMenuButton component - Flutter Material parity
 *
 * A button that shows a popup menu when clicked, following Material Design 3 specifications.
 * Displays a list of menu items in a dropdown overlay.
 *
 * **Flutter Equivalent:** `PopupMenuButton`
 * **Material Design 3:** https://m3.material.io/components/menus/overview
 *
 * ## Features
 * - Shows popup menu on click
 * - Customizable menu items
 * - Automatic positioning (below, above, or adaptive)
 * - Keyboard navigation support
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * PopupMenuButton(
 *     icon = "more_vert",
 *     tooltip = "More options",
 *     items = listOf(
 *         PopupMenuItem("1", "Edit", icon = "edit"),
 *         PopupMenuItem("2", "Delete", icon = "delete", enabled = false),
 *         PopupMenuItem("3", "Share", icon = "share")
 *     ),
 *     onSelected = { value ->
 *         // Handle menu item selection
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property icon Optional icon for the button (default: more_vert)
 * @property child Optional custom child widget (overrides icon)
 * @property tooltip Tooltip text shown on hover
 * @property initialValue Initially selected item value
 * @property items List of menu items to display
 * @property enabled Whether the button is enabled
 * @property offset Menu offset from anchor point
 * @property elevation Menu elevation (shadow depth)
 * @property shape Menu shape override
 * @property color Menu background color
 * @property iconSize Size of the button icon
 * @property position Menu position relative to button
 * @property contentDescription Accessibility description for TalkBack
 * @property onSelected Callback invoked when menu item is selected (not serialized)
 * @property onOpened Callback invoked when menu is opened (not serialized)
 * @property onCanceled Callback invoked when menu is dismissed without selection (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class PopupMenuButton(
    override val type: String = "PopupMenuButton",
    override val id: String? = null,
    val icon: String? = "more_vert",
    val child: Component? = null,
    val tooltip: String? = null,
    val initialValue: String? = null,
    val items: List<PopupMenuItem> = emptyList(),
    val enabled: Boolean = true,
    val offset: Offset = Offset(0f, 0f),
    val elevation: Float? = null,
    val shape: String? = null,
    val color: String? = null,
    val iconSize: Float? = null,
    val position: PopupMenuPosition = PopupMenuPosition.Below,
    val contentDescription: String? = null,
    @Transient
    val onSelected: ((String) -> Unit)? = null,
    @Transient
    val onOpened: (() -> Unit)? = null,
    @Transient
    val onCanceled: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        return contentDescription ?: tooltip ?: "Show menu"
    }

    /**
     * Menu position relative to button
     */
    enum class PopupMenuPosition {
        /** Menu appears below the button */
        Below,

        /** Menu appears above the button */
        Above,

        /** Menu position adapts based on available space */
        Auto
    }

    /**
     * Offset for menu positioning
     */
    @Serializable
    data class Offset(
        val dx: Float = 0f,
        val dy: Float = 0f
    )

    companion object {
        /**
         * Create a simple popup menu button with text items
         */
        fun simple(
            items: List<PopupMenuItem>,
            onSelected: ((String) -> Unit)? = null
        ) = PopupMenuButton(
            items = items,
            onSelected = onSelected
        )

        /**
         * Create a popup menu button with custom icon
         */
        fun withIcon(
            icon: String,
            items: List<PopupMenuItem>,
            tooltip: String? = null,
            onSelected: ((String) -> Unit)? = null
        ) = PopupMenuButton(
            icon = icon,
            tooltip = tooltip,
            items = items,
            onSelected = onSelected
        )

        /**
         * Create a popup menu button with tooltip
         */
        fun withTooltip(
            tooltip: String,
            items: List<PopupMenuItem>,
            onSelected: ((String) -> Unit)? = null
        ) = PopupMenuButton(
            tooltip = tooltip,
            items = items,
            onSelected = onSelected
        )
    }
}

/**
 * Menu item for PopupMenuButton
 *
 * @property value Unique value identifying this menu item
 * @property text Display text for the menu item
 * @property icon Optional icon resource name
 * @property enabled Whether the menu item is enabled
 * @property height Custom height override
 */
@Serializable
data class PopupMenuItem(
    val value: String,
    val text: String,
    val icon: String? = null,
    val enabled: Boolean = true,
    val height: Float? = null
)
