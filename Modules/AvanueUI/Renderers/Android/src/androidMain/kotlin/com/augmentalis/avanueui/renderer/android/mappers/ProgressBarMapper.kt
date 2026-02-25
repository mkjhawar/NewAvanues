package com.augmentalis.avanueui.renderer.android.mappers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.avanueui.ui.core.feedback.ProgressBarComponent
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.ModifierConverter
import com.augmentalis.avanueui.renderer.android.toComposeColor

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
