package com.augmentalis.avamagic.renderer.android.mappers.foundation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.avamagic.core.Orientation
import com.augmentalis.avamagic.dsl.ScrollViewComponent
import com.augmentalis.avamagic.renderer.android.ComponentMapper
import com.augmentalis.avamagic.renderer.android.ComposeRenderer
import com.augmentalis.avamagic.renderer.android.ModifierConverter

/**
 * ScrollViewMapper - Maps ScrollViewComponent to Compose scrollable layouts
 */
class ScrollViewMapper : ComponentMapper<ScrollViewComponent> {
    private val modifierConverter = ModifierConverter()

    @Composable
    override fun map(component: ScrollViewComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val scrollState = rememberScrollState()
            val baseModifier = modifierConverter.convert(component.modifiers)

            when (component.orientation) {
                Orientation.Vertical -> {
                    Column(
                        modifier = baseModifier.verticalScroll(scrollState)
                    ) {
                        component.child?.let { child ->
                            renderer.RenderComponent(child)
                        }
                    }
                }
                Orientation.Horizontal -> {
                    Row(
                        modifier = baseModifier.horizontalScroll(scrollState)
                    ) {
                        component.child?.let { child ->
                            renderer.RenderComponent(child)
                        }
                    }
                }
            }
        }
    }
}
