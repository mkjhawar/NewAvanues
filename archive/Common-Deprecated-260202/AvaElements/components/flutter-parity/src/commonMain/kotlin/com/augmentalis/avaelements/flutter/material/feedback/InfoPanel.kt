package com.augmentalis.avaelements.flutter.material.feedback

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * InfoPanel component - Flutter Material parity
 *
 * A Material Design 3 information panel with blue theme and info icon.
 * Used for displaying informational messages, tips, or helpful content.
 *
 * **Web Equivalent:** `Alert` severity="info" (MUI)
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Blue primary color theme
 * - Info icon (ⓘ)
 * - Title and message content
 * - Optional dismiss button
 * - Optional action buttons
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * InfoPanel(
 *     title = "Did you know?",
 *     message = "You can customize your dashboard layout by dragging widgets",
 *     dismissible = true,
 *     actions = listOf(
 *         InfoPanel.Action("Learn More") { /* Navigate to help */ }
 *     ),
 *     onDismiss = { /* Handle dismiss */ }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Panel title
 * @property message Panel message content
 * @property icon Optional custom icon (defaults to "info")
 * @property dismissible Whether to show dismiss button
 * @property actions Optional list of action buttons
 * @property elevation Shadow elevation in dp
 * @property contentDescription Accessibility description for TalkBack
 * @property onDismiss Callback invoked when dismissed (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.2.0-feedback-components
 */
data class InfoPanel(
    override val type: String = "InfoPanel",
    override val id: String? = null,
    val title: String,
    val message: String,
    val icon: String? = null,
    val dismissible: Boolean = false,
    val actions: List<Action> = emptyList(),
    val elevation: Float = 0f,
    val contentDescription: String? = null,
    @Transient
    val onDismiss: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Action button for panel
     */
    data class Action(
        val label: String,
        @Transient
        val onClick: (() -> Unit)? = null
    )

    /**
     * Get effective icon (default to "info")
     */
    fun getEffectiveIcon(): String {
        return icon ?: "info"
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Information"
        val dismissInfo = if (dismissible) ", dismissible" else ""
        val actionsInfo = if (actions.isNotEmpty()) ", ${actions.size} actions available" else ""
        return "$base: $title. $message$dismissInfo$actionsInfo"
    }

    companion object {
        /**
         * Create a simple info panel
         */
        fun simple(
            title: String,
            message: String
        ) = InfoPanel(
            title = title,
            message = message
        )

        /**
         * Create a dismissible info panel
         */
        fun dismissible(
            title: String,
            message: String,
            onDismiss: (() -> Unit)? = null
        ) = InfoPanel(
            title = title,
            message = message,
            dismissible = true,
            onDismiss = onDismiss
        )

        /**
         * Create an info panel with actions
         */
        fun withActions(
            title: String,
            message: String,
            actions: List<Action>
        ) = InfoPanel(
            title = title,
            message = message,
            actions = actions
        )
    }
}
