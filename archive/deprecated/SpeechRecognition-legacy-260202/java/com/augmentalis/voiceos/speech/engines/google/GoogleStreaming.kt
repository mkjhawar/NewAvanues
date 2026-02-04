/**
 * GoogleStreaming.kt - Google Cloud Speech streaming recognition management
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * Handles streaming audio recognition, audio recording, buffering, and real-time processing
 * for Google Cloud Speech Recognition
 */
package com.augmentalis.voiceos.speech.engines.google

import android.annotation.SuppressLint
import android.util.Log
// import com.augmentalis.speechrecognition.engines.GoogleCloudSpeechLite  // Commented out - class doesn't exist
import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import com.augmentalis.voiceisolation.VoiceIsolation

/**
 * Manages streaming audio recognition for Google Cloud Speech.
 * Handles audio recording, buffering, and real-time speech processing.
 */
class GoogleStreaming(
    private val performanceMonitor: PerformanceMonitor,
    private val voiceIsolation: VoiceIsolation? = null
) {
    
    companion object {
        private const val TAG = "GoogleStreaming"
        
        // Stream limits
        const val MAX_STREAM_DURATION_MS = 300000L  // 5 minutes max per stream
        const val STREAM_RESTART_DELAY_MS = 100L
        
        // Audio settings
        const val SAMPLE_RATE_HERTZ = 16000
        const val AUDIO_BUFFER_SIZE = 3200  // 100ms chunks at 16kHz
        const val AUDIO_CHUNK_THRESHOLD = 32000 // ~2 seconds worth of audio
        const val MIN_AUDIO_SIZE = 1600 // Minimum 0.1 seconds
        
        // Error codes
        const val ERROR_STREAM = 1004
        const val ERROR_AUDIO = 1005
    }
    
    // Recognition state
    private val isRecognizing = AtomicBoolean(false)
    private val isStreaming = AtomicBoolean(false)
    private var recognitionStartTime = 0L
    
    // Audio recording
    private var audioRecorder: AudioRecorder? = null
    private val audioBuffer = mutableListOf<ByteArray>()
    private val maxBufferSize = 10 // Maximum number of chunks to buffer
    
    // Coroutine management
    private val streamingScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + 
        CoroutineName("GoogleStreaming")
    )
    private var recognitionJob: Job? = null
    private var audioJob: Job? = null
    
    // Recognition callback
    private var onRecognitionResult: ((ByteArray, Any?) -> Unit)? = null  // Changed from GoogleCloudSpeechLite.RecognitionConfig to Any?
    private var onError: ((Int, String) -> Unit)? = null
    private var onTimeout: (() -> Unit)? = null
    
    /**
     * Initialize streaming components
     */
    fun initialize(): Result<Unit> {
        return try {
            Log.i(TAG, "Initializing streaming components...")
            
            // Initialize audio recorder
            audioRecorder = AudioRecorder(
                sampleRate = SAMPLE_RATE_HERTZ,
                bufferSize = AUDIO_BUFFER_SIZE
            )
            
            Log.i(TAG, "Streaming components initialized successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize streaming components", e)
            Result.failure(e)
        }
    }
    
    /**
     * Start streaming recognition
     */
    fun startStreaming(
        recognitionConfig: Any?,  // Changed from GoogleCloudSpeechLite.RecognitionConfig to Any?
        onResult: (ByteArray, Any?) -> Unit,  // Changed from GoogleCloudSpeechLite.RecognitionConfig to Any?
        onError: (Int, String) -> Unit,
        onTimeout: () -> Unit = {}  // Added onTimeout parameter with default empty lambda
    ): Result<Unit> {
        return try {
            if (isStreaming.get()) {
                Log.w(TAG, "Already streaming")
                return Result.success(Unit)
            }
            
            Log.i(TAG, "Starting streaming recognition...")
            
            // Store callbacks
            this.onRecognitionResult = onResult
            this.onError = onError
            this.onTimeout = onTimeout
            
            // Start recognition
            startRecognition(recognitionConfig)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start streaming", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stop streaming recognition
     */
    fun stopStreaming(): Result<Unit> {
        return try {
            Log.i(TAG, "Stopping streaming recognition...")
            
            stopRecognition()
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop streaming", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if currently streaming
     */
    fun isStreaming(): Boolean = isStreaming.get()
    
    /**
     * Check if currently recognizing
     */
    fun isRecognizing(): Boolean = isRecognizing.get()
    
    /**
     * Get streaming duration
     */
    fun getStreamingDuration(): Long {
        return if (recognitionStartTime > 0) {
            System.currentTimeMillis() - recognitionStartTime
        } else 0
    }
    
    /**
     * Restart streaming (for timeout/error recovery)
     */
    fun restartStreaming(recognitionConfig: Any?) {  // Changed from GoogleCloudSpeechLite.RecognitionConfig to Any?
        if (isRecognizing.get()) {
            Log.i(TAG, "Restarting streaming due to timeout or error")
            
            streamingScope.launch {
                // Stop current recognition
                stopRecognition()
                
                // Small delay
                delay(STREAM_RESTART_DELAY_MS)
                
                // Restart if still needed
                if (onRecognitionResult != null) {
                    startRecognition(recognitionConfig)
                }
            }
        }
    }
    
    /**
     * Start recognition (internal)
     */
    private fun startRecognition(recognitionConfig: Any?) {  // Changed from GoogleCloudSpeechLite.RecognitionConfig to Any?
        try {
            // Update state
            isRecognizing.set(true)
            isStreaming.set(true)
            recognitionStartTime = System.currentTimeMillis()
            performanceMonitor.startSession()
            
            // Start audio recording and recognition
            startAudioRecordingAndRecognition(recognitionConfig)
            
            Log.i(TAG, "Started recognition successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recognition", e)
            handleError(ERROR_STREAM, "Recognition start failed: ${e.message}")
        }
    }
    
    /**
     * Stop recognition (internal)
     */
    private fun stopRecognition() {
        try {
            // Cancel recognition jobs
            isRecognizing.set(false)
            isStreaming.set(false)
            recognitionJob?.cancel()
            audioJob?.cancel()
            
            // Stop audio recording
            audioRecorder?.stop()
            
            // Clear audio buffer
            synchronized(audioBuffer) {
                audioBuffer.clear()
            }
            
            // End performance session
            performanceMonitor.recordRecognition(
                System.currentTimeMillis(),
                true,
                "Streaming stopped"
            )
            
            Log.i(TAG, "Stopped recognition successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognition", e)
        }
    }
    
    /**
     * Start audio recording and recognition processing
     */
    private fun startAudioRecordingAndRecognition(recognitionConfig: Any?) {  // Changed from GoogleCloudSpeechLite.RecognitionConfig to Any?
        // Start audio recording job
        audioJob = streamingScope.launch {
            try {
                audioRecorder?.start()
                Log.d(TAG, "Audio recording started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start audio recording", e)
                handleError(ERROR_AUDIO, "Audio recording failed: ${e.message}")
            }
        }
        
        // Start recognition processing job
        recognitionJob = streamingScope.launch {
            try {
                val audioBuffer = mutableListOf<ByteArray>()
                
                // Collect audio data and process in chunks
                audioRecorder?.audioFlow?.collect { rawAudioData ->
                    if (isRecognizing.get()) {
                        // Apply VoiceIsolation preprocessing if available
                        val audioData = voiceIsolation?.let { isolation ->
                            if (isolation.isEnabled()) {
                                isolation.process(rawAudioData)
                            } else {
                                rawAudioData
                            }
                        } ?: rawAudioData

                        synchronized(audioBuffer) {
                            audioBuffer.add(audioData)
                            
                            // Limit buffer size to prevent memory issues
                            while (audioBuffer.size > maxBufferSize) {
                                audioBuffer.removeAt(0)
                            }
                        }
                        
                        // Process recognition when we have enough data
                        val totalAudioSize = audioBuffer.sumOf { it.size }
                        if (totalAudioSize >= AUDIO_CHUNK_THRESHOLD) {
                            processAudioChunk(audioBuffer.toList(), recognitionConfig)
                            audioBuffer.clear()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in audio recognition processing", e)
                handleError(ERROR_STREAM, "Audio processing failed: ${e.message}")
            }
        }
    }
    
    /**
     * Process audio chunk for recognition
     */
    private suspend fun processAudioChunk(
        audioChunks: List<ByteArray>,
        recognitionConfig: Any?  // Changed from GoogleCloudSpeechLite.RecognitionConfig to Any?
    ) {
        try {
            // Combine audio chunks
            val combinedAudio = audioChunks.fold(ByteArray(0)) { acc, chunk -> acc + chunk }
            
            // Skip if too small
            if (combinedAudio.size < MIN_AUDIO_SIZE) {
                Log.v(TAG, "Skipping small audio chunk: ${combinedAudio.size} bytes")
                return
            }
            
            Log.v(TAG, "Processing audio chunk: ${combinedAudio.size} bytes")
            
            // Call recognition callback
            onRecognitionResult?.invoke(combinedAudio, recognitionConfig)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio chunk", e)
            handleError(ERROR_STREAM, "Audio chunk processing failed: ${e.message}")
        }
    }
    
    /**
     * Get current audio buffer size
     */
    fun getAudioBufferSize(): Int {
        return synchronized(audioBuffer) {
            audioBuffer.size
        }
    }
    
    /**
     * Get audio buffer statistics
     */
    fun getAudioStats(): Map<String, Any> {
        return synchronized(audioBuffer) {
            val totalBytes = audioBuffer.sumOf { it.size }
            mapOf(
                "bufferChunks" to audioBuffer.size,
                "totalBytes" to totalBytes,
                "averageChunkSize" to if (audioBuffer.isNotEmpty()) totalBytes / audioBuffer.size else 0,
                "maxBufferSize" to maxBufferSize,
                "sampleRate" to SAMPLE_RATE_HERTZ,
                "chunkSize" to AUDIO_BUFFER_SIZE
            )
        }
    }
    
    /**
     * Clear audio buffer
     */
    fun clearAudioBuffer() {
        synchronized(audioBuffer) {
            audioBuffer.clear()
        }
        Log.d(TAG, "Audio buffer cleared")
    }
    
    /**
     * Handle streaming errors
     */
    private fun handleError(errorCode: Int, message: String) {
        Log.e(TAG, "Streaming error: $message (code: $errorCode)")
        
        // Stop recognition on error
        stopRecognition()
        
        // Call error callback
        onError?.invoke(errorCode, message)
    }
    
    /**
     * Get streaming statistics
     */
    fun getStreamingStats(): Map<String, Any> {
        return mapOf(
            "isStreaming" to isStreaming.get(),
            "isRecognizing" to isRecognizing.get(),
            "streamingDuration" to getStreamingDuration(),
            "recognitionStartTime" to recognitionStartTime,
            "audioRecorderActive" to (audioRecorder?.isRecording() == true),
            "hasRecognitionCallback" to (onRecognitionResult != null),
            "hasErrorCallback" to (onError != null),
            "recognitionJobActive" to (recognitionJob?.isActive == true),
            "audioJobActive" to (audioJob?.isActive == true),
            "audioStats" to getAudioStats()
        )
    }
    
    /**
     * Shutdown streaming components
     */
    fun shutdown() {
        Log.i(TAG, "Shutting down streaming components...")
        
        // Stop streaming
        stopRecognition()
        
        // Cancel all jobs
        streamingScope.cancel()
        
        // Release audio recorder
        audioRecorder?.release()
        audioRecorder = null
        
        // Clear callbacks
        onRecognitionResult = null
        onError = null
        onTimeout = null
        
        // Clear audio buffer
        synchronized(audioBuffer) {
            audioBuffer.clear()
        }
        
        Log.i(TAG, "Streaming components shutdown complete")
    }
}

/**
 * Audio recorder for capturing microphone input.
 * Provides audio stream as Flow for coroutine-based processing.
 */
class AudioRecorder(
    private val sampleRate: Int,
    private val bufferSize: Int
) {
    @SuppressLint("MissingPermission")
    private val audioRecord = android.media.AudioRecord(
        android.media.MediaRecorder.AudioSource.MIC,
        sampleRate,
        android.media.AudioFormat.CHANNEL_IN_MONO,
        android.media.AudioFormat.ENCODING_PCM_16BIT,
        bufferSize * 2
    )

    private val _audioFlow = MutableSharedFlow<ByteArray>()
    val audioFlow: SharedFlow<ByteArray> = _audioFlow.asSharedFlow()

    // Managed coroutine scope instead of GlobalScope
    private val recorderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordingJob: Job? = null
    private val isRecording = AtomicBoolean(false)

    fun start() {
        if (audioRecord.state != android.media.AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord not initialized")
        }

        if (isRecording.get()) {
            return // Already recording
        }

        audioRecord.startRecording()
        isRecording.set(true)

        recordingJob = recorderScope.launch {
            val buffer = ByteArray(bufferSize)

            while (isActive && isRecording.get()) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    _audioFlow.emit(buffer.copyOf(read))
                }
            }
        }
    }

    fun stop() {
        isRecording.set(false)
        recordingJob?.cancel()
        audioRecord.stop()
    }

    fun release() {
        stop()
        recorderScope.cancel() // Cancel scope on release
        audioRecord.release()
    }

    fun isRecording(): Boolean = isRecording.get()
}