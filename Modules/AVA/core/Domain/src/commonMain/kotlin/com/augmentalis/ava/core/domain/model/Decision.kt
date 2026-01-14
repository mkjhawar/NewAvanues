package com.augmentalis.ava.core.domain.model

/**
 * Domain model for decision logging
 * Tracks AVA's decision-making process for transparency
 */
data class Decision(
    val id: String,
    val conversationId: String,
    val decisionType: DecisionType,
    val inputData: Map<String, String>,
    val outputData: Map<String, String>,
    val confidence: Float,
    val timestamp: Long,
    val reasoning: String? = null
)

enum class DecisionType {
    INTENT_CLASSIFICATION,
    ACTION_SELECTION,
    RESPONSE_GENERATION,
    CONTEXT_RETRIEVAL,
    MEMORY_RECALL,
    /** Fallback for unknown/corrupted decision types (Issue 1.3) */
    UNKNOWN
}
