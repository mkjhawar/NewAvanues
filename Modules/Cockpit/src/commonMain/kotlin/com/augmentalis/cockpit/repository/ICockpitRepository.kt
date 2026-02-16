package com.augmentalis.cockpit.repository

import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.CockpitSession
import com.augmentalis.cockpit.model.LayoutMode
import com.augmentalis.cockpit.model.WorkflowStep
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Cockpit session and frame persistence.
 *
 * Implemented per-platform:
 * - Android: SQLDelight via VoiceOSDatabase
 * - Desktop: SQLDelight via JVM SQLite driver
 * - iOS: SQLDelight via native driver (future)
 */
interface ICockpitRepository {

    // ── Sessions ─────────────────────────────────────────────────────

    /** Observe all sessions (ordered by updatedAt descending) */
    fun observeSessions(): Flow<List<CockpitSession>>

    /** Get a session by ID with all its frames and workflow steps */
    suspend fun getSession(sessionId: String): CockpitSession?

    /** Get or create the default session */
    suspend fun getOrCreateDefaultSession(): CockpitSession

    /** Create a new session */
    suspend fun createSession(name: String, layoutMode: LayoutMode = LayoutMode.DEFAULT): CockpitSession

    /** Update session metadata (name, layout mode, selected frame) */
    suspend fun updateSession(session: CockpitSession)

    /** Delete a session and all its frames */
    suspend fun deleteSession(sessionId: String)

    /** Duplicate a session (deep copy with new IDs) */
    suspend fun duplicateSession(sessionId: String, newName: String): CockpitSession?

    // ── Frames ───────────────────────────────────────────────────────

    /** Observe frames for a session (ordered by zOrder) */
    fun observeFrames(sessionId: String): Flow<List<CockpitFrame>>

    /** Add a new frame to a session */
    suspend fun addFrame(frame: CockpitFrame)

    /** Update frame state (position, size, content, visibility) */
    suspend fun updateFrame(frame: CockpitFrame)

    /** Remove a frame */
    suspend fun removeFrame(frameId: String)

    /** Reorder frames (update zOrder values) */
    suspend fun reorderFrames(sessionId: String, orderedFrameIds: List<String>)

    // ── Workflow Steps ────────────────────────────────────────────────

    /** Get workflow steps for a session */
    suspend fun getWorkflowSteps(sessionId: String): List<WorkflowStep>

    /** Set workflow steps (replaces all existing steps) */
    suspend fun setWorkflowSteps(sessionId: String, steps: List<WorkflowStep>)

    // ── Bulk Operations ──────────────────────────────────────────────

    /** Export session as JSON string (for sharing/backup) */
    suspend fun exportSession(sessionId: String): String

    /** Import session from JSON string */
    suspend fun importSession(json: String): CockpitSession?
}
