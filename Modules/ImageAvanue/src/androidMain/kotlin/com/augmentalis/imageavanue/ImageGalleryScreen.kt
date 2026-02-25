package com.augmentalis.imageavanue

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.imageavanue.model.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gallery screen showing device images in a grid.
 * Queries MediaStore for image files, displays thumbnails via Coil.
 *
 * @param onImageSelected Called when user taps an image item
 * @param columns Grid column count (default 3 - images are typically more numerous than videos)
 * @param modifier Root modifier
 */
@Composable
fun ImageGalleryScreen(
    onImageSelected: (ImageItem) -> Unit,
    columns: Int = 3,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AvanueTheme.colors
    var images by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        images = queryImages(context)
        isLoading = false
    }

    Box(modifier.fillMaxSize().background(colors.background)) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    Modifier.size(48.dp).align(Alignment.Center),
                    color = colors.primary
                )
            }
            images.isEmpty() -> {
                Text(
                    "No images found",
                    color = colors.textPrimary.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(images, key = { it.uri }) { image ->
                        ImageThumbnailCard(
                            image = image,
                            onClick = { onImageSelected(image) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageThumbnailCard(image: ImageItem, onClick: () -> Unit) {
    val colors = AvanueTheme.colors

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Voice: click ${image.title.ifBlank { "image" }}" }
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = image.uri,
                contentDescription = image.title,
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        Modifier.fillMaxSize().background(colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            Modifier.size(24.dp),
                            color = colors.primary,
                            strokeWidth = 2.dp
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            image.title.ifBlank { "Untitled" },
            color = colors.textPrimary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

/**
 * Query device images from MediaStore.
 * Returns sorted by date modified (newest first).
 */
private suspend fun queryImages(context: Context): List<ImageItem> = withContext(Dispatchers.IO) {
    val images = mutableListOf<ImageItem>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_MODIFIED
    )

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null, null,
        "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
        val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
            )
            images.add(
                ImageItem(
                    uri = contentUri.toString(),
                    title = cursor.getString(nameCol) ?: "",
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    mimeType = cursor.getString(mimeCol) ?: "image/*",
                    fileSizeBytes = cursor.getLong(sizeCol),
                    dateModified = cursor.getLong(dateModCol)
                )
            )
        }
    }
    images
}
