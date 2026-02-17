package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * A Cockpit session — a named collection of frames with a layout mode.
 *
 * Users can create multiple sessions (e.g., "Research", "Meeting Notes",
 * "Field Inspection") and switch between them. Each session preserves
 * its own frame arrangement and state independently.
 */
@Serializable
data class CockpitSession(
    /** Unique identifier (AVID-generated) */
    val id: String,
    /** User-visible session name */
    val name: String,
    /** How frames are arranged in this session */
    val layoutMode: LayoutMode = LayoutMode.DEFAULT,
    /** Ordered list of frames in this session */
    val frames: List<CockpitFrame> = emptyList(),
    /** Workflow steps (only used when layoutMode == WORKFLOW) */
    val workflowSteps: List<WorkflowStep> = emptyList(),
    /** ID of the currently selected/focused frame (null if none) */
    val selectedFrameId: String? = null,
    /** Whether this is the auto-created default session */
    val isDefault: Boolean = false,
    /** Optional background URI for the session (wallpaper, gradient, etc.) */
    val backgroundUri: String? = null,
    /** Creation timestamp (ISO 8601) */
    val createdAt: String = "",
    /** Last modification timestamp (ISO 8601) */
    val updatedAt: String = "",
) {
    /** Number of visible (non-hidden) frames */
    val visibleFrameCount: Int get() = frames.count { it.state.isVisible }

    /** Total frame count */
    val frameCount: Int get() = frames.size

    /** Get a frame by its 1-based frame number */
    fun frameByNumber(number: Int): CockpitFrame? =
        frames.getOrNull(number - 1)

    /** Get a frame by ID */
    fun frameById(id: String): CockpitFrame? =
        frames.firstOrNull { it.id == id }

    /** Frames with 1-based numbering applied */
    val numberedFrames: List<Pair<Int, CockpitFrame>>
        get() = frames.mapIndexed { index, frame -> (index + 1) to frame }
}
