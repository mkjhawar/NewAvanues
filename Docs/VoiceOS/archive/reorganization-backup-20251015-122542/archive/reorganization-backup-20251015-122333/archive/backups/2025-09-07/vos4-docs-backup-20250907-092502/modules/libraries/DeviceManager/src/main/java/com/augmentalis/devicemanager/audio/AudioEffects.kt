package com.augmentalis.devicemanager.audio

import android.media.audiofx.*
import android.util.Log

/**
 * Handles audio effects processing
 */
class AudioEffects {
    
    companion object {
        private const val TAG = "AudioEffects"
    }
    
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var reverb: EnvironmentalReverb? = null
    
    /**
     * Configure equalizer with preset or custom settings
     */
    fun configureEqualizer(audioSessionId: Int, preset: EqualizerPreset? = null): Boolean {
        return try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                if (preset != null) {
                    usePreset(preset.value.toShort())
                }
                enabled = true
            }
            Log.d(TAG, "Equalizer configured: ${preset?.name ?: "Custom"}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure equalizer", e)
            false
        }
    }
    
    /**
     * Set custom equalizer band levels
     */
    fun setEqualizerBandLevel(band: Short, level: Short): Boolean {
        return try {
            equalizer?.setBandLevel(band, level)
            Log.d(TAG, "Equalizer band $band set to $level")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set equalizer band", e)
            false
        }
    }
    
    /**
     * Configure bass boost
     */
    fun configureBassBoost(audioSessionId: Int, strength: Int = 500): Boolean {
        return try {
            bassBoost?.release()
            bassBoost = BassBoost(0, audioSessionId).apply {
                setStrength(strength.coerceIn(0, 1000).toShort())
                enabled = true
            }
            Log.d(TAG, "Bass boost: $strength")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure bass boost", e)
            false
        }
    }
    
    /**
     * Configure virtualizer for spatial widening
     */
    fun configureVirtualizer(audioSessionId: Int, strength: Int = 500): Boolean {
        return try {
            virtualizer?.release()
            virtualizer = Virtualizer(0, audioSessionId).apply {
                setStrength(strength.coerceIn(0, 1000).toShort())
                enabled = true
            }
            Log.d(TAG, "Virtualizer: $strength")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure virtualizer", e)
            false
        }
    }
    
    /**
     * Configure environmental reverb
     */
    fun configureReverb(audioSessionId: Int, preset: ReverbPreset): Boolean {
        return try {
            reverb?.release()
            
            if (preset == ReverbPreset.NONE) {
                reverb = null
                Log.d(TAG, "Reverb disabled")
                return true
            }
            
            reverb = EnvironmentalReverb(0, audioSessionId).apply {
                when (preset) {
                    ReverbPreset.SMALL_ROOM -> {
                        roomLevel = -1000
                        roomHFLevel = -600
                        decayTime = 1100
                        decayHFRatio = 830
                        reflectionsLevel = -400
                        reflectionsDelay = 5
                        reverbLevel = 200
                        reverbDelay = 10
                        diffusion = 1000
                        density = 1000
                    }
                    ReverbPreset.LARGE_ROOM -> {
                        roomLevel = -1000
                        roomHFLevel = -600
                        decayTime = 1800
                        decayHFRatio = 700
                        reflectionsLevel = -1300
                        reflectionsDelay = 15
                        reverbLevel = -800
                        reverbDelay = 30
                        diffusion = 1000
                        density = 1000
                    }
                    ReverbPreset.HALL -> {
                        roomLevel = -1000
                        roomHFLevel = -600
                        decayTime = 2900
                        decayHFRatio = 860
                        reflectionsLevel = -1000
                        reflectionsDelay = 20
                        reverbLevel = -300
                        reverbDelay = 40
                        diffusion = 1000
                        density = 1000
                    }
                    ReverbPreset.OUTDOOR -> {
                        roomLevel = -1000
                        roomHFLevel = -2500
                        decayTime = 1500
                        decayHFRatio = 210
                        reflectionsLevel = -2000
                        reflectionsDelay = 7
                        reverbLevel = -1400
                        reverbDelay = 15
                        diffusion = 1000
                        density = 200
                    }
                    else -> {} // NONE already handled
                }
                enabled = true
            }
            Log.d(TAG, "Reverb: $preset")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure reverb", e)
            false
        }
    }
    
    /**
     * Apply effect configuration
     */
    fun applyConfig(audioSessionId: Int, config: EffectConfig): Boolean {
        var success = true
        
        // Bass boost
        if (config.bassBoostStrength > 0) {
            success = configureBassBoost(audioSessionId, config.bassBoostStrength) && success
        } else {
            bassBoost?.apply {
                enabled = false
                release()
            }
            bassBoost = null
        }
        
        // Virtualizer
        if (config.virtualizerStrength > 0) {
            success = configureVirtualizer(audioSessionId, config.virtualizerStrength) && success
        } else {
            virtualizer?.apply {
                enabled = false
                release()
            }
            virtualizer = null
        }
        
        // Equalizer
        if (config.equalizerPreset != null) {
            success = configureEqualizer(audioSessionId, config.equalizerPreset) && success
        } else {
            equalizer?.apply {
                enabled = false
                release()
            }
            equalizer = null
        }
        
        // Reverb
        success = configureReverb(audioSessionId, config.reverbPreset) && success
        
        return success
    }
    
    /**
     * Get current effect status
     */
    fun getStatus(): EffectConfig {
        return EffectConfig(
            bassBoostStrength = bassBoost?.roundedStrength?.toInt() ?: 0,
            virtualizerStrength = virtualizer?.roundedStrength?.toInt() ?: 0,
            equalizerPreset = null, // Would need to track current preset
            reverbPreset = if (reverb?.enabled == true) ReverbPreset.SMALL_ROOM else ReverbPreset.NONE
        )
    }
    
    /**
     * Disable all effects
     */
    fun disableAll() {
        equalizer?.apply {
            enabled = false
            release()
        }
        equalizer = null
        
        bassBoost?.apply {
            enabled = false
            release()
        }
        bassBoost = null
        
        virtualizer?.apply {
            enabled = false
            release()
        }
        virtualizer = null
        
        reverb?.apply {
            enabled = false
            release()
        }
        reverb = null
        
        Log.d(TAG, "All effects disabled")
    }
    
    /**
     * Release all effects
     */
    fun release() {
        disableAll()
    }
}
