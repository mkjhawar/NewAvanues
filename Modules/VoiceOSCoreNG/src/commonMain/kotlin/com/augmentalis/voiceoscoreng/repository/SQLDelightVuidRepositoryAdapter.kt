/**
 * SQLDelightVuidRepositoryAdapter.kt - Bridge to VoiceOS/core/database
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Adapts VoiceOSCoreNG's IVuidRepository to use VoiceOS/core/database's
 * IScrapedElementRepository for SQLDelight persistence.
 */
package com.augmentalis.voiceoscoreng.repository

import com.augmentalis.database.dto.ScrapedElementDTO
import com.augmentalis.database.repositories.IScrapedElementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Adapter that bridges VoiceOSCoreNG's IVuidRepository to
 * VoiceOS/core/database's IScrapedElementRepository.
 *
 * This allows VoiceOSCoreNG to save VUID entries to the same SQLDelight
 * database used by VoiceOSCore for scraped elements.
 *
 * @param delegate The underlying SQLDelight repository
 */
class SQLDelightVuidRepositoryAdapter(
    private val delegate: IScrapedElementRepository
) : IVuidRepository {

    // In-memory cache for flow observation
    private val screenFlows = mutableMapOf<String, MutableStateFlow<List<VuidEntry>>>()

    override suspend fun save(entry: VuidEntry): Result<Unit> {
        return try {
            val dto = entry.toDTO()
            delegate.insert(dto)
            refreshScreenFlow(entry.packageName, entry.activityName)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveAll(entries: List<VuidEntry>): Result<Unit> {
        return try {
            entries.forEach { entry ->
                delegate.insert(entry.toDTO())
            }
            // Refresh all affected screens
            entries.map { it.packageName to it.activityName }
                .toSet()
                .forEach { (pkg, activity) -> refreshScreenFlow(pkg, activity) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getById(vuid: String): VuidEntry? {
        // VUID is stored in uuid field
        // We need to search by element hash since that's the unique key
        return delegate.getByHash(vuid)?.toVuidEntry()
    }

    override suspend fun getByPackage(packageName: String): List<VuidEntry> {
        return delegate.getByApp(packageName).map { it.toVuidEntry() }
    }

    override suspend fun getByScreen(packageName: String, activityName: String): List<VuidEntry> {
        // Use screen_hash to filter by activity/screen
        return delegate.getByApp(packageName)
            .filter { dto ->
                // Activity is stored in screen_hash field
                dto.screen_hash?.contains(activityName) == true ||
                dto.formGroupId?.contains(activityName) == true
            }
            .map { it.toVuidEntry() }
    }

    override suspend fun updateAlias(vuid: String, alias: String): Result<Unit> {
        return try {
            val existing = getById(vuid) ?: return Result.failure(
                IllegalArgumentException("VUID not found: $vuid")
            )
            val updated = existing.copy(
                alias = alias,
                updatedAt = currentTimeMillis()
            )
            save(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(vuid: String): Result<Unit> {
        return try {
            delegate.deleteByHash(vuid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteByPackage(packageName: String): Result<Unit> {
        return try {
            delegate.deleteByApp(packageName)
            // Clear all screen flows for this app
            screenFlows.keys
                .filter { it.startsWith("$packageName:") }
                .forEach { screenFlows.remove(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exists(vuid: String): Boolean {
        return getById(vuid) != null
    }

    override fun observeByScreen(packageName: String, activityName: String): Flow<List<VuidEntry>> {
        val key = "$packageName:$activityName"
        return screenFlows.getOrPut(key) {
            MutableStateFlow(emptyList())
        }
    }

    override suspend fun getByHash(hash: String): VuidEntry? {
        return delegate.getByHash(hash)?.toVuidEntry()
    }

    // ========== Private Helpers ==========

    private suspend fun refreshScreenFlow(packageName: String, activityName: String) {
        val key = "$packageName:$activityName"
        screenFlows[key]?.let { flow ->
            flow.value = getByScreen(packageName, activityName)
        }
    }

    /**
     * Convert VuidEntry to ScrapedElementDTO for database storage.
     */
    private fun VuidEntry.toDTO(): ScrapedElementDTO {
        val boundsJson = bounds?.let {
            """{"left":${it[0]},"top":${it[1]},"right":${it[2]},"bottom":${it[3]}}"""
        } ?: "{}"

        return ScrapedElementDTO(
            id = 0, // Auto-generated by database
            elementHash = contentHash,
            appId = packageName,
            uuid = vuid,
            className = elementType,
            viewIdResourceName = metadata["viewId"],
            text = text,
            contentDescription = contentDescription,
            bounds = boundsJson,
            isClickable = if (metadata["isClickable"] == "true") 1L else 0L,
            isLongClickable = if (metadata["isLongClickable"] == "true") 1L else 0L,
            isEditable = if (metadata["isEditable"] == "true") 1L else 0L,
            isScrollable = if (metadata["isScrollable"] == "true") 1L else 0L,
            isCheckable = if (metadata["isCheckable"] == "true") 1L else 0L,
            isFocusable = if (metadata["isFocusable"] == "true") 1L else 0L,
            isEnabled = if (metadata["isEnabled"] != "false") 1L else 0L, // Default enabled
            depth = metadata["depth"]?.toLongOrNull() ?: 0L,
            indexInParent = metadata["indexInParent"]?.toLongOrNull() ?: 0L,
            scrapedAt = createdAt,
            semanticRole = metadata["semanticRole"],
            inputType = metadata["inputType"],
            visualWeight = metadata["visualWeight"],
            isRequired = if (metadata["isRequired"] == "true") 1L else null,
            formGroupId = activityName, // Store activity name here for querying
            placeholderText = metadata["placeholder"],
            validationPattern = metadata["validationPattern"],
            backgroundColor = metadata["backgroundColor"],
            screen_hash = activityName // Also store here for backward compatibility
        )
    }

    /**
     * Convert ScrapedElementDTO to VuidEntry.
     */
    private fun ScrapedElementDTO.toVuidEntry(): VuidEntry {
        val boundsArray = parseBoundsJson(bounds)

        return VuidEntry(
            vuid = uuid ?: elementHash,
            packageName = appId,
            activityName = screen_hash ?: formGroupId ?: "",
            contentHash = elementHash,
            elementType = className,
            alias = null, // Alias stored separately if needed
            text = text,
            contentDescription = contentDescription,
            bounds = boundsArray,
            createdAt = scrapedAt,
            updatedAt = scrapedAt,
            metadata = buildMap {
                viewIdResourceName?.let { put("viewId", it) }
                put("isClickable", (isClickable == 1L).toString())
                put("isLongClickable", (isLongClickable == 1L).toString())
                put("isEditable", (isEditable == 1L).toString())
                put("isScrollable", (isScrollable == 1L).toString())
                put("isCheckable", (isCheckable == 1L).toString())
                put("isFocusable", (isFocusable == 1L).toString())
                put("isEnabled", (isEnabled == 1L).toString())
                put("depth", depth.toString())
                put("indexInParent", indexInParent.toString())
                semanticRole?.let { put("semanticRole", it) }
                inputType?.let { put("inputType", it) }
                visualWeight?.let { put("visualWeight", it) }
                isRequired?.let { put("isRequired", (it == 1L).toString()) }
                placeholderText?.let { put("placeholder", it) }
                validationPattern?.let { put("validationPattern", it) }
                backgroundColor?.let { put("backgroundColor", it) }
                put("id", id.toString())
            }
        )
    }

    /**
     * Parse bounds JSON to IntArray.
     */
    private fun parseBoundsJson(boundsJson: String): IntArray? {
        return try {
            // Simple JSON parsing: {"left":0,"top":0,"right":100,"bottom":100}
            val pattern = Regex(""""(left|top|right|bottom)":(-?\d+)""")
            val matches = pattern.findAll(boundsJson)
            val values = mutableMapOf<String, Int>()
            matches.forEach { match ->
                values[match.groupValues[1]] = match.groupValues[2].toInt()
            }
            if (values.size == 4) {
                intArrayOf(
                    values["left"] ?: 0,
                    values["top"] ?: 0,
                    values["right"] ?: 0,
                    values["bottom"] ?: 0
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
