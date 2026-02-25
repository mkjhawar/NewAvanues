package com.augmentalis.avanueui.renderer.android.mappers.foundation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.augmentalis.avanueui.dsl.IconComponent
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.ModifierConverter
import com.augmentalis.avanueui.renderer.android.toComposeColor

/**
 * IconMapper - Maps IconComponent to Material3 Icon
 */
class IconMapper : ComponentMapper<IconComponent> {
    private val modifierConverter = ModifierConverter()

    @Composable
    override fun map(component: IconComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val imageVector = getIconByName(component.name)

            Icon(
                imageVector = imageVector,
                contentDescription = component.contentDescription,
                modifier = modifierConverter.convert(component.modifiers),
                tint = component.tint?.toComposeColor()
                    ?: androidx.compose.ui.graphics.Color.Unspecified
            )
        }
    }

    /**
     * Map icon name to Material Icons
     * TODO: Expand this mapping to support more icons
     */
    private fun getIconByName(name: String): ImageVector {
        return when (name.lowercase()) {
            "home" -> Icons.Default.Home
            "settings" -> Icons.Default.Settings
            "person" -> Icons.Default.Person
            "email" -> Icons.Default.Email
            "phone" -> Icons.Default.Phone
            "search" -> Icons.Default.Search
            "menu" -> Icons.Default.Menu
            "close" -> Icons.Default.Close
            "check" -> Icons.Default.Check
            "add" -> Icons.Default.Add
            "delete" -> Icons.Default.Delete
            "edit" -> Icons.Default.Edit
            "favorite" -> Icons.Default.Favorite
            "star" -> Icons.Default.Star
            "info" -> Icons.Default.Info
            "warning" -> Icons.Default.Warning
            "lock" -> Icons.Default.Lock
            "visibility" -> Icons.Default.Visibility
            "visibilityoff" -> Icons.Default.VisibilityOff
            "arrowback" -> Icons.Default.ArrowBack
            "arrowforward" -> Icons.Default.ArrowForward
            else -> Icons.Default.Info // Default fallback
        }
    }
}
