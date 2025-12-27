/**
 * ElementCommandManager.kt - Business logic for user-assigned element commands
 *
 * Part of Metadata Quality Overlay & Manual Command Assignment feature (VOS-META-001)
 * Created: 2025-12-03
 *
 * Manages user-assigned voice commands for UI elements that lack proper accessibility metadata.
 * Provides CRUD operations, validation, and integration with CommandProcessor.
 */
package com.augmentalis.voiceoscore.commands

import android.util.Log
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.ElementCommandDTO
import com.augmentalis.database.dto.QualityMetricDTO
import com.augmentalis.database.dto.QualityStatsDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Element Command Manager
 *
 * Manages user-assigned voice commands for specific UI elements.
 * Handles validation, synonym management, and integration with voice recognition.
 *
 * ## Usage:
 * ```kotlin
 * val manager = ElementCommandManager(databaseManager)
 *
 * // Add command
 * val id = manager.addCommand(
 *     elementUuid = "abc-123",
 *     commandPhrase = "submit button",
 *     appId = "com.example.app"
 * )
 *
 * // Find element by voice command
 * val uuid = manager.findElementByCommand("submit button", "com.example.app")
 *
 * // Get quality stats
 * val stats = manager.getQualityStats("com.example.app")
 * ```
 */
class ElementCommandManager(
    private val databaseManager: VoiceOSDatabaseManager
) {
    companion object {
        private const val TAG = "ElementCommandManager"
        private const val MIN_COMMAND_LENGTH = 3
        private const val MAX_COMMAND_LENGTH = 50

        // Profanity filter (basic list)
        private val PROFANITY_LIST = setOf<String>(
            // Add profanity words here if needed
        )
    }

    // Cached commands for fast lookup (appId -> commands)
    private val _commandCache = MutableStateFlow<Map<String, List<ElementCommandDTO>>>(emptyMap())
    val commandCache: StateFlow<Map<String, List<ElementCommandDTO>>> = _commandCache.asStateFlow()

    // Quality metrics cache (appId -> stats)
    private val _qualityStatsCache = MutableStateFlow<Map<String, QualityStatsDTO>>(emptyMap())
    val qualityStatsCache: StateFlow<Map<String, QualityStatsDTO>> = _qualityStatsCache.asStateFlow()

    /**
     * Add custom command for element
     *
     * @param elementUuid Element UUID from ThirdPartyUuidGenerator
     * @param commandPhrase User-spoken command (e.g., "submit button")
     * @param appId Package name
     * @param isSynonym true if adding synonym to existing command (auto-detected if false)
     * @return Command ID or -1 on error
     */
    suspend fun addCommand(
        elementUuid: String,
        commandPhrase: String,
        appId: String,
        isSynonym: Boolean = false
    ): Long {
        // Validate command phrase
        val sanitized = sanitizeCommandPhrase(commandPhrase)
        if (!isValidCommandPhrase(sanitized)) {
            Log.w(TAG, "Invalid command phrase: $commandPhrase (sanitized: $sanitized)")
            return -1L
        }

        // Check for duplicates
        val existing = databaseManager.elementCommands.getByPhrase(sanitized, appId)
        if (existing != null) {
            Log.w(TAG, "Command phrase already exists: $sanitized for app $appId")
            return -1L
        }

        // Determine if this is a synonym
        val hasPrimary = databaseManager.elementCommands.hasPrimaryCommand(elementUuid)
        val actualIsSynonym = isSynonym || hasPrimary

        // Create command
        val command = ElementCommandDTO(
            elementUuid = elementUuid,
            commandPhrase = sanitized,
            confidence = 1.0,
            createdAt = System.currentTimeMillis(),
            createdBy = "user",
            isSynonym = actualIsSynonym,
            appId = appId
        )

        val id = databaseManager.elementCommands.insert(command)

        if (id > 0) {
            Log.i(TAG, "Added element command: '$sanitized' for $elementUuid (synonym=$actualIsSynonym, appId=$appId)")

            // Update command count in quality metrics
            updateCommandCount(elementUuid, appId)

            // Refresh cache
            refreshCache(appId)
        } else {
            Log.e(TAG, "Failed to insert command: $sanitized")
        }

        return id
    }

    /**
     * Get all commands for element
     */
    suspend fun getCommandsForElement(elementUuid: String): List<ElementCommandDTO> {
        return databaseManager.elementCommands.getAllForElement(elementUuid)
    }

    /**
     * Get all commands for app
     */
    suspend fun getCommandsForApp(appId: String): List<ElementCommandDTO> {
        return databaseManager.elementCommands.getByApp(appId)
    }

    /**
     * Find element UUID by command phrase
     *
     * @param phrase User-spoken command
     * @param appId Package name
     * @return Element UUID or null if not found
     */
    suspend fun findElementByCommand(phrase: String, appId: String): String? {
        val sanitized = sanitizeCommandPhrase(phrase)
        val command = databaseManager.elementCommands.getByPhrase(sanitized, appId)
        return command?.elementUuid
    }

    /**
     * Delete command
     */
    suspend fun deleteCommand(commandId: Long, appId: String) {
        databaseManager.elementCommands.delete(commandId)
        refreshCache(appId)
        Log.i(TAG, "Deleted element command: $commandId")
    }

    /**
     * Delete all synonyms for element (keeps primary command)
     */
    suspend fun deleteSynonyms(elementUuid: String, appId: String) {
        databaseManager.elementCommands.deleteSynonyms(elementUuid)
        updateCommandCount(elementUuid, appId)
        refreshCache(appId)
        Log.i(TAG, "Deleted synonyms for: $elementUuid")
    }

    /**
     * Update command phrase
     */
    suspend fun updateCommand(commandId: Long, newPhrase: String, appId: String): Boolean {
        val sanitized = sanitizeCommandPhrase(newPhrase)
        if (!isValidCommandPhrase(sanitized)) {
            Log.w(TAG, "Invalid command phrase for update: $newPhrase")
            return false
        }

        // Check if new phrase already exists
        val existing = databaseManager.elementCommands.getByPhrase(sanitized, appId)
        if (existing != null && existing.id != commandId) {
            Log.w(TAG, "Command phrase already exists: $sanitized")
            return false
        }

        databaseManager.elementCommands.updateCommand(commandId, sanitized, 1.0)
        refreshCache(appId)
        Log.i(TAG, "Updated command $commandId to: $sanitized")
        return true
    }

    /**
     * Get quality metrics for app
     */
    suspend fun getQualityMetrics(appId: String): List<QualityMetricDTO> {
        return databaseManager.qualityMetrics.getByApp(appId)
    }

    /**
     * Get quality stats for app
     */
    suspend fun getQualityStats(appId: String): QualityStatsDTO? {
        return databaseManager.qualityMetrics.getQualityStats(appId)
    }

    /**
     * Get elements with poor quality (score < 40)
     */
    suspend fun getPoorQualityElements(appId: String): List<QualityMetricDTO> {
        return databaseManager.qualityMetrics.getPoorQualityElements(appId)
    }

    /**
     * Get elements needing commands (poor quality, no commands)
     */
    suspend fun getElementsNeedingCommands(appId: String): List<QualityMetricDTO> {
        return databaseManager.qualityMetrics.getElementsNeedingCommands(appId)
    }

    /**
     * Store quality metric for element
     */
    suspend fun storeQualityMetric(metric: QualityMetricDTO) {
        databaseManager.qualityMetrics.insertOrUpdate(metric)
        Log.d(TAG, "Stored quality metric for ${metric.elementUuid}: score=${metric.qualityScore}")
    }

    /**
     * Validate command phrase
     */
    private fun isValidCommandPhrase(phrase: String): Boolean {
        return phrase.length in MIN_COMMAND_LENGTH..MAX_COMMAND_LENGTH &&
               phrase.matches(Regex("[a-zA-Z0-9 ]+")) &&
               !isProfanity(phrase)
    }

    /**
     * Sanitize command phrase (lowercase, trim, collapse spaces)
     */
    private fun sanitizeCommandPhrase(phrase: String): String {
        return phrase.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Check for profanity
     */
    private fun isProfanity(phrase: String): Boolean {
        val words = phrase.split(" ")
        return words.any { it in PROFANITY_LIST }
    }

    /**
     * Update command count in quality metrics
     */
    private suspend fun updateCommandCount(elementUuid: String, appId: String) {
        val commands = getCommandsForElement(elementUuid)
        val manualCount = commands.size

        // Update quality metric
        databaseManager.qualityMetrics.updateCommandCounts(
            elementUuid = elementUuid,
            commandCount = manualCount, // For now, only manual commands
            manualCount = manualCount
        )

        Log.d(TAG, "Updated command count for $elementUuid: $manualCount commands")
    }

    /**
     * Refresh command cache for app
     */
    private suspend fun refreshCache(appId: String) {
        val commands = getCommandsForApp(appId)
        val currentCache = _commandCache.value.toMutableMap()
        currentCache[appId] = commands
        _commandCache.value = currentCache
        Log.d(TAG, "Refreshed command cache for $appId: ${commands.size} commands")

        // Also refresh quality stats
        refreshQualityStats(appId)
    }

    /**
     * Refresh quality stats cache
     */
    private suspend fun refreshQualityStats(appId: String) {
        val stats = getQualityStats(appId)
        if (stats != null) {
            val currentCache = _qualityStatsCache.value.toMutableMap()
            currentCache[appId] = stats
            _qualityStatsCache.value = currentCache
            Log.d(TAG, "Refreshed quality stats for $appId: ${stats.totalElements} elements")
        }
    }

    /**
     * Preload cache for app (call when app becomes active)
     */
    suspend fun preloadCache(appId: String) {
        refreshCache(appId)
        Log.i(TAG, "Preloaded cache for: $appId")
    }

    /**
     * Clear cache for app
     */
    fun clearCache(appId: String) {
        val currentCache = _commandCache.value.toMutableMap()
        currentCache.remove(appId)
        _commandCache.value = currentCache

        val currentStatsCache = _qualityStatsCache.value.toMutableMap()
        currentStatsCache.remove(appId)
        _qualityStatsCache.value = currentStatsCache

        Log.d(TAG, "Cleared cache for: $appId")
    }

    /**
     * Delete all commands and metrics for app
     */
    suspend fun deleteApp(appId: String) {
        databaseManager.elementCommands.deleteByApp(appId)
        databaseManager.qualityMetrics.deleteByApp(appId)
        clearCache(appId)
        Log.i(TAG, "Deleted all data for app: $appId")
    }
}
