package com.augmentalis.videoavanue

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.augmentalis.avanueui.theme.AvanueTheme
import kotlinx.coroutines.delay

/**
 * Full-featured video player with ExoPlayer, SpatialVoice-themed custom controls.
 *
 * Features: play/pause, seek bar, seek forward/backward, speed control (0.25x-2.0x),
 * loop toggle, mute/unmute, fullscreen callback. All interactive elements have AVID semantics.
 *
 * @param uri Video URI (content://, file://, or https://)
 * @param autoPlay Start playing immediately
 * @param initialPositionMs Resume from this position
 * @param initialMuted Start muted
 * @param initialSpeed Start at this playback speed
 * @param onPositionChanged Callback for position sync (Cockpit frame persistence)
 * @param onFullscreenRequested Callback when user taps fullscreen — host handles window
 * @param modifier Root modifier
 */
@Composable
fun VideoPlayer(
    uri: String,
    autoPlay: Boolean = true,
    initialPositionMs: Long = 0,
    initialMuted: Boolean = false,
    initialSpeed: Float = 1.0f,
    onPositionChanged: (Long) -> Unit = {},
    onFullscreenRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AvanueTheme.colors

    var isPlaying by remember { mutableStateOf(autoPlay) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isMuted by remember { mutableStateOf(initialMuted) }
    var isLoading by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(initialSpeed) }
    var isLooping by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            prepare()
            seekTo(initialPositionMs)
            playWhenReady = autoPlay
            volume = if (initialMuted) 0f else 1f
            setPlaybackSpeed(initialSpeed)
        }
    }

    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isLoading = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) duration = exoPlayer.duration.coerceAtLeast(0)
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        exoPlayer.addListener(listener)
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0)
            onPositionChanged(currentPosition)
            delay(500)
        }
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    Column(modifier = modifier.fillMaxSize()) {
        // Video surface
        Box(
            Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { PlayerView(it).apply { player = exoPlayer; useController = false } },
                modifier = Modifier.fillMaxSize()
            )
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(48.dp), color = colors.primary)
            }
        }

        // Control bar
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.surface.copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            // Seek bar
            Slider(
                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                onValueChange = {
                    val seekTo = (it * duration).toLong()
                    exoPlayer.seekTo(seekTo)
                    currentPosition = seekTo
                },
                colors = SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.textPrimary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Main controls row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time elapsed
                Text(
                    formatTime(currentPosition),
                    color = colors.textPrimary.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )

                // Playback controls: rewind, play/pause, forward
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { exoPlayer.seekBack() },
                        modifier = Modifier.semantics { contentDescription = "Voice: click skip backward" }
                    ) {
                        Icon(Icons.Default.FastRewind, "Rewind", tint = colors.textPrimary)
                    }
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = if (isPlaying) "Voice: click pause video" else "Voice: click play video"
                        }
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "Play/Pause",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    IconButton(
                        onClick = { exoPlayer.seekForward() },
                        modifier = Modifier.semantics { contentDescription = "Voice: click skip forward" }
                    ) {
                        Icon(Icons.Default.FastForward, "Forward", tint = colors.textPrimary)
                    }
                }

                // Duration
                Text(
                    formatTime(duration),
                    color = colors.textPrimary.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            // Secondary controls: speed, loop, mute, fullscreen
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed chip — cycles through 0.5x, 1.0x, 1.25x, 1.5x, 2.0x
                SpeedChip(
                    speed = playbackSpeed,
                    onClick = {
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        val currentIdx = speeds.indexOfFirst { s -> s == playbackSpeed }
                        val nextIdx = if (currentIdx < 0 || currentIdx >= speeds.lastIndex) 0 else currentIdx + 1
                        playbackSpeed = speeds[nextIdx]
                        exoPlayer.setPlaybackSpeed(playbackSpeed)
                    }
                )

                // Loop toggle
                IconButton(
                    onClick = {
                        isLooping = !isLooping
                        exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    },
                    modifier = Modifier.semantics { contentDescription = "Voice: click loop video" }
                ) {
                    Icon(
                        if (isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        "Loop",
                        tint = if (isLooping) colors.primary else colors.textPrimary.copy(alpha = 0.6f)
                    )
                }

                // Mute/unmute
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        exoPlayer.volume = if (isMuted) 0f else 1f
                    },
                    modifier = Modifier.semantics {
                        contentDescription = if (isMuted) "Voice: click unmute video" else "Voice: click mute video"
                    }
                ) {
                    Icon(
                        if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        "Mute",
                        tint = colors.textPrimary
                    )
                }

                // Fullscreen
                IconButton(
                    onClick = onFullscreenRequested,
                    modifier = Modifier.semantics { contentDescription = "Voice: click fullscreen video" }
                ) {
                    Icon(Icons.Default.Fullscreen, "Fullscreen", tint = colors.textPrimary)
                }
            }
        }
    }
}

/**
 * Speed chip — shows current speed, toggles on click.
 * Highlighted when speed is not 1.0x.
 */
@Composable
private fun SpeedChip(speed: Float, onClick: () -> Unit) {
    val colors = AvanueTheme.colors
    val isNonDefault = speed != 1.0f
    val label = if (speed == speed.toLong().toFloat()) "${speed.toLong()}x" else "${speed}x"

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isNonDefault) colors.primary.copy(alpha = 0.2f) else colors.surface.copy(alpha = 0.5f),
        modifier = Modifier.semantics { contentDescription = "Voice: click speed up video" }
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Speed,
                "Speed",
                tint = if (isNonDefault) colors.primary else colors.textPrimary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (isNonDefault) FontWeight.Bold else FontWeight.Normal,
                color = if (isNonDefault) colors.primary else colors.textPrimary.copy(alpha = 0.6f)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, sec) else String.format("%d:%02d", m, sec)
}
