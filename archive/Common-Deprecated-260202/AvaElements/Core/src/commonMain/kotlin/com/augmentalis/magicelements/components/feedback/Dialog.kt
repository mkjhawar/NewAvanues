package com.augmentalis.avaelements.components.feedback

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

/**
 * Dialog Component
 *
 * A modal dialog component for displaying content and actions.
 *
 * Features:
 * - Modal overlay with backdrop
 * - Title and content areas
 * - Action buttons (confirm/cancel)
 * - Dismissible on backdrop click
 * - Keyboard navigation (ESC to close)
 * - Custom content support
 * - Animation transitions
 *
 * Platform mappings:
 * - Android: AlertDialog or MaterialDialog
 * - iOS: UIAlertController or custom modal
 * - Web: Modal dialog element
 *
 * Usage:
 * ```kotlin
 * Dialog(
 *     isOpen = true,
 *     title = "Confirm Action",
 *     content = TextComponent("Are you sure?"),
 *     actions = listOf(
 *         DialogAction("Cancel", DialogActionStyle.Text) { },
 *         DialogAction("Confirm", DialogActionStyle.Primary) { }
 *     ),
 *     dismissible = true,
 *     onDismiss = { }
 * )
 * ```
 */
data class DialogComponent(
    override val type: String = "Dialog",
    val isOpen: Boolean = false,
    val title: String? = null,
    val content: Component? = null,
    val actions: List<DialogAction> = emptyList(),
    val dismissible: Boolean = true,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onDismiss: (() -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * Dialog action button configuration
 */
data class DialogAction(
    val label: String,
    val style: DialogActionStyle = DialogActionStyle.Text,
    val onClick: () -> Unit
) {
    init {
        require(label.isNotBlank()) { "Dialog action label cannot be blank" }
    }
}

/**
 * Dialog action button styles
 */
enum class DialogActionStyle {
    Primary,
    Secondary,
    Text,
    Outlined
}
