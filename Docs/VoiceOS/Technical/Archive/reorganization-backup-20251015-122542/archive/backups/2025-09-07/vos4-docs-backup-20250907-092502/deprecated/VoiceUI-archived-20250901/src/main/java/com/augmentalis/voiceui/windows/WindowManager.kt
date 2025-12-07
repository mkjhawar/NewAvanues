/**
 * WindowManager.kt - Multi-window management system
 */

package com.augmentalis.voiceui.windows

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Multi-window management system for VoiceUI
 */
class WindowManager(private val context: Context) {
    
    companion object {
        private const val TAG = "WindowManager"
    }
    
    // Window data class
    data class Window(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val content: (@Composable () -> Unit)? = null,
        val x: Float = 0f,
        val y: Float = 0f,
        val z: Float = -2f,
        val width: Int = 800,
        val height: Int = 600,
        val visible: Boolean = true,
        val isVisible: Boolean = true,
        val isMinimized: Boolean = false,
        val zIndex: Int = 0
    )
    
    // Window state
    private val _windows = MutableStateFlow<List<Window>>(emptyList())
    val windows: StateFlow<List<Window>> = _windows
    
    /**
     * Create a new window
     */
    fun createWindow(
        title: String,
        content: @Composable () -> Unit
    ): String {
        val window = Window(
            title = title,
            content = content
        )
        
        _windows.value = _windows.value + window
        Log.d(TAG, "Created window: ${window.id} ($title)")
        return window.id
    }
    
    /**
     * Close a window
     */
    fun closeWindow(windowId: String) {
        _windows.value = _windows.value.filter { it.id != windowId }
        Log.d(TAG, "Closed window: $windowId")
    }
    
    /**
     * Minimize/restore a window
     */
    fun toggleMinimize(windowId: String) {
        _windows.value = _windows.value.map { window ->
            if (window.id == windowId) {
                window.copy(isMinimized = !window.isMinimized)
            } else {
                window
            }
        }
    }
    
    fun shutdown() {
        _windows.value = emptyList()
    }
    
    // Additional methods for intent/provider support
    fun setSpatialMode(enabled: Boolean) {
        Log.d(TAG, "Spatial mode set to: $enabled")
    }
    
    fun createSpatialWindow(
        title: String,
        width: Int = 800,
        height: Int = 600,
        x: Float = 0f,
        y: Float = 0f,
        z: Float = -2f
    ): String {
        val window = Window(
            title = title,
            width = width,
            height = height,
            x = x,
            y = y,
            z = z
        )
        _windows.value = _windows.value + window
        Log.d(TAG, "Created spatial window: ${window.id} ($title)")
        return window.id
    }
    
    fun createWindow(
        windowId: String,
        title: String,
        width: Int,
        height: Int,
        x: Float,
        y: Float,
        z: Float
    ): String {
        val window = Window(
            id = windowId,
            title = title,
            width = width,
            height = height,
            x = x,
            y = y,
            z = z
        )
        _windows.value = _windows.value + window
        Log.d(TAG, "Created window: $windowId ($title)")
        return windowId
    }
    
    fun showWindow(windowId: String, animated: Boolean = true) {
        _windows.value = _windows.value.map { window ->
            if (window.id == windowId) {
                window.copy(visible = true, isVisible = true)
            } else {
                window
            }
        }
        Log.d(TAG, "Showing window: $windowId (animated: $animated)")
    }
    
    fun hideWindow(windowId: String, animated: Boolean = true) {
        _windows.value = _windows.value.map { window ->
            if (window.id == windowId) {
                window.copy(visible = false, isVisible = false)
            } else {
                window
            }
        }
        Log.d(TAG, "Hiding window: $windowId (animated: $animated)")
    }
    
    fun moveWindow(windowId: String, x: Float, y: Float, z: Float) {
        _windows.value = _windows.value.map { window ->
            if (window.id == windowId) {
                window.copy(x = x, y = y, z = z)
            } else {
                window
            }
        }
        Log.d(TAG, "Moving window: $windowId to ($x, $y, $z)")
    }
    
    fun resizeWindow(windowId: String, width: Int, height: Int) {
        _windows.value = _windows.value.map { window ->
            if (window.id == windowId) {
                window.copy(width = width, height = height)
            } else {
                window
            }
        }
        Log.d(TAG, "Resizing window: $windowId to ${width}x${height}")
    }
    
    fun destroyWindow(windowId: String) {
        closeWindow(windowId)
    }
    
    fun getActiveWindows(): List<Window> = _windows.value
}