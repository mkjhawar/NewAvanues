/**
 * AdaptiveTimingManager.kt - Self-tuning voice pipeline timing
 *
 * Inspired by TCP congestion control: start aggressive (low delays),
 * observe signal quality, back off when issues occur, speed up when clean.
 * Uses Exponential Moving Average (EMA) smoothing for stable adaptation.
 *
 * Thread-safe for visibility via @Volatile fields. Compound read-modify-write
 * operations are intentionally NOT synchronized — rare lost updates are acceptable
 * for a convergent heuristic (values settle to optimal regardless of occasional
 * missed increments). If precise counting is ever required, add synchronization.
 *
 * Pure Kotlin, KMP-safe, no external dependencies.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore

import kotlin.concurrent.Volatile
import kotlin.math.max
import kotlin.math.min

/**
 * Singleton that tracks speech pipeline health signals and computes
 * optimal timing values using EMA smoothing.
 *
 * Signals in:
 *   commandSuccess(), commandDuplicate(), grammarCompiled(ms),
 *   confidenceValue(f), wakeWordHit(ms), wakeWordTimeout()
 *
 * Timing values out:
 *   getProcessingDelayMs(), getConfidenceFloor(), getScrollDebounceMs(),
 *   getSpeechUpdateDebounceMs(), getCommandWindowMs()
 */
object AdaptiveTimingManager {

    // ═══════════════════════════════════════════════════════════════════
    // Constants
    // ═══════════════════════════════════════════════════════════════════

    /** EMA smoothing factor — settles in ~15 samples */
    private const val ALPHA = 0.15f

    // Processing delay: time between recognition result and emission
    private const val PROCESSING_DELAY_START = 50L
    private const val PROCESSING_DELAY_MIN = 0L
    private const val PROCESSING_DELAY_MAX = 300L

    // Confidence floor: minimum confidence to accept a command
    private const val CONFIDENCE_FLOOR_DEFAULT = 0.45f
    private const val CONFIDENCE_FLOOR_MIN = 0.3f
    private const val CONFIDENCE_FLOOR_MAX = 0.7f

    // Scroll debounce: lighter than content debounce
    private const val SCROLL_DEBOUNCE_START = 200L
    private const val SCROLL_DEBOUNCE_MIN = 100L
    private const val SCROLL_DEBOUNCE_MAX = 500L

    // Speech update debounce: time to wait before grammar recompilation
    private const val SPEECH_UPDATE_DEBOUNCE_START = 200L
    private const val SPEECH_UPDATE_DEBOUNCE_MIN = 100L
    private const val SPEECH_UPDATE_DEBOUNCE_MAX = 500L

    // Command window: post-wake-word listening duration
    private const val COMMAND_WINDOW_START = 4000L
    private const val COMMAND_WINDOW_MIN = 2000L
    private const val COMMAND_WINDOW_MAX = 8000L

    // Duplicate detection window
    private const val DUPLICATE_WINDOW_MS = 500L

    // Success streak threshold for bonus decrease
    private const val SUCCESS_STREAK_THRESHOLD = 10

    // ═══════════════════════════════════════════════════════════════════
    // Adaptive values (hot path reads — @Volatile for visibility)
    // ═══════════════════════════════════════════════════════════════════

    @Volatile
    private var processingDelayMs: Long = PROCESSING_DELAY_START

    @Volatile
    private var confidenceFloor: Float = CONFIDENCE_FLOOR_DEFAULT

    @Volatile
    private var scrollDebounceMs: Long = SCROLL_DEBOUNCE_START

    @Volatile
    private var speechUpdateDebounceMs: Long = SPEECH_UPDATE_DEBOUNCE_START

    @Volatile
    private var commandWindowMs: Long = COMMAND_WINDOW_START

    // ═══════════════════════════════════════════════════════════════════
    // Signal tracking state
    // ═══════════════════════════════════════════════════════════════════

    @Volatile
    private var consecutiveSuccesses: Int = 0

    @Volatile
    private var lastCommandText: String = ""

    @Volatile
    private var lastCommandTimeMs: Long = 0

    @Volatile
    private var totalCommands: Long = 0

    @Volatile
    private var totalDuplicates: Long = 0

    @Volatile
    private var totalNearMisses: Long = 0

    @Volatile
    private var totalWakeWordHits: Long = 0

    @Volatile
    private var totalWakeWordTimeouts: Long = 0

    // ═══════════════════════════════════════════════════════════════════
    // Getters — read by pipeline components
    // ═══════════════════════════════════════════════════════════════════

    /** Processing delay between recognition and command emission (ms) */
    fun getProcessingDelayMs(): Long = processingDelayMs

    /** Minimum confidence to accept a command (0.0-1.0) */
    fun getConfidenceFloor(): Float = confidenceFloor

    /** Scroll event debounce (ms) — lighter than content debounce */
    fun getScrollDebounceMs(): Long = scrollDebounceMs

    /** Speech engine grammar update debounce (ms) */
    fun getSpeechUpdateDebounceMs(): Long = speechUpdateDebounceMs

    /** Wake word command window duration (ms) */
    fun getCommandWindowMs(): Long = commandWindowMs

    // ═══════════════════════════════════════════════════════════════════
    // Signal recording — called by pipeline components
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Record a successful command execution.
     * Effect: decreases processing delay (multiplicative decrease: *= 0.95)
     */
    fun recordCommandSuccess() {
        totalCommands++
        consecutiveSuccesses++

        // Multiplicative decrease — ramp down aggressively on success
        processingDelayMs = max(
            PROCESSING_DELAY_MIN,
            (processingDelayMs * 0.95).toLong()
        )

        // Bonus: success streak threshold — extra decrease
        if (consecutiveSuccesses >= SUCCESS_STREAK_THRESHOLD) {
            processingDelayMs = max(
                PROCESSING_DELAY_MIN,
                processingDelayMs - 10
            )
            consecutiveSuccesses = 0 // reset streak counter after bonus
        }
    }

    /**
     * Record a duplicate command (same text within 500ms window).
     * Effect: increases processing delay (additive increase: += 25ms)
     */
    fun recordCommandDuplicate() {
        totalDuplicates++
        consecutiveSuccesses = 0

        // Additive increase — conservative backoff
        processingDelayMs = min(
            PROCESSING_DELAY_MAX,
            processingDelayMs + 25
        )
    }

    /**
     * Record a confidence near-miss (between floor-0.05 and floor).
     * Effect: logs for tracking, no timing change.
     */
    @Suppress("UNUSED_PARAMETER") // confidence param reserved for future near-miss analytics
    fun recordConfidenceNearMiss(confidence: Float) {
        totalNearMisses++
        // Near-misses are tracked for metrics/dashboard; no timing adaptation.
        // If near-miss rate is high, the user should adjust confidenceFloor
        // via DeveloperSettings, not via auto-adaptation.
    }

    /**
     * Record grammar compilation time.
     * Effect: adapts speechUpdateDebounceMs via EMA of (compileTime * 0.8)
     */
    fun recordGrammarCompile(durationMs: Long) {
        val target = (durationMs * 0.8).toLong()
        speechUpdateDebounceMs = ema(speechUpdateDebounceMs, target)
            .coerceIn(SPEECH_UPDATE_DEBOUNCE_MIN, SPEECH_UPDATE_DEBOUNCE_MAX)
    }

    /**
     * Record a wake word command hit (user spoke a command within the window).
     * Effect: adapts commandWindowMs via EMA of (responseTime * 1.5)
     *
     * @param responseTimeMs time between wake word detection and command arrival
     */
    fun recordWakeWordHit(responseTimeMs: Long) {
        totalWakeWordHits++
        val target = (responseTimeMs * 1.5).toLong()
        commandWindowMs = ema(commandWindowMs, target)
            .coerceIn(COMMAND_WINDOW_MIN, COMMAND_WINDOW_MAX)
    }

    /**
     * Record wake word timeout (command window expired unused).
     * Effect: shrinks commandWindowMs (* 0.9) — user is slow, reduce waste.
     */
    fun recordWakeWordTimeout() {
        totalWakeWordTimeouts++
        commandWindowMs = max(
            COMMAND_WINDOW_MIN,
            (commandWindowMs * 0.9).toLong()
        )
    }

    /**
     * Check if the given text + timestamp constitutes a duplicate.
     * Returns true if same text within [DUPLICATE_WINDOW_MS] of last command.
     */
    fun isDuplicate(text: String, timestampMs: Long): Boolean {
        val isDup = text.equals(lastCommandText, ignoreCase = true) &&
            (timestampMs - lastCommandTimeMs) < DUPLICATE_WINDOW_MS
        lastCommandText = text
        lastCommandTimeMs = timestampMs
        return isDup
    }

    // ═══════════════════════════════════════════════════════════════════
    // Confidence floor management
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Set the confidence floor from DeveloperSettings.
     * This is the single source of truth — replaces the hardcoded 0.5f
     * in VoiceOSAccessibilityService and the 0.45f in engine config.
     */
    fun setConfidenceFloor(threshold: Float) {
        confidenceFloor = threshold.coerceIn(CONFIDENCE_FLOOR_MIN, CONFIDENCE_FLOOR_MAX)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Snapshot for metrics dashboard
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Immutable snapshot of all adaptive timing values and counters.
     */
    data class Snapshot(
        val processingDelayMs: Long,
        val confidenceFloor: Float,
        val scrollDebounceMs: Long,
        val speechUpdateDebounceMs: Long,
        val commandWindowMs: Long,
        val totalCommands: Long,
        val totalDuplicates: Long,
        val totalNearMisses: Long,
        val totalWakeWordHits: Long,
        val totalWakeWordTimeouts: Long,
        val consecutiveSuccesses: Int
    )

    /** Take an immutable snapshot of current state for display/logging */
    fun snapshot(): Snapshot = Snapshot(
        processingDelayMs = processingDelayMs,
        confidenceFloor = confidenceFloor,
        scrollDebounceMs = scrollDebounceMs,
        speechUpdateDebounceMs = speechUpdateDebounceMs,
        commandWindowMs = commandWindowMs,
        totalCommands = totalCommands,
        totalDuplicates = totalDuplicates,
        totalNearMisses = totalNearMisses,
        totalWakeWordHits = totalWakeWordHits,
        totalWakeWordTimeouts = totalWakeWordTimeouts,
        consecutiveSuccesses = consecutiveSuccesses
    )

    // ═══════════════════════════════════════════════════════════════════
    // Persistence round-trip
    // ═══════════════════════════════════════════════════════════════════

    /** Persistence key constants */
    object Keys {
        const val PROCESSING_DELAY = "adaptive_processing_delay_ms"
        const val SCROLL_DEBOUNCE = "adaptive_scroll_debounce_ms"
        const val SPEECH_UPDATE_DEBOUNCE = "adaptive_speech_update_debounce_ms"
        const val COMMAND_WINDOW = "adaptive_command_window_ms"
    }

    /**
     * Export current learned values as a map for DataStore persistence.
     */
    fun toPersistedMap(): Map<String, Long> = mapOf(
        Keys.PROCESSING_DELAY to processingDelayMs,
        Keys.SCROLL_DEBOUNCE to scrollDebounceMs,
        Keys.SPEECH_UPDATE_DEBOUNCE to speechUpdateDebounceMs,
        Keys.COMMAND_WINDOW to commandWindowMs
    )

    /**
     * Restore learned values from DataStore.
     * Values are clamped to valid ranges to handle corrupt/stale data.
     */
    fun applyPersistedValues(map: Map<String, Long>) {
        map[Keys.PROCESSING_DELAY]?.let {
            processingDelayMs = it.coerceIn(PROCESSING_DELAY_MIN, PROCESSING_DELAY_MAX)
        }
        map[Keys.SCROLL_DEBOUNCE]?.let {
            scrollDebounceMs = it.coerceIn(SCROLL_DEBOUNCE_MIN, SCROLL_DEBOUNCE_MAX)
        }
        map[Keys.SPEECH_UPDATE_DEBOUNCE]?.let {
            speechUpdateDebounceMs = it.coerceIn(SPEECH_UPDATE_DEBOUNCE_MIN, SPEECH_UPDATE_DEBOUNCE_MAX)
        }
        map[Keys.COMMAND_WINDOW]?.let {
            commandWindowMs = it.coerceIn(COMMAND_WINDOW_MIN, COMMAND_WINDOW_MAX)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Reset
    // ═══════════════════════════════════════════════════════════════════

    /** Reset all adaptive values to defaults. */
    fun reset() {
        processingDelayMs = PROCESSING_DELAY_START
        confidenceFloor = CONFIDENCE_FLOOR_DEFAULT
        scrollDebounceMs = SCROLL_DEBOUNCE_START
        speechUpdateDebounceMs = SPEECH_UPDATE_DEBOUNCE_START
        commandWindowMs = COMMAND_WINDOW_START
        consecutiveSuccesses = 0
        lastCommandText = ""
        lastCommandTimeMs = 0
        totalCommands = 0
        totalDuplicates = 0
        totalNearMisses = 0
        totalWakeWordHits = 0
        totalWakeWordTimeouts = 0
    }

    // ═══════════════════════════════════════════════════════════════════
    // Internal: EMA calculation
    // ═══════════════════════════════════════════════════════════════════

    /** Exponential Moving Average: newValue = alpha * sample + (1-alpha) * current */
    private fun ema(current: Long, sample: Long): Long {
        return (ALPHA * sample + (1 - ALPHA) * current).toLong()
    }
}
