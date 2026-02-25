package com.augmentalis.avanueui.renderer.android.mappers.foundation

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.avanueui.dsl.ColumnComponent
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.ModifierConverter

/**
 * ColumnMapper - Maps ColumnComponent to Jetpack Compose Column
 */
class ColumnMapper : ComponentMapper<ColumnComponent> {
    private val modifierConverter = ModifierConverter()

    @Composable
    override fun map(component: ColumnComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Column(
                modifier = modifierConverter.convert(component.modifiers),
                verticalArrangement = modifierConverter.toComposeArrangement(component.arrangement),
                horizontalAlignment = when (component.horizontalAlignment) {
                    com.augmentalis.avanueui.core.Alignment.TopStart,
                    com.augmentalis.avanueui.core.Alignment.CenterStart,
                    com.augmentalis.avanueui.core.Alignment.BottomStart ->
                        androidx.compose.ui.Alignment.Start

                    com.augmentalis.avanueui.core.Alignment.TopCenter,
                    com.augmentalis.avanueui.core.Alignment.Center,
                    com.augmentalis.avanueui.core.Alignment.BottomCenter ->
                        androidx.compose.ui.Alignment.CenterHorizontally

                    com.augmentalis.avanueui.core.Alignment.TopEnd,
                    com.augmentalis.avanueui.core.Alignment.CenterEnd,
                    com.augmentalis.avanueui.core.Alignment.BottomEnd ->
                        androidx.compose.ui.Alignment.End
                }
            ) {
                component.children.forEach { child ->
                    renderer.RenderComponent(child)
                }
            }
        }
    }
}
