// filename: Modules/AVA/WakeWord/src/commonMain/kotlin/com/augmentalis/ava/features/wakeword/IWakeWordDetector.kt
// created: 2025-12-17
// author: Claude Code
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.wakeword

import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.flow.StateFlow

/**
 * Wake Word Detector Interface (KMP-compatible)
 *
 * Abstracts wake word detection for cross-platform use.
 * Platform-specific implementations handle actual audio processing
 * (Porcupine on Android, CoreML on iOS, etc.)
 *
 * Features:
 * - On-device processing (privacy-first)
 * - Low CPU and battery usage
 * - Configurable sensitivity
 * - Multiple wake word options
 *
 * @see WakeWordDetector for Android implementation
 *
 * @author Manoj Jhawar
 * @since 2025-12-17
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
     * Loads the wake word engine with specified settings and wake word.
     *
     * @param settings Wake word configuration
     * @param onDetected Callback when wake word is detected
     * @return Result.Success if initialized, Result.Error otherwise
     */
    suspend fun initialize(
        settings: WakeWordSettingsData,
        onDetected: (WakeWordKeyword) -> Unit
    ): Result<Unit>

    /**
     * Start wake word detection
     *
     * Begins listening for wake word. Must call initialize() first.
     *
     * @return Result.Success if started, Result.Error otherwise
     */
    suspend fun start(): Result<Unit>

    /**
     * Stop wake word detection
     *
     * Stops listening for wake word but keeps detector initialized.
     *
     * @return Result.Success if stopped, Result.Error otherwise
     */
    suspend fun stop(): Result<Unit>

    /**
     * Pause wake word detection
     *
     * Temporarily pauses detection (e.g., when screen is off for battery optimization).
     *
     * @param reason Reason for pausing (for logging)
     */
    suspend fun pause(reason: String)

    /**
     * Resume wake word detection
     *
     * Resumes detection after pause.
     */
    suspend fun resume()

    /**
     * Clean up resources
     *
     * Stops detection and releases the wake word engine.
     * Call this when wake word detection is no longer needed.
     */
    suspend fun cleanup()

    /**
     * Check if detector is currently listening
     */
    fun isListening(): Boolean
}
