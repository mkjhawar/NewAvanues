package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * A single frame (window) within a Cockpit session.
 *
 * Each frame displays one content type (web, PDF, note, etc.) and has
 * independent position, size, and visual state. Frames can be freely
 * arranged in FREEFORM mode, or auto-positioned by the LayoutEngine
 * in other modes.
 */
@Serializable
data class CockpitFrame(
    /** Unique identifier (AVID-generated) */
    val id: String,
    /** Parent session ID */
    val sessionId: String,
    /** User-visible frame title */
    val title: String = "",
    /** Content displayed in this frame */
    val content: FrameContent,
    /** Position, size, and visual state */
    val state: FrameState = FrameState(),
    /** Grid position in spatial canvas (CENTER = default visible area) */
    val spatialPosition: SpatialPosition = SpatialPosition.CENTER,
    /** Creation timestamp (ISO 8601) */
    val createdAt: String = "",
    /** Last modification timestamp (ISO 8601) */
    val updatedAt: String = "",
) {
    /** Frame number within its session (1-based, assigned by session order) */
    val frameNumber: Int get() = 0 // Computed by session, not stored

    /** Content type identifier for display and renderer lookup */
    val contentType: String get() = content.typeId

    /** Semantic accent for themed border coloring */
    val accent: ContentAccent get() = ContentAccent.forContentType(contentType)

    /** Whether this frame is locked to a non-center spatial position */
    val isSpatiallyLocked: Boolean get() = !spatialPosition.isCenter
}
