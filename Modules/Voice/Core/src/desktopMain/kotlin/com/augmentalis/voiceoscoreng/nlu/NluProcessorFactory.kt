/**
 * NluProcessorFactory.kt - Desktop NLU Processor Factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Desktop (JVM) implementation of NluProcessorFactory.
 */
package com.augmentalis.voiceoscoreng.nlu

/**
 * Desktop implementation of [NluProcessorFactory].
 */
actual object NluProcessorFactory {
    /**
     * Create a Desktop NLU processor (stub).
     *
     * @param config NLU configuration
     * @return DesktopNluProcessor stub instance
     */
    actual fun create(config: NluConfig): INluProcessor {
        return DesktopNluProcessor(config)
    }
}
