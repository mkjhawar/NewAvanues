package com.augmentalis.avamagic.renderer.android.mappers.input

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avamagic.ui.core.form.SliderComponent
import com.augmentalis.avamagic.renderer.android.ComponentMapper
import com.augmentalis.avamagic.renderer.android.ComposeRenderer
import com.augmentalis.avamagic.renderer.android.ModifierConverter

/**
 * SliderMapper - Maps SliderComponent to Material3 Slider
 */
class SliderMapper : ComponentMapper<SliderComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: SliderComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Column(modifier = modifierConverter.convert(component.modifiers)) {
                if (component.label != null || component.showValue) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        component.label?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        if (component.showValue) {
                            Text(
                                text = "%.1f".format(component.value),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Calculate steps from step size
                val stepsCount = if (component.step > 0) {
                    ((component.max - component.min) / component.step).toInt() - 1
                } else {
                    0
                }

                Slider(
                    value = component.value,
                    onValueChange = { component.onValueChange?.invoke(it) },
                    valueRange = component.min..component.max,
                    steps = if (stepsCount > 0) stepsCount else 0,
                    enabled = component.enabled
                )
            }
        }
    }
}
