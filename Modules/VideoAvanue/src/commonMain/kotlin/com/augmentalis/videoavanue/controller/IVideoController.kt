package com.augmentalis.videoavanue.controller

import com.augmentalis.videoavanue.model.VideoPlayerState
import kotlinx.coroutines.flow.StateFlow

/**
 * Controller interface for video playback.
 * Manages playback state, speed, volume, seeking, and loop control.
 *
 * Architecture: Defined in commonMain, implemented per-platform.
 * Android wraps Media3 ExoPlayer; Desktop wraps JavaFX MediaPlayer.
 */
interface IVideoController {

    /** Observable playback state. */
    val state: StateFlow<VideoPlayerState>

    /** Play or resume playback. */
    fun play()

    /** Pause playback. */
    fun pause()

    /** Stop playback and reset to beginning. */
    fun stop()

    /** Seek forward by given milliseconds (default 10 seconds). */
    fun seekForward(ms: Long = 10_000L)

    /** Seek backward by given milliseconds (default 10 seconds). */
    fun seekBackward(ms: Long = 10_000L)

    /** Seek to absolute position in milliseconds. */
    fun seekTo(positionMs: Long)

    /** Increase playback speed by one step (0.25x increments, max 2.0x). */
    fun speedUp()

    /** Decrease playback speed by one step (0.25x increments, min 0.25x). */
    fun speedDown()

    /** Reset playback speed to 1.0x. */
    fun normalSpeed()

    /** Toggle mute/unmute. */
    fun toggleMute()

    /** Set volume (0.0 to 1.0). */
    fun setVolume(volume: Float)

    /** Toggle loop playback. */
    fun toggleLoop()

    /** Toggle fullscreen mode. */
    fun toggleFullscreen()

    /** Release player resources. Call when done with playback. */
    fun release()
}
