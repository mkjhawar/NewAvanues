package com.augmentalis.commandmanager

// currentTimeMillis is defined in ISpeechEngine.kt

/**
 * ExplorationState represents the lifecycle states of a LearnApp exploration session.
 *
 * The state machine flow:
 * ```
 * IDLE → INITIALIZING → SCANNING ↔ PROCESSING → LEARNING
 *   ↑         ↓            ↓↑                       ↓
 *   └──── ERROR ←──────────┴───────────────────────┘
 *         ↓
 *   PAUSED → SCANNING | COMPLETING
 *              ↓
 *         COMPLETING → COMPLETED → IDLE (restart)
 * ```
 *
 * Phase 14: LearnApp System - Exploration State Management
 */
enum class ExplorationState {
    /** No exploration active */
    IDLE,

    /** Setting up exploration */
    INITIALIZING,

    /** Scanning UI elements */
    SCANNING,

    /** Processing scanned elements */
    PROCESSING,

    /** Learning commands for elements */
    LEARNING,

    /** Exploration paused */
    PAUSED,

    /** Wrapping up exploration */
    COMPLETING,

    /** Exploration finished */
    COMPLETED,

    /** Error state */
    ERROR
}

/**
 * ExplorationSession tracks the context, progress, and state transitions of a LearnApp exploration.
 *
 * @property sessionId Unique identifier for this exploration session
 * @property packageName The Android package name being explored
 * @property startTime The timestamp when the session started (milliseconds since epoch)
 * @property state The current exploration state
 * @property elementsScanned Count of UI elements that have been scanned
 * @property elementsLearned Count of UI elements that have learned commands
 * @property errorMessage Optional error message if in ERROR state
 * @property endTime The timestamp when the session ended (milliseconds since epoch), null if not ended
 *
 * Phase 14: LearnApp System - Exploration State Management
 */
data class ExplorationSession(
    val sessionId: String,
    val packageName: String,
    val startTime: Long = com.augmentalis.commandmanager.currentTimeMillis(),
    var state: ExplorationState = ExplorationState.IDLE,
    var elementsScanned: Int = 0,
    var elementsLearned: Int = 0,
    var errorMessage: String? = null,
    var endTime: Long? = null
) {
    companion object {
        /**
         * Returns the current system time in milliseconds since epoch.
         * Delegates to the features package function.
         */
        fun currentTimeMillis(): Long = com.augmentalis.commandmanager.currentTimeMillis()

        /**
         * Generates a unique session ID using timestamp and random component.
         *
         * Format: "session_{timestamp}_{random}"
         */
        fun generateSessionId(): String {
            return "session_${currentTimeMillis()}_${(0..9999).random()}"
        }
    }

    /**
     * Valid state transitions map.
     * Each state maps to the set of states it can transition to.
     */
    private val validTransitions: Map<ExplorationState, Set<ExplorationState>> = mapOf(
        ExplorationState.IDLE to setOf(ExplorationState.INITIALIZING),
        ExplorationState.INITIALIZING to setOf(ExplorationState.SCANNING, ExplorationState.ERROR),
        ExplorationState.SCANNING to setOf(ExplorationState.PROCESSING, ExplorationState.PAUSED, ExplorationState.ERROR),
        ExplorationState.PROCESSING to setOf(ExplorationState.LEARNING, ExplorationState.ERROR),
        ExplorationState.LEARNING to setOf(ExplorationState.SCANNING, ExplorationState.COMPLETING, ExplorationState.ERROR),
        ExplorationState.PAUSED to setOf(ExplorationState.SCANNING, ExplorationState.COMPLETING),
        ExplorationState.COMPLETING to setOf(ExplorationState.COMPLETED),
        ExplorationState.COMPLETED to setOf(ExplorationState.IDLE),  // Allow restart
        ExplorationState.ERROR to setOf(ExplorationState.IDLE)  // Allow recovery
    )

    /**
     * Attempts to transition to a new state.
     *
     * @param newState The target state to transition to
     * @return true if the transition was valid and performed, false if the transition is not allowed
     */
    fun transition(newState: ExplorationState): Boolean {
        val allowed = validTransitions[state]?.contains(newState) ?: false
        if (allowed) {
            state = newState
            if (newState == ExplorationState.COMPLETED || newState == ExplorationState.ERROR) {
                endTime = currentTimeMillis()
            }
        }
        return allowed
    }

    /**
     * Checks if the exploration is currently active (actively processing elements).
     *
     * Active states are: INITIALIZING, SCANNING, PROCESSING, LEARNING
     *
     * @return true if the exploration is actively processing
     */
    fun isActive(): Boolean = state in setOf(
        ExplorationState.INITIALIZING,
        ExplorationState.SCANNING,
        ExplorationState.PROCESSING,
        ExplorationState.LEARNING
    )

    /**
     * Checks if the exploration can be paused from the current state.
     *
     * Only SCANNING state can be paused.
     *
     * @return true if the exploration can be paused
     */
    fun canPause(): Boolean = state == ExplorationState.SCANNING

    /**
     * Gets the duration of the exploration session in milliseconds.
     *
     * @return The duration in milliseconds, or null if the session has not ended
     */
    fun getDuration(): Long? {
        return endTime?.let { it - startTime }
    }

    /**
     * Increments the count of scanned elements by 1.
     */
    fun incrementScanned() {
        elementsScanned++
    }

    /**
     * Increments the count of learned elements by 1.
     */
    fun incrementLearned() {
        elementsLearned++
    }

    /**
     * Sets an error message and transitions to the ERROR state.
     *
     * Note: If the current state cannot transition to ERROR, the errorMessage
     * will still be set but the state will remain unchanged.
     *
     * @param message The error message describing what went wrong
     */
    fun setError(message: String) {
        errorMessage = message
        transition(ExplorationState.ERROR)
    }
}
