package com.augmentalis.avanueui.renderer.android.mappers.foundation

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.avanueui.dsl.RowComponent
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.ModifierConverter

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
                    com.augmentalis.avanueui.core.Alignment.TopStart,
                    com.augmentalis.avanueui.core.Alignment.TopCenter,
                    com.augmentalis.avanueui.core.Alignment.TopEnd ->
                        androidx.compose.ui.Alignment.Top

                    com.augmentalis.avanueui.core.Alignment.CenterStart,
                    com.augmentalis.avanueui.core.Alignment.Center,
                    com.augmentalis.avanueui.core.Alignment.CenterEnd ->
                        androidx.compose.ui.Alignment.CenterVertically

                    com.augmentalis.avanueui.core.Alignment.BottomStart,
                    com.augmentalis.avanueui.core.Alignment.BottomCenter,
                    com.augmentalis.avanueui.core.Alignment.BottomEnd ->
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
