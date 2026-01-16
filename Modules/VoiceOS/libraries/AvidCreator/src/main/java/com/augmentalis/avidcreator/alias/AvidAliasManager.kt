/**
 * AvidAliasManager.kt - Custom aliases for VUIDs
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-03
 *
 * Manages human-readable aliases for VUIDs.
 * Uses IAvidRepository for persistence.
 */
package com.augmentalis.avidcreator.alias

import com.augmentalis.database.dto.AvidAliasDTO
import com.augmentalis.database.repositories.IAvidRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * VUID Alias Manager
 *
 * Creates and manages human-readable aliases for VUIDs.
 * Aliases provide smaller footprint and easier voice command integration.
 *
 * @property repository IAvidRepository for persistence
 */
class AvidAliasManager(
    private val repository: IAvidRepository
) {
    /**
     * In-memory cache for fast lookups
     */
    private val aliasToAvid = mutableMapOf<String, String>()
    private val avidToAliases = mutableMapOf<String, MutableSet<String>>()

    @Volatile
    private var isLoaded = false

    /**
     * Load cache from database
     */
    suspend fun loadCache() = withContext(Dispatchers.IO) {
        if (!isLoaded) {
            val aliases = repository.getAllAliases()
            aliases.forEach { dto ->
                aliasToAvid[dto.alias] = dto.avid
                avidToAliases.getOrPut(dto.avid) { mutableSetOf() }.add(dto.alias)
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
        avid: String,
        elementName: String?,
        elementType: String,
        useAbbreviation: Boolean = true
    ): String = withContext(Dispatchers.Default) {
        ensureLoaded()

        val appName = extractAppNameFromAvid(avid)
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

        setAlias(avid, alias, isPrimary = true)
        alias
    }

    /**
     * Set manual alias
     */
    suspend fun setAlias(avid: String, alias: String, isPrimary: Boolean = false) = withContext(Dispatchers.IO) {
        ensureLoaded()
        validateAlias(alias)

        if (aliasToAvid.containsKey(alias)) {
            val existingAvid = aliasToAvid[alias]
            if (existingAvid != avid) {
                throw IllegalArgumentException("Alias '$alias' already exists for AVID: $existingAvid")
            }
            return@withContext
        }

        aliasToAvid[alias] = avid
        avidToAliases.getOrPut(avid) { mutableSetOf() }.add(alias)

        repository.insertAlias(
            AvidAliasDTO(
                id = 0,
                alias = alias,
                avid = avid,
                isPrimary = isPrimary,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Resolve alias to AVID
     */
    suspend fun resolveAlias(alias: String): String? = withContext(Dispatchers.IO) {
        ensureLoaded()
        aliasToAvid[alias]
    }

    /**
     * Get aliases for AVID
     */
    fun getAliases(avid: String): Set<String> {
        return avidToAliases[avid] ?: emptySet()
    }

    /**
     * Get primary alias
     */
    fun getPrimaryAlias(avid: String): String? {
        return avidToAliases[avid]?.firstOrNull()
    }

    /**
     * Remove alias
     */
    suspend fun removeAlias(alias: String): Boolean = withContext(Dispatchers.IO) {
        ensureLoaded()
        val avid = aliasToAvid.remove(alias) ?: return@withContext false
        avidToAliases[avid]?.remove(alias)
        repository.deleteAliasByName(alias)
        true
    }

    private fun extractAppNameFromAvid(avid: String): String {
        val parts = avid.split('.')
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
        if (!aliasToAvid.containsKey(baseAlias)) return baseAlias
        var counter = 2
        while (aliasToAvid.containsKey("${baseAlias}_$counter")) {
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
            totalAliases = aliasToAvid.size,
            totalVuidsWithAliases = avidToAliases.size,
            averageAliasesPerVuid = if (avidToAliases.isNotEmpty()) {
                avidToAliases.values.map { it.size }.average().toFloat()
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
