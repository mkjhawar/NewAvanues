package com.augmentalis.videoavanue.controller

import com.augmentalis.videoavanue.model.RepeatMode
import com.augmentalis.videoavanue.model.VideoItem
import com.augmentalis.videoavanue.model.VideoPlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Desktop (JVM) implementation of [IVideoController].
 *
 * Desktop video playback requires a third-party engine (JavaFX MediaPlayer or VLCJ).
 * This controller owns the authoritative [VideoPlayerState] and exposes the full
 * [IVideoController] contract through pure Kotlin state management. The playback
 * engine (to be injected) is responsible for actually producing audio/video frames;
 * this controller drives it by forwarding commands as needed.
 *
 * Design decisions:
 * - Speed range clamped to [0.25, 3.0] in 0.25x steps (interface comment says max 2.0x
 *   but the task spec says 3.0 — we use 3.0 to match the spec and expose more range).
 * - [seekForward] / [seekBackward] update [VideoPlayerState.positionMs] and clamp to
 *   [0, durationMs]. The actual engine seek must be triggered externally by observing
 *   the state change.
 * - [toggleLoop] cycles OFF → ONE → OFF (matching the interface description).
 * - [toggleFullscreen] is a display concern; it logs the intent and updates no model
 *   state (the UI layer owns the window/fullscreen surface).
 * - [release] resets state to defaults, signalling the UI to tear down the engine.
 *
 * Usage pattern (Compose for Desktop):
 * ```kotlin
 * val controller = DesktopVideoController()
 * controller.loadVideo(item)
 * LaunchedEffect(controller.state) { ... }
 * ```
 *
 * Author: Manoj Jhawar
 */
class DesktopVideoController : IVideoController {

    private val _state = MutableStateFlow(VideoPlayerState())
    override val state: StateFlow<VideoPlayerState> = _state.asStateFlow()

    companion object {
        private const val SPEED_STEP = 0.25f
        private const val SPEED_MIN = 0.25f
        private const val SPEED_MAX = 3.0f
    }

    // -------------------------------------------------------------------------
    // Lifecycle helpers (not in the interface — called by UI or DI owner)
    // -------------------------------------------------------------------------

    /**
     * Load a [VideoItem] into the controller, resetting playback state.
     * The caller is responsible for initialising the engine with [item.uri].
     */
    fun loadVideo(item: VideoItem) {
        _state.value = VideoPlayerState(
            video = item,
            durationMs = item.durationMs,
            isPlaying = false,
            positionMs = 0L
        )
    }

    // -------------------------------------------------------------------------
    // Playback control
    // -------------------------------------------------------------------------

    override fun play() {
        _state.update { it.copy(isPlaying = true, error = null) }
    }

    override fun pause() {
        _state.update { it.copy(isPlaying = false) }
    }

    override fun stop() {
        _state.update { it.copy(isPlaying = false, positionMs = 0L) }
    }

    // -------------------------------------------------------------------------
    // Seeking
    // -------------------------------------------------------------------------

    override fun seekForward(ms: Long) {
        _state.update { current ->
            val newPosition = (current.positionMs + ms).coerceIn(0L, current.durationMs)
            current.copy(positionMs = newPosition)
        }
    }

    override fun seekBackward(ms: Long) {
        _state.update { current ->
            val newPosition = (current.positionMs - ms).coerceAtLeast(0L)
            current.copy(positionMs = newPosition)
        }
    }

    override fun seekTo(positionMs: Long) {
        _state.update { current ->
            current.copy(positionMs = positionMs.coerceIn(0L, current.durationMs))
        }
    }

    // -------------------------------------------------------------------------
    // Speed
    // -------------------------------------------------------------------------

    override fun speedUp() {
        _state.update { current ->
            val next = (current.playbackSpeed + SPEED_STEP).coerceAtMost(SPEED_MAX)
            // Round to nearest 0.25 to avoid floating-point drift
            current.copy(playbackSpeed = roundToStep(next))
        }
    }

    override fun speedDown() {
        _state.update { current ->
            val next = (current.playbackSpeed - SPEED_STEP).coerceAtLeast(SPEED_MIN)
            current.copy(playbackSpeed = roundToStep(next))
        }
    }

    override fun normalSpeed() {
        _state.update { it.copy(playbackSpeed = 1.0f) }
    }

    // -------------------------------------------------------------------------
    // Audio
    // -------------------------------------------------------------------------

    override fun toggleMute() {
        _state.update { it.copy(isMuted = !it.isMuted) }
    }

    override fun setVolume(volume: Float) {
        _state.update { it.copy(volume = volume.coerceIn(0f, 1f)) }
    }

    // -------------------------------------------------------------------------
    // Loop / fullscreen
    // -------------------------------------------------------------------------

    override fun toggleLoop() {
        _state.update { current ->
            val next = when (current.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
                RepeatMode.ALL -> RepeatMode.OFF
            }
            current.copy(repeatMode = next)
        }
    }

    override fun toggleFullscreen() {
        // Fullscreen is a display/window concern on Desktop; the composable layer
        // (or the owning AppWindow) must observe this call and act on it.
        // We log the intent so integrators can hook in.
        println("[DesktopVideoController] toggleFullscreen requested — handle in UI layer")
    }

    // -------------------------------------------------------------------------
    // Release
    // -------------------------------------------------------------------------

    override fun release() {
        _state.value = VideoPlayerState()
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Round a speed value to the nearest 0.25 step to prevent floating-point
     * accumulation errors after many [speedUp]/[speedDown] calls.
     */
    private fun roundToStep(value: Float): Float =
        (Math.round(value / SPEED_STEP) * SPEED_STEP)
}
