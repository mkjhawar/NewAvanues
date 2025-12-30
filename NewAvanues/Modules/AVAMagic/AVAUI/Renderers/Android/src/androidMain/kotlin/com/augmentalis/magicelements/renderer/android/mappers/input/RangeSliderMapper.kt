package com.augmentalis.avaelements.renderer.android.mappers.input

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avamagic.ui.core.form.RangeSliderComponent
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * RangeSliderMapper - Maps RangeSliderComponent to Material3 RangeSlider
 */
class RangeSliderMapper : ComponentMapper<RangeSliderComponent> {
    private val modifierConverter = ModifierConverter()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun map(component: RangeSliderComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Column(modifier = modifierConverter.convert(component.modifiers)) {
                if (component.label != null || component.showValues) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        component.label?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        if (component.showValues) {
                            Text(
                                text = "%.1f - %.1f".format(component.minValue, component.maxValue),
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

                RangeSlider(
                    value = component.minValue..component.maxValue,
                    onValueChange = { /* Component uses updateMin/updateMax methods */ },
                    valueRange = component.min..component.max,
                    steps = if (stepsCount > 0) stepsCount else 0,
                    enabled = component.enabled
                )
            }
        }
    }
}
