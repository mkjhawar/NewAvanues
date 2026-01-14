package com.augmentalis.avamagic.renderer.android.mappers.foundation

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.augmentalis.avamagic.dsl.ImageComponent
import com.augmentalis.avamagic.dsl.ImageScope
import com.augmentalis.avamagic.renderer.android.ComponentMapper
import com.augmentalis.avamagic.renderer.android.ComposeRenderer
import com.augmentalis.avamagic.renderer.android.ModifierConverter

/**
 * ImageMapper - Maps ImageComponent to Compose Image
 *
 * Supports both local resources and network URLs using Coil
 */
class ImageMapper : ComponentMapper<ImageComponent> {
    private val modifierConverter = ModifierConverter()

    @Composable
    override fun map(component: ImageComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val contentScale = when (component.contentScale) {
                ImageScope.ContentScale.Fit -> ContentScale.Fit
                ImageScope.ContentScale.Fill -> ContentScale.FillBounds
                ImageScope.ContentScale.Crop -> ContentScale.Crop
                ImageScope.ContentScale.None -> ContentScale.None
            }

            // Use Coil for network images or local file paths
            // For Android resources, you'd need to parse "R.drawable.xxx"
            if (component.source.startsWith("http://") ||
                component.source.startsWith("https://") ||
                component.source.startsWith("file://")) {
                AsyncImage(
                    model = component.source,
                    contentDescription = component.contentDescription,
                    modifier = modifierConverter.convert(component.modifiers),
                    contentScale = contentScale
                )
            } else {
                // For local resources, this would need resource ID lookup
                // This is a simplified version
                AsyncImage(
                    model = component.source,
                    contentDescription = component.contentDescription,
                    modifier = modifierConverter.convert(component.modifiers),
                    contentScale = contentScale
                )
            }
        }
    }
}
