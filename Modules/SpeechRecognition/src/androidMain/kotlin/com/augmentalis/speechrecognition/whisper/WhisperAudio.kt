/**
 * WhisperAudio.kt - Audio capture pipeline for Whisper engine
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * Captures 16kHz mono 16-bit PCM audio from the microphone,
 * converts to Float32 (whisper.cpp native format), and provides
 * a circular buffer for continuous capture with chunk extraction.
 */
package com.augmentalis.speechrecognition.whisper

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Audio capture pipeline for Whisper speech recognition.
 *
 * Captures PCM audio at 16kHz mono (whisper.cpp requirement),
 * converts Int16 → Float32, and manages a growable buffer
 * for batch transcription.
 */
class WhisperAudio {

    companion object {
        private const val TAG = "WhisperAudio"

        /** Whisper requires 16kHz sample rate */
        const val SAMPLE_RATE = 16_000

        /** Mono channel */
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO

        /** 16-bit PCM encoding */
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        /** Read buffer size in samples (100ms chunks) */
        private const val READ_BUFFER_SAMPLES = SAMPLE_RATE / 10 // 1600 samples = 100ms

        /** Maximum buffer duration in seconds (prevents OOM) */
        private const val MAX_BUFFER_SECONDS = 60
    }

    // Audio recording
    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private var recordingThread: Thread? = null

    // Audio buffer — growable, stores Float32 samples ready for whisper
    private val audioBuffer = ArrayList<Float>(SAMPLE_RATE * 5) // pre-alloc ~5 seconds
    private val bufferLock = Any()

    // Audio level monitoring
    @Volatile
    var currentRmsLevel: Float = 0f
        private set

    // Callbacks
    var onAudioLevel: ((Float) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    /**
     * Initialize the AudioRecord instance.
     * Must be called before [start].
     */
    @SuppressLint("MissingPermission")
    fun initialize(): Boolean {
        return try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid min buffer size: $minBufferSize")
                onError?.invoke("Audio hardware does not support 16kHz recording")
                return false
            }

            // Use 2x min buffer for safety
            val bufferSize = maxOf(minBufferSize * 2, READ_BUFFER_SAMPLES * 2 * 2) // *2 for 16-bit

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                onError?.invoke("AudioRecord initialization failed")
                return false
            }

            Log.i(TAG, "Audio initialized: ${SAMPLE_RATE}Hz, buffer=${bufferSize}B")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Audio initialization error", e)
            onError?.invoke("Audio init error: ${e.message}")
            false
        }
    }

    /**
     * Start capturing audio into the internal buffer.
     */
    fun start(): Boolean {
        if (isRecording.get()) {
            Log.w(TAG, "Already recording")
            return true
        }

        val record = audioRecord ?: run {
            Log.e(TAG, "AudioRecord not initialized")
            return false
        }

        return try {
            record.startRecording()
            isRecording.set(true)

            recordingThread = Thread({
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
                captureLoop(record)
            }, "WhisperAudioCapture").apply {
                isDaemon = true
                start()
            }

            Log.i(TAG, "Audio capture started")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            isRecording.set(false)
            onError?.invoke("Failed to start recording: ${e.message}")
            false
        }
    }

    /**
     * Stop capturing audio.
     */
    fun stop() {
        if (!isRecording.getAndSet(false)) return

        try {
            audioRecord?.stop()
            recordingThread?.join(1000)
            recordingThread = null
            Log.i(TAG, "Audio capture stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
    }

    /**
     * Release all audio resources.
     */
    fun release() {
        stop()
        try {
            audioRecord?.release()
            audioRecord = null
            clearBuffer()
            Log.d(TAG, "Audio resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio", e)
        }
    }

    /**
     * Get the current buffer contents as a Float32 array for whisper.cpp.
     * Does NOT clear the buffer.
     */
    fun getBufferSnapshot(): FloatArray {
        synchronized(bufferLock) {
            return audioBuffer.toFloatArray()
        }
    }

    /**
     * Get and clear the current buffer contents.
     * Returns the Float32 audio data and resets the buffer.
     */
    fun drainBuffer(): FloatArray {
        synchronized(bufferLock) {
            val data = audioBuffer.toFloatArray()
            audioBuffer.clear()
            return data
        }
    }

    /**
     * Get the current buffer duration in milliseconds.
     */
    fun getBufferDurationMs(): Long {
        synchronized(bufferLock) {
            return (audioBuffer.size * 1000L) / SAMPLE_RATE
        }
    }

    /**
     * Get the current buffer size in samples.
     */
    fun getBufferSampleCount(): Int {
        synchronized(bufferLock) {
            return audioBuffer.size
        }
    }

    /**
     * Clear the audio buffer without stopping recording.
     */
    fun clearBuffer() {
        synchronized(bufferLock) {
            audioBuffer.clear()
        }
    }

    /**
     * Check if currently recording.
     */
    fun isRecording(): Boolean = isRecording.get()

    // --- Private ---

    /**
     * Main audio capture loop running on the recording thread.
     */
    private fun captureLoop(record: AudioRecord) {
        val readBuffer = ShortArray(READ_BUFFER_SAMPLES)
        val maxBufferSamples = MAX_BUFFER_SECONDS * SAMPLE_RATE

        while (isRecording.get()) {
            val samplesRead = record.read(readBuffer, 0, READ_BUFFER_SAMPLES)

            if (samplesRead > 0) {
                // Calculate RMS audio level
                var sumSquares = 0.0
                for (i in 0 until samplesRead) {
                    val sample = readBuffer[i].toFloat()
                    sumSquares += sample * sample
                }
                currentRmsLevel = sqrt(sumSquares / samplesRead).toFloat() / Short.MAX_VALUE
                onAudioLevel?.invoke(currentRmsLevel)

                // Convert Int16 → Float32 and append to buffer
                synchronized(bufferLock) {
                    // Prevent unbounded growth
                    if (audioBuffer.size + samplesRead > maxBufferSamples) {
                        // Drop oldest samples (shift left)
                        val excess = audioBuffer.size + samplesRead - maxBufferSamples
                        if (excess > 0 && excess < audioBuffer.size) {
                            audioBuffer.subList(0, excess).clear()
                        }
                    }

                    for (i in 0 until samplesRead) {
                        audioBuffer.add(readBuffer[i].toFloat() / Short.MAX_VALUE)
                    }
                }
            } else if (samplesRead < 0) {
                Log.e(TAG, "AudioRecord.read error: $samplesRead")
                if (samplesRead == AudioRecord.ERROR_DEAD_OBJECT) {
                    isRecording.set(false)
                    onError?.invoke("Audio device disconnected")
                    break
                }
            }
        }
    }
}
