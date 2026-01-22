/**
 * WakeWordPlugin.kt - Wake word detection plugin interface
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the contract for wake word detection plugins in the Universal Plugin system.
 * Implementations can provide always-on voice activation with minimal power consumption.
 */
package com.augmentalis.magiccode.plugins.universal.contracts.speech

import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import com.augmentalis.magiccode.plugins.universal.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

// =============================================================================
// Wake Word Event
// =============================================================================

/**
 * Event emitted when a wake word is detected.
 *
 * Contains information about the detected wake word including
 * the word itself, confidence level, and timestamp.
 *
 * ## Usage
 * ```kotlin
 * wakeWordPlugin.detectionEvents.collect { event ->
 *     if (event.confidence > 0.85f) {
 *         println("Wake word '${event.wakeWord}' detected!")
 *         activateVoiceCommand()
 *     }
 * }
 * ```
 *
 * @property wakeWord The wake word or phrase that was detected
 * @property confidence Detection confidence from 0.0 (low) to 1.0 (high)
 * @property timestamp When the wake word was detected (epoch millis)
 * @since 1.0.0
 */
@Serializable
data class WakeWordEvent(
    val wakeWord: String,
    val confidence: Float,
    val timestamp: Long = currentTimeMillis()
) {
    /**
     * Check if this detection has high confidence.
     *
     * @param threshold Minimum confidence threshold (default 0.8)
     * @return true if confidence exceeds the threshold
     */
    fun isHighConfidence(threshold: Float = 0.8f): Boolean = confidence >= threshold

    /**
     * Get age of this event in milliseconds.
     *
     * @return Milliseconds since detection
     */
    fun ageMs(): Long = currentTimeMillis() - timestamp

    /**
     * Check if this event is recent.
     *
     * @param maxAgeMs Maximum age to consider recent (default 5000ms)
     * @return true if event is within the specified age
     */
    fun isRecent(maxAgeMs: Long = 5000L): Boolean = ageMs() < maxAgeMs

    companion object {
        /**
         * Create a wake word event with current timestamp.
         *
         * @param wakeWord The detected wake word
         * @param confidence Detection confidence
         * @return New WakeWordEvent
         */
        fun detected(wakeWord: String, confidence: Float): WakeWordEvent {
            return WakeWordEvent(
                wakeWord = wakeWord,
                confidence = confidence
            )
        }
    }
}

// =============================================================================
// Wake Word Plugin Interface
// =============================================================================

/**
 * Wake word detection plugin interface.
 *
 * Extends [UniversalPlugin] to provide always-on wake word detection.
 * Wake word engines listen continuously for trigger phrases with
 * minimal resource usage, activating full recognition only when triggered.
 *
 * ## Design Goals
 * - **Low Power**: Optimized for continuous background listening
 * - **Low Latency**: Fast detection response
 * - **Customizable**: Support for custom wake words
 * - **Adjustable**: Sensitivity tuning for different environments
 *
 * ## Implementation Example
 * ```kotlin
 * class PorcupineWakeWordPlugin : WakeWordPlugin {
 *     override val pluginId = "com.augmentalis.wakeword.porcupine"
 *     override val pluginName = "Porcupine Wake Word Engine"
 *     override val version = "1.0.0"
 *
 *     private val _wakeWords = mutableListOf("hey assistant")
 *     override val wakeWords: List<String> get() = _wakeWords.toList()
 *
 *     private var _sensitivity = 0.5f
 *     override val sensitivity: Float get() = _sensitivity
 *
 *     private val _isListening = MutableStateFlow(false)
 *     override val isListening: StateFlow<Boolean> = _isListening
 *
 *     private val _detectionEvents = MutableSharedFlow<WakeWordEvent>()
 *     override val detectionEvents: Flow<WakeWordEvent> = _detectionEvents
 *
 *     override suspend fun startListening(): Result<Unit> {
 *         _isListening.value = true
 *         // Start low-power audio processing loop
 *         return Result.success(Unit)
 *     }
 *
 *     override fun stopListening() {
 *         _isListening.value = false
 *     }
 *
 *     // ... implement other methods
 * }
 * ```
 *
 * ## Usage Pattern
 * ```kotlin
 * // Initialize wake word detection
 * val wakePlugin = pluginRegistry.getPlugin<WakeWordPlugin>("com.augmentalis.wakeword.porcupine")
 *
 * // Set sensitivity based on environment
 * wakePlugin.setSensitivity(0.7f)
 *
 * // Start listening and handle detections
 * launch {
 *     wakePlugin.startListening()
 *     wakePlugin.detectionEvents.collect { event ->
 *         if (event.isHighConfidence()) {
 *             // Activate full speech recognition
 *             speechEngine.startRecognition()
 *         }
 *     }
 * }
 * ```
 *
 * ## Capability Registration
 * Wake word plugins should register the capability:
 * `com.augmentalis.capability.speech.wakeword`
 *
 * @since 1.0.0
 * @see UniversalPlugin
 * @see WakeWordEvent
 */
interface WakeWordPlugin : UniversalPlugin {

    // =========================================================================
    // Wake Word Configuration
    // =========================================================================

    /**
     * List of currently active wake words.
     *
     * These are the phrases that will trigger a [WakeWordEvent]
     * when detected. Wake words are typically short phrases like
     * "Hey Jarvis" or "OK Google".
     *
     * ## Thread Safety
     * This list may be modified via [addWakeWord] and [removeWakeWord].
     * Implementations should ensure thread-safe access.
     *
     * @see addWakeWord
     * @see removeWakeWord
     */
    val wakeWords: List<String>

    /**
     * Current detection sensitivity.
     *
     * Controls the trade-off between false positives and false negatives:
     * - **Lower sensitivity** (0.0 - 0.3): Fewer false positives, may miss some utterances
     * - **Medium sensitivity** (0.4 - 0.6): Balanced for typical environments
     * - **Higher sensitivity** (0.7 - 1.0): More detections, may have more false positives
     *
     * @see setSensitivity
     */
    val sensitivity: Float

    /**
     * Observable listening state.
     *
     * Emits `true` when the engine is actively listening for wake words,
     * `false` when stopped.
     *
     * ## Usage
     * ```kotlin
     * wakePlugin.isListening.collect { listening ->
     *     updateMicrophoneIndicator(listening)
     * }
     * ```
     */
    val isListening: StateFlow<Boolean>

    /**
     * Flow of wake word detection events.
     *
     * Emits a [WakeWordEvent] each time a wake word is detected.
     * Multiple collectors can subscribe to this flow.
     *
     * ## Buffering
     * Events may be buffered briefly. Collectors should handle
     * events promptly to avoid missing detections in rapid succession.
     *
     * @see WakeWordEvent
     */
    val detectionEvents: Flow<WakeWordEvent>

    // =========================================================================
    // Listening Operations
    // =========================================================================

    /**
     * Start listening for wake words.
     *
     * Begins continuous background audio processing to detect
     * configured wake words. This operation should be low-power
     * and suitable for always-on listening.
     *
     * ## Prerequisites
     * - Plugin must be initialized and in ACTIVE state
     * - Microphone permission must be granted
     * - At least one wake word must be configured
     *
     * ## Resource Usage
     * Wake word detection is designed for low power consumption.
     * However, implementations may vary in their resource usage.
     * Consider battery impact in mobile applications.
     *
     * @return Result indicating success or failure
     * @see stopListening
     * @see isListening
     */
    suspend fun startListening(): Result<Unit>

    /**
     * Stop listening for wake words.
     *
     * Stops the audio processing loop and releases audio resources.
     * After calling stop:
     * - [isListening] emits `false`
     * - No more [detectionEvents] are emitted
     * - Audio capture stops
     *
     * If not currently listening, this method does nothing.
     *
     * @see startListening
     */
    fun stopListening()

    // =========================================================================
    // Wake Word Management
    // =========================================================================

    /**
     * Add a wake word to the detection list.
     *
     * The new wake word becomes active immediately if currently listening.
     * Some implementations may require a brief restart of detection.
     *
     * ## Limitations
     * - Maximum number of wake words may be limited by implementation
     * - Some wake words may not be supported (too short, too common)
     * - Custom wake words may require model generation
     *
     * @param word The wake word or phrase to add
     * @return Result indicating success or failure
     * @throws IllegalArgumentException if wake word is invalid
     * @see removeWakeWord
     */
    suspend fun addWakeWord(word: String): Result<Unit>

    /**
     * Remove a wake word from the detection list.
     *
     * The wake word is no longer detected after removal.
     * Removal takes effect immediately.
     *
     * @param word The wake word to remove
     * @return Result indicating success or failure
     * @throws IllegalArgumentException if wake word not found
     * @see addWakeWord
     */
    suspend fun removeWakeWord(word: String): Result<Unit>

    // =========================================================================
    // Sensitivity Control
    // =========================================================================

    /**
     * Set the detection sensitivity.
     *
     * Adjusts the trade-off between false positives and false negatives.
     * Takes effect immediately, even while listening.
     *
     * ## Recommendations
     * - **Quiet environment**: Use lower sensitivity (0.3 - 0.5)
     * - **Noisy environment**: Use higher sensitivity (0.6 - 0.8)
     * - **Accessibility**: Consider higher sensitivity for motor-impaired users
     *
     * @param sensitivity Value from 0.0 (least sensitive) to 1.0 (most sensitive)
     * @throws IllegalArgumentException if sensitivity is out of range
     * @see sensitivity
     */
    fun setSensitivity(sensitivity: Float)
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Check if a specific wake word is configured.
 *
 * @param word Wake word to check
 * @return true if the wake word is in the list
 */
fun WakeWordPlugin.hasWakeWord(word: String): Boolean {
    return word.lowercase() in wakeWords.map { it.lowercase() }
}

/**
 * Check if currently listening for wake words.
 *
 * @return true if listening is active
 */
fun WakeWordPlugin.isCurrentlyListening(): Boolean {
    return isListening.value
}

/**
 * Get the number of configured wake words.
 *
 * @return Count of wake words
 */
fun WakeWordPlugin.wakeWordCount(): Int {
    return wakeWords.size
}

/**
 * Check if any wake words are configured.
 *
 * @return true if at least one wake word is configured
 */
fun WakeWordPlugin.hasAnyWakeWords(): Boolean {
    return wakeWords.isNotEmpty()
}

/**
 * Set sensitivity to a low level (fewer false positives).
 *
 * Equivalent to `setSensitivity(0.3f)`.
 */
fun WakeWordPlugin.setLowSensitivity() {
    setSensitivity(0.3f)
}

/**
 * Set sensitivity to a medium level (balanced).
 *
 * Equivalent to `setSensitivity(0.5f)`.
 */
fun WakeWordPlugin.setMediumSensitivity() {
    setSensitivity(0.5f)
}

/**
 * Set sensitivity to a high level (more detections).
 *
 * Equivalent to `setSensitivity(0.7f)`.
 */
fun WakeWordPlugin.setHighSensitivity() {
    setSensitivity(0.7f)
}

/**
 * Replace all wake words with a new list.
 *
 * Removes all existing wake words and adds the new ones.
 *
 * @param words New list of wake words
 * @return Result indicating success or failure
 */
suspend fun WakeWordPlugin.setWakeWords(words: List<String>): Result<Unit> {
    // Remove existing wake words
    wakeWords.toList().forEach { word ->
        val result = removeWakeWord(word)
        if (result.isFailure) return result
    }

    // Add new wake words
    words.forEach { word ->
        val result = addWakeWord(word)
        if (result.isFailure) return result
    }

    return Result.success(Unit)
}

/**
 * Clear all wake words.
 *
 * Removes all configured wake words. Detection will not trigger
 * until new wake words are added.
 *
 * @return Result indicating success or failure
 */
suspend fun WakeWordPlugin.clearWakeWords(): Result<Unit> {
    wakeWords.toList().forEach { word ->
        val result = removeWakeWord(word)
        if (result.isFailure) return result
    }
    return Result.success(Unit)
}

/**
 * Start listening with safety check.
 *
 * Ensures at least one wake word is configured before starting.
 *
 * @return Result indicating success or failure
 */
suspend fun WakeWordPlugin.startListeningSafe(): Result<Unit> {
    if (!hasAnyWakeWords()) {
        return Result.failure(IllegalStateException("No wake words configured"))
    }
    return startListening()
}
