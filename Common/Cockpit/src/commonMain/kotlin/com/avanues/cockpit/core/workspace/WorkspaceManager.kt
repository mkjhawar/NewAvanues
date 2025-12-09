package com.avanues.cockpit.core.workspace

import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.layout.LayoutEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Workspace Manager
 *
 * Manages workspace state, persistence, and operations.
 * Integrates with LayoutEngine for automatic layout application.
 *
 * **Responsibilities:**
 * - Track active workspace
 * - Manage workspace list
 * - Save/load workspaces
 * - Handle workspace switching
 * - Apply layout changes
 *
 * **Voice-First Integration:**
 * - "Load work setup" → loadWorkspace("work")
 * - "Save workspace as evening" → saveWorkspace(current, "evening")
 * - "Switch to next workspace" → nextWorkspace()
 * - "What workspace am I in?" → activeWorkspace.value.name
 *
 * **Usage:**
 * ```kotlin
 * val manager = WorkspaceManager(layoutEngine, storage)
 * manager.loadWorkspace("work")
 * manager.addWindowToActive(gmailWindow)
 * manager.applyLayout("ARC_3_FRONT")
 * ```
 */
class WorkspaceManager(
    private val layoutEngine: LayoutEngine,
    private val storage: WorkspaceStorage
) {
    /**
     * Currently active workspace
     */
    private val _activeWorkspace = MutableStateFlow(Workspace.EMPTY)
    val activeWorkspace: StateFlow<Workspace> = _activeWorkspace.asStateFlow()

    /**
     * List of all saved workspaces
     */
    private val _workspaces = MutableStateFlow<List<Workspace>>(emptyList())
    val workspaces: StateFlow<List<Workspace>> = _workspaces.asStateFlow()

    /**
     * Initializes the manager
     * Loads saved workspaces from storage
     */
    suspend fun initialize() {
        val saved = storage.loadAll()
        _workspaces.value = saved

        // Load last active workspace or create empty
        val lastActive = storage.loadLastActive()
        _activeWorkspace.value = lastActive ?: Workspace.EMPTY
    }

    // ==================== Workspace Operations ====================

    /**
     * Creates a new empty workspace
     *
     * Voice: "Create new workspace"
     *
     * @param name Workspace name
     * @param voiceName Voice-friendly name
     * @param layoutPresetId Initial layout preset
     * @return Created workspace
     */
    fun createWorkspace(
        name: String,
        voiceName: String = name.lowercase(),
        layoutPresetId: String = LayoutEngine.DEFAULT_PRESET
    ): Workspace {
        val workspace = Workspace(
            id = generateId(),
            name = name,
            voiceName = voiceName,
            layoutPresetId = layoutPresetId
        )

        // Add to workspace list
        _workspaces.value = _workspaces.value + workspace

        return workspace
    }

    /**
     * Saves a workspace to storage
     *
     * Voice: "Save workspace as work setup"
     *
     * @param workspace Workspace to save
     * @param name Optional new name (for "save as")
     */
    suspend fun saveWorkspace(workspace: Workspace, name: String? = null) {
        val toSave = if (name != null) {
            workspace.rename(name, name.lowercase())
        } else {
            workspace
        }

        storage.save(toSave)

        // Update workspace list
        val existing = _workspaces.value.find { it.id == toSave.id }
        _workspaces.value = if (existing != null) {
            _workspaces.value.map { if (it.id == toSave.id) toSave else it }
        } else {
            _workspaces.value + toSave
        }

        // If this is the active workspace, update it
        if (_activeWorkspace.value.id == toSave.id) {
            _activeWorkspace.value = toSave
        }
    }

    /**
     * Loads a workspace by ID
     *
     * @param workspaceId Workspace ID
     * @return Loaded workspace or null
     */
    suspend fun loadWorkspace(workspaceId: String): Workspace? {
        val workspace = storage.load(workspaceId)
        if (workspace != null) {
            _activeWorkspace.value = workspace
            storage.saveLastActive(workspace.id)
        }
        return workspace
    }

    /**
     * Loads a workspace by voice name
     *
     * Voice: "Load work setup" → loadWorkspaceByVoiceName("work")
     *
     * @param voiceName Voice-friendly name
     * @return Loaded workspace or null
     */
    suspend fun loadWorkspaceByVoiceName(voiceName: String): Workspace? {
        val workspace = _workspaces.value.find {
            it.voiceName.equals(voiceName, ignoreCase = true)
        }

        if (workspace != null) {
            _activeWorkspace.value = workspace
            storage.saveLastActive(workspace.id)
        }

        return workspace
    }

    /**
     * Deletes a workspace
     *
     * Voice: "Delete this workspace"
     *
     * @param workspaceId Workspace ID to delete
     */
    suspend fun deleteWorkspace(workspaceId: String) {
        storage.delete(workspaceId)

        // Remove from list
        _workspaces.value = _workspaces.value.filterNot { it.id == workspaceId }

        // If active workspace was deleted, switch to empty
        if (_activeWorkspace.value.id == workspaceId) {
            _activeWorkspace.value = Workspace.EMPTY
        }
    }

    /**
     * Switches to next workspace in list
     *
     * Voice: "Next workspace"
     */
    suspend fun nextWorkspace() {
        val current = _activeWorkspace.value
        val list = _workspaces.value

        if (list.isEmpty()) return

        val currentIndex = list.indexOfFirst { it.id == current.id }
        val nextIndex = (currentIndex + 1) % list.size

        _activeWorkspace.value = list[nextIndex]
        storage.saveLastActive(list[nextIndex].id)
    }

    /**
     * Switches to previous workspace in list
     *
     * Voice: "Previous workspace"
     */
    suspend fun previousWorkspace() {
        val current = _activeWorkspace.value
        val list = _workspaces.value

        if (list.isEmpty()) return

        val currentIndex = list.indexOfFirst { it.id == current.id }
        val prevIndex = if (currentIndex <= 0) list.size - 1 else currentIndex - 1

        _activeWorkspace.value = list[prevIndex]
        storage.saveLastActive(list[prevIndex].id)
    }

    // ==================== Window Operations ====================

    /**
     * Adds a window to the active workspace
     *
     * Automatically re-applies layout to position the new window.
     *
     * Voice: "Add Gmail window"
     *
     * @param window Window to add
     */
    suspend fun addWindowToActive(window: AppWindow) {
        val updated = layoutEngine.addWindowWithLayout(_activeWorkspace.value, window)
        _activeWorkspace.value = updated
        saveWorkspace(updated)
    }

    /**
     * Removes a window from the active workspace
     *
     * Automatically re-applies layout to reposition remaining windows.
     *
     * Voice: "Remove browser window"
     *
     * @param windowId Window ID to remove
     */
    suspend fun removeWindowFromActive(windowId: String) {
        val updated = layoutEngine.removeWindowWithLayout(_activeWorkspace.value, windowId)
        _activeWorkspace.value = updated
        saveWorkspace(updated)
    }

    /**
     * Updates a window in the active workspace
     *
     * Voice: "Make browser bigger" → updateWindowInActive(browserId) { it.makeBigger() }
     *
     * @param windowId Window ID to update
     * @param update Update function
     */
    suspend fun updateWindowInActive(
        windowId: String,
        update: (AppWindow) -> AppWindow
    ) {
        val updated = _activeWorkspace.value.updateWindow(windowId, update)
        _activeWorkspace.value = updated
        saveWorkspace(updated)
    }

    /**
     * Gets a window from active workspace by ID
     */
    fun getWindow(windowId: String): AppWindow? {
        return _activeWorkspace.value.getWindow(windowId)
    }

    /**
     * Gets a window from active workspace by voice name
     *
     * Voice: "Focus email" → getWindowByVoiceName("email")
     */
    fun getWindowByVoiceName(voiceName: String): AppWindow? {
        return _activeWorkspace.value.getWindowByVoiceName(voiceName)
    }

    // ==================== Layout Operations ====================

    /**
     * Applies a layout preset to the active workspace
     *
     * Voice: "Linear mode" → applyLayout("LINEAR_HORIZONTAL")
     *
     * @param presetId Layout preset ID
     */
    suspend fun applyLayout(presetId: String) {
        val updated = layoutEngine.applyLayout(_activeWorkspace.value, presetId)
        _activeWorkspace.value = updated
        saveWorkspace(updated)
    }

    /**
     * Applies a layout using voice command
     *
     * Voice: "Linear mode" → applyLayoutByVoice("Linear mode")
     *
     * @param voiceCommand Voice command for layout
     * @return True if layout was applied
     */
    suspend fun applyLayoutByVoice(voiceCommand: String): Boolean {
        val updated = layoutEngine.applyLayoutByVoice(_activeWorkspace.value, voiceCommand)
        return if (updated != null) {
            _activeWorkspace.value = updated
            saveWorkspace(updated)
            true
        } else {
            false
        }
    }

    /**
     * Moves the active workspace center point
     *
     * Voice: "Move workspace forward"
     *
     * @param offset Vector to move by
     */
    suspend fun moveWorkspace(offset: Vector3D) {
        val updated = layoutEngine.moveWorkspace(_activeWorkspace.value, offset)
        _activeWorkspace.value = updated
        saveWorkspace(updated)
    }

    /**
     * Checks if more windows can be added to active workspace
     *
     * Voice: "Can I add another window?"
     */
    fun canAddWindow(): Boolean {
        return layoutEngine.canAddWindow(_activeWorkspace.value)
    }

    /**
     * Gets remaining window capacity for active workspace
     *
     * Voice: "How many more windows can I add?"
     */
    fun getRemainingCapacity(): Int {
        return layoutEngine.getRemainingCapacity(_activeWorkspace.value)
    }

    // ==================== Voice Integration ====================

    /**
     * Gets voice description of active workspace
     *
     * Voice: "What workspace am I in?" → VoiceOS announces this
     */
    fun getActiveWorkspaceDescription(): String {
        return _activeWorkspace.value.toVoiceDescription()
    }

    /**
     * Gets voice description of current layout
     *
     * Voice: "What layout am I using?" → VoiceOS announces this
     */
    fun getLayoutDescription(): String {
        return layoutEngine.getLayoutDescription(_activeWorkspace.value)
    }

    /**
     * Lists all saved workspace voice names
     *
     * Voice: "What workspaces do I have?" → VoiceOS reads this list
     */
    fun getWorkspaceVoiceNames(): List<String> {
        return _workspaces.value.map { it.voiceName }
    }

    /**
     * Lists all available layout voice commands
     *
     * Voice: "What layout modes are available?" → VoiceOS reads this list
     */
    fun getAvailableLayoutCommands(): List<String> {
        return layoutEngine.getAvailableVoiceCommands()
    }

    // ==================== Utilities ====================

    /**
     * Generates a unique ID for workspaces
     */
    private fun generateId(): String {
        return "workspace_${System.currentTimeMillis()}"
    }

    companion object {
        /**
         * Creates a WorkspaceManager with default configuration
         */
        fun createDefault(storage: WorkspaceStorage): WorkspaceManager {
            return WorkspaceManager(
                layoutEngine = LayoutEngine.createDefault(),
                storage = storage
            )
        }
    }
}

/**
 * Workspace Storage Interface (expect/actual)
 *
 * Platform-specific storage for workspace persistence.
 * Android: SharedPreferences or Room
 * iOS: UserDefaults or CoreData
 * Desktop: File system
 */
expect class WorkspaceStorage {
    suspend fun save(workspace: Workspace)
    suspend fun load(workspaceId: String): Workspace?
    suspend fun loadAll(): List<Workspace>
    suspend fun delete(workspaceId: String)
    suspend fun saveLastActive(workspaceId: String)
    suspend fun loadLastActive(): Workspace?
}
