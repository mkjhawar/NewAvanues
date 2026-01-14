package com.augmentalis.magicelements.renderer.android.mappers

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.magicelements.dsl.CheckboxComponent
import com.augmentalis.magicelements.renderer.android.ComponentMapper
import com.augmentalis.magicelements.renderer.android.ComposeRenderer
import com.augmentalis.magicelements.renderer.android.ModifierConverter

/**
 * CheckboxMapper - Maps CheckboxComponent to Material3 Checkbox with label
 */
class CheckboxMapper : ComponentMapper<CheckboxComponent> {
    private val modifierConverter = ModifierConverter()

    @Composable
    override fun map(component: CheckboxComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var checked by remember { mutableStateOf(component.checked) }

            Row(
                modifier = modifierConverter.convert(component.modifiers),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { newValue ->
                        checked = newValue
                        component.onCheckedChange?.invoke(newValue)
                    },
                    enabled = component.enabled
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(component.label)
            }
        }
    }
}
