// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.devicemanager.audio

import android.media.AudioFormat

/**
 * Audio configuration parameters for recording
 * Immutable data class following Single Responsibility Principle
 */
data class AudioConfig(
    val sampleRate: Int = 16000,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val bufferSizeMultiplier: Int = 2,
    val useVoiceRecognition: Boolean = true,
    val noiseSuppression: Boolean = true,
    val echoCancellation: Boolean = false,
    val automaticGainControl: Boolean = true
) {
    
    companion object {
        /**
         * Default parameters for speech recognition
         */
        fun forSpeechRecognition() = AudioConfig(
            sampleRate = 16000,
            channelConfig = AudioFormat.CHANNEL_IN_MONO,
            audioFormat = AudioFormat.ENCODING_PCM_16BIT,
            useVoiceRecognition = true,
            noiseSuppression = true,
            automaticGainControl = true
        )
        
        /**
         * High quality parameters for dictation
         */
        fun forDictation() = AudioConfig(
            sampleRate = 44100,
            channelConfig = AudioFormat.CHANNEL_IN_MONO,
            audioFormat = AudioFormat.ENCODING_PCM_16BIT,
            useVoiceRecognition = true,
            noiseSuppression = true,
            echoCancellation = true,
            automaticGainControl = true
        )
        
        /**
         * Low latency parameters for wake word detection
         */
        fun forWakeWord() = AudioConfig(
            sampleRate = 16000,
            channelConfig = AudioFormat.CHANNEL_IN_MONO,
            audioFormat = AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeMultiplier = 1,
            useVoiceRecognition = true,
            noiseSuppression = false,
            automaticGainControl = false
        )
    }
    
    /**
     * Calculate buffer size based on parameters
     */
    fun getBufferSize(): Int {
        val minBufferSize = android.media.AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        )
        return minBufferSize * bufferSizeMultiplier
    }
}