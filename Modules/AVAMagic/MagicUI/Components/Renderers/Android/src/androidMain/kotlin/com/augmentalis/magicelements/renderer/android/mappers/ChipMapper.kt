package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.magicui.ui.core.display.ChipComponent
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * ChipMapper - Maps ChipComponent to Material3 Chip variants
 */
class ChipMapper : ComponentMapper<ChipComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ChipComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val modifier = modifierConverter.convert(component.modifiers)

            if (component.deletable) {
                InputChip(
                    selected = component.selected,
                    onClick = { /* no onClick on ChipComponent */ },
                    label = { Text(component.label) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete",
                            modifier = Modifier
                        )
                    },
                    modifier = modifier
                )
            } else {
                FilterChip(
                    selected = component.selected,
                    onClick = { /* no onClick on ChipComponent */ },
                    label = { Text(component.label) },
                    modifier = modifier
                )
            }
        }
    }
}
