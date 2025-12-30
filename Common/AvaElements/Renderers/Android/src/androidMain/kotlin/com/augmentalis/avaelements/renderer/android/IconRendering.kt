package com.augmentalis.avaelements.renderer.android

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.augmentalis.magicelements.core.resources.AndroidIconResourceManager
import com.augmentalis.magicelements.core.resources.IconResource
import com.augmentalis.magicelements.core.resources.IconSize
import kotlinx.coroutines.launch

/**
 * Render an icon from IconResource
 *
 * Handles all icon types:
 * - Material Icons
 * - Vector Drawables
 * - Raster Images
 * - Network Images
 * - Base64 Images
 *
 * @param iconResource Icon resource to render (null = no icon)
 * @param size Icon size in dp
 * @param contentDescription Accessibility description
 * @param tint Icon tint color
 * @param modifier Compose modifier
 */
@Composable
fun IconFromResource(
    iconResource: IconResource?,
    size: Dp = 24.dp,
    contentDescription: String? = null,
    tint: Color? = null,
    modifier: Modifier = Modifier
) {
    if (iconResource == null) return

    val context = LocalContext.current
    val iconManager = remember { AndroidIconResourceManager.getInstance(context) }
    val iconSize = IconSize.fromDp(size.value)

    when (iconResource) {
        is IconResource.MaterialIcon -> {
            MaterialIconFromResource(
                iconResource = iconResource,
                size = size,
                contentDescription = contentDescription,
                tint = tint,
                modifier = modifier
            )
        }

        is IconResource.NetworkImage -> {
            NetworkIconFromResource(
                iconResource = iconResource,
                size = size,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }

        else -> {
            // Fallback for other types
            var iconState by remember { mutableStateOf<Any?>(null) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(iconResource) {
                scope.launch {
                    iconState = iconManager.loadIcon(
                        resource = iconResource,
                        size = iconSize,
                        tint = tint?.toString()
                    )
                }
            }

            iconState?.let { icon ->
                if (icon is ImageVector) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        modifier = modifier,
                        tint = tint ?: Color.Unspecified
                    )
                }
            }
        }
    }
}

/**
 * Render Material Icon
 */
@Composable
private fun MaterialIconFromResource(
    iconResource: IconResource.MaterialIcon,
    size: Dp,
    contentDescription: String?,
    tint: Color?,
    modifier: Modifier
) {
    val context = LocalContext.current
    val iconManager = remember { AndroidIconResourceManager.getInstance(context) }
    var icon by remember { mutableStateOf<ImageVector?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(iconResource) {
        scope.launch {
            val result = iconManager.loadIcon(
                resource = iconResource,
                size = IconSize.fromDp(size.value)
            )
            if (result is ImageVector) {
                icon = result
            }
        }
    }

    icon?.let {
        Icon(
            imageVector = it,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint ?: Color.Unspecified
        )
    }
}

/**
 * Render Network Icon using Coil
 */
@Composable
private fun NetworkIconFromResource(
    iconResource: IconResource.NetworkImage,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(iconResource.url)
            .size((size.value * LocalContext.current.resources.displayMetrics.density).toInt())
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        placeholder = iconResource.placeholder?.let {
            // TODO: Render placeholder icon
            null
        },
        error = iconResource.errorIcon?.let {
            // TODO: Render error icon
            null
        }
    )
}

/**
 * Load icon from string resource name
 *
 * Auto-detects icon type and renders appropriately.
 *
 * @param iconName Icon name (Material icon, URL, or resource path)
 * @param size Icon size in dp
 * @param contentDescription Accessibility description
 * @param tint Icon tint color
 * @param modifier Compose modifier
 */
@Composable
fun IconFromString(
    iconName: String?,
    size: Dp = 24.dp,
    contentDescription: String? = null,
    tint: Color? = null,
    modifier: Modifier = Modifier
) {
    val iconResource = remember(iconName) {
        iconName?.let { IconResource.fromString(it) }
    }

    IconFromResource(
        iconResource = iconResource,
        size = size,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
    )
}

/**
 * Preload icons for better performance
 *
 * Use this to preload commonly used icons during app startup.
 *
 * @param iconNames List of icon names to preload
 */
@Composable
fun PreloadIcons(iconNames: List<String>) {
    val context = LocalContext.current
    val iconManager = remember { AndroidIconResourceManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(iconNames) {
        scope.launch {
            val resources = iconNames.map { IconResource.fromString(it) }
            iconManager.preloadIcons(resources)
        }
    }
}
