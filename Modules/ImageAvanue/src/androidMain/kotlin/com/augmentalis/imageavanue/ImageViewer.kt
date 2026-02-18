package com.augmentalis.imageavanue

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.augmentalis.imageavanue.model.ImageFilter
import com.augmentalis.avanueui.theme.AvanueTheme

/**
 * Full-featured image viewer with pan/zoom/rotate gestures,
 * filter support, gallery navigation, and AVID voice semantics.
 *
 * @param uri Primary image URI to display.
 * @param gallery List of image URIs for gallery navigation.
 * @param initialIndex Starting index in gallery.
 * @param filter Currently applied image filter.
 * @param onImageChanged Called when navigating to a different image.
 * @param onFilterChanged Called when filter changes.
 * @param modifier Layout modifier.
 */
@Composable
fun ImageViewer(
    uri: String,
    gallery: List<String> = emptyList(),
    initialIndex: Int = 0,
    filter: ImageFilter = ImageFilter.NONE,
    onImageChanged: (Int) -> Unit = {},
    onFilterChanged: (ImageFilter) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val imageList = gallery.ifEmpty { listOf(uri) }
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, imageList.lastIndex)) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var flipH by remember { mutableStateOf(false) }
    var showMetadata by remember { mutableStateOf(false) }
    val currentUri = imageList.getOrElse(currentIndex) { uri }

    val colorFilter = remember(filter) { buildColorFilter(filter) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Voice: click image viewer" }
    ) {
        SubcomposeAsyncImage(
            model = currentUri,
            contentDescription = "Image ${currentIndex + 1} of ${imageList.size}",
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter,
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(48.dp), color = colors.primary)
                }
            },
            error = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Failed to load", color = colors.error)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = zoom * (if (flipH) -1f else 1f),
                    scaleY = zoom,
                    translationX = offsetX,
                    translationY = offsetY,
                    rotationZ = rotation
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, gestureRotation ->
                        zoom = (zoom * gestureZoom).coerceIn(0.5f, 8f)
                        offsetX += pan.x
                        offsetY += pan.y
                        rotation += gestureRotation
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        if (zoom > 1.1f) {
                            zoom = 1f; offsetX = 0f; offsetY = 0f; rotation = 0f
                        } else {
                            zoom = 3f
                        }
                    })
                }
        )

        // Metadata overlay
        if (showMetadata) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(colors.surface.copy(alpha = 0.85f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Index: ${currentIndex + 1}/${imageList.size}\nFilter: $filter",
                    color = colors.textPrimary,
                    fontSize = 11.sp
                )
            }
        }

        // Bottom control bar
        if (imageList.size > 1 || true) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(colors.surface.copy(alpha = 0.7f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentIndex > 0) {
                            currentIndex--; zoom = 1f; offsetX = 0f; offsetY = 0f; rotation = 0f
                            onImageChanged(currentIndex)
                        }
                    },
                    enabled = currentIndex > 0,
                    modifier = Modifier.semantics { contentDescription = "Voice: click previous image" }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous", tint = colors.textPrimary)
                }

                Text("${currentIndex + 1} / ${imageList.size}", color = colors.textPrimary)

                IconButton(
                    onClick = { rotation += 90f },
                    modifier = Modifier.semantics { contentDescription = "Voice: click rotate right" }
                ) {
                    Icon(Icons.Default.RotateRight, "Rotate", tint = colors.textPrimary)
                }

                IconButton(
                    onClick = { flipH = !flipH },
                    modifier = Modifier.semantics { contentDescription = "Voice: click flip horizontal" }
                ) {
                    Icon(Icons.Default.Flip, "Flip", tint = colors.textPrimary)
                }

                IconButton(
                    onClick = { showMetadata = !showMetadata },
                    modifier = Modifier.semantics { contentDescription = "Voice: click image info" }
                ) {
                    Icon(Icons.Default.Info, "Info", tint = colors.textPrimary)
                }

                IconButton(
                    onClick = {
                        if (currentIndex < imageList.lastIndex) {
                            currentIndex++; zoom = 1f; offsetX = 0f; offsetY = 0f; rotation = 0f
                            onImageChanged(currentIndex)
                        }
                    },
                    enabled = currentIndex < imageList.lastIndex,
                    modifier = Modifier.semantics { contentDescription = "Voice: click next image" }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next", tint = colors.textPrimary)
                }
            }
        }
    }
}

/**
 * Build a Compose ColorFilter from ImageFilter enum.
 * Grayscale/Sepia/Contrast/Invert use ColorMatrix; BLUR/SHARPEN require RenderEffect (not applied here).
 */
private fun buildColorFilter(filter: ImageFilter): ColorFilter? {
    return when (filter) {
        ImageFilter.NONE -> null

        ImageFilter.GRAYSCALE -> {
            val matrix = ColorMatrix().apply { setToSaturation(0f) }
            ColorFilter.colorMatrix(matrix)
        }

        ImageFilter.SEPIA -> {
            val grayscale = ColorMatrix().apply { setToSaturation(0f) }
            val sepiaTint = ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            ColorFilter.colorMatrix(sepiaTint)
        }

        ImageFilter.HIGH_CONTRAST -> {
            val contrast = 1.5f
            val translate = (-.5f * contrast + .5f) * 255f
            val matrix = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            ColorFilter.colorMatrix(matrix)
        }

        ImageFilter.INVERTED -> {
            val matrix = ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            ColorFilter.colorMatrix(matrix)
        }

        ImageFilter.BRIGHTNESS_UP -> {
            val matrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, 40f,
                0f, 1f, 0f, 0f, 40f,
                0f, 0f, 1f, 0f, 40f,
                0f, 0f, 0f, 1f, 0f
            ))
            ColorFilter.colorMatrix(matrix)
        }

        ImageFilter.BRIGHTNESS_DOWN -> {
            val matrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, -40f,
                0f, 1f, 0f, 0f, -40f,
                0f, 0f, 1f, 0f, -40f,
                0f, 0f, 0f, 1f, 0f
            ))
            ColorFilter.colorMatrix(matrix)
        }

        // BLUR/SHARPEN need RenderEffect — handled at Bitmap level, not ColorFilter
        ImageFilter.BLUR, ImageFilter.SHARPEN -> null
    }
}
