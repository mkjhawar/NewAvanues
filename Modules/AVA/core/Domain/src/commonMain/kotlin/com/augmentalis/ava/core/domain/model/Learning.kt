package com.augmentalis.ava.core.domain.model

/**
 * Domain model for learning events
 * Tracks user feedback and corrections for continuous improvement
 */
data class Learning(
    val id: String,
    val decisionId: String,
    val feedbackType: FeedbackType,
    val userCorrection: Map<String, String>? = null,
    val timestamp: Long,
    val outcome: Outcome,
    val notes: String? = null
)

enum class FeedbackType {
    POSITIVE,
    NEGATIVE,
    CORRECTION
}

enum class Outcome {
    SUCCESS,
    FAILURE,
    PARTIAL
}
