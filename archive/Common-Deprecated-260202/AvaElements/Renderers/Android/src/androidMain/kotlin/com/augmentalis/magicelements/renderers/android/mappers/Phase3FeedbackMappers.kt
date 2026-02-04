package com.augmentalis.avaelements.renderers.android.mappers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.core.Theme
import com.augmentalis.avaelements.components.phase3.feedback.*

// ============================================================================
// PHASE 3 FEEDBACK MAPPERS
// ============================================================================
// Material3 implementations for feedback/notification components
// Last updated: 2025-11-13
// ============================================================================

/**
 * Alert - Simple alert message with severity indicator
 *
 * Material3: Card with colored border based on severity
 *
 * Props:
 * - message: String - Alert message
 * - severity: String - "info", "success", "warning", "error" (default: "info")
 * - onClose: (() -> Unit)? - Optional close handler
 */
@Composable
fun RenderAlert(c: Alert, theme: Theme) {
    val backgroundColor = when (c.severity) {
        "error" -> theme.colorScheme.errorContainer.toCompose()
        "warning" -> androidx.compose.ui.graphics.Color(0xFFFFF4E5) // Light orange
        "success" -> androidx.compose.ui.graphics.Color(0xFFE8F5E9) // Light green
        else -> theme.colorScheme.surfaceVariant.toCompose()
    }

    val contentColor = when (c.severity) {
        "error" -> theme.colorScheme.onErrorContainer.toCompose()
        "warning" -> androidx.compose.ui.graphics.Color(0xFFE65100) // Dark orange
        "success" -> androidx.compose.ui.graphics.Color(0xFF2E7D32) // Dark green
        else -> theme.colorScheme.onSurfaceVariant.toCompose()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = c.message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )

            c.onClose?.let { onClose ->
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close alert"
                    )
                }
            }
        }
    }
}

/**
 * Toast - Temporary notification message
 *
 * Material3: Snackbar styled as toast (simplified)
 * Note: In production, would use SnackbarHost with duration management
 *
 * Props:
 * - message: String - Toast message
 * - position: String - "top" or "bottom" (default: "bottom")
 */
@Composable
fun RenderToast(c: Toast, theme: Theme) {
    // Simple toast implementation using Snackbar
    // In production: Use SnackbarHost with coroutine-based dismissal
    Snackbar(
        modifier = Modifier.padding(16.dp),
        containerColor = (theme.colorScheme.inverseSurface ?: theme.colorScheme.surface).toCompose(),
        contentColor = (theme.colorScheme.inverseOnSurface ?: theme.colorScheme.onSurface).toCompose()
    ) {
        Text(text = c.message)
    }
}

/**
 * Snackbar - Material snackbar with optional action
 *
 * Material3: Snackbar
 *
 * Props:
 * - message: String - Snackbar message
 * - duration: Int - Display duration in ms (default: 3000)
 * - onDismiss: (() -> Unit)? - Dismiss handler
 */
@Composable
fun RenderSnackbar(c: Snackbar, theme: Theme) {
    androidx.compose.material3.Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            c.onDismiss?.let { onDismiss ->
                TextButton(onClick = onDismiss) {
                    Text(text = "Dismiss")
                }
            }
        },
        containerColor = (theme.colorScheme.inverseSurface ?: theme.colorScheme.surface).toCompose(),
        contentColor = (theme.colorScheme.inverseOnSurface ?: theme.colorScheme.onSurface).toCompose()
    ) {
        Text(text = c.message)
    }
}

/**
 * Modal - Full-screen modal dialog
 *
 * Material3: AlertDialog (basic implementation)
 * Note: Could use ModalBottomSheet for more advanced modal behavior
 *
 * Props:
 * - open: Boolean - Modal visibility state
 * - title: String? - Optional modal title
 * - onClose: (() -> Unit)? - Close handler
 */
@Composable
fun RenderModal(c: Modal, theme: Theme) {
    if (c.open) {
        AlertDialog(
            onDismissRequest = c.onClose ?: {},
            title = c.title?.let { title ->
                { Text(text = title, style = MaterialTheme.typography.titleLarge) }
            },
            text = {
                Text(text = "Modal content placeholder")
            },
            confirmButton = {
                TextButton(onClick = c.onClose ?: {}) {
                    Text(text = "Close")
                }
            },
            containerColor = theme.colorScheme.surface.toCompose(),
            titleContentColor = theme.colorScheme.onSurface.toCompose(),
            textContentColor = theme.colorScheme.onSurfaceVariant.toCompose()
        )
    }
}

/**
 * Confirm - Confirmation dialog with confirm/cancel actions
 *
 * Material3: AlertDialog
 *
 * Props:
 * - message: String - Confirmation message
 * - confirmText: String - Confirm button text (default: "OK")
 * - cancelText: String - Cancel button text (default: "Cancel")
 * - onConfirm: (() -> Unit)? - Confirm handler
 * - onCancel: (() -> Unit)? - Cancel handler
 */
@Composable
fun RenderConfirm(c: Confirm, theme: Theme) {
    AlertDialog(
        onDismissRequest = c.onCancel ?: {},
        title = {
            Text(text = "Confirm", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(text = c.message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            TextButton(onClick = c.onConfirm ?: {}) {
                Text(text = c.confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = c.onCancel ?: {}) {
                Text(text = c.cancelText)
            }
        },
        containerColor = theme.colorScheme.surface.toCompose(),
        titleContentColor = theme.colorScheme.onSurface.toCompose(),
        textContentColor = theme.colorScheme.onSurfaceVariant.toCompose()
    )
}

/**
 * ContextMenu - Right-click context menu
 *
 * Material3: DropdownMenu
 *
 * Props:
 * - items: List<String> - Menu item labels
 * - onItemClick: ((Int) -> Unit)? - Item click handler (receives index)
 */
@Composable
fun RenderContextMenu(c: ContextMenu, theme: Theme) {
    var expanded by remember { mutableStateOf(false) }

    // In production: This would be triggered by long-press or right-click
    // For now, showing as expandable dropdown
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(text = "Menu")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            c.items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        expanded = false
                        c.onItemClick?.invoke(index)
                    }
                )
            }
        }
    }
}
