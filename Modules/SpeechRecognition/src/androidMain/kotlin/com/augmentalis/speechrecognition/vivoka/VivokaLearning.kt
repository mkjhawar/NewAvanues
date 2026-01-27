/**
 * VivokaLearning.kt - STUB VERSION (VoiceDataManager dependency removed)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-24
 * Updated: 2026-01-27 - Migrated to KMP androidMain
 *
 * Stub implementation for VivokaEngine learning functionality.
 * All methods are no-ops to allow VivokaEngine to compile without learning features.
 */
package com.augmentalis.speechrecognition.vivoka

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope

/**
 * Stub Vivoka learning system - all functionality disabled
 */
class VivokaLearning(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {

    companion object {
        private const val TAG = "VivokaLearning [STUB]"
    }

    /**
     * Stub initialize - always returns true
     */
    suspend fun initialize(): Boolean {
        Log.w(TAG, "VivokaLearning DISABLED - VoiceDataManager dependency removed")
        return true  // Return success so engine continues
    }

    /**
     * Stub registerCommands - does nothing
     */
    fun registerCommands(commands: List<String>) {
        // No-op
    }

    /**
     * Stub processCommandWithLearning - returns original command with no learning
     */
    fun processCommandWithLearning(
        command: String,
        registeredCommands: List<String>,
        confidence: Float = 1.0f
    ): Pair<String, Boolean> {
        // Return original command and false (not learned)
        return Pair(command, false)
    }

    /**
     * Stub syncLearningData - does nothing
     */
    suspend fun syncLearningData() {
        // No-op
    }

    /**
     * Stub clearAllLearningData - does nothing
     */
    suspend fun clearAllLearningData() {
        // No-op
    }

    /**
     * Stub destroy - does nothing
     */
    suspend fun destroy() {
        // No-op
    }
}
