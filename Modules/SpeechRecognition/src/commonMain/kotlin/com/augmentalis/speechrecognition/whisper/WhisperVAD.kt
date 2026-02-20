/**
 * WhisperVAD.kt - Voice Activity Detection for Whisper chunked transcription
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * Energy-based VAD that segments continuous audio into speech chunks.
 * Uses RMS energy with adaptive threshold, hangover timer, and min-duration
 * filtering to produce clean speech boundaries for batch transcription.
 *
 * This is lightweight by design — no neural network, no external dependencies.
 * Platform-agnostic: lives in commonMain so Android, iOS, and Desktop share
 * the same VAD algorithm.
 */
package com.augmentalis.speechrecognition.whisper

import com.augmentalis.speechrecognition.logDebug
import kotlin.math.sqrt

/**
 * Energy-based Voice Activity Detection for segmenting audio into speech chunks.
 *
 * Algorithm:
 * 1. Compute RMS energy for each audio frame (10ms windows)
 * 2. Compare against adaptive threshold
 * 3. Use hangover timer to bridge short pauses within utterances
 * 4. Emit complete chunks when silence exceeds hangover threshold
 *
 * Thread safety: All methods must be called from the same thread (the audio capture thread).
 *
 * @param speechThreshold RMS energy threshold (0.0-1.0). Auto-calibrates if 0.
 * @param silenceTimeoutMs Duration (ms) of silence before a speech chunk is finalized
 * @param minSpeechDurationMs Minimum speech duration (ms) to consider valid
 * @param maxSpeechDurationMs Maximum speech duration (ms) before forced emission
 * @param hangoverFrames Number of silence frames tolerated within speech
 * @param paddingMs Padding (ms) to add before and after speech boundaries
 * @param sampleRate Audio sample rate (default 16000 Hz for Whisper)
 */
class WhisperVAD(
    private var speechThreshold: Float = 0f,
    private val silenceTimeoutMs: Long = 700,
    private val minSpeechDurationMs: Long = 300,
    private val maxSpeechDurationMs: Long = 30_000,
    private val hangoverFrames: Int = 5,
    private val paddingMs: Long = 150,
    private val sampleRate: Int = 16000
) {
    companion object {
        private const val TAG = "WhisperVAD"

        /** Frame size for energy computation: 10ms at 16kHz = 160 samples */
        private const val FRAME_SIZE = 160

        /** Initial adaptive threshold if auto-calibration is enabled */
        private const val INITIAL_THRESHOLD = 0.003f

        /** Adaptive threshold smoothing factor (lower = slower adaptation) */
        private const val THRESHOLD_ALPHA = 0.02f

        /** Minimum threshold floor to prevent silence from zeroing out */
        private const val MIN_THRESHOLD = 0.001f
    }

    // State
    private var state = VADState.SILENCE
    private var hangoverCount = 0

    // Audio accumulation
    private val speechBuffer = ArrayList<Float>(sampleRate * 5) // pre-alloc 5s
    private val paddingBuffer = ArrayList<Float>(paddingMs.toInt() * sampleRate / 1000)
    private var speechStartTimeMs = 0L
    private var lastSpeechTimeMs = 0L

    // Adaptive threshold
    private var adaptiveThreshold = if (speechThreshold > 0f) speechThreshold else INITIAL_THRESHOLD
    private var noiseFloor = 0f
    private var calibrationFrames = 0

    // Callback
    var onSpeechChunkReady: OnSpeechChunkReady? = null

    /**
     * Process a block of audio samples through the VAD.
     *
     * @param samples Float32 PCM audio samples at configured sample rate
     * @param timestampMs Current timestamp in milliseconds
     */
    fun processAudio(samples: FloatArray, timestampMs: Long) {
        var offset = 0

        while (offset + FRAME_SIZE <= samples.size) {
            val frameEnergy = computeFrameRMS(samples, offset, FRAME_SIZE)
            val isSpeech = frameEnergy > adaptiveThreshold

            // Update adaptive threshold during silence
            if (!isSpeech && state == VADState.SILENCE) {
                updateNoiseFloor(frameEnergy)
            }

            processFrame(isSpeech, samples, offset, FRAME_SIZE, timestampMs)

            offset += FRAME_SIZE
        }

        // Process remaining samples (partial frame)
        if (offset < samples.size) {
            val remaining = samples.size - offset
            val frameEnergy = computeFrameRMS(samples, offset, remaining)
            val isSpeech = frameEnergy > adaptiveThreshold
            processFrame(isSpeech, samples, offset, remaining, timestampMs)
        }
    }

    /**
     * Force-finalize any buffered speech (e.g., when stopping the engine).
     */
    fun flush() {
        if (speechBuffer.isNotEmpty() && state != VADState.SILENCE) {
            emitChunk()
        }
        reset()
    }

    /**
     * Reset VAD state (but keep calibration).
     */
    fun reset() {
        state = VADState.SILENCE
        hangoverCount = 0
        speechBuffer.clear()
        paddingBuffer.clear()
        speechStartTimeMs = 0L
        lastSpeechTimeMs = 0L
    }

    /**
     * Get current VAD state.
     */
    fun getState(): VADState = state

    /**
     * Get current adaptive threshold.
     */
    fun getAdaptiveThreshold(): Float = adaptiveThreshold

    // --- Private implementation ---

    private fun processFrame(
        isSpeech: Boolean,
        samples: FloatArray,
        offset: Int,
        length: Int,
        timestampMs: Long
    ) {
        when (state) {
            VADState.SILENCE -> {
                if (isSpeech) {
                    // Speech onset detected
                    state = VADState.SPEECH
                    speechStartTimeMs = timestampMs
                    lastSpeechTimeMs = timestampMs
                    hangoverCount = 0

                    // Include padding buffer (audio just before speech onset)
                    speechBuffer.addAll(paddingBuffer)
                    appendSamples(samples, offset, length)

                    logDebug(TAG, "Speech onset detected at ${timestampMs}ms")
                } else {
                    // Maintain rolling padding buffer
                    appendToPaddingBuffer(samples, offset, length)
                }
            }

            VADState.SPEECH -> {
                appendSamples(samples, offset, length)

                if (isSpeech) {
                    lastSpeechTimeMs = timestampMs
                    hangoverCount = 0
                } else {
                    hangoverCount++
                    if (hangoverCount >= hangoverFrames) {
                        state = VADState.HANGOVER
                    }
                }

                // Force emit if max duration exceeded
                val speechDuration = timestampMs - speechStartTimeMs
                if (speechDuration >= maxSpeechDurationMs) {
                    logDebug(TAG, "Max duration reached, forcing chunk emission")
                    emitChunk()
                }
            }

            VADState.HANGOVER -> {
                appendSamples(samples, offset, length)

                if (isSpeech) {
                    // Speech resumed during hangover
                    state = VADState.SPEECH
                    lastSpeechTimeMs = timestampMs
                    hangoverCount = 0
                } else {
                    val silenceDuration = timestampMs - lastSpeechTimeMs
                    if (silenceDuration >= silenceTimeoutMs) {
                        // Silence confirmed — emit the chunk
                        emitChunk()
                    }
                }
            }
        }
    }

    private fun emitChunk() {
        val durationMs = if (speechStartTimeMs > 0L) {
            lastSpeechTimeMs - speechStartTimeMs + (paddingMs)
        } else {
            (speechBuffer.size * 1000L) / sampleRate
        }

        if (durationMs >= minSpeechDurationMs && speechBuffer.isNotEmpty()) {
            val audioData = speechBuffer.toFloatArray()
            logDebug(TAG, "Emitting speech chunk: ${audioData.size} samples, ~${durationMs}ms")
            onSpeechChunkReady?.onChunkReady(audioData, durationMs)
        } else {
            logDebug(TAG, "Discarding short chunk: ${durationMs}ms (min: ${minSpeechDurationMs}ms)")
        }

        // Reset for next utterance
        speechBuffer.clear()
        paddingBuffer.clear()
        state = VADState.SILENCE
        hangoverCount = 0
        speechStartTimeMs = 0L
        lastSpeechTimeMs = 0L
    }

    private fun appendSamples(samples: FloatArray, offset: Int, length: Int) {
        for (i in offset until offset + length) {
            speechBuffer.add(samples[i])
        }
    }

    private fun appendToPaddingBuffer(samples: FloatArray, offset: Int, length: Int) {
        val maxPaddingSamples = (paddingMs * sampleRate / 1000).toInt()

        for (i in offset until offset + length) {
            paddingBuffer.add(samples[i])
        }

        // Trim to max padding size
        while (paddingBuffer.size > maxPaddingSamples) {
            paddingBuffer.removeAt(0)
        }
    }

    private fun computeFrameRMS(samples: FloatArray, offset: Int, length: Int): Float {
        var sumSquares = 0.0
        val end = minOf(offset + length, samples.size)
        for (i in offset until end) {
            val s = samples[i].toDouble()
            sumSquares += s * s
        }
        return sqrt(sumSquares / (end - offset)).toFloat()
    }

    private fun updateNoiseFloor(energy: Float) {
        calibrationFrames++
        if (calibrationFrames <= 10) {
            // Initial calibration: accumulate noise floor
            noiseFloor = if (calibrationFrames == 1) energy
            else noiseFloor * 0.9f + energy * 0.1f
        } else {
            // Running update
            noiseFloor = noiseFloor * (1f - THRESHOLD_ALPHA) + energy * THRESHOLD_ALPHA
        }

        // Set threshold above noise floor
        if (speechThreshold <= 0f) {
            adaptiveThreshold = maxOf(noiseFloor * 3f, MIN_THRESHOLD)
        }
    }
}
