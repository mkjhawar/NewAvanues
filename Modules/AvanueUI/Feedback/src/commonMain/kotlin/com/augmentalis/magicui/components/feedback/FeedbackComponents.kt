package com.augmentalis.avanueui.feedback

import kotlinx.serialization.Serializable

/**
 * MagicUI Feedback Components
 *
 * 6 feedback components for user notifications and confirmations
 */

/**
 * Alert component for important messages
 */
@Serializable
data class Alert(
    val id: String,
    val title: String? = null,
    val message: String,
    val severity: AlertSeverity = AlertSeverity.Info,
    val variant: AlertVariant = AlertVariant.Filled,
    val icon: String? = null,
    val closeable: Boolean = true,
    val actions: List<AlertAction> = emptyList()
)

/**
 * Snackbar component for brief notifications
 */
@Serializable
data class Snackbar(
    val id: String,
    val message: String,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val action: SnackbarAction? = null
)

/**
 * Modal component for dialog overlays
 */
@Serializable
data class Modal(
    val id: String,
    val open: Boolean = false,
    val title: String? = null,
    val contentId: String? = null,
    val actions: List<ModalAction> = emptyList(),
    val closeable: Boolean = true,
    val size: ModalSize = ModalSize.Medium
)

/**
 * Toast component for temporary messages
 */
@Serializable
data class Toast(
    val id: String,
    val message: String,
    val type: ToastType = ToastType.Info,
    val position: ToastPosition = ToastPosition.BottomCenter,
    val duration: Long = 3000,
    val icon: String? = null
)

/**
 * Confirm component for confirmation dialogs
 */
@Serializable
data class Confirm(
    val id: String,
    val open: Boolean = false,
    val title: String,
    val message: String,
    val confirmText: String = "Confirm",
    val cancelText: String = "Cancel",
    val severity: ConfirmSeverity = ConfirmSeverity.Warning
)

/**
 * ContextMenu component for right-click menus
 */
@Serializable
data class ContextMenu(
    val id: String,
    val items: List<ContextMenuItem>,
    val open: Boolean = false,
    val position: MenuPosition? = null
)

// Supporting enums and data classes

@Serializable
enum class AlertSeverity {
    Info,
    Success,
    Warning,
    Error
}

@Serializable
enum class AlertVariant {
    Filled,
    Outlined,
    Standard
}

@Serializable
data class AlertAction(
    val id: String,
    val label: String
)

@Serializable
enum class SnackbarDuration {
    Short,
    Long,
    Indefinite
}

@Serializable
data class SnackbarAction(
    val label: String
)

@Serializable
data class ModalAction(
    val id: String,
    val label: String,
    val variant: ModalActionVariant = ModalActionVariant.Text
)

@Serializable
enum class ModalActionVariant {
    Text,
    Outlined,
    Filled
}

@Serializable
enum class ModalSize {
    Small,
    Medium,
    Large,
    ExtraLarge,
    FullScreen
}

@Serializable
enum class ToastType {
    Info,
    Success,
    Warning,
    Error
}

@Serializable
enum class ToastPosition {
    TopStart,
    TopCenter,
    TopEnd,
    BottomStart,
    BottomCenter,
    BottomEnd
}

@Serializable
enum class ConfirmSeverity {
    Info,
    Warning,
    Danger
}

@Serializable
data class ContextMenuItem(
    val id: String,
    val label: String,
    val icon: String? = null,
    val enabled: Boolean = true,
    val divider: Boolean = false
)

@Serializable
data class MenuPosition(
    val x: Float,
    val y: Float
)
