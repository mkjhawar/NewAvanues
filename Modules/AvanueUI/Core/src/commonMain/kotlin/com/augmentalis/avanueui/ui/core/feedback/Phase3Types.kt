package com.augmentalis.avanueui.ui.core.feedback

import com.augmentalis.avanueui.core.*

// Type aliases for mappers that expect shorter names
typealias Toast = ToastComponent
typealias Snackbar = SnackbarComponent
typealias Alert = AlertComponent

// Toast-specific enums expected by mappers
enum class ToastType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

enum class ToastPosition {
    TOP_START,
    TOP_CENTER,
    TOP_END,
    BOTTOM_START,
    BOTTOM_CENTER,
    BOTTOM_END
}

// Snackbar-specific enums
enum class SnackbarDuration {
    SHORT,
    LONG,
    INDEFINITE
}

// Modal component and enums
data class Modal(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val title: String,
    val content: Component? = null,
    val actions: List<ModalAction> = emptyList(),
    val size: ModalSize = ModalSize.MEDIUM,
    val dismissible: Boolean = true,
    val onDismiss: (() -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

data class ModalAction(
    val label: String,
    val variant: ModalActionVariant = ModalActionVariant.TEXT,
    val onClick: () -> Unit
)

enum class ModalSize {
    SMALL,
    MEDIUM,
    LARGE,
    FULL_WIDTH,
    FULL_SCREEN
}

enum class ModalActionVariant {
    TEXT,
    OUTLINED,
    FILLED
}

// Confirm dialog
data class Confirm(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val title: String,
    val message: String,
    val confirmLabel: String = "Confirm",
    val cancelLabel: String = "Cancel",
    val severity: ConfirmSeverity = ConfirmSeverity.INFO,
    val onConfirm: (() -> Unit)? = null,
    val onCancel: (() -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

enum class ConfirmSeverity {
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}

// Context menu
data class ContextMenu(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val items: List<ContextMenuItem>,
    val anchor: Component? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

data class ContextMenuItem(
    val label: String,
    val icon: String? = null,
    val disabled: Boolean = false,
    val divider: Boolean = false,
    val onClick: (() -> Unit)? = null
)
