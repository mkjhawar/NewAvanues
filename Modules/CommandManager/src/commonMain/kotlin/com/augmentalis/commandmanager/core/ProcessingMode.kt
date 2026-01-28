package com.augmentalis.commandmanager

/**
 * Defines how the VoiceOS accessibility engine processes UI elements.
 *
 * The processing mode determines the trade-off between responsiveness and thoroughness:
 * - [IMMEDIATE]: Optimized for real-time interaction with instant feedback
 * - [BATCH]: Optimized for comprehensive exploration and analysis
 *
 * @property description Human-readable description of the processing mode
 * @property maxBatchSize Maximum number of elements to process in a single batch
 * @property timeoutMs Maximum time in milliseconds allowed for processing operations
 */
enum class ProcessingMode(
    val description: String,
    val maxBatchSize: Int,
    val timeoutMs: Long
) {
    /**
     * JIT (Just-In-Time) mode for real-time processing.
     *
     * Processes UI elements one at a time as they are encountered.
     * Optimized for:
     * - Voice command execution requiring immediate feedback
     * - Real-time navigation with instant responses
     * - Low-latency accessibility interactions
     *
     * Characteristics:
     * - Single element processing (maxBatchSize = 1)
     * - Fast timeout (100ms) for responsive UX
     * - Minimal memory footprint
     */
    IMMEDIATE(
        description = "JIT mode - process elements one at a time",
        maxBatchSize = 1,
        timeoutMs = 100L
    ),

    /**
     * Exploration mode for batch processing.
     *
     * Collects UI elements and processes them in batches for comprehensive analysis.
     * Optimized for:
     * - Screen exploration and discovery
     * - Learning app UI structure
     * - Building element hierarchies for voice navigation
     *
     * Characteristics:
     * - Large batch processing (maxBatchSize = 100)
     * - Extended timeout (5000ms) for thorough analysis
     * - Enables holistic screen understanding
     */
    BATCH(
        description = "Exploration mode - collect and process in batches",
        maxBatchSize = 100,
        timeoutMs = 5000L
    )
}
