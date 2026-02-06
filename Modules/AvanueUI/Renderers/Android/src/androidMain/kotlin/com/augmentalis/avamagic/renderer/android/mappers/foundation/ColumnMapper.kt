package com.augmentalis.avamagic.renderer.android.mappers.foundation

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.avamagic.dsl.ColumnComponent
import com.augmentalis.avamagic.renderer.android.ComponentMapper
import com.augmentalis.avamagic.renderer.android.ComposeRenderer
import com.augmentalis.avamagic.renderer.android.ModifierConverter

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
                    com.augmentalis.avamagic.core.Alignment.TopStart,
                    com.augmentalis.avamagic.core.Alignment.CenterStart,
                    com.augmentalis.avamagic.core.Alignment.BottomStart ->
                        androidx.compose.ui.Alignment.Start

                    com.augmentalis.avamagic.core.Alignment.TopCenter,
                    com.augmentalis.avamagic.core.Alignment.Center,
                    com.augmentalis.avamagic.core.Alignment.BottomCenter ->
                        androidx.compose.ui.Alignment.CenterHorizontally

                    com.augmentalis.avamagic.core.Alignment.TopEnd,
                    com.augmentalis.avamagic.core.Alignment.CenterEnd,
                    com.augmentalis.avamagic.core.Alignment.BottomEnd ->
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
