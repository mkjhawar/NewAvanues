/**
 * WhisperProcessor.kt - Audio processing component for Whisper engine
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * Handles audio stream processing, VAD, noise reduction, and audio format conversion.
 * Separated from monolithic WhisperEngine for better maintainability and testability.
 */
package com.augmentalis.voiceos.speech.engines.whisper

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.augmentalis.voiceos.speech.engines.common.AudioStateManager
import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.sqrt
import com.augmentalis.voiceisolation.VoiceIsolation
import com.augmentalis.voiceisolation.VoiceIsolationConfig

/**
 * Whisper processing modes
 */
enum class WhisperProcessingMode {
    REAL_TIME,      // Stream processing with minimal latency
    BATCH,          // Process complete audio segments for maximum accuracy
    HYBRID          // Combine real-time with batch corrections
}

/**
 * Voice Activity Detection state
 */
private enum class VADState {
    SILENCE,
    SPEECH
}

/**
 * Audio buffer information
 */
data class AudioBufferInfo(
    val sampleRate: Int,
    val channels: Int,
    val encoding: Int,
    val bufferSize: Int,
    val durationMs: Long
)

/**
 * Manages audio stream processing, VAD, noise reduction, and format conversion for Whisper.
 * Handles real-time audio capture and preprocessing before sending to native inference.
 */
class WhisperProcessor(
    private val audioStateManager: AudioStateManager,
    private val performanceMonitor: PerformanceMonitor,
    private val voiceIsolation: VoiceIsolation? = null
) {
    
    companion object {
        private const val TAG = "WhisperProcessor"
        
        // Audio configuration constants
        private const val SAMPLE_RATE = 16000 // Whisper expects 16kHz
        private const val CHANNELS = 1 // Mono
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        
        // Buffer sizes for different processing modes
        private const val REAL_TIME_BUFFER_SIZE = 5    // ~0.5 seconds at 16kHz
        private const val HYBRID_BUFFER_SIZE = 15      // ~1.5 seconds
        private const val BATCH_BUFFER_SIZE = 50       // ~5 seconds
        
        // VAD parameters
        private const val SILENCE_TIMEOUT = 500L       // 500ms of silence to transition
        private const val ENERGY_HISTORY_SIZE = 50
        private const val VAD_SENSITIVITY_DEFAULT = 0.5f
    }
    
    // Audio recording
    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private val isProcessing = AtomicBoolean(false)
    
    // Processing configuration
    private var processingMode: WhisperProcessingMode = WhisperProcessingMode.HYBRID
    private var noiseReductionLevel: Float = 0.7f
    private var vadSensitivity: Float = VAD_SENSITIVITY_DEFAULT
    
    // Audio buffers
    private val audioBuffer = ArrayList<ByteArray>()
    private val audioBufferMutex = Mutex()
    
    // VAD state
    private var vadState = VADState.SILENCE
    private var silenceStartTime = 0L
    private var speechStartTime = 0L
    private val energyHistory = mutableListOf<Float>()
    
    // Buffer size calculation
    private val bufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, 
        AudioFormat.CHANNEL_IN_MONO, 
        AUDIO_FORMAT
    ) * 2
    
    // Coroutines
    private val processingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioProcessingJob: Job? = null
    
    // Callbacks
    private var onAudioReady: ((FloatArray) -> Unit)? = null
    private var onVadStateChanged: ((Boolean) -> Unit)? = null
    private var onProcessingError: ((String, Throwable?) -> Unit)? = null
    
    /**
     * Initialize audio processing system
     */
    @android.annotation.SuppressLint("MissingPermission")
    suspend fun initialize(): Boolean {
        return try {
            // Initialize AudioRecord
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AUDIO_FORMAT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                throw Exception("AudioRecord initialization failed")
            }
            
            Log.i(TAG, "✅ Audio processing initialized - Sample Rate: ${SAMPLE_RATE}Hz, Buffer Size: $bufferSize")
            true
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Audio permission not granted", e)
            onProcessingError?.invoke("Audio permission required", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio processing", e)
            onProcessingError?.invoke("Audio initialization failed: ${e.message}", e)
            false
        }
    }
    
    /**
     * Start audio processing
     */
    suspend fun startProcessing(): Boolean {
        if (isProcessing.get()) {
            Log.d(TAG, "Audio processing already running")
            return true
        }
        
        return try {
            audioRecord?.startRecording()
            isRecording.set(true)
            isProcessing.set(true)
            
            // Start audio processing coroutine
            audioProcessingJob = processingScope.launch {
                processAudioStream()
            }
            
            audioStateManager.startRecording()
            Log.i(TAG, "✅ Audio processing started")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio processing", e)
            onProcessingError?.invoke("Failed to start recording: ${e.message}", e)
            false
        }
    }
    
    /**
     * Stop audio processing
     */
    suspend fun stopProcessing() {
        try {
            isRecording.set(false)
            audioRecord?.stop()
            
            // Cancel processing job
            audioProcessingJob?.cancelAndJoin()
            
            // Process any remaining buffered audio
            processBufferedAudio()
            
            audioStateManager.stopRecording()
            isProcessing.set(false)
            
            Log.i(TAG, "✅ Audio processing stopped")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio processing", e)
        }
    }
    
    /**
     * Main audio stream processing loop
     */
    private suspend fun processAudioStream() {
        val buffer = ByteArray(bufferSize)
        
        while (isRecording.get() && isProcessing.get()) {
            try {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (bytesRead > 0) {
                    val startTime = System.currentTimeMillis()
                    
                    // Copy buffer data
                    val audioData = buffer.copyOf(bytesRead)
                    
                    // Apply noise reduction if enabled
                    val noiseReducedAudio = if (noiseReductionLevel > 0f) {
                        applyNoiseReduction(audioData)
                    } else {
                        audioData
                    }

                    // Apply VoiceIsolation preprocessing if available
                    val processedAudio = voiceIsolation?.let { isolation ->
                        if (isolation.isEnabled()) {
                            isolation.process(noiseReducedAudio)
                        } else {
                            noiseReducedAudio
                        }
                    } ?: noiseReducedAudio

                    // Perform Voice Activity Detection
                    val hasVoice = performVAD(processedAudio)
                    
                    // Add to buffer based on VAD result
                    if (hasVoice || processingMode == WhisperProcessingMode.REAL_TIME) {
                        addToAudioBuffer(processedAudio)
                    }
                    
                    // Record processing performance
                    val processingTime = System.currentTimeMillis() - startTime
                    performanceMonitor.recordSlowOperation("audio_processing", processingTime, 50L)
                }
                
                // Small delay to prevent excessive CPU usage
                delay(10)
                
            } catch (e: CancellationException) {
                Log.d(TAG, "Audio processing cancelled")
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error in audio processing loop", e)
                delay(100)
            }
        }
        
        Log.d(TAG, "Audio processing loop ended")
    }
    
    /**
     * Add audio data to buffer and trigger processing when ready
     */
    private suspend fun addToAudioBuffer(audioData: ByteArray) {
        audioBufferMutex.withLock {
            audioBuffer.add(audioData)
            
            // Determine when to process based on mode
            val shouldProcess = when (processingMode) {
                WhisperProcessingMode.REAL_TIME -> {
                    audioBuffer.size >= REAL_TIME_BUFFER_SIZE
                }
                WhisperProcessingMode.BATCH -> {
                    audioBuffer.size >= BATCH_BUFFER_SIZE
                }
                WhisperProcessingMode.HYBRID -> {
                    audioBuffer.size >= HYBRID_BUFFER_SIZE
                }
            }
            
            if (shouldProcess) {
                processBufferedAudio()
            }
        }
    }
    
    /**
     * Voice Activity Detection using energy-based algorithm
     */
    private fun performVAD(audioData: ByteArray): Boolean {
        // Calculate energy of the audio frame
        val energy = calculateAudioEnergy(audioData)
        
        // Update energy history
        energyHistory.add(energy)
        if (energyHistory.size > ENERGY_HISTORY_SIZE) {
            energyHistory.removeAt(0)
        }
        
        // Calculate dynamic threshold based on recent history
        val avgEnergy = if (energyHistory.isNotEmpty()) {
            energyHistory.average().toFloat()
        } else {
            0f
        }
        val threshold = avgEnergy + (vadSensitivity * avgEnergy)
        
        val currentTime = System.currentTimeMillis()
        val previousState = vadState
        
        when (vadState) {
            VADState.SILENCE -> {
                if (energy > threshold) {
                    vadState = VADState.SPEECH
                    speechStartTime = currentTime
                    silenceStartTime = 0L
                }
            }
            
            VADState.SPEECH -> {
                if (energy <= threshold) {
                    if (silenceStartTime == 0L) {
                        silenceStartTime = currentTime
                    } else if (currentTime - silenceStartTime > SILENCE_TIMEOUT) {
                        vadState = VADState.SILENCE
                        silenceStartTime = 0L
                        speechStartTime = 0L
                    }
                } else {
                    silenceStartTime = 0L
                }
            }
        }
        
        // Notify state change
        if (previousState != vadState) {
            val hasSpeech = vadState == VADState.SPEECH
            onVadStateChanged?.invoke(hasSpeech)
            Log.d(TAG, "VAD state changed: ${if (hasSpeech) "SPEECH" else "SILENCE"}")
        }
        
        return vadState == VADState.SPEECH
    }
    
    /**
     * Calculate audio energy for VAD
     */
    private fun calculateAudioEnergy(audioData: ByteArray): Float {
        if (audioData.isEmpty()) return 0f
        
        var sumSquares = 0.0
        val samples = audioData.size / 2 // 16-bit samples
        
        for (i in 0 until samples * 2 step 2) {
            if (i + 1 < audioData.size) {
                // Convert bytes to 16-bit sample
                val sample = (audioData[i].toInt() and 0xFF) or 
                           ((audioData[i + 1].toInt() and 0xFF) shl 8)
                val normalizedSample = sample.toShort().toDouble() / 32768.0
                sumSquares += normalizedSample * normalizedSample
            }
        }
        
        return if (samples > 0) {
            sqrt(sumSquares / samples).toFloat()
        } else {
            0f
        }
    }
    
    /**
     * Apply noise reduction to audio data
     */
    private fun applyNoiseReduction(audioData: ByteArray): ByteArray {
        if (noiseReductionLevel <= 0f) return audioData
        
        // Simple spectral subtraction-based noise reduction
        val processedData = audioData.copyOf()
        
        // Calculate current energy
        val energy = calculateAudioEnergy(audioData)
        val avgEnergy = if (energyHistory.isNotEmpty()) {
            energyHistory.average().toFloat()
        } else {
            energy
        }
        
        // Apply noise reduction if energy is below threshold
        if (energy < avgEnergy * 0.5f) {
            // Likely noise, reduce amplitude
            val reductionFactor = 1.0f - noiseReductionLevel
            
            for (i in 0 until processedData.size step 2) {
                if (i + 1 < processedData.size) {
                    // Convert to sample, apply reduction, convert back
                    val sample = (processedData[i].toInt() and 0xFF) or 
                               ((processedData[i + 1].toInt() and 0xFF) shl 8)
                    val reducedSample = (sample.toShort() * reductionFactor).toInt().toShort()
                    
                    processedData[i] = (reducedSample.toInt() and 0xFF).toByte()
                    processedData[i + 1] = ((reducedSample.toInt() shr 8) and 0xFF).toByte()
                }
            }
        }
        
        return processedData
    }
    
    /**
     * Process buffered audio and prepare for inference
     */
    private suspend fun processBufferedAudio() {
        audioBufferMutex.withLock {
            if (audioBuffer.isEmpty()) return
            
            try {
                // Combine all audio buffer chunks
                val combinedAudio = combineAudioBuffers(audioBuffer)
                audioBuffer.clear()
                
                // Convert to float array for Whisper
                val floatAudio = convertToFloatArray(combinedAudio)
                
                // Validate audio before sending
                if (validateFloatAudio(floatAudio)) {
                    // Send to callback for inference
                    onAudioReady?.invoke(floatAudio)
                    Log.d(TAG, "Audio ready for inference: ${floatAudio.size} samples")
                } else {
                    Log.w(TAG, "Audio validation failed, skipping inference")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing buffered audio", e)
                onProcessingError?.invoke("Audio processing failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Combine multiple audio buffers into one
     */
    private fun combineAudioBuffers(buffers: List<ByteArray>): ByteArray {
        val totalSize = buffers.sumOf { it.size }
        val combined = ByteArray(totalSize)
        var offset = 0
        
        for (buffer in buffers) {
            System.arraycopy(buffer, 0, combined, offset, buffer.size)
            offset += buffer.size
        }
        
        return combined
    }
    
    /**
     * Convert byte array to float array for Whisper processing
     */
    private fun convertToFloatArray(audioData: ByteArray): FloatArray {
        val samples = audioData.size / 2
        val floatArray = FloatArray(samples)
        
        val byteBuffer = ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN)
        
        for (i in 0 until samples) {
            val sample = byteBuffer.getShort(i * 2).toFloat() / 32768.0f
            floatArray[i] = sample.coerceIn(-1.0f, 1.0f) // Clamp to valid range
        }
        
        return floatArray
    }
    
    /**
     * Validate float audio array
     */
    private fun validateFloatAudio(floatAudio: FloatArray): Boolean {
        if (floatAudio.isEmpty()) return false
        
        // Check for NaN or infinite values
        var validSamples = 0
        var silentSamples = 0
        
        for (sample in floatAudio) {
            when {
                sample.isNaN() || sample.isInfinite() -> return false
                abs(sample) < 0.001f -> silentSamples++
                else -> validSamples++
            }
        }
        
        // Ensure we have some non-silent audio
        val silencePercentage = (silentSamples.toFloat() / floatAudio.size) * 100f
        if (silencePercentage > 95f) {
            Log.w(TAG, "Audio is ${silencePercentage}% silent")
            return false
        }
        
        return true
    }
    
    /**
     * Set processing mode
     */
    fun setProcessingMode(mode: WhisperProcessingMode) {
        processingMode = mode
        Log.d(TAG, "Processing mode set to: $mode")
    }
    
    /**
     * Set noise reduction level (0.0 to 1.0)
     */
    fun setNoiseReductionLevel(level: Float) {
        noiseReductionLevel = level.coerceIn(0f, 1f)
        Log.d(TAG, "Noise reduction level set to: $noiseReductionLevel")
    }
    
    /**
     * Set VAD sensitivity (0.0 to 1.0)
     */
    fun setVadSensitivity(sensitivity: Float) {
        vadSensitivity = sensitivity.coerceIn(0f, 1f)
        Log.d(TAG, "VAD sensitivity set to: $vadSensitivity")
    }
    
    /**
     * Get current audio buffer information
     */
    fun getBufferInfo(): AudioBufferInfo {
        val bufferDurationMs = (bufferSize / 2 * 1000) / SAMPLE_RATE
        return AudioBufferInfo(
            sampleRate = SAMPLE_RATE,
            channels = CHANNELS,
            encoding = AUDIO_FORMAT,
            bufferSize = bufferSize,
            durationMs = bufferDurationMs.toLong()
        )
    }
    
    /**
     * Get current VAD state
     */
    fun isVoiceDetected(): Boolean = vadState == VADState.SPEECH
    
    /**
     * Get processing statistics
     */
    fun getProcessingStats(): Map<String, Any> {
        return mapOf(
            "isProcessing" to isProcessing.get(),
            "isRecording" to isRecording.get(),
            "processingMode" to processingMode.name,
            "bufferSize" to bufferSize,
            "sampleRate" to SAMPLE_RATE,
            "noiseReductionLevel" to noiseReductionLevel,
            "vadSensitivity" to vadSensitivity,
            "vadState" to vadState.name,
            "energyHistorySize" to energyHistory.size,
            "currentBufferedChunks" to audioBuffer.size
        )
    }
    
    /**
     * Set audio processing callbacks
     */
    fun setCallbacks(
        onReady: ((FloatArray) -> Unit)? = null,
        onVadChanged: ((Boolean) -> Unit)? = null,
        onError: ((String, Throwable?) -> Unit)? = null
    ) {
        onAudioReady = onReady
        onVadStateChanged = onVadChanged
        onProcessingError = onError
    }
    
    /**
     * Destroy and release all resources
     */
    suspend fun destroy() {
        try {
            // Stop processing
            stopProcessing()
            
            // Cancel coroutines
            processingScope.cancel()
            
            // Release AudioRecord
            audioRecord?.release()
            audioRecord = null
            
            // Clear buffers
            audioBufferMutex.withLock {
                audioBuffer.clear()
            }
            energyHistory.clear()
            
            // Clear callbacks
            onAudioReady = null
            onVadStateChanged = null
            onProcessingError = null
            
            Log.i(TAG, "✅ WhisperProcessor destroyed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during WhisperProcessor destroy", e)
        }
    }
}