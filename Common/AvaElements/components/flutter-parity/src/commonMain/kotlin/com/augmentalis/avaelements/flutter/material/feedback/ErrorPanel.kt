package com.augmentalis.avaelements.flutter.material.feedback

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ErrorPanel component - Flutter Material parity
 *
 * A Material Design 3 error panel with red theme and error icon.
 * Used for displaying error messages, validation failures, or critical issues.
 *
 * **Web Equivalent:** `Alert` severity="error" (MUI)
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Red error color theme
 * - Error icon (⚠)
 * - Title and message content
 * - Optional dismiss button
 * - Optional action buttons (e.g., "Retry", "Report")
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility with "Error" announcement
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * ErrorPanel(
 *     title = "Upload Failed",
 *     message = "The file could not be uploaded. Please check your connection and try again.",
 *     dismissible = true,
 *     actions = listOf(
 *         ErrorPanel.Action("Retry") { /* Retry upload */ },
 *         ErrorPanel.Action("Report Issue") { /* Open support */ }
 *     ),
 *     onDismiss = { /* Handle dismiss */ }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Panel title
 * @property message Panel message content
 * @property icon Optional custom icon (defaults to "error")
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
data class ErrorPanel(
    override val type: String = "ErrorPanel",
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
     * Get effective icon (default to "error")
     */
    fun getEffectiveIcon(): String {
        return icon ?: "error"
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Error"
        val dismissInfo = if (dismissible) ", dismissible" else ""
        val actionsInfo = if (actions.isNotEmpty()) ", ${actions.size} actions available" else ""
        return "$base: $title. $message$dismissInfo$actionsInfo"
    }

    companion object {
        /**
         * Create a simple error panel
         */
        fun simple(
            title: String,
            message: String
        ) = ErrorPanel(
            title = title,
            message = message
        )

        /**
         * Create a dismissible error panel
         */
        fun dismissible(
            title: String,
            message: String,
            onDismiss: (() -> Unit)? = null
        ) = ErrorPanel(
            title = title,
            message = message,
            dismissible = true,
            onDismiss = onDismiss
        )

        /**
         * Create an error panel with retry action
         */
        fun withRetry(
            title: String,
            message: String,
            onRetry: (() -> Unit)? = null
        ) = ErrorPanel(
            title = title,
            message = message,
            actions = listOf(
                Action("Retry", onRetry)
            )
        )
    }
}
