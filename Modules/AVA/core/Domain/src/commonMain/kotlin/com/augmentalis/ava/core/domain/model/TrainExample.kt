package com.augmentalis.ava.core.domain.model

/**
 * Domain model for Teach-Ava training example
 *
 * ADR-013: Extended for LLM-as-Teacher self-learning architecture
 */
data class TrainExample(
    val id: Long = 0,
    val exampleHash: String,
    val utterance: String,
    val intent: String,
    val locale: String = "en-US",
    val source: TrainExampleSource,
    val createdAt: Long,
    val usageCount: Int = 0,
    val lastUsed: Long? = null,
    // ADR-013: Self-learning fields
    val confidence: Float = 0.8f,
    val userConfirmed: Boolean = false,
    val timesMatched: Int = 0,
    val embeddingVector: List<Float>? = null,
    val embeddingDimension: Int = 384
)

/**
 * Source of training example with database value mapping.
 *
 * Issue 2.1 Fix: Added dbValue property for backwards-compatible database mapping.
 * Database uses lowercase snake_case values (llm_auto, user_taught, etc.)
 * while enum uses Kotlin naming convention (LLM_AUTO, USER_TAUGHT, etc.).
 */
enum class TrainExampleSource(val dbValue: String) {
    // Legacy values (pre-ADR-013)
    MANUAL("user_taught"),              // User manually added via Teach-AVA
    AUTO_LEARN("llm_auto"),             // Automatically learned from successful interaction
    CORRECTION("correction"),            // User corrected AVA's misunderstanding

    // ADR-013: LLM-as-Teacher self-learning values
    LLM_AUTO("llm_auto"),               // LLM auto-generated example
    LLM_VARIATION("llm_variation"),     // LLM-generated variation of existing
    LLM_CONFIRMED("llm_confirmed"),     // LLM example confirmed by user
    USER_TAUGHT("user_taught"),         // Explicit user teaching

    /** Fallback for unknown/corrupted source values (Issue 2.1) */
    UNKNOWN("unknown");

    companion object {
        /**
         * Parse database value to enum with graceful fallback.
         * Handles both enum names (MANUAL) and dbValues (user_taught).
         */
        fun fromDbValue(value: String): TrainExampleSource {
            // First try exact dbValue match
            entries.find { it.dbValue == value }?.let { return it }

            // Then try enum name match (backwards compatibility)
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                println("TrainExampleSource: Unknown source value '$value', defaulting to UNKNOWN")
                UNKNOWN
            }
        }
    }
}
