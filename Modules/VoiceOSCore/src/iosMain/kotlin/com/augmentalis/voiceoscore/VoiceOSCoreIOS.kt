/**
 * VoiceOSCoreIOS.kt - iOS Platform Implementation (Stub)
 *
 * Provides iOS-specific initialization and functionality for VoiceOSCore.
 * Currently a stub - will be implemented when iOS support is needed.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 */

package com.augmentalis.voiceoscore

/**
 * VoiceOSCoreIOS - iOS-specific platform implementation.
 *
 * Entry point for iOS apps using VoiceOSCore. Will handle:
 * - Apple Speech Framework integration
 * - iOS Accessibility features
 * - SQLDelight native driver (iOS)
 * - SwiftUI integration
 *
 * Usage (Swift):
 * ```swift
 * // In AppDelegate
 * VoiceOSCoreIOS.shared.initialize()
 * ```
 */
class VoiceOSCoreIOS : PlatformProvider {

    override val platformName: String = "iOS"

    override suspend fun initialize() {
        // TODO: Initialize iOS-specific components:
        // - Apple Speech Framework
        // - iOS Accessibility features
        // - SQLDelight native driver
        // - SwiftUI bridges
    }

    override suspend fun cleanup() {
        // Cleanup iOS resources
    }

    companion object {
        private var instance: VoiceOSCoreIOS? = null

        /**
         * Shared instance for iOS.
         */
        val shared: VoiceOSCoreIOS
            get() = instance ?: VoiceOSCoreIOS().also {
                instance = it
                VoiceOSCore.platformProvider = it
            }

        /**
         * Initialize VoiceOSCore for iOS.
         */
        fun init(): VoiceOSCoreIOS = shared
    }
}
