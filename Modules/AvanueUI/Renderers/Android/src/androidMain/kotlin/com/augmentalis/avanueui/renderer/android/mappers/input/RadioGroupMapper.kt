package com.augmentalis.avanueui.renderer.android.mappers.input

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.ui.core.form.RadioGroupComponent
import com.augmentalis.avanueui.ui.core.form.RadioComponent
import com.augmentalis.avanueui.core.Orientation
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.ModifierConverter

/**
 * RadioGroupMapper - Maps RadioGroupComponent to Material3 RadioButtons
 */
class RadioGroupMapper : ComponentMapper<RadioGroupComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: RadioGroupComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Column(modifier = modifierConverter.convert(component.modifiers)) {
                if (component.orientation == Orientation.Vertical) {
                    Column {
                        component.options.forEach { (value, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = value == component.selectedValue,
                                        onClick = { /* No callback in component */ },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = value == component.selectedValue,
                                    onClick = null,
                                    enabled = component.enabled
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(label)
                            }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        component.options.forEach { (value, label) ->
                            Row(
                                modifier = Modifier.selectable(
                                    selected = value == component.selectedValue,
                                    onClick = { /* No callback in component */ },
                                    role = Role.RadioButton
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = value == component.selectedValue,
                                    onClick = null,
                                    enabled = component.enabled
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(label)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * RadioMapper - Maps individual RadioComponent to Material3 RadioButton
 */
class RadioMapper : ComponentMapper<RadioComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: RadioComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Row(
                modifier = modifierConverter.convert(component.modifiers)
                    .selectable(
                        selected = component.selected,
                        onClick = { component.onSelectedChange?.invoke(!component.selected) },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = component.selected,
                    onClick = { component.onSelectedChange?.invoke(!component.selected) },
                    enabled = component.enabled
                )
                component.label?.let { label ->
                    Spacer(Modifier.width(8.dp))
                    Text(label)
                }
            }
        }
    }
}
