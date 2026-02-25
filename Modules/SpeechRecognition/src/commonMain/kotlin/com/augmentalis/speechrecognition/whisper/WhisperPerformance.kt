/**
 * WhisperPerformance.kt - Shared performance tracking for Whisper engines
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * Platform-agnostic performance metrics tracker for Whisper speech recognition.
 * Tracks latency, real-time factor, accuracy indicators, and resource usage.
 * Used by both Android WhisperEngine and DesktopWhisperEngine.
 */
package com.augmentalis.speechrecognition.whisper

import com.augmentalis.speechrecognition.SpeechMetricsSnapshot
import kotlin.concurrent.Volatile
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Tracks performance metrics for Whisper transcription operations.
 *
 * Thread-safe via SynchronizedObject + synchronized blocks (cross-platform).
 * Maintains a rolling window of recent samples for accurate averages.
 *
 * Usage:
 * ```kotlin
 * val perf = WhisperPerformance()
 * perf.recordInitialization(initTimeMs = 1200, modelSize = WhisperModelSize.BASE)
 * perf.recordTranscription(
 *     audioDurationMs = 3000,
 *     processingTimeMs = 800,
 *     textLength = 42,
 *     segmentCount = 3,
 *     avgConfidence = 0.87f
 * )
 * val metrics = perf.getMetrics()
 * ```
 */
class WhisperPerformance : SynchronizedObject() {

    companion object {
        /** Maximum samples to keep for rolling averages */
        private const val MAX_SAMPLES = 100
    }

    // Initialization metrics
    @kotlin.concurrent.Volatile var initTimeMs: Long = 0L; private set
    @kotlin.concurrent.Volatile var modelSize: WhisperModelSize? = null; private set
    @kotlin.concurrent.Volatile var initAttempts: Int = 0; private set

    // Transcription metrics (rolling window)
    private val latencySamples = ArrayDeque<Long>(MAX_SAMPLES)
    private val rtfSamples = ArrayDeque<Float>(MAX_SAMPLES)
    private val confidenceSamples = ArrayDeque<Float>(MAX_SAMPLES)

    // Counters
    @kotlin.concurrent.Volatile var totalTranscriptions: Int = 0; private set
    @kotlin.concurrent.Volatile var emptyTranscriptions: Int = 0; private set
    @kotlin.concurrent.Volatile var totalAudioProcessedMs: Long = 0; private set
    @kotlin.concurrent.Volatile var totalProcessingTimeMs: Long = 0; private set
    @kotlin.concurrent.Volatile var totalCharactersTranscribed: Long = 0; private set
    @kotlin.concurrent.Volatile var totalSegments: Long = 0; private set

    // Peaks
    @kotlin.concurrent.Volatile var peakLatencyMs: Long = 0; private set
    @kotlin.concurrent.Volatile var peakRTF: Float = 0f; private set

    // Language detection
    @kotlin.concurrent.Volatile var detectedLanguage: String? = null; private set
    @kotlin.concurrent.Volatile var languageDetectionCount: Int = 0; private set

    /**
     * Record engine initialization timing.
     */
    fun recordInitialization(initTimeMs: Long, modelSize: WhisperModelSize, attempts: Int = 1) {
        synchronized(this) {
            this.initTimeMs = initTimeMs
            this.modelSize = modelSize
            this.initAttempts = attempts
        }
    }

    /**
     * Record a transcription operation's metrics.
     */
    fun recordTranscription(
        audioDurationMs: Long,
        processingTimeMs: Long,
        textLength: Int,
        segmentCount: Int,
        avgConfidence: Float = 0f
    ) {
        synchronized(this) {
            totalTranscriptions++
            totalAudioProcessedMs += audioDurationMs
            totalProcessingTimeMs += processingTimeMs
            totalCharactersTranscribed += textLength
            totalSegments += segmentCount

            if (textLength == 0) {
                emptyTranscriptions++
            }

            // Latency
            addSample(latencySamples, processingTimeMs)
            if (processingTimeMs > peakLatencyMs) peakLatencyMs = processingTimeMs

            // Real-time factor (processing time / audio duration)
            if (audioDurationMs > 0) {
                val rtf = processingTimeMs.toFloat() / audioDurationMs
                addSample(rtfSamples, rtf)
                if (rtf > peakRTF) peakRTF = rtf
            }

            // Confidence
            if (avgConfidence > 0f) {
                addSample(confidenceSamples, avgConfidence)
            }
        }
    }

    /**
     * Record an empty transcription (silence/noise detected).
     */
    fun recordEmptyTranscription(audioDurationMs: Long, processingTimeMs: Long) {
        recordTranscription(
            audioDurationMs = audioDurationMs,
            processingTimeMs = processingTimeMs,
            textLength = 0,
            segmentCount = 0
        )
    }

    /**
     * Record a detected language from whisper auto-detection.
     */
    fun recordLanguageDetection(language: String) {
        synchronized(this) {
            this.detectedLanguage = language
            languageDetectionCount++
        }
    }

    /**
     * Get all metrics as a map for logging/reporting.
     */
    fun getMetrics(): Map<String, Any> {
        return synchronized(this) {
            val metrics = mutableMapOf<String, Any>(
                "initTimeMs" to initTimeMs,
                "initAttempts" to initAttempts,
                "totalTranscriptions" to totalTranscriptions,
                "emptyTranscriptions" to emptyTranscriptions,
                "totalAudioProcessedMs" to totalAudioProcessedMs,
                "totalProcessingTimeMs" to totalProcessingTimeMs,
                "totalCharactersTranscribed" to totalCharactersTranscribed,
                "totalSegments" to totalSegments,
                "peakLatencyMs" to peakLatencyMs,
                "peakRTF" to peakRTF
            )

            modelSize?.let { metrics["modelSize"] = it.name }

            if (latencySamples.isNotEmpty()) {
                metrics["avgLatencyMs"] = latencySamples.average().toLong()
                metrics["recentLatencyMs"] = latencySamples.last()
            }

            if (rtfSamples.isNotEmpty()) {
                metrics["avgRTF"] = rtfSamples.average().toFloat()
            }

            if (confidenceSamples.isNotEmpty()) {
                metrics["avgConfidence"] = confidenceSamples.average().toFloat()
                metrics["minConfidence"] = confidenceSamples.min()
            }

            detectedLanguage?.let { metrics["detectedLanguage"] = it }

            val successRate = if (totalTranscriptions > 0) {
                ((totalTranscriptions - emptyTranscriptions).toFloat() / totalTranscriptions * 100)
            } else 0f
            metrics["successRate"] = successRate

            metrics
        }
    }

    /**
     * Get the average latency over the rolling window.
     */
    fun getAverageLatencyMs(): Long {
        return synchronized(this) {
            if (latencySamples.isNotEmpty()) latencySamples.average().toLong() else 0L
        }
    }

    /**
     * Get the average real-time factor over the rolling window.
     * RTF < 1.0 means faster-than-realtime. RTF > 1.0 means slower.
     */
    fun getAverageRTF(): Float {
        return synchronized(this) {
            if (rtfSamples.isNotEmpty()) rtfSamples.average().toFloat() else 0f
        }
    }

    /**
     * Get the average confidence over the rolling window.
     */
    fun getAverageConfidence(): Float {
        return synchronized(this) {
            if (confidenceSamples.isNotEmpty()) confidenceSamples.average().toFloat() else 0f
        }
    }

    /**
     * Create an immutable [SpeechMetricsSnapshot] capturing the current state.
     *
     * @param engineName Name of the engine ("Whisper", "GoogleCloud")
     * @param engineState Current engine state name (e.g. "READY", "LISTENING")
     * @return Frozen snapshot suitable for UI display or serialization
     */
    fun toSnapshot(engineName: String, engineState: String, timestampMs: Long = 0): SpeechMetricsSnapshot {
        return synchronized(this) {
            val successRate = if (totalTranscriptions > 0) {
                ((totalTranscriptions - emptyTranscriptions).toFloat() / totalTranscriptions * 100)
            } else 0f

            SpeechMetricsSnapshot(
                engineName = engineName,
                modelSize = modelSize?.name,
                initTimeMs = initTimeMs,
                totalTranscriptions = totalTranscriptions,
                successRate = successRate,
                avgLatencyMs = if (latencySamples.isNotEmpty()) latencySamples.average().toLong() else 0L,
                avgRTF = if (rtfSamples.isNotEmpty()) rtfSamples.average().toFloat() else 0f,
                avgConfidence = if (confidenceSamples.isNotEmpty()) confidenceSamples.average().toFloat() else 0f,
                peakLatencyMs = peakLatencyMs,
                peakRTF = peakRTF,
                totalAudioProcessedMs = totalAudioProcessedMs,
                detectedLanguage = detectedLanguage,
                engineState = engineState,
                timestampMs = timestampMs
            )
        }
    }

    /**
     * Reset all metrics.
     */
    fun reset() {
        synchronized(this) {
            initTimeMs = 0L
            modelSize = null
            initAttempts = 0
            latencySamples.clear()
            rtfSamples.clear()
            confidenceSamples.clear()
            totalTranscriptions = 0
            emptyTranscriptions = 0
            totalAudioProcessedMs = 0
            totalProcessingTimeMs = 0
            totalCharactersTranscribed = 0
            totalSegments = 0
            peakLatencyMs = 0
            peakRTF = 0f
            detectedLanguage = null
            languageDetectionCount = 0
        }
    }

    private fun <T> addSample(deque: ArrayDeque<T>, value: T) {
        if (deque.size >= MAX_SAMPLES) {
            deque.removeFirst()
        }
        deque.addLast(value)
    }
}
