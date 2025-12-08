package com.augmentalis.ava.core.domain.model

/**
 * Domain model for Teach-Ava training example
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
    val lastUsed: Long? = null
)

enum class TrainExampleSource {
    MANUAL,        // User manually added
    AUTO_LEARN,    // Automatically learned from successful interaction
    CORRECTION     // User corrected AVA's misunderstanding
}
