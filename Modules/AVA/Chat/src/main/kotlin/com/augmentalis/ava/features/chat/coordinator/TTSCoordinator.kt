/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Extracted from ChatViewModel (SRP)
 */

package com.augmentalis.ava.features.chat.coordinator

import android.util.Log
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.features.chat.tts.TTSManager
import com.augmentalis.ava.features.chat.tts.TTSPreferences
import com.augmentalis.ava.features.chat.tts.TTSSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TTS Coordinator - Single Responsibility: Text-to-Speech orchestration
 *
 * Extracted from ChatViewModel as part of SOLID refactoring.
 * Handles all TTS-related operations:
 * - TTS initialization state delegation
 * - Speaking state management
 * - Message-to-speech mapping
 * - Settings access
 *
 * Wraps TTSManager and TTSPreferences for clean ViewModel integration.
 *
 * @param ttsManager Text-to-speech engine manager
 * @param ttsPreferences TTS user preferences
 *
 * @author Manoj Jhawar / Claude AI
 * @since 2025-12-17
 */
@Singleton
class TTSCoordinator @Inject constructor(
    private val ttsManager: TTSManager,
    private val ttsPreferences: TTSPreferences
) {
    companion object {
        private const val TAG = "TTSCoordinator"
    }

    // ==================== Delegated State ====================

    /**
     * TTS initialization state (delegated from TTSManager)
     */
    val isTTSReady: StateFlow<Boolean> = ttsManager.isInitialized

    /**
     * Currently speaking state (delegated from TTSManager)
     */
    val isTTSSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    /**
     * TTS settings (delegated from TTSPreferences)
     */
    val ttsSettings: StateFlow<TTSSettings> = ttsPreferences.settings

    // ==================== Local State ====================

    /**
     * ID of message currently being spoken (for UI highlight)
     */
    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    // ==================== Operations ====================

    /**
     * Speak the given text, associating it with a message ID for UI tracking.
     *
     * @param text Text to speak
     * @param messageId Message ID for UI highlight tracking
     * @return Result indicating success or failure
     */
    suspend fun speak(text: String, messageId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Speaking message: $messageId")
            _speakingMessageId.value = messageId

            val result = ttsManager.speak(text)

            // Clear speaking message ID when done (TTS completes asynchronously)
            // Note: This is immediate return - actual completion handled by listener
            result
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak error: ${e.message}", e)
            _speakingMessageId.value = null
            Result.Error(e, "Failed to speak: ${e.message}")
        }
    }

    /**
     * Speak text without message ID tracking.
     *
     * @param text Text to speak
     * @return Result indicating success or failure
     */
    suspend fun speak(text: String): Result<Unit> {
        return try {
            Log.d(TAG, "Speaking text (no message ID)")
            ttsManager.speak(text)
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak error: ${e.message}", e)
            Result.Error(e, "Failed to speak: ${e.message}")
        }
    }

    /**
     * Stop current speech playback.
     */
    fun stop() {
        Log.d(TAG, "Stopping TTS")
        ttsManager.stop()
        _speakingMessageId.value = null
    }

    /**
     * Check if auto-speak is enabled for assistant messages.
     */
    fun isAutoSpeakEnabled(): Boolean {
        return ttsSettings.value.autoSpeak
    }

    /**
     * Clear speaking message ID (call when speech completes).
     */
    fun clearSpeakingMessageId() {
        _speakingMessageId.value = null
    }

    /**
     * Update speaking message ID (for streaming TTS scenarios).
     */
    fun setSpeakingMessageId(messageId: String?) {
        _speakingMessageId.value = messageId
    }

    // ==================== Settings Access ====================

    /**
     * Get current speech rate.
     */
    fun getSpeechRate(): Float = ttsSettings.value.speechRate

    /**
     * Get current pitch.
     */
    fun getPitch(): Float = ttsSettings.value.pitch

    /**
     * Check if TTS is ready to speak.
     */
    fun isReady(): Boolean = isTTSReady.value
}
