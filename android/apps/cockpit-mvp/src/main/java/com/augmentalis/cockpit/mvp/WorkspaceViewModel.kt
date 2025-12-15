package com.augmentalis.cockpit.mvp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateListOf
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.window.WindowContent
import com.avanues.cockpit.core.window.WindowType
import com.avanues.cockpit.core.window.DocumentType
import com.avanues.cockpit.core.window.WidgetType
import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.layout.presets.LayoutPreset
import com.avanues.cockpit.layout.presets.WindowPosition
import com.avanues.cockpit.layout.presets.LinearHorizontalLayout
import com.avanues.cockpit.layout.presets.ArcFrontLayout
import com.avanues.cockpit.layout.presets.TheaterLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.UUID

/**
 * WorkspaceViewModel - Manages workspace state for Cockpit MVP
 *
 * Features:
 * - Window management (add, remove, reset)
 * - Spatial positioning with multiple layout presets
 * - Layout cycling (Linear → Arc → Theater → Linear)
 * - Spatial mode toggle (2D flat vs 3D curved)
 * - Head cursor navigation toggle
 * - Window color management
 */
class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    // Available layout presets
    private val layoutPresets = listOf(
        LinearHorizontalLayout,
        ArcFrontLayout,
        TheaterLayout
    )
    private var currentLayoutIndex = 1  // Default to ArcFrontLayout (angled windows)

    // Mutable list of windows
    private val _windows = MutableStateFlow<List<AppWindow>>(emptyList())
    val windows: StateFlow<List<AppWindow>> = _windows.asStateFlow()

    // Window positions
    private val _windowPositions = MutableStateFlow<Map<String, Vector3D>>(emptyMap())
    val windowPositions: StateFlow<Map<String, Vector3D>> = _windowPositions.asStateFlow()

    // Window colors (for spatial mode)
    private val _windowColors = MutableStateFlow<Map<String, String>>(emptyMap())
    val windowColors: StateFlow<Map<String, String>> = _windowColors.asStateFlow()

    // Head cursor enabled state
    private val _isHeadCursorEnabled = MutableStateFlow(false)
    val isHeadCursorEnabled: StateFlow<Boolean> = _isHeadCursorEnabled.asStateFlow()

    // Workspace mode (FLAT / SPATIAL / CURVED)
    private val _workspaceMode = MutableStateFlow(WorkspaceMode.CURVED)  // Default to curved
    val workspaceMode: StateFlow<WorkspaceMode> = _workspaceMode.asStateFlow()

    // Legacy spatial mode (for backwards compatibility)
    val isSpatialMode: StateFlow<Boolean> = _workspaceMode.map { it == WorkspaceMode.SPATIAL }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Current layout preset
    private val _layoutPreset = MutableStateFlow<LayoutPreset>(layoutPresets[currentLayoutIndex])
    val layoutPreset: StateFlow<LayoutPreset> = _layoutPreset.asStateFlow()

    // Selected window ID (for head cursor highlighting)
    private val _selectedWindowId = MutableStateFlow<String?>(null)
    val selectedWindowId: StateFlow<String?> = _selectedWindowId.asStateFlow()

    init {
        // Initialize with 5 sample windows (Ocean Blue professional colors)
        addWindow("Augmentalis", WindowType.WEB_APP, "#2D5F7F", WindowContent.WebContent("https://www.augmentalis.com"))
        addWindow("Google", WindowType.WEB_APP, "#2A6B6A", WindowContent.WebContent("https://google.com"))
        addWindow("Calculator", WindowType.WIDGET, "#3A7B8A", WindowContent.WidgetContent(WidgetType.CALCULATOR))
        addWindow("Weather", WindowType.WEB_APP, "#4A90B8", WindowContent.WebContent("https://weather.com"))
        addWindow("Maps", WindowType.WEB_APP, "#8A9BA8", WindowContent.WebContent("https://maps.google.com"))
    }

    fun addWindow(title: String, type: WindowType, color: String, content: WindowContent = WindowContent.MockContent) {
        if (_windows.value.size >= 6) return // Max 6 windows

        val window = AppWindow(
            id = UUID.randomUUID().toString(),
            title = title,
            type = type,
            sourceId = "demo.$title",
            position = Vector3D(0f, 0f, -2f),
            widthMeters = 1.2f,  // Increased from 0.8f (50% larger)
            heightMeters = 0.9f,  // Increased from 0.6f (50% larger)
            voiceName = title.lowercase(),
            content = content
        )
        _windows.update { it + window }

        // Store window color
        _windowColors.update { it + (window.id to color) }

        updatePositions()
    }

    fun removeWindow(windowId: String) {
        _windows.update { it.filter { window -> window.id != windowId } }
        _windowColors.update { it.filterKeys { key -> key != windowId } }
        updatePositions()
    }

    fun resetWorkspace() {
        _windows.update { emptyList() }
        _windowPositions.update { emptyMap() }
        _windowColors.update { emptyMap() }
        _isHeadCursorEnabled.update { false }
        _workspaceMode.update { WorkspaceMode.CURVED }  // Default to curved workspace
        currentLayoutIndex = 1  // Reset to ArcFrontLayout (angled windows)
        _layoutPreset.update { layoutPresets[currentLayoutIndex] }

        // Re-initialize with sample windows (Ocean Blue professional colors)
        addWindow("Augmentalis", WindowType.WEB_APP, "#2D5F7F", WindowContent.WebContent("https://www.augmentalis.com"))
        addWindow("Google", WindowType.WEB_APP, "#2A6B6A", WindowContent.WebContent("https://google.com"))
        addWindow("Calculator", WindowType.WIDGET, "#3A7B8A", WindowContent.WidgetContent(WidgetType.CALCULATOR))
        addWindow("Weather", WindowType.WEB_APP, "#4A90B8", WindowContent.WebContent("https://weather.com"))
        addWindow("Maps", WindowType.WEB_APP, "#8A9BA8", WindowContent.WebContent("https://maps.google.com"))
    }

    fun toggleHeadCursor() {
        _isHeadCursorEnabled.update { !it }
    }

    /**
     * Toggle/cycle workspace mode
     * FLAT → SPATIAL → CURVED → FLAT (cycle)
     */
    fun toggleSpatialMode() {
        _workspaceMode.update { it.next() }
    }

    /**
     * Set specific workspace mode
     */
    fun setWorkspaceMode(mode: WorkspaceMode) {
        _workspaceMode.update { mode }
    }

    /**
     * Cycle to next/previous layout preset
     *
     * @param forward True for next layout, false for previous
     */
    fun cycleLayoutPreset(forward: Boolean = true) {
        if (forward) {
            currentLayoutIndex = (currentLayoutIndex + 1) % layoutPresets.size
        } else {
            currentLayoutIndex = (currentLayoutIndex - 1 + layoutPresets.size) % layoutPresets.size
        }
        _layoutPreset.update { layoutPresets[currentLayoutIndex] }
        updatePositions()
    }

    /**
     * Get current layout preset name for display
     */
    fun getLayoutPresetName(): String {
        return when (layoutPresets[currentLayoutIndex]) {
            is LinearHorizontalLayout -> "Linear"
            is ArcFrontLayout -> "Arc"
            is TheaterLayout -> "Theater"
            else -> "Unknown"
        }
    }

    /**
     * Update cursor position and determine which window is being hovered
     *
     * This method will be called from MainActivity when the head cursor position changes.
     * In spatial mode, it determines which window the cursor is over and updates selectedWindowId.
     *
     * @param screenX Cursor X position in screen coordinates
     * @param screenY Cursor Y position in screen coordinates
     */
    fun updateCursorPosition(screenX: Float, screenY: Float) {
        // For now, we store the cursor position
        // The actual hit detection happens in SpatialWindowRenderer
        // and is passed back through the view
        // This is just a placeholder - actual implementation will be in SpatialWorkspaceView
    }

    /**
     * Set the selected window ID (called from SpatialWorkspaceView when cursor hovers)
     *
     * @param windowId The ID of the window being hovered, or null if no window
     */
    fun setSelectedWindow(windowId: String?) {
        _selectedWindowId.update { windowId }
    }

    /**
     * Minimize window (set isHidden = true)
     * Hidden windows show as collapsed title bar only (48dp height)
     *
     * @param windowId The ID of the window to minimize
     */
    fun minimizeWindow(windowId: String) {
        _windows.update { windows -> windows.map { window ->
            if (window.id == windowId) {
                window.copy(
                    isHidden = true,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                window
            }
        } }
    }

    /**
     * Restore minimized window (set isHidden = false)
     *
     * @param windowId The ID of the window to restore
     */
    fun restoreWindow(windowId: String) {
        _windows.update { windows -> windows.map { window ->
            if (window.id == windowId) {
                window.copy(
                    isHidden = false,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                window
            }
        } }
    }

    /**
     * Toggle window size between normal (300x400dp) and maximized (screen - 40dp)
     *
     * State Transitions:
     * - Normal (isHidden=false, isLarge=false) → Maximized (isHidden=false, isLarge=true)
     * - Maximized (isHidden=false, isLarge=true) → Normal (isHidden=false, isLarge=false)
     * - Minimized (isHidden=true, isLarge=*) → Restore (isHidden=false, keep isLarge)
     *
     * Fix (Issue #2): Restore to previous state when minimized
     * When window is minimized, restore to pre-minimize size (not always maximize)
     *
     * @param windowId The ID of the window to toggle size
     */
    fun toggleWindowSize(windowId: String) {
        _windows.update { windows -> windows.map { window ->
            if (window.id == windowId) {
                if (window.isHidden) {
                    // Restore from minimized: clear isHidden, keep isLarge (preserve pre-minimize state)
                    window.copy(
                        isHidden = false,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    // Normal maximize/restore toggle
                    window.copy(
                        isLarge = !window.isLarge,
                        updatedAt = System.currentTimeMillis()
                    )
                }
            } else {
                window
            }
        } }
    }

    /**
     * Select a window (for Phase 2)
     * Sets this window as the selected window (for voice commands, keyboard shortcuts)
     *
     * @param windowId The ID of the window to select
     */
    fun selectWindow(windowId: String) {
        _selectedWindowId.update { windowId }
    }

    /**
     * Update WebView scroll position (Phase 3: FR-3.1)
     * Saves scroll state so it persists across window switches
     *
     * @param windowId The ID of the window containing the WebView
     * @param scrollX Horizontal scroll position
     * @param scrollY Vertical scroll position
     */
    fun updateWebViewScrollPosition(windowId: String, scrollX: Int, scrollY: Int) {
        _windows.update { windows -> windows.map { window ->
            val content = window.content
            if (window.id == windowId && content is WindowContent.WebContent) {
                window.copy(
                    content = content.copy(
                        scrollX = scrollX,
                        scrollY = scrollY
                    ),
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                window
            }
        } }
    }

    /**
     * Update PDF document state (Phase 3: FR-3.2)
     * Saves page number, zoom level, and scroll position
     *
     * @param windowId The ID of the window containing the PDF
     * @param currentPage Current page number
     * @param zoomLevel Zoom level (1.0 = 100%)
     * @param scrollX Horizontal scroll position
     * @param scrollY Vertical scroll position
     */
    fun updatePdfState(windowId: String, currentPage: Int, zoomLevel: Float, scrollX: Float, scrollY: Float) {
        _windows.update { windows -> windows.map { window ->
            val content = window.content
            if (window.id == windowId && content is WindowContent.DocumentContent && content.documentType == DocumentType.PDF) {
                window.copy(
                    content = content.copy(
                        currentPage = currentPage,
                        zoomLevel = zoomLevel,
                        scrollX = scrollX,
                        scrollY = scrollY
                    ),
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                window
            }
        } }
    }

    /**
     * Update video playback position (Phase 3: FR-3.3)
     * Saves playback position every 5 seconds during playback
     *
     * @param windowId The ID of the window containing the video
     * @param playbackPosition Playback position in milliseconds
     */
    fun updateVideoPlaybackPosition(windowId: String, playbackPosition: Long) {
        _windows.update { windows -> windows.map { window ->
            val content = window.content
            if (window.id == windowId && content is WindowContent.DocumentContent && content.documentType == DocumentType.VIDEO) {
                window.copy(
                    content = content.copy(
                        playbackPosition = playbackPosition
                    ),
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                window
            }
        } }
    }

    /**
     * Update window content (generic method for Phase 3)
     * Used by content renderers to update their state
     *
     * @param windowId The ID of the window to update
     * @param newContent The new content state
     */
    fun updateWindowContent(windowId: String, newContent: WindowContent) {
        _windows.update { windows -> windows.map { window ->
            if (window.id == windowId) {
                window.copy(
                    content = newContent,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                window
            }
        } }
    }

    private fun updatePositions() {
        if (_windows.value.isEmpty()) {
            _windowPositions.update { emptyMap() }
            return
        }

        val centerPoint = Vector3D(0f, 0f, -2f)
        val positions = _layoutPreset.value.calculatePositions(_windows.value, centerPoint)

        _windowPositions.update { positions.associate { it.windowId to it.position } }
    }
}
