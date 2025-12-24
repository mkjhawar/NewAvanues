package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.augmentalis.magicui.ui.core.form.TextFieldComponent
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * TextFieldMapper - Maps TextFieldComponent to Material3 OutlinedTextField
 */
class TextFieldMapper : ComponentMapper<TextFieldComponent> {
    private val modifierConverter = ModifierConverter()

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
                placeholder = component.placeholder?.let { { Text(it) } },
                isError = component.isError,
                supportingText = if (component.isError && component.errorMessage != null) {
                    { Text(component.errorMessage!!) }
                } else null,
                singleLine = component.maxLength != null
            )
        }
    }
}
