package com.augmentalis.cockpit.mvp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages head-based navigation state and window selection
 */
class HeadNavigationManager : ViewModel() {

    // Head cursor enabled state
    var isHeadCursorEnabled by mutableStateOf(false)
        private set

    // Current cursor position
    var cursorPosition by mutableStateOf(Offset.Zero)
        private set

    // Currently hovered window
    var hoveredWindowId by mutableStateOf<String?>(null)
        private set

    // Dwell time progress (0.0 to 1.0)
    var dwellProgress by mutableFloatStateOf(0f)
        private set

    // Sensitivity settings
    private var _sensitivity by mutableFloatStateOf(1.0f)
    val sensitivity: Float get() = _sensitivity

    // Dwell time for selection (milliseconds)
    private val dwellTimeMs = 1500L
    private var dwellJob: Job? = null

    // Window bounds for hit detection
    private val windowBounds = mutableMapOf<String, Rect>()

    /**
     * Toggle head cursor on/off
     */
    fun toggleHeadCursor() {
        isHeadCursorEnabled = !isHeadCursorEnabled
        if (!isHeadCursorEnabled) {
            resetDwell()
        }
    }

    /**
     * Update cursor position from IMU
     */
    fun updateCursorPosition(x: Float, y: Float) {
        cursorPosition = Offset(x, y)
        checkWindowHover()
    }

    /**
     * Register window bounds for hit detection
     */
    fun registerWindowBounds(windowId: String, bounds: Rect) {
        windowBounds[windowId] = bounds
    }

    /**
     * Unregister window bounds
     */
    fun unregisterWindowBounds(windowId: String) {
        windowBounds.remove(windowId)
    }

    /**
     * Check if cursor is hovering over a window
     */
    private fun checkWindowHover() {
        val previousHovered = hoveredWindowId
        hoveredWindowId = windowBounds.entries.firstOrNull { (_, bounds) ->
            bounds.contains(cursorPosition)
        }?.key

        // If hovered window changed, reset dwell
        if (previousHovered != hoveredWindowId) {
            resetDwell()
            if (hoveredWindowId != null) {
                startDwell()
            }
        }
    }

    /**
     * Start dwell timer for selection
     */
    private fun startDwell() {
        dwellJob?.cancel()
        dwellJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (dwellProgress < 1.0f) {
                val elapsed = System.currentTimeMillis() - startTime
                dwellProgress = (elapsed.toFloat() / dwellTimeMs).coerceIn(0f, 1f)
                delay(16) // ~60fps update

                // Check if still hovering
                if (hoveredWindowId == null) {
                    resetDwell()
                    break
                }
            }

            // Selection complete
            if (dwellProgress >= 1.0f) {
                onWindowSelected(hoveredWindowId)
                resetDwell()
            }
        }
    }

    /**
     * Reset dwell progress
     */
    private fun resetDwell() {
        dwellJob?.cancel()
        dwellProgress = 0f
    }

    /**
     * Called when window is selected via dwell
     */
    private fun onWindowSelected(windowId: String?) {
        windowId?.let {
            // Window selected - can trigger focus, open, etc.
            // For now, just reset
            println("Window selected: $windowId")
        }
    }

    /**
     * Update sensitivity
     */
    fun setSensitivity(value: Float) {
        _sensitivity = value.coerceIn(0.1f, 3.0f)
    }

    /**
     * Calibrate head position (reset center)
     */
    fun calibrate() {
        // This would reset the IMU base orientation
        // For now, just a placeholder
        println("Calibrating head position...")
    }

    override fun onCleared() {
        super.onCleared()
        dwellJob?.cancel()
    }
}
