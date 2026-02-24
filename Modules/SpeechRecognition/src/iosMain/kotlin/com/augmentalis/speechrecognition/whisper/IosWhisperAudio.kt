/**
 * IosWhisperAudio.kt - iOS audio capture pipeline for Whisper engine
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 *
 * Captures audio from the device microphone using AVAudioEngine and converts
 * to the format required by whisper.cpp (16kHz, mono, float32).
 *
 * Unlike the Apple Speech path in IosSpeechRecognitionService (which feeds
 * raw buffers to SFSpeechRecognizer), this pipeline captures and converts
 * audio into float32 samples for direct consumption by WhisperVAD and
 * the native whisper_bridge transcription functions.
 */
package com.augmentalis.speechrecognition.whisper

import com.augmentalis.speechrecognition.logDebug
import com.augmentalis.speechrecognition.logError
import com.augmentalis.speechrecognition.logInfo
import com.augmentalis.speechrecognition.logWarn
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import platform.AVFAudio.*
import platform.Foundation.NSError

/**
 * Audio capture pipeline for iOS Whisper engine.
 *
 * Captures microphone audio at the hardware sample rate, then converts
 * to 16kHz mono float32 samples via AVAudioConverter (when needed).
 *
 * Usage:
 * ```kotlin
 * val audio = IosWhisperAudio()
 * audio.initialize()
 * audio.start()
 * // Periodically:
 * val samples = audio.drainBuffer()
 * // Feed samples to WhisperVAD
 * audio.stop()
 * audio.release()
 * ```
 */
@OptIn(ExperimentalForeignApi::class)
class IosWhisperAudio {

    companion object {
        private const val TAG = "IosWhisperAudio"
        const val SAMPLE_RATE = 16000
        private const val BUFFER_SIZE: UInt = 4096u
        private const val MAX_BUFFER_SAMPLES = SAMPLE_RATE * 30 // 30 seconds max buffer
    }

    private val lock = SynchronizedObject()

    private var audioEngine: AVAudioEngine? = null
    private var isCapturing = false

    // Circular buffer for accumulated float32 samples
    private val sampleBuffer = FloatArray(MAX_BUFFER_SAMPLES)
    private var writePos = 0
    private var availableSamples = 0

    /**
     * Initialize the audio capture pipeline.
     * Sets up AVAudioSession for recording and creates the audio engine.
     *
     * @return true if initialization succeeds
     */
    fun initialize(): Boolean {
        return try {
            // Configure audio session for recording
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryRecord, error = null)
            session.setActive(true, error = null)

            audioEngine = AVAudioEngine()

            logInfo(TAG, "Audio capture initialized")
            true
        } catch (e: Exception) {
            logError(TAG, "Failed to initialize audio: ${e.message}")
            false
        }
    }

    /**
     * Start capturing audio from the microphone.
     * Audio is accumulated in the internal buffer until drained.
     *
     * @return true if capture started successfully
     */
    fun start(): Boolean {
        val engine = audioEngine ?: run {
            logError(TAG, "Audio engine not initialized")
            return false
        }

        return try {
            val inputNode = engine.inputNode
            val inputFormat = inputNode.outputFormatForBus(0u)

            // Target format: 16kHz, mono, float32
            val targetFormat = AVAudioFormat(
                commonFormat = AVAudioPCMFormatFloat32,
                sampleRate = SAMPLE_RATE.toDouble(),
                channels = 1u,
                interleaved = false
            )

            // Create converter if sample rates differ
            val needsConversion = inputFormat.sampleRate.toInt() != SAMPLE_RATE

            // Install tap on input node
            inputNode.installTapOnBus(
                bus = 0u,
                bufferSize = BUFFER_SIZE,
                format = if (needsConversion) null else targetFormat
            ) { buffer, _ ->
                buffer?.let { pcmBuffer ->
                    processCapturedBuffer(pcmBuffer, needsConversion, inputFormat.sampleRate)
                }
            }

            engine.prepare()
            engine.startAndReturnError(null)

            synchronized(lock) {
                isCapturing = true
                writePos = 0
                availableSamples = 0
            }

            logInfo(TAG, "Audio capture started (input: ${inputFormat.sampleRate}Hz, " +
                    "target: ${SAMPLE_RATE}Hz, conversion: $needsConversion)")
            true
        } catch (e: Exception) {
            logError(TAG, "Failed to start capture: ${e.message}")
            false
        }
    }

    /**
     * Stop capturing audio. Audio engine is stopped but not released.
     */
    fun stop() {
        try {
            audioEngine?.let { engine ->
                engine.inputNode.removeTapOnBus(0u)
                engine.stop()
            }

            synchronized(lock) {
                isCapturing = false
            }

            logInfo(TAG, "Audio capture stopped")
        } catch (e: Exception) {
            logError(TAG, "Error stopping capture: ${e.message}")
        }
    }

    /**
     * Release all audio resources.
     */
    fun release() {
        stop()
        audioEngine = null
        synchronized(lock) {
            writePos = 0
            availableSamples = 0
        }
        logInfo(TAG, "Audio resources released")
    }

    /**
     * Drain accumulated audio samples from the buffer.
     * Returns a copy of available samples and resets the buffer.
     *
     * @return Float array of audio samples (16kHz mono float32), or empty if none available
     */
    fun drainBuffer(): FloatArray {
        return synchronized(lock) {
            if (availableSamples == 0) {
                return@synchronized FloatArray(0)
            }

            val result = sampleBuffer.copyOfRange(0, availableSamples)
            writePos = 0
            availableSamples = 0
            result
        }
    }

    /**
     * Check if audio capture is currently active.
     */
    fun isCapturing(): Boolean = synchronized(lock) { isCapturing }

    /**
     * Process captured audio buffer — convert to 16kHz float32 if needed
     * and append to the sample buffer.
     */
    private fun processCapturedBuffer(
        buffer: AVAudioPCMBuffer,
        needsConversion: Boolean,
        inputSampleRate: Double
    ) {
        val frameCount = buffer.frameLength.toInt()
        if (frameCount == 0) return

        try {
            // Get float32 channel data
            val channelData = buffer.floatChannelData
            if (channelData == null) {
                logWarn(TAG, "No float channel data in buffer")
                return
            }

            // Read samples from first channel
            val rawSamples = FloatArray(frameCount)
            val channel0 = channelData[0]!!
            for (i in 0 until frameCount) {
                rawSamples[i] = channel0[i]
            }

            // Downsample if needed (simple linear interpolation)
            val processedSamples = if (needsConversion) {
                downsample(rawSamples, inputSampleRate.toInt(), SAMPLE_RATE)
            } else {
                rawSamples
            }

            // Append to buffer
            synchronized(lock) {
                val spaceAvailable = MAX_BUFFER_SAMPLES - writePos
                val samplesToWrite = minOf(processedSamples.size, spaceAvailable)

                if (samplesToWrite < processedSamples.size) {
                    logWarn(TAG, "Buffer overflow, dropping ${processedSamples.size - samplesToWrite} samples")
                }

                processedSamples.copyInto(sampleBuffer, writePos, 0, samplesToWrite)
                writePos += samplesToWrite
                availableSamples = writePos
            }
        } catch (e: Exception) {
            logError(TAG, "Error processing audio buffer: ${e.message}")
        }
    }

    // Cached FIR anti-aliasing filter coefficients (computed once per sample rate pair)
    private var cachedFilterCoeffs: FloatArray? = null
    private var cachedFilterFromRate: Int = 0

    /**
     * Downsample with anti-aliasing FIR low-pass filter.
     * Applies a windowed sinc filter before linear interpolation to prevent
     * frequencies above the target Nyquist from aliasing into the output.
     */
    private fun downsample(input: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (fromRate == toRate) return input

        // Get or compute FIR coefficients for this sample rate pair
        val coeffs = getAntiAliasingFilter(fromRate, toRate)

        // Apply anti-aliasing low-pass filter
        val filtered = applyFirFilter(input, coeffs)

        // Linear interpolation on filtered signal
        val ratio = fromRate.toDouble() / toRate.toDouble()
        val outputSize = (input.size / ratio).toInt()
        val output = FloatArray(outputSize)

        for (i in 0 until outputSize) {
            val srcIndex = i * ratio
            val srcIndexInt = srcIndex.toInt()
            val frac = (srcIndex - srcIndexInt).toFloat()

            output[i] = if (srcIndexInt + 1 < filtered.size) {
                filtered[srcIndexInt] * (1f - frac) + filtered[srcIndexInt + 1] * frac
            } else if (srcIndexInt < filtered.size) {
                filtered[srcIndexInt]
            } else {
                0f
            }
        }

        return output
    }

    /**
     * Get cached or compute FIR anti-aliasing filter coefficients.
     * Uses a 15-tap windowed sinc filter with Hamming window.
     * Cutoff at target Nyquist frequency (toRate / 2) relative to input rate.
     */
    private fun getAntiAliasingFilter(fromRate: Int, toRate: Int): FloatArray {
        if (cachedFilterCoeffs != null && cachedFilterFromRate == fromRate) {
            return cachedFilterCoeffs!!
        }

        // Cutoff ratio: target Nyquist / source Nyquist
        val cutoffRatio = toRate.toFloat() / fromRate.toFloat()
        val numTaps = 15
        val half = numTaps / 2
        val coeffs = FloatArray(numTaps)
        var sum = 0f

        for (i in 0 until numTaps) {
            val n = i - half
            coeffs[i] = if (n == 0) {
                cutoffRatio
            } else {
                val x = n.toFloat() * PI.toFloat()
                sin(cutoffRatio * x) / x
            }
            // Hamming window
            coeffs[i] *= (0.54f - 0.46f * cos(2f * PI.toFloat() * i / (numTaps - 1)))
            sum += coeffs[i]
        }

        // Normalize so filter has unity gain at DC
        for (i in 0 until numTaps) coeffs[i] /= sum

        cachedFilterCoeffs = coeffs
        cachedFilterFromRate = fromRate
        return coeffs
    }

    /**
     * Apply FIR filter to input signal via convolution.
     */
    private fun applyFirFilter(input: FloatArray, coeffs: FloatArray): FloatArray {
        val half = coeffs.size / 2
        val output = FloatArray(input.size)

        for (i in input.indices) {
            var acc = 0f
            for (j in coeffs.indices) {
                val idx = i - half + j
                if (idx in input.indices) {
                    acc += input[idx] * coeffs[j]
                }
            }
            output[i] = acc
        }

        return output
    }
}
