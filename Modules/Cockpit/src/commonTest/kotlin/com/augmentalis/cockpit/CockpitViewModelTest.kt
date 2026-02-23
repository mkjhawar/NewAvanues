/**
 * CockpitViewModelTest.kt — Unit tests for CockpitViewModel
 *
 * Covers session lifecycle, frame CRUD, layout switching, module launching,
 * template instantiation, special module routing, and overlapping load
 * cancellation. Uses FakeCockpitRepository for deterministic in-memory
 * persistence and UnconfinedTestDispatcher for synchronous coroutine execution.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-24
 */
package com.augmentalis.cockpit

import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.CockpitSession
import com.augmentalis.cockpit.model.CrossFrameLink
import com.augmentalis.cockpit.model.DashboardModuleRegistry
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.cockpit.model.FrameState
import com.augmentalis.cockpit.model.LayoutMode
import com.augmentalis.cockpit.model.PinnedFrame
import com.augmentalis.cockpit.model.TimelineEvent
import com.augmentalis.cockpit.model.WorkflowStep
import com.augmentalis.cockpit.repository.ICockpitRepository
import com.augmentalis.cockpit.viewmodel.CockpitViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// ── Fake Repository ────────────────────────────────────────────────────────

/**
 * In-memory ICockpitRepository for testing. Implements the core session/frame
 * CRUD methods used by CockpitViewModel. Methods not called by the ViewModel
 * (workflow steps, pinned frames, cross-frame links, timeline, bulk ops)
 * return empty defaults.
 */
class FakeCockpitRepository : ICockpitRepository {

    val sessions = mutableListOf<CockpitSession>()
    val allFrames = mutableListOf<CockpitFrame>()

    var saveSessionCallCount = 0
    var saveFrameCallCount = 0

    override suspend fun getSessions(): List<CockpitSession> =
        sessions.sortedByDescending { it.updatedAt }

    override suspend fun getSession(sessionId: String): CockpitSession? =
        sessions.find { it.id == sessionId }

    override suspend fun saveSession(session: CockpitSession) {
        saveSessionCallCount++
        sessions.removeAll { it.id == session.id }
        sessions.add(session)
    }

    override suspend fun deleteSession(sessionId: String) {
        sessions.removeAll { it.id == sessionId }
        allFrames.removeAll { it.sessionId == sessionId }
    }

    override suspend fun getFrames(sessionId: String): List<CockpitFrame> =
        allFrames.filter { it.sessionId == sessionId }.sortedBy { it.state.zOrder }

    override suspend fun saveFrame(frame: CockpitFrame) {
        saveFrameCallCount++
        allFrames.removeAll { it.id == frame.id }
        allFrames.add(frame)
    }

    override suspend fun deleteFrame(frameId: String) {
        allFrames.removeAll { it.id == frameId }
    }

    override suspend fun updateFrameContent(frameId: String, contentJson: String) {}

    // ── Not used by CockpitViewModel ──────────────────────────────────────
    override suspend fun getWorkflowSteps(sessionId: String): List<WorkflowStep> = emptyList()
    override suspend fun saveWorkflowStep(step: WorkflowStep) {}
    override suspend fun getPinnedFrames(): List<PinnedFrame> = emptyList()
    override suspend fun pinFrame(pinnedFrame: PinnedFrame) {}
    override suspend fun unpinFrame(frameId: String) {}
    override suspend fun getCrossFrameLinks(sessionId: String): List<CrossFrameLink> = emptyList()
    override suspend fun getLinksFromFrame(sourceFrameId: String): List<CrossFrameLink> = emptyList()
    override suspend fun saveCrossFrameLink(sessionId: String, link: CrossFrameLink) {}
    override suspend fun deleteCrossFrameLink(linkId: String) {}
    override suspend fun getTimelineEvents(sessionId: String, limit: Int): List<TimelineEvent> = emptyList()
    override suspend fun logEvent(event: TimelineEvent) {}
    override suspend fun clearOldEvents(beforeTimestamp: String) {}
    override suspend fun exportSession(sessionId: String): String = "{}"
    override suspend fun importSession(json: String): CockpitSession? = null
}

// ── ViewModel Tests ────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class CockpitViewModelTest {

    // ── Initialize ─────────────────────────────────────────────────────────

    @Test
    fun `initialize loads existing sessions and sets DASHBOARD mode`() = runTest {
        val repo = FakeCockpitRepository()
        repo.sessions.add(CockpitSession(id = "s1", name = "Old Session", updatedAt = "2026-01-01"))
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))

        vm.initialize()

        assertEquals(LayoutMode.DASHBOARD, vm.layoutMode.value)
        assertEquals(1, vm.sessions.value.size)
        assertEquals("Old Session", vm.sessions.value.first().name)
        vm.dispose()
    }

    @Test
    fun `initialize with no sessions shows empty dashboard`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))

        vm.initialize()

        assertEquals(LayoutMode.DASHBOARD, vm.layoutMode.value)
        assertTrue(vm.sessions.value.isEmpty())
        assertNull(vm.activeSession.value)
        vm.dispose()
    }

    // ── Launch Module ──────────────────────────────────────────────────────

    @Test
    fun `launchModule creates session with frame in FULLSCREEN`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))

        vm.launchModule("webavanue")

        assertNotNull(vm.activeSession.value)
        assertEquals("WebAvanue", vm.activeSession.value!!.name)
        assertEquals(1, vm.frames.value.size)
        assertTrue(vm.frames.value.first().content is FrameContent.Web)
        assertEquals(LayoutMode.FULLSCREEN, vm.layoutMode.value)
        vm.dispose()
    }

    @Test
    fun `launchModule with PDF creates PDF frame`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))

        vm.launchModule("pdfavanue")

        assertEquals(1, vm.frames.value.size)
        assertTrue(vm.frames.value.first().content is FrameContent.Pdf)
        assertEquals("PDFAvanue", vm.activeSession.value?.name)
        vm.dispose()
    }

    @Test
    fun `launchModule with camera creates Camera frame`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))

        vm.launchModule("photoavanue")

        assertEquals(1, vm.frames.value.size)
        assertTrue(vm.frames.value.first().content is FrameContent.Camera)
        vm.dispose()
    }

    @Test
    fun `launchModule with unknown id does nothing`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))

        vm.launchModule("nonexistent")

        assertNull(vm.activeSession.value)
        assertTrue(vm.frames.value.isEmpty())
        vm.dispose()
    }

    @Test
    fun `launchModule with cursor emits specialModuleLaunch`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        val emitted = mutableListOf<String>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.specialModuleLaunch.collect { emitted.add(it) }
        }

        vm.launchModule("voicecursor")

        assertEquals(1, emitted.size)
        assertEquals("voicecursor", emitted.first())
        assertNull(vm.activeSession.value) // No session created for cursor
        job.cancel()
        vm.dispose()
    }

    // ── Resume Session ─────────────────────────────────────────────────────

    @Test
    fun `resumeSession loads session and frames from repository`() = runTest {
        val repo = FakeCockpitRepository()
        val session = CockpitSession(id = "s1", name = "Existing", layoutMode = LayoutMode.GRID)
        repo.sessions.add(session)
        repo.allFrames.add(
            CockpitFrame(
                id = "f1", sessionId = "s1", title = "Web",
                content = FrameContent.Web(), state = FrameState(zOrder = 1)
            )
        )
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))

        vm.resumeSession("s1")

        assertEquals("s1", vm.activeSession.value?.id)
        assertEquals(LayoutMode.GRID, vm.layoutMode.value)
        assertEquals(1, vm.frames.value.size)
        assertEquals("f1", vm.selectedFrameId.value)
        vm.dispose()
    }

    @Test
    fun `resumeSession with nonexistent id leaves state unchanged`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.initialize()

        vm.resumeSession("nonexistent")

        assertNull(vm.activeSession.value)
        vm.dispose()
    }

    // ── Launch Template ────────────────────────────────────────────────────

    @Test
    fun `launchTemplate creates session with template frames and layout`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))

        vm.launchTemplate("tmpl_research")

        assertNotNull(vm.activeSession.value)
        assertEquals("Research", vm.activeSession.value?.name)
        assertEquals(LayoutMode.SPLIT_LEFT, vm.layoutMode.value)
        // Research template: 3 Web + 1 Note = 4 frames
        assertEquals(4, vm.frames.value.size)
        vm.dispose()
    }

    @Test
    fun `launchTemplate with unknown id does nothing`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))

        vm.launchTemplate("nonexistent")

        assertNull(vm.activeSession.value)
        assertTrue(vm.frames.value.isEmpty())
        vm.dispose()
    }

    // ── Return to Dashboard ────────────────────────────────────────────────

    @Test
    fun `returnToDashboard saves session and clears state`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue")
        val sessionId = vm.activeSession.value!!.id

        vm.returnToDashboard()

        assertEquals(LayoutMode.DASHBOARD, vm.layoutMode.value)
        assertNull(vm.activeSession.value)
        assertTrue(vm.frames.value.isEmpty())
        assertNull(vm.selectedFrameId.value)
        // Session should still exist in repository
        assertNotNull(repo.getSession(sessionId))
        vm.dispose()
    }

    // ── Add Frame ──────────────────────────────────────────────────────────

    @Test
    fun `addFrame adds frame to active session`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue") // Creates session + 1 frame

        vm.addFrame(FrameContent.Note(), "My Note")

        assertEquals(2, vm.frames.value.size)
        assertTrue(vm.frames.value.last().content is FrameContent.Note)
        assertEquals("My Note", vm.frames.value.last().title)
        vm.dispose()
    }

    @Test
    fun `addFrame without active session does nothing`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))

        vm.addFrame(FrameContent.Web(), "Web")

        assertTrue(vm.frames.value.isEmpty())
        vm.dispose()
    }

    @Test
    fun `addFrame respects MAX_FRAMES limit`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue") // 1 frame

        // Try to add MAX - 1 more (since we already have 1)
        repeat(CockpitConstants.MAX_FRAMES_PER_SESSION) {
            vm.addFrame(FrameContent.Note(), "Note $it")
        }

        assertEquals(CockpitConstants.MAX_FRAMES_PER_SESSION, vm.frames.value.size)
        vm.dispose()
    }

    // ── Remove Frame ───────────────────────────────────────────────────────

    @Test
    fun `removeFrame removes frame from list`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue") // 1 frame
        vm.addFrame(FrameContent.Note(), "Note") // 2 frames
        val firstId = vm.frames.value.first().id

        vm.removeFrame(vm.frames.value.last().id)

        assertEquals(1, vm.frames.value.size)
        assertEquals(firstId, vm.frames.value.first().id)
        vm.dispose()
    }

    @Test
    fun `removeFrame updates selection when selected frame is removed`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue")
        val frameId = vm.frames.value.first().id
        assertEquals(frameId, vm.selectedFrameId.value)

        vm.removeFrame(frameId)

        assertTrue(vm.frames.value.isEmpty())
        assertNull(vm.selectedFrameId.value)
        vm.dispose()
    }

    // ── Select Frame ───────────────────────────────────────────────────────

    @Test
    fun `selectFrame updates selection and bumps z-order`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue")
        vm.addFrame(FrameContent.Note(), "Note")
        val frame1Id = vm.frames.value.first().id
        val frame2ZBefore = vm.frames.value.last().state.zOrder

        vm.selectFrame(frame1Id)

        assertEquals(frame1Id, vm.selectedFrameId.value)
        val frame1 = vm.frames.value.first { it.id == frame1Id }
        assertTrue(frame1.state.zOrder > frame2ZBefore)
        vm.dispose()
    }

    // ── Move / Resize ──────────────────────────────────────────────────────

    @Test
    fun `moveFrame updates position`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue")
        val frameId = vm.frames.value.first().id

        vm.moveFrame(frameId, 100f, 200f)

        val frame = vm.frames.value.first()
        assertEquals(100f, frame.state.posX)
        assertEquals(200f, frame.state.posY)
        vm.dispose()
    }

    @Test
    fun `resizeFrame updates dimensions`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue")
        val frameId = vm.frames.value.first().id

        vm.resizeFrame(frameId, 500f, 400f)

        val frame = vm.frames.value.first()
        assertEquals(500f, frame.state.width)
        assertEquals(400f, frame.state.height)
        vm.dispose()
    }

    // ── Toggle Minimize / Maximize ─────────────────────────────────────────

    @Test
    fun `toggleMinimize sets minimized and clears maximized`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue")
        val frameId = vm.frames.value.first().id
        vm.toggleMaximize(frameId) // Maximize first

        vm.toggleMinimize(frameId)

        val frame = vm.frames.value.first()
        assertTrue(frame.state.isMinimized)
        assertFalse(frame.state.isMaximized)
        vm.dispose()
    }

    @Test
    fun `toggleMaximize sets maximized and clears minimized`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue")
        val frameId = vm.frames.value.first().id
        vm.toggleMinimize(frameId) // Minimize first

        vm.toggleMaximize(frameId)

        val frame = vm.frames.value.first()
        assertTrue(frame.state.isMaximized)
        assertFalse(frame.state.isMinimized)
        vm.dispose()
    }

    @Test
    fun `toggleMinimize is idempotent on double toggle`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue")
        val frameId = vm.frames.value.first().id

        vm.toggleMinimize(frameId)
        assertTrue(vm.frames.value.first().state.isMinimized)

        vm.toggleMinimize(frameId)
        assertFalse(vm.frames.value.first().state.isMinimized)
        vm.dispose()
    }

    // ── Layout Mode ────────────────────────────────────────────────────────

    @Test
    fun `setLayoutMode updates layout and active session`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue")

        vm.setLayoutMode(LayoutMode.GRID)

        assertEquals(LayoutMode.GRID, vm.layoutMode.value)
        assertEquals(LayoutMode.GRID, vm.activeSession.value?.layoutMode)
        vm.dispose()
    }

    @Test
    fun `setLayoutMode without active session only updates flow`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))

        vm.setLayoutMode(LayoutMode.FREEFORM)

        assertEquals(LayoutMode.FREEFORM, vm.layoutMode.value)
        assertNull(vm.activeSession.value)
        vm.dispose()
    }

    // ── Update Frame Content ───────────────────────────────────────────────

    @Test
    fun `updateFrameContent changes content in place`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue")
        val frameId = vm.frames.value.first().id

        vm.updateFrameContent(frameId, FrameContent.Web(url = "https://example.com"))

        val content = vm.frames.value.first().content as FrameContent.Web
        assertEquals("https://example.com", content.url)
        vm.dispose()
    }

    // ── Delete Session ─────────────────────────────────────────────────────

    @Test
    fun `deleteSession of active session loads next session`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        // Create two sessions
        vm.launchModule("webavanue")
        val session1Id = vm.activeSession.value!!.id
        vm.returnToDashboard()
        vm.launchModule("pdfavanue")
        val session2Id = vm.activeSession.value!!.id

        vm.deleteSession(session2Id)

        // Should have loaded the remaining session
        assertNotNull(vm.activeSession.value)
        assertEquals(session1Id, vm.activeSession.value!!.id)
        vm.dispose()
    }

    @Test
    fun `deleteSession of non-active session preserves active`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue")
        val activeId = vm.activeSession.value!!.id
        // Add another session directly to repo
        repo.sessions.add(CockpitSession(id = "other", name = "Other"))

        vm.deleteSession("other")

        assertEquals(activeId, vm.activeSession.value?.id)
        vm.dispose()
    }

    // ── Rename Session ─────────────────────────────────────────────────────

    @Test
    fun `renameSession updates active session name`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue")

        vm.renameSession("New Name")

        assertEquals("New Name", vm.activeSession.value?.name)
        vm.dispose()
    }

    @Test
    fun `renameSession without active session does nothing`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))

        vm.renameSession("No Effect")

        assertNull(vm.activeSession.value)
        vm.dispose()
    }

    // ── Dispose ────────────────────────────────────────────────────────────

    @Test
    fun `dispose cancels all coroutines without crash`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.initialize()
        vm.launchModule("webavanue")

        vm.dispose()

        // After dispose, state is stale but accessing it should not crash
        assertNotNull(vm.activeSession.value)
    }

    // ── Content Type Mapping ───────────────────────────────────────────────

    @Test
    fun `all DashboardModuleRegistry modules launch correctly`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        val specialEmitted = mutableListOf<String>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.specialModuleLaunch.collect { specialEmitted.add(it) }
        }

        for (module in DashboardModuleRegistry.allModules) {
            vm.launchModule(module.id)

            if (module.contentType == "cursor") {
                // Cursor is handled via specialModuleLaunch — no session
                continue
            }

            assertNotNull(
                vm.activeSession.value,
                "Module ${module.id} should create a session"
            )
            assertTrue(
                vm.frames.value.isNotEmpty(),
                "Module ${module.id} should add a frame"
            )

            vm.returnToDashboard()
        }

        // voicecursor should have been emitted as a special module
        assertTrue(specialEmitted.contains("voicecursor"))
        job.cancel()
        vm.dispose()
    }

    // ── Session Persists Across Return-to-Dashboard Cycle ──────────────────

    @Test
    fun `session persists after returnToDashboard and can be resumed`() = runTest {
        val repo = FakeCockpitRepository()
        val vm = CockpitViewModel(repo, UnconfinedTestDispatcher(testScheduler))
        vm.launchModule("webavanue")
        val sessionId = vm.activeSession.value!!.id
        vm.addFrame(FrameContent.Note(), "Note")
        assertEquals(2, vm.frames.value.size)

        vm.returnToDashboard()
        assertNull(vm.activeSession.value)

        vm.resumeSession(sessionId)

        assertEquals(sessionId, vm.activeSession.value?.id)
        // Note: frames loaded from repo — only the initial frame was saved via auto-save
        // The in-memory frames were cleared on returnToDashboard, so repo has what was saved
        vm.dispose()
    }
}
