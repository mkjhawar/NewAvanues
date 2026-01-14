package com.augmentalis.voiceoscore.commands

import com.augmentalis.database.dto.ElementCommandDTO
import com.augmentalis.database.dto.QualityMetricDTO
import com.augmentalis.database.repositories.IElementCommandRepository
import com.augmentalis.database.repositories.IQualityMetricRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Business logic layer for managing element commands.
 *
 * Responsibilities:
 * - Cache frequently accessed commands in memory
 * - Validate command phrases (3-50 chars, alphanumeric + spaces)
 * - Manage synonyms for elements
 * - Calculate quality scores (0-100%)
 * - Batch operations for performance
 */
class ElementCommandManager(
    private val repository: IElementCommandRepository,
    private val qualityRepository: IQualityMetricRepository
) {
    // Command cache: elementUuid -> List<ElementCommandDTO>
    private val commandCache = ConcurrentHashMap<String, List<ElementCommandDTO>>()

    // Quality metrics cache: elementUuid -> QualityMetricDTO
    private val qualityCache = ConcurrentHashMap<String, QualityMetricDTO>()

    private val cacheMutex = Mutex()

    companion object {
        private const val MIN_PHRASE_LENGTH = 3
        private const val MAX_PHRASE_LENGTH = 50
        private val PHRASE_REGEX = Regex("[a-zA-Z0-9 ]+")

        // Quality score weights
        private const val WEIGHT_TEXT = 30
        private const val WEIGHT_CONTENT_DESC = 35
        private const val WEIGHT_RESOURCE_ID = 35

        // Quality score thresholds
        private const val EXCELLENT_THRESHOLD = 80
        private const val GOOD_THRESHOLD = 60
        private const val ACCEPTABLE_THRESHOLD = 40
    }

    /**
     * Assign a new voice command to an element.
     *
     * @param elementUuid Unique identifier for the element
     * @param phrase Voice command phrase (3-50 chars, alphanumeric + spaces)
     * @param appId Application package name
     * @return Result containing command ID on success, or error on failure
     */
    suspend fun assignCommand(
        elementUuid: String,
        phrase: String,
        appId: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Validate phrase
            val validationResult = validatePhrase(phrase)
            if (!validationResult.isSuccess) {
                return@withContext validationResult
            }

            // Check for duplicate phrase in this app
            val existingCommand = repository.getByPhrase(phrase, appId)
            if (existingCommand != null && existingCommand.elementUuid != elementUuid) {
                return@withContext Result.failure(
                    IllegalArgumentException("Command '$phrase' is already assigned to another element")
                )
            }

            // Create command DTO
            val command = ElementCommandDTO(
                elementUuid = elementUuid,
                commandPhrase = phrase.trim(),
                confidence = 1.0,
                createdAt = System.currentTimeMillis(),
                createdBy = "user",
                isSynonym = false,
                appId = appId
            )

            // Insert into database
            val commandId = repository.insert(command)

            // Update cache
            invalidateCommandCache(elementUuid)

            // Update quality metric (increment command count)
            updateQualityMetricCommandCount(elementUuid, appId)

            Result.success(commandId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add a synonym for an existing element command.
     *
     * @param elementUuid Element identifier
     * @param synonym Alternative phrase for the same element
     * @param appId Application package name
     * @return Result containing synonym command ID on success
     */
    suspend fun addSynonym(
        elementUuid: String,
        synonym: String,
        appId: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Validate phrase
            val validationResult = validatePhrase(synonym)
            if (!validationResult.isSuccess) {
                return@withContext validationResult
            }

            // Check if element has at least one primary command
            val existingCommands = repository.getByUuid(elementUuid)
            if (existingCommands.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("Cannot add synonym: Element has no primary command")
                )
            }

            // Check for duplicate phrase
            val existingCommand = repository.getByPhrase(synonym, appId)
            if (existingCommand != null) {
                return@withContext Result.failure(
                    IllegalArgumentException("Synonym '$synonym' is already in use")
                )
            }

            // Create synonym command
            val synonymCommand = ElementCommandDTO(
                elementUuid = elementUuid,
                commandPhrase = synonym.trim(),
                confidence = 1.0,
                createdAt = System.currentTimeMillis(),
                createdBy = "user",
                isSynonym = true,
                appId = appId
            )

            // Insert into database
            val commandId = repository.insert(synonymCommand)

            // Update cache
            invalidateCommandCache(elementUuid)

            // Update quality metric (increment command count)
            updateQualityMetricCommandCount(elementUuid, appId)

            Result.success(commandId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all commands (including synonyms) for an element.
     * Uses cache for performance.
     *
     * @param elementUuid Element identifier
     * @return List of commands (primary + synonyms)
     */
    suspend fun getCommands(elementUuid: String): List<ElementCommandDTO> = withContext(Dispatchers.IO) {
        // Check cache first
        commandCache[elementUuid]?.let { return@withContext it }

        // Fetch from database
        val commands = repository.getByUuid(elementUuid)

        // Update cache
        cacheMutex.withLock {
            commandCache[elementUuid] = commands
        }

        commands
    }

    /**
     * Delete a command by ID.
     *
     * @param commandId Database ID of the command to delete
     */
    suspend fun deleteCommand(commandId: Long) = withContext(Dispatchers.IO) {
        // Get command details before deletion (for cache invalidation)
        val allCommands = repository.getAll()
        val commandToDelete = allCommands.find { it.id == commandId }

        // Delete from database
        repository.delete(commandId)

        // Invalidate cache
        commandToDelete?.let {
            invalidateCommandCache(it.elementUuid)
            updateQualityMetricCommandCount(it.elementUuid, it.appId)
        }
    }

    /**
     * Calculate quality score for an element based on its metadata.
     *
     * Score breakdown:
     * - Has text: 30 points
     * - Has contentDescription: 35 points
     * - Has resourceId: 35 points
     *
     * @return Quality score (0-100)
     */
    fun calculateQualityScore(
        hasText: Boolean,
        hasContentDesc: Boolean,
        hasResourceId: Boolean
    ): Int {
        var score = 0
        if (hasText) score += WEIGHT_TEXT
        if (hasContentDesc) score += WEIGHT_CONTENT_DESC
        if (hasResourceId) score += WEIGHT_RESOURCE_ID
        return score
    }

    /**
     * Get quality level label based on score.
     */
    fun getQualityLevel(score: Int): String = when {
        score >= EXCELLENT_THRESHOLD -> "EXCELLENT"
        score >= GOOD_THRESHOLD -> "GOOD"
        score >= ACCEPTABLE_THRESHOLD -> "ACCEPTABLE"
        else -> "POOR"
    }

    /**
     * Get elements with poor quality that need manual commands.
     *
     * @param appId Application package name
     * @param maxScore Maximum quality score to filter (default 39 = POOR)
     * @return List of quality metrics for elements needing commands
     */
    suspend fun getElementsNeedingCommands(
        appId: String,
        maxScore: Int = ACCEPTABLE_THRESHOLD - 1
    ): List<QualityMetricDTO> = withContext(Dispatchers.IO) {
        qualityRepository.getByApp(appId)
            .filter { it.qualityScore <= maxScore }
            .sortedBy { it.qualityScore } // Worst quality first
    }

    /**
     * Get elements with no manual commands assigned.
     *
     * @param appId Application package name
     * @return List of quality metrics for elements without manual commands
     */
    suspend fun getElementsWithoutCommands(appId: String): List<QualityMetricDTO> =
        withContext(Dispatchers.IO) {
            qualityRepository.getByApp(appId)
                .filter { it.manualCommandCount == 0 }
                .sortedBy { it.qualityScore }
        }

    /**
     * Batch update quality metrics for multiple elements.
     * Used after app exploration.
     *
     * @param metrics List of quality metrics to insert/update
     */
    suspend fun batchUpdateQualityMetrics(metrics: List<QualityMetricDTO>) =
        withContext(Dispatchers.IO) {
            metrics.forEach { metric ->
                qualityRepository.insertOrUpdate(metric)

                // Update cache
                cacheMutex.withLock {
                    qualityCache[metric.elementUuid] = metric
                }
            }
        }

    /**
     * Get cached quality metric or fetch from database.
     *
     * @param elementUuid Element identifier
     * @return Quality metric or null if not found
     */
    suspend fun getQualityMetric(elementUuid: String): QualityMetricDTO? =
        withContext(Dispatchers.IO) {
            // Check cache first
            qualityCache[elementUuid]?.let { return@withContext it }

            // Fetch from database
            val metric = qualityRepository.getByUuid(elementUuid)

            // Update cache
            metric?.let {
                cacheMutex.withLock {
                    qualityCache[elementUuid] = it
                }
            }

            metric
        }

    /**
     * Clear all caches. Used for testing or memory management.
     */
    suspend fun clearCaches() {
        cacheMutex.withLock {
            commandCache.clear()
            qualityCache.clear()
        }
    }

    // Private helper methods

    private fun validatePhrase(phrase: String): Result<Long> {
        val trimmed = phrase.trim()

        return when {
            trimmed.isEmpty() -> Result.failure(
                IllegalArgumentException("Command phrase cannot be empty")
            )
            trimmed.length < MIN_PHRASE_LENGTH -> Result.failure(
                IllegalArgumentException("Command phrase must be at least $MIN_PHRASE_LENGTH characters")
            )
            trimmed.length > MAX_PHRASE_LENGTH -> Result.failure(
                IllegalArgumentException("Command phrase must be at most $MAX_PHRASE_LENGTH characters")
            )
            !trimmed.matches(PHRASE_REGEX) -> Result.failure(
                IllegalArgumentException("Command phrase can only contain letters, numbers, and spaces")
            )
            else -> Result.success(0L) // Dummy value, actual ID assigned later
        }
    }

    private suspend fun invalidateCommandCache(elementUuid: String) {
        cacheMutex.withLock {
            commandCache.remove(elementUuid)
        }
    }

    private suspend fun updateQualityMetricCommandCount(elementUuid: String, appId: String) {
        val commands = repository.getByUuid(elementUuid)
        val manualCommandCount = commands.count { it.createdBy == "user" }

        // Get existing metric or create new one
        val existingMetric = qualityRepository.getByUuid(elementUuid)

        if (existingMetric != null) {
            val updatedMetric = existingMetric.copy(
                manualCommandCount = manualCommandCount,
                commandCount = commands.size
            )
            qualityRepository.insertOrUpdate(updatedMetric)

            // Update cache
            cacheMutex.withLock {
                qualityCache[elementUuid] = updatedMetric
            }
        }
    }
}
