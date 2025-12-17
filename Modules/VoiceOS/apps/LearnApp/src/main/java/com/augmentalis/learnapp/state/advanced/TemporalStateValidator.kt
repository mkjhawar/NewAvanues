/**
 * TemporalStateValidator.kt - Validates state duration and temporal patterns
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 *
 * Tracks state duration over time and applies temporal validation rules.
 * Reduces confidence for transient states (<500ms) and detects state
 * flicker patterns that indicate unstable detection.
 */
package com.augmentalis.learnapp.state.advanced

import com.augmentalis.learnapp.state.AppState

/**
 * State duration tracking entry
 */
data class StateDurationEntry(
    val state: AppState,
    val startTime: Long,
    var endTime: Long? = null,
    var duration: Long = 0L
) {
    fun updateDuration(currentTime: Long) {
        endTime = currentTime
        duration = currentTime - startTime
    }

    fun isTransient(thresholdMs: Long = 500L): Boolean {
        return duration < thresholdMs
    }
}

/**
 * State flicker detection result
 */
data class FlickerPattern(
    val states: List<AppState>,
    val occurrences: Int,
    val timeWindow: Long,
    val avgDuration: Long
)

/**
 * Temporal validation result
 */
data class TemporalValidationResult(
    val isValid: Boolean,
    val confidenceAdjustment: Float,  // Positive or negative adjustment
    val reason: String,
    val patterns: List<FlickerPattern> = emptyList()
)

/**
 * Validates state duration and detects temporal anomalies
 *
 * Maintains state duration history and applies temporal rules to
 * adjust confidence scores based on state stability and duration.
 */
class TemporalStateValidator(
    private val historyWindowMs: Long = 10_000L  // 10 second history window
) {

    companion object {
        private const val TAG = "TemporalStateValidator"

        // Temporal thresholds
        private const val TRANSIENT_THRESHOLD_MS = 500L
        private const val FLICKER_THRESHOLD_MS = 200L
        private const val STABLE_THRESHOLD_MS = 2000L

        // Confidence adjustments
        private const val PENALTY_TRANSIENT = 0.3f
        private const val PENALTY_FLICKER = 0.4f
        private const val BONUS_STABLE = 0.1f

        // Flicker detection parameters
        private const val MIN_FLICKER_OCCURRENCES = 3
        private const val FLICKER_WINDOW_MS = 5000L

        // Expected minimum durations for states (in milliseconds)
        private val STATE_MIN_DURATIONS = mapOf(
            AppState.LOADING to 300L,
            AppState.TUTORIAL to 1000L,
            AppState.PERMISSION to 500L,
            AppState.DIALOG to 500L,
            AppState.ERROR to 1000L,
            AppState.LOGIN to 500L
        )
    }

    // State duration history
    private val durationHistory = mutableListOf<StateDurationEntry>()

    // Current state tracking
    private var currentEntry: StateDurationEntry? = null

    /**
     * Track state change and update duration history
     *
     * @param state New state
     * @param timestamp Current timestamp
     */
    fun trackStateChange(state: AppState, timestamp: Long = System.currentTimeMillis()) {
        // Update current entry
        currentEntry?.let { entry ->
            entry.updateDuration(timestamp)
            durationHistory.add(entry)
        }

        // Start new entry
        currentEntry = StateDurationEntry(state, timestamp)

        // Clean old history
        cleanHistoryWindow(timestamp)
    }

    /**
     * Validate current state temporally
     *
     * @param state Current state
     * @param baseConfidence Base confidence score
     * @param timestamp Current timestamp
     * @return Validation result with confidence adjustment
     */
    fun validateState(
        state: AppState,
        baseConfidence: Float,
        timestamp: Long = System.currentTimeMillis()
    ): TemporalValidationResult {
        currentEntry?.updateDuration(timestamp)

        val currentDuration = currentEntry?.duration ?: 0L

        // Check for transient state
        if (currentDuration < TRANSIENT_THRESHOLD_MS && currentDuration > 0L) {
            return TemporalValidationResult(
                isValid = false,
                confidenceAdjustment = -PENALTY_TRANSIENT,
                reason = "Transient state: ${currentDuration}ms (threshold: ${TRANSIENT_THRESHOLD_MS}ms)"
            )
        }

        // Check for flicker patterns
        val flickerPatterns = detectFlickerPatterns(timestamp)
        if (flickerPatterns.isNotEmpty()) {
            return TemporalValidationResult(
                isValid = false,
                confidenceAdjustment = -PENALTY_FLICKER,
                reason = "Flicker detected: ${flickerPatterns.size} patterns",
                patterns = flickerPatterns
            )
        }

        // Check state-specific minimum duration
        val minDuration = STATE_MIN_DURATIONS[state] ?: 0L
        if (minDuration > 0L && currentDuration < minDuration) {
            val penalty = PENALTY_TRANSIENT * (1f - currentDuration.toFloat() / minDuration)
            return TemporalValidationResult(
                isValid = false,
                confidenceAdjustment = -penalty,
                reason = "$state below minimum duration: ${currentDuration}ms (min: ${minDuration}ms)"
            )
        }

        // Check for stable state (bonus)
        if (currentDuration >= STABLE_THRESHOLD_MS) {
            return TemporalValidationResult(
                isValid = true,
                confidenceAdjustment = BONUS_STABLE,
                reason = "Stable state: ${currentDuration}ms"
            )
        }

        // Default: valid but no adjustment
        return TemporalValidationResult(
            isValid = true,
            confidenceAdjustment = 0f,
            reason = "Normal duration: ${currentDuration}ms"
        )
    }

    /**
     * Detect state flicker patterns
     *
     * Flicker = rapid state changes (e.g., LOADING -> READY -> LOADING -> READY)
     *
     * @param currentTime Current timestamp
     * @return List of detected flicker patterns
     */
    fun detectFlickerPatterns(currentTime: Long = System.currentTimeMillis()): List<FlickerPattern> {
        val patterns = mutableListOf<FlickerPattern>()

        // Get recent history within flicker window
        val recentHistory = durationHistory.filter {
            currentTime - it.startTime <= FLICKER_WINDOW_MS
        }

        if (recentHistory.size < MIN_FLICKER_OCCURRENCES) {
            return patterns
        }

        // Group consecutive state changes
        val stateSequence = recentHistory.map { it.state }
        val stateChanges = mutableListOf<Pair<AppState, AppState>>()

        for (i in 0 until stateSequence.size - 1) {
            stateChanges.add(Pair(stateSequence[i], stateSequence[i + 1]))
        }

        // Count rapid back-and-forth changes
        val changePatterns = stateChanges.groupBy { it }.mapValues { it.value.size }

        changePatterns.forEach { (change, count) ->
            if (count >= MIN_FLICKER_OCCURRENCES) {
                // Check if reverse pattern also exists (A->B and B->A)
                val reverseChange = Pair(change.second, change.first)
                val reverseCount = changePatterns[reverseChange] ?: 0

                if (reverseCount >= MIN_FLICKER_OCCURRENCES) {
                    val relevantEntries = recentHistory.filter {
                        it.state == change.first || it.state == change.second
                    }

                    val avgDuration = relevantEntries.map { it.duration }.average().toLong()

                    patterns.add(
                        FlickerPattern(
                            states = listOf(change.first, change.second),
                            occurrences = count + reverseCount,
                            timeWindow = FLICKER_WINDOW_MS,
                            avgDuration = avgDuration
                        )
                    )
                }
            }
        }

        return patterns
    }

    /**
     * Get current state duration
     *
     * @return Duration in milliseconds
     */
    fun getCurrentStateDuration(): Long {
        currentEntry?.updateDuration(System.currentTimeMillis())
        return currentEntry?.duration ?: 0L
    }

    /**
     * Get state duration history
     *
     * @return List of historical duration entries
     */
    fun getDurationHistory(): List<StateDurationEntry> {
        return durationHistory.toList()
    }

    /**
     * Reset validator state
     */
    fun reset() {
        durationHistory.clear()
        currentEntry = null
    }

    /**
     * Clean history entries outside time window
     */
    private fun cleanHistoryWindow(currentTime: Long) {
        durationHistory.removeAll { entry ->
            currentTime - entry.startTime > historyWindowMs
        }
    }
}
