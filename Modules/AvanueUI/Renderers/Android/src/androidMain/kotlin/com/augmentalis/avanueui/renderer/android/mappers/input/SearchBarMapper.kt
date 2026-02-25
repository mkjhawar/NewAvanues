package com.augmentalis.avanueui.renderer.android.mappers.input

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.augmentalis.avanueui.ui.core.form.SearchBarComponent
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.ModifierConverter

/**
 * SearchBarMapper - Maps SearchBarComponent to Material3 SearchBar
 */
class SearchBarMapper : ComponentMapper<SearchBarComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: SearchBarComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            OutlinedTextField(
                value = component.value,
                onValueChange = { component.onValueChange?.invoke(it) },
                placeholder = { Text(component.placeholder) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (component.value.isNotEmpty() && component.showClearButton) {
                        IconButton(onClick = { component.onValueChange?.invoke("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { component.onSearch?.invoke(component.value) }
                ),
                modifier = modifierConverter.convert(component.modifiers).fillMaxWidth()
            )
        }
    }
}
