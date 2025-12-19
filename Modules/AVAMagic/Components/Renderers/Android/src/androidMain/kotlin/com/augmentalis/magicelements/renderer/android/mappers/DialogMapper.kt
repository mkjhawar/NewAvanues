package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.augmentalis.avanues.avamagic.ui.core.feedback.DialogComponent
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * DialogMapper - Maps DialogComponent to Material3 AlertDialog
 */
class DialogMapper : ComponentMapper<DialogComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: DialogComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            AlertDialog(
                onDismissRequest = { /* Dialog dismissal is handled by parent state */ },
                title = { Text(component.title) },
                text = { Text(component.content) },
                confirmButton = {
                    TextButton(onClick = { /* Confirm action */ }) {
                        Text(component.confirmLabel)
                    }
                },
                dismissButton = component.cancelLabel?.let { label ->
                    {
                        TextButton(onClick = { /* Cancel action */ }) {
                            Text(label)
                        }
                    }
                },
                modifier = modifierConverter.convert(component.modifiers)
            )
        }
    }
}
