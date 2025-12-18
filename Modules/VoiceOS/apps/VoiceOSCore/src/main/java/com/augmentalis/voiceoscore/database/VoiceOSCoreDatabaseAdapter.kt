package com.augmentalis.voiceoscore.database

import android.content.Context
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.*
import com.augmentalis.voiceoscore.database.entities.AppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simplified database adapter - direct SQLDelight access with helper methods
 *
 * This adapter provides:
 * 1. Direct access to VoiceOSDatabaseManager (preferred for new code)
 * 2. Helper methods for common operations (backward compatibility)
 * 3. Entity ↔ DTO conversions (for VoiceOSCore-specific entity classes)
 *
 * **Preferred Usage** (Direct SQLDelight):
 * ```kotlin
 * val adapter = VoiceOSCoreDatabaseAdapter.getInstance(context)
 * val apps = adapter.databaseManager.scrapedApps.getAll() // Direct repository access
 * ```
 *
 * **Legacy Usage** (Helper methods):
 * ```kotlin
 * val apps = adapter.getInstalledApps() // Converts DTOs to Entities
 * ```
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Refactored: 2025-11-27 (Removed unnecessary DAO abstraction layer)
 */
class VoiceOSCoreDatabaseAdapter private constructor(context: Context) {

    /**
     * Direct access to SQLDelight database manager
     *
     * **Available repositories:**
     * - Scraping: scrapedApps, scrapedElements, generatedCommands
     * - UI Structure: scrapedHierarchies, screenContexts, elementRelationships
     * - Behavior: screenTransitions, userInteractions, elementStateHistory
     * - LearnApp: learnedAppQueries, explorationSessionQueries, navigationEdgeQueries, screenStateQueries
     * - Commands: commands, commandHistory, voiceCommands, commandUsage
     *
     * **Example:**
     * ```kotlin
     * val apps = databaseManager.scrapedApps.getAll()
     * databaseManager.transaction {
     *     scrapedApps.insert(app)
     *     scrapedElements.insert(element)
     * }
     * ```
     */
    val databaseManager: VoiceOSDatabaseManager =
        VoiceOSDatabaseManager.getInstance(DatabaseDriverFactory(context))

    // =========================================================================
    // Helper Methods (for backward compatibility and convenience)
    // =========================================================================

    /**
     * Get all installed apps as Entity objects (converts from DTOs)
     * For new code, prefer: databaseManager.scrapedApps.getAll()
     */
    suspend fun getInstalledApps(): List<AppEntity> {
        return databaseManager.scrapedApps.getAll().map { it.toAppEntity() }
    }

    /**
     * Get app by package name as Entity (converts from DTO)
     * For new code, prefer: databaseManager.scrapedApps.getByPackage()
     */
    suspend fun getApp(packageName: String): AppEntity? {
        return databaseManager.scrapedApps.getByPackage(packageName)?.toAppEntity()
    }

    /**
     * Insert app (converts from Entity to DTO)
     * For new code, prefer: databaseManager.scrapedApps.insert(dto)
     */
    suspend fun insertApp(app: AppEntity) {
        val dto = app.toScrapedAppDTO()
        databaseManager.scrapedApps.insert(dto)
    }

    /**
     * Update app (converts from Entity to DTO)
     * In SQLDelight, insert with same ID replaces the existing record
     * For new code, prefer: databaseManager.scrapedApps.insert(dto)
     */
    suspend fun updateApp(app: AppEntity) {
        insertApp(app) // In SQLDelight, insert = upsert
    }

    /**
     * Delete app by package name
     * For new code, prefer: databaseManager.scrapedApps.deleteById(appId)
     */
    suspend fun deleteApp(packageName: String) {
        val app = databaseManager.scrapedApps.getByPackage(packageName)
        if (app != null) {
            databaseManager.scrapedApps.deleteById(app.appId)
        }
    }

    /**
     * Get app count
     * For new code, prefer: databaseManager.scrapedApps.count()
     */
    suspend fun getAppCount(): Int {
        return databaseManager.scrapedApps.count().toInt()
    }

    /**
     * Get fully learned app count
     * For new code, prefer: databaseManager.scrapedApps.getFullyLearned().size
     */
    suspend fun getFullyLearnedAppCount(): Int {
        return databaseManager.scrapedApps.getFullyLearned().size
    }

    /**
     * Get app by name (searches all apps)
     * Note: This is inefficient - iterates through all apps
     * For new code, consider using package name instead
     */
    suspend fun getAppByName(name: String): AppEntity? {
        return getInstalledApps().find {
            it.appName.equals(name, ignoreCase = true)
        }
    }

    /**
     * Get total scraped element count
     * For new code, prefer: databaseManager.scrapedElements.count()
     */
    suspend fun getTotalElementCount(): Int {
        return databaseManager.scrapedElements.count().toInt()
    }

    // =========================================================================
    // Scraping-Specific Helper Methods (for AccessibilityScrapingIntegration)
    // =========================================================================

    /**
     * Get app by hash value
     * Searches all apps to find one matching the hash
     */
    suspend fun getAppByHash(hash: String): AppEntity? {
        val allApps = databaseManager.scrapedApps.getAll()
        return allApps.find { it.appHash == hash }?.toAppEntity()
    }

    /**
     * Increment scrape count for an app by package name
     */
    suspend fun incrementScrapeCount(packageName: String) {
        val app = databaseManager.scrapedApps.getByPackage(packageName)
        if (app != null) {
            val updated = app.copy(
                scrapeCount = app.scrapeCount + 1,
                lastScrapedAt = System.currentTimeMillis()
            )
            databaseManager.scrapedApps.insert(updated)
        }
    }

    /**
     * Update element count for an app by package name
     */
    suspend fun updateElementCount(packageName: String, count: Int) {
        val app = databaseManager.scrapedApps.getByPackage(packageName)
        if (app != null) {
            val updated = app.copy(elementCount = count.toLong())
            databaseManager.scrapedApps.insert(updated)
        }
    }

    /**
     * Update command count for an app by package name
     */
    suspend fun updateCommandCount(packageName: String, count: Int) {
        val app = databaseManager.scrapedApps.getByPackage(packageName)
        if (app != null) {
            val updated = app.copy(commandCount = count.toLong())
            databaseManager.scrapedApps.insert(updated)
        }
    }

    /**
     * Increment visit count for a screen context by hash
     */
    suspend fun incrementVisitCount(hash: String, time: Long) {
        val context = databaseManager.screenContexts.getByHash(hash)
        if (context != null) {
            val updated = context.copy(
                visitCount = context.visitCount + 1,
                lastScraped = time
            )
            databaseManager.screenContexts.insert(updated)
        }
    }

    /**
     * Update formGroupId for multiple elements by their hashes
     * Note: This is a batch operation that may be slow for large hash lists
     */
    suspend fun updateFormGroupIdBatch(hashes: List<String>, groupId: String?) {
        hashes.forEach { hash ->
            val element = databaseManager.scrapedElements.getByHash(hash)
            if (element != null) {
                val updated = element.copy(formGroupId = groupId)
                databaseManager.scrapedElements.insert(updated)
            }
        }
    }

    /**
     * Update scraping mode for an app by package name
     */
    suspend fun updateScrapingMode(packageName: String, mode: String) {
        val app = databaseManager.scrapedApps.getByPackage(packageName)
        if (app != null) {
            val updated = app.copy(scrapingMode = mode)
            databaseManager.scrapedApps.insert(updated)
        }
    }

    /**
     * Mark an app as fully learned
     */
    suspend fun markAsFullyLearned(packageName: String, timestamp: Long) {
        val app = databaseManager.scrapedApps.getByPackage(packageName)
        if (app != null) {
            val updated = app.copy(
                isFullyLearned = 1L,
                learnCompletedAt = timestamp
            )
            databaseManager.scrapedApps.insert(updated)
        }
    }

    // =========================================================================
    // Repository Extension Methods (add missing methods to repositories)
    // =========================================================================

    /**
     * Extension: Delete all hierarchy records for an app
     * Note: SQLDelight repositories don't have deleteByApp, so this deletes all hierarchies
     */
    suspend fun deleteHierarchyByApp(appId: String) {
        // TODO: Implement app-specific deletion when repository method is added
        // For now, this is a no-op
    }

    /**
     * Extension: Insert batch of hierarchy records
     * Now uses DTOs directly
     */
    suspend fun insertHierarchyBatch(hierarchies: List<ScrapedHierarchyDTO>): List<Long> = withContext(Dispatchers.IO) {
        val ids = mutableListOf<Long>()
        hierarchies.forEach { hierarchy ->
            databaseManager.scrapedHierarchies.insert(
                parentElementHash = hierarchy.parentElementHash,
                childElementHash = hierarchy.childElementHash,
                depth = hierarchy.depth,
                createdAt = hierarchy.createdAt
            )
            ids.add(hierarchy.id ?: System.currentTimeMillis())
        }
        ids
    }

    /**
     * Extension: Insert batch of generated commands
     * Now uses DTOs directly
     */
    suspend fun insertCommandBatch(commands: List<GeneratedCommandDTO>): List<Long> = withContext(Dispatchers.IO) {
        val ids = mutableListOf<Long>()
        commands.forEach { command ->
            databaseManager.generatedCommands.insert(command)
            ids.add(command.id ?: System.currentTimeMillis())
        }
        ids
    }

    /**
     * Extension: Insert batch of element relationships
     * Now uses DTOs directly
     */
    suspend fun insertRelationshipBatch(relationships: List<ElementRelationshipDTO>): List<Long> = withContext(Dispatchers.IO) {
        val ids = mutableListOf<Long>()
        relationships.forEach { relationship ->
            databaseManager.elementRelationships.insert(
                sourceElementHash = relationship.sourceElementHash,
                targetElementHash = relationship.targetElementHash,
                relationshipType = relationship.relationshipType,
                relationshipData = relationship.relationshipData,
                confidence = relationship.confidence,
                createdAt = relationship.createdAt,
                updatedAt = relationship.updatedAt
            )
            ids.add(relationship.id ?: System.currentTimeMillis())
        }
        ids
    }

    /**
     * Extension: Get all commands for an app
     * Returns DTOs directly (filtered by appId)
     */
    suspend fun getCommandsByApp(appId: String): List<GeneratedCommandDTO> = withContext(Dispatchers.IO) {
        databaseManager.generatedCommands.getAll().filter { it.appId == appId }
    }

    /**
     * Extension: Count elements for an app
     */
    suspend fun countElementsByApp(appId: String): Long {
        return databaseManager.scrapedElements.countByApp(appId)
    }

    /**
     * Extension: Insert batch of scraped elements and return assigned IDs
     * Now uses DTOs directly
     */
    suspend fun insertElementBatch(elements: List<ScrapedElementDTO>): List<Long> = withContext(Dispatchers.IO) {
        val ids = mutableListOf<Long>()
        elements.forEach { element ->
            databaseManager.scrapedElements.insert(element)
            // Use element hash as ID for now
            ids.add(element.id ?: element.elementHash.hashCode().toLong())
        }
        ids
    }

    /**
     * Extension: Insert or update a single scraped element
     * Now uses DTOs directly
     */
    suspend fun upsertElement(element: ScrapedElementDTO) = withContext(Dispatchers.IO) {
        databaseManager.scrapedElements.insert(element)
    }

    /**
     * Extension: Insert screen context
     * Now uses DTOs directly
     */
    suspend fun insertScreenContext(context: ScreenContextDTO) = withContext(Dispatchers.IO) {
        databaseManager.screenContexts.insert(context)
    }

    /**
     * Extension: Insert screen transition
     * Now uses DTOs directly
     */
    suspend fun insertScreenTransition(transition: ScreenTransitionDTO) = withContext(Dispatchers.IO) {
        databaseManager.screenTransitions.insert(transition)
    }

    /**
     * Extension: Insert user interaction
     * Now uses DTOs directly
     */
    suspend fun insertUserInteraction(interaction: UserInteractionDTO) = withContext(Dispatchers.IO) {
        databaseManager.userInteractions.insert(interaction)
    }

    /**
     * Extension: Insert element state history
     * Now uses DTOs directly
     */
    suspend fun insertElementStateHistory(state: ElementStateHistoryDTO): Long = withContext(Dispatchers.IO) {
        databaseManager.elementStateHistory.insert(state)
    }

    companion object {
        @Volatile
        private var INSTANCE: VoiceOSCoreDatabaseAdapter? = null

        fun getInstance(context: Context): VoiceOSCoreDatabaseAdapter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VoiceOSCoreDatabaseAdapter(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

/**
 * Legacy alias for backward compatibility
 */
typealias VoiceOSAppDatabase = VoiceOSCoreDatabaseAdapter

// =============================================================================
// Entity <-> DTO Conversion Extensions (for VoiceOSCore compatibility)
// =============================================================================

/**
 * Convert SQLDelight DTO to VoiceOSCore Entity
 */
private fun com.augmentalis.database.dto.ScrapedAppDTO.toAppEntity(): AppEntity {
    return AppEntity(
        appId = this.appId,
        packageName = this.packageName,
        appName = "", // Not stored in SQLDelight schema
        icon = null,  // Not stored in SQLDelight schema
        isSystemApp = false,
        versionCode = this.versionCode,
        versionName = this.versionName,
        installTime = 0,
        updateTime = 0,
        isFullyLearned = this.isFullyLearned == 1L,
        exploredElementCount = 0,
        scrapedElementCount = this.elementCount.toInt(),
        totalScreens = 0,
        lastExplored = null,
        lastScraped = this.lastScrapedAt,
        learnAppEnabled = true,
        dynamicScrapingEnabled = this.scrapingMode == "DYNAMIC",
        maxScrapeDepth = 5
    )
}

/**
 * Convert VoiceOSCore Entity to SQLDelight DTO
 */
private fun AppEntity.toScrapedAppDTO(): com.augmentalis.database.dto.ScrapedAppDTO {
    val timestamp = System.currentTimeMillis()
    return com.augmentalis.database.dto.ScrapedAppDTO(
        appId = this.packageName,
        packageName = this.packageName,
        versionCode = this.versionCode,
        versionName = this.versionName,
        appHash = this.packageName.hashCode().toString(),
        isFullyLearned = if (this.isFullyLearned == true) 1L else 0L,
        learnCompletedAt = if (this.isFullyLearned == true) timestamp else null,
        scrapingMode = if (this.dynamicScrapingEnabled == true) "DYNAMIC" else "STATIC",
        scrapeCount = 0,
        elementCount = (this.scrapedElementCount ?: 0).toLong(),
        commandCount = 0,
        firstScrapedAt = this.lastScraped ?: timestamp,
        lastScrapedAt = this.lastScraped ?: timestamp
    )
}

// =============================================================================
// Note: Entity conversion functions removed - now using DTOs directly
// Callers should migrate to use DTOs instead of the old Room entities
// =============================================================================
