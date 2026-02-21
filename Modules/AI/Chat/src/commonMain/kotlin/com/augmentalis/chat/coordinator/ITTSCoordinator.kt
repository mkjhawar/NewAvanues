package com.augmentalis.chat.coordinator

import com.augmentalis.ava.core.common.Result
import com.augmentalis.chat.tts.TTSSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * TTS Coordinator Interface - Cross-platform TTS coordination
 *
 * Abstracts TTS operations for cross-platform use in KMP.
 * Provides:
 * - TTS initialization state management
 * - Speaking state and message tracking
 * - Settings access and toggling
 *
 * Issue 5.1: Created interface for better testability and potential
 * alternative implementations (iOS, Desktop, mock for testing).
 *
 * @see TTSCoordinator for Android implementation
 *
 * @author Manoj Jhawar
 * @since 2025-12-18
 */
interface ITTSCoordinator {
    // ==================== State ====================

    /**
     * TTS initialization state (true when TTS engine is ready).
     */
    val isTTSReady: StateFlow<Boolean>

    /**
     * Currently speaking state (true when audio is playing).
     */
    val isTTSSpeaking: StateFlow<Boolean>

    /**
     * ID of message currently being spoken (for UI highlight).
     * Null when not speaking.
     */
    val speakingMessageId: StateFlow<String?>

    /**
     * Current TTS settings (speech rate, pitch, auto-speak, etc.).
     */
    val ttsSettings: StateFlow<TTSSettings>

    // ==================== Operations ====================

    /**
     * Speak the given text, associating it with a message ID for UI tracking.
     *
     * @param text Text to speak
     * @param messageId Message ID for UI highlight tracking (null for no tracking)
     * @param onComplete Optional callback when speech completes
     * @return Result indicating success or failure
     */
    fun speak(
        text: String,
        messageId: String? = null,
        onComplete: (() -> Unit)? = null
    ): Result<Unit>

    /**
     * Stop current speech playback.
     */
    fun stop()

    /**
     * Check if auto-speak is enabled for assistant messages.
     */
    fun isAutoSpeakEnabled(): Boolean

    /**
     * Clear speaking message ID.
     * Call when speech completes or is interrupted.
     */
    fun clearSpeakingMessageId()

    /**
     * Update speaking message ID (for streaming TTS scenarios).
     */
    fun setSpeakingMessageId(messageId: String?)

    // ==================== Settings Access ====================

    /**
     * Get current speech rate multiplier (0.5x - 2.0x).
     */
    fun getSpeechRate(): Float

    /**
     * Get current pitch multiplier (0.5x - 2.0x).
     */
    fun getPitch(): Float

    /**
     * Check if TTS is ready to speak.
     */
    fun isReady(): Boolean

    // ==================== Settings Toggles ====================

    /**
     * Toggle TTS enabled state.
     */
    fun toggleEnabled()

    /**
     * Toggle TTS auto-speak state.
     */
    fun toggleAutoSpeak()
}
