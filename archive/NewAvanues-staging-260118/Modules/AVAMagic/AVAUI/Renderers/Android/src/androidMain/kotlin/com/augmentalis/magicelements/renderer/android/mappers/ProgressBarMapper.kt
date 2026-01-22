package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.avamagic.ui.core.feedback.ProgressBarComponent
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter
import com.augmentalis.avaelements.renderer.android.toComposeColor

/**
 * ProgressBarMapper - Maps ProgressBarComponent to Material3 LinearProgressIndicator
 */
class ProgressBarMapper : ComponentMapper<ProgressBarComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ProgressBarComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            if (component.indeterminate) {
                LinearProgressIndicator(
                    modifier = modifierConverter.convert(component.modifiers).fillMaxWidth(),
                    color = component.color.toComposeColor()
                )
            } else {
                val progress = component.value / component.max
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = modifierConverter.convert(component.modifiers).fillMaxWidth(),
                    color = component.color.toComposeColor()
                )
            }
        }
    }
}
