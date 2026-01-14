package com.augmentalis.avamagic.renderer.android.mappers.foundation

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.avamagic.dsl.RowComponent
import com.augmentalis.avamagic.renderer.android.ComponentMapper
import com.augmentalis.avamagic.renderer.android.ComposeRenderer
import com.augmentalis.avamagic.renderer.android.ModifierConverter

/**
 * RowMapper - Maps RowComponent to Jetpack Compose Row
 */
class RowMapper : ComponentMapper<RowComponent> {
    private val modifierConverter = ModifierConverter()

    @Composable
    override fun map(component: RowComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Row(
                modifier = modifierConverter.convert(component.modifiers),
                horizontalArrangement = modifierConverter.toComposeArrangement(component.arrangement),
                verticalAlignment = when (component.verticalAlignment) {
                    com.augmentalis.avamagic.core.Alignment.TopStart,
                    com.augmentalis.avamagic.core.Alignment.TopCenter,
                    com.augmentalis.avamagic.core.Alignment.TopEnd ->
                        androidx.compose.ui.Alignment.Top

                    com.augmentalis.avamagic.core.Alignment.CenterStart,
                    com.augmentalis.avamagic.core.Alignment.Center,
                    com.augmentalis.avamagic.core.Alignment.CenterEnd ->
                        androidx.compose.ui.Alignment.CenterVertically

                    com.augmentalis.avamagic.core.Alignment.BottomStart,
                    com.augmentalis.avamagic.core.Alignment.BottomCenter,
                    com.augmentalis.avamagic.core.Alignment.BottomEnd ->
                        androidx.compose.ui.Alignment.Bottom
                }
            ) {
                component.children.forEach { child ->
                    renderer.RenderComponent(child)
                }
            }
        }
    }
}
