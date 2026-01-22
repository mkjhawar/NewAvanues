package com.augmentalis.devicemanager.audio

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles 3D audio and spatial sound processing
 */
class SpatialAudio(private val context: Context) {
    
    companion object {
        private const val TAG = "SpatialAudio"
    }
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var virtualizer: Virtualizer? = null
    
    private val _spatialEnabled = MutableStateFlow(false)
    val spatialEnabled: StateFlow<Boolean> = _spatialEnabled
    
    private val _headTrackingEnabled = MutableStateFlow(false)
    val headTrackingEnabled: StateFlow<Boolean> = _headTrackingEnabled
    
    /**
     * Enable spatial audio with native support or fallback
     */
    fun enable(audioSessionId: Int): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Try native spatial audio first
                val spatializer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    audioManager.spatializer
                } else null
                if (spatializer != null && spatializer.isAvailable) {
                    if (!spatializer.isEnabled) {
                        // Note: System setting, can't directly enable
                        Log.w(TAG, "Spatial audio available but disabled in system settings")
                    }
                    _spatialEnabled.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) spatializer.isEnabled else false
                    _headTrackingEnabled.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) spatializer.isHeadTrackerAvailable else false
                    Log.d(TAG, "Native spatial audio: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) spatializer.isEnabled else false}")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) spatializer.isEnabled else false
                } else {
                    // Fallback to virtualizer
                    enableVirtualizer(audioSessionId)
                }
            } else {
                // Use virtualizer for older devices
                enableVirtualizer(audioSessionId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable spatial audio", e)
            false
        }
    }
    
    /**
     * Enable virtualizer as spatial audio fallback
     */
    private fun enableVirtualizer(audioSessionId: Int): Boolean {
        return try {
            virtualizer?.release()
            virtualizer = Virtualizer(0, audioSessionId).apply {
                setStrength(1000) // Maximum spatial effect
                enabled = true
            }
            _spatialEnabled.value = true
            _headTrackingEnabled.value = false // Not available with virtualizer
            Log.d(TAG, "Virtualizer enabled for spatial audio")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable virtualizer", e)
            false
        }
    }
    
    /**
     * Disable spatial audio
     */
    fun disable() {
        virtualizer?.apply {
            enabled = false
            release()
        }
        virtualizer = null
        _spatialEnabled.value = false
        _headTrackingEnabled.value = false
        Log.d(TAG, "Spatial audio disabled")
    }
    
    /**
     * Check if spatial audio is available
     */
    fun isAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val spatializer = audioManager.spatializer
            spatializer?.isAvailable == true
        } else {
            // Virtualizer is generally available on most devices
            true
        }
    }
    
    /**
     * Check if head tracking is available
     */
    fun isHeadTrackingAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val spatializer = audioManager.spatializer
            spatializer?.isHeadTrackerAvailable == true
        } else {
            false
        }
    }
    
    /**
     * Set spatial audio strength (for virtualizer mode)
     */
    fun setStrength(strength: Int): Boolean {
        return try {
            virtualizer?.setStrength(strength.coerceIn(0, 1000).toShort())
            Log.d(TAG, "Spatial strength: $strength")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set spatial strength", e)
            false
        }
    }
    
    /**
     * Get compatible output devices for spatial audio
     */
    fun getCompatibleDevices(): List<String> {
        val devices = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val spatializer = audioManager.spatializer
            if (spatializer != null && spatializer.isAvailable) {
                // Check common spatial audio compatible devices
                devices.add("Headphones")
                devices.add("Earbuds")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && spatializer.isHeadTrackerAvailable) {
                    devices.add("Head-tracking capable devices")
                }
            }
        } else {
            // Virtualizer works best with headphones
            devices.add("Headphones")
            devices.add("Earbuds")
        }
        
        return devices
    }
    
    /**
     * Apply spatial configuration
     */
    fun applyConfig(audioSessionId: Int, config: SpatialConfig): Boolean {
        return if (config.enabled) {
            val success = enable(audioSessionId)
            if (success && config.binauralMode && virtualizer != null) {
                // Apply binaural processing if using virtualizer
                setStrength(800) // Strong binaural effect
            }
            success
        } else {
            disable()
            true
        }
    }
    
    /**
     * Get current spatial status
     */
    fun getStatus(): SpatialConfig {
        return SpatialConfig(
            enabled = _spatialEnabled.value,
            headTracking = _headTrackingEnabled.value,
            binauralMode = virtualizer?.enabled == true
        )
    }
    
    /**
     * Release resources
     */
    fun release() {
        disable()
    }
}
