package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.augmentalis.avanues.avamagic.ui.core.display.ImageComponent
import com.augmentalis.avanues.avamagic.ui.core.display.ImageSource
import com.augmentalis.avanues.avamagic.ui.core.display.ContentScale as ComponentContentScale
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * ImageMapper - Maps ImageComponent to Compose Image
 *
 * Supports both local resources and network URLs using Coil
 */
class ImageMapper : ComponentMapper<ImageComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ImageComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val contentScale = when (component.contentScale) {
                ComponentContentScale.Fit -> ContentScale.Fit
                ComponentContentScale.Fill -> ContentScale.FillBounds
                ComponentContentScale.Crop -> ContentScale.Crop
                ComponentContentScale.Inside -> ContentScale.Inside
                ComponentContentScale.None -> ContentScale.None
            }

            val imageModel = when (val source = component.source) {
                is ImageSource.Url -> source.url
                is ImageSource.Asset -> source.name
                is ImageSource.Resource -> source.id
                is ImageSource.Base64 -> source.data
            }

            AsyncImage(
                model = imageModel,
                contentDescription = component.contentDescription,
                modifier = modifierConverter.convert(component.modifiers),
                contentScale = contentScale
            )
        }
    }
}
