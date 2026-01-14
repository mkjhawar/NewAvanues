package com.augmentalis.magicelements.renderer.android.mappers

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.magicelements.dsl.CardComponent
import com.augmentalis.magicelements.renderer.android.ComponentMapper
import com.augmentalis.magicelements.renderer.android.ComposeRenderer
import com.augmentalis.magicelements.renderer.android.ModifierConverter

/**
 * CardMapper - Maps CardComponent to Material3 Card
 */
class CardMapper : ComponentMapper<CardComponent> {
    private val modifierConverter = ModifierConverter()

    @Composable
    override fun map(component: CardComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Card(
                modifier = modifierConverter.convert(component.modifiers),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = component.elevation.dp
                )
            ) {
                Column {
                    component.children.forEach { child ->
                        renderer.RenderComponent(child)
                    }
                }
            }
        }
    }
}
