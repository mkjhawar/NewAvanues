package com.augmentalis.cockpit.repository

import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.CockpitSession
import com.augmentalis.cockpit.model.CrossFrameLink
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.cockpit.model.FrameState
import com.augmentalis.cockpit.model.LayoutMode
import com.augmentalis.cockpit.model.LinkAction
import com.augmentalis.cockpit.model.LinkTrigger
import com.augmentalis.cockpit.model.PinnedFrame
import com.augmentalis.cockpit.model.TimelineEvent
import com.augmentalis.cockpit.model.WorkflowStep
import com.augmentalis.database.VoiceOSDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Android implementation of [ICockpitRepository] backed by SQLDelight.
 *
 * Handles serialization of [FrameContent] to/from JSON for the `contentData`
 * column, and maps between SQLDelight generated row types and domain models.
 */
class AndroidCockpitRepository(
    private val database: VoiceOSDatabase
) : ICockpitRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    // ── Session CRUD ──────────────────────────────────────────────

    override suspend fun getSessions(): List<CockpitSession> = withContext(Dispatchers.IO) {
        database.cockpitSessionQueries.getAll().executeAsList().map { row ->
            CockpitSession(
                id = row.id,
                name = row.name,
                layoutMode = parseLayoutMode(row.layoutMode),
                selectedFrameId = row.selectedFrameId,
                isDefault = row.isDefault == 1L,
                createdAt = row.createdAt,
                updatedAt = row.updatedAt
            )
        }
    }

    override suspend fun getSession(sessionId: String): CockpitSession? = withContext(Dispatchers.IO) {
        database.cockpitSessionQueries.getById(sessionId).executeAsOneOrNull()?.let { row ->
            CockpitSession(
                id = row.id,
                name = row.name,
                layoutMode = parseLayoutMode(row.layoutMode),
                selectedFrameId = row.selectedFrameId,
                isDefault = row.isDefault == 1L,
                createdAt = row.createdAt,
                updatedAt = row.updatedAt
            )
        }
    }

    override suspend fun saveSession(session: CockpitSession) = withContext(Dispatchers.IO) {
        database.cockpitSessionQueries.insert(
            id = session.id,
            name = session.name,
            layoutMode = session.layoutMode.name,
            selectedFrameId = session.selectedFrameId,
            createdAt = session.createdAt,
            updatedAt = session.updatedAt,
            isDefault = if (session.isDefault) 1L else 0L
        )
    }

    override suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        database.cockpitSessionQueries.delete(sessionId)
    }

    // ── Frame CRUD ────────────────────────────────────────────────

    override suspend fun getFrames(sessionId: String): List<CockpitFrame> = withContext(Dispatchers.IO) {
        database.cockpitFrameQueries.getBySession(sessionId).executeAsList().map { row ->
            val content = deserializeContent(row.contentType, row.contentData)
            CockpitFrame(
                id = row.id,
                sessionId = row.sessionId,
                title = row.title,
                content = content,
                state = FrameState(
                    posX = row.posX.toFloat(),
                    posY = row.posY.toFloat(),
                    width = row.width.toFloat(),
                    height = row.height.toFloat(),
                    zOrder = row.zOrder.toInt(),
                    isMinimized = row.isMinimized == 1L,
                    isMaximized = row.isMaximized == 1L,
                    isVisible = row.isVisible == 1L
                ),
                createdAt = row.createdAt,
                updatedAt = row.updatedAt
            )
        }
    }

    override suspend fun saveFrame(frame: CockpitFrame) = withContext(Dispatchers.IO) {
        val contentJson = json.encodeToString(frame.content)
        database.cockpitFrameQueries.insert(
            id = frame.id,
            sessionId = frame.sessionId,
            contentType = frame.contentType,
            title = frame.title,
            posX = frame.state.posX.toDouble(),
            posY = frame.state.posY.toDouble(),
            width = frame.state.width.toDouble(),
            height = frame.state.height.toDouble(),
            zOrder = frame.state.zOrder.toLong(),
            isMinimized = if (frame.state.isMinimized) 1L else 0L,
            isMaximized = if (frame.state.isMaximized) 1L else 0L,
            isVisible = if (frame.state.isVisible) 1L else 0L,
            contentData = contentJson,
            createdAt = frame.createdAt,
            updatedAt = frame.updatedAt
        )
    }

    override suspend fun deleteFrame(frameId: String) = withContext(Dispatchers.IO) {
        database.cockpitFrameQueries.delete(frameId)
    }

    override suspend fun updateFrameContent(frameId: String, contentJson: String) = withContext(Dispatchers.IO) {
        val now = kotlinx.datetime.Clock.System.now().toString()
        database.cockpitFrameQueries.updateContent(
            contentData = contentJson,
            updatedAt = now,
            id = frameId
        )
    }

    // ── Workflow Steps ─────────────────────────────────────────────

    override suspend fun getWorkflowSteps(sessionId: String): List<WorkflowStep> = withContext(Dispatchers.IO) {
        database.cockpitWorkflowStepQueries.getBySession(sessionId).executeAsList().map { row ->
            WorkflowStep(
                id = row.id,
                sessionId = row.sessionId,
                frameId = row.frameId,
                stepNumber = row.stepNumber.toInt(),
                name = row.name,
                description = row.description
            )
        }
    }

    override suspend fun saveWorkflowStep(step: WorkflowStep) = withContext(Dispatchers.IO) {
        database.cockpitWorkflowStepQueries.insert(
            id = step.id,
            sessionId = step.sessionId,
            frameId = step.frameId,
            stepNumber = step.stepNumber.toLong(),
            name = step.name,
            description = step.description
        )
    }

    // ── Pinned Frames ─────────────────────────────────────────────

    override suspend fun getPinnedFrames(): List<PinnedFrame> = withContext(Dispatchers.IO) {
        database.cockpitPinnedFrameQueries.getAll().executeAsList().map { row ->
            PinnedFrame(
                frameId = row.frameId,
                sourceSessionId = row.sourceSessionId,
                pipX = row.pipX.toFloat(),
                pipY = row.pipY.toFloat(),
                pipScale = row.pipScale.toFloat(),
                opacity = row.opacity.toFloat()
            )
        }
    }

    override suspend fun pinFrame(pinnedFrame: PinnedFrame) = withContext(Dispatchers.IO) {
        database.cockpitPinnedFrameQueries.insert(
            frameId = pinnedFrame.frameId,
            sourceSessionId = pinnedFrame.sourceSessionId,
            pipX = pinnedFrame.pipX.toDouble(),
            pipY = pinnedFrame.pipY.toDouble(),
            pipScale = pinnedFrame.pipScale.toDouble(),
            opacity = pinnedFrame.opacity.toDouble()
        )
    }

    override suspend fun unpinFrame(frameId: String) = withContext(Dispatchers.IO) {
        database.cockpitPinnedFrameQueries.delete(frameId)
    }

    // ── Cross-Frame Links ─────────────────────────────────────────

    override suspend fun getCrossFrameLinks(sessionId: String): List<CrossFrameLink> = withContext(Dispatchers.IO) {
        database.cockpitCrossFrameLinkQueries.getBySession(sessionId).executeAsList().map { row ->
            CrossFrameLink(
                id = row.id,
                sourceFrameId = row.sourceFrameId,
                targetFrameId = row.targetFrameId,
                trigger = parseLinkTrigger(row.trigger),
                action = parseLinkAction(row.action),
                isEnabled = row.isEnabled == 1L
            )
        }
    }

    override suspend fun getLinksFromFrame(sourceFrameId: String): List<CrossFrameLink> = withContext(Dispatchers.IO) {
        database.cockpitCrossFrameLinkQueries.getBySourceFrame(sourceFrameId).executeAsList().map { row ->
            CrossFrameLink(
                id = row.id,
                sourceFrameId = row.sourceFrameId,
                targetFrameId = row.targetFrameId,
                trigger = parseLinkTrigger(row.trigger),
                action = parseLinkAction(row.action),
                isEnabled = row.isEnabled == 1L
            )
        }
    }

    override suspend fun saveCrossFrameLink(sessionId: String, link: CrossFrameLink) = withContext(Dispatchers.IO) {
        database.cockpitCrossFrameLinkQueries.insert(
            id = link.id,
            sessionId = sessionId,
            sourceFrameId = link.sourceFrameId,
            targetFrameId = link.targetFrameId,
            trigger = link.trigger.name,
            action = link.action.name,
            isEnabled = if (link.isEnabled) 1L else 0L
        )
    }

    override suspend fun deleteCrossFrameLink(linkId: String) = withContext(Dispatchers.IO) {
        database.cockpitCrossFrameLinkQueries.delete(linkId)
    }

    // ── Timeline Events ───────────────────────────────────────────

    override suspend fun getTimelineEvents(sessionId: String, limit: Int): List<TimelineEvent> = withContext(Dispatchers.IO) {
        database.cockpitTimelineEventQueries.getRecent(sessionId, limit.toLong()).executeAsList().map { row ->
            TimelineEvent(
                id = row.id,
                sessionId = row.sessionId,
                eventType = row.eventType,
                description = row.description,
                screenshotUri = row.screenshotUri,
                metadata = row.metadata,
                timestamp = row.timestamp
            )
        }
    }

    override suspend fun logEvent(event: TimelineEvent) = withContext(Dispatchers.IO) {
        database.cockpitTimelineEventQueries.insert(
            id = event.id,
            sessionId = event.sessionId,
            eventType = event.eventType,
            description = event.description,
            screenshotUri = event.screenshotUri,
            metadata = event.metadata,
            timestamp = event.timestamp
        )
    }

    override suspend fun clearOldEvents(beforeTimestamp: String) = withContext(Dispatchers.IO) {
        database.cockpitTimelineEventQueries.deleteOlderThan(beforeTimestamp)
    }

    // ── Import/Export ──────────────────────────────────────────────

    override suspend fun exportSession(sessionId: String): String = withContext(Dispatchers.IO) {
        val session = getSession(sessionId) ?: return@withContext "{}"
        val frames = getFrames(sessionId)
        val steps = getWorkflowSteps(sessionId)

        json.encodeToString(SessionExport(session, frames, steps))
    }

    override suspend fun importSession(jsonData: String): CockpitSession? = withContext(Dispatchers.IO) {
        try {
            val export = json.decodeFromString<SessionExport>(jsonData)
            val sourceSession = export.session
            val sourceFrames = export.frames
            val sourceSteps = export.steps

            // Generate fresh IDs so the imported session doesn't collide with existing ones
            val now = kotlinx.datetime.Clock.System.now()
            val newSessionId = "${now.toEpochMilliseconds()}_${(0..99999).random()}"

            val newSession = sourceSession.copy(
                id = newSessionId,
                isDefault = false,
                createdAt = now.toString(),
                updatedAt = now.toString()
            )
            saveSession(newSession)

            sourceFrames.forEach { frame ->
                val newFrameId = "${now.toEpochMilliseconds()}_${(0..99999).random()}"
                val newFrame = frame.copy(
                    id = newFrameId,
                    sessionId = newSessionId,
                    createdAt = now.toString(),
                    updatedAt = now.toString()
                )
                saveFrame(newFrame)
            }

            sourceSteps.forEach { step ->
                val newStepId = "${now.toEpochMilliseconds()}_${(0..99999).random()}"
                saveWorkflowStep(step.copy(id = newStepId, sessionId = newSessionId))
            }

            newSession
        } catch (e: Exception) {
            null
        }
    }

    // ── Private helpers ───────────────────────────────────────────

    private fun parseLayoutMode(name: String): LayoutMode =
        LayoutMode.entries.firstOrNull { it.name == name } ?: LayoutMode.FREEFORM

    private fun parseLinkTrigger(name: String): LinkTrigger =
        LinkTrigger.entries.firstOrNull { it.name == name } ?: LinkTrigger.SEND_TO

    private fun parseLinkAction(name: String): LinkAction =
        LinkAction.entries.firstOrNull { it.name == name } ?: LinkAction.COPY_CONTENT

    private fun deserializeContent(contentType: String, contentData: String): FrameContent {
        return try {
            json.decodeFromString<FrameContent>(contentData)
        } catch (e: Exception) {
            // Fallback: create default content based on type string
            when (contentType) {
                FrameContent.TYPE_WEB -> FrameContent.Web()
                FrameContent.TYPE_PDF -> FrameContent.Pdf()
                FrameContent.TYPE_IMAGE -> FrameContent.Image()
                FrameContent.TYPE_VIDEO -> FrameContent.Video()
                FrameContent.TYPE_NOTE -> FrameContent.Note()
                FrameContent.TYPE_CAMERA -> FrameContent.Camera()
                FrameContent.TYPE_VOICE_NOTE -> FrameContent.VoiceNote()
                FrameContent.TYPE_FORM -> FrameContent.Form()
                FrameContent.TYPE_SIGNATURE -> FrameContent.Signature()
                FrameContent.TYPE_MAP -> FrameContent.Map()
                FrameContent.TYPE_WHITEBOARD -> FrameContent.Whiteboard()
                FrameContent.TYPE_TERMINAL -> FrameContent.Terminal()
                FrameContent.TYPE_AI_SUMMARY -> FrameContent.AiSummary()
                FrameContent.TYPE_SCREEN_CAST -> FrameContent.ScreenCast()
                FrameContent.TYPE_WIDGET -> FrameContent.Widget()
                FrameContent.TYPE_EXTERNAL_APP -> FrameContent.ExternalApp()
                else -> FrameContent.Note()
            }
        }
    }
}
