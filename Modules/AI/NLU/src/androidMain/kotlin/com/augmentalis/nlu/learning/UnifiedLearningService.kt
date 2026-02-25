package com.augmentalis.nlu.learning

import com.augmentalis.nlu.nluLogDebug
import com.augmentalis.nlu.nluLogError
import com.augmentalis.nlu.nluLogInfo
import com.augmentalis.nlu.nluLogWarn
import com.augmentalis.nlu.IntentClassifier
import com.augmentalis.nlu.learning.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified Learning Service - Orchestrates learning between VoiceOS and AVA
 *
 * This service acts as the central hub for the learning pipeline:
 * 1. Receives commands from VoiceOS (via ILearningSource)
 * 2. Computes embeddings using AVA's IntentClassifier
 * 3. Stores in unified database
 * 4. Notifies consumers of new learned commands
 *
 * Design:
 * - Event-driven architecture with SharedFlow
 * - Loose coupling via interfaces
 * - Background processing with coroutines
 *
 * @see ADR-014: Unified Learning Architecture
 */
@Singleton
class UnifiedLearningService @Inject constructor(
    private val intentClassifier: IntentClassifier
) : ILearningConsumer, LearningEventListener {

    companion object {
        private const val TAG = "UnifiedLearningService"

        /** Minimum confidence to accept VoiceOS commands */
        const val VOICEOS_MIN_CONFIDENCE = 0.6f

        /** Minimum confidence to accept AVA intents */
        const val AVA_MIN_CONFIDENCE = 0.6f

        /** Batch size for embedding computation */
        const val EMBEDDING_BATCH_SIZE = 20
    }

    // Coroutine scope for background operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Event flow for learning events
    private val _events = MutableSharedFlow<LearningEvent>(replay = 10)
    val events: SharedFlow<LearningEvent> = _events.asSharedFlow()

    // Registered sources
    private val sources = mutableListOf<ILearningSource>()

    // Registered consumers
    private val consumers = mutableListOf<ILearningConsumer>()

    // ==================== ILearningConsumer Implementation ====================

    override val consumerId: String = "unified_learning_service"
    override val consumerName: String = "Unified Learning Service"

    override suspend fun consume(command: LearnedCommand): Boolean = withContext(Dispatchers.IO) {
        try {
            // PII-safe: log utterance length, not content
            nluLogDebug(TAG, "Consuming command: ${command.utterance.length}-char cmd -> ${command.intent}")

            // 1. Compute embedding if needed
            val commandWithEmbedding = if (!command.hasEmbedding) {
                computeEmbedding(command)
            } else {
                command
            }

            // 2. Save to AVA's NLU system
            val saved = saveToNLU(commandWithEmbedding)

            if (saved) {
                // 3. Emit event
                emitEvent(LearningEvent.CommandLearned(
                    command = commandWithEmbedding,
                    sourceSystem = command.source.name
                ))

                // 4. Notify other consumers
                notifyConsumers(commandWithEmbedding)

                nluLogInfo(TAG, "Successfully consumed: ${command.utterance.length}-char cmd -> ${command.intent}")
            }

            saved
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to consume command: ${e.message}", e)
            false
        }
    }

    override suspend fun consumeBatch(commands: List<LearnedCommand>): Int = withContext(Dispatchers.IO) {
        var successCount = 0

        commands.chunked(EMBEDDING_BATCH_SIZE).forEach { batch ->
            batch.forEach { command ->
                if (consume(command)) {
                    successCount++
                }
            }
        }

        nluLogInfo(TAG, "Batch consumed: $successCount/${commands.size} commands")
        successCount
    }

    override fun canConsume(command: LearnedCommand): Boolean {
        return command.confidence >= getMinConfidenceThreshold()
    }

    override fun getMinConfidenceThreshold(): Float {
        return if (true) AVA_MIN_CONFIDENCE else VOICEOS_MIN_CONFIDENCE
    }

    // ==================== LearningEventListener Implementation ====================

    override fun onLearningEvent(event: LearningEvent) {
        scope.launch {
            when (event) {
                is LearningEvent.CommandLearned -> {
                    nluLogDebug(TAG, "Received CommandLearned event from ${event.sourceSystem}")
                    consume(event.command)
                }
                is LearningEvent.CommandUpdated -> {
                    nluLogDebug(TAG, "Received CommandUpdated event: ${event.updateReason}")
                    // Forward to consumers
                    _events.emit(event)
                }
                is LearningEvent.CommandDeleted -> {
                    nluLogDebug(TAG, "Received CommandDeleted event: ${event.reason}")
                    _events.emit(event)
                }
                is LearningEvent.EmbeddingComputed -> {
                    nluLogDebug(TAG, "Received EmbeddingComputed event: dim=${event.embeddingDimension}")
                    _events.emit(event)
                }
                is LearningEvent.SyncCompleted -> {
                    nluLogInfo(TAG, "Sync completed: ${event.syncedCount} from ${event.sourceSystem} to ${event.targetSystem}")
                    _events.emit(event)
                }
            }
        }
    }

    // ==================== Source/Consumer Registration ====================

    /**
     * Register a learning source
     */
    fun registerSource(source: ILearningSource) {
        sources.add(source)
        source.addLearningListener(this)
        nluLogInfo(TAG, "Registered source: ${source.sourceName}")
    }

    /**
     * Unregister a learning source
     */
    fun unregisterSource(source: ILearningSource) {
        source.removeLearningListener(this)
        sources.remove(source)
        nluLogInfo(TAG, "Unregistered source: ${source.sourceName}")
    }

    /**
     * Register a learning consumer
     */
    fun registerConsumer(consumer: ILearningConsumer) {
        if (consumer !== this) { // Don't register self
            consumers.add(consumer)
            nluLogInfo(TAG, "Registered consumer: ${consumer.consumerName}")
        }
    }

    /**
     * Unregister a learning consumer
     */
    fun unregisterConsumer(consumer: ILearningConsumer) {
        consumers.remove(consumer)
        nluLogInfo(TAG, "Unregistered consumer: ${consumer.consumerName}")
    }

    // ==================== Sync Operations ====================

    /**
     * Sync commands from all registered sources
     *
     * @param limit Maximum commands to sync per source
     * @return Total number of synced commands
     */
    suspend fun syncFromAllSources(limit: Int = 100): Int = withContext(Dispatchers.IO) {
        var totalSynced = 0

        sources.forEach { source ->
            try {
                val synced = syncFromSource(source, limit)
                totalSynced += synced
            } catch (e: Exception) {
                nluLogError(TAG, "Failed to sync from ${source.sourceName}: ${e.message}", e)
            }
        }

        nluLogInfo(TAG, "Total synced from all sources: $totalSynced")
        totalSynced
    }

    /**
     * Sync commands from a specific source
     *
     * @param source The source to sync from
     * @param limit Maximum commands to sync
     * @return Number of synced commands
     */
    suspend fun syncFromSource(source: ILearningSource, limit: Int = 100): Int = withContext(Dispatchers.IO) {
        try {
            val commands = source.getUnsyncedCommands(limit)

            if (commands.isEmpty()) {
                nluLogDebug(TAG, "No unsynced commands from ${source.sourceName}")
                return@withContext 0
            }

            nluLogDebug(TAG, "Syncing ${commands.size} commands from ${source.sourceName}")

            val synced = consumeBatch(commands)

            if (synced > 0) {
                val syncedIds = commands.take(synced).map { it.id }
                source.markSynced(syncedIds)

                emitEvent(LearningEvent.SyncCompleted(
                    syncedCount = synced,
                    sourceSystem = source.sourceName,
                    targetSystem = "AVA NLU"
                ))
            }

            synced
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to sync from ${source.sourceName}: ${e.message}", e)
            0
        }
    }

    // ==================== VoiceOS-Specific Operations ====================

    /**
     * Learn from VoiceOS generated command
     *
     * Converts VoiceOS command format to unified format and processes.
     *
     * @param commandText The voice command text (e.g., "click like")
     * @param actionType The action type (e.g., "click", "scroll")
     * @param elementHash The UI element hash
     * @param confidence The confidence score
     * @param packageName Optional app package name
     * @param synonyms Optional command synonyms
     * @return true if successfully learned
     */
    suspend fun learnFromVoiceOS(
        commandText: String,
        actionType: String,
        elementHash: String,
        confidence: Float,
        packageName: String? = null,
        synonyms: List<String> = emptyList()
    ): Boolean {
        if (confidence < VOICEOS_MIN_CONFIDENCE) {
            nluLogDebug(TAG, "VoiceOS command confidence too low: $confidence < $VOICEOS_MIN_CONFIDENCE")
            return false
        }

        val command = LearnedCommand.fromVoiceOS(
            commandText = commandText,
            actionType = actionType,
            confidence = confidence.toDouble(),
            elementHash = elementHash,
            packageName = packageName,
            synonyms = synonyms,
            createdAt = currentTimeMillis()
        )

        return consume(command)
    }

    /**
     * Map VoiceOS action type to semantic intent
     *
     * Creates a meaningful intent name that can be understood by AVA.
     */
    private fun mapToSemanticIntent(actionType: String, elementHash: String): String {
        val baseAction = when (actionType.lowercase()) {
            "click", "tap" -> "tap"
            "long_click", "hold" -> "hold"
            "scroll", "swipe" -> "scroll"
            "type", "input" -> "type"
            else -> actionType.lowercase()
        }
        return "${baseAction}_element_${elementHash.take(8)}"
    }

    // ==================== Private Helpers ====================

    /**
     * Compute embedding for a command
     */
    private suspend fun computeEmbedding(command: LearnedCommand): LearnedCommand {
        return try {
            val embedding = intentClassifier.computeEmbedding(command.utterance)
            if (embedding != null) {
                nluLogDebug(TAG, "Computed embedding for ${command.utterance.length}-char cmd: ${embedding.size} dims")
                command.copy(embedding = embedding)
            } else {
                nluLogWarn(TAG, "Failed to compute embedding for ${command.utterance.length}-char cmd")
                command
            }
        } catch (e: Exception) {
            nluLogError(TAG, "Error computing embedding: ${e.message}", e)
            command
        }
    }

    /**
     * Save command to AVA's NLU system
     */
    private suspend fun saveToNLU(command: LearnedCommand): Boolean {
        return try {
            if (command.embedding != null) {
                intentClassifier.saveTrainedEmbedding(
                    utterance = command.utterance,
                    intent = command.intent,
                    embedding = command.embedding,
                    source = command.source.name.lowercase(),
                    confidence = command.confidence
                )
            } else {
                // Schedule background computation
                nluLogDebug(TAG, "Command has no embedding, skipping NLU save: ${command.utterance.length}-char cmd")
                false
            }
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to save to NLU: ${e.message}", e)
            false
        }
    }

    /**
     * Notify all registered consumers
     */
    private suspend fun notifyConsumers(command: LearnedCommand) {
        consumers.forEach { consumer ->
            try {
                if (consumer.canConsume(command)) {
                    consumer.consume(command)
                }
            } catch (e: Exception) {
                nluLogError(TAG, "Consumer ${consumer.consumerName} failed: ${e.message}", e)
            }
        }
    }

    /**
     * Emit a learning event
     */
    private suspend fun emitEvent(event: LearningEvent) {
        _events.emit(event)
    }

    // ==================== Statistics ====================

    /**
     * Get learning statistics
     */
    suspend fun getStats(): UnifiedLearningStats = withContext(Dispatchers.IO) {
        try {
            val nluStats = intentClassifier.getLearningStats()

            UnifiedLearningStats(
                totalCommands = nluStats.total,
                fromVoiceOS = nluStats.voiceosCommands,
                fromLLM = nluStats.llmAuto + nluStats.llmVariation,
                fromUser = nluStats.user,
                withEmbedding = nluStats.withEmbedding,
                registeredSources = sources.size,
                registeredConsumers = consumers.size
            )
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to get stats: ${e.message}", e)
            UnifiedLearningStats(0, 0, 0, 0, 0, sources.size, consumers.size)
        }
    }

    /**
     * Unified learning statistics
     */
    data class UnifiedLearningStats(
        val totalCommands: Int,
        val fromVoiceOS: Int,
        val fromLLM: Int,
        val fromUser: Int,
        val withEmbedding: Int,
        val registeredSources: Int,
        val registeredConsumers: Int
    ) {
        val voiceosPercent: Int
            get() = if (totalCommands > 0) (fromVoiceOS * 100) / totalCommands else 0

        val llmPercent: Int
            get() = if (totalCommands > 0) (fromLLM * 100) / totalCommands else 0
    }
}
