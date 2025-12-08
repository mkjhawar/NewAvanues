package com.augmentalis.devicemanager.audio

import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log

/**
 * Handles audio enhancement features for voice processing
 */
class AudioEnhancement {
    
    companion object {
        private const val TAG = "AudioEnhancement"
    }
    
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var gainControl: AutomaticGainControl? = null
    
    /**
     * Enable echo cancellation
     */
    fun enableEchoCancellation(audioSessionId: Int): Boolean {
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
                Log.w(TAG, "Echo cancellation not available")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable echo cancellation", e)
            false
        }
    }
    
    /**
     * Disable echo cancellation
     */
    fun disableEchoCancellation() {
        echoCanceler?.apply {
            enabled = false
            release()
        }
        echoCanceler = null
        Log.d(TAG, "Echo cancellation disabled")
    }
    
    /**
     * Enable noise suppression
     */
    fun enableNoiseSuppression(audioSessionId: Int): Boolean {
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
                Log.w(TAG, "Noise suppression not available")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable noise suppression", e)
            false
        }
    }
    
    /**
     * Disable noise suppression
     */
    fun disableNoiseSuppression() {
        noiseSuppressor?.apply {
            enabled = false
            release()
        }
        noiseSuppressor = null
        Log.d(TAG, "Noise suppression disabled")
    }
    
    /**
     * Enable automatic gain control
     */
    fun enableAutomaticGainControl(audioSessionId: Int): Boolean {
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
                Log.w(TAG, "AGC not available")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable AGC", e)
            false
        }
    }
    
    /**
     * Disable automatic gain control
     */
    fun disableAutomaticGainControl() {
        gainControl?.apply {
            enabled = false
            release()
        }
        gainControl = null
        Log.d(TAG, "AGC disabled")
    }
    
    /**
     * Apply enhancement configuration
     */
    fun applyConfig(audioSessionId: Int, config: EnhancementConfig): Boolean {
        var success = true
        
        // Echo cancellation
        if (config.echoCancellation) {
            success = enableEchoCancellation(audioSessionId) && success
        } else {
            disableEchoCancellation()
        }
        
        // Noise suppression
        if (config.noiseSuppression) {
            success = enableNoiseSuppression(audioSessionId) && success
        } else {
            disableNoiseSuppression()
        }
        
        // Automatic gain control
        if (config.automaticGainControl) {
            success = enableAutomaticGainControl(audioSessionId) && success
        } else {
            disableAutomaticGainControl()
        }
        
        return success
    }
    
    /**
     * Get current enhancement status
     */
    fun getStatus(): EnhancementConfig {
        return EnhancementConfig(
            echoCancellation = echoCanceler?.enabled == true,
            noiseSuppression = noiseSuppressor?.enabled == true,
            automaticGainControl = gainControl?.enabled == true
        )
    }
    
    /**
     * Check feature availability
     */
    fun checkAvailability(): Map<String, Boolean> {
        return mapOf(
            "echoCancellation" to AcousticEchoCanceler.isAvailable(),
            "noiseSuppression" to NoiseSuppressor.isAvailable(),
            "automaticGainControl" to AutomaticGainControl.isAvailable()
        )
    }
    
    /**
     * Release all enhancements
     */
    fun release() {
        disableEchoCancellation()
        disableNoiseSuppression()
        disableAutomaticGainControl()
        Log.d(TAG, "All enhancements released")
    }
}
