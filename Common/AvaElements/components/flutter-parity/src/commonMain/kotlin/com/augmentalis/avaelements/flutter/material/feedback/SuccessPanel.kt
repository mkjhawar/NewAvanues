package com.augmentalis.avaelements.flutter.material.feedback

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * SuccessPanel component - Flutter Material parity
 *
 * A Material Design 3 success panel with green theme and checkmark icon.
 * Used for displaying success messages, completion confirmations, or positive feedback.
 *
 * **Web Equivalent:** `Alert` severity="success" (MUI)
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Green success color theme
 * - Checkmark icon (✓)
 * - Title and message content
 * - Optional dismiss button
 * - Optional action buttons
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility with "Success" announcement
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * SuccessPanel(
 *     title = "Payment Successful",
 *     message = "Your payment of $49.99 has been processed successfully.",
 *     dismissible = true,
 *     actions = listOf(
 *         SuccessPanel.Action("View Receipt") { /* Show receipt */ }
 *     ),
 *     onDismiss = { /* Handle dismiss */ }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Panel title
 * @property message Panel message content
 * @property icon Optional custom icon (defaults to "check_circle")
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
data class SuccessPanel(
    override val type: String = "SuccessPanel",
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
     * Get effective icon (default to "check_circle")
     */
    fun getEffectiveIcon(): String {
        return icon ?: "check_circle"
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Success"
        val dismissInfo = if (dismissible) ", dismissible" else ""
        val actionsInfo = if (actions.isNotEmpty()) ", ${actions.size} actions available" else ""
        return "$base: $title. $message$dismissInfo$actionsInfo"
    }

    companion object {
        /**
         * Create a simple success panel
         */
        fun simple(
            title: String,
            message: String
        ) = SuccessPanel(
            title = title,
            message = message
        )

        /**
         * Create a dismissible success panel
         */
        fun dismissible(
            title: String,
            message: String,
            onDismiss: (() -> Unit)? = null
        ) = SuccessPanel(
            title = title,
            message = message,
            dismissible = true,
            onDismiss = onDismiss
        )

        /**
         * Create a success panel with actions
         */
        fun withActions(
            title: String,
            message: String,
            actions: List<Action>
        ) = SuccessPanel(
            title = title,
            message = message,
            actions = actions
        )
    }
}
