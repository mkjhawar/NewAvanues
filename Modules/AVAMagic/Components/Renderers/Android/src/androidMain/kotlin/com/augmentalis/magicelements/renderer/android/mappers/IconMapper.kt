package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.augmentalis.avanues.avamagic.ui.core.display.IconComponent
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.IconResolver
import com.augmentalis.avaelements.renderer.android.ModifierConverter
import com.augmentalis.avaelements.renderer.android.toComposeColor

/**
 * IconMapper - Maps IconComponent to Material3 Icon
 * Uses centralized IconResolver for icon resolution
 */
class IconMapper : ComponentMapper<IconComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: IconComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Icon(
                imageVector = IconResolver.resolve(component.icon),
                contentDescription = component.contentDescription,
                modifier = modifierConverter.convert(component.modifiers),
                tint = component.color.toComposeColor()
            )
        }
    }
}
