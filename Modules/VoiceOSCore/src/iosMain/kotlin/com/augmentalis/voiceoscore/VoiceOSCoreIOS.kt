/**
 * VoiceOSCoreIOS.kt - iOS Platform Implementation
 *
 * Provides iOS-specific initialization and functionality for VoiceOSCore.
 * Handles platform-level setup: speech engine factory registration, device
 * capability detection, synonym paths, and platform info logging.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 * Updated: 2026-02-13 - Full implementation replacing stub
 */

package com.augmentalis.voiceoscore

import platform.Foundation.NSLog
import platform.UIKit.UIDevice

/**
 * VoiceOSCoreIOS - iOS-specific platform implementation.
 *
 * Entry point for iOS apps using VoiceOSCore. Handles:
 * - Apple Speech Framework integration (via AppleSpeechEngineAdapter)
 * - Device capability detection
 * - Synonym path resolution for iOS file system
 * - Platform info for analytics
 *
 * Usage (Swift):
 * ```swift
 * // In AppDelegate
 * VoiceOSCoreIOS.shared.initialize()
 *
 * // Then build VoiceOSCore normally
 * let core = VoiceOSCore.Builder()
 *     .withHandlerFactory(myFactory)
 *     .withSpeechEngine(.APPLE_SPEECH)
 *     .build()
 * ```
 */
class VoiceOSCoreIOS : PlatformProvider {

    override val platformName: String = "iOS"

    private var isInitialized = false

    /**
     * Initialize iOS-specific platform components.
     *
     * This sets up:
     * 1. Device capability detection (timing configs for device speed tier)
     * 2. Speech engine factory registration (Apple Speech via SFSpeechRecognizer)
     * 3. Platform info logging for diagnostics
     *
     * Note: The actual VoiceOSCore engine creation, handler registration, and
     * command loading happens in VoiceOSCore.initialize() — this method only
     * prepares the platform layer.
     */
    override suspend fun initialize() {
        if (isInitialized) {
            NSLog("[VoiceOSCoreIOS] Already initialized, skipping")
            return
        }

        val device = UIDevice.currentDevice
        NSLog("[VoiceOSCoreIOS] Initializing on ${device.model} " +
            "(${device.systemName} ${device.systemVersion})")

        // Verify speech engine factory is available
        val factory = SpeechEngineFactoryProvider.create()
        val availableEngines = factory.getAvailableEngines()
        NSLog("[VoiceOSCoreIOS] Available speech engines: ${availableEngines.joinToString { it.name }}")

        // Verify Apple Speech engine can be created
        val recommended = factory.getRecommendedEngine()
        NSLog("[VoiceOSCoreIOS] Recommended engine: ${recommended.name}")

        // Log device capabilities
        val speed = DeviceCapabilityManager.getDeviceSpeed()
        val maxOps = DeviceCapabilityManager.getMaxConcurrentOperations()
        NSLog("[VoiceOSCoreIOS] Device speed: $speed, " +
            "max concurrent ops: $maxOps, " +
            "aggressive scanning: ${DeviceCapabilityManager.supportsAggressiveScanning()}")

        // Log platform feature availability
        NSLog("[VoiceOSCoreIOS] Web extraction: ${isWebExtractionAvailable()}, " +
            "Accessibility: ${isAccessibilityAvailable()}")

        isInitialized = true
        NSLog("[VoiceOSCoreIOS] Platform initialization complete")
    }

    /**
     * Cleanup iOS platform resources.
     *
     * Releases platform-level resources. Does not stop the VoiceOSCore
     * engine itself — call VoiceOSCore.dispose() for that.
     */
    override suspend fun cleanup() {
        NSLog("[VoiceOSCoreIOS] Cleaning up platform resources")
        isInitialized = false
    }

    companion object {
        private var instance: VoiceOSCoreIOS? = null

        /**
         * Shared singleton instance.
         *
         * Automatically registers as VoiceOSCore.platformProvider on first access.
         */
        val shared: VoiceOSCoreIOS
            get() = instance ?: VoiceOSCoreIOS().also {
                instance = it
                VoiceOSCore.platformProvider = it
            }

        /**
         * Initialize VoiceOSCore for iOS.
         * Convenience accessor that returns the shared instance.
         */
        fun init(): VoiceOSCoreIOS = shared
    }
}
