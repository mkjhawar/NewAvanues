/**
 * VivokaEngineFactory.ios.kt - iOS actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-17
 *
 * iOS implementation of Vivoka engine factory.
 * Vivoka is not available on iOS - returns stub.
 */
package com.augmentalis.voiceoscore

/**
 * iOS Vivoka engine factory implementation.
 *
 * Vivoka SDK is not available on iOS, always returns stub.
 */
actual object VivokaEngineFactory {
    /**
     * Vivoka is not available on iOS.
     */
    actual fun isAvailable(): Boolean = false

    /**
     * Create iOS-specific Vivoka engine (stub).
     *
     * @param config Vivoka configuration
     * @return IVivokaEngine stub that returns NotSupported
     */
    actual fun create(config: VivokaConfig): IVivokaEngine =
        StubVivokaEngine("Vivoka is not available on iOS")
}
