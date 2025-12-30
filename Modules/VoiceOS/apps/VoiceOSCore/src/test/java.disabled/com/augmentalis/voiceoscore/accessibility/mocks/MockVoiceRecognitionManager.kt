/**
 * MockVoiceRecognitionManager.kt - Mock implementation of VoiceRecognitionManager for testing
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-08-28
 */
package com.augmentalis.voiceoscore.accessibility.mocks

import android.content.Context
import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator

/**
 * Mock VoiceRecognitionManager for testing
 */
class MockVoiceRecognitionManager(private val actionCoordinator: ActionCoordinator) {
    private var isConnected = false
    
    @Suppress("UNUSED_PARAMETER") // Context parameter not needed in mock implementation
    fun initialize(_context: Context) {
        // Mock initialization
        isConnected = true
    }
    
    fun isServiceConnected(): Boolean = isConnected
    
    fun startListening(): Boolean {
        if (!isConnected) throw IllegalStateException("Service not connected")
        return true
    }
    
    fun dispose() {
        isConnected = false
    }
}