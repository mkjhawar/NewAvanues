package com.augmentalis.avaelements.renderer.android.mappers.input

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.augmentalis.avanues.avamagic.ui.core.form.DropdownComponent
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * DropdownMapper - Maps DropdownComponent to Material3 ExposedDropdownMenu
 */
class DropdownMapper : ComponentMapper<DropdownComponent> {
    private val modifierConverter = ModifierConverter()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun map(component: DropdownComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var expanded by remember { mutableStateOf(false) }
            val selectedOption = component.options.find { it.value == component.selectedValue }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (component.enabled) expanded = !expanded },
                modifier = modifierConverter.convert(component.modifiers)
            ) {
                OutlinedTextField(
                    value = selectedOption?.label ?: "",
                    onValueChange = {},
                    readOnly = true,
                    enabled = component.enabled,
                    label = component.label?.let { { Text(it) } },
                    placeholder = { Text(component.placeholder) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    component.options.forEachIndexed { index, option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                component.onValueChange?.invoke(option.value)
                                component.onSelectionChanged?.invoke(index)
                                expanded = false
                            },
                            enabled = option.enabled
                        )
                    }
                }
            }
        }
    }
}
