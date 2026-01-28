/**
 * VivokaEngineFactory.kt - Factory for Vivoka engine instances
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Platform-specific factory for creating Vivoka engine instances.
 */
package com.augmentalis.commandmanager

/**
 * Factory for creating Vivoka engine instances.
 *
 * Platform implementations:
 * - Android: Creates real VivokaEngine using Vivoka SDK
 * - iOS/Desktop: Creates stub that returns NotSupported
 */
expect object VivokaEngineFactory {

    /**
     * Check if Vivoka is available on this platform.
     */
    fun isAvailable(): Boolean

    /**
     * Create a Vivoka engine instance.
     *
     * @param config Vivoka configuration
     * @return IVivokaEngine instance (may be stub on unsupported platforms)
     */
    fun create(config: VivokaConfig = VivokaConfig.DEFAULT): IVivokaEngine
}
