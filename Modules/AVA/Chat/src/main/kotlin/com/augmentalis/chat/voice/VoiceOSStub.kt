// filename: Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/voice/VoiceOSStub.kt
// created: 2025-11-23
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// Voice Input - VoiceOS Integration Stub

package com.augmentalis.chat.voice

/**
 * Stub interface for VoiceOS voice input integration.
 *
 * This is a placeholder implementation that will be replaced when VoiceOS
 * is integrated into the AVA project. VoiceOS provides:
 * - On-device voice recognition (no network required)
 * - Kotlin Multiplatform support (Android, iOS, Desktop)
 * - Custom wake word detection
 * - Low-latency command processing
 * - Privacy-focused (all processing local)
 *
 * **Current Status:** STUB - Integration pending VoiceOS merger
 * **Target:** Phase 4.0 - VoiceOS Integration
 * **Migration Path:** See README.md in this directory
 *
 * @author Manoj Jhawar
 */
interface VoiceInputProvider {
    /**
     * Starts voice input listening.
     *
     * @param callback Callback for voice input events
     * @param language Language code (default: en-US)
     */
    fun startListening(callback: VoiceInputCallback, language: String = "en-US")

    /**
     * Stops voice input listening.
     */
    fun stopListening()

    /**
     * Cancels voice input without triggering results.
     */
    fun cancel()

    /**
     * Checks if voice input is currently active.
     */
    fun isActive(): Boolean

    /**
     * Checks if voice input is available on this device.
     */
    fun isAvailable(): Boolean

    /**
     * Releases all resources.
     */
    fun release()

    /**
     * Callback interface for voice input events.
     */
    interface VoiceInputCallback {
        fun onReadyForSpeech()
        fun onBeginningOfSpeech()
        fun onPartialResult(partialText: String)
        fun onFinalResult(results: List<String>, confidenceScores: FloatArray?)
        fun onEndOfSpeech()
        fun onError(error: VoiceInputError)
        fun onRmsChanged(rmsdB: Float)
    }

    /**
     * Voice input error types.
     */
    sealed class VoiceInputError(val message: String, val isRecoverable: Boolean) {
        object NotAvailable : VoiceInputError(
            "Voice input will be available after VoiceOS integration (Phase 4.0)",
            false
        )
        object NotImplemented : VoiceInputError(
            "This feature is coming soon with VoiceOS",
            false
        )
    }
}

/**
 * Stub implementation of VoiceInputProvider.
 *
 * Returns "not available" for all operations until VoiceOS is integrated.
 *
 * **Migration Note:**
 * When VoiceOS is integrated, this class will be replaced with a real
 * implementation that uses the VoiceOS SDK for on-device voice recognition.
 */
class VoiceOSStub : VoiceInputProvider {

    override fun startListening(
        callback: VoiceInputProvider.VoiceInputCallback,
        language: String
    ) {
        // Immediately notify that voice input is not yet available
        callback.onError(VoiceInputProvider.VoiceInputError.NotAvailable)
    }

    override fun stopListening() {
        // No-op: nothing to stop
    }

    override fun cancel() {
        // No-op: nothing to cancel
    }

    override fun isActive(): Boolean = false

    override fun isAvailable(): Boolean = false

    override fun release() {
        // No-op: no resources to release
    }
}
