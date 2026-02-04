package com.augmentalis.magicelements.renderer.android.mappers

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.magicelements.dsl.ColumnComponent
import com.augmentalis.magicelements.renderer.android.ComponentMapper
import com.augmentalis.magicelements.renderer.android.ComposeRenderer
import com.augmentalis.magicelements.renderer.android.ModifierConverter

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
                    com.augmentalis.magicelements.core.Alignment.TopStart,
                    com.augmentalis.magicelements.core.Alignment.CenterStart,
                    com.augmentalis.magicelements.core.Alignment.BottomStart ->
                        androidx.compose.ui.Alignment.Start

                    com.augmentalis.magicelements.core.Alignment.TopCenter,
                    com.augmentalis.magicelements.core.Alignment.Center,
                    com.augmentalis.magicelements.core.Alignment.BottomCenter ->
                        androidx.compose.ui.Alignment.CenterHorizontally

                    com.augmentalis.magicelements.core.Alignment.TopEnd,
                    com.augmentalis.magicelements.core.Alignment.CenterEnd,
                    com.augmentalis.magicelements.core.Alignment.BottomEnd ->
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
