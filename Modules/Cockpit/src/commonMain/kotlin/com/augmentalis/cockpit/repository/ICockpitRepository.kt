package com.augmentalis.cockpit.repository

import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.CockpitSession
import com.augmentalis.cockpit.model.WorkflowStep

/**
 * Repository interface for Cockpit session and frame persistence.
 *
 * Provides imperative suspend-based CRUD operations.
 * Reactive Flow-based observers will be added per-platform when needed.
 *
 * Implemented per-platform:
 * - Android: SQLDelight via VoiceOSDatabase
 * - Desktop: SQLDelight via JVM SQLite driver
 * - iOS: SQLDelight via native driver (future)
 */
interface ICockpitRepository {

    // ── Sessions ─────────────────────────────────────────────────────

    /** Get all sessions (ordered by updatedAt descending) */
    suspend fun getSessions(): List<CockpitSession>

    /** Get a session by ID */
    suspend fun getSession(sessionId: String): CockpitSession?

    /** Insert or replace a session */
    suspend fun saveSession(session: CockpitSession)

    /** Delete a session and all its frames */
    suspend fun deleteSession(sessionId: String)

    // ── Frames ───────────────────────────────────────────────────────

    /** Get all frames for a session (ordered by zOrder) */
    suspend fun getFrames(sessionId: String): List<CockpitFrame>

    /** Insert or replace a frame */
    suspend fun saveFrame(frame: CockpitFrame)

    /** Delete a frame by ID */
    suspend fun deleteFrame(frameId: String)

    /** Update only the content JSON for a frame */
    suspend fun updateFrameContent(frameId: String, contentJson: String)

    // ── Workflow Steps ────────────────────────────────────────────────

    /** Get workflow steps for a session */
    suspend fun getWorkflowSteps(sessionId: String): List<WorkflowStep>

    /** Save a single workflow step */
    suspend fun saveWorkflowStep(step: WorkflowStep)

    // ── Bulk Operations ──────────────────────────────────────────────

    /** Export session as JSON string (for sharing/backup) */
    suspend fun exportSession(sessionId: String): String

    /** Import session from JSON string */
    suspend fun importSession(json: String): CockpitSession?
}
