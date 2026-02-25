package com.augmentalis.cockpit.viewmodel

import com.augmentalis.cockpit.CockpitConstants
import com.augmentalis.cockpit.model.BuiltInTemplates
import com.augmentalis.cockpit.model.SimplifiedShellMode
import com.augmentalis.cockpit.ui.BackgroundScene
import com.augmentalis.cockpit.ui.ContentAction
import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.CockpitSession
import com.augmentalis.cockpit.model.DashboardModuleRegistry
import com.augmentalis.cockpit.model.DashboardState
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.cockpit.model.FrameState
import com.augmentalis.cockpit.model.LayoutMode
import com.augmentalis.cockpit.repository.ICockpitRepository
import com.augmentalis.speechrecognition.SpeechMetricsSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

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
    private val repository: ICockpitRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val viewModelJob = SupervisorJob()
    // Dispatchers.Default is safe on all KMP targets and the ViewModel does not drive UI directly
    // — it exposes StateFlows that platform UI layers collect on their own main/UI dispatcher.
    // Tests can inject a TestDispatcher for deterministic execution.
    private val scope = CoroutineScope(viewModelJob + dispatcher)
    private val _activeSession = MutableStateFlow<CockpitSession?>(null)
    val activeSession: StateFlow<CockpitSession?> = _activeSession.asStateFlow()

    private val _frames = MutableStateFlow<List<CockpitFrame>>(emptyList())
    val frames: StateFlow<List<CockpitFrame>> = _frames.asStateFlow()

    private val _selectedFrameId = MutableStateFlow<String?>(null)
    val selectedFrameId: StateFlow<String?> = _selectedFrameId.asStateFlow()

    private val _layoutMode = MutableStateFlow(LayoutMode.DEFAULT)
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()

    private val _shellMode = MutableStateFlow(SimplifiedShellMode.DEFAULT)
    val shellMode: StateFlow<SimplifiedShellMode> = _shellMode.asStateFlow()

    private val _sessions = MutableStateFlow<List<CockpitSession>>(emptyList())
    val sessions: StateFlow<List<CockpitSession>> = _sessions.asStateFlow()

    private var autoSaveJob: Job? = null
    private var activeLoadJob: Job? = null
    private var dashboardCollectionJob: Job? = null
    private var nextZOrder = 0

    private val _backgroundScene = MutableStateFlow(BackgroundScene.GRADIENT)
    val backgroundScene: StateFlow<BackgroundScene> = _backgroundScene.asStateFlow()

    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    // Speech engine metrics — pushed by platform bridge collecting from engine StateFlow
    private val _speechMetrics = MutableStateFlow<SpeechMetricsSnapshot?>(null)
    val speechMetrics: StateFlow<SpeechMetricsSnapshot?> = _speechMetrics.asStateFlow()

    /** Emits module IDs that don't map to frame content (e.g. "voicecursor")
     *  and need special navigation handling by the platform layer. */
    private val _specialModuleLaunch = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val specialModuleLaunch: SharedFlow<String> = _specialModuleLaunch.asSharedFlow()

    /** Emits content-specific actions (web back/forward, PDF page turn, etc.)
     *  targeted at the currently selected frame's content renderer. */
    private val _contentAction = MutableSharedFlow<ContentAction>(extraBufferCapacity = 1)
    val contentAction: SharedFlow<ContentAction> = _contentAction.asSharedFlow()

    init {
        // Derive dashboard state from sessions + active session + speech metrics
        dashboardCollectionJob = scope.launch {
            combine(_sessions, _activeSession, _speechMetrics) { sessions, active, metrics ->
                DashboardState(
                    recentSessions = sessions.take(8),
                    availableModules = DashboardModuleRegistry.allModules,
                    activeSession = active,
                    templates = BuiltInTemplates.ALL,
                    isLoading = false,
                    speechMetrics = metrics
                )
            }.collect { _dashboardState.value = it }
        }
    }

    /**
     * Initialize by loading existing sessions and showing the Dashboard.
     * The Dashboard is the home view; the user selects a session from there
     * or launches a new module.
     */
    fun initialize() {
        scope.launch {
            val existingSessions = repository.getSessions()
            _sessions.value = existingSessions
            // Start on the Dashboard — no session loaded yet
            _layoutMode.value = LayoutMode.DASHBOARD
        }
    }

    /**
     * Load a session and its frames from persistence.
     * Public API — fires and forgets. Use [loadSessionInternal] when you need
     * to await completion within an existing coroutine (e.g. launchModule).
     */
    fun loadSession(sessionId: String) {
        activeLoadJob?.cancel()
        activeLoadJob = scope.launch { loadSessionInternal(sessionId) }
    }

    /**
     * Suspend version of session loading — awaits until session state is fully
     * populated. Must be called from a coroutine (scope.launch or another suspend fun).
     */
    private suspend fun loadSessionInternal(sessionId: String) {
        val session = repository.getSession(sessionId) ?: return
        val sessionFrames = repository.getFrames(sessionId)

        _activeSession.value = session
        _frames.value = sessionFrames
        _layoutMode.value = session.layoutMode
        _selectedFrameId.value = session.selectedFrameId ?: sessionFrames.firstOrNull()?.id
        nextZOrder = (sessionFrames.maxOfOrNull { it.state.zOrder } ?: 0) + 1
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
        autoSaveJob?.cancel() // Prevent in-flight auto-save from re-inserting deleted frame
        _frames.value = _frames.value.filter { it.id != frameId }
        if (_selectedFrameId.value == frameId) {
            _selectedFrameId.value = _frames.value.lastOrNull()?.id
        }
        scope.launch { repository.deleteFrame(frameId) }
        scheduleAutoSave()
    }

    /**
     * Apply a transform to a single frame by ID, stamp updatedAt, and auto-save.
     *
     * Centralizes the map-match-copy-save pattern that was duplicated across
     * selectFrame, moveFrame, resizeFrame, toggleMinimize, toggleMaximize,
     * updateFrameContent, and renameFrame.
     */
    private fun updateFrame(frameId: String, transform: (CockpitFrame) -> CockpitFrame) {
        _frames.value = _frames.value.map { frame ->
            if (frame.id == frameId) transform(frame).copy(updatedAt = Clock.System.now().toString())
            else frame
        }
        scheduleAutoSave()
    }

    /**
     * Select/focus a frame (brings it to front in freeform mode).
     */
    fun selectFrame(frameId: String) {
        _selectedFrameId.value = frameId
        updateFrame(frameId) { frame ->
            frame.copy(state = frame.state.copy(zOrder = nextZOrder++))
        }
    }

    /**
     * Move a frame to a new position (freeform mode).
     */
    fun moveFrame(frameId: String, newX: Float, newY: Float) {
        updateFrame(frameId) { frame ->
            frame.copy(state = frame.state.copy(posX = newX, posY = newY))
        }
    }

    /**
     * Resize a frame to new dimensions (freeform mode).
     */
    fun resizeFrame(frameId: String, newWidth: Float, newHeight: Float) {
        updateFrame(frameId) { frame ->
            frame.copy(state = frame.state.copy(width = newWidth, height = newHeight))
        }
    }

    /**
     * Toggle minimize state for a frame.
     */
    fun toggleMinimize(frameId: String) {
        updateFrame(frameId) { frame ->
            frame.copy(state = frame.state.copy(
                isMinimized = !frame.state.isMinimized,
                isMaximized = false
            ))
        }
    }

    /**
     * Toggle maximize state for a frame.
     */
    fun toggleMaximize(frameId: String) {
        updateFrame(frameId) { frame ->
            frame.copy(state = frame.state.copy(
                isMaximized = !frame.state.isMaximized,
                isMinimized = false
            ))
        }
    }

    /**
     * Update the content for a frame (e.g., current URL, page number).
     * Updates in-memory state AND schedules auto-save to persist.
     */
    fun updateFrameContent(frameId: String, newContent: FrameContent) {
        updateFrame(frameId) { frame ->
            frame.copy(content = newContent)
        }
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
     * Switch the home screen shell variation (Classic, AvanueViews, Lens, Canvas).
     * Also returns to DASHBOARD layout mode to show the home screen.
     */
    fun setShellMode(mode: SimplifiedShellMode) {
        _shellMode.value = mode
        // Return to dashboard to show the home screen with the new shell
        if (_layoutMode.value != LayoutMode.DASHBOARD) {
            _layoutMode.value = LayoutMode.DASHBOARD
        }
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
        activeLoadJob?.cancel()
        activeLoadJob = scope.launch {
            repository.deleteSession(sessionId)
            _sessions.value = repository.getSessions()

            if (_activeSession.value?.id == sessionId) {
                val remaining = _sessions.value
                if (remaining.isNotEmpty()) {
                    loadSessionInternal(remaining.first().id)
                } else {
                    val newSession = createSession("Quick View", isDefault = true)
                    loadSessionInternal(newSession.id)
                }
            }
        }
    }

    // ── Workflow CRUD ─────────────────────────────────────────────────

    /**
     * Rename a frame's title (used in workflow step editing).
     */
    fun renameFrame(frameId: String, newTitle: String) {
        updateFrame(frameId) { frame ->
            frame.copy(title = newTitle)
        }
    }

    /**
     * Reorder a frame by moving it [delta] positions in the list.
     * Positive delta moves toward the end, negative toward the start.
     */
    fun reorderFrame(frameId: String, delta: Int) {
        val currentFrames = _frames.value.toMutableList()
        val currentIndex = currentFrames.indexOfFirst { it.id == frameId }
        if (currentIndex < 0) return

        val newIndex = (currentIndex + delta).coerceIn(0, currentFrames.lastIndex)
        if (newIndex == currentIndex) return

        val frame = currentFrames.removeAt(currentIndex)
        currentFrames.add(newIndex, frame)
        _frames.value = currentFrames
        scheduleAutoSave()
    }

    // ── Speech Metrics ─────────────────────────────────────────────────

    /**
     * Update the speech engine metrics displayed on the Dashboard.
     * Called by platform bridges that collect from engine StateFlows
     * (e.g. WhisperEngine.metricsSnapshot, GoogleCloudEngine.metricsSnapshot).
     */
    fun updateSpeechMetrics(snapshot: SpeechMetricsSnapshot?) {
        _speechMetrics.value = snapshot
    }

    // ── Dashboard Operations ──────────────────────────────────────────

    /**
     * Launch a module from the Dashboard — creates a new session named after
     * the module, adds a single frame with the corresponding content type,
     * and switches to FULLSCREEN layout.
     */
    fun launchModule(moduleId: String) {
        val module = DashboardModuleRegistry.findById(moduleId) ?: return
        val content = contentForType(module.contentType)
        if (content == null) {
            // Non-frame module (e.g. CursorAvanue) — don't create a session,
            // emit to specialModuleLaunch so the platform layer can navigate.
            _specialModuleLaunch.tryEmit(moduleId)
            return
        }
        activeLoadJob?.cancel()
        activeLoadJob = scope.launch {
            val session = createSession(module.displayName)
            loadSessionInternal(session.id)
            addFrame(content, module.displayName)
            setLayoutMode(LayoutMode.FULLSCREEN)
        }
    }

    /**
     * Resume a session from the Dashboard by loading it and switching
     * to its stored layout mode.
     */
    fun resumeSession(sessionId: String) {
        activeLoadJob?.cancel()
        activeLoadJob = scope.launch {
            loadSessionInternal(sessionId)
        }
    }

    /**
     * Launch a session from a template — creates a new session with the
     * template's layout mode and pre-configured frames.
     */
    fun launchTemplate(templateId: String) {
        val template = BuiltInTemplates.ALL.firstOrNull { it.id == templateId } ?: return
        activeLoadJob?.cancel()
        activeLoadJob = scope.launch {
            val session = createSession(template.name)
            loadSessionInternal(session.id)
            setLayoutMode(template.layoutMode)

            for (def in template.frameDefinitions) {
                val content = contentForType(def.contentType)
                if (content != null) {
                    addFrame(content, def.title)
                }
            }
        }
    }

    /**
     * Navigate back to the Dashboard — saves the current session and
     * switches to DASHBOARD layout mode.
     */
    fun returnToDashboard() {
        scope.launch {
            save()
            _activeSession.value = null
            _frames.value = emptyList()
            _selectedFrameId.value = null
            _layoutMode.value = LayoutMode.DASHBOARD
            // Refresh session list for dashboard display
            _sessions.value = repository.getSessions()
        }
    }

    /**
     * Set the background scene for the Cockpit display.
     */
    fun setBackgroundScene(scene: BackgroundScene) {
        _backgroundScene.value = scene
    }

    /**
     * Dispatch a content-specific action (e.g. web back/forward, PDF page turn)
     * to the currently selected frame's content renderer.
     *
     * The action is emitted via [contentAction] SharedFlow and collected by the
     * active ContentRenderer instance for the selected frame.
     */
    fun dispatchContentAction(action: ContentAction) {
        _contentAction.tryEmit(action)
    }

    // ── Deep Link Entry Points ────────────────────────────────────────

    /**
     * Handle a deep link URI and navigate to the corresponding state.
     *
     * Supported URI schemes:
     * - `cockpit://session/{sessionId}` — Resume an existing session
     * - `cockpit://module/{moduleId}` — Launch a module (creates new session)
     * - `cockpit://layout/{layoutMode}` — Switch to a layout mode in the current session
     * - `cockpit://template/{templateId}` — Launch a session template
     * - `cockpit://dashboard` — Return to the Dashboard
     *
     * Returns true if the deep link was handled, false if the URI was unrecognized.
     * Unknown segments are silently ignored — this prevents crashes from malformed URIs.
     */
    fun handleDeepLink(uri: String): Boolean {
        if (!uri.startsWith("cockpit://")) return false
        val normalized = uri.removePrefix("cockpit://").trimEnd('/')
        val segments = normalized.split("/", limit = 2)
        val action = segments.firstOrNull() ?: return false
        val param = segments.getOrNull(1)

        return when (action) {
            "session" -> {
                val sessionId = param ?: return false
                resumeSession(sessionId)
                true
            }
            "module" -> {
                val moduleId = param ?: return false
                launchModule(moduleId)
                true
            }
            "layout" -> {
                val modeName = param?.uppercase() ?: return false
                val mode = LayoutMode.entries.firstOrNull { it.name == modeName } ?: return false
                setLayoutMode(mode)
                true
            }
            "template" -> {
                val templateId = param ?: return false
                launchTemplate(templateId)
                true
            }
            "dashboard" -> {
                returnToDashboard()
                true
            }
            else -> false
        }
    }

    /**
     * Map a content type string to its corresponding [FrameContent] instance.
     */
    private fun contentForType(contentType: String): FrameContent? = when (contentType) {
        FrameContent.TYPE_WEB, "web" -> FrameContent.Web()
        FrameContent.TYPE_PDF, "pdf" -> FrameContent.Pdf()
        FrameContent.TYPE_IMAGE, "image" -> FrameContent.Image()
        FrameContent.TYPE_VIDEO, "video" -> FrameContent.Video()
        FrameContent.TYPE_NOTE, "note" -> FrameContent.Note()
        FrameContent.TYPE_CAMERA, "camera" -> FrameContent.Camera()
        FrameContent.TYPE_VOICE_NOTE, "voice_note" -> FrameContent.VoiceNote()
        FrameContent.TYPE_FORM, "form" -> FrameContent.Form()
        FrameContent.TYPE_VOICE, "voice" -> FrameContent.Voice()
        FrameContent.TYPE_MAP, "map" -> FrameContent.Map()
        FrameContent.TYPE_WHITEBOARD, "whiteboard" -> FrameContent.Whiteboard()
        FrameContent.TYPE_TERMINAL, "terminal" -> FrameContent.Terminal()
        FrameContent.TYPE_AI_SUMMARY, "ai_summary" -> FrameContent.AiSummary()
        FrameContent.TYPE_SCREEN_CAST, "screencast", "screen_cast" -> FrameContent.ScreenCast()
        FrameContent.TYPE_WIDGET, "widget" -> FrameContent.Widget()
        FrameContent.TYPE_FILE, "file" -> FrameContent.File()
        FrameContent.TYPE_EXTERNAL_APP, "external_app" -> FrameContent.ExternalApp()
        "cursor" -> null // Cursor control is handled via VoiceOSCore, not a frame content
        else -> null
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
        activeLoadJob?.cancel()
        dashboardCollectionJob?.cancel()
        viewModelJob.cancel()
    }

    /**
     * Generate a unique ID for sessions/frames.
     * Uses timestamp prefix (for sortability) + Uuid random suffix (for uniqueness).
     * Kotlin 2.1.0 stdlib Uuid replaces kotlin.random.Random for cryptographic quality.
     */
    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    private fun generateId(): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return "${timestamp}_${kotlin.uuid.Uuid.random().toHexString().take(12)}"
    }
}
