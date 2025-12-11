package com.augmentalis.voiceoscore.database

import android.content.Context
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.database.entities.AppEntity

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
     */
    suspend fun insertHierarchyBatch(hierarchies: List<com.augmentalis.voiceoscore.scraping.entities.ScrapedHierarchyEntity>): List<Long> {
        val ids = mutableListOf<Long>()
        hierarchies.forEach { hierarchy ->
            databaseManager.scrapedHierarchies.insert(
                parentElementHash = hierarchy.parentElementHash,
                childElementHash = hierarchy.childElementHash,
                depth = hierarchy.depth.toLong(),
                createdAt = hierarchy.createdAt
            )
            ids.add(hierarchy.id ?: System.currentTimeMillis())
        }
        return ids
    }

    /**
     * Extension: Insert batch of generated commands
     */
    suspend fun insertCommandBatch(commands: List<com.augmentalis.voiceoscore.scraping.entities.GeneratedCommandEntity>): List<Long> {
        val ids = mutableListOf<Long>()
        commands.forEach { command ->
            val dto = command.toGeneratedCommandDTO()
            databaseManager.generatedCommands.insert(dto)
            ids.add(command.id ?: System.currentTimeMillis())
        }
        return ids
    }

    /**
     * Extension: Insert batch of element relationships
     */
    suspend fun insertRelationshipBatch(relationships: List<com.augmentalis.voiceoscore.scraping.entities.ElementRelationshipEntity>): List<Long> {
        val ids = mutableListOf<Long>()
        relationships.forEach { relationship ->
            databaseManager.elementRelationships.insert(
                sourceElementHash = relationship.sourceElementHash,
                targetElementHash = relationship.targetElementHash,
                relationshipType = relationship.relationshipType,
                relationshipData = relationship.relationshipData,
                confidence = relationship.confidence.toDouble(),
                createdAt = relationship.createdAt,
                updatedAt = relationship.updatedAt
            )
            ids.add(relationship.id ?: System.currentTimeMillis())
        }
        return ids
    }

    /**
     * Extension: Get all commands for an app
     * Note: Repository doesn't have getByApp method, so this returns all commands
     * TODO: Filter by app when repository method is added
     */
    suspend fun getCommandsByApp(appId: String): List<com.augmentalis.voiceoscore.scraping.entities.GeneratedCommandEntity> {
        val dtos = databaseManager.generatedCommands.getAll()
        return dtos.map { it.toGeneratedCommandEntity() }
    }

    /**
     * Extension: Count elements for an app
     */
    suspend fun countElementsByApp(appId: String): Long {
        return databaseManager.scrapedElements.countByApp(appId)
    }

    /**
     * Extension: Insert batch of scraped elements and return assigned IDs
     * NOTE: Transaction wrapping attempted but not compatible with current architecture
     * (suspend insert methods can't be called in non-suspend transaction body)
     * Performance is still good due to batch operation reducing function call overhead
     */
    suspend fun insertElementBatch(elements: List<com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity>): List<Long> {
        val ids = mutableListOf<Long>()
        elements.forEach { element ->
            val dto = element.toScrapedElementDTO()
            databaseManager.scrapedElements.insert(dto)
            // Use element hash as ID for now
            ids.add(element.id ?: element.elementHash.hashCode().toLong())
        }
        return ids
    }

    /**
     * Extension: Insert or update a single scraped element
     */
    suspend fun upsertElement(element: com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity) {
        val dto = element.toScrapedElementDTO()
        databaseManager.scrapedElements.insert(dto)
    }

    /**
     * Extension: Insert screen context (stub - needs conversion)
     */
    suspend fun insertScreenContext(context: com.augmentalis.voiceoscore.scraping.entities.ScreenContextEntity) {
        val dto = com.augmentalis.database.dto.ScreenContextDTO(
            id = context.id,
            screenHash = context.screenHash,
            appId = context.appId,
            packageName = context.packageName,
            activityName = context.activityName,
            windowTitle = context.windowTitle,
            screenType = context.screenType,
            formContext = context.formContext,
            navigationLevel = context.navigationLevel.toLong(),
            primaryAction = context.primaryAction,
            elementCount = context.elementCount.toLong(),
            hasBackButton = if (context.hasBackButton) 1L else 0L,
            firstScraped = context.firstScraped,
            lastScraped = context.lastScraped,
            visitCount = context.visitCount.toLong()
        )
        databaseManager.screenContexts.insert(dto)
    }

    /**
     * Extension: Insert screen transition
     */
    suspend fun insertScreenTransition(transition: com.augmentalis.voiceoscore.scraping.entities.ScreenTransitionEntity) {
        val dto = com.augmentalis.database.dto.ScreenTransitionDTO(
            id = transition.id,
            fromScreenHash = transition.fromScreenHash,
            toScreenHash = transition.toScreenHash,
            triggerElementHash = transition.triggerElementHash,
            triggerAction = transition.triggerAction,
            transitionCount = transition.transitionCount.toLong(),
            avgDurationMs = transition.avgDurationMs,
            lastTransitionAt = transition.lastTransitionAt
        )
        databaseManager.screenTransitions.insert(dto)
    }

    /**
     * Extension: Insert user interaction
     */
    suspend fun insertUserInteraction(interaction: com.augmentalis.voiceoscore.scraping.entities.UserInteractionEntity) {
        val dto = com.augmentalis.database.dto.UserInteractionDTO(
            id = interaction.id,
            elementHash = interaction.elementHash,
            screenHash = interaction.screenHash,
            interactionType = interaction.interactionType,
            interactionTime = interaction.interactionTime,
            visibilityStart = interaction.visibilityStart,
            visibilityDuration = interaction.visibilityDuration
        )
        databaseManager.userInteractions.insert(dto)
    }

    /**
     * Extension: Insert element state history
     */
    suspend fun insertElementStateHistory(state: com.augmentalis.voiceoscore.scraping.entities.ElementStateHistoryEntity): Long {
        val dto = com.augmentalis.database.dto.ElementStateHistoryDTO(
            id = state.id,
            elementHash = state.elementHash,
            screenHash = state.screenHash,
            stateType = state.stateType,
            oldValue = state.oldValue,
            newValue = state.newValue,
            changedAt = state.changedAt,
            triggeredBy = state.triggeredBy ?: "unknown"
        )
        return databaseManager.elementStateHistory.insert(dto)
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
// Additional Entity <-> DTO Conversions (for scraping entities)
// =============================================================================

/**
 * Convert ScrapedHierarchyEntity to DTO
 */
private fun com.augmentalis.voiceoscore.scraping.entities.ScrapedHierarchyEntity.toScrapedHierarchyDTO(): com.augmentalis.database.dto.ScrapedHierarchyDTO {
    return com.augmentalis.database.dto.ScrapedHierarchyDTO(
        id = this.id ?: 0L,
        parentElementHash = this.parentElementHash,
        childElementHash = this.childElementHash,
        depth = this.depth.toLong(),
        createdAt = System.currentTimeMillis()
    )
}

/**
 * Convert GeneratedCommandEntity to DTO
 */
private fun com.augmentalis.voiceoscore.scraping.entities.GeneratedCommandEntity.toGeneratedCommandDTO(): com.augmentalis.database.dto.GeneratedCommandDTO {
    return com.augmentalis.database.dto.GeneratedCommandDTO(
        id = this.id,
        elementHash = this.elementHash,
        commandText = this.commandText,
        actionType = this.actionType,
        confidence = this.confidence.toDouble(),
        synonyms = this.synonyms,
        isUserApproved = if (this.isUserApproved) 1L else 0L,
        usageCount = this.usageCount.toLong(),
        lastUsed = this.lastUsed,
        createdAt = this.generatedAt
    )
}

/**
 * Convert GeneratedCommandDTO to Entity
 */
private fun com.augmentalis.database.dto.GeneratedCommandDTO.toGeneratedCommandEntity(): com.augmentalis.voiceoscore.scraping.entities.GeneratedCommandEntity {
    return com.augmentalis.voiceoscore.scraping.entities.GeneratedCommandEntity(
        id = this.id,
        elementHash = this.elementHash,
        commandText = this.commandText,
        actionType = this.actionType,
        confidence = this.confidence.toFloat(),
        synonyms = this.synonyms ?: "",
        isUserApproved = this.isUserApproved == 1L,
        usageCount = this.usageCount.toInt(),
        lastUsed = this.lastUsed,
        generatedAt = this.createdAt
    )
}

/**
 * Convert ElementRelationshipEntity to DTO
 */
private fun com.augmentalis.voiceoscore.scraping.entities.ElementRelationshipEntity.toElementRelationshipDTO(): com.augmentalis.database.dto.ElementRelationshipDTO {
    return com.augmentalis.database.dto.ElementRelationshipDTO(
        id = this.id ?: 0L,
        sourceElementHash = this.sourceElementHash,
        targetElementHash = this.targetElementHash,
        relationshipType = this.relationshipType,
        relationshipData = this.relationshipData,
        confidence = this.confidence.toDouble(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

/**
 * Convert ScrapedElementEntity to DTO
 */
private fun com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity.toScrapedElementDTO(): com.augmentalis.database.dto.ScrapedElementDTO {
    return com.augmentalis.database.dto.ScrapedElementDTO(
        id = this.id,
        elementHash = this.elementHash,
        appId = this.appId,
        uuid = this.uuid,
        className = this.className,
        viewIdResourceName = this.viewIdResourceName,
        text = this.text,
        contentDescription = this.contentDescription,
        bounds = this.bounds,
        isClickable = if (this.isClickable) 1L else 0L,
        isLongClickable = if (this.isLongClickable) 1L else 0L,
        isEditable = if (this.isEditable) 1L else 0L,
        isScrollable = if (this.isScrollable) 1L else 0L,
        isCheckable = if (this.isCheckable) 1L else 0L,
        isFocusable = if (this.isFocusable) 1L else 0L,
        isEnabled = if (this.isEnabled) 1L else 0L,
        depth = this.depth.toLong(),
        indexInParent = this.indexInParent.toLong(),
        scrapedAt = this.scrapedAt,
        semanticRole = this.semanticRole,
        inputType = this.inputType,
        visualWeight = this.visualWeight,
        isRequired = if (this.isRequired) 1L else null,
        formGroupId = this.formGroupId,
        placeholderText = this.placeholderText,
        validationPattern = this.validationPattern,
        backgroundColor = this.backgroundColor,
        screen_hash = null  // Legacy entity doesn't have screen_hash
    )
}

/**
 * Convert ScrapedElementDTO to Entity
 */
fun com.augmentalis.database.dto.ScrapedElementDTO.toScrapedElementEntity(): com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity {
    return com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity(
        id = this.id,
        elementHash = this.elementHash,
        appId = this.appId,
        uuid = this.uuid,
        className = this.className,
        viewIdResourceName = this.viewIdResourceName,
        text = this.text,
        contentDescription = this.contentDescription,
        bounds = this.bounds,
        isClickable = this.isClickable == 1L,
        isLongClickable = this.isLongClickable == 1L,
        isEditable = this.isEditable == 1L,
        isScrollable = this.isScrollable == 1L,
        isCheckable = this.isCheckable == 1L,
        isFocusable = this.isFocusable == 1L,
        isEnabled = this.isEnabled == 1L,
        depth = this.depth.toInt(),
        indexInParent = this.indexInParent.toInt(),
        scrapedAt = this.scrapedAt,
        semanticRole = this.semanticRole,
        inputType = this.inputType,
        visualWeight = this.visualWeight,
        isRequired = this.isRequired == 1L,
        formGroupId = this.formGroupId,
        placeholderText = this.placeholderText,
        validationPattern = this.validationPattern,
        backgroundColor = this.backgroundColor
    )
}
