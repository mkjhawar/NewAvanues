package com.augmentalis.avanueui.renderer.android.mappers

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.avanueui.ui.core.display.BadgeComponent
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.ModifierConverter

/**
 * BadgeMapper - Maps BadgeComponent to Material3 Badge
 */
class BadgeMapper : ComponentMapper<BadgeComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: BadgeComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val modifier = modifierConverter.convert(component.modifiers)

            Badge(
                modifier = modifier
            ) {
                if (component.content.isNotBlank()) {
                    Text(text = component.content)
                }
            }
        }
    }
}
