package com.augmentalis.avaelements.renderer.android.mappers.input

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.augmentalis.avanues.avamagic.ui.core.form.AutocompleteComponent
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * AutocompleteMapper - Maps AutocompleteComponent to Material3 ExposedDropdownMenu with filtering
 */
class AutocompleteMapper : ComponentMapper<AutocompleteComponent> {
    private val modifierConverter = ModifierConverter()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun map(component: AutocompleteComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var expanded by remember { mutableStateOf(false) }

            // Use component's built-in filtered suggestions
            val suggestions = component.filteredSuggestions

            ExposedDropdownMenuBox(
                expanded = expanded && suggestions.isNotEmpty(),
                onExpandedChange = { expanded = it },
                modifier = modifierConverter.convert(component.modifiers)
            ) {
                OutlinedTextField(
                    value = component.value,
                    onValueChange = { /* Component handles this via updateValue */ },
                    enabled = component.enabled,
                    label = component.label?.let { { Text(it) } },
                    placeholder = { Text(component.placeholder) },
                    singleLine = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded && suggestions.isNotEmpty(),
                    onDismissRequest = { expanded = false }
                ) {
                    suggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                // Component uses selectSuggestion method
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
