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
)
