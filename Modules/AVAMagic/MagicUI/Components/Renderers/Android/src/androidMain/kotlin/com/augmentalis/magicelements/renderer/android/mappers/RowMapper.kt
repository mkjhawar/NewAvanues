package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.augmentalis.magicui.ui.core.layout.RowComponent
import com.augmentalis.magicui.ui.core.layout.VerticalAlignment
import com.augmentalis.magicui.ui.core.layout.HorizontalArrangement
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * RowMapper - Maps RowComponent to Jetpack Compose Row
 */
class RowMapper : ComponentMapper<RowComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: RowComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Row(
                modifier = modifierConverter.convert(component.modifiers),
                horizontalArrangement = when (component.horizontalArrangement) {
                    HorizontalArrangement.Start -> Arrangement.Start
                    HorizontalArrangement.End -> Arrangement.End
                    HorizontalArrangement.Center -> Arrangement.Center
                    HorizontalArrangement.SpaceBetween -> Arrangement.SpaceBetween
                    HorizontalArrangement.SpaceAround -> Arrangement.SpaceAround
                    HorizontalArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
                },
                verticalAlignment = when (component.verticalAlignment) {
                    VerticalAlignment.Top -> Alignment.Top
                    VerticalAlignment.Center -> Alignment.CenterVertically
                    VerticalAlignment.Bottom -> Alignment.Bottom
                }
            ) {
                component.children.forEach { child ->
                    renderer.RenderComponent(child)
                }
            }
        }
    }
}
