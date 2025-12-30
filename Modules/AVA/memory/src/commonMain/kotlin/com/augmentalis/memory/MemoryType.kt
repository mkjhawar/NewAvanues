package com.augmentalis.memory

/**
 * Types of memory in the AVA system, inspired by human cognition.
 */
enum class MemoryType {
    /**
     * Short-term conversation memory - current context window
     */
    SHORT_TERM,

    /**
     * Long-term user preferences and settings
     */
    LONG_TERM_PREFERENCES,

    /**
     * Episodic memory - past conversations and events
     */
    EPISODIC,

    /**
     * Semantic memory - facts and knowledge about the user
     */
    SEMANTIC
}
