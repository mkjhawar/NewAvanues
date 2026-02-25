// filename: Modules/AVA/WakeWord/src/commonMain/kotlin/com/augmentalis/ava/features/wakeword/IWakeWordSettingsRepository.kt
// created: 2025-12-17
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.wakeword

import kotlinx.coroutines.flow.Flow

/**
 * Wake Word Settings Repository Interface (KMP-compatible)
 *
 * Abstracts wake word configuration persistence for cross-platform use.
 * Platform-specific implementations handle actual storage (DataStore on Android,
 * UserDefaults on iOS, etc.)
 *
 * @see WakeWordSettingsRepository for Android implementation
 *
 * @author Manoj Jhawar
 * @since 2025-12-17
 */
interface IWakeWordSettingsRepository {
    /**
     * Get wake word settings as Flow
     */
    val settings: Flow<WakeWordSettingsData>

    /**
     * Update wake word settings
     */
    suspend fun updateSettings(settings: WakeWordSettingsData)

    /**
     * Update enabled state
     */
    suspend fun setEnabled(enabled: Boolean)

    /**
     * Update keyword
     */
    suspend fun setKeyword(keyword: WakeWordKeyword)

    /**
     * Update sensitivity
     */
    suspend fun setSensitivity(sensitivity: Float)

    /**
     * Update battery optimization
     */
    suspend fun setBatteryOptimization(enabled: Boolean)

    /**
     * Reset to defaults
     */
    suspend fun resetToDefaults()
}
