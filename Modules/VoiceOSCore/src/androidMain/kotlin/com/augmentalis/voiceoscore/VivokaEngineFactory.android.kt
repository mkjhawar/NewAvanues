/**
 * VivokaEngineFactory.android.kt - Android actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-17
 *
 * Android implementation of Vivoka engine factory.
 * TODO: Integrate with Vivoka SDK AAR.
 */
package com.augmentalis.voiceoscore

/**
 * Android Vivoka engine factory implementation.
 *
 * Creates stub Vivoka engine until SDK integration is complete.
 */
actual object VivokaEngineFactory {
    /**
     * Check if Vivoka is available on Android.
     * TODO: Check for Vivoka SDK presence.
     */
    actual fun isAvailable(): Boolean = false

    /**
     * Create Android-specific Vivoka engine.
     *
     * @param config Vivoka configuration
     * @return IVivokaEngine implementation (stub from commonMain for now)
     */
    actual fun create(config: VivokaConfig): IVivokaEngine =
        StubVivokaEngine("Vivoka SDK not available on Android")
}
