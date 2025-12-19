package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.avanues.avamagic.ui.core.layout.ContainerComponent
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * ContainerMapper - Maps ContainerComponent to Jetpack Compose Box
 */
class ContainerMapper : ComponentMapper<ContainerComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ContainerComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Box(
                modifier = modifierConverter.convert(component.modifiers),
                contentAlignment = modifierConverter.toComposeAlignment(component.contentAlignment)
            ) {
                component.child?.let { child ->
                    renderer.RenderComponent(child)
                }
            }
        }
    }
}
