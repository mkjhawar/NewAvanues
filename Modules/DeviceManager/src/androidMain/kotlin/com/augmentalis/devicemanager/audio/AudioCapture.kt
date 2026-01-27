// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.devicemanager.audio

import android.content.Context
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

/**
 * Audio capture component following Single Responsibility Principle
 * Responsible only for audio capture and streaming via Flow
 */
class AudioCapture(
    private val context: Context,
    private val config: AudioConfig = AudioConfig()
) {
    
    companion object {
        private const val TAG = "AudioCapture"
    }
    
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val _audioFlow = MutableSharedFlow<ByteArray>()
    private val audioFlowInstance: Flow<ByteArray> = _audioFlow.asSharedFlow()

    // Managed coroutine scope for recording (avoids orphaned coroutines)
    private val recordingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var isRecording = false
    
    /**
     * Start audio recording
     * Returns true if recording started successfully
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        if (isRecording) {
            Log.w(TAG, "Recording already in progress")
            return false
        }
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                config.sampleRate,
                config.channelConfig,
                config.audioFormat,
                config.getBufferSize()
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize AudioRecord")
                release()
                return false
            }
            
            audioRecord?.startRecording()
            isRecording = true
            
            // Start recording in managed coroutine scope
            recordingJob = recordingScope.launch {
                readAudioData()
            }
            
            Log.d(TAG, "Audio recording started successfully")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            release()
            return false
        }
    }
    
    /**
     * Stop audio recording
     */
    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        
        audioRecord?.apply {
            try {
                stop()
                Log.d(TAG, "Audio recording stopped")
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping recording", e)
            }
        }
    }
    
    /**
     * Get audio stream as Flow
     */
    fun getAudioFlow(): Flow<ByteArray> = audioFlowInstance
    
    /**
     * Release all resources
     */
    fun release() {
        stopRecording()
        recordingScope.cancel() // Cancel the scope to cleanup any pending work
        audioRecord?.release()
        audioRecord = null
        Log.d(TAG, "AudioCapture resources released")
    }
    
    /**
     * Read audio data from AudioRecord and emit to Flow
     */
    private suspend fun readAudioData() {
        val buffer = ByteArray(config.getBufferSize())
        
        while (isRecording) {
            try {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (bytesRead > 0) {
                    // Emit copy of buffer to prevent data corruption
                    _audioFlow.emit(buffer.copyOf(bytesRead))
                } else if (bytesRead < 0) {
                    Log.w(TAG, "AudioRecord read error: $bytesRead")
                    break
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading audio data", e)
                break
            }
        }
    }
}