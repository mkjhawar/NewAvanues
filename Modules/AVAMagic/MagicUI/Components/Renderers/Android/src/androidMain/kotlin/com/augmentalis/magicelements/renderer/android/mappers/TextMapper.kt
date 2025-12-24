package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.augmentalis.magicui.ui.core.display.TextComponent
import com.augmentalis.magicui.ui.core.display.TextAlign as ComponentTextAlign
import com.augmentalis.magicui.ui.core.display.TextOverflow as ComponentTextOverflow
import com.augmentalis.magicui.ui.core.display.FontWeight as ComponentFontWeight
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter
import com.augmentalis.avaelements.renderer.android.toComposeColor

/**
 * TextMapper - Maps TextComponent to Material3 Text
 */
class TextMapper : ComponentMapper<TextComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: TextComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Text(
                text = component.text,
                modifier = modifierConverter.convert(component.modifiers),
                style = TextStyle(
                    fontSize = component.fontSize.sp,
                    fontWeight = when (component.fontWeight) {
                        ComponentFontWeight.Thin -> FontWeight.Thin
                        ComponentFontWeight.ExtraLight -> FontWeight.ExtraLight
                        ComponentFontWeight.Light -> FontWeight.Light
                        ComponentFontWeight.Normal -> FontWeight.Normal
                        ComponentFontWeight.Medium -> FontWeight.Medium
                        ComponentFontWeight.SemiBold -> FontWeight.SemiBold
                        ComponentFontWeight.Bold -> FontWeight.Bold
                        ComponentFontWeight.ExtraBold -> FontWeight.ExtraBold
                        ComponentFontWeight.Black -> FontWeight.Black
                    }
                ),
                color = component.color.toComposeColor(),
                textAlign = when (component.textAlign) {
                    ComponentTextAlign.Start -> androidx.compose.ui.text.style.TextAlign.Start
                    ComponentTextAlign.Center -> androidx.compose.ui.text.style.TextAlign.Center
                    ComponentTextAlign.End -> androidx.compose.ui.text.style.TextAlign.End
                    ComponentTextAlign.Justify -> androidx.compose.ui.text.style.TextAlign.Justify
                },
                overflow = when (component.overflow) {
                    ComponentTextOverflow.Clip -> androidx.compose.ui.text.style.TextOverflow.Clip
                    ComponentTextOverflow.Ellipsis -> androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    ComponentTextOverflow.Visible -> androidx.compose.ui.text.style.TextOverflow.Visible
                },
                maxLines = component.maxLines ?: Int.MAX_VALUE
            )
        }
    }
}
