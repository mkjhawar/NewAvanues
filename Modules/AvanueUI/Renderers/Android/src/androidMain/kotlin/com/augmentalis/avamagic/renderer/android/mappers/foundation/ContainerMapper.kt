package com.augmentalis.avamagic.renderer.android.mappers.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.avamagic.dsl.ContainerComponent
import com.augmentalis.avamagic.renderer.android.ComponentMapper
import com.augmentalis.avamagic.renderer.android.ComposeRenderer
import com.augmentalis.avamagic.renderer.android.ModifierConverter

/**
 * ContainerMapper - Maps ContainerComponent to Jetpack Compose Box
 */
class ContainerMapper : ComponentMapper<ContainerComponent> {
    private val modifierConverter = ModifierConverter()

    @Composable
    override fun map(component: ContainerComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Box(
                modifier = modifierConverter.convert(component.modifiers),
                contentAlignment = modifierConverter.toComposeAlignment(component.alignment)
            ) {
                component.child?.let { child ->
                    renderer.RenderComponent(child)
                }
            }
        }
    }
}
