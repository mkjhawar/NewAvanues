package com.augmentalis.magicelements.renderer.android.mappers

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.magicelements.dsl.RowComponent
import com.augmentalis.magicelements.renderer.android.ComponentMapper
import com.augmentalis.magicelements.renderer.android.ComposeRenderer
import com.augmentalis.magicelements.renderer.android.ModifierConverter

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
                    com.augmentalis.magicelements.core.Alignment.TopStart,
                    com.augmentalis.magicelements.core.Alignment.TopCenter,
                    com.augmentalis.magicelements.core.Alignment.TopEnd ->
                        androidx.compose.ui.Alignment.Top

                    com.augmentalis.magicelements.core.Alignment.CenterStart,
                    com.augmentalis.magicelements.core.Alignment.Center,
                    com.augmentalis.magicelements.core.Alignment.CenterEnd ->
                        androidx.compose.ui.Alignment.CenterVertically

                    com.augmentalis.magicelements.core.Alignment.BottomStart,
                    com.augmentalis.magicelements.core.Alignment.BottomCenter,
                    com.augmentalis.magicelements.core.Alignment.BottomEnd ->
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
