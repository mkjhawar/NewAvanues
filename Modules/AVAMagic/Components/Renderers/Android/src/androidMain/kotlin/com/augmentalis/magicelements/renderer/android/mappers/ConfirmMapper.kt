package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.augmentalis.avanues.avamagic.ui.core.feedback.Confirm
import com.augmentalis.avanues.avamagic.ui.core.feedback.ConfirmSeverity
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * ConfirmMapper - Maps Confirm to Material3 AlertDialog
 */
class ConfirmMapper : ComponentMapper<Confirm> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: Confirm, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val (containerColor, contentColor) = when (component.severity) {
                ConfirmSeverity.INFO -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
                ConfirmSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                ConfirmSeverity.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                ConfirmSeverity.SUCCESS -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            }

            AlertDialog(
                onDismissRequest = { component.onCancel?.invoke() },
                title = {
                    Text(
                        text = component.title,
                        color = contentColor
                    )
                },
                text = {
                    Text(
                        text = component.message,
                        color = contentColor
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { component.onConfirm?.invoke() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (component.severity) {
                                ConfirmSeverity.ERROR -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        Text(component.confirmLabel)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { component.onCancel?.invoke() }) {
                        Text(component.cancelLabel)
                    }
                },
                containerColor = containerColor,
                modifier = modifierConverter.convert(component.modifiers)
            )
        }
    }
}
