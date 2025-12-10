package com.augmentalis.cockpit.mvp

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.window.WindowType
import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.layout.presets.LinearHorizontalLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * WorkspaceViewModel - Manages workspace state for Cockpit MVP
 *
 * Features:
 * - Window management (add, remove, reset)
 * - Spatial positioning with LinearHorizontalLayout
 * - Head cursor navigation toggle
 */
class WorkspaceViewModel : ViewModel() {

    // Mutable list of windows
    private val _windows = MutableStateFlow<List<AppWindow>>(emptyList())
    val windows: StateFlow<List<AppWindow>> = _windows.asStateFlow()

    // Window positions
    private val _windowPositions = MutableStateFlow<Map<String, Vector3D>>(emptyMap())
    val windowPositions: StateFlow<Map<String, Vector3D>> = _windowPositions.asStateFlow()

    // Head cursor enabled state
    private val _isHeadCursorEnabled = MutableStateFlow(false)
    val isHeadCursorEnabled: StateFlow<Boolean> = _isHeadCursorEnabled.asStateFlow()

    // Layout preset
    private val layoutPreset = LinearHorizontalLayout

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
        updatePositions()
    }

    fun removeWindow(windowId: String) {
        _windows.value = _windows.value.filter { it.id != windowId }
        updatePositions()
    }

    fun resetWorkspace() {
        _windows.value = emptyList()
        _windowPositions.value = emptyMap()
        _isHeadCursorEnabled.value = false

        // Re-initialize with sample windows
        addWindow("Email", WindowType.ANDROID_APP, "#FF6B9D")
        addWindow("Browser", WindowType.WEB_APP, "#4ECDC4")
        addWindow("Calculator", WindowType.WIDGET, "#95E1D3")
    }

    fun toggleHeadCursor() {
        _isHeadCursorEnabled.value = !_isHeadCursorEnabled.value
    }

    private fun updatePositions() {
        if (_windows.value.isEmpty()) {
            _windowPositions.value = emptyMap()
            return
        }

        val centerPoint = Vector3D(0f, 0f, -2f)
        val positions = layoutPreset.calculatePositions(_windows.value, centerPoint)

        _windowPositions.value = positions.associate { it.windowId to it.position }
    }
}
