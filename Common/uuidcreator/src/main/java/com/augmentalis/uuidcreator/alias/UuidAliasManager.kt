/**
 * UuidAliasManager.kt - Custom aliases for long UUIDs
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/alias/UuidAliasManager.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Manages human-readable aliases for long third-party UUIDs
 */

package com.augmentalis.uuidcreator.alias

import com.augmentalis.uuidcreator.database.UUIDCreatorDatabase
import com.augmentalis.uuidcreator.database.entities.UUIDElementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UUID Alias Manager
 *
 * Creates and manages human-readable aliases for ALL UUID types.
 * Aliases provide smaller footprint and easier voice command integration.
 *
 * ## Supported UUID Formats
 *
 * ### 1. Standard UUIDs
 * ```
 * 550e8400-e29b-41d4-a716-446655440000 → submit_btn
 * ```
 *
 * ### 2. Custom Prefixed UUIDs
 * ```
 * btn-550e8400-e29b-41d4-a716-446655440000 → main_submit
 * ```
 *
 * ### 3. Third-Party UUIDs
 * ```
 * com.instagram.android.v12.0.0.button-a7f3e2c1d4b5 → instagram_submit
 * ```
 *
 * ## Alias Examples
 *
 * ```
 * submit_btn          → Any UUID for submit button
 * main_menu           → Main menu UUID
 * profile_pic         → Profile picture UUID
 * instagram_like      → Instagram like button
 * fb_share            → Facebook share button
 * ```
 *
 * ## Alias Generation Strategies
 *
 * ### 1. Auto-Generated
 * Based on app name + element content:
 * - `{app}_{content}_{type}`
 * - Example: `instagram_submit_button`
 *
 * ### 2. Manual
 * User-defined custom alias:
 * - `my_favorite_button`
 * - `submit_btn`
 *
 * ### 3. Smart
 * AI-generated based on context:
 * - `insta_like_btn` (detected "like" in content)
 * - `fb_share_menu` (detected "share" action)
 *
 * ## Voice Command Integration
 *
 * ```kotlin
 * // Instead of:
 * voiceCommand("click com.instagram.android.v12.0.0.button-a7f3e2c1d4b5")
 *
 * // Use alias:
 * voiceCommand("click instagram_submit")
 * ```
 *
 * ## Usage Example
 *
 * ```kotlin
 * val aliasManager = UuidAliasManager(context)
 *
 * // Auto-generate alias
 * val alias = aliasManager.createAutoAlias(
 *     uuid = "com.instagram.android.v12.0.0.button-a7f3e2c1d4b5",
 *     elementName = "Submit",
 *     elementType = "button"
 * )
 * // Returns: "instagram_submit_btn"
 *
 * // Manual alias
 * aliasManager.setAlias(
 *     uuid = "com.instagram.android.v12.0.0.button-a7f3e2c1d4b5",
 *     alias = "my_submit_button"
 * )
 *
 * // Resolve alias to UUID
 * val uuid = aliasManager.resolveAlias("instagram_submit_btn")
 * // Returns: "com.instagram.android.v12.0.0.button-a7f3e2c1d4b5"
 * ```
 *
 * @property database UUIDCreator database
 *
 * @since 1.0.0
 */
class UuidAliasManager(
    private val database: UUIDCreatorDatabase
) {

    /**
     * Alias DAO for database persistence
     */
    private val aliasDao = database.uuidAliasDao()

    /**
     * Bidirectional mapping (in-memory cache)
     * - alias → UUID
     * - UUID → aliases (one UUID can have multiple aliases)
     *
     * NOTE: This is synchronized with database via loadCache()
     */
    private val aliasToUuid = mutableMapOf<String, String>()
    private val uuidToAliases = mutableMapOf<String, MutableSet<String>>()

    /**
     * Flag indicating if cache has been loaded from database
     */
    @Volatile
    private var isLoaded = false

    /**
     * Load cache from database
     *
     * Called automatically on first access. Populates in-memory maps
     * from persisted alias entities.
     */
    suspend fun loadCache() = withContext(Dispatchers.IO) {
        if (!isLoaded) {
            val aliases = aliasDao.getAll()
            aliases.forEach { entity ->
                aliasToUuid[entity.alias] = entity.uuid
                uuidToAliases.getOrPut(entity.uuid) { mutableSetOf() }.add(entity.alias)
            }
            isLoaded = true
        }
    }

    /**
     * Ensure cache is loaded
     *
     * Internal helper to lazy-load cache on first access.
     */
    private suspend fun ensureLoaded() {
        if (!isLoaded) {
            loadCache()
        }
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
     *
     * Generates human-readable alias based on element properties.
     *
     * ## Format
     * `{app}_{content}_{type}`
     *
     * ## Examples
     * - Instagram submit button → `instagram_submit_btn`
     * - Facebook share menu → `facebook_share_menu`
     * - Twitter like button → `twitter_like_btn`
     *
     * @param uuid Full UUID
     * @param elementName Element name/text
     * @param elementType Element type
     * @param useAbbreviation Use app abbreviation (default: true)
     * @return Generated alias
     */
    suspend fun createAutoAlias(
        uuid: String,
        elementName: String?,
        elementType: String,
        useAbbreviation: Boolean = true
    ): String = withContext(Dispatchers.Default) {
        ensureLoaded()

        // Extract app name from UUID
        val appName = extractAppNameFromUuid(uuid)

        // Use abbreviation if available
        val appPart = if (useAbbreviation) {
            appAbbreviations[appName.lowercase()] ?: appName
        } else {
            appName
        }

        // Clean element name
        val namePart = elementName
            ?.lowercase()
            ?.replace(Regex("[^a-z0-9]+"), "_")
            ?.trim('_')
            ?: "element"

        // Abbreviate type
        val typePart = abbreviateType(elementType)

        // Build alias
        var alias = "${appPart}_${namePart}_$typePart"

        // Ensure uniqueness
        alias = ensureUniqueAlias(alias)

        // Register alias (with database persistence)
        setAlias(uuid, alias, isPrimary = true)

        alias
    }

    /**
     * Set manual alias
     *
     * Creates user-defined custom alias for UUID.
     *
     * @param uuid UUID to alias
     * @param alias Custom alias (must be unique)
     * @param isPrimary Whether this is the primary alias (default: false)
     * @throws IllegalArgumentException if alias already exists
     */
    suspend fun setAlias(uuid: String, alias: String, isPrimary: Boolean = false) = withContext(Dispatchers.IO) {
        ensureLoaded()

        // Validate alias format
        validateAlias(alias)

        // Check if alias already exists
        if (aliasToUuid.containsKey(alias)) {
            val existingUuid = aliasToUuid[alias]
            if (existingUuid != uuid) {
                throw IllegalArgumentException("Alias '$alias' already exists for UUID: $existingUuid")
            }
            // Same alias for same UUID → update existing
            return@withContext
        }

        // Register bidirectional mapping (cache)
        aliasToUuid[alias] = uuid
        uuidToAliases.getOrPut(uuid) { mutableSetOf() }.add(alias)

        // Persist to database
        val aliasEntity = com.augmentalis.uuidcreator.database.entities.UUIDAliasEntity(
            alias = alias,
            uuid = uuid,
            isPrimary = isPrimary,
            createdAt = System.currentTimeMillis()
        )
        aliasDao.insert(aliasEntity)
    }

    /**
     * Resolve alias to UUID
     *
     * Looks up UUID for given alias.
     *
     * @param alias Alias to resolve
     * @return UUID or null if alias not found
     */
    suspend fun resolveAlias(alias: String): String? = withContext(Dispatchers.IO) {
        ensureLoaded()
        aliasToUuid[alias]
    }

    /**
     * Get aliases for UUID
     *
     * Returns all aliases for given UUID.
     *
     * @param uuid UUID to query
     * @return Set of aliases
     */
    fun getAliases(uuid: String): Set<String> {
        return uuidToAliases[uuid] ?: emptySet()
    }

    /**
     * Get primary alias
     *
     * Returns first (usually auto-generated) alias for UUID.
     *
     * @param uuid UUID to query
     * @return Primary alias or null
     */
    fun getPrimaryAlias(uuid: String): String? {
        return uuidToAliases[uuid]?.firstOrNull()
    }

    /**
     * Remove alias
     *
     * Deletes alias mapping from cache and database.
     *
     * @param alias Alias to remove
     * @return true if removed, false if not found
     */
    suspend fun removeAlias(alias: String): Boolean = withContext(Dispatchers.IO) {
        ensureLoaded()

        val uuid = aliasToUuid.remove(alias) ?: return@withContext false
        uuidToAliases[uuid]?.remove(alias)

        // Remove from database
        aliasDao.deleteByAlias(alias)

        true
    }

    /**
     * Batch create aliases for package
     *
     * Auto-generates aliases for all elements in package.
     *
     * @param packageName Package to alias
     * @return Map of UUID → generated alias
     */
    suspend fun createAliasesForPackage(packageName: String): Map<String, String> = withContext(Dispatchers.IO) {
        val elements = database.uuidElementDao()
            .getAll()
            .filter { it.uuid.startsWith(packageName) }

        val aliases = mutableMapOf<String, String>()

        elements.forEach { entity ->
            try {
                val alias = createAutoAlias(
                    uuid = entity.uuid,
                    elementName = entity.name,
                    elementType = entity.type
                )
                aliases[entity.uuid] = alias
            } catch (e: Exception) {
                // Skip elements that fail to generate alias
            }
        }

        aliases
    }

    /**
     * Export aliases as JSON
     *
     * @return JSON string of all aliases
     */
    fun exportAliasesAsJson(): String {
        return aliasToUuid.entries.joinToString(",\n", "{\n", "\n}") { (alias, uuid) ->
            """  "$alias": "$uuid""""
        }
    }

    /**
     * Extract app name from UUID
     *
     * Parses package name from third-party UUID format.
     *
     * @param uuid Third-party UUID
     * @return App name (e.g., "instagram" from "com.instagram.android")
     */
    private fun extractAppNameFromUuid(uuid: String): String {
        // Format: com.instagram.android.v12.0.0.button-hash
        val parts = uuid.split('.')

        // Find main app name (usually second-to-last before version)
        return when {
            parts.size >= 3 -> {
                // Extract "instagram" from "com.instagram.android"
                parts.getOrNull(1) ?: "app"
            }
            else -> "app"
        }
    }

    /**
     * Abbreviate element type
     *
     * @param type Full type name
     * @return Abbreviated type
     */
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

    /**
     * Ensure alias is unique
     *
     * Appends number suffix if alias exists.
     *
     * @param baseAlias Base alias
     * @return Unique alias
     */
    private fun ensureUniqueAlias(baseAlias: String): String {
        if (!aliasToUuid.containsKey(baseAlias)) {
            return baseAlias
        }

        // Append counter until unique
        var counter = 2
        while (aliasToUuid.containsKey("${baseAlias}_$counter")) {
            counter++
        }

        return "${baseAlias}_$counter"
    }

    /**
     * Validate alias format
     *
     * Rules:
     * - Lowercase alphanumeric + underscores
     * - 3-50 characters
     * - Must start with letter
     *
     * @param alias Alias to validate
     * @throws IllegalArgumentException if invalid
     */
    private fun validateAlias(alias: String) {
        require(alias.length in 3..50) {
            "Alias must be 3-50 characters"
        }

        require(alias.matches(Regex("^[a-z][a-z0-9_]*$"))) {
            "Alias must start with letter and contain only lowercase alphanumeric + underscores"
        }
    }

    /**
     * Get alias statistics
     *
     * @return Alias stats
     */
    fun getStats(): AliasStats {
        return AliasStats(
            totalAliases = aliasToUuid.size,
            totalUuidsWithAliases = uuidToAliases.size,
            averageAliasesPerUuid = if (uuidToAliases.isNotEmpty()) {
                uuidToAliases.values.map { it.size }.average().toFloat()
            } else {
                0f
            }
        )
    }
}

/**
 * Alias Statistics
 *
 * @property totalAliases Total number of registered aliases
 * @property totalUuidsWithAliases Number of UUIDs that have aliases
 * @property averageAliasesPerUuid Average aliases per UUID
 */
data class AliasStats(
    val totalAliases: Int,
    val totalUuidsWithAliases: Int,
    val averageAliasesPerUuid: Float
)
