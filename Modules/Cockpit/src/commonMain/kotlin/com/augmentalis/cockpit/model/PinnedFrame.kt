package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * A pinned frame that persists across session switches.
 *
 * Pinned frames float as PiP (picture-in-picture) overlays that remain
 * visible regardless of which session is active. Common uses:
 * - Camera feed always visible
 * - Clock/timer widget
 * - Voice channel during remote assistance
 * - Live metrics dashboard
 */
@Serializable
data class PinnedFrame(
    /** The frame to pin (must exist in some session) */
    val frameId: String,
    /** Source session ID */
    val sourceSessionId: String,
    /** PiP position on screen (0.0-1.0 normalized) */
    val pipX: Float = 0.8f,
    /** PiP position on screen (0.0-1.0 normalized) */
    val pipY: Float = 0.1f,
    /** PiP size as fraction of screen width (0.15 = 15%) */
    val pipScale: Float = 0.25f,
    /** PiP opacity (0.0-1.0) */
    val opacity: Float = 1.0f,
)
