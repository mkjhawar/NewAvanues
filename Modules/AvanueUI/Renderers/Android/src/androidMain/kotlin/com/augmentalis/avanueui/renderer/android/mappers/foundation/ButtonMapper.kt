package com.augmentalis.avanueui.renderer.android.mappers.foundation

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.avanueui.dsl.ButtonComponent
import com.augmentalis.avanueui.dsl.ButtonScope
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.ModifierConverter

/**
 * ButtonMapper - Maps ButtonComponent to Material3 Button variants
 */
class ButtonMapper : ComponentMapper<ButtonComponent> {
    private val modifierConverter = ModifierConverter()

    @Composable
    override fun map(component: ButtonComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val modifier = modifierConverter.convert(component.modifiers)
            val onClick = component.onClick ?: {}

            when (component.buttonStyle) {
                ButtonScope.ButtonStyle.Primary -> {
                    Button(
                        onClick = onClick,
                        modifier = modifier,
                        enabled = component.enabled
                    ) {
                        Text(component.text)
                    }
                }
                ButtonScope.ButtonStyle.Secondary -> {
                    FilledTonalButton(
                        onClick = onClick,
                        modifier = modifier,
                        enabled = component.enabled
                    ) {
                        Text(component.text)
                    }
                }
                ButtonScope.ButtonStyle.Tertiary -> {
                    FilledTonalButton(
                        onClick = onClick,
                        modifier = modifier,
                        enabled = component.enabled,
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Text(component.text)
                    }
                }
                ButtonScope.ButtonStyle.Text -> {
                    TextButton(
                        onClick = onClick,
                        modifier = modifier,
                        enabled = component.enabled
                    ) {
                        Text(component.text)
                    }
                }
                ButtonScope.ButtonStyle.Outlined -> {
                    OutlinedButton(
                        onClick = onClick,
                        modifier = modifier,
                        enabled = component.enabled
                    ) {
                        Text(component.text)
                    }
                }
            }
        }
    }
}
