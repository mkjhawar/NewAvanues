package com.augmentalis.videoavanue

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.decode.VideoFrameDecoder
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.videoavanue.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gallery screen showing device videos in a grid.
 * Queries MediaStore for video files, displays thumbnails via Coil video frame decoder.
 *
 * @param onVideoSelected Called when user taps a video item
 * @param columns Grid column count (default 2)
 * @param modifier Root modifier
 */
@Composable
fun VideoGalleryScreen(
    onVideoSelected: (VideoItem) -> Unit,
    columns: Int = 2,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AvanueTheme.colors
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        videos = queryVideos(context)
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
            videos.isEmpty() -> {
                Text(
                    "No videos found",
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
                    items(videos, key = { it.uri }) { video ->
                        VideoThumbnailCard(
                            video = video,
                            onClick = { onVideoSelected(video) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoThumbnailCard(video: VideoItem, onClick: () -> Unit) {
    val colors = AvanueTheme.colors
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Voice: click ${video.title.ifBlank { "video" }}" }
    ) {
        // Thumbnail with play overlay
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.uri)
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .videoFrameMillis(1000L)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
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
            // Play icon overlay
            Icon(
                Icons.Default.PlayCircleOutline,
                "Play",
                tint = colors.textPrimary.copy(alpha = 0.8f),
                modifier = Modifier.size(40.dp)
            )
            // Duration badge
            if (video.durationMs > 0) {
                Text(
                    video.durationFormatted,
                    color = colors.textPrimary,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(colors.surface.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        // Title
        Text(
            video.title.ifBlank { "Untitled" },
            color = colors.textPrimary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

/**
 * Query device videos from MediaStore.
 * Returns sorted by date modified (newest first).
 */
private suspend fun queryVideos(context: Context): List<VideoItem> = withContext(Dispatchers.IO) {
    val videos = mutableListOf<VideoItem>()
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT,
        MediaStore.Video.Media.MIME_TYPE,
        MediaStore.Video.Media.SIZE
    )

    context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        null, null,
        "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
        val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
            )
            videos.add(
                VideoItem(
                    uri = contentUri.toString(),
                    title = cursor.getString(nameCol) ?: "",
                    durationMs = cursor.getLong(durationCol),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    mimeType = cursor.getString(mimeCol) ?: "video/*",
                    fileSizeBytes = cursor.getLong(sizeCol)
                )
            )
        }
    }
    videos
}
