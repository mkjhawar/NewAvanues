/**
 * SynonymPathsProvider.android.kt - Android actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-17
 *
 * Android implementation of synonym paths provider.
 */
package com.augmentalis.voiceoscore

/**
 * Android synonym paths provider implementation.
 *
 * Provides paths for synonym storage on Android:
 * - Built-in: app assets (read-only)
 * - Downloaded: external storage VoiceOS folder
 * - Custom: internal app storage
 */
actual object SynonymPathsProvider {
    // Default paths for Android (can be overridden at runtime)
    private var _paths: ISynonymPaths? = null

    /**
     * Get Android-specific synonym paths.
     *
     * @return ISynonymPaths configured for Android storage locations
     */
    actual fun getPaths(): ISynonymPaths {
        return _paths ?: createDefaultPaths()
    }

    /**
     * Initialize paths with Android context-derived directories.
     *
     * Should be called during app initialization with actual paths.
     *
     * @param filesDir App's internal files directory
     * @param externalDir Shared external directory (optional)
     */
    fun initialize(filesDir: String, externalDir: String? = null) {
        _paths = DefaultSynonymPaths.forAndroid(filesDir, externalDir)
    }

    private fun createDefaultPaths(): ISynonymPaths {
        // Default fallback if not initialized
        // These paths may not work without proper initialization
        return DefaultSynonymPaths.forAndroid(
            filesDir = "/data/user/0/com.augmentalis.voiceos/files",
            externalDir = "/storage/emulated/0/VoiceOS/synonyms"
        )
    }
}
