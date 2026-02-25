/**
 * ISpeechPreFilter.kt - Cross-platform speech pre-filter interface
 *
 * Defines a lightweight pre-filter that sits alongside the primary speech engine.
 * The pre-filter processes audio in parallel with the main engine and can
 * short-circuit command processing for high-confidence results.
 *
 * On Android: Implemented by AvxPreFilterEngine (AVX → Vivoka pre-filter)
 * On other platforms: No-op (no pre-filter available yet)
 */
package com.augmentalis.voiceoscore

/**
 * Speech pre-filter interface for command-mode acceleration.
 *
 * The pre-filter runs alongside the primary speech engine (e.g., Vivoka)
 * and processes the same audio. When it produces a high-confidence result,
 * the primary engine's result is suppressed to avoid duplicate commands.
 *
 * Lifecycle:
 * 1. Created and enabled during VoiceOSCore initialization
 * 2. startListening()/stopListening() called in sync with main engine
 * 3. Commands updated via updateCommands() whenever grammar changes
 * 4. Results emitted via onResult callback → VoiceOSCore.processCommand()
 * 5. Destroyed with VoiceOSCore.dispose()
 */
interface ISpeechPreFilter {

    /** Whether the pre-filter engine is loaded and ready */
    val isReady: Boolean

    /**
     * Check if pre-filter should be active for the given speech mode.
     * Typically active for COMMAND modes, inactive for DICTATION.
     */
    fun isActiveFor(speechMode: SpeechMode): Boolean

    /**
     * Start listening in parallel with the main speech engine.
     * Only starts if [isActiveFor] returns true for the given mode.
     */
    fun startListening(speechMode: SpeechMode)

    /** Stop listening */
    fun stopListening()

    /** Update hot words / command vocabulary */
    fun updateCommands(commands: List<String>)

    /**
     * Check if the pre-filter recently accepted a result matching [text].
     * Used by VoiceOSCore to suppress duplicate results from the primary engine.
     *
     * @param text The text to check against recent pre-filter acceptances
     * @param windowMs Time window in ms to consider (default 2000ms)
     * @return true if pre-filter handled this text recently
     */
    fun wasRecentlyAccepted(text: String, windowMs: Long = 2000L): Boolean

    /** Release all resources */
    fun destroy()

    /**
     * Result callback — set by VoiceOSCore during initialization.
     * Called when the pre-filter produces a high-confidence result.
     * The callback receives (text, confidence).
     */
    var onResult: ((text: String, confidence: Float) -> Unit)?
}
