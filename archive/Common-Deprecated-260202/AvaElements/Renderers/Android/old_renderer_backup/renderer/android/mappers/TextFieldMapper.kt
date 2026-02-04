package com.augmentalis.magicelements.renderer.android.mappers

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.augmentalis.magicelements.dsl.TextFieldComponent
import com.augmentalis.magicelements.renderer.android.ComponentMapper
import com.augmentalis.magicelements.renderer.android.ComposeRenderer
import com.augmentalis.magicelements.renderer.android.ModifierConverter

/**
 * TextFieldMapper - Maps TextFieldComponent to Material3 OutlinedTextField
 */
class TextFieldMapper : ComponentMapper<TextFieldComponent> {
    private val modifierConverter = ModifierConverter()

    @Composable
    override fun map(component: TextFieldComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var value by remember { mutableStateOf(component.value) }

            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    value = newValue
                    component.onValueChange?.invoke(newValue)
                },
                modifier = modifierConverter.convert(component.modifiers),
                enabled = component.enabled,
                readOnly = component.readOnly,
                label = component.label?.let { { Text(it) } },
                placeholder = { Text(component.placeholder) },
                isError = component.isError,
                supportingText = if (component.isError && component.errorMessage != null) {
                    { Text(component.errorMessage!!) }
                } else null,
                singleLine = component.maxLength != null
            )
        }
    }
}
