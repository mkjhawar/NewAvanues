package com.augmentalis.avamagic.renderer.android.mappers

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.augmentalis.avamagic.ui.core.feedback.Confirm
import com.augmentalis.avamagic.ui.core.feedback.ConfirmSeverity
import com.augmentalis.avamagic.renderer.android.ComponentMapper
import com.augmentalis.avamagic.renderer.android.ComposeRenderer
import com.augmentalis.avamagic.renderer.android.ModifierConverter
import com.augmentalis.avanueui.theme.AvanueTheme

/**
 * ConfirmMapper - Maps Confirm to Material3 AlertDialog
 */
class ConfirmMapper : ComponentMapper<Confirm> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: Confirm, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val (containerColor, contentColor) = when (component.severity) {
                ConfirmSeverity.INFO -> AvanueTheme.colors.surface to AvanueTheme.colors.textPrimary
                ConfirmSeverity.WARNING -> AvanueTheme.colors.tertiaryContainer to AvanueTheme.colors.onTertiaryContainer
                ConfirmSeverity.ERROR -> AvanueTheme.colors.errorContainer to AvanueTheme.colors.onErrorContainer
                ConfirmSeverity.SUCCESS -> AvanueTheme.colors.primaryContainer to AvanueTheme.colors.onPrimaryContainer
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
                                ConfirmSeverity.ERROR -> AvanueTheme.colors.error
                                else -> AvanueTheme.colors.primary
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
