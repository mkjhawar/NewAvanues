package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.augmentalis.avamagic.ui.core.display.AvatarComponent
import com.augmentalis.avamagic.ui.core.display.AvatarShape
import com.augmentalis.avamagic.components.core.ComponentSize
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * AvatarMapper - Maps AvatarComponent to circular/square image or initials
 */
class AvatarMapper : ComponentMapper<AvatarComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: AvatarComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val modifier = modifierConverter.convert(component.modifiers)
            val sizeDp = when (component.size) {
                ComponentSize.XS -> 24.dp
                ComponentSize.SM -> 32.dp
                ComponentSize.MD -> 48.dp
                ComponentSize.LG -> 64.dp
                ComponentSize.XL -> 80.dp
            }

            val shape = when (component.shape) {
                AvatarShape.CIRCLE -> CircleShape
                AvatarShape.SQUARE -> RoundedCornerShape(0.dp)
                AvatarShape.ROUNDED -> RoundedCornerShape(8.dp)
            }

            Box(
                modifier = modifier
                    .size(sizeDp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                component.initials?.let { initials ->
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                // TODO: Handle image loading from component.imageUrl when image loading library is added
            }
        }
    }
}
