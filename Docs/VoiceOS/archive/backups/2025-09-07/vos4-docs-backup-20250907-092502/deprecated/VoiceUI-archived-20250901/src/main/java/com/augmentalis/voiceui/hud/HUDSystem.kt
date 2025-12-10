/**
 * HUDSystem.kt - HUD system optimized for smart glasses
 */

package com.augmentalis.voiceui.hud

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import com.augmentalis.voiceui.theme.ThemeEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * HUD system optimized for smart glasses and spatial computing
 */
class HUDSystem(
    private val context: Context,
    private val themeEngine: ThemeEngine
) {
    
    companion object {
        private const val TAG = "HUDSystem"
    }
    
    // HUD element data class
    data class HUDElement(
        val id: String,
        val content: @Composable () -> Unit,
        val position: HUDPosition = HUDPosition.CENTER,
        val isVisible: Boolean = true,
        val priority: Int = 0
    )
    
    // HUD positions for spatial computing
    enum class HUDPosition {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        CENTER_LEFT,
        CENTER,
        CENTER_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT,
        FLOATING
    }
    
    // HUD state
    private val _hudElements = MutableStateFlow<List<HUDElement>>(emptyList())
    val hudElements: StateFlow<List<HUDElement>> = _hudElements
    
    private val _isVisible = MutableStateFlow(true)
    val isVisible: StateFlow<Boolean> = _isVisible
    
    /**
     * Add a HUD element
     */
    fun addElement(
        id: String,
        content: @Composable () -> Unit,
        position: HUDPosition = HUDPosition.CENTER,
        priority: Int = 0
    ) {
        val element = HUDElement(
            id = id,
            content = content,
            position = position,
            priority = priority
        )
        
        _hudElements.value = (_hudElements.value + element)
            .sortedByDescending { it.priority }
        
        Log.d(TAG, "Added HUD element: $id at $position")
    }
    
    /**
     * Remove a HUD element
     */
    fun removeElement(id: String) {
        _hudElements.value = _hudElements.value.filter { it.id != id }
        Log.d(TAG, "Removed HUD element: $id")
    }
    
    /**
     * Toggle HUD visibility
     */
    fun toggleVisibility() {
        _isVisible.value = !_isVisible.value
    }
    
    /**
     * Show/hide HUD
     */
    fun setVisible(visible: Boolean) {
        _isVisible.value = visible
    }
    
    fun shutdown() {
        _hudElements.value = emptyList()
    }
    
    // Additional methods for intent/provider support
    fun showNotification(message: String, duration: Int = 2000) {
        Log.d(TAG, "Showing notification: $message for ${duration}ms")
        // TODO: Implement actual notification display
    }
    
    fun showNotification(
        message: String,
        duration: Int,
        position: String,
        priority: String
    ) {
        val hudPosition = when(position) {
            "TOP_LEFT" -> HUDPosition.TOP_LEFT
            "TOP_CENTER" -> HUDPosition.TOP_CENTER
            "TOP_RIGHT" -> HUDPosition.TOP_RIGHT
            "BOTTOM_CENTER" -> HUDPosition.BOTTOM_CENTER
            else -> HUDPosition.CENTER
        }
        Log.d(TAG, "Showing notification: $message at $hudPosition with $priority priority")
        // TODO: Implement actual notification display with position and priority
    }
    
    fun updateOverlay(overlayId: String, content: String) {
        Log.d(TAG, "Updating overlay $overlayId with content: $content")
        // TODO: Implement overlay update
    }
    
    fun toggleVisibility(visible: Boolean, fadeDuration: Int) {
        _isVisible.value = visible
        Log.d(TAG, "Toggling visibility to $visible with fade duration ${fadeDuration}ms")
    }
    
    fun isVisible(): Boolean = _isVisible.value
}