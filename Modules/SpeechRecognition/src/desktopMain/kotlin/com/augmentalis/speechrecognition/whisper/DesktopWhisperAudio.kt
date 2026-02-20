/**
 * DesktopWhisperAudio.kt - Desktop audio capture for Whisper using javax.sound
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * Captures microphone audio on Desktop JVM using javax.sound.sampled API.
 * Outputs 16kHz mono Float32 PCM data compatible with whisper.cpp.
 *
 * Audio pipeline: Microphone → TargetDataLine (16kHz/16bit/mono) →
 * byte[] buffer → Int16 PCM → Float32 normalization → consumer
 */
package com.augmentalis.speechrecognition.whisper

import com.augmentalis.speechrecognition.logDebug
import com.augmentalis.speechrecognition.logError
import com.augmentalis.speechrecognition.logInfo
import com.augmentalis.speechrecognition.logWarn
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * Desktop audio capture using javax.sound.sampled.
 *
 * Captures 16kHz mono 16-bit PCM and converts to Float32 for whisper.cpp.
 * Thread-safe: capture runs on its own thread, buffer access synchronized.
 */
class DesktopWhisperAudio {

    companion object {
        private const val TAG = "DesktopWhisperAudio"

        /** Whisper expects 16kHz mono audio */
        const val SAMPLE_RATE = 16000

        /** 16-bit PCM = 2 bytes per sample */
        private const val BYTES_PER_SAMPLE = 2

        /** Read buffer size: 100ms of audio = 1600 samples = 3200 bytes */
        private const val READ_BUFFER_SIZE = SAMPLE_RATE / 10 * BYTES_PER_SAMPLE

        /** Maximum buffer duration: 60 seconds */
        private const val MAX_BUFFER_SAMPLES = SAMPLE_RATE * 60
    }

    // Audio format: 16kHz, 16-bit signed PCM, mono, little-endian
    private val audioFormat = AudioFormat(
        SAMPLE_RATE.toFloat(),   // sample rate
        16,                       // sample size in bits
        1,                        // channels (mono)
        true,                     // signed
        false                     // little-endian
    )

    // Audio line
    private var targetLine: TargetDataLine? = null

    // State
    private val isRecording = AtomicBoolean(false)
    private var captureThread: Thread? = null

    // Audio buffer (synchronized access)
    private val bufferLock = Object()
    private val audioBuffer = ArrayList<Float>(SAMPLE_RATE * 5) // pre-alloc 5s

    // Error callback
    var onError: ((String) -> Unit)? = null

    // Audio level (RMS of last read, for UI meters)
    @Volatile
    var currentLevel: Float = 0f
        private set

    /**
     * Initialize the audio capture system.
     * @return true if a compatible microphone is available
     */
    fun initialize(): Boolean {
        return try {
            val info = DataLine.Info(TargetDataLine::class.java, audioFormat)

            if (!AudioSystem.isLineSupported(info)) {
                logError(TAG, "Audio line not supported: $audioFormat")
                return false
            }

            targetLine = AudioSystem.getLine(info) as TargetDataLine
            logInfo(TAG, "Audio initialized: ${audioFormat.sampleRate}Hz, " +
                    "${audioFormat.sampleSizeInBits}bit, ${audioFormat.channels}ch")
            true
        } catch (e: Exception) {
            logError(TAG, "Failed to initialize audio", e)
            false
        }
    }

    /**
     * Start recording from the microphone.
     * @return true if recording started successfully
     */
    fun start(): Boolean {
        if (isRecording.get()) {
            logWarn(TAG, "Already recording")
            return true
        }

        val line = targetLine ?: run {
            logError(TAG, "Audio not initialized")
            return false
        }

        return try {
            line.open(audioFormat)
            line.start()
            isRecording.set(true)

            // Start capture thread
            captureThread = Thread({
                captureLoop(line)
            }, "WhisperAudio-Capture").apply {
                isDaemon = true
                priority = Thread.MAX_PRIORITY
                start()
            }

            logInfo(TAG, "Recording started")
            true
        } catch (e: Exception) {
            logError(TAG, "Failed to start recording", e)
            onError?.invoke("Failed to start audio capture: ${e.message}")
            false
        }
    }

    /**
     * Stop recording.
     */
    fun stop() {
        isRecording.set(false)
        captureThread?.join(1000)
        captureThread = null

        targetLine?.let { line ->
            if (line.isOpen) {
                line.stop()
                line.close()
            }
        }

        logInfo(TAG, "Recording stopped")
    }

    /**
     * Release all audio resources.
     */
    fun release() {
        stop()
        targetLine = null
        synchronized(bufferLock) {
            audioBuffer.clear()
        }
    }

    /**
     * Check if currently recording.
     */
    fun isRecording(): Boolean = isRecording.get()

    /**
     * Drain the audio buffer and return all accumulated samples.
     * Clears the buffer after reading.
     */
    fun drainBuffer(): FloatArray {
        synchronized(bufferLock) {
            if (audioBuffer.isEmpty()) return FloatArray(0)
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

    // --- Private implementation ---

    private fun captureLoop(line: TargetDataLine) {
        val readBuffer = ByteArray(READ_BUFFER_SIZE)
        logDebug(TAG, "Capture loop started")

        while (isRecording.get()) {
            val bytesRead = line.read(readBuffer, 0, readBuffer.size)
            if (bytesRead <= 0) continue

            // Convert Int16 PCM bytes to Float32
            val samplesRead = bytesRead / BYTES_PER_SAMPLE
            val floatSamples = FloatArray(samplesRead)
            var rmsSum = 0.0

            for (i in 0 until samplesRead) {
                val byteOffset = i * BYTES_PER_SAMPLE
                // Little-endian 16-bit signed
                val sample = (readBuffer[byteOffset + 1].toInt() shl 8) or
                        (readBuffer[byteOffset].toInt() and 0xFF)
                val normalized = sample.toShort().toFloat() / Short.MAX_VALUE
                floatSamples[i] = normalized
                rmsSum += normalized * normalized
            }

            currentLevel = kotlin.math.sqrt(rmsSum / samplesRead).toFloat()

            synchronized(bufferLock) {
                for (sample in floatSamples) {
                    audioBuffer.add(sample)
                }

                // Enforce max buffer size
                while (audioBuffer.size > MAX_BUFFER_SAMPLES) {
                    audioBuffer.removeAt(0)
                }
            }
        }

        logDebug(TAG, "Capture loop ended")
    }
}
