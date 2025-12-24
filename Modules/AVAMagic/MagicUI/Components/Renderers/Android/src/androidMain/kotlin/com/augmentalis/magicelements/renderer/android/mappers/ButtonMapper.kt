package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.augmentalis.magicui.ui.core.form.ButtonComponent
import com.augmentalis.magicui.ui.core.form.ButtonScope
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * ButtonMapper - Maps ButtonComponent to Material3 Button variants
 */
class ButtonMapper : ComponentMapper<ButtonComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ButtonComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val modifier = modifierConverter.convert(component.modifiers)
            val onClick = component.onClick ?: {}

            when (component.buttonStyle) {
                ButtonScope.Filled -> {
                    Button(
                        onClick = onClick,
                        modifier = modifier,
                        enabled = component.enabled
                    ) {
                        Text(component.text)
                    }
                }
                ButtonScope.Outlined -> {
                    OutlinedButton(
                        onClick = onClick,
                        modifier = modifier,
                        enabled = component.enabled
                    ) {
                        Text(component.text)
                    }
                }
                ButtonScope.Text -> {
                    TextButton(
                        onClick = onClick,
                        modifier = modifier,
                        enabled = component.enabled
                    ) {
                        Text(component.text)
                    }
                }
                ButtonScope.Elevated -> {
                    ElevatedButton(
                        onClick = onClick,
                        modifier = modifier,
                        enabled = component.enabled
                    ) {
                        Text(component.text)
                    }
                }
                ButtonScope.Tonal -> {
                    FilledTonalButton(
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
