package com.augmentalis.cockpit.mvp

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.window.WindowType
import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.layout.presets.LayoutPreset
import com.avanues.cockpit.layout.presets.WindowPosition
import com.avanues.cockpit.layout.presets.LinearHorizontalLayout
import com.avanues.cockpit.layout.presets.ArcFrontLayout
import com.avanues.cockpit.layout.presets.TheaterLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class WorkspaceViewModel : ViewModel() {

    // Available layout presets
    private val layoutPresets = listOf(
        LinearHorizontalLayout,
        ArcFrontLayout,
        TheaterLayout
    )
    private var currentLayoutIndex = 0

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

    // Spatial mode enabled state
    private val _isSpatialMode = MutableStateFlow(false)
    val isSpatialMode: StateFlow<Boolean> = _isSpatialMode.asStateFlow()

    // Current layout preset
    private val _layoutPreset = MutableStateFlow<LayoutPreset>(layoutPresets[currentLayoutIndex])
    val layoutPreset: StateFlow<LayoutPreset> = _layoutPreset.asStateFlow()

    // Selected window ID (for head cursor highlighting)
    private val _selectedWindowId = MutableStateFlow<String?>(null)
    val selectedWindowId: StateFlow<String?> = _selectedWindowId.asStateFlow()

    init {
        // Initialize with 3 sample windows
        addWindow("Email", WindowType.ANDROID_APP, "#FF6B9D")
        addWindow("Browser", WindowType.WEB_APP, "#4ECDC4")
        addWindow("Calculator", WindowType.WIDGET, "#95E1D3")
    }

    fun addWindow(title: String, type: WindowType, color: String) {
        if (_windows.value.size >= 6) return // Max 6 windows

        val window = AppWindow(
            id = UUID.randomUUID().toString(),
            title = title,
            type = type,
            sourceId = "demo.$title",
            position = Vector3D(0f, 0f, -2f),
            widthMeters = 0.8f,
            heightMeters = 0.6f,
            voiceName = title.lowercase()
        )
        _windows.value = _windows.value + window

        // Store window color
        _windowColors.value = _windowColors.value + (window.id to color)

        updatePositions()
    }

    fun removeWindow(windowId: String) {
        _windows.value = _windows.value.filter { it.id != windowId }
        _windowColors.value = _windowColors.value.filterKeys { it != windowId }
        updatePositions()
    }

    fun resetWorkspace() {
        _windows.value = emptyList()
        _windowPositions.value = emptyMap()
        _windowColors.value = emptyMap()
        _isHeadCursorEnabled.value = false
        _isSpatialMode.value = false
        currentLayoutIndex = 0
        _layoutPreset.value = layoutPresets[currentLayoutIndex]

        // Re-initialize with sample windows
        addWindow("Email", WindowType.ANDROID_APP, "#FF6B9D")
        addWindow("Browser", WindowType.WEB_APP, "#4ECDC4")
        addWindow("Calculator", WindowType.WIDGET, "#95E1D3")
    }

    fun toggleHeadCursor() {
        _isHeadCursorEnabled.value = !_isHeadCursorEnabled.value
    }

    fun toggleSpatialMode() {
        _isSpatialMode.value = !_isSpatialMode.value
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
        _layoutPreset.value = layoutPresets[currentLayoutIndex]
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
        _selectedWindowId.value = windowId
    }

    private fun updatePositions() {
        if (_windows.value.isEmpty()) {
            _windowPositions.value = emptyMap()
            return
        }

        val centerPoint = Vector3D(0f, 0f, -2f)
        val positions = _layoutPreset.value.calculatePositions(_windows.value, centerPoint)

        _windowPositions.value = positions.associate { it.windowId to it.position }
    }
}
