package com.augmentalis.avanueui.renderer.android.mappers.foundation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.augmentalis.avanueui.dsl.TextComponent
import com.augmentalis.avanueui.dsl.TextScope
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.ModifierConverter
import com.augmentalis.avanueui.renderer.android.toComposeColor
import com.augmentalis.avanueui.renderer.android.toTextStyle

/**
 * TextMapper - Maps TextComponent to Material3 Text
 */
class TextMapper : ComponentMapper<TextComponent> {
    private val modifierConverter = ModifierConverter()

    @Composable
    override fun map(component: TextComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Text(
                text = component.text,
                modifier = modifierConverter.convert(component.modifiers),
                style = component.font.toTextStyle(),
                color = component.color.toComposeColor(),
                textAlign = when (component.textAlign) {
                    TextScope.TextAlign.Start -> TextAlign.Start
                    TextScope.TextAlign.Center -> TextAlign.Center
                    TextScope.TextAlign.End -> TextAlign.End
                    TextScope.TextAlign.Justify -> TextAlign.Justify
                },
                overflow = when (component.overflow) {
                    TextScope.TextOverflow.Clip -> TextOverflow.Clip
                    TextScope.TextOverflow.Ellipsis -> TextOverflow.Ellipsis
                    TextScope.TextOverflow.Visible -> TextOverflow.Visible
                },
                maxLines = component.maxLines ?: Int.MAX_VALUE
            )
        }
    }
}
