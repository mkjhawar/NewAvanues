package com.augmentalis.voiceisolation

import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform voice isolation service.
 *
 * Provides audio preprocessing for speech recognition including:
 * - Noise suppression
 * - Echo cancellation
 * - Automatic gain control
 *
 * Usage:
 * ```kotlin
 * val voiceIsolation = VoiceIsolation.create(context)
 * voiceIsolation.initialize(audioSessionId)
 *
 * // Process audio before sending to speech recognition
 * val processedAudio = voiceIsolation.process(rawAudio)
 *
 * // Toggle for A/B testing
 * voiceIsolation.toggle(enabled = false)
 *
 * // Cleanup
 * voiceIsolation.release()
 * ```
 */
expect class VoiceIsolation {
    /**
     * Current state of voice isolation.
     */
    val state: StateFlow<VoiceIsolationState>

    /**
     * Initialize voice isolation with an audio session.
     *
     * @param audioSessionId The audio session ID from AudioRecord or MediaRecorder
     * @param config Optional configuration (defaults to VoiceIsolationConfig.DEFAULT)
     * @return true if initialization succeeded
     */
    fun initialize(audioSessionId: Int, config: VoiceIsolationConfig = VoiceIsolationConfig.DEFAULT): Boolean

    /**
     * Process audio data through voice isolation.
     *
     * If voice isolation is disabled, returns the input unchanged.
     *
     * @param audioData Raw audio bytes (PCM 16-bit recommended)
     * @return Processed audio bytes
     */
    fun process(audioData: ByteArray): ByteArray

    /**
     * Update the voice isolation configuration.
     *
     * @param config New configuration to apply
     */
    fun updateConfig(config: VoiceIsolationConfig)

    /**
     * Check if voice isolation is currently enabled.
     */
    fun isEnabled(): Boolean

    /**
     * Toggle voice isolation on/off for A/B testing.
     *
     * @param enabled true to enable, false to disable (passthrough)
     */
    fun toggle(enabled: Boolean)

    /**
     * Get feature availability on current device.
     *
     * @return Map of feature name to availability boolean
     */
    fun getAvailability(): Map<String, Boolean>

    /**
     * Release all voice isolation resources.
     * Must be called when done using voice isolation.
     */
    fun release()
}

/**
 * Factory for creating platform-specific VoiceIsolation instances.
 */
expect object VoiceIsolationFactory {
    /**
     * Create a new VoiceIsolation instance.
     * On Android, requires context initialization first via initialize(context).
     */
    fun create(): VoiceIsolation
}
