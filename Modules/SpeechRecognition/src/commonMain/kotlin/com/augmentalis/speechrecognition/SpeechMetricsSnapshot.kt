/**
 * SpeechMetricsSnapshot.kt - Immutable metrics snapshot for speech engine monitoring
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Platform-agnostic data class capturing a point-in-time snapshot of speech engine
 * performance. Used by the Cockpit dashboard card and available for persistence
 * Used by the Cockpit dashboard card for real-time engine monitoring.
 */
package com.augmentalis.speechrecognition

/**
 * Immutable snapshot of speech engine performance metrics.
 *
 * Captures all key indicators at a point in time:
 * - Engine identity (name, model, state)
 * - Latency and throughput (avg/peak latency, RTF)
 * - Accuracy (confidence, success rate)
 * - Volume (total transcriptions, audio processed)
 * - Language detection state
 *
 * Note: This is a plain data class (no kotlinx.serialization) to avoid adding
 * serialization as a dependency to the SpeechRecognition module. Cockpit can
 * serialize it manually if needed for persistence.
 */
data class SpeechMetricsSnapshot(
    val engineName: String,
    val modelSize: String? = null,
    val initTimeMs: Long = 0,
    val totalTranscriptions: Int = 0,
    val successRate: Float = 0f,
    val avgLatencyMs: Long = 0,
    val avgRTF: Float = 0f,
    val avgConfidence: Float = 0f,
    val peakLatencyMs: Long = 0,
    val peakRTF: Float = 0f,
    val totalAudioProcessedMs: Long = 0,
    val detectedLanguage: String? = null,
    val engineState: String = "UNKNOWN",
    val timestampMs: Long = 0
) {
    /**
     * Health status derived from success rate and latency.
     * - GOOD: success rate >= 80% and avg latency < 2000ms
     * - WARNING: success rate >= 50% or avg latency < 5000ms
     * - CRITICAL: success rate < 50% or avg latency >= 5000ms
     * - IDLE: no transcriptions yet
     */
    val healthStatus: HealthStatus get() = when {
        totalTranscriptions == 0 -> HealthStatus.IDLE
        successRate >= 80f && avgLatencyMs < 2000 -> HealthStatus.GOOD
        successRate >= 50f && avgLatencyMs < 5000 -> HealthStatus.WARNING
        else -> HealthStatus.CRITICAL
    }

    enum class HealthStatus { GOOD, WARNING, CRITICAL, IDLE }
}
