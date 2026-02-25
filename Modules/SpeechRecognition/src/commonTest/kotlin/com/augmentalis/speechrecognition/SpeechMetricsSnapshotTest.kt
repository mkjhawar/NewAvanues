package com.augmentalis.speechrecognition

import com.augmentalis.speechrecognition.whisper.WhisperModelSize
import com.augmentalis.speechrecognition.whisper.WhisperPerformance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpeechMetricsSnapshotTest {

    @Test
    fun defaultSnapshotHasIdleHealth() {
        val snapshot = SpeechMetricsSnapshot(engineName = "Test")
        assertEquals(SpeechMetricsSnapshot.HealthStatus.IDLE, snapshot.healthStatus)
        assertEquals(0, snapshot.totalTranscriptions)
        assertEquals(0f, snapshot.successRate)
    }

    @Test
    fun goodHealthWhenHighSuccessAndLowLatency() {
        val snapshot = SpeechMetricsSnapshot(
            engineName = "Whisper",
            totalTranscriptions = 10,
            successRate = 90f,
            avgLatencyMs = 500
        )
        assertEquals(SpeechMetricsSnapshot.HealthStatus.GOOD, snapshot.healthStatus)
    }

    @Test
    fun warningHealthWhenModerateSuccess() {
        val snapshot = SpeechMetricsSnapshot(
            engineName = "Whisper",
            totalTranscriptions = 10,
            successRate = 60f,
            avgLatencyMs = 1000
        )
        assertEquals(SpeechMetricsSnapshot.HealthStatus.WARNING, snapshot.healthStatus)
    }

    @Test
    fun warningHealthWhenHighLatency() {
        val snapshot = SpeechMetricsSnapshot(
            engineName = "Whisper",
            totalTranscriptions = 10,
            successRate = 90f,
            avgLatencyMs = 3000
        )
        assertEquals(SpeechMetricsSnapshot.HealthStatus.WARNING, snapshot.healthStatus)
    }

    @Test
    fun criticalHealthWhenLowSuccess() {
        val snapshot = SpeechMetricsSnapshot(
            engineName = "Whisper",
            totalTranscriptions = 10,
            successRate = 30f,
            avgLatencyMs = 1000
        )
        assertEquals(SpeechMetricsSnapshot.HealthStatus.CRITICAL, snapshot.healthStatus)
    }

    @Test
    fun criticalHealthWhenVeryHighLatency() {
        val snapshot = SpeechMetricsSnapshot(
            engineName = "Whisper",
            totalTranscriptions = 10,
            successRate = 90f,
            avgLatencyMs = 6000
        )
        assertEquals(SpeechMetricsSnapshot.HealthStatus.CRITICAL, snapshot.healthStatus)
    }

    @Test
    fun toSnapshotFromPerformance() {
        val perf = WhisperPerformance()
        perf.recordInitialization(1200, WhisperModelSize.BASE, 1)
        perf.recordTranscription(3000, 800, 42, 3, 0.87f)
        perf.recordTranscription(2000, 600, 30, 2, 0.92f)

        val ts = 1708800000000L
        val snapshot = perf.toSnapshot("Whisper", "READY", timestampMs = ts)
        assertEquals("Whisper", snapshot.engineName)
        assertEquals("BASE", snapshot.modelSize)
        assertEquals(1200L, snapshot.initTimeMs)
        assertEquals(2, snapshot.totalTranscriptions)
        assertEquals(100f, snapshot.successRate) // 2/2 non-empty = 100%
        assertTrue(snapshot.avgLatencyMs > 0)
        assertTrue(snapshot.avgRTF > 0f)
        assertTrue(snapshot.avgConfidence > 0f)
        assertEquals(800L, snapshot.peakLatencyMs)
        assertEquals("READY", snapshot.engineState)
        assertEquals(ts, snapshot.timestampMs)
    }

    @Test
    fun toSnapshotWithEmptyPerformance() {
        val perf = WhisperPerformance()
        val snapshot = perf.toSnapshot("GoogleCloud", "UNINITIALIZED")
        assertEquals("GoogleCloud", snapshot.engineName)
        assertNull(snapshot.modelSize)
        assertEquals(0, snapshot.totalTranscriptions)
        assertEquals(0f, snapshot.successRate)
        assertEquals(SpeechMetricsSnapshot.HealthStatus.IDLE, snapshot.healthStatus)
    }

    @Test
    fun successRateCalculation() {
        val perf = WhisperPerformance()
        perf.recordInitialization(500, WhisperModelSize.TINY_EN, 1)
        perf.recordTranscription(1000, 300, 20, 1, 0.9f)
        perf.recordEmptyTranscription(500, 100)
        perf.recordTranscription(2000, 500, 35, 2, 0.85f)

        val snapshot = perf.toSnapshot("Whisper", "LISTENING")
        // 3 total, 1 empty → success = (3-1)/3 * 100 = 66.67%
        assertTrue(snapshot.successRate > 66f)
        assertTrue(snapshot.successRate < 67f)
    }
}
