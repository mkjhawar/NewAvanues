package com.augmentalis.avanues.avamagic.ui.core.feedback

import com.augmentalis.avanues.avamagic.components.core.*

/**
 * Alert Component
 *
 * An alert banner component for displaying important messages.
 *
 * Features:
 * - Severity-based styling (info/success/warning/error)
 * - Title and message content
 * - Optional icon
 * - Dismissible with close button
 * - Full-width or inline display
 * - Customizable colors per severity
 *
 * Platform mappings:
 * - Android: MaterialBanner or custom alert
 * - iOS: Custom alert banner
 * - Web: Alert element
 *
 * Usage:
 * ```kotlin
 * Alert(
 *     title = "Warning",
 *     message = "Your session will expire in 5 minutes",
 *     severity = AlertSeverity.Warning,
 *     dismissible = true,
 *     icon = "warning",
 *     onDismiss = { hideAlert() }
 * )
 * ```
 */
data class AlertComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val title: String,
    val message: String,
    val severity: AlertSeverity = AlertSeverity.Info,
    val variant: AlertVariant = AlertVariant.Standard, // For mapper compatibility
    val dismissible: Boolean = true,
    val closeable: Boolean = true, // For mapper compatibility
    val icon: String? = null,
    val actions: List<AlertAction> = emptyList(), // For mapper compatibility
    val onDismiss: (() -> Unit)? = null,
    val onClose: (() -> Unit)? = null // For mapper compatibility
) : Component {
    init {
        require(title.isNotBlank()) { "Alert title cannot be blank" }
        require(message.isNotBlank()) { "Alert message cannot be blank" }
    }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

data class AlertAction(
    val label: String,
    val onClick: () -> Unit
)

enum class AlertVariant {
    Standard,
    Filled,
    Outlined
}

/**
 * Alert severity levels
 */
enum class AlertSeverity {
    Success,
    Info,
    Warning,
    Error
}
