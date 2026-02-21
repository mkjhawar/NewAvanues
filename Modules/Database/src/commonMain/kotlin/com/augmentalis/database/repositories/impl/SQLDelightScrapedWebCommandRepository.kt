// Author: Manoj Jhawar

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.ScrapedWebCommandDTO
import com.augmentalis.database.dto.toScrapedWebCommandDTO
import com.augmentalis.database.repositories.IScrapedWebCommandRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SQLDelightScrapedWebCommandRepository(
    private val database: VoiceOSDatabase
) : IScrapedWebCommandRepository {

    private val queries = database.scrapedWebCommandQueries

    override suspend fun insert(command: ScrapedWebCommandDTO): Long = withContext(Dispatchers.Default) {
        database.transactionWithResult {
            queries.insertCommand(
                element_hash = command.elementHash,
                domain_id = command.domainId,
                url_pattern = command.urlPattern,
                css_selector = command.cssSelector,
                xpath = command.xpath,
                command_text = command.commandText,
                element_text = command.elementText,
                element_tag = command.elementTag,
                element_type = command.elementType,
                allowed_actions = command.allowedActionsJson(),
                primary_action = command.primaryAction,
                confidence = command.confidence.toDouble(),
                created_at = command.createdAt,
                last_verified = command.lastVerified,
                bound_left = command.boundLeft?.toLong(),
                bound_top = command.boundTop?.toLong(),
                bound_width = command.boundWidth?.toLong(),
                bound_height = command.boundHeight?.toLong()
            )
            queries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun insertBatch(commands: List<ScrapedWebCommandDTO>) = withContext(Dispatchers.Default) {
        database.transaction {
            for (cmd in commands) {
                queries.insertCommand(
                    element_hash = cmd.elementHash,
                    domain_id = cmd.domainId,
                    url_pattern = cmd.urlPattern,
                    css_selector = cmd.cssSelector,
                    xpath = cmd.xpath,
                    command_text = cmd.commandText,
                    element_text = cmd.elementText,
                    element_tag = cmd.elementTag,
                    element_type = cmd.elementType,
                    allowed_actions = cmd.allowedActionsJson(),
                    primary_action = cmd.primaryAction,
                    confidence = cmd.confidence.toDouble(),
                    created_at = cmd.createdAt,
                    last_verified = cmd.lastVerified,
                    bound_left = cmd.boundLeft?.toLong(),
                    bound_top = cmd.boundTop?.toLong(),
                    bound_width = cmd.boundWidth?.toLong(),
                    bound_height = cmd.boundHeight?.toLong()
                )
            }
        }
    }

    override suspend fun getById(id: Long): ScrapedWebCommandDTO? = withContext(Dispatchers.Default) {
        queries.selectById(id).executeAsOneOrNull()?.toScrapedWebCommandDTO()
    }

    override suspend fun getByDomain(domainId: String): List<ScrapedWebCommandDTO> = withContext(Dispatchers.Default) {
        queries.selectByDomain(domainId).executeAsList().map { it.toScrapedWebCommandDTO() }
    }

    override suspend fun getByDomainAndUrl(domainId: String, url: String): List<ScrapedWebCommandDTO> = withContext(Dispatchers.Default) {
        queries.selectByDomainAndUrl(domainId, url).executeAsList().map { it.toScrapedWebCommandDTO() }
    }

    override suspend fun getByElementHash(elementHash: String, domainId: String): List<ScrapedWebCommandDTO> = withContext(Dispatchers.Default) {
        queries.selectByElementHash(elementHash, domainId).executeAsList().map { it.toScrapedWebCommandDTO() }
    }

    override suspend fun getHighConfidence(domainId: String, minConfidence: Float): List<ScrapedWebCommandDTO> = withContext(Dispatchers.Default) {
        queries.selectHighConfidence(domainId, minConfidence.toDouble()).executeAsList().map { it.toScrapedWebCommandDTO() }
    }

    override suspend fun getUserApproved(domainId: String): List<ScrapedWebCommandDTO> = withContext(Dispatchers.Default) {
        queries.selectUserApproved(domainId).executeAsList().map { it.toScrapedWebCommandDTO() }
    }

    override suspend fun getMostUsed(domainId: String, limit: Int): List<ScrapedWebCommandDTO> = withContext(Dispatchers.Default) {
        queries.selectMostUsed(domainId, limit.toLong()).executeAsList().map { it.toScrapedWebCommandDTO() }
    }

    override suspend fun countByDomain(domainId: String): Long = withContext(Dispatchers.Default) {
        queries.countByDomain(domainId).executeAsOne()
    }

    override suspend fun updateConfidence(id: Long, confidence: Float, lastVerified: Long) = withContext(Dispatchers.Default) {
        queries.updateConfidence(confidence.toDouble(), lastVerified, id)
    }

    override suspend fun markApproved(id: Long, approvedAt: Long) = withContext(Dispatchers.Default) {
        queries.markApproved(approvedAt, id)
    }

    override suspend fun updateSynonyms(id: Long, synonyms: List<String>) = withContext(Dispatchers.Default) {
        val json = Json.encodeToString(synonyms)
        queries.addSynonym(json, id)
    }

    override suspend fun incrementUsage(id: Long, lastUsed: Long) = withContext(Dispatchers.Default) {
        queries.incrementUsage(lastUsed, id)
    }

    override suspend fun incrementUsageByHash(elementHash: String, domainId: String, lastUsed: Long) = withContext(Dispatchers.Default) {
        queries.incrementUsageByHash(lastUsed, elementHash, domainId)
    }

    override suspend fun markVerified(elementHash: String, domainId: String, lastVerified: Long) = withContext(Dispatchers.Default) {
        queries.markVerified(lastVerified, elementHash, domainId)
    }

    override suspend fun markDeprecated(domainId: String, olderThan: Long) = withContext(Dispatchers.Default) {
        queries.markDeprecated(domainId, olderThan)
    }

    override suspend fun deleteDeprecated(olderThan: Long) = withContext(Dispatchers.Default) {
        queries.deleteDeprecated(olderThan)
    }

    override suspend fun deleteByDomain(domainId: String) = withContext(Dispatchers.Default) {
        queries.deleteByDomain(domainId)
    }

    override suspend fun vacuum(olderThan: Long) = withContext(Dispatchers.Default) {
        queries.vacuumTable(olderThan)
    }
}
