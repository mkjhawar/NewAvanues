// filename: Modules/AVA/WakeWord/src/commonMain/kotlin/com/augmentalis/ava/features/wakeword/WakeWordModels.kt
// created: 2025-11-22
// updated: 2025-12-17 - Converted to KMP-compatible format
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.wakeword

import kotlinx.datetime.Clock

/**
 * Wake word detection configuration and models (KMP-compatible)
 *
 * Supports Porcupine wake word engine for hands-free "Hey AVA" activation.
 *
 * @author Manoj Jhawar
 */

/**
 * Wake word keyword options
 */
enum class WakeWordKeyword(val displayName: String, val porcupineKeyword: String) {
    /**
     * "Hey AVA" wake word (custom trained)
     */
    HEY_AVA("Hey AVA", "hey-ava"),

    /**
     * "OK AVA" wake word (custom trained)
     */
    OK_AVA("OK AVA", "ok-ava"),

    /**
     * Built-in "Hey Google" style (for testing)
     */
    JARVIS("Jarvis", "jarvis"),

    /**
     * Built-in "Alexa" style (for testing)
     */
    ALEXA("Alexa", "alexa"),

    /**
     * Built-in "Computer" (for testing)
     */
    COMPUTER("Computer", "computer");

    companion object {
        /**
         * Get keyword from Porcupine keyword string
         */
        fun fromPorcupineKeyword(keyword: String): WakeWordKeyword? {
            return entries.firstOrNull { it.porcupineKeyword == keyword }
        }
    }
}

/**
 * Wake word detection settings (KMP-compatible base)
 *
 * Platform-specific implementations may extend this for serialization
 * (e.g., Parcelable on Android).
 */
data class WakeWordSettingsData(
    /**
     * Enable/disable wake word detection
     */
    val enabled: Boolean = false,

    /**
     * Selected wake word keyword
     */
    val keyword: WakeWordKeyword = WakeWordKeyword.HEY_AVA,

    /**
     * Detection sensitivity (0.0 - 1.0)
     *
     * Lower values = fewer false positives, more false negatives
     * Higher values = more false positives, fewer false negatives
     *
     * Default: 0.5 (balanced)
     */
    val sensitivity: Float = 0.5f,

    /**
     * Enable background listening (requires foreground service)
     */
    val backgroundListening: Boolean = true,

    /**
     * Battery optimization - pause detection when screen is off
     */
    val batteryOptimization: Boolean = true,

    /**
     * Show persistent notification when listening
     */
    val showNotification: Boolean = true,

    /**
     * Play sound feedback when wake word detected
     */
    val playSoundFeedback: Boolean = true,

    /**
     * Vibrate when wake word detected
     */
    val vibrateOnDetection: Boolean = false
)

/**
 * Wake word detection state
 */
enum class WakeWordState {
    /**
     * Not initialized
     */
    UNINITIALIZED,

    /**
     * Initializing Porcupine engine
     */
    INITIALIZING,

    /**
     * Listening for wake word
     */
    LISTENING,

    /**
     * Paused (screen off or battery optimization)
     */
    PAUSED,

    /**
     * Stopped
     */
    STOPPED,

    /**
     * Error state
     */
    ERROR
}

/**
 * Wake word detection event
 */
sealed class WakeWordEvent {
    /**
     * Wake word detected
     */
    data class Detected(
        val keyword: WakeWordKeyword,
        val timestamp: Long = Clock.System.now().toEpochMilliseconds()
    ) : WakeWordEvent()

    /**
     * Detection started
     */
    data object Started : WakeWordEvent()

    /**
     * Detection stopped
     */
    data object Stopped : WakeWordEvent()

    /**
     * Detection paused
     */
    data class Paused(val reason: String) : WakeWordEvent()

    /**
     * Detection resumed
     */
    data object Resumed : WakeWordEvent()

    /**
     * Error occurred
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : WakeWordEvent()
}

/**
 * Wake word detection statistics
 */
data class WakeWordStats(
    /**
     * Total detections since last reset
     */
    val totalDetections: Int = 0,

    /**
     * False positives (user corrections)
     */
    val falsePositives: Int = 0,

    /**
     * Average battery consumption per hour (mAh)
     */
    val avgBatteryPerHour: Double = 0.0,

    /**
     * Total listening time in seconds
     */
    val totalListeningTimeSeconds: Long = 0,

    /**
     * Last detection timestamp
     */
    val lastDetection: Long? = null,

    /**
     * Detection accuracy (1.0 - false positive rate)
     */
    val accuracy: Float = 1.0f
) {
    /**
     * Calculate false positive rate
     */
    fun falsePositiveRate(): Float {
        if (totalDetections == 0) return 0.0f
        return falsePositives.toFloat() / totalDetections.toFloat()
    }
}
