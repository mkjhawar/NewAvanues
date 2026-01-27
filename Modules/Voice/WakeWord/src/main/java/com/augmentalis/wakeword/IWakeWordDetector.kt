// filename: Modules/Voice/WakeWord/src/main/java/com/augmentalis/wakeword/IWakeWordDetector.kt
// created: 2026-01-27
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.wakeword

import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.flow.StateFlow

/**
 * Wake Word Detector Interface
 *
 * Abstracts wake word detection for different implementations:
 * - PhonemeWakeWordDetector: Phoneme-based pattern matching (current)
 * - Future: Neural network models, cloud-based, etc.
 *
 * @author Manoj Jhawar
 */
interface IWakeWordDetector {
    /**
     * Current detection state
     */
    val state: StateFlow<WakeWordState>

    /**
     * Total detection count since initialization
     */
    val detectionCount: StateFlow<Int>

    /**
     * Initialize wake word detector
     *
     * @param settings Wake word configuration
     * @param onDetected Callback when wake word is detected
     * @return Result.Success if initialized, Result.Error otherwise
     */
    suspend fun initialize(
        settings: WakeWordSettings,
        onDetected: (WakeWordKeyword) -> Unit
    ): Result<Unit>

    /**
     * Start wake word detection
     *
     * @return Result.Success if started, Result.Error otherwise
     */
    suspend fun start(): Result<Unit>

    /**
     * Stop wake word detection
     *
     * @return Result.Success if stopped, Result.Error otherwise
     */
    suspend fun stop(): Result<Unit>

    /**
     * Pause wake word detection
     *
     * @param reason Reason for pausing (for logging)
     */
    suspend fun pause(reason: String)

    /**
     * Resume wake word detection
     */
    suspend fun resume()

    /**
     * Clean up resources
     */
    suspend fun cleanup()

    /**
     * Check if detector is currently listening
     */
    fun isListening(): Boolean
}
