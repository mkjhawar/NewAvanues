package com.augmentalis.avaelements.flutter.material.feedback

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * WarningPanel component - Flutter Material parity
 *
 * A Material Design 3 warning panel with orange/yellow theme and warning icon.
 * Used for displaying warning messages, cautionary notes, or non-critical issues.
 *
 * **Web Equivalent:** `Alert` severity="warning" (MUI)
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Orange/yellow warning color theme
 * - Warning icon (⚠)
 * - Title and message content
 * - Optional dismiss button
 * - Optional action buttons
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility with "Warning" announcement
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * WarningPanel(
 *     title = "Low Storage",
 *     message = "Your device is running low on storage space. Consider deleting unused files.",
 *     dismissible = true,
 *     actions = listOf(
 *         WarningPanel.Action("Manage Storage") { /* Open storage settings */ }
 *     ),
 *     onDismiss = { /* Handle dismiss */ }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Panel title
 * @property message Panel message content
 * @property icon Optional custom icon (defaults to "warning")
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
data class WarningPanel(
    override val type: String = "WarningPanel",
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
     * Get effective icon (default to "warning")
     */
    fun getEffectiveIcon(): String {
        return icon ?: "warning"
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Warning"
        val dismissInfo = if (dismissible) ", dismissible" else ""
        val actionsInfo = if (actions.isNotEmpty()) ", ${actions.size} actions available" else ""
        return "$base: $title. $message$dismissInfo$actionsInfo"
    }

    companion object {
        /**
         * Create a simple warning panel
         */
        fun simple(
            title: String,
            message: String
        ) = WarningPanel(
            title = title,
            message = message
        )

        /**
         * Create a dismissible warning panel
         */
        fun dismissible(
            title: String,
            message: String,
            onDismiss: (() -> Unit)? = null
        ) = WarningPanel(
            title = title,
            message = message,
            dismissible = true,
            onDismiss = onDismiss
        )

        /**
         * Create a warning panel with actions
         */
        fun withActions(
            title: String,
            message: String,
            actions: List<Action>
        ) = WarningPanel(
            title = title,
            message = message,
            actions = actions
        )
    }
}
