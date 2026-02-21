package com.augmentalis.videoavanue

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.handlers.ModuleCommandCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun VideoPlayer(
    uri: String,
    autoPlay: Boolean = true,
    initialPositionMs: Long = 0,
    onPositionChanged: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AvanueTheme.colors
    var isPlaying by remember { mutableStateOf(autoPlay) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isMuted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var isLooping by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            prepare(); seekTo(initialPositionMs); playWhenReady = autoPlay
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isLoading = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) duration = exoPlayer.duration.coerceAtLeast(0)
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0)
            onPositionChanged(currentPosition); delay(500)
        }
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    // Wire voice command executor for video playback control
    DisposableEffect(exoPlayer) {
        ModuleCommandCallbacks.videoExecutor = { actionType, _ ->
            withContext(Dispatchers.Main) {
                executeVideoCommand(exoPlayer, actionType,
                    getSpeed = { playbackSpeed },
                    setSpeed = { playbackSpeed = it },
                    getMuted = { isMuted },
                    setMuted = { isMuted = it },
                    getLooping = { isLooping },
                    setLooping = { isLooping = it }
                )
            }
        }
        onDispose { ModuleCommandCallbacks.videoExecutor = null }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { PlayerView(it).apply { player = exoPlayer; useController = false } },
                modifier = Modifier.fillMaxSize()
            )
            if (isLoading) CircularProgressIndicator(Modifier.size(48.dp), color = colors.primary)
        }

        Column(Modifier.fillMaxWidth().background(colors.surface.copy(alpha = 0.85f)).padding(horizontal = 12.dp, vertical = 4.dp)) {
            Slider(
                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                onValueChange = { val seekTo = (it * duration).toLong(); exoPlayer.seekTo(seekTo); currentPosition = seekTo },
                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary, inactiveTrackColor = colors.textPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(currentPosition), color = colors.textPrimary.copy(alpha = 0.7f), fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { exoPlayer.seekBack() }, modifier = Modifier.semantics { contentDescription = "Voice: click skip backward" }) { Icon(Icons.Default.FastRewind, "Rewind", tint = colors.textPrimary) }
                    IconButton(onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }, modifier = Modifier.semantics { contentDescription = if (isPlaying) "Voice: click pause video" else "Voice: click play video" }) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = colors.textPrimary, modifier = Modifier.size(36.dp))
                    }
                    IconButton(onClick = { exoPlayer.seekForward() }, modifier = Modifier.semantics { contentDescription = "Voice: click skip forward" }) { Icon(Icons.Default.FastForward, "Forward", tint = colors.textPrimary) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isMuted = !isMuted; exoPlayer.volume = if (isMuted) 0f else 1f }, modifier = Modifier.semantics { contentDescription = if (isMuted) "Voice: click unmute video" else "Voice: click mute video" }) {
                        Icon(if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, "Mute", tint = colors.textPrimary)
                    }
                    Text(formatTime(duration), color = colors.textPrimary.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, sec) else String.format("%d:%02d", m, sec)
}

/**
 * Maps video voice commands to ExoPlayer operations.
 * Must be called on [Dispatchers.Main] — ExoPlayer methods are main-thread only.
 */
private fun executeVideoCommand(
    player: ExoPlayer,
    actionType: CommandActionType,
    getSpeed: () -> Float,
    setSpeed: (Float) -> Unit,
    getMuted: () -> Boolean,
    setMuted: (Boolean) -> Unit,
    getLooping: () -> Boolean,
    setLooping: (Boolean) -> Unit,
): HandlerResult {
    return when (actionType) {
        CommandActionType.VIDEO_PLAY -> {
            player.play()
            HandlerResult.success("Playing")
        }
        CommandActionType.VIDEO_PAUSE -> {
            player.pause()
            HandlerResult.success("Paused")
        }
        CommandActionType.VIDEO_STOP -> {
            player.stop()
            HandlerResult.success("Stopped")
        }
        CommandActionType.VIDEO_SEEK_FWD -> {
            player.seekForward()
            HandlerResult.success("Skipped forward")
        }
        CommandActionType.VIDEO_SEEK_BACK -> {
            player.seekBack()
            HandlerResult.success("Skipped backward")
        }
        CommandActionType.VIDEO_SPEED_UP -> {
            val newSpeed = (getSpeed() + 0.25f).coerceAtMost(2.0f)
            player.setPlaybackSpeed(newSpeed)
            setSpeed(newSpeed)
            HandlerResult.success("Speed: ${newSpeed}x")
        }
        CommandActionType.VIDEO_SPEED_DOWN -> {
            val newSpeed = (getSpeed() - 0.25f).coerceAtLeast(0.25f)
            player.setPlaybackSpeed(newSpeed)
            setSpeed(newSpeed)
            HandlerResult.success("Speed: ${newSpeed}x")
        }
        CommandActionType.VIDEO_SPEED_NORMAL -> {
            player.setPlaybackSpeed(1f)
            setSpeed(1f)
            HandlerResult.success("Speed: 1.0x")
        }
        CommandActionType.VIDEO_MUTE -> {
            player.volume = 0f
            setMuted(true)
            HandlerResult.success("Muted")
        }
        CommandActionType.VIDEO_UNMUTE -> {
            player.volume = 1f
            setMuted(false)
            HandlerResult.success("Unmuted")
        }
        CommandActionType.VIDEO_LOOP -> {
            val newLooping = !getLooping()
            player.repeatMode = if (newLooping) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            setLooping(newLooping)
            HandlerResult.success(if (newLooping) "Loop on" else "Loop off")
        }
        CommandActionType.VIDEO_FULLSCREEN -> {
            // Fullscreen is a Cockpit frame operation — handled by CockpitCommandHandler
            HandlerResult.failure("Fullscreen is a frame command — say 'maximize frame'", recoverable = true)
        }
        else -> HandlerResult.failure("Unsupported video action: $actionType", recoverable = true)
    }
}
