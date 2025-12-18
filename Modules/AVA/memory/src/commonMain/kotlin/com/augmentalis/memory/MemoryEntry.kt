package com.augmentalis.memory

import kotlinx.datetime.Instant

/**
 * Represents a single memory entry in the AVA system.
 *
 * @property id Unique identifier for this memory
 * @property type Type of memory (short-term, episodic, semantic, etc.)
 * @property content The actual content/data of the memory
 * @property timestamp When this memory was created
 * @property importance Importance score (0.0 to 1.0) for memory decay/retention
 * @property metadata Additional contextual information
 * @property lastAccessed When this memory was last retrieved
 * @property accessCount How many times this memory has been accessed
 */
data class MemoryEntry(
    val id: String,
    val type: MemoryType,
    val content: String,
    val timestamp: Instant,
    val importance: Float = 0.5f,
    val metadata: Map<String, String> = emptyMap(),
    val lastAccessed: Instant? = null,
    val accessCount: Int = 0
) {
    init {
        require(importance in 0.0f..1.0f) {
            "Importance must be between 0.0 and 1.0"
        }
        require(accessCount >= 0) {
            "Access count cannot be negative"
        }
    }

    /**
     * Creates a copy of this memory with updated access information.
     */
    fun withAccess(accessTime: Instant): MemoryEntry {
        return copy(
            lastAccessed = accessTime,
            accessCount = accessCount + 1
        )
    }

    /**
     * Creates a copy of this memory with updated importance score.
     */
    fun withImportance(newImportance: Float): MemoryEntry {
        require(newImportance in 0.0f..1.0f) {
            "Importance must be between 0.0 and 1.0"
        }
        return copy(importance = newImportance)
    }
}
