/**
 * GeneratedWebCommandDao.kt - SQLDelight repository for web command operations
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnweb/GeneratedWebCommandDao.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 * Migrated to SQLDelight: 2025-12-17
 *
 * Repository for generated web command CRUD operations
 */

package com.augmentalis.voiceoscore.learnweb

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.web.GeneratedWebCommandQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generated Web Command Repository
 *
 * Repository for generated web command operations using SQLDelight.
 * Supports command retrieval, usage tracking, and synonym searches.
 *
 * @since 1.0.0
 */
class GeneratedWebCommandDao(private val database: VoiceOSDatabase) {

    private val queries: GeneratedWebCommandQueries = database.generatedWebCommandQueries

    /**
     * Insert or replace command
     *
     * @param command Command to insert
     * @return Row ID
     */
    suspend fun insert(command: GeneratedWebCommand): Long = withContext(Dispatchers.IO) {
        queries.insertGeneratedWebCommandAuto(
            website_url_hash = command.websiteUrlHash,
            element_hash = command.elementHash,
            command_text = command.commandText,
            synonyms = command.synonyms,
            action = command.action,
            xpath = command.xpath,
            generated_at = command.generatedAt,
            usage_count = command.usageCount.toLong(),
            last_used_at = command.lastUsedAt
        )
        queries.getByElementHash(command.elementHash).executeAsList().lastOrNull()?.id ?: 0L
    }

    /**
     * Insert multiple commands
     *
     * @param commands Commands to insert
     * @return List of row IDs
     */
    suspend fun insertAll(commands: List<GeneratedWebCommand>): List<Long> = withContext(Dispatchers.IO) {
        commands.map { insert(it) }
    }

    /**
     * Update command
     *
     * @param command Command to update
     */
    suspend fun update(command: GeneratedWebCommand) = insert(command)

    /**
     * Get all commands for a website
     *
     * @param websiteUrlHash Website URL hash
     * @return List of commands
     */
    suspend fun getByWebsiteUrlHash(websiteUrlHash: String): List<GeneratedWebCommand> = withContext(Dispatchers.IO) {
        queries.getByWebsiteUrlHash(websiteUrlHash).executeAsList().map { mapToGeneratedWebCommand(it) }
    }

    /**
     * Get commands for a URL
     *
     * @param url Normalized URL (without protocol, www)
     * @return List of commands for this URL
     */
    suspend fun getCommandsForUrl(url: String): List<GeneratedWebCommand> = withContext(Dispatchers.IO) {
        queries.getCommandsForUrl(url, url).executeAsList().map { mapToGeneratedWebCommand(it) }
    }

    /**
     * Get commands by element hash
     *
     * @param elementHash Element hash
     * @return List of commands
     */
    suspend fun getByElementHash(elementHash: String): List<GeneratedWebCommand> = withContext(Dispatchers.IO) {
        queries.getByElementHash(elementHash).executeAsList().map { mapToGeneratedWebCommand(it) }
    }

    /**
     * Search commands by text (includes synonyms)
     *
     * @param websiteUrlHash Website URL hash
     * @param searchText Search text (case-insensitive)
     * @return List of matching commands
     */
    suspend fun searchCommands(websiteUrlHash: String, searchText: String): List<GeneratedWebCommand> = withContext(Dispatchers.IO) {
        queries.searchCommands(websiteUrlHash, searchText, searchText).executeAsList().map { mapToGeneratedWebCommand(it) }
    }

    /**
     * Get commands by action type
     *
     * @param websiteUrlHash Website URL hash
     * @param action Action type (e.g., "CLICK", "SCROLL_TO")
     * @return List of commands
     */
    suspend fun getByAction(websiteUrlHash: String, action: String): List<GeneratedWebCommand> = withContext(Dispatchers.IO) {
        queries.getByAction(websiteUrlHash, action).executeAsList().map { mapToGeneratedWebCommand(it) }
    }

    /**
     * Update command usage
     *
     * @param commandId Command ID
     * @param usageCount New usage count
     * @param lastUsedAt Timestamp of last usage
     */
    suspend fun updateUsage(commandId: Long, usageCount: Int, lastUsedAt: Long) = withContext(Dispatchers.IO) {
        queries.updateUsage(
            id = commandId,
            usage_count = usageCount.toLong(),
            last_used_at = lastUsedAt
        )
    }

    /**
     * Increment command usage
     *
     * @param commandId Command ID
     * @param timestamp Current timestamp
     */
    suspend fun incrementUsage(commandId: Long, timestamp: Long) = withContext(Dispatchers.IO) {
        queries.incrementUsage(
            id = commandId,
            last_used_at = timestamp
        )
    }

    /**
     * Get most used commands for a website
     *
     * @param websiteUrlHash Website URL hash
     * @param limit Number of commands to return
     * @return List of most used commands
     */
    suspend fun getMostUsed(websiteUrlHash: String, limit: Int): List<GeneratedWebCommand> = withContext(Dispatchers.IO) {
        queries.getMostUsed(websiteUrlHash, limit.toLong()).executeAsList().map { mapToGeneratedWebCommand(it) }
    }

    /**
     * Get recently used commands for a website
     *
     * @param websiteUrlHash Website URL hash
     * @param limit Number of commands to return
     * @return List of recently used commands
     */
    suspend fun getRecentlyUsed(websiteUrlHash: String, limit: Int): List<GeneratedWebCommand> = withContext(Dispatchers.IO) {
        queries.getRecentlyUsed(websiteUrlHash, limit.toLong()).executeAsList().map { mapToGeneratedWebCommand(it) }
    }

    /**
     * Delete all commands for a website
     *
     * @param websiteUrlHash Website URL hash
     */
    suspend fun deleteByWebsiteUrlHash(websiteUrlHash: String) = withContext(Dispatchers.IO) {
        queries.deleteByWebsiteUrlHash(websiteUrlHash)
    }

    /**
     * Delete commands by element hash
     *
     * @param elementHash Element hash
     */
    suspend fun deleteByElementHash(elementHash: String) = withContext(Dispatchers.IO) {
        queries.deleteByElementHash(elementHash)
    }

    /**
     * Delete all commands
     */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        queries.deleteAll()
    }

    /**
     * Get all commands (for testing and registration)
     *
     * @return List of all commands
     */
    suspend fun getAllCommands(): List<GeneratedWebCommand> = withContext(Dispatchers.IO) {
        queries.getAllCommands().executeAsList().map { mapToGeneratedWebCommand(it) }
    }

    /**
     * Get command count for a website
     *
     * @param websiteUrlHash Website URL hash
     * @return Command count
     */
    suspend fun getCommandCount(websiteUrlHash: String): Int = withContext(Dispatchers.IO) {
        queries.getCommandCount(websiteUrlHash).executeAsOne().toInt()
    }

    /**
     * Get total command usage for a website
     *
     * @param websiteUrlHash Website URL hash
     * @return Total usage count
     */
    suspend fun getTotalUsage(websiteUrlHash: String): Int? = withContext(Dispatchers.IO) {
        queries.getTotalUsage(websiteUrlHash).executeAsOneOrNull()?.toInt()
    }

    /**
     * Map SQLDelight result to GeneratedWebCommand data class
     */
    private fun mapToGeneratedWebCommand(result: com.augmentalis.database.web.GetByWebsiteUrlHash): GeneratedWebCommand {
        return GeneratedWebCommand(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            commandText = result.command_text,
            synonyms = result.synonyms,
            action = result.action,
            xpath = result.xpath,
            generatedAt = result.generated_at,
            usageCount = result.usage_count.toInt(),
            lastUsedAt = result.last_used_at
        )
    }

    private fun mapToGeneratedWebCommand(result: com.augmentalis.database.web.GetCommandsForUrl): GeneratedWebCommand {
        return GeneratedWebCommand(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            commandText = result.command_text,
            synonyms = result.synonyms,
            action = result.action,
            xpath = result.xpath,
            generatedAt = result.generated_at,
            usageCount = result.usage_count.toInt(),
            lastUsedAt = result.last_used_at
        )
    }

    private fun mapToGeneratedWebCommand(result: com.augmentalis.database.web.GetByElementHash): GeneratedWebCommand {
        return GeneratedWebCommand(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            commandText = result.command_text,
            synonyms = result.synonyms,
            action = result.action,
            xpath = result.xpath,
            generatedAt = result.generated_at,
            usageCount = result.usage_count.toInt(),
            lastUsedAt = result.last_used_at
        )
    }

    private fun mapToGeneratedWebCommand(result: com.augmentalis.database.web.SearchCommands): GeneratedWebCommand {
        return GeneratedWebCommand(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            commandText = result.command_text,
            synonyms = result.synonyms,
            action = result.action,
            xpath = result.xpath,
            generatedAt = result.generated_at,
            usageCount = result.usage_count.toInt(),
            lastUsedAt = result.last_used_at
        )
    }

    private fun mapToGeneratedWebCommand(result: com.augmentalis.database.web.GetByAction): GeneratedWebCommand {
        return GeneratedWebCommand(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            commandText = result.command_text,
            synonyms = result.synonyms,
            action = result.action,
            xpath = result.xpath,
            generatedAt = result.generated_at,
            usageCount = result.usage_count.toInt(),
            lastUsedAt = result.last_used_at
        )
    }

    private fun mapToGeneratedWebCommand(result: com.augmentalis.database.web.GetMostUsed): GeneratedWebCommand {
        return GeneratedWebCommand(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            commandText = result.command_text,
            synonyms = result.synonyms,
            action = result.action,
            xpath = result.xpath,
            generatedAt = result.generated_at,
            usageCount = result.usage_count.toInt(),
            lastUsedAt = result.last_used_at
        )
    }

    private fun mapToGeneratedWebCommand(result: com.augmentalis.database.web.GetRecentlyUsed): GeneratedWebCommand {
        return GeneratedWebCommand(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            commandText = result.command_text,
            synonyms = result.synonyms,
            action = result.action,
            xpath = result.xpath,
            generatedAt = result.generated_at,
            usageCount = result.usage_count.toInt(),
            lastUsedAt = result.last_used_at
        )
    }

    private fun mapToGeneratedWebCommand(result: com.augmentalis.database.web.GetAllCommands): GeneratedWebCommand {
        return GeneratedWebCommand(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            commandText = result.command_text,
            synonyms = result.synonyms,
            action = result.action,
            xpath = result.xpath,
            generatedAt = result.generated_at,
            usageCount = result.usage_count.toInt(),
            lastUsedAt = result.last_used_at
        )
    }
}
