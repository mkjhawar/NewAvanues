/**
 * VoiceOSCore.kt - Module Entry Point
 *
 * Unified KMP voice control module providing cross-platform voice command
 * processing, NLU, and accessibility features.
 *
 * Source Sets:
 * - commonMain: Cross-platform shared logic (result, hash, constants, validation,
 *   exceptions, command-models, accessibility-types, logging, text-utils,
 *   json-utils, database, synonym, llm, nlu, cursor, exploration, jit, e2e)
 * - androidMain: Android UI, services, accessibility, speech engines
 * - iosMain: iOS implementations
 * - desktopMain: Desktop implementations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 */

package com.augmentalis.voiceoscore

/**
 * VoiceOSCore module version and metadata.
 */
object VoiceOSCore {
    const val VERSION = "1.0.0"
    const val MODULE_NAME = "VoiceOSCore"

    /**
     * Platform-specific implementation provider.
     * Set by platform source sets (androidMain, iosMain, desktopMain).
     */
    var platformProvider: PlatformProvider? = null

    /**
     * Initialize the VoiceOSCore module.
     * Must be called before using any VoiceOS functionality.
     */
    fun initialize() {
        // Module initialization logic
        // Platform-specific initialization handled by PlatformProvider
    }
}

/**
 * Platform-specific functionality provider interface.
 * Implemented in androidMain, iosMain, and desktopMain.
 */
interface PlatformProvider {
    /**
     * Platform name (Android, iOS, Desktop).
     */
    val platformName: String

    /**
     * Initialize platform-specific components.
     */
    suspend fun initialize()

    /**
     * Cleanup platform-specific resources.
     */
    suspend fun cleanup()
}
