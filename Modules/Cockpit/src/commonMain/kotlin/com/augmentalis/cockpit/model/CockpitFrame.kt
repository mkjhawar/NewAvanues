package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * Role of a frame within a workflow layout's multi-pane arrangement.
 *
 * - [STEPS]: Left panel — workflow step navigation list
 * - [CONTENT]: Center panel — main content (pictures, instructions, etc.)
 * - [AUXILIARY]: Right panel — supporting content (video call, chat, notes)
 *
 * When any frame in a workflow has [AUXILIARY] role, the layout engine
 * switches from 2-panel (30/70) to 3-panel (20/60/20) mode.
 */
@Serializable
enum class PanelRole {
    STEPS,
    CONTENT,
    AUXILIARY
}

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
    /** Role in workflow multi-pane layout (STEPS, CONTENT, or AUXILIARY) */
    val panelRole: PanelRole = PanelRole.CONTENT,
    /** Creation timestamp (ISO 8601) */
    val createdAt: String = "",
    /** Last modification timestamp (ISO 8601) */
    val updatedAt: String = "",
) {
    /** Content type identifier for display and renderer lookup */
    val contentType: String get() = content.typeId

    /** Semantic accent for themed border coloring */
    val accent: ContentAccent get() = ContentAccent.forContentType(contentType)

    /** Whether this frame is locked to a non-center spatial position */
    val isSpatiallyLocked: Boolean get() = !spatialPosition.isCenter
}
