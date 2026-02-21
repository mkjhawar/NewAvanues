package com.augmentalis.cockpit.viewmodel

import com.augmentalis.cockpit.CockpitConstants
import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.CockpitSession
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.cockpit.model.FrameState
import com.augmentalis.cockpit.model.LayoutMode
import com.augmentalis.cockpit.repository.ICockpitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * ViewModel managing the active Cockpit session, frames, layout, and persistence.
 *
 * Responsibilities:
 * - Session lifecycle (create, load, save, delete)
 * - Frame CRUD (add, remove, move, resize, update content)
 * - Layout mode switching
 * - Auto-save with debounce
 * - Z-order management (bring-to-front on select)
 *
 * This is a plain Kotlin class (not Android ViewModel) for KMP compatibility.
 * Android apps wrap this in an AndroidX ViewModel or Hilt-injected scope.
 */
class CockpitViewModel(
    private val repository: ICockpitRepository
) {
    private val viewModelJob = SupervisorJob()
    // Dispatchers.Main is not available on all KMP targets (e.g. Desktop JVM without a main
    // dispatcher library). Dispatchers.Default is safe on all platforms and the ViewModel does not
    // drive UI directly — it exposes StateFlows that platform UI layers collect on their own
    // main/UI dispatcher.
    private val scope = CoroutineScope(viewModelJob + Dispatchers.Default)
    private val _activeSession = MutableStateFlow<CockpitSession?>(null)
    val activeSession: StateFlow<CockpitSession?> = _activeSession.asStateFlow()

    private val _frames = MutableStateFlow<List<CockpitFrame>>(emptyList())
    val frames: StateFlow<List<CockpitFrame>> = _frames.asStateFlow()

    private val _selectedFrameId = MutableStateFlow<String?>(null)
    val selectedFrameId: StateFlow<String?> = _selectedFrameId.asStateFlow()

    private val _layoutMode = MutableStateFlow(LayoutMode.DEFAULT)
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()

    private val _sessions = MutableStateFlow<List<CockpitSession>>(emptyList())
    val sessions: StateFlow<List<CockpitSession>> = _sessions.asStateFlow()

    private var autoSaveJob: Job? = null
    private var nextZOrder = 0

    /**
     * Initialize by loading existing sessions or creating a default one.
     */
    fun initialize() {
        scope.launch {
            val existingSessions = repository.getSessions()
            _sessions.value = existingSessions

            val defaultSession = existingSessions.firstOrNull { it.isDefault }
                ?: existingSessions.firstOrNull()
                ?: createSession("Quick View", isDefault = true)

            loadSession(defaultSession.id)
        }
    }

    /**
     * Load a session and its frames from persistence.
     */
    fun loadSession(sessionId: String) {
        scope.launch {
            val session = repository.getSession(sessionId) ?: return@launch
            val sessionFrames = repository.getFrames(sessionId)

            _activeSession.value = session
            _frames.value = sessionFrames
            _layoutMode.value = session.layoutMode
            _selectedFrameId.value = session.selectedFrameId ?: sessionFrames.firstOrNull()?.id
            nextZOrder = (sessionFrames.maxOfOrNull { it.state.zOrder } ?: 0) + 1
        }
    }

    /**
     * Create a new session with an optional initial frame.
     * Suspends until the session is persisted to the database.
     */
    suspend fun createSession(name: String, isDefault: Boolean = false): CockpitSession {
        val now = Clock.System.now().toString()
        val session = CockpitSession(
            id = generateId(),
            name = name,
            layoutMode = LayoutMode.DEFAULT,
            isDefault = isDefault,
            createdAt = now,
            updatedAt = now
        )
        repository.saveSession(session)
        _sessions.value = repository.getSessions()
        return session
    }

    /**
     * Add a new frame with the given content type to the active session.
     */
    fun addFrame(content: FrameContent, title: String = "") {
        val session = _activeSession.value ?: return
        val currentFrames = _frames.value

        if (currentFrames.size >= CockpitConstants.MAX_FRAMES_PER_SESSION) return

        // Calculate initial position for new frame (cascade from top-left)
        val cascadeStep = 30f
        val cascadeIndex = currentFrames.size
        val initialX = (cascadeIndex * cascadeStep) % 200f
        val initialY = (cascadeIndex * cascadeStep) % 150f

        val now = Clock.System.now().toString()
        val frame = CockpitFrame(
            id = generateId(),
            sessionId = session.id,
            title = title,
            content = content,
            state = FrameState(
                posX = initialX,
                posY = initialY,
                width = CockpitConstants.DEFAULT_FRAME_WIDTH.toFloat(),
                height = CockpitConstants.DEFAULT_FRAME_HEIGHT.toFloat(),
                zOrder = nextZOrder++
            ),
            createdAt = now,
            updatedAt = now
        )

        _frames.value = currentFrames + frame
        _selectedFrameId.value = frame.id

        scheduleAutoSave()
    }

    /**
     * Remove a frame from the active session.
     */
    fun removeFrame(frameId: String) {
        _frames.value = _frames.value.filter { it.id != frameId }
        if (_selectedFrameId.value == frameId) {
            _selectedFrameId.value = _frames.value.lastOrNull()?.id
        }
        scope.launch { repository.deleteFrame(frameId) }
    }

    /**
     * Select/focus a frame (brings it to front in freeform mode).
     */
    fun selectFrame(frameId: String) {
        _selectedFrameId.value = frameId

        // Bring to front: update z-order
        _frames.value = _frames.value.map { frame ->
            if (frame.id == frameId) {
                frame.copy(
                    state = frame.state.copy(zOrder = nextZOrder++),
                    updatedAt = Clock.System.now().toString()
                )
            } else frame
        }

        scheduleAutoSave()
    }

    /**
     * Move a frame to a new position (freeform mode).
     */
    fun moveFrame(frameId: String, newX: Float, newY: Float) {
        _frames.value = _frames.value.map { frame ->
            if (frame.id == frameId) {
                frame.copy(
                    state = frame.state.copy(posX = newX, posY = newY),
                    updatedAt = Clock.System.now().toString()
                )
            } else frame
        }
        scheduleAutoSave()
    }

    /**
     * Resize a frame to new dimensions (freeform mode).
     */
    fun resizeFrame(frameId: String, newWidth: Float, newHeight: Float) {
        _frames.value = _frames.value.map { frame ->
            if (frame.id == frameId) {
                frame.copy(
                    state = frame.state.copy(width = newWidth, height = newHeight),
                    updatedAt = Clock.System.now().toString()
                )
            } else frame
        }
        scheduleAutoSave()
    }

    /**
     * Toggle minimize state for a frame.
     */
    fun toggleMinimize(frameId: String) {
        _frames.value = _frames.value.map { frame ->
            if (frame.id == frameId) {
                frame.copy(
                    state = frame.state.copy(
                        isMinimized = !frame.state.isMinimized,
                        isMaximized = false // un-maximize if minimizing
                    ),
                    updatedAt = Clock.System.now().toString()
                )
            } else frame
        }
        scheduleAutoSave()
    }

    /**
     * Toggle maximize state for a frame.
     */
    fun toggleMaximize(frameId: String) {
        _frames.value = _frames.value.map { frame ->
            if (frame.id == frameId) {
                frame.copy(
                    state = frame.state.copy(
                        isMaximized = !frame.state.isMaximized,
                        isMinimized = false // un-minimize if maximizing
                    ),
                    updatedAt = Clock.System.now().toString()
                )
            } else frame
        }
        scheduleAutoSave()
    }

    /**
     * Update the content for a frame (e.g., current URL, page number).
     * Updates in-memory state AND schedules auto-save to persist.
     */
    fun updateFrameContent(frameId: String, newContent: FrameContent) {
        _frames.value = _frames.value.map { frame ->
            if (frame.id == frameId) {
                frame.copy(
                    content = newContent,
                    updatedAt = Clock.System.now().toString()
                )
            } else frame
        }
        scheduleAutoSave()
    }

    /**
     * Switch the layout mode for the active session.
     */
    fun setLayoutMode(mode: LayoutMode) {
        _layoutMode.value = mode
        val updatedSession = _activeSession.value?.copy(
            layoutMode = mode,
            updatedAt = Clock.System.now().toString()
        ) ?: return
        _activeSession.value = updatedSession
        _sessions.value = _sessions.value.map { s ->
            if (s.id == updatedSession.id) updatedSession else s
        }
        scheduleAutoSave()
    }

    /**
     * Rename the active session.
     */
    fun renameSession(newName: String) {
        val updatedSession = _activeSession.value?.copy(
            name = newName,
            updatedAt = Clock.System.now().toString()
        ) ?: return
        _activeSession.value = updatedSession
        _sessions.value = _sessions.value.map { s ->
            if (s.id == updatedSession.id) updatedSession else s
        }
        scheduleAutoSave()
    }

    /**
     * Delete a session and all its frames.
     */
    fun deleteSession(sessionId: String) {
        scope.launch {
            repository.deleteSession(sessionId)
            _sessions.value = repository.getSessions()

            if (_activeSession.value?.id == sessionId) {
                val remaining = _sessions.value
                if (remaining.isNotEmpty()) {
                    loadSession(remaining.first().id)
                } else {
                    val newSession = createSession("Quick View", isDefault = true)
                    loadSession(newSession.id)
                }
            }
        }
    }

    /**
     * Debounced auto-save: waits 500ms after last change, then persists everything.
     */
    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            delay(CockpitConstants.AUTO_SAVE_DEBOUNCE_MS)
            save()
        }
    }

    /**
     * Persist the current session and all frames to the database.
     * Syncs selectedFrameId from the authoritative StateFlow before saving.
     */
    private suspend fun save() {
        val currentSelectedId = _selectedFrameId.value
        val session = _activeSession.value?.copy(
            selectedFrameId = currentSelectedId
        ) ?: return
        _activeSession.value = session
        repository.saveSession(session)
        _frames.value.forEach { frame ->
            repository.saveFrame(frame)
        }
    }

    /**
     * Force an immediate save (e.g., on app pause/stop).
     */
    fun saveNow() {
        scope.launch { save() }
    }

    /**
     * Release all resources and cancel pending coroutines.
     * Must be called when the ViewModel is no longer needed.
     */
    fun dispose() {
        autoSaveJob?.cancel()
        viewModelJob.cancel()
    }

    /**
     * Generate a unique ID for sessions/frames.
     * Uses timestamp + random Long (base-36) for high entropy without UUID dependency.
     */
    private fun generateId(): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val random = Random.nextLong(0, Long.MAX_VALUE)
        return "${timestamp}_${random.toString(36)}"
    }
}
