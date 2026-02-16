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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.augmentalis.avanueui.theme.AvanueTheme

@Composable
fun ImageViewer(
    uri: String,
    gallery: List<String> = emptyList(),
    initialIndex: Int = 0,
    onImageChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val imageList = gallery.ifEmpty { listOf(uri) }
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, imageList.lastIndex)) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    val currentUri = imageList.getOrElse(currentIndex) { uri }

    Box(modifier = modifier.fillMaxSize()) {
        SubcomposeAsyncImage(
            model = currentUri,
            contentDescription = "Image ${currentIndex + 1}",
            contentScale = ContentScale.Fit,
            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(48.dp), color = colors.primary) } },
            error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Failed to load", color = colors.error) } },
            modifier = Modifier.fillMaxSize()
                .graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = offsetX, translationY = offsetY, rotationZ = rotation)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, gestureRotation ->
                        zoom = (zoom * gestureZoom).coerceIn(0.5f, 8f)
                        offsetX += pan.x; offsetY += pan.y; rotation += gestureRotation
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        if (zoom > 1.1f) { zoom = 1f; offsetX = 0f; offsetY = 0f; rotation = 0f } else zoom = 3f
                    })
                }
        )
        if (imageList.size > 1) {
            Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(colors.surface.copy(alpha = 0.7f)).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (currentIndex > 0) { currentIndex--; zoom = 1f; offsetX = 0f; offsetY = 0f; rotation = 0f; onImageChanged(currentIndex) } }, enabled = currentIndex > 0) {
                    Icon(Icons.Default.ChevronLeft, "Previous", tint = colors.onSurface) }
                Text("${currentIndex + 1} / ${imageList.size}", color = colors.onSurface)
                IconButton(onClick = { rotation += 90f }) { Icon(Icons.Default.RotateRight, "Rotate", tint = colors.onSurface) }
                IconButton(onClick = { if (currentIndex < imageList.lastIndex) { currentIndex++; zoom = 1f; offsetX = 0f; offsetY = 0f; rotation = 0f; onImageChanged(currentIndex) } }, enabled = currentIndex < imageList.lastIndex) {
                    Icon(Icons.Default.ChevronRight, "Next", tint = colors.onSurface) }
            }
        }
    }
}
