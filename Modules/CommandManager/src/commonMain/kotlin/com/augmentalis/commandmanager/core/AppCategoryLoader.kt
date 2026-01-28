/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * AppCategoryLoader.kt - Loads app category database from ACD files into SQLite
 *
 * Part of the Hybrid Persistence system for VoiceOSCore.
 * Loads curated app categories from AVU-format ACD files and populates
 * the app_category_override and app_pattern_group SQLite tables.
 *
 * Author: Manoj Jhawar
 * Created: 2026-01-26
 *
 * @since 2.1.0 (Hybrid Persistence)
 */

package com.augmentalis.commandmanager

import com.augmentalis.avucodec.AVUDecoder

/**
 * Interface for platform-specific asset reading.
 * Android implementation reads from assets folder.
 */
interface IAssetReader {
    /**
     * Read an asset file as a string.
     * @param filename The asset filename (e.g., "known-apps.acd")
     * @return The file contents as a string, or null if not found
     */
    suspend fun readAsset(filename: String): String?

    /**
     * Check if an asset file exists.
     * @param filename The asset filename
     * @return True if the asset exists
     */
    fun assetExists(filename: String): Boolean
}

/**
 * Repository interface for app category persistence.
 * Implemented by platform-specific database code.
 */
interface IAppCategoryRepository {
    /**
     * Insert or update an app category entry.
     */
    suspend fun upsertCategory(
        packageName: String,
        category: String,
        source: String,
        confidence: Float,
        acdVersion: String?,
        createdAt: Long,
        updatedAt: Long
    )

    /**
     * Get category for a package.
     */
    suspend fun getCategory(packageName: String): AppCategoryEntry?

    /**
     * Get the current ACD version loaded in the database.
     */
    suspend fun getLoadedAcdVersion(): String?

    /**
     * Delete all system entries (before re-import).
     */
    suspend fun deleteSystemEntries()

    /**
     * Get all entries.
     */
    suspend fun getAllEntries(): List<AppCategoryEntry>

    /**
     * Count entries.
     */
    suspend fun count(): Long
}

/**
 * Repository interface for app pattern groups.
 */
interface IAppPatternGroupRepository {
    /**
     * Insert a pattern.
     */
    suspend fun insertPattern(
        category: String,
        pattern: String,
        priority: Int,
        acdVersion: String?,
        createdAt: Long
    )

    /**
     * Get all patterns.
     */
    suspend fun getAllPatterns(): List<AppPatternEntry>

    /**
     * Delete all patterns.
     */
    suspend fun deleteAll()

    /**
     * Count patterns.
     */
    suspend fun count(): Long
}

/**
 * App category entry data class.
 */
data class AppCategoryEntry(
    val packageName: String,
    val category: String,
    val source: String,
    val confidence: Float,
    val acdVersion: String?
)

/**
 * App pattern entry data class.
 */
data class AppPatternEntry(
    val category: String,
    val pattern: String,
    val priority: Int
)

/**
 * Result of loading an ACD file.
 */
data class AcdLoadResult(
    val success: Boolean,
    val entriesLoaded: Int,
    val patternsLoaded: Int,
    val version: String?,
    val error: String? = null
) {
    companion object {
        fun success(entries: Int, patterns: Int, version: String) = AcdLoadResult(
            success = true,
            entriesLoaded = entries,
            patternsLoaded = patterns,
            version = version
        )

        fun error(message: String) = AcdLoadResult(
            success = false,
            entriesLoaded = 0,
            patternsLoaded = 0,
            version = null,
            error = message
        )

        fun skipped(reason: String, currentVersion: String) = AcdLoadResult(
            success = true,
            entriesLoaded = 0,
            patternsLoaded = 0,
            version = currentVersion,
            error = reason
        )
    }
}

/**
 * Loads app category database from ACD files into SQLite.
 *
 * This loader handles:
 * - Reading ACD files from assets (via [IAssetReader])
 * - Parsing using [AVUDecoder.parseAppCategoryDatabase]
 * - Upserting entries into [IAppCategoryRepository]
 * - Upserting patterns into [IAppPatternGroupRepository]
 * - Version checking to avoid redundant reloads
 *
 * ## Usage
 *
 * ```kotlin
 * val loader = AppCategoryLoader(assetReader, categoryRepo, patternRepo)
 *
 * // Load on app startup
 * val result = loader.loadFromAssets("known-apps.acd")
 * if (result.success) {
 *     println("Loaded ${result.entriesLoaded} entries")
 * }
 *
 * // Force reload (e.g., after app update)
 * loader.loadFromAssets("known-apps.acd", forceReload = true)
 * ```
 *
 * ## Load Flow
 *
 * 1. Read ACD file from assets
 * 2. Parse using AVUDecoder
 * 3. Check version against database
 * 4. If newer (or forceReload), delete system entries and re-import
 * 5. Insert APC entries into app_category_override
 * 6. Insert APG patterns into app_pattern_group
 *
 * @param assetReader Platform-specific asset reader
 * @param categoryRepository Repository for app category entries
 * @param patternRepository Repository for pattern groups
 */
class AppCategoryLoader(
    private val assetReader: IAssetReader,
    private val categoryRepository: IAppCategoryRepository,
    private val patternRepository: IAppPatternGroupRepository
) {
    companion object {
        const val DEFAULT_ACD_FILENAME = "known-apps.acd"
        private const val TAG = "AppCategoryLoader"
    }

    /**
     * Load app categories from an ACD file in assets.
     *
     * @param filename The ACD filename (default: "known-apps.acd")
     * @param forceReload If true, reload even if version hasn't changed
     * @return [AcdLoadResult] with load status and counts
     */
    suspend fun loadFromAssets(
        filename: String = DEFAULT_ACD_FILENAME,
        forceReload: Boolean = false
    ): AcdLoadResult {
        // Check if asset exists
        if (!assetReader.assetExists(filename)) {
            return AcdLoadResult.error("Asset file not found: $filename")
        }

        // Read asset file
        val content = assetReader.readAsset(filename)
            ?: return AcdLoadResult.error("Failed to read asset: $filename")

        return loadFromString(content, forceReload)
    }

    /**
     * Load app categories from an ACD string (for testing or external files).
     *
     * @param content The ACD file content as a string
     * @param forceReload If true, reload even if version hasn't changed
     * @return [AcdLoadResult] with load status and counts
     */
    suspend fun loadFromString(
        content: String,
        forceReload: Boolean = false
    ): AcdLoadResult {
        // Parse ACD content
        val database = AVUDecoder.parseAppCategoryDatabase(content)
            ?: return AcdLoadResult.error("Failed to parse ACD content")

        val acdVersion = database.version

        // Check if we need to reload
        if (!forceReload) {
            val loadedVersion = categoryRepository.getLoadedAcdVersion()
            if (loadedVersion != null && isVersionSameOrNewer(loadedVersion, acdVersion)) {
                return AcdLoadResult.skipped(
                    "ACD version $loadedVersion already loaded (file: $acdVersion)",
                    loadedVersion
                )
            }
        }

        // Clear existing system entries
        categoryRepository.deleteSystemEntries()
        patternRepository.deleteAll()

        val now = currentTimeMillis()
        var entriesLoaded = 0
        var patternsLoaded = 0

        // Load category entries
        for (entry in database.entries) {
            categoryRepository.upsertCategory(
                packageName = entry.packageName,
                category = entry.category,
                source = entry.source,
                confidence = entry.confidence,
                acdVersion = acdVersion,
                createdAt = now,
                updatedAt = now
            )
            entriesLoaded++
        }

        // Load pattern groups
        for (group in database.patternGroups) {
            var priority = group.patterns.size // Higher priority for earlier patterns
            for (pattern in group.patterns) {
                patternRepository.insertPattern(
                    category = group.category,
                    pattern = pattern,
                    priority = priority--,
                    acdVersion = acdVersion,
                    createdAt = now
                )
                patternsLoaded++
            }
        }

        return AcdLoadResult.success(entriesLoaded, patternsLoaded, acdVersion)
    }

    /**
     * Check if a version is the same or newer than another.
     * Simple semver comparison (X.Y.Z format).
     */
    private fun isVersionSameOrNewer(loaded: String, incoming: String): Boolean {
        val loadedParts = loaded.split(".").mapNotNull { it.toIntOrNull() }
        val incomingParts = incoming.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(loadedParts.size, incomingParts.size)) {
            val l = loadedParts.getOrElse(i) { 0 }
            val n = incomingParts.getOrElse(i) { 0 }
            if (l > n) return true
            if (l < n) return false
        }
        return true // Same version
    }

    // Uses expect/actual currentTimeMillis() from ISpeechEngine.kt
}

/**
 * Extension function to query category from database with fallback to patterns.
 */
suspend fun IAppCategoryRepository.getCategoryWithPatternFallback(
    packageName: String,
    patternRepository: IAppPatternGroupRepository
): AppCategoryEntry? {
    // First try exact match
    getCategory(packageName)?.let { return it }

    // Try pattern matching
    val patterns = patternRepository.getAllPatterns()
    val lowerPackage = packageName.lowercase()

    for (pattern in patterns.sortedByDescending { it.priority }) {
        if (lowerPackage.contains(pattern.pattern.lowercase())) {
            return AppCategoryEntry(
                packageName = packageName,
                category = pattern.category,
                source = "pattern",
                confidence = 0.70f,
                acdVersion = null
            )
        }
    }

    return null
}
