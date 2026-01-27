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
 * Supports phoneme-based wake word detection for hands-free "Hey AVA" activation.
 *
 * @author Manoj Jhawar
 */

/**
 * Wake word keyword options
 *
 * Each keyword includes its IPA phoneme pattern for phoneme-based detection.
 * Phoneme patterns use ARPAbet notation (CMU dictionary compatible).
 */
enum class WakeWordKeyword(
    val displayName: String,
    val phonemePattern: String
) {
    /**
     * "Hey AVA" wake word
     * IPA: /heɪ ˈɑːvə/
     */
    HEY_AVA("Hey AVA", "HH EY1 AA1 V AH0"),

    /**
     * "OK AVA" wake word
     * IPA: /oʊˈkeɪ ˈɑːvə/
     */
    OK_AVA("OK AVA", "OW1 K EY1 AA1 V AH0"),

    /**
     * "Computer" wake word
     * IPA: /kəmˈpjuːtər/
     */
    COMPUTER("Computer", "K AH0 M P Y UW1 T ER0"),

    /**
     * Custom user-defined wake word (phoneme pattern set at runtime)
     */
    CUSTOM("Custom", "");

    companion object {
        /**
         * Get keyword from display name
         */
        fun fromDisplayName(name: String): WakeWordKeyword? {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
        }

        /**
         * Create custom wake word with user-defined phoneme pattern
         */
        fun custom(displayName: String, phonemePattern: String): WakeWordKeyword {
            // Note: For truly custom keywords, use CUSTOM and set pattern via settings
            return CUSTOM
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
     * Initializing phoneme detection engine
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
