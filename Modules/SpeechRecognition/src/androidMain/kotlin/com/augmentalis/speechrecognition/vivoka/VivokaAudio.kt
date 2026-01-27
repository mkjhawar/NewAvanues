/**
 * VivokaAudio.kt - Audio pipeline management for Vivoka VSDK engine
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * Updated: 2026-01-27 - Migrated to KMP androidMain
 *
 * Extracted from monolithic VivokaEngine.kt as part of SOLID refactoring
 * Handles audio recording, pipeline management, and audio-related error recovery
 */
package com.augmentalis.speechrecognition.vivoka

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.augmentalis.speechrecognition.SpeechErrorCodes
import com.vivoka.vsdk.asr.csdk.recognizer.Recognizer
import com.vivoka.vsdk.audio.Pipeline
import com.vivoka.vsdk.audio.producers.AudioRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages audio pipeline for the Vivoka engine including recording, processing, and recovery
 */
class VivokaAudio(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {

    companion object {
        private const val TAG = "VivokaAudio"
        private const val SILENCE_CHECK_INTERVAL = 100L
        private const val PIPELINE_RECOVERY_DELAY = 1000L
        private const val AUDIO_ERROR_TIMEOUT = 5000L
    }

    // Audio components
    private var pipeline: Pipeline? = null
    private var audioRecorder: AudioRecorder? = null
    private var recognizer: Recognizer? = null

    // Silence detection for dictation
    private val silenceCheckHandler = Handler(Looper.getMainLooper())
    private var silenceCheckRunnable: Runnable? = null
    private var silenceDetectionCallback: (() -> Unit)? = null

    // Audio state
    @Volatile
    private var isPipelineRunning = false
    @Volatile
    private var isRecording = false
    @Volatile
    private var isRecovering = false

    // Error handling
    private var audioErrorListener: ((String, Int) -> Unit)? = null
    private var lastPipelineError: String? = null
    private var pipelineRecoveryCount = 0

    /**
     * Initialize audio pipeline with recognizer
     */
    @SuppressLint("MissingPermission")
    suspend fun initializePipeline(recognizer: Recognizer): Boolean {
        return try {
            Log.d(TAG, "Initializing audio pipeline")

            this.recognizer = recognizer

            // Clean up any existing pipeline first
            //stopPipeline()

            // Create audio recorder
            audioRecorder = AudioRecorder()
            if (audioRecorder == null) {
                throw Exception("Failed to create AudioRecorder")
            }

            // Create pipeline
            pipeline = Pipeline().apply {
                setProducer(audioRecorder)
                pushBackConsumer(recognizer)
            }

            if (pipeline == null) {
                throw Exception("Failed to create Pipeline")
            }

            Log.d(TAG, "Audio pipeline initialized successfully")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio pipeline", e)
            audioErrorListener?.invoke("Audio pipeline initialization failed: ${e.message}", SpeechErrorCodes.AUDIO_PIPELINE_ERROR)
            false
        }
    }

    /**
     * Start the audio pipeline
     */
    @SuppressLint("MissingPermission")
    suspend fun startPipeline(): Boolean {
        return try {
            Log.d(TAG, "Starting audio pipeline")

            if (pipeline == null) {
                Log.e(TAG, "Cannot start pipeline - not initialized")
                return false
            }

            // Start the pipeline
            pipeline?.start()
            isPipelineRunning = true

            Log.d(TAG, "Audio pipeline started successfully")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio pipeline", e)
            audioErrorListener?.invoke("Audio pipeline start failed: ${e.message}", SpeechErrorCodes.AUDIO_PIPELINE_ERROR)
            false
        }
    }

    /**
     * Stop the audio pipeline
     */
    fun stopPipeline() {
        try {
            Log.d(TAG, "Stopping audio pipeline")

            // Stop recording first
            stopRecording()

            // Stop pipeline
            pipeline?.stop()
            isPipelineRunning = false

            // Clean up components
            audioRecorder = null
            pipeline = null

            Log.d(TAG, "Audio pipeline stopped")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio pipeline", e)
        }
    }

    /**
     * Start audio recording
     */
    fun startRecording(): Boolean {
        return try {
            if (!isPipelineRunning) {
                Log.w(TAG, "Cannot start recording - pipeline not running")
                return false
            }

            Log.d(TAG, "Starting audio recording")

            audioRecorder?.start()
            isRecording = true

            Log.d(TAG, "Audio recording started")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            audioErrorListener?.invoke("Audio recording start failed: ${e.message}", SpeechErrorCodes.AUDIO_PIPELINE_ERROR)
            false
        }
    }

    /**
     * Stop audio recording
     */
    fun stopRecording() {
        try {
            if (!isRecording) return

            Log.d(TAG, "Stopping audio recording")

            audioRecorder?.stop()
            isRecording = false

            // Stop silence detection
            stopSilenceDetection()

            Log.d(TAG, "Audio recording stopped")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recording", e)
        }
    }

    /**
     * Start silence detection for dictation mode
     */
    fun startSilenceDetection(timeoutMs: Long, onTimeoutCallback: () -> Unit) {
        Log.d(TAG, "Starting silence detection with timeout: ${timeoutMs}ms")

        // Stop any existing silence detection
        stopSilenceDetection()

        silenceDetectionCallback = onTimeoutCallback

        // Create new runnable for silence checking
        val runnable = object : Runnable {
            override fun run() {
                silenceDetectionCallback?.invoke()
                silenceCheckHandler.postDelayed(this, SILENCE_CHECK_INTERVAL)
            }
        }
        silenceCheckRunnable = runnable

        // Start silence detection
        silenceCheckHandler.post(runnable)
    }

    /**
     * Stop silence detection
     */
    fun stopSilenceDetection() {
        silenceCheckRunnable?.let { runnable ->
            silenceCheckHandler.removeCallbacks(runnable)
            silenceCheckRunnable = null
            Log.d(TAG, "Silence detection stopped")
        }
        silenceDetectionCallback = null
    }

    /**
     * Recover from audio pipeline errors
     */
    suspend fun recoverPipeline(): Boolean {
        if (isRecovering) {
            Log.d(TAG, "Pipeline recovery already in progress")
            return false
        }

        return try {
            isRecovering = true
            pipelineRecoveryCount++

            Log.i(TAG, "Attempting audio pipeline recovery (attempt $pipelineRecoveryCount)")

            // Stop current pipeline
            stopPipeline()

            // Wait for cleanup
            delay(PIPELINE_RECOVERY_DELAY)

            // Re-initialize if we have a recognizer
            val currentRecognizer = recognizer
            if (currentRecognizer != null) {
                val initSuccess = initializePipeline(currentRecognizer)
                if (initSuccess) {
                    val startSuccess = startPipeline()
                    if (startSuccess) {
                        Log.i(TAG, "Audio pipeline recovery successful")
                        pipelineRecoveryCount = 0
                        isRecovering = false
                        return true
                    }
                }
            }

            Log.e(TAG, "Audio pipeline recovery failed")
            isRecovering = false
            false

        } catch (e: Exception) {
            Log.e(TAG, "Audio pipeline recovery failed with exception", e)
            isRecovering = false
            false
        }
    }

    /**
     * Check if audio pipeline is healthy
     */
    fun isPipelineHealthy(): Boolean {
        return isPipelineRunning &&
               pipeline != null &&
               audioRecorder != null &&
               !isRecovering
    }

    /**
     * Get audio pipeline status
     */
    fun getPipelineStatus(): Map<String, Any> {
        return mapOf(
            "isPipelineRunning" to isPipelineRunning,
            "isRecording" to isRecording,
            "isRecovering" to isRecovering,
            "hasPipeline" to (pipeline != null),
            "hasAudioRecorder" to (audioRecorder != null),
            "hasRecognizer" to (recognizer != null),
            "recoveryCount" to pipelineRecoveryCount,
            "lastError" to (lastPipelineError ?: "none"),
            "isHealthy" to isPipelineHealthy()
        )
    }

    /**
     * Handle audio-related errors
     */
    fun handleAudioError(errorCode: String?, message: String?) {
        lastPipelineError = "$errorCode: $message"
        Log.e(TAG, "Audio error - Code: $errorCode, Message: $message")

        // Determine error severity and recovery strategy
        val shouldRecover = when {
            errorCode?.contains("audio", ignoreCase = true) == true -> true
            errorCode?.contains("pipeline", ignoreCase = true) == true -> true
            errorCode?.contains("recorder", ignoreCase = true) == true -> true
            pipelineRecoveryCount < 3 -> true
            else -> false
        }

        if (shouldRecover && !isRecovering) {
            Log.w(TAG, "Attempting automatic audio pipeline recovery")
            coroutineScope.launch {
                val recoverySuccess = recoverPipeline()
                if (!recoverySuccess) {
                    audioErrorListener?.invoke(
                        "Audio pipeline recovery failed: $message",
                        SpeechErrorCodes.AUDIO_PIPELINE_ERROR
                    )
                }
            }
        } else {
            audioErrorListener?.invoke(
                "Audio error: $message",
                SpeechErrorCodes.AUDIO_PIPELINE_ERROR
            )
        }
    }

    /**
     * Perform audio system diagnostics
     */
    suspend fun performDiagnostics(): Map<String, Any> {
        val diagnostics = mutableMapOf<String, Any>()

        try {
            // Check pipeline state
            diagnostics["pipelineState"] = when {
                pipeline == null -> "not_initialized"
                !isPipelineRunning -> "initialized_but_stopped"
                isPipelineRunning -> "running"
                else -> "unknown"
            }

            // Check audio recorder state
            diagnostics["audioRecorderState"] = when {
                audioRecorder == null -> "not_initialized"
                !isRecording -> "initialized_but_stopped"
                isRecording -> "recording"
                else -> "unknown"
            }

            // Check recognizer state
            diagnostics["recognizerState"] = when {
                recognizer == null -> "not_assigned"
                else -> "assigned"
            }

            // Recovery statistics
            diagnostics["recoveryAttempts"] = pipelineRecoveryCount
            diagnostics["isCurrentlyRecovering"] = isRecovering

            // Timing information
            diagnostics["lastErrorTime"] = System.currentTimeMillis()

            // Overall health assessment
            val isHealthy = isPipelineHealthy()
            diagnostics["overallHealth"] = if (isHealthy) "healthy" else "unhealthy"

            // Recommendations
            val recommendations = mutableListOf<String>()
            if (!isPipelineRunning && pipeline != null) {
                recommendations.add("Start pipeline")
            }
            if (!isRecording && isPipelineRunning) {
                recommendations.add("Start recording")
            }
            if (pipelineRecoveryCount > 2) {
                recommendations.add("Consider full engine restart")
            }
            if (pipeline == null) {
                recommendations.add("Re-initialize pipeline")
            }

            diagnostics["recommendations"] = recommendations

            Log.d(TAG, "Audio diagnostics completed: ${diagnostics["overallHealth"]}")

        } catch (e: Exception) {
            Log.e(TAG, "Audio diagnostics failed", e)
            diagnostics["error"] = "Diagnostics failed: ${e.message}"
            diagnostics["overallHealth"] = "error"
        }

        return diagnostics
    }

    /**
     * Reset audio system to initial state
     */
    fun reset() {
        Log.d(TAG, "Resetting audio system")

        try {
            // Stop all audio operations
            stopSilenceDetection()
            stopPipeline()

            // Reset state
            isPipelineRunning = false
            isRecording = false
            isRecovering = false
            pipelineRecoveryCount = 0
            lastPipelineError = null

            // Clear references
            recognizer = null
            silenceDetectionCallback = null

            Log.d(TAG, "Audio system reset completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error during audio system reset", e)
        }
    }

    /**
     * Set error listener for audio-related errors
     */
    fun setErrorListener(listener: (String, Int) -> Unit) {
        this.audioErrorListener = listener
    }

    /**
     * Get current audio recorder instance
     */
    fun getAudioRecorder(): AudioRecorder? = audioRecorder

    /**
     * Get current pipeline instance
     */
    fun getPipeline(): Pipeline? = pipeline

    /**
     * Check if audio recording is active
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Check if pipeline is running
     */
    fun isPipelineRunning(): Boolean = isPipelineRunning

    /**
     * Check if system is recovering
     */
    fun isRecovering(): Boolean = isRecovering

    /**
     * Get recovery attempt count
     */
    fun getRecoveryCount(): Int = pipelineRecoveryCount

    /**
     * Force pipeline restart (for testing or manual recovery)
     */
    suspend fun forcePipelineRestart(): Boolean {
        Log.w(TAG, "Forcing pipeline restart")

        return try {
            stopPipeline()
            delay(PIPELINE_RECOVERY_DELAY)

            val currentRecognizer = recognizer
            if (currentRecognizer != null) {
                val success = initializePipeline(currentRecognizer) && startPipeline()
                if (success) {
                    Log.i(TAG, "Forced pipeline restart successful")
                } else {
                    Log.e(TAG, "Forced pipeline restart failed")
                }
                success
            } else {
                Log.e(TAG, "Cannot restart pipeline - no recognizer available")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Forced pipeline restart failed with exception", e)
            false
        }
    }
}
