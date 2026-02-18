/**
 * CastReceiverView.kt — Composable display for incoming MJPEG stream
 *
 * Collects JPEG frame bytes from the [frames] flow, decodes each frame to a
 * Bitmap, and renders it via an Image composable. Shows a placeholder when
 * the stream is not yet connected.
 *
 * Design: SpatialVoice glass aesthetic using AvanueTheme tokens.
 * AVID: All interactive elements carry contentDescription voice identifiers.
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.remotecast.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Displays the live screen being received from a remote cast sender.
 *
 * @param frames      Flow of raw JPEG bytes emitted by [MjpegTcpClient].
 * @param isConnected Whether the TCP connection to the sender is established.
 * @param modifier    Layout modifier applied to the root container.
 */
@Composable
fun CastReceiverView(
    frames: Flow<ByteArray>,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    // Holds the latest decoded bitmap; null before the first frame arrives.
    var currentBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Collect frames and decode on IO dispatcher to avoid blocking composition.
    LaunchedEffect(frames) {
        frames.collect { jpegBytes ->
            val bitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            }
            if (bitmap != null) {
                // Recycle the previous bitmap before replacing it
                currentBitmap?.recycle()
                currentBitmap = bitmap
            }
        }
    }

    // Recycle the bitmap when the composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            currentBitmap?.recycle()
            currentBitmap = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .semantics { contentDescription = "Voice: cast receiver view" }
    ) {
        when {
            currentBitmap != null -> {
                // Live frame
                Image(
                    bitmap = currentBitmap!!.asImageBitmap(),
                    contentDescription = "Remote screen content",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "Voice: remote screen frame" }
                )
            }

            isConnected -> {
                // Connected but waiting for first frame
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(36.dp)
                            .semantics { contentDescription = "Voice: waiting for frame" }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Receiving stream…",
                        color = colors.textPrimary.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            else -> {
                // Not connected
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CastConnected,
                        contentDescription = "Connecting",
                        tint = colors.textPrimary.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Connecting…",
                        color = colors.textPrimary.copy(alpha = 0.5f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Waiting for the sender to start casting",
                        color = colors.textPrimary.copy(alpha = 0.35f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
