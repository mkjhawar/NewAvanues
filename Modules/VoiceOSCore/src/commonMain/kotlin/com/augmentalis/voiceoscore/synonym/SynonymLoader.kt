/**
 * SynonymLoader.kt - Multi-tier synonym pack loader
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-08
 *
 * Loads synonym packs from tiered storage:
 * 1. Custom (app-specific overrides) - highest priority
 * 2. Downloaded (shared folder, downloaded with ASR)
 * 3. Built-in (shipped with app) - lowest priority
 */
package com.augmentalis.voiceoscore

/**
 * Multi-tier synonym pack loader.
 *
 * Loads synonyms in priority order:
 * Custom > Downloaded > Built-in
 *
 * Usage:
 * ```kotlin
 * val loader = SynonymLoader(paths, resourceLoader)
 * val synonyms = loader.load("en")  // Returns SynonymMap or null
 *
 * // Check what's available
 * val languages = loader.getAvailableLanguages()
 *
 * // Force reload
 * loader.clearCache()
 * val fresh = loader.load("en")
 * ```
 */
class SynonymLoader(
    private val paths: ISynonymPaths,
    private val resourceLoader: IResourceLoader,
    private val fileLoader: IFileLoader? = null,
    private val preferBinary: Boolean = true
) {
    private val cache = mutableMapOf<String, SynonymMap?>()
    private val loadedTiers = mutableMapOf<String, SynonymTier>()

    /**
     * Load synonym map for a language.
     *
     * Checks tiers in order: Custom > Downloaded > Built-in
     *
     * @param language ISO 639-1 language code
     * @return SynonymMap or null if not found
     */
    fun load(language: String): SynonymMap? {
        // Check cache first
        if (cache.containsKey(language)) {
            return cache[language]
        }

        val map = loadFromTiers(language)
        cache[language] = map
        return map
    }

    /**
     * Load from all tiers in priority order.
     */
    private fun loadFromTiers(language: String): SynonymMap? {
        for ((tier, path) in paths.getAllPaths(language, preferBinary)) {
            val map = when (tier) {
                SynonymTier.CUSTOM, SynonymTier.DOWNLOADED -> {
                    loadFromFile(path, preferBinary)
                }
                SynonymTier.BUILTIN -> {
                    loadFromResource(path, preferBinary)
                }
            }

            if (map != null) {
                loadedTiers[language] = tier
                return map
            }
        }

        // Fallback: try loading text format from built-in
        if (preferBinary) {
            val textPath = paths.getBuiltinPath(language, binary = false)
            val map = loadFromResource(textPath, binary = false)
            if (map != null) {
                loadedTiers[language] = SynonymTier.BUILTIN
                return map
            }
        }

        return null
    }

    /**
     * Load from file system.
     */
    private fun loadFromFile(path: String, binary: Boolean): SynonymMap? {
        val loader = fileLoader ?: return null

        return try {
            val data = loader.readFile(path) ?: return null

            if (binary) {
                SynonymBinaryFormat.read(data)
            } else {
                SynonymParser.parse(data.decodeToString())
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load from app resources.
     */
    private fun loadFromResource(path: String, binary: Boolean): SynonymMap? {
        return try {
            val data = resourceLoader.loadResource(path) ?: return null

            if (binary) {
                SynonymBinaryFormat.read(data)
            } else {
                SynonymParser.parse(data.decodeToString())
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the tier from which a language was loaded.
     *
     * @param language ISO 639-1 language code
     * @return Tier, or null if not loaded
     */
    fun getLoadedTier(language: String): SynonymTier? {
        return loadedTiers[language]
    }

    /**
     * Get list of available languages across all tiers.
     */
    fun getAvailableLanguages(): List<String> {
        val languages = mutableSetOf<String>()

        // Add built-in languages
        languages.addAll(DefaultSynonymPaths.BUILTIN_LANGUAGES)

        // Add downloaded/custom languages from paths
        for (tier in listOf(SynonymTier.DOWNLOADED, SynonymTier.CUSTOM)) {
            languages.addAll(paths.listAvailableLanguages(tier))
        }

        return languages.toList().sorted()
    }

    /**
     * Check if a language is available.
     */
    fun isAvailable(language: String): Boolean {
        return language in getAvailableLanguages() || load(language) != null
    }

    /**
     * Clear the cache.
     */
    fun clearCache() {
        cache.clear()
        loadedTiers.clear()
    }

    /**
     * Preload specified languages.
     */
    fun preload(languages: List<String>) {
        languages.forEach { load(it) }
    }

    /**
     * Get statistics about loaded synonyms.
     */
    fun getStats(): LoaderStats {
        return LoaderStats(
            cachedLanguages = cache.keys.toList(),
            loadedFromTiers = loadedTiers.toMap(),
            totalEntries = cache.values.filterNotNull().sumOf { it.size }
        )
    }
}

/**
 * Statistics about loaded synonym packs.
 */
data class LoaderStats(
    val cachedLanguages: List<String>,
    val loadedFromTiers: Map<String, SynonymTier>,
    val totalEntries: Int
)

/**
 * Interface for loading resources from app bundle.
 *
 * Platform implementations should provide this.
 */
interface IResourceLoader {
    /**
     * Load a resource from the app bundle.
     *
     * @param path Resource path (e.g., "synonyms/en.qsyn")
     * @return ByteArray content, or null if not found
     */
    fun loadResource(path: String): ByteArray?

    /**
     * Check if a resource exists.
     *
     * @param path Resource path
     * @return true if resource exists
     */
    fun resourceExists(path: String): Boolean
}

/**
 * Interface for loading files from file system.
 *
 * Platform implementations should provide this.
 */
interface IFileLoader {
    /**
     * Read a file from the file system.
     *
     * @param path Absolute file path
     * @return ByteArray content, or null if not found
     */
    fun readFile(path: String): ByteArray?

    /**
     * Write a file to the file system.
     *
     * @param path Absolute file path
     * @param data Content to write
     * @return true if successful
     */
    fun writeFile(path: String, data: ByteArray): Boolean

    /**
     * Check if a file exists.
     *
     * @param path Absolute file path
     * @return true if file exists
     */
    fun fileExists(path: String): Boolean

    /**
     * List files in a directory matching a pattern.
     *
     * @param directory Directory path
     * @param extension File extension filter (e.g., "syn", "qsyn")
     * @return List of file names (without path)
     */
    fun listFiles(directory: String, extension: String): List<String>
}
