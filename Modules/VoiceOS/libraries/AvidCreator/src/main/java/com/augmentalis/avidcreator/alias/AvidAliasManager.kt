/**
 * AvidAliasManager.kt - Custom aliases for VUIDs
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-03
 *
 * Manages human-readable aliases for VUIDs.
 * Uses IVUIDRepository for persistence.
 */
package com.augmentalis.avidcreator.alias

import com.augmentalis.database.dto.VUIDAliasDTO
import com.augmentalis.database.repositories.IVUIDRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * VUID Alias Manager
 *
 * Creates and manages human-readable aliases for VUIDs.
 * Aliases provide smaller footprint and easier voice command integration.
 *
 * @property repository IVUIDRepository for persistence
 */
class AvidAliasManager(
    private val repository: IVUIDRepository
) {
    /**
     * In-memory cache for fast lookups
     */
    private val aliasToVuid = mutableMapOf<String, String>()
    private val vuidToAliases = mutableMapOf<String, MutableSet<String>>()

    @Volatile
    private var isLoaded = false

    /**
     * Load cache from database
     */
    suspend fun loadCache() = withContext(Dispatchers.IO) {
        if (!isLoaded) {
            val aliases = repository.getAllAliases()
            aliases.forEach { dto ->
                aliasToVuid[dto.alias] = dto.vuid
                vuidToAliases.getOrPut(dto.vuid) { mutableSetOf() }.add(dto.alias)
            }
            isLoaded = true
        }
    }

    private suspend fun ensureLoaded() {
        if (!isLoaded) loadCache()
    }

    /**
     * Common app name abbreviations
     */
    private val appAbbreviations = mapOf(
        "instagram" to "ig",
        "facebook" to "fb",
        "twitter" to "tw",
        "tiktok" to "tt",
        "youtube" to "yt",
        "whatsapp" to "wa",
        "telegram" to "tg",
        "snapchat" to "sc",
        "reddit" to "rd",
        "linkedin" to "li"
    )

    /**
     * Create auto-generated alias
     */
    suspend fun createAutoAlias(
        vuid: String,
        elementName: String?,
        elementType: String,
        useAbbreviation: Boolean = true
    ): String = withContext(Dispatchers.Default) {
        ensureLoaded()

        val appName = extractAppNameFromVuid(vuid)
        val appPart = if (useAbbreviation) {
            appAbbreviations[appName.lowercase()] ?: appName
        } else {
            appName
        }

        val namePart = elementName
            ?.lowercase()
            ?.replace(Regex("[^a-z0-9]+"), "_")
            ?.trim('_')
            ?: "element"

        val typePart = abbreviateType(elementType)
        var alias = "${appPart}_${namePart}_$typePart"
        alias = ensureUniqueAlias(alias)

        setAlias(vuid, alias, isPrimary = true)
        alias
    }

    /**
     * Set manual alias
     */
    suspend fun setAlias(vuid: String, alias: String, isPrimary: Boolean = false) = withContext(Dispatchers.IO) {
        ensureLoaded()
        validateAlias(alias)

        if (aliasToVuid.containsKey(alias)) {
            val existingVuid = aliasToVuid[alias]
            if (existingVuid != vuid) {
                throw IllegalArgumentException("Alias '$alias' already exists for VUID: $existingVuid")
            }
            return@withContext
        }

        aliasToVuid[alias] = vuid
        vuidToAliases.getOrPut(vuid) { mutableSetOf() }.add(alias)

        repository.insertAlias(
            VUIDAliasDTO(
                id = 0,
                alias = alias,
                vuid = vuid,
                isPrimary = isPrimary,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Resolve alias to VUID
     */
    suspend fun resolveAlias(alias: String): String? = withContext(Dispatchers.IO) {
        ensureLoaded()
        aliasToVuid[alias]
    }

    /**
     * Get aliases for VUID
     */
    fun getAliases(vuid: String): Set<String> {
        return vuidToAliases[vuid] ?: emptySet()
    }

    /**
     * Get primary alias
     */
    fun getPrimaryAlias(vuid: String): String? {
        return vuidToAliases[vuid]?.firstOrNull()
    }

    /**
     * Remove alias
     */
    suspend fun removeAlias(alias: String): Boolean = withContext(Dispatchers.IO) {
        ensureLoaded()
        val vuid = aliasToVuid.remove(alias) ?: return@withContext false
        vuidToAliases[vuid]?.remove(alias)
        repository.deleteAliasByName(alias)
        true
    }

    private fun extractAppNameFromVuid(vuid: String): String {
        val parts = vuid.split('.')
        return when {
            parts.size >= 3 -> parts.getOrNull(1) ?: "app"
            else -> "app"
        }
    }

    private fun abbreviateType(type: String): String {
        return when (type.lowercase()) {
            "button", "imagebutton" -> "btn"
            "textview", "text" -> "txt"
            "edittext", "input" -> "input"
            "imageview", "image" -> "img"
            "checkbox" -> "chk"
            "radiobutton" -> "radio"
            "switch", "togglebutton" -> "toggle"
            "viewgroup", "container" -> "container"
            "layout" -> "layout"
            "menu" -> "menu"
            "tab" -> "tab"
            else -> type.lowercase().take(5)
        }
    }

    private fun ensureUniqueAlias(baseAlias: String): String {
        if (!aliasToVuid.containsKey(baseAlias)) return baseAlias
        var counter = 2
        while (aliasToVuid.containsKey("${baseAlias}_$counter")) {
            counter++
        }
        return "${baseAlias}_$counter"
    }

    private fun validateAlias(alias: String) {
        require(alias.length in 3..50) { "Alias must be 3-50 characters" }
        require(alias.matches(Regex("^[a-z][a-z0-9_]*$"))) {
            "Alias must start with letter and contain only lowercase alphanumeric + underscores"
        }
    }

    /**
     * Set multiple aliases in batch
     *
     * @param vuidAliasMap Map of VUID to alias pairs (vuid -> desiredAlias)
     * @return Map of VUID to actual registered alias (vuid -> actualAlias)
     */
    suspend fun setAliasesBatch(vuidAliasMap: Map<String, String>): Map<String, String> = withContext(Dispatchers.IO) {
        ensureLoaded()
        val result = mutableMapOf<String, String>()
        vuidAliasMap.forEach { (vuid, alias) ->
            try {
                setAlias(vuid, alias, isPrimary = false)
                // Get the actual alias that was registered (might be deduplicated)
                val actualAlias = getAliases(vuid).firstOrNull() ?: alias
                result[vuid] = actualAlias
            } catch (e: Exception) {
                // Skip invalid aliases in batch mode, but still track them
                result[vuid] = alias
            }
        }
        result
    }

    /**
     * Get alias statistics
     */
    fun getStats(): AliasStats {
        return AliasStats(
            totalAliases = aliasToVuid.size,
            totalVuidsWithAliases = vuidToAliases.size,
            averageAliasesPerVuid = if (vuidToAliases.isNotEmpty()) {
                vuidToAliases.values.map { it.size }.average().toFloat()
            } else {
                0f
            }
        )
    }
}

/**
 * Alias Statistics
 */
data class AliasStats(
    val totalAliases: Int,
    val totalVuidsWithAliases: Int,
    val averageAliasesPerVuid: Float
)
