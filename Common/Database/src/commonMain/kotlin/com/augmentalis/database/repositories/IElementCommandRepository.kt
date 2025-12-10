/**
 * IElementCommandRepository.kt - Repository interface for element commands
 *
 * Part of Metadata Quality Overlay & Manual Command Assignment feature (VOS-META-001)
 * Created: 2025-12-03
 */
package com.avanues.database.repositories

import com.avanues.database.dto.ElementCommandDTO
import com.avanues.database.dto.QualityMetricDTO
import com.avanues.database.dto.QualityStatsDTO

/**
 * Repository for managing user-assigned voice commands for UI elements
 */
interface IElementCommandRepository {

    /**
     * Insert new element command
     *
     * @param command Command to insert
     * @return Database ID of inserted command, or -1 on error
     */
    suspend fun insert(command: ElementCommandDTO): Long

    /**
     * Get all commands for a specific element
     *
     * @param elementUuid Element UUID
     * @return List of commands (primary + synonyms)
     */
    suspend fun getByUuid(elementUuid: String): List<ElementCommandDTO>

    /**
     * Get all commands for an app
     *
     * @param appId Package name
     * @return List of all commands for the app
     */
    suspend fun getByApp(appId: String): List<ElementCommandDTO>

    /**
     * Find command by phrase
     *
     * @param phrase Command phrase
     * @param appId Package name (commands are app-scoped)
     * @return Command if found, null otherwise
     */
    suspend fun getByPhrase(phrase: String, appId: String): ElementCommandDTO?

    /**
     * Delete command by ID
     *
     * @param commandId Command ID
     */
    suspend fun delete(commandId: Long)

    /**
     * Delete all synonyms for element (keeps primary command)
     *
     * @param elementUuid Element UUID
     */
    suspend fun deleteSynonyms(elementUuid: String)

    /**
     * Get all commands for element (ordered: primary first, then synonyms)
     *
     * @param elementUuid Element UUID
     * @return List of commands
     */
    suspend fun getAllForElement(elementUuid: String): List<ElementCommandDTO>

    /**
     * Check if element has a primary command
     *
     * @param elementUuid Element UUID
     * @return true if element has at least one non-synonym command
     */
    suspend fun hasPrimaryCommand(elementUuid: String): Boolean

    /**
     * Count commands for element
     *
     * @param elementUuid Element UUID
     * @return Number of commands
     */
    suspend fun countCommands(elementUuid: String): Long

    /**
     * Update command phrase and confidence
     *
     * @param commandId Command ID
     * @param newPhrase New phrase
     * @param confidence New confidence score
     */
    suspend fun updateCommand(commandId: Long, newPhrase: String, confidence: Double)

    /**
     * Delete all commands for app
     *
     * @param appId Package name
     */
    suspend fun deleteByApp(appId: String)

    /**
     * Get all commands (for export/backup)
     *
     * @return All commands in database
     */
    suspend fun getAll(): List<ElementCommandDTO>
}

/**
 * Repository for managing element quality metrics
 */
interface IQualityMetricRepository {

    /**
     * Insert or update quality metric
     *
     * @param metric Quality metric
     */
    suspend fun insertOrUpdate(metric: QualityMetricDTO)

    /**
     * Get quality metrics for app
     *
     * @param appId Package name
     * @return List of quality metrics
     */
    suspend fun getByApp(appId: String): List<QualityMetricDTO>

    /**
     * Get quality metric for specific element
     *
     * @param elementUuid Element UUID
     * @return Quality metric if found, null otherwise
     */
    suspend fun getByUuid(elementUuid: String): QualityMetricDTO?

    /**
     * Get elements with poor quality (score < 40)
     *
     * @param appId Package name
     * @return List of poor quality elements
     */
    suspend fun getPoorQualityElements(appId: String): List<QualityMetricDTO>

    /**
     * Get elements without voice commands
     *
     * @param appId Package name
     * @return List of elements needing commands
     */
    suspend fun getElementsWithoutCommands(appId: String): List<QualityMetricDTO>

    /**
     * Update command counts for element
     *
     * @param elementUuid Element UUID
     * @param commandCount Total command count
     * @param manualCount Manual command count
     */
    suspend fun updateCommandCounts(
        elementUuid: String,
        commandCount: Int,
        manualCount: Int
    )

    /**
     * Get quality statistics for app
     *
     * @param appId Package name
     * @return Quality statistics summary
     */
    suspend fun getQualityStats(appId: String): QualityStatsDTO?

    /**
     * Delete quality metrics for app
     *
     * @param appId Package name
     */
    suspend fun deleteByApp(appId: String)

    /**
     * Get elements that need commands (poor quality, no commands)
     *
     * @param appId Package name
     * @return List of elements needing attention
     */
    suspend fun getElementsNeedingCommands(appId: String): List<QualityMetricDTO>
}
