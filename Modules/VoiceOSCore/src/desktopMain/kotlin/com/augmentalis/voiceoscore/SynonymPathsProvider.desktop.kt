/**
 * SynonymPathsProvider.desktop.kt - Desktop actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 *
 * Desktop/JVM implementation of synonym paths provider.
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.synonym.DefaultSynonymPaths
import com.augmentalis.voiceoscore.synonym.ISynonymPaths

/**
 * Desktop synonym paths provider implementation.
 *
 * Provides paths for synonym storage on Desktop:
 * - Built-in: classpath resources (read-only)
 * - Downloaded: app data folder
 * - Custom: app data/synonyms/custom folder
 */
actual object SynonymPathsProvider {
    // Default paths for Desktop (can be overridden at runtime)
    private var _paths: ISynonymPaths? = null

    /**
     * Get Desktop-specific synonym paths.
     *
     * @return ISynonymPaths configured for Desktop storage locations
     */
    actual fun getPaths(): ISynonymPaths {
        return _paths ?: createDefaultPaths()
    }

    /**
     * Initialize paths with Desktop-derived directories.
     *
     * Should be called during app initialization with actual paths.
     *
     * @param appDataDir Application data directory
     */
    fun initialize(appDataDir: String) {
        _paths = DefaultSynonymPaths.forDesktop(appDataDir)
    }

    private fun createDefaultPaths(): ISynonymPaths {
        // Default fallback using user home directory
        val userHome = java.lang.System.getProperty("user.home") ?: "."
        val appDataDir = "$userHome/.voiceos"
        return DefaultSynonymPaths.forDesktop(appDataDir)
    }
}
