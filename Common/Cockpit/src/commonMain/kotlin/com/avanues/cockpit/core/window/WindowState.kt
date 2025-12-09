package com.avanues.cockpit.core.window

import kotlinx.serialization.Serializable

/**
 * Persistent state for a window
 *
 * Tracks scroll position, zoom level, media playback state, and last access time.
 * Ported from old Task_Cockpit scroll/zoom tracking to enable seamless window
 * switching and state restoration.
 *
 * **Voice-First Integration:**
 * - VoiceOS can announce state: "Browser at 150% zoom, scrolled to middle"
 * - Voice commands: "Reset zoom", "Resume video", "Go back to where I was"
 *
 * @property scrollX Horizontal scroll position in pixels
 * @property scrollY Vertical scroll position in pixels
 * @property zoomLevel Current zoom level (1.0 = 100%, 2.0 = 200%)
 * @property mediaPlaybackPosition Video/audio position in milliseconds
 * @property isPlaying Whether media is currently playing
 * @property lastAccessed Timestamp of last interaction (for LRU sorting)
 */
@Serializable
data class WindowState(
    val scrollX: Int = 0,
    val scrollY: Int = 0,
    val zoomLevel: Float = 1.0f,
    val mediaPlaybackPosition: Long = 0L,
    val isPlaying: Boolean = false,
    val lastAccessed: Long = System.currentTimeMillis()
) {
    companion object {
        val DEFAULT = WindowState()

        /** Minimum zoom level (50%) */
        const val MIN_ZOOM = 0.5f

        /** Maximum zoom level (300%) */
        const val MAX_ZOOM = 3.0f

        /** Default zoom increment for voice commands like "Zoom in" */
        const val ZOOM_STEP = 0.25f
    }

    /**
     * Creates a new state with updated scroll position
     * Voice command: "Remember my scroll position"
     */
    fun withScroll(x: Int, y: Int): WindowState = copy(
        scrollX = x,
        scrollY = y,
        lastAccessed = System.currentTimeMillis()
    )

    /**
     * Creates a new state with updated zoom level
     * Voice commands: "Zoom in", "Zoom out", "Set zoom to 150%"
     */
    fun withZoom(level: Float): WindowState = copy(
        zoomLevel = level.coerceIn(MIN_ZOOM, MAX_ZOOM),
        lastAccessed = System.currentTimeMillis()
    )

    /**
     * Creates a new state with incremented zoom
     * Voice command: "Zoom in"
     */
    fun zoomIn(): WindowState = withZoom(zoomLevel + ZOOM_STEP)

    /**
     * Creates a new state with decremented zoom
     * Voice command: "Zoom out"
     */
    fun zoomOut(): WindowState = withZoom(zoomLevel - ZOOM_STEP)

    /**
     * Creates a new state with default zoom (100%)
     * Voice command: "Reset zoom"
     */
    fun resetZoom(): WindowState = withZoom(1.0f)

    /**
     * Creates a new state with updated media playback position
     * Voice commands: "Remember playback position", "Pause here"
     */
    fun withMediaPosition(positionMs: Long, playing: Boolean = isPlaying): WindowState = copy(
        mediaPlaybackPosition = positionMs,
        isPlaying = playing,
        lastAccessed = System.currentTimeMillis()
    )

    /**
     * Creates a new state marking media as playing
     * Voice command: "Resume video"
     */
    fun play(): WindowState = copy(
        isPlaying = true,
        lastAccessed = System.currentTimeMillis()
    )

    /**
     * Creates a new state marking media as paused
     * Voice command: "Pause"
     */
    fun pause(): WindowState = copy(
        isPlaying = false,
        lastAccessed = System.currentTimeMillis()
    )

    /**
     * Creates a new state with updated access timestamp
     * Called automatically when window gains focus
     */
    fun touch(): WindowState = copy(
        lastAccessed = System.currentTimeMillis()
    )

    /**
     * Returns human-readable description for VoiceOS announcements
     * Example: "Scrolled 50% down, zoomed to 150%, video at 2 minutes 30 seconds"
     */
    fun toVoiceDescription(): String {
        val parts = mutableListOf<String>()

        // Scroll state
        if (scrollY > 0) {
            parts.add("scrolled down")
        }

        // Zoom state
        if (zoomLevel != 1.0f) {
            val zoomPercent = (zoomLevel * 100).toInt()
            parts.add("zoomed to $zoomPercent%")
        }

        // Media state
        if (mediaPlaybackPosition > 0L) {
            val minutes = mediaPlaybackPosition / 60000
            val seconds = (mediaPlaybackPosition % 60000) / 1000
            val timeStr = if (minutes > 0) {
                "$minutes minute${if (minutes != 1L) "s" else ""} $seconds seconds"
            } else {
                "$seconds seconds"
            }
            val playState = if (isPlaying) "playing" else "paused"
            parts.add("$playState at $timeStr")
        }

        return if (parts.isEmpty()) "default state" else parts.joinToString(", ")
    }
}
