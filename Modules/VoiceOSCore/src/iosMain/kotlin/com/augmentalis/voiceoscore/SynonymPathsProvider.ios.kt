/**
 * SynonymPathsProvider.ios.kt - iOS actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-17
 *
 * iOS implementation of synonym paths provider.
 */
package com.augmentalis.voiceoscore

/**
 * iOS synonym paths provider implementation.
 *
 * Provides paths for synonym storage on iOS:
 * - Built-in: app bundle resources (read-only)
 * - Downloaded: Documents folder
 * - Custom: Documents/synonyms/custom folder
 */
actual object SynonymPathsProvider {
    // Default paths for iOS (can be overridden at runtime)
    private var _paths: ISynonymPaths? = null

    /**
     * Get iOS-specific synonym paths.
     *
     * @return ISynonymPaths configured for iOS storage locations
     */
    actual fun getPaths(): ISynonymPaths {
        return _paths ?: createDefaultPaths()
    }

    /**
     * Initialize paths with iOS-derived directories.
     *
     * Should be called during app initialization with actual paths.
     *
     * @param documentsDir App's documents directory
     * @param sharedDir App group shared directory (optional)
     */
    fun initialize(documentsDir: String, sharedDir: String? = null) {
        _paths = DefaultSynonymPaths.forIOS(documentsDir, sharedDir)
    }

    private fun createDefaultPaths(): ISynonymPaths {
        // Default fallback if not initialized
        // These paths may not work without proper initialization
        return DefaultSynonymPaths.forIOS(
            documentsDir = "Documents",
            sharedDir = null
        )
    }
}
