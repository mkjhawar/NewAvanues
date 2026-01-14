package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.augmentalis.magicui.ui.core.feedback.SnackbarComponent
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * SnackbarMapper - Maps SnackbarComponent to Material3 Snackbar
 */
class SnackbarMapper : ComponentMapper<SnackbarComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: SnackbarComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Snackbar(
                modifier = modifierConverter.convert(component.modifiers),
                action = component.actionLabel?.let { label ->
                    {
                        TextButton(onClick = { /* Action handler */ }) {
                            Text(label)
                        }
                    }
                }
            ) {
                Text(component.message)
            }
        }
    }
}
