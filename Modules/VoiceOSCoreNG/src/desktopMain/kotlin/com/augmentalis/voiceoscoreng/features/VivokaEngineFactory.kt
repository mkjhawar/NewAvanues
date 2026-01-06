/**
 * VivokaEngineFactory.kt - Desktop Vivoka factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Desktop (JVM) implementation of Vivoka engine factory.
 * Returns stub since Vivoka SDK is Android-only.
 */
package com.augmentalis.voiceoscoreng.features

/**
 * Desktop Vivoka engine factory.
 * Returns stub engine since Vivoka is not available on Desktop.
 */
actual object VivokaEngineFactory {

    /**
     * Vivoka is not available on Desktop.
     */
    actual fun isAvailable(): Boolean = false

    /**
     * Create stub Vivoka engine.
     */
    actual fun create(config: VivokaConfig): IVivokaEngine {
        return StubVivokaEngine("Vivoka SDK is not available on Desktop. Use Vosk or platform speech instead.")
    }
}
