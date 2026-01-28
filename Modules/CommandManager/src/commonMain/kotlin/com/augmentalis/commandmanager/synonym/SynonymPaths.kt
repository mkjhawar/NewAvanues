/**
 * SynonymPaths.kt - Path configuration for synonym pack storage
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 *
 * Defines storage locations for synonym packs across platforms.
 *
 * Tiered Storage:
 * - Tier 1: Built-in (shipped with app, in resources)
 * - Tier 2: Downloaded (shared folder, downloaded with ASR)
 * - Tier 3: Custom (app-specific overrides)
 */
package com.augmentalis.commandmanager

/**
 * Storage tier for synonym packs.
 */
enum class SynonymTier {
    /** Built into app resources */
    BUILTIN,

    /** Downloaded to shared folder */
    DOWNLOADED,

    /** Custom app-specific overrides */
    CUSTOM
}

/**
 * Path configuration for synonym storage.
 *
 * Platform implementations should provide actual paths.
 */
interface ISynonymPaths {
    /**
     * Get the resource path for built-in synonyms.
     * This is a classpath/bundle resource path.
     *
     * @param language ISO 639-1 language code
     * @param binary True for .qsyn, false for .syn
     * @return Resource path (e.g., "synonyms/en.qsyn")
     */
    fun getBuiltinPath(language: String, binary: Boolean = true): String

    /**
     * Get the file system path for downloaded synonyms.
     *
     * @param language ISO 639-1 language code
     * @param binary True for .qsyn, false for .syn
     * @return Absolute file path
     */
    fun getDownloadedPath(language: String, binary: Boolean = true): String

    /**
     * Get the file system path for custom synonyms.
     *
     * @param language ISO 639-1 language code
     * @param binary True for .qsyn, false for .syn
     * @return Absolute file path
     */
    fun getCustomPath(language: String, binary: Boolean = true): String

    /**
     * Get all paths for a language in priority order.
     * Custom > Downloaded > Builtin
     *
     * @param language ISO 639-1 language code
     * @param binary True for .qsyn, false for .syn
     * @return List of paths to check, in priority order
     */
    fun getAllPaths(language: String, binary: Boolean = true): List<Pair<SynonymTier, String>> {
        return listOf(
            SynonymTier.CUSTOM to getCustomPath(language, binary),
            SynonymTier.DOWNLOADED to getDownloadedPath(language, binary),
            SynonymTier.BUILTIN to getBuiltinPath(language, binary)
        )
    }

    /**
     * Get the download target directory.
     *
     * @return Absolute path to download directory
     */
    fun getDownloadDirectory(): String

    /**
     * Get the custom directory.
     *
     * @return Absolute path to custom directory
     */
    fun getCustomDirectory(): String

    /**
     * List available languages in a tier.
     *
     * @param tier The storage tier to check
     * @return List of ISO 639-1 language codes
     */
    fun listAvailableLanguages(tier: SynonymTier): List<String>
}

/**
 * Default path configuration.
 *
 * Uses standard VoiceOS directory structure.
 * Platform implementations can override via [SynonymPathsProvider].
 */
class DefaultSynonymPaths(
    private val downloadDir: String,
    private val customDir: String
) : ISynonymPaths {

    override fun getBuiltinPath(language: String, binary: Boolean): String {
        val ext = if (binary) "qsyn" else "syn"
        return "synonyms/$language.$ext"
    }

    override fun getDownloadedPath(language: String, binary: Boolean): String {
        val ext = if (binary) "qsyn" else "syn"
        return "$downloadDir/$language.$ext"
    }

    override fun getCustomPath(language: String, binary: Boolean): String {
        val ext = if (binary) "qsyn" else "syn"
        return "$customDir/$language.$ext"
    }

    override fun getDownloadDirectory(): String = downloadDir

    override fun getCustomDirectory(): String = customDir

    override fun listAvailableLanguages(tier: SynonymTier): List<String> {
        // This requires platform-specific file listing
        // Default returns empty, platforms override
        return emptyList()
    }

    companion object {
        /** Built-in languages shipped with the app */
        val BUILTIN_LANGUAGES = listOf("en", "es")

        /**
         * Create paths for Android.
         *
         * @param filesDir App's internal files directory
         * @param externalDir Shared external directory (optional)
         */
        fun forAndroid(filesDir: String, externalDir: String? = null): DefaultSynonymPaths {
            return DefaultSynonymPaths(
                downloadDir = externalDir ?: "$filesDir/VoiceOS/synonyms",
                customDir = "$filesDir/synonyms/custom"
            )
        }

        /**
         * Create paths for iOS.
         *
         * @param documentsDir App's documents directory
         * @param sharedDir App group shared directory (optional)
         */
        fun forIOS(documentsDir: String, sharedDir: String? = null): DefaultSynonymPaths {
            return DefaultSynonymPaths(
                downloadDir = sharedDir ?: "$documentsDir/VoiceOS/synonyms",
                customDir = "$documentsDir/synonyms/custom"
            )
        }

        /**
         * Create paths for Desktop.
         *
         * @param appDataDir Application data directory
         */
        fun forDesktop(appDataDir: String): DefaultSynonymPaths {
            return DefaultSynonymPaths(
                downloadDir = "$appDataDir/VoiceOS/synonyms",
                customDir = "$appDataDir/synonyms/custom"
            )
        }
    }
}

/**
 * Platform-specific path provider.
 *
 * Implement this in platform source sets.
 */
expect object SynonymPathsProvider {
    /**
     * Get the platform-specific paths instance.
     */
    fun getPaths(): ISynonymPaths
}
