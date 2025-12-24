package com.augmentalis.avaelements.phase3

import com.augmentalis.avaelements.core.*

/**
 * Phase 3 Feedback Components - Common Interface
 * 6 feedback components for user notifications and confirmations
 */

/**
 * Alert component for important messages
 */
data class Alert(
    val id: String,
    val title: String? = null,
    val message: String,
    val severity: AlertSeverity = AlertSeverity.Info,
    val variant: AlertVariant = AlertVariant.Filled,
    val icon: String? = null,
    val closeable: Boolean = true,
    val actions: List<AlertAction> = emptyList(),
    val onClose: (() -> Unit)? = null
) : Component

/**
 * Snackbar component for brief notifications
 */
data class Snackbar(
    val id: String,
    val message: String,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val action: SnackbarAction? = null,
    val onDismiss: (() -> Unit)? = null
) : Component

/**
 * Modal component for dialog overlays
 */
data class Modal(
    val id: String,
    val open: Boolean = false,
    val title: String? = null,
    val content: Component? = null,
    val actions: List<ModalAction> = emptyList(),
    val closeable: Boolean = true,
    val size: ModalSize = ModalSize.Medium,
    val onClose: (() -> Unit)? = null
) : Component

/**
 * Toast component for temporary messages
 */
data class Toast(
    val id: String,
    val message: String,
    val type: ToastType = ToastType.Info,
    val position: ToastPosition = ToastPosition.BottomCenter,
    val duration: Long = 3000, // milliseconds
    val icon: String? = null,
    val onDismiss: (() -> Unit)? = null
) : Component

/**
 * Confirm component for confirmation dialogs
 */
data class Confirm(
    val id: String,
    val open: Boolean = false,
    val title: String,
    val message: String,
    val confirmText: String = "Confirm",
    val cancelText: String = "Cancel",
    val severity: ConfirmSeverity = ConfirmSeverity.Warning,
    val onConfirm: (() -> Unit)? = null,
    val onCancel: (() -> Unit)? = null
) : Component

/**
 * ContextMenu component for right-click menus
 */
data class ContextMenu(
    val id: String,
    val items: List<ContextMenuItem>,
    val open: Boolean = false,
    val position: MenuPosition? = null,
    val onClose: (() -> Unit)? = null
) : Component

// Supporting enums and data classes

/**
 * Alert severity levels
 */
enum class AlertSeverity {
    Info,
    Success,
    Warning,
    Error
}

/**
 * Alert variant
 */
enum class AlertVariant {
    Filled,
    Outlined,
    Standard
}

/**
 * Alert action
 */
data class AlertAction(
    val id: String,
    val label: String,
    val onClick: (() -> Unit)? = null
)

/**
 * Snackbar duration
 */
enum class SnackbarDuration {
    Short,      // 4 seconds
    Long,       // 10 seconds
    Indefinite  // Manual dismiss only
}

/**
 * Snackbar action
 */
data class SnackbarAction(
    val label: String,
    val onClick: (() -> Unit)? = null
)

/**
 * Modal action
 */
data class ModalAction(
    val id: String,
    val label: String,
    val variant: ModalActionVariant = ModalActionVariant.Text,
    val onClick: (() -> Unit)? = null
)

/**
 * Modal action variant
 */
enum class ModalActionVariant {
    Text,
    Outlined,
    Filled
}

/**
 * Modal size
 */
enum class ModalSize {
    Small,      // 400dp
    Medium,     // 600dp
    Large,      // 900dp
    ExtraLarge, // 1200dp
    FullScreen  // Full screen
}

/**
 * Toast type
 */
enum class ToastType {
    Info,
    Success,
    Warning,
    Error
}

/**
 * Toast position
 */
enum class ToastPosition {
    TopStart,
    TopCenter,
    TopEnd,
    BottomStart,
    BottomCenter,
    BottomEnd
}

/**
 * Confirmation severity
 */
enum class ConfirmSeverity {
    Info,
    Warning,
    Danger
}

/**
 * Context menu item
 */
data class ContextMenuItem(
    val id: String,
    val label: String,
    val icon: String? = null,
    val enabled: Boolean = true,
    val divider: Boolean = false,
    val onClick: (() -> Unit)? = null
)

/**
 * Menu position coordinates
 */
data class MenuPosition(
    val x: Float,
    val y: Float
)
