/**
 * VivokaEngineFactory.desktop.kt - Desktop actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-17
 *
 * Desktop implementation of Vivoka engine factory.
 * Vivoka is not available on Desktop - returns stub.
 */
package com.augmentalis.commandmanager

/**
 * Desktop Vivoka engine factory implementation.
 *
 * Vivoka SDK is not available on Desktop, always returns stub.
 */
actual object VivokaEngineFactory {
    /**
     * Vivoka is not available on Desktop.
     */
    actual fun isAvailable(): Boolean = false

    /**
     * Create Desktop-specific Vivoka engine (stub).
     *
     * @param config Vivoka configuration
     * @return IVivokaEngine stub that returns NotSupported
     */
    actual fun create(config: VivokaConfig): IVivokaEngine =
        StubVivokaEngine("Vivoka is not available on Desktop")
}
