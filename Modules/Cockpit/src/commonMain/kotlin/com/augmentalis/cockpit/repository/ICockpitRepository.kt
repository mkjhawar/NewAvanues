package com.augmentalis.cockpit.repository

import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.CockpitSession
import com.augmentalis.cockpit.model.CrossFrameLink
import com.augmentalis.cockpit.model.PinnedFrame
import com.augmentalis.cockpit.model.TimelineEvent
import com.augmentalis.cockpit.model.WorkflowStep
import kotlinx.serialization.Serializable

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

    // ── Pinned Frames ─────────────────────────────────────────────────

    /** Get all pinned frames (PiP overlays visible across sessions) */
    suspend fun getPinnedFrames(): List<PinnedFrame>

    /** Pin a frame for PiP overlay */
    suspend fun pinFrame(pinnedFrame: PinnedFrame)

    /** Unpin a frame by its frame ID */
    suspend fun unpinFrame(frameId: String)

    // ── Cross-Frame Links ─────────────────────────────────────────────

    /** Get all links for a session */
    suspend fun getCrossFrameLinks(sessionId: String): List<CrossFrameLink>

    /** Get enabled links originating from a specific frame */
    suspend fun getLinksFromFrame(sourceFrameId: String): List<CrossFrameLink>

    /** Save a cross-frame link */
    suspend fun saveCrossFrameLink(sessionId: String, link: CrossFrameLink)

    /** Delete a cross-frame link by ID */
    suspend fun deleteCrossFrameLink(linkId: String)

    // ── Timeline Events ───────────────────────────────────────────────

    /** Get timeline events for a session (newest first) */
    suspend fun getTimelineEvents(sessionId: String, limit: Int = 50): List<TimelineEvent>

    /** Log a timeline event */
    suspend fun logEvent(event: TimelineEvent)

    /** Delete events older than the given ISO 8601 timestamp */
    suspend fun clearOldEvents(beforeTimestamp: String)

    // ── Bulk Operations ──────────────────────────────────────────────

    /** Export session as JSON string (for sharing/backup) */
    suspend fun exportSession(sessionId: String): String

    /** Import session from JSON string */
    suspend fun importSession(json: String): CockpitSession?
}

/**
 * Serialization wrapper for session export/import.
 * Avoids double-encoding by holding typed objects directly.
 */
@Serializable
data class SessionExport(
    val session: CockpitSession,
    val frames: List<CockpitFrame>,
    val steps: List<WorkflowStep>
)
