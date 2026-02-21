package com.augmentalis.nlu.learning.domain

/**
 * Unified Learning Domain - Core abstractions for VoiceOS-AVA integration
 *
 * This module defines the shared contracts between VoiceOS (pattern-based)
 * and AVA (semantic/embedding-based) learning systems.
 *
 * Design Principles:
 * - SOLID: Single responsibility, interface segregation
 * - Loose coupling: Systems communicate via domain events
 * - Cross-platform: KMP commonMain for iOS/Android/Desktop
 *
 * @see ADR-014: Unified Learning Architecture
 * @author Manoj Jhawar
 * @since 2025-12-18
 */

// ==================== Value Objects ====================

/**
 * Source of a learned command/intent
 *
 * Tracks where a learned example originated for:
 * - Analytics (which source provides best learning?)
 * - Priority (user-confirmed > llm_auto > voiceos_scrape)
 * - Debugging (trace learning issues)
 */
enum class LearningSource(val priority: Int) {
    /** User explicitly taught via Teach AVA UI */
    USER_TAUGHT(100),

    /** User confirmed an LLM suggestion */
    USER_CONFIRMED(90),

    /** LLM classified with high confidence */
    LLM_AUTO(70),

    /** LLM-generated variation of a learned intent */
    LLM_VARIATION(60),

    /** VoiceOS scraped from UI with user approval */
    VOICEOS_APPROVED(85),

    /** VoiceOS scraped from UI (auto-generated) */
    VOICEOS_SCRAPE(50),

    /** VoiceOS JIT (just-in-time) learning */
    VOICEOS_JIT(55),

    /** Bundled with app (core intents) */
    BUNDLED(30),

    /** Unknown/legacy source */
    UNKNOWN(0);

    companion object {
        fun fromString(source: String): LearningSource {
            return entries.find { it.name.equals(source, ignoreCase = true) }
                ?: when (source.lowercase()) {
                    "user" -> USER_TAUGHT
                    "llm_auto" -> LLM_AUTO
                    "llm_variation" -> LLM_VARIATION
                    "llm_confirmed" -> USER_CONFIRMED
                    "voiceos_scrape", "voiceos" -> VOICEOS_SCRAPE
                    "voiceos_jit", "jit" -> VOICEOS_JIT
                    "core", "bundled" -> BUNDLED
                    else -> UNKNOWN
                }
        }
    }
}

/**
 * Type of learning action
 */
enum class LearningActionType {
    /** Click/tap action on UI element */
    CLICK,

    /** Long press action */
    LONG_CLICK,

    /** Scroll action */
    SCROLL,

    /** Text input action */
    TYPE,

    /** Intent classification (AVA) */
    INTENT,

    /** Navigation between screens */
    NAVIGATE,

    /** System action (volume, brightness) */
    SYSTEM,

    /** Unknown action type */
    UNKNOWN
}

/**
 * Learned command representation (Value Object)
 *
 * Unified data structure that can represent:
 * - VoiceOS generated commands (click like, tap home)
 * - AVA intents (greeting, set_timer, play_music)
 *
 * Immutable by design - create new instance for changes.
 */
data class LearnedCommand(
    /** Unique identifier (hash of utterance + intent) */
    val id: String,

    /** The text/utterance that triggers this command */
    val utterance: String,

    /** The intent/action identifier */
    val intent: String,

    /** Action type (click, intent, scroll, etc.) */
    val actionType: LearningActionType,

    /** Confidence score 0.0-1.0 */
    val confidence: Float,

    /** Where this command was learned from */
    val source: LearningSource,

    /** Locale/language code (e.g., en-US) */
    val locale: String = "en-US",

    /** Optional synonyms/variations */
    val synonyms: List<String> = emptyList(),

    /** Optional embedding vector (384 or 768 dimensions) */
    val embedding: FloatArray? = null,

    /** Timestamp when created */
    val createdAt: Long = 0L,

    /** Timestamp when last used */
    val lastUsedAt: Long? = null,

    /** Usage count for confidence adjustment */
    val usageCount: Int = 0,

    /** Whether user has approved/confirmed this command */
    val isUserApproved: Boolean = false,

    /** Optional package name (for VoiceOS element-specific commands) */
    val packageName: String? = null,

    /** Optional element hash (for VoiceOS UI element linking) */
    val elementHash: String? = null
) {
    /** Check if this command has an embedding computed */
    val hasEmbedding: Boolean get() = embedding != null && embedding.isNotEmpty()

    /** Check if this is a high-confidence command */
    val isHighConfidence: Boolean get() = confidence >= 0.85f

    /** Check if this came from VoiceOS */
    val isVoiceOSCommand: Boolean
        get() = source in listOf(
            LearningSource.VOICEOS_SCRAPE,
            LearningSource.VOICEOS_APPROVED,
            LearningSource.VOICEOS_JIT
        )

    /** Check if this is an AVA-native intent */
    val isAVAIntent: Boolean
        get() = source in listOf(
            LearningSource.USER_TAUGHT,
            LearningSource.USER_CONFIRMED,
            LearningSource.LLM_AUTO,
            LearningSource.LLM_VARIATION,
            LearningSource.BUNDLED
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as LearnedCommand

        if (id != other.id) return false
        if (utterance != other.utterance) return false
        if (intent != other.intent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + utterance.hashCode()
        result = 31 * result + intent.hashCode()
        return result
    }

    companion object {
        /** Generate ID from utterance and intent */
        fun generateId(utterance: String, intent: String): String {
            val input = "$intent:$utterance"
            return input.hashCode().toString(16)
        }

        /** Create from VoiceOS GeneratedCommandDTO */
        fun fromVoiceOS(
            commandText: String,
            actionType: String,
            confidence: Double,
            elementHash: String,
            packageName: String? = null,
            synonyms: List<String> = emptyList(),
            isUserApproved: Boolean = false,
            createdAt: Long = 0L,
            usageCount: Int = 0
        ): LearnedCommand {
            val action = when (actionType.lowercase()) {
                "click", "tap" -> LearningActionType.CLICK
                "long_click", "hold" -> LearningActionType.LONG_CLICK
                "scroll", "swipe" -> LearningActionType.SCROLL
                "type", "input" -> LearningActionType.TYPE
                "navigate" -> LearningActionType.NAVIGATE
                else -> LearningActionType.UNKNOWN
            }

            val intent = "${actionType.lowercase()}_${elementHash.take(8)}"

            return LearnedCommand(
                id = generateId(commandText, intent),
                utterance = commandText,
                intent = intent,
                actionType = action,
                confidence = confidence.toFloat(),
                source = if (isUserApproved) LearningSource.VOICEOS_APPROVED else LearningSource.VOICEOS_SCRAPE,
                synonyms = synonyms,
                createdAt = createdAt,
                usageCount = usageCount,
                isUserApproved = isUserApproved,
                packageName = packageName,
                elementHash = elementHash
            )
        }

        /** Create from AVA TrainExample */
        fun fromAVATrainExample(
            utterance: String,
            intent: String,
            confidence: Float,
            source: String,
            locale: String = "en-US",
            embedding: FloatArray? = null,
            createdAt: Long = 0L,
            usageCount: Int = 0,
            isConfirmed: Boolean = false
        ): LearnedCommand {
            return LearnedCommand(
                id = generateId(utterance, intent),
                utterance = utterance,
                intent = intent,
                actionType = LearningActionType.INTENT,
                confidence = confidence,
                source = LearningSource.fromString(source),
                locale = locale,
                embedding = embedding,
                createdAt = createdAt,
                usageCount = usageCount,
                isUserApproved = isConfirmed
            )
        }
    }
}

// ==================== Domain Events ====================

/**
 * Learning event - emitted when a command is learned
 *
 * Follows event-driven architecture for loose coupling:
 * - VoiceOS emits CommandLearned when scraping UI
 * - AVA emits CommandLearned when LLM teaches
 * - UnifiedLearningService consumes both
 */
sealed class LearningEvent {
    /** Timestamp when event was created */
    abstract val timestamp: Long

    /** Event source identifier */
    abstract val sourceSystem: String

    /**
     * New command learned event
     */
    data class CommandLearned(
        val command: LearnedCommand,
        override val timestamp: Long = currentTimeMillis(),
        override val sourceSystem: String = "unknown"
    ) : LearningEvent()

    /**
     * Command updated event (confidence changed, user approved, etc.)
     */
    data class CommandUpdated(
        val command: LearnedCommand,
        val previousConfidence: Float,
        val updateReason: String,
        override val timestamp: Long = currentTimeMillis(),
        override val sourceSystem: String = "unknown"
    ) : LearningEvent()

    /**
     * Command deleted event
     */
    data class CommandDeleted(
        val commandId: String,
        val reason: String,
        override val timestamp: Long = currentTimeMillis(),
        override val sourceSystem: String = "unknown"
    ) : LearningEvent()

    /**
     * Embedding computed event
     */
    data class EmbeddingComputed(
        val commandId: String,
        val embeddingDimension: Int,
        override val timestamp: Long = currentTimeMillis(),
        override val sourceSystem: String = "unknown"
    ) : LearningEvent()

    /**
     * Sync completed event
     */
    data class SyncCompleted(
        val syncedCount: Int,
        override val sourceSystem: String,
        val targetSystem: String,
        override val timestamp: Long = currentTimeMillis()
    ) : LearningEvent()
}

// ==================== Interfaces ====================

/**
 * Learning source interface
 *
 * Implemented by any system that can produce learned commands:
 * - VoiceOS LearnAppCore (scraping)
 * - AVA NLUSelfLearner (LLM teaching)
 * - User teaching UI
 */
interface ILearningSource {
    /** Unique identifier for this source */
    val sourceId: String

    /** Human-readable name */
    val sourceName: String

    /**
     * Get all unsynchronized commands
     *
     * @param limit Maximum number to return
     * @return List of commands not yet synced to unified system
     */
    suspend fun getUnsyncedCommands(limit: Int = 100): List<LearnedCommand>

    /**
     * Mark commands as synced
     *
     * @param commandIds List of command IDs that were synced
     */
    suspend fun markSynced(commandIds: List<String>)

    /**
     * Get command count
     */
    suspend fun getCommandCount(): Int

    /**
     * Register a listener for learning events
     */
    fun addLearningListener(listener: LearningEventListener)

    /**
     * Remove a learning event listener
     */
    fun removeLearningListener(listener: LearningEventListener)
}

/**
 * Learning consumer interface
 *
 * Implemented by systems that consume learned commands:
 * - AVA IntentClassifier (receives VoiceOS commands)
 * - VoiceOS CommandManager (receives AVA intents)
 */
interface ILearningConsumer {
    /** Unique identifier for this consumer */
    val consumerId: String

    /** Human-readable name */
    val consumerName: String

    /**
     * Receive a learned command
     *
     * @param command The command to learn
     * @return true if successfully consumed
     */
    suspend fun consume(command: LearnedCommand): Boolean

    /**
     * Receive multiple commands (batch operation)
     *
     * @param commands List of commands to learn
     * @return Number of successfully consumed commands
     */
    suspend fun consumeBatch(commands: List<LearnedCommand>): Int

    /**
     * Check if consumer can handle this command
     *
     * @param command The command to check
     * @return true if this consumer can process the command
     */
    fun canConsume(command: LearnedCommand): Boolean

    /**
     * Get the minimum confidence threshold for consuming
     */
    fun getMinConfidenceThreshold(): Float
}

/**
 * Learning event listener
 */
fun interface LearningEventListener {
    fun onLearningEvent(event: LearningEvent)
}

/**
 * Unified learning repository interface
 *
 * Single source of truth for all learned commands across systems.
 */
interface IUnifiedLearningRepository {
    /**
     * Save a learned command
     */
    suspend fun save(command: LearnedCommand): Boolean

    /**
     * Save multiple commands (batch)
     */
    suspend fun saveBatch(commands: List<LearnedCommand>): Int

    /**
     * Find by utterance
     */
    suspend fun findByUtterance(utterance: String): LearnedCommand?

    /**
     * Find by intent
     */
    suspend fun findByIntent(intent: String): List<LearnedCommand>

    /**
     * Find by source
     */
    suspend fun findBySource(source: LearningSource): List<LearnedCommand>

    /**
     * Get all high-confidence commands
     */
    suspend fun getHighConfidence(minConfidence: Float = 0.8f): List<LearnedCommand>

    /**
     * Get commands needing embedding computation
     */
    suspend fun getCommandsWithoutEmbedding(limit: Int = 50): List<LearnedCommand>

    /**
     * Update embedding for a command
     */
    suspend fun updateEmbedding(commandId: String, embedding: FloatArray): Boolean

    /**
     * Update confidence for a command
     */
    suspend fun updateConfidence(commandId: String, newConfidence: Float): Boolean

    /**
     * Increment usage count
     */
    suspend fun incrementUsage(commandId: String): Boolean

    /**
     * Delete a command
     */
    suspend fun delete(commandId: String): Boolean

    /**
     * Get statistics
     */
    suspend fun getStats(): LearningStats
}

/**
 * Learning statistics
 */
data class LearningStats(
    val totalCommands: Int,
    val bySource: Map<LearningSource, Int>,
    val withEmbedding: Int,
    val withoutEmbedding: Int,
    val highConfidence: Int,
    val lowConfidence: Int,
    val userApproved: Int
)

// ==================== Platform Utilities ====================

/**
 * Get current time in milliseconds (platform-independent)
 */
expect fun currentTimeMillis(): Long
