package com.augmentalis.avamagic.renderer.android.mappers.foundation

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.augmentalis.avamagic.dsl.SwitchComponent
import com.augmentalis.avamagic.renderer.android.ComponentMapper
import com.augmentalis.avamagic.renderer.android.ComposeRenderer
import com.augmentalis.avamagic.renderer.android.ModifierConverter

/**
 * SwitchMapper - Maps SwitchComponent to Material3 Switch
 */
class SwitchMapper : ComponentMapper<SwitchComponent> {
    private val modifierConverter = ModifierConverter()

    @Composable
    override fun map(component: SwitchComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var checked by remember { mutableStateOf(component.checked) }

            Switch(
                checked = checked,
                onCheckedChange = { newValue ->
                    checked = newValue
                    component.onCheckedChange?.invoke(newValue)
                },
                modifier = modifierConverter.convert(component.modifiers),
                enabled = component.enabled
            )
        }
    }
}
