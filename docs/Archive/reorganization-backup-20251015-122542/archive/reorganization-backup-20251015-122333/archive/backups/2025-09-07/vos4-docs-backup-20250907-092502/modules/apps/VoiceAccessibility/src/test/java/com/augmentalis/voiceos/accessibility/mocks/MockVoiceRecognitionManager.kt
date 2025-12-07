/**
 * MockVoiceRecognitionManager.kt - Mock implementation of VoiceRecognitionManager for testing
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-08-28
 * 
 * Provides a mock implementation of VoiceRecognitionManager for unit testing.
 */
package com.augmentalis.voiceos.accessibility.mocks

import android.content.Context
import com.augmentalis.voiceos.accessibility.managers.ActionCoordinator

/**
 * Mock VoiceRecognitionManager for testing
 */
class MockVoiceRecognitionManager(private val actionCoordinator: ActionCoordinator) {
    private var isConnected = false
    
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