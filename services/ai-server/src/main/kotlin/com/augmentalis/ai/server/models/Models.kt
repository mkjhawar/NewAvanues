package com.augmentalis.ai.server.models

import kotlinx.serialization.Serializable

/**
 * Instruction category for classification
 */
@Serializable
enum class InstructionCategory {
    ARCHITECTURE,   // Tech stack, patterns, structure
    CONVENTIONS,    // Naming, formatting, style
    WORKFLOWS,      // Processes, CI/CD, branching
    DOMAIN,         // Business logic, terminology
    SECURITY,       // Auth, permissions, secrets
    TESTING         // Test patterns, coverage
}

/**
 * Rule type detected in instruction
 */
@Serializable
enum class RuleType {
    PREFERENCE,     // "always use X"
    PROHIBITION,    // "never use X"
    ALTERNATIVE,    // "use X or Y"
    MAPPING,        // "X should be Y"
    REQUIREMENT     // "must have X"
}

/**
 * NLU classification request
 */
@Serializable
data class ClassifyRequest(
    val text: String,
    val candidateCategories: List<String>? = null
)

/**
 * NLU classification response
 */
@Serializable
data class ClassifyResponse(
    val category: InstructionCategory,
    val confidence: Float,
    val ruleType: RuleType,
    val entities: List<String>,
    val allScores: Map<String, Float> = emptyMap(),
    val inferenceTimeMs: Long
)

/**
 * Entity extraction request
 */
@Serializable
data class ExtractEntitiesRequest(
    val text: String
)

/**
 * Entity extraction response
 */
@Serializable
data class ExtractEntitiesResponse(
    val entities: List<ExtractedEntity>,
    val inferenceTimeMs: Long
)

/**
 * Extracted entity with type
 */
@Serializable
data class ExtractedEntity(
    val text: String,
    val type: String,  // TECH, FRAMEWORK, PATTERN, etc.
    val confidence: Float
)

/**
 * Embedding computation request
 */
@Serializable
data class EmbeddingRequest(
    val text: String,
    val normalize: Boolean = true
)

/**
 * Embedding computation response
 */
@Serializable
data class EmbeddingResponse(
    val embedding: List<Float>,
    val dimension: Int,
    val model: String,
    val inferenceTimeMs: Long
)

/**
 * Similarity computation request
 */
@Serializable
data class SimilarityRequest(
    val text1: String,
    val text2: String
)

/**
 * Similarity computation response
 */
@Serializable
data class SimilarityResponse(
    val similarity: Float,
    val inferenceTimeMs: Long
)

/**
 * Instruction conversion request
 */
@Serializable
data class ConvertRequest(
    val input: String,
    val category: InstructionCategory? = null
)

/**
 * Instruction conversion response
 */
@Serializable
data class ConvertResponse(
    val original: String,
    val compact: String,
    val category: InstructionCategory,
    val ruleType: RuleType,
    val entities: List<String>,
    val confidence: Float,
    val inferenceTimeMs: Long
)

/**
 * Health check response
 */
@Serializable
data class HealthResponse(
    val status: String,
    val version: String,
    val models: Map<String, ModelStatus>,
    val uptime: Long
)

/**
 * Model status
 */
@Serializable
data class ModelStatus(
    val loaded: Boolean,
    val path: String?,
    val sizeBytes: Long?,
    val lastUsed: Long?
)
