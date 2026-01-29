package com.augmentalis.voiceisolation

import android.content.Context
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of VoiceIsolation using native audio effects.
 *
 * Uses Android's built-in audio effects:
 * - NoiseSuppressor: Removes background noise
 * - AcousticEchoCanceler: Removes echo feedback
 * - AutomaticGainControl: Normalizes audio levels
 */
actual class VoiceIsolation(
    private val context: Context
) {
    companion object {
        private const val TAG = "VoiceIsolation"
    }

    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var gainControl: AutomaticGainControl? = null

    private var currentConfig = VoiceIsolationConfig.DEFAULT
    private var isInitialized = false

    private val _state = MutableStateFlow(
        VoiceIsolationState(
            isEnabled = true,
            isActive = false,
            config = currentConfig,
            availability = checkFeatureAvailability()
        )
    )
    actual val state: StateFlow<VoiceIsolationState> = _state.asStateFlow()

    actual fun initialize(audioSessionId: Int, config: VoiceIsolationConfig): Boolean {
        if (isInitialized) {
            Log.w(TAG, "Already initialized - releasing existing resources first")
            release()
        }

        currentConfig = config
        var success = true

        if (config.enabled) {
            // Enable noise suppression
            if (config.noiseSuppression) {
                success = enableNoiseSuppression(audioSessionId) && success
            }

            // Enable echo cancellation
            if (config.echoCancellation) {
                success = enableEchoCancellation(audioSessionId) && success
            }

            // Enable automatic gain control
            if (config.automaticGainControl) {
                success = enableAutomaticGainControl(audioSessionId) && success
            }
        }

        isInitialized = true
        updateState()

        Log.d(TAG, "Initialized with config: $config, success: $success")
        return success
    }

    actual fun process(audioData: ByteArray): ByteArray {
        // If disabled, return audio unchanged (passthrough)
        if (!currentConfig.enabled || !isInitialized) {
            return audioData
        }

        // Android's audio effects are applied at the hardware level when attached
        // to an audio session. The effects process audio automatically without
        // needing explicit byte-by-byte processing.
        //
        // For software-based processing (e.g., custom DSP), this would be where
        // we apply noise reduction algorithms. Currently, we rely on the hardware
        // effects which are already processing the audio stream.
        return audioData
    }

    actual fun updateConfig(config: VoiceIsolationConfig) {
        val wasEnabled = currentConfig.enabled
        currentConfig = config

        if (!isInitialized) {
            updateState()
            return
        }

        // Handle master toggle
        if (config.enabled != wasEnabled) {
            if (config.enabled) {
                // Re-enable all configured features
                // Note: We need the audio session ID to reinitialize
                Log.d(TAG, "Config changed to enabled - features will be applied on next initialize()")
            } else {
                // Disable all features
                disableAllEffects()
            }
        } else if (config.enabled) {
            // Update individual features
            noiseSuppressor?.enabled = config.noiseSuppression
            echoCanceler?.enabled = config.echoCancellation
            gainControl?.enabled = config.automaticGainControl
        }

        updateState()
        Log.d(TAG, "Config updated: $config")
    }

    actual fun isEnabled(): Boolean = currentConfig.enabled

    actual fun toggle(enabled: Boolean) {
        if (currentConfig.enabled == enabled) return

        currentConfig = currentConfig.copy(enabled = enabled)

        if (enabled) {
            // Features will be enabled on next initialize() call
            Log.d(TAG, "Voice isolation toggled ON - will apply on next audio session")
        } else {
            disableAllEffects()
            Log.d(TAG, "Voice isolation toggled OFF - passthrough mode")
        }

        updateState()
    }

    actual fun getAvailability(): Map<String, Boolean> {
        return mapOf(
            "noiseSuppression" to NoiseSuppressor.isAvailable(),
            "echoCancellation" to AcousticEchoCanceler.isAvailable(),
            "automaticGainControl" to AutomaticGainControl.isAvailable()
        )
    }

    actual fun release() {
        disableAllEffects()
        isInitialized = false
        updateState()
        Log.d(TAG, "Released all voice isolation resources")
    }

    // Private helper methods

    private fun enableNoiseSuppression(audioSessionId: Int): Boolean {
        return try {
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor?.release()
                noiseSuppressor = NoiseSuppressor.create(audioSessionId)?.apply {
                    enabled = true
                }
                val success = noiseSuppressor != null
                Log.d(TAG, "Noise suppression: ${if (success) "enabled" else "failed"}")
                success
            } else {
                Log.w(TAG, "Noise suppression not available on this device")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable noise suppression", e)
            false
        }
    }

    private fun enableEchoCancellation(audioSessionId: Int): Boolean {
        return try {
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler?.release()
                echoCanceler = AcousticEchoCanceler.create(audioSessionId)?.apply {
                    enabled = true
                }
                val success = echoCanceler != null
                Log.d(TAG, "Echo cancellation: ${if (success) "enabled" else "failed"}")
                success
            } else {
                Log.w(TAG, "Echo cancellation not available on this device")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable echo cancellation", e)
            false
        }
    }

    private fun enableAutomaticGainControl(audioSessionId: Int): Boolean {
        return try {
            if (AutomaticGainControl.isAvailable()) {
                gainControl?.release()
                gainControl = AutomaticGainControl.create(audioSessionId)?.apply {
                    enabled = true
                }
                val success = gainControl != null
                Log.d(TAG, "AGC: ${if (success) "enabled" else "failed"}")
                success
            } else {
                Log.w(TAG, "AGC not available on this device")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable AGC", e)
            false
        }
    }

    private fun disableAllEffects() {
        noiseSuppressor?.apply {
            enabled = false
            release()
        }
        noiseSuppressor = null

        echoCanceler?.apply {
            enabled = false
            release()
        }
        echoCanceler = null

        gainControl?.apply {
            enabled = false
            release()
        }
        gainControl = null
    }

    private fun checkFeatureAvailability(): FeatureAvailability {
        return FeatureAvailability(
            noiseSuppression = NoiseSuppressor.isAvailable(),
            echoCancellation = AcousticEchoCanceler.isAvailable(),
            automaticGainControl = AutomaticGainControl.isAvailable()
        )
    }

    private fun updateState() {
        _state.value = VoiceIsolationState(
            isEnabled = currentConfig.enabled,
            isActive = isInitialized && currentConfig.enabled,
            config = currentConfig,
            availability = checkFeatureAvailability()
        )
    }
}

/**
 * Android factory for creating VoiceIsolation instances.
 */
actual object VoiceIsolationFactory {
    private var applicationContext: Context? = null

    /**
     * Initialize the factory with application context.
     * Must be called before create().
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    actual fun create(): VoiceIsolation {
        val context = applicationContext ?: throw IllegalStateException(
            "VoiceIsolationFactory.initialize(context) must be called before create()"
        )
        return VoiceIsolation(context)
    }

    /**
     * Create a VoiceIsolation instance with explicit context.
     */
    fun create(context: Context): VoiceIsolation {
        return VoiceIsolation(context)
    }
}
