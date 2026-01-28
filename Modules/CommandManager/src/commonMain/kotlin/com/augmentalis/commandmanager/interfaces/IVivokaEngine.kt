/**
 * IVivokaEngine.kt - Vivoka speech engine interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * KMP interface for Vivoka speech recognition engine.
 * Platform-specific implementations:
 * - Android: Uses Vivoka SDK AAR via VivokaSDK wrapper
 * - iOS/Desktop: Stub (Vivoka not available)
 */
package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.ISpeechEngine
import com.augmentalis.commandmanager.SpeechEngine
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// ═══════════════════════════════════════════════════════════════════
// SEGREGATED INTERFACES (ISP compliance)
// ═══════════════════════════════════════════════════════════════════

/**
 * Wake word detection capability.
 */
interface IWakeWordCapable {
    /**
     * Whether wake word detection is enabled.
     */
    val isWakeWordEnabled: StateFlow<Boolean>

    /**
     * Wake word detection events.
     */
    val wakeWordDetected: SharedFlow<WakeWordEvent>

    /**
     * Enable wake word detection.
     */
    suspend fun enableWakeWord(wakeWord: String): Result<Unit>

    /**
     * Disable wake word detection.
     */
    suspend fun disableWakeWord(): Result<Unit>

    /**
     * Get available wake words.
     */
    fun getAvailableWakeWords(): List<String>
}

/**
 * Model management capability.
 */
interface IModelManageable {
    /**
     * Available Vivoka models.
     */
    val availableModels: StateFlow<List<VivokaModel>>

    /**
     * Currently loaded model.
     */
    val currentModel: StateFlow<VivokaModel?>

    /**
     * Load a specific Vivoka model.
     */
    suspend fun loadModel(modelId: String): Result<Unit>

    /**
     * Unload current model to free resources.
     */
    suspend fun unloadModel(): Result<Unit>

    /**
     * Check if a specific model is downloaded.
     */
    suspend fun isModelDownloaded(modelId: String): Boolean

    /**
     * Download a model for offline use.
     */
    suspend fun downloadModel(modelId: String, progressCallback: ((Float) -> Unit)? = null): Result<Unit>

    /**
     * Delete a downloaded model.
     */
    suspend fun deleteModel(modelId: String): Result<Unit>

    /**
     * Get disk space used by models.
     */
    suspend fun getModelsDiskUsage(): Long
}

/**
 * Vivoka-specific speech engine interface.
 *
 * Extends base ISpeechEngine with Vivoka-specific features
 * through composition of focused interfaces.
 */
interface IVivokaEngine : ISpeechEngine, IWakeWordCapable, IModelManageable

/**
 * Wake word detection event.
 */
data class WakeWordEvent(
    val wakeWord: String,
    val confidence: Float,
    val timestamp: Long
)

/**
 * Vivoka model information.
 */
data class VivokaModel(
    val id: String,
    val name: String,
    val language: String,
    val sizeBytes: Long,
    val isDownloaded: Boolean,
    val version: String,
    val features: Set<VivokaFeature> = emptySet()
)

/**
 * Vivoka feature flags.
 */
enum class VivokaFeature {
    OFFLINE_RECOGNITION,
    WAKE_WORD,
    SPEAKER_ID,
    NLU_INTEGRATION,
    CONTINUOUS_LISTENING,
    LOW_LATENCY
}

/**
 * Vivoka engine configuration.
 */
data class VivokaConfig(
    val modelId: String? = null,
    val wakeWord: String? = null,
    val enableNLU: Boolean = true,
    val enableSpeakerId: Boolean = false,
    val continuousListening: Boolean = true,
    val audioSampleRate: Int = 16000,
    val audioChannels: Int = 1,
    val maxSilenceMs: Int = 1500,
    val minSpeechMs: Int = 300
) {
    companion object {
        val DEFAULT = VivokaConfig()
    }
}
