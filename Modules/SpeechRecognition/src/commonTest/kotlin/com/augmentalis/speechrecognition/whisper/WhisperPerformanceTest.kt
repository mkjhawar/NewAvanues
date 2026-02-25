/**
 * WhisperPerformanceTest.kt - Tests for WhisperPerformance metrics tracker
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-24
 *
 * Tests initialization recording, transcription counters, rolling-window averages,
 * peak tracking, language detection, success rate, metrics map, and reset behavior.
 */
package com.augmentalis.speechrecognition.whisper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WhisperPerformanceTest {

    // ── Initial State ──────────────────────────────────────────────

    @Test
    fun `fresh instance has all counters at zero`() {
        val perf = WhisperPerformance()
        assertEquals(0, perf.totalTranscriptions)
        assertEquals(0, perf.emptyTranscriptions)
        assertEquals(0L, perf.totalAudioProcessedMs)
        assertEquals(0L, perf.totalProcessingTimeMs)
        assertEquals(0L, perf.totalCharactersTranscribed)
        assertEquals(0L, perf.totalSegments)
        assertEquals(0L, perf.peakLatencyMs)
        assertEquals(0f, perf.peakRTF)
        assertEquals(0, perf.initAttempts)
        assertEquals(0L, perf.initTimeMs)
        assertEquals(0, perf.languageDetectionCount)
    }

    @Test
    fun `fresh instance has null modelSize and detectedLanguage`() {
        val perf = WhisperPerformance()
        assertNull(perf.modelSize)
        assertNull(perf.detectedLanguage)
    }

    @Test
    fun `fresh instance getAverageLatencyMs returns zero`() {
        val perf = WhisperPerformance()
        assertEquals(0L, perf.getAverageLatencyMs())
    }

    @Test
    fun `fresh instance getAverageRTF returns zero`() {
        val perf = WhisperPerformance()
        assertEquals(0f, perf.getAverageRTF())
    }

    @Test
    fun `fresh instance getAverageConfidence returns zero`() {
        val perf = WhisperPerformance()
        assertEquals(0f, perf.getAverageConfidence())
    }

    // ── Initialization Recording ────────────────────────────────────

    @Test
    fun `recordInitialization sets initTimeMs modelSize and initAttempts`() {
        val perf = WhisperPerformance()
        perf.recordInitialization(initTimeMs = 1500, modelSize = WhisperModelSize.BASE, attempts = 3)
        assertEquals(1500L, perf.initTimeMs)
        assertEquals(WhisperModelSize.BASE, perf.modelSize)
        assertEquals(3, perf.initAttempts)
    }

    @Test
    fun `recordInitialization defaults to 1 attempt`() {
        val perf = WhisperPerformance()
        perf.recordInitialization(initTimeMs = 800, modelSize = WhisperModelSize.TINY)
        assertEquals(1, perf.initAttempts)
    }

    @Test
    fun `multiple recordInitialization calls overwrite not accumulate`() {
        val perf = WhisperPerformance()
        perf.recordInitialization(initTimeMs = 1000, modelSize = WhisperModelSize.TINY, attempts = 2)
        perf.recordInitialization(initTimeMs = 500, modelSize = WhisperModelSize.SMALL, attempts = 1)
        assertEquals(500L, perf.initTimeMs)
        assertEquals(WhisperModelSize.SMALL, perf.modelSize)
        assertEquals(1, perf.initAttempts)
    }

    // ── Transcription Counters ──────────────────────────────────────

    @Test
    fun `single transcription increments totalTranscriptions`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(
            audioDurationMs = 3000,
            processingTimeMs = 600,
            textLength = 50,
            segmentCount = 2,
            avgConfidence = 0.9f
        )
        assertEquals(1, perf.totalTranscriptions)
    }

    @Test
    fun `multiple transcriptions accumulate audio and processing totals`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(audioDurationMs = 3000, processingTimeMs = 600, textLength = 50, segmentCount = 2)
        perf.recordTranscription(audioDurationMs = 5000, processingTimeMs = 1200, textLength = 80, segmentCount = 4)
        assertEquals(2, perf.totalTranscriptions)
        assertEquals(8000L, perf.totalAudioProcessedMs)
        assertEquals(1800L, perf.totalProcessingTimeMs)
    }

    @Test
    fun `characters and segments accumulate correctly`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 200, textLength = 30, segmentCount = 1)
        perf.recordTranscription(audioDurationMs = 2000, processingTimeMs = 400, textLength = 70, segmentCount = 3)
        perf.recordTranscription(audioDurationMs = 1500, processingTimeMs = 300, textLength = 50, segmentCount = 2)
        assertEquals(150L, perf.totalCharactersTranscribed)
        assertEquals(6L, perf.totalSegments)
    }

    @Test
    fun `textLength zero increments emptyTranscriptions`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(audioDurationMs = 2000, processingTimeMs = 300, textLength = 0, segmentCount = 0)
        assertEquals(1, perf.emptyTranscriptions)
        assertEquals(1, perf.totalTranscriptions)
    }

    @Test
    fun `textLength greater than zero does not increment emptyTranscriptions`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(audioDurationMs = 2000, processingTimeMs = 300, textLength = 10, segmentCount = 1)
        assertEquals(0, perf.emptyTranscriptions)
        assertEquals(1, perf.totalTranscriptions)
    }

    // ── Rolling Window Averages ─────────────────────────────────────

    @Test
    fun `getAverageLatencyMs returns correct average after multiple recordings`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 100, textLength = 10, segmentCount = 1)
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 200, textLength = 10, segmentCount = 1)
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 300, textLength = 10, segmentCount = 1)
        // Average of [100, 200, 300] = 200
        assertEquals(200L, perf.getAverageLatencyMs())
    }

    @Test
    fun `getAverageRTF computes processing over audio ratio correctly`() {
        val perf = WhisperPerformance()
        // RTF = 500 / 1000 = 0.5
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 500, textLength = 10, segmentCount = 1)
        assertEquals(0.5f, perf.getAverageRTF())
    }

    @Test
    fun `getAverageConfidence averages recorded confidence values`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 200, textLength = 10, segmentCount = 1, avgConfidence = 0.8f)
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 200, textLength = 10, segmentCount = 1, avgConfidence = 0.9f)
        // Average of [0.8, 0.9] ~ 0.85
        val avg = perf.getAverageConfidence()
        assertTrue(avg > 0.84f && avg < 0.86f, "Expected ~0.85 but got $avg")
    }

    @Test
    fun `confidence of zero is not added to rolling window`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 200, textLength = 10, segmentCount = 1, avgConfidence = 0.9f)
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 200, textLength = 10, segmentCount = 1, avgConfidence = 0f)
        // Only the 0.9f sample should be in the window
        assertEquals(0.9f, perf.getAverageConfidence())
    }

    @Test
    fun `RTF not recorded when audioDurationMs is zero`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(audioDurationMs = 0, processingTimeMs = 200, textLength = 10, segmentCount = 1)
        // No RTF sample added — division guard on line 108
        assertEquals(0f, perf.getAverageRTF())
    }

    @Test
    fun `rolling window evicts oldest sample at capacity`() {
        val perf = WhisperPerformance()
        // First sample: outlier latency of 1000ms
        perf.recordTranscription(audioDurationMs = 2000, processingTimeMs = 1000, textLength = 10, segmentCount = 1)
        // Fill to capacity with 99 more samples of 100ms
        repeat(99) {
            perf.recordTranscription(audioDurationMs = 200, processingTimeMs = 100, textLength = 10, segmentCount = 1)
        }
        // Window is full (100 samples): [1000, 100, 100, ..., 100]
        // Average = (1000 + 99*100) / 100 = 10900 / 100 = 109
        assertEquals(109L, perf.getAverageLatencyMs())

        // 101st sample evicts the 1000ms outlier
        perf.recordTranscription(audioDurationMs = 200, processingTimeMs = 100, textLength = 10, segmentCount = 1)
        // Window: [100, 100, ..., 100] — all 100
        assertEquals(100L, perf.getAverageLatencyMs())
    }

    // ── Peak Tracking ───────────────────────────────────────────────

    @Test
    fun `peakLatencyMs tracks highest processingTimeMs`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 200, textLength = 10, segmentCount = 1)
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 500, textLength = 10, segmentCount = 1)
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 300, textLength = 10, segmentCount = 1)
        assertEquals(500L, perf.peakLatencyMs)
    }

    @Test
    fun `peakRTF tracks highest real-time factor`() {
        val perf = WhisperPerformance()
        // RTF = 200/1000 = 0.2
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 200, textLength = 10, segmentCount = 1)
        // RTF = 1500/1000 = 1.5
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 1500, textLength = 10, segmentCount = 1)
        // RTF = 300/1000 = 0.3
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 300, textLength = 10, segmentCount = 1)
        assertEquals(1.5f, perf.peakRTF)
    }

    @Test
    fun `peak values do not decrease when lower values are recorded`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 800, textLength = 10, segmentCount = 1)
        val peakAfterFirst = perf.peakLatencyMs
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 100, textLength = 10, segmentCount = 1)
        assertEquals(peakAfterFirst, perf.peakLatencyMs)
        assertEquals(800L, perf.peakLatencyMs)
    }

    // ── Empty Transcription Shorthand ────────────────────────────────

    @Test
    fun `recordEmptyTranscription increments emptyTranscriptions`() {
        val perf = WhisperPerformance()
        perf.recordEmptyTranscription(audioDurationMs = 2000, processingTimeMs = 150)
        assertEquals(1, perf.emptyTranscriptions)
        assertEquals(1, perf.totalTranscriptions)
        assertEquals(0L, perf.totalCharactersTranscribed)
    }

    @Test
    fun `recordEmptyTranscription still records latency and RTF samples`() {
        val perf = WhisperPerformance()
        perf.recordEmptyTranscription(audioDurationMs = 2000, processingTimeMs = 400)
        // Latency sample should be recorded
        assertEquals(400L, perf.getAverageLatencyMs())
        // RTF = 400/2000 = 0.2
        assertEquals(0.2f, perf.getAverageRTF())
    }

    // ── Language Detection ──────────────────────────────────────────

    @Test
    fun `recordLanguageDetection sets language and increments count`() {
        val perf = WhisperPerformance()
        perf.recordLanguageDetection("en")
        assertEquals("en", perf.detectedLanguage)
        assertEquals(1, perf.languageDetectionCount)
    }

    @Test
    fun `multiple language detections update language and accumulate count`() {
        val perf = WhisperPerformance()
        perf.recordLanguageDetection("en")
        perf.recordLanguageDetection("fr")
        perf.recordLanguageDetection("de")
        assertEquals("de", perf.detectedLanguage)
        assertEquals(3, perf.languageDetectionCount)
    }

    // ── Success Rate ────────────────────────────────────────────────

    @Test
    fun `success rate is 100 percent when no empty transcriptions`() {
        val perf = WhisperPerformance()
        repeat(5) {
            perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 200, textLength = 20, segmentCount = 1)
        }
        val metrics = perf.getMetrics()
        assertEquals(100f, metrics["successRate"])
    }

    @Test
    fun `success rate is 0 percent when all transcriptions are empty`() {
        val perf = WhisperPerformance()
        repeat(3) {
            perf.recordEmptyTranscription(audioDurationMs = 1000, processingTimeMs = 100)
        }
        val metrics = perf.getMetrics()
        assertEquals(0f, metrics["successRate"])
    }

    @Test
    fun `success rate is correct ratio for mixed transcriptions`() {
        val perf = WhisperPerformance()
        // 3 successful
        repeat(3) {
            perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 200, textLength = 20, segmentCount = 1)
        }
        // 1 empty
        perf.recordEmptyTranscription(audioDurationMs = 1000, processingTimeMs = 100)
        // 3/4 = 75%
        val metrics = perf.getMetrics()
        assertEquals(75f, metrics["successRate"])
    }

    // ── getMetrics Completeness ─────────────────────────────────────

    @Test
    fun `getMetrics contains all base keys`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 200, textLength = 10, segmentCount = 1)
        val metrics = perf.getMetrics()
        val requiredKeys = listOf(
            "initTimeMs", "initAttempts", "totalTranscriptions", "emptyTranscriptions",
            "totalAudioProcessedMs", "totalProcessingTimeMs", "totalCharactersTranscribed",
            "totalSegments", "peakLatencyMs", "peakRTF", "successRate"
        )
        requiredKeys.forEach { key ->
            assertTrue(metrics.containsKey(key), "Missing key: $key")
        }
    }

    @Test
    fun `getMetrics contains avgLatencyMs and recentLatencyMs after recording`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 300, textLength = 10, segmentCount = 1)
        val metrics = perf.getMetrics()
        assertTrue(metrics.containsKey("avgLatencyMs"), "Missing avgLatencyMs")
        assertTrue(metrics.containsKey("recentLatencyMs"), "Missing recentLatencyMs")
        assertEquals(300L, metrics["avgLatencyMs"])
        assertEquals(300L, metrics["recentLatencyMs"])
    }

    @Test
    fun `getMetrics contains modelSize only when set`() {
        val perf = WhisperPerformance()
        val metricsBefore = perf.getMetrics()
        assertFalse(metricsBefore.containsKey("modelSize"), "modelSize should not be present before init")

        perf.recordInitialization(initTimeMs = 1000, modelSize = WhisperModelSize.SMALL)
        val metricsAfter = perf.getMetrics()
        assertTrue(metricsAfter.containsKey("modelSize"), "modelSize should be present after init")
        assertEquals("SMALL", metricsAfter["modelSize"])
    }

    @Test
    fun `getMetrics successRate is computed correctly`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(audioDurationMs = 1000, processingTimeMs = 200, textLength = 20, segmentCount = 1)
        perf.recordEmptyTranscription(audioDurationMs = 1000, processingTimeMs = 100)
        val metrics = perf.getMetrics()
        // 1 success out of 2 = 50%
        assertEquals(50f, metrics["successRate"])
    }

    // ── Reset ───────────────────────────────────────────────────────

    @Test
    fun `reset clears all counters peaks and nulls`() {
        val perf = WhisperPerformance()
        perf.recordInitialization(initTimeMs = 1200, modelSize = WhisperModelSize.BASE, attempts = 2)
        perf.recordTranscription(
            audioDurationMs = 3000, processingTimeMs = 800,
            textLength = 42, segmentCount = 3, avgConfidence = 0.87f
        )
        perf.recordLanguageDetection("en")

        perf.reset()

        assertEquals(0L, perf.initTimeMs)
        assertNull(perf.modelSize)
        assertEquals(0, perf.initAttempts)
        assertEquals(0, perf.totalTranscriptions)
        assertEquals(0, perf.emptyTranscriptions)
        assertEquals(0L, perf.totalAudioProcessedMs)
        assertEquals(0L, perf.totalProcessingTimeMs)
        assertEquals(0L, perf.totalCharactersTranscribed)
        assertEquals(0L, perf.totalSegments)
        assertEquals(0L, perf.peakLatencyMs)
        assertEquals(0f, perf.peakRTF)
        assertNull(perf.detectedLanguage)
        assertEquals(0, perf.languageDetectionCount)
    }

    @Test
    fun `reset clears rolling windows so averages return zero`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(
            audioDurationMs = 2000, processingTimeMs = 500,
            textLength = 30, segmentCount = 2, avgConfidence = 0.85f
        )
        assertTrue(perf.getAverageLatencyMs() > 0, "Latency should be non-zero before reset")
        assertTrue(perf.getAverageRTF() > 0f, "RTF should be non-zero before reset")
        assertTrue(perf.getAverageConfidence() > 0f, "Confidence should be non-zero before reset")

        perf.reset()

        assertEquals(0L, perf.getAverageLatencyMs())
        assertEquals(0f, perf.getAverageRTF())
        assertEquals(0f, perf.getAverageConfidence())
    }

    // ── Edge Cases ──────────────────────────────────────────────────

    @Test
    fun `zero audioDurationMs does not produce RTF sample`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(audioDurationMs = 0, processingTimeMs = 500, textLength = 10, segmentCount = 1)
        assertEquals(0f, perf.getAverageRTF())
        assertEquals(0f, perf.peakRTF)
    }

    @Test
    fun `large values do not crash`() {
        val perf = WhisperPerformance()
        perf.recordTranscription(
            audioDurationMs = 999_999_999L,
            processingTimeMs = 999_999_999L,
            textLength = Int.MAX_VALUE,
            segmentCount = Int.MAX_VALUE,
            avgConfidence = 1.0f
        )
        assertEquals(1, perf.totalTranscriptions)
        assertTrue(perf.totalAudioProcessedMs > 0, "Should have recorded audio duration")
        assertTrue(perf.totalCharactersTranscribed > 0, "Should have recorded character count")
    }

    @Test
    fun `getMetrics on fresh instance returns valid map with zeros`() {
        val perf = WhisperPerformance()
        val metrics = perf.getMetrics()
        assertTrue(metrics.isNotEmpty(), "Metrics map should not be empty")
        assertEquals(0L, metrics["initTimeMs"])
        assertEquals(0, metrics["initAttempts"])
        assertEquals(0, metrics["totalTranscriptions"])
        assertEquals(0f, metrics["successRate"])
        assertFalse(metrics.containsKey("avgLatencyMs"), "No latency samples yet")
        assertFalse(metrics.containsKey("modelSize"), "No model initialized")
        assertFalse(metrics.containsKey("detectedLanguage"), "No language detected")
    }
}
