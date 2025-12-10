package com.augmentalis.ava.core.domain.model

/**
 * Domain model for long-term memory
 * Stores facts, preferences, and context for personalization
 */
data class Memory(
    val id: String,
    val memoryType: MemoryType,
    val content: String,
    val embedding: List<Float>? = null,
    val importance: Float,
    val createdAt: Long,
    val lastAccessed: Long,
    val accessCount: Int = 0,
    val metadata: Map<String, String>? = null
)

enum class MemoryType {
    FACT,
    PREFERENCE,
    CONTEXT,
    SKILL
}
