package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.augmentalis.avanues.avamagic.ui.core.layout.ColumnComponent
import com.augmentalis.avanues.avamagic.ui.core.layout.HorizontalAlignment
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter
import com.augmentalis.avanues.avamagic.ui.core.layout.Arrangement as LayoutArrangement

/**
 * ColumnMapper - Maps ColumnComponent to Jetpack Compose Column
 */
class ColumnMapper : ComponentMapper<ColumnComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ColumnComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Column(
                modifier = modifierConverter.convert(component.modifiers),
                verticalArrangement = when (component.arrangement) {
                    LayoutArrangement.Top -> Arrangement.Top
                    LayoutArrangement.Center -> Arrangement.Center
                    LayoutArrangement.Bottom -> Arrangement.Bottom
                    LayoutArrangement.SpaceBetween -> Arrangement.SpaceBetween
                    LayoutArrangement.SpaceAround -> Arrangement.SpaceAround
                    LayoutArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
                    else -> Arrangement.Top
                },
                horizontalAlignment = when (component.horizontalAlignment) {
                    HorizontalAlignment.Start -> Alignment.Start
                    HorizontalAlignment.Center -> Alignment.CenterHorizontally
                    HorizontalAlignment.End -> Alignment.End
                }
            ) {
                component.children.forEach { child ->
                    renderer.RenderComponent(child)
                }
            }
        }
    }
}
