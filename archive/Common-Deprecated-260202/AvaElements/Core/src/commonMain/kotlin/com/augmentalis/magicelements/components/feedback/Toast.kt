package com.augmentalis.avaelements.components.feedback

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

/**
 * Toast Component
 *
 * A temporary notification message component.
 *
 * Features:
 * - Temporary display with auto-dismiss
 * - Configurable duration
 * - Position control (top/bottom, left/center/right)
 * - Severity levels (success/info/warning/error)
 * - Optional action button
 * - Queue management for multiple toasts
 * - Swipe to dismiss
 *
 * Platform mappings:
 * - Android: Toast or Snackbar
 * - iOS: Custom toast view
 * - Web: Toast notification
 *
 * Usage:
 * ```kotlin
 * Toast(
 *     message = "Operation successful!",
 *     duration = 3000,
 *     severity = ToastSeverity.Success,
 *     position = ToastPosition.BottomCenter,
 *     action = ToastAction("Undo") { undoOperation() }
 * )
 * ```
 */
data class ToastComponent(
    override val type: String = "Toast",
    val message: String,
    val duration: Long = 3000,
    val severity: ToastSeverity = ToastSeverity.Info,
    val position: ToastPosition = ToastPosition.BottomCenter,
    val action: ToastAction? = null,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    init {
        require(message.isNotBlank()) { "Toast message cannot be blank" }
        require(duration > 0) { "Toast duration must be positive" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * Toast severity levels
 */
enum class ToastSeverity {
    Success,
    Info,
    Warning,
    Error
}

/**
 * Toast position on screen
 */
enum class ToastPosition {
    TopLeft,
    TopCenter,
    TopRight,
    BottomLeft,
    BottomCenter,
    BottomRight
}

/**
 * Toast action button
 */
data class ToastAction(
    val label: String,
    val onClick: () -> Unit
) {
    init {
        require(label.isNotBlank()) { "Toast action label cannot be blank" }
    }
}
