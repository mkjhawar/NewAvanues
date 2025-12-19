package com.augmentalis.avaelements.flutter.material.advanced

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * SplitButton component - Flutter Material parity
 *
 * A Material Design 3 split button with a primary action and dropdown menu for additional actions.
 * Provides a ButtonGroup-style component with main button + menu button.
 *
 * **Web Equivalent:** `SplitButton` (MUI)
 * **Material Design 3:** https://m3.material.io/components/buttons/overview
 *
 * ## Features
 * - Primary action button with dropdown menu
 * - Configurable menu items with individual handlers
 * - Optional icons for menu items
 * - Menu position control (bottom/top)
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * SplitButton(
 *     text = "Save",
 *     onPressed = {
 *         // Handle main action
 *     },
 *     menuItems = listOf(
 *         MenuItem("save_draft", "Save as Draft"),
 *         MenuItem("save_template", "Save as Template")
 *     ),
 *     onMenuItemPressed = { value ->
 *         // Handle menu item selection
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property text Button text label
 * @property icon Optional icon name/resource for main button
 * @property enabled Whether the button is enabled for user interaction
 * @property menuItems List of menu items to display
 * @property menuPosition Position of menu relative to button (bottom/top)
 * @property contentDescription Accessibility description for TalkBack
 * @property onPressed Callback invoked when main button is pressed (not serialized)
 * @property onMenuItemPressed Callback invoked when menu item is selected (not serialized)
 * @property style Optional button style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class SplitButton(
    override val type: String = "SplitButton",
    override val id: String? = null,
    val text: String,
    val icon: String? = null,
    val enabled: Boolean = true,
    val menuItems: List<MenuItem> = emptyList(),
    val menuPosition: MenuPosition = MenuPosition.Bottom,
    val contentDescription: String? = null,
    @Transient
    val onPressed: (() -> Unit)? = null,
    @Transient
    val onMenuItemPressed: ((String) -> Unit)? = null,
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
        val base = contentDescription ?: text
        val state = if (enabled) "split button" else "split button, disabled"
        return "$base, $state"
    }

    /**
     * Menu item data class
     */
    data class MenuItem(
        val value: String,
        val label: String,
        val icon: String? = null,
        val enabled: Boolean = true,
        @Transient
        val onPressed: (() -> Unit)? = null
    )

    /**
     * Menu position relative to button
     */
    enum class MenuPosition {
        /** Menu appears below button */
        Bottom,

        /** Menu appears above button */
        Top
    }

    companion object {
        /**
         * Create a simple split button with text only
         */
        fun simple(
            text: String,
            menuItems: List<MenuItem>,
            enabled: Boolean = true,
            onPressed: (() -> Unit)? = null,
            onMenuItemPressed: ((String) -> Unit)? = null
        ) = SplitButton(
            text = text,
            menuItems = menuItems,
            enabled = enabled,
            onPressed = onPressed,
            onMenuItemPressed = onMenuItemPressed
        )

        /**
         * Create a split button with icon
         */
        fun withIcon(
            text: String,
            icon: String,
            menuItems: List<MenuItem>,
            enabled: Boolean = true,
            onPressed: (() -> Unit)? = null,
            onMenuItemPressed: ((String) -> Unit)? = null
        ) = SplitButton(
            text = text,
            icon = icon,
            menuItems = menuItems,
            enabled = enabled,
            onPressed = onPressed,
            onMenuItemPressed = onMenuItemPressed
        )
    }
}
