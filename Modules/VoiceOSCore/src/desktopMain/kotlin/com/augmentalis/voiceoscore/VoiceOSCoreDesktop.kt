/**
 * VoiceOSCoreDesktop.kt - Desktop Platform Implementation (Stub)
 *
 * Provides Desktop/JVM-specific initialization and functionality for VoiceOSCore.
 * Currently a stub - will be implemented when Desktop support is needed.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 */

package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.voice.PlatformProvider
import com.augmentalis.voiceoscore.voice.VoiceOSCore

/**
 * VoiceOSCoreDesktop - Desktop/JVM-specific platform implementation.
 *
 * Entry point for Desktop apps using VoiceOSCore. Will handle:
 * - JVM speech recognition (Vosk JNI, etc.)
 * - Desktop accessibility features (Java Access Bridge)
 * - SQLDelight SQLite driver (JVM)
 * - Compose Desktop UI
 *
 * Usage:
 * ```kotlin
 * // In main()
 * VoiceOSCoreDesktop.init()
 * ```
 */
class VoiceOSCoreDesktop : PlatformProvider {

    override val platformName: String = "Desktop"

    override suspend fun initialize() {
        // TODO: Initialize Desktop-specific components:
        // - JVM speech recognition (Vosk JNI, etc.)
        // - Desktop accessibility features (Java Access Bridge)
        // - SQLDelight SQLite driver
        // - Compose Desktop UI integration
    }

    override suspend fun cleanup() {
        // Cleanup Desktop resources
    }

    companion object {
        private var instance: VoiceOSCoreDesktop? = null

        /**
         * Initialize VoiceOSCore for Desktop.
         */
        fun init(): VoiceOSCoreDesktop {
            return instance ?: VoiceOSCoreDesktop().also {
                instance = it
                VoiceOSCore.platformProvider = it
            }
        }

        /**
         * Get the singleton instance.
         * @throws IllegalStateException if init() hasn't been called
         */
        fun getInstance(): VoiceOSCoreDesktop {
            return instance ?: throw IllegalStateException(
                "VoiceOSCoreDesktop not initialized. Call init() first."
            )
        }
    }
}
