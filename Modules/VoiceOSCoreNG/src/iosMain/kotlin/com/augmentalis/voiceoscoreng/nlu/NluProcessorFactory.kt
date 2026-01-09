/**
 * NluProcessorFactory.kt - iOS NLU Processor Factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * iOS implementation of NluProcessorFactory.
 */
package com.augmentalis.voiceoscoreng.nlu

/**
 * iOS implementation of [NluProcessorFactory].
 */
actual object NluProcessorFactory {
    /**
     * Create an iOS NLU processor (stub).
     *
     * @param config NLU configuration
     * @return IOSNluProcessor stub instance
     */
    actual fun create(config: NluConfig): INluProcessor {
        return IOSNluProcessor(config)
    }
}
