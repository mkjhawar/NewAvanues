/**
 * SQLDelightElementCommandRepository.kt - SQLDelight implementation of element command repository
 *
 * Part of Metadata Quality Overlay & Manual Command Assignment feature (VOS-META-001)
 * Created: 2025-12-03
 */
package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.ElementCommandDTO
import com.augmentalis.database.dto.QualityMetricDTO
import com.augmentalis.database.dto.QualityStatsDTO
import com.augmentalis.database.dto.toElementCommandDTO
import com.augmentalis.database.repositories.IElementCommandRepository
import com.augmentalis.database.repositories.IQualityMetricRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * SQLDelight implementation of IElementCommandRepository
 */
class SQLDelightElementCommandRepository(
    private val database: VoiceOSDatabase
) : IElementCommandRepository {

    private val queries = database.elementCommandQueries

    override suspend fun insert(command: ElementCommandDTO): Long = withContext(Dispatchers.Default) {
        try {
            queries.insertElementCommand(
                element_uuid = command.elementUuid,
                command_phrase = command.commandPhrase,
                confidence = command.confidence,
                created_at = command.createdAt,
                created_by = command.createdBy,
                is_synonym = if (command.isSynonym) 1L else 0L,
                app_id = command.appId
            )
            queries.lastInsertRowId().executeAsOne()
        } catch (e: Exception) {
            -1L
        }
    }

    override suspend fun getByUuid(elementUuid: String): List<ElementCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getCommandsByUuid(elementUuid)
                .executeAsList()
                .map { it.toElementCommandDTO() }
        }

    override suspend fun getByApp(appId: String): List<ElementCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getCommandsByApp(appId)
                .executeAsList()
                .map { it.toElementCommandDTO() }
        }

    override suspend fun getByPhrase(phrase: String, appId: String): ElementCommandDTO? =
        withContext(Dispatchers.Default) {
            queries.getCommandByPhrase(phrase, appId)
                .executeAsOneOrNull()
                ?.toElementCommandDTO()
        }

    override suspend fun delete(commandId: Long) = withContext(Dispatchers.Default) {
        queries.deleteCommand(commandId)
    }

    override suspend fun deleteSynonyms(elementUuid: String) = withContext(Dispatchers.Default) {
        queries.deleteCommandsBySynonym(elementUuid)
    }

    override suspend fun getAllForElement(elementUuid: String): List<ElementCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getAllCommandsForElement(elementUuid)
                .executeAsList()
                .map { it.toElementCommandDTO() }
        }

    override suspend fun hasPrimaryCommand(elementUuid: String): Boolean =
        withContext(Dispatchers.Default) {
            val count = queries.countPrimaryCommands(elementUuid).executeAsOne()
            count > 0
        }

    override suspend fun countCommands(elementUuid: String): Long =
        withContext(Dispatchers.Default) {
            queries.countCommandsByElement(elementUuid).executeAsOne()
        }

    override suspend fun updateCommand(commandId: Long, newPhrase: String, confidence: Double) =
        withContext(Dispatchers.Default) {
            queries.updateCommandPhrase(newPhrase, confidence, commandId)
        }

    override suspend fun deleteByApp(appId: String) = withContext(Dispatchers.Default) {
        queries.deleteCommandsByApp(appId)
    }

    override suspend fun getAll(): List<ElementCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getAllCommands()
                .executeAsList()
                .map { it.toElementCommandDTO() }
        }
}

/**
 * SQLDelight implementation of IQualityMetricRepository
 */
class SQLDelightQualityMetricRepository(
    private val database: VoiceOSDatabase
) : IQualityMetricRepository {

    private val queries = database.elementCommandQueries

    override suspend fun insertOrUpdate(metric: QualityMetricDTO) = withContext(Dispatchers.Default) {
        queries.insertQualityMetric(
            element_uuid = metric.elementUuid,
            app_id = metric.appId,
            quality_score = metric.qualityScore.toLong(),
            has_text = if (metric.hasText) 1L else 0L,
            has_content_desc = if (metric.hasContentDesc) 1L else 0L,
            has_resource_id = if (metric.hasResourceId) 1L else 0L,
            command_count = metric.commandCount.toLong(),
            manual_command_count = metric.manualCommandCount.toLong(),
            last_assessed = metric.lastAssessed
        )
    }

    override suspend fun getByApp(appId: String): List<QualityMetricDTO> =
        withContext(Dispatchers.Default) {
            queries.getQualityMetricsByApp(appId)
                .executeAsList()
                .map { it.toElementCommandDTO() }
        }

    override suspend fun getByUuid(elementUuid: String): QualityMetricDTO? =
        withContext(Dispatchers.Default) {
            queries.getQualityMetricByUuid(elementUuid)
                .executeAsOneOrNull()
                ?.toElementCommandDTO()
        }

    override suspend fun getPoorQualityElements(appId: String): List<QualityMetricDTO> =
        withContext(Dispatchers.Default) {
            queries.getPoorQualityElements(appId)
                .executeAsList()
                .map { it.toElementCommandDTO() }
        }

    override suspend fun getElementsWithoutCommands(appId: String): List<QualityMetricDTO> =
        withContext(Dispatchers.Default) {
            queries.getElementsWithoutCommands(appId)
                .executeAsList()
                .map { it.toElementCommandDTO() }
        }

    override suspend fun updateCommandCounts(
        elementUuid: String,
        commandCount: Int,
        manualCount: Int
    ) = withContext(Dispatchers.Default) {
        queries.updateCommandCounts(
            commandCount.toLong(),
            manualCount.toLong(),
            Clock.System.now().toEpochMilliseconds(),
            elementUuid
        )
    }

    override suspend fun getQualityStats(appId: String): QualityStatsDTO? =
        withContext(Dispatchers.Default) {
            val result = queries.getQualityStats(appId).executeAsOneOrNull()
            result?.let {
                // Return null if there are no elements for this app
                val totalElements = it.total_elements?.toInt() ?: 0
                if (totalElements == 0) {
                    return@withContext null
                }

                QualityStatsDTO(
                    appId = appId,
                    totalElements = totalElements,
                    excellentCount = it.excellent_count?.toInt() ?: 0,
                    goodCount = it.good_count?.toInt() ?: 0,
                    acceptableCount = it.acceptable_count?.toInt() ?: 0,
                    poorCount = it.poor_count?.toInt() ?: 0,
                    totalManualCommands = it.total_manual_commands?.toInt() ?: 0,
                    avgQualityScore = it.avg_quality_score ?: 0.0
                )
            }
        }

    override suspend fun deleteByApp(appId: String) = withContext(Dispatchers.Default) {
        queries.deleteQualityMetricsByApp(appId)
    }

    override suspend fun getElementsNeedingCommands(appId: String): List<QualityMetricDTO> =
        withContext(Dispatchers.Default) {
            queries.getElementsNeedingCommands(appId)
                .executeAsList()
                .map { it.toElementCommandDTO() }
        }
}
