/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Extracted from ChatViewModel (SRP)
 */

package com.augmentalis.chat.coordinator

import android.util.Log
import com.augmentalis.ava.core.common.Result
import com.augmentalis.chat.tts.TTSManager
import com.augmentalis.chat.tts.TTSPreferences
import com.augmentalis.chat.tts.TTSSettings
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
 * Issue 5.1: Implements ITTSCoordinator for better testability and
 * potential alternative implementations.
 *
 * @param ttsManager Text-to-speech engine manager
 * @param ttsPreferences TTS user preferences
 *
 * @author Manoj Jhawar
 * @since 2025-12-17
 */
@Singleton
class TTSCoordinator @Inject constructor(
    private val ttsManager: TTSManager,
    private val ttsPreferences: TTSPreferences
) : ITTSCoordinator {
    companion object {
        private const val TAG = "TTSCoordinator"
    }

    // ==================== Delegated State ====================

    /**
     * TTS initialization state (delegated from TTSManager)
     */
    override val isTTSReady: StateFlow<Boolean> = ttsManager.isInitialized

    /**
     * Currently speaking state (delegated from TTSManager)
     */
    override val isTTSSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    /**
     * TTS settings (delegated from TTSPreferences)
     */
    override val ttsSettings: StateFlow<TTSSettings> = ttsPreferences.settings

    // ==================== Local State ====================

    /**
     * ID of message currently being spoken (for UI highlight)
     */
    private val _speakingMessageId = MutableStateFlow<String?>(null)
    override val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    // ==================== Operations ====================

    /**
     * Speak the given text, associating it with a message ID for UI tracking.
     *
     * @param text Text to speak
     * @param messageId Message ID for UI highlight tracking (null for no tracking)
     * @param onComplete Optional callback when speech completes
     * @return Result indicating success or failure
     */
    override fun speak(
        text: String,
        messageId: String?,
        onComplete: (() -> Unit)?
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Speaking message: $messageId")
            _speakingMessageId.value = messageId

            val result = ttsManager.speak(
                text = text,
                onComplete = {
                    // Clear speaking message ID when done
                    _speakingMessageId.value = null
                    onComplete?.invoke()
                }
            )

            result
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak error: ${e.message}", e)
            _speakingMessageId.value = null
            Result.Error(e, "Failed to speak: ${e.message}")
        }
    }

    /**
     * Stop current speech playback.
     */
    override fun stop() {
        Log.d(TAG, "Stopping TTS")
        ttsManager.stop()
        _speakingMessageId.value = null
    }

    /**
     * Check if auto-speak is enabled for assistant messages.
     */
    override fun isAutoSpeakEnabled(): Boolean {
        return ttsSettings.value.autoSpeak
    }

    /**
     * Clear speaking message ID (call when speech completes).
     */
    override fun clearSpeakingMessageId() {
        _speakingMessageId.value = null
    }

    /**
     * Update speaking message ID (for streaming TTS scenarios).
     */
    override fun setSpeakingMessageId(messageId: String?) {
        _speakingMessageId.value = messageId
    }

    // ==================== Settings Access ====================

    /**
     * Get current speech rate.
     */
    override fun getSpeechRate(): Float = ttsSettings.value.speechRate

    /**
     * Get current pitch.
     */
    override fun getPitch(): Float = ttsSettings.value.pitch

    /**
     * Check if TTS is ready to speak.
     */
    override fun isReady(): Boolean = isTTSReady.value

    // ==================== Settings Toggles ====================

    /**
     * Toggle TTS enabled state.
     */
    override fun toggleEnabled() {
        ttsPreferences.toggleEnabled()
        Log.d(TAG, "TTS enabled toggled")
    }

    /**
     * Toggle TTS auto-speak state.
     */
    override fun toggleAutoSpeak() {
        ttsPreferences.toggleAutoSpeak()
        Log.d(TAG, "TTS auto-speak toggled")
    }
}
