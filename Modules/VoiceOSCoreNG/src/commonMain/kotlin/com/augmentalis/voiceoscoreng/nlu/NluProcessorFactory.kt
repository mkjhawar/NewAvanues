/**
 * NluProcessorFactory.kt - Factory for creating NLU processors
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Platform-specific factory for creating INluProcessor instances.
 */
package com.augmentalis.voiceoscoreng.nlu

/**
 * Factory for creating platform-specific NLU processors.
 *
 * Platform implementations:
 * - Android: AndroidNluProcessor (wraps IntentClassifier with ONNX/BERT)
 * - iOS: IOSNluProcessor (stub, TODO: CoreML)
 * - Desktop: DesktopNluProcessor (stub, TODO: ONNX JVM)
 */
expect object NluProcessorFactory {
    /**
     * Create a platform-specific NLU processor.
     *
     * @param config NLU configuration
     * @return Platform-specific INluProcessor implementation
     */
    fun create(config: NluConfig = NluConfig.DEFAULT): INluProcessor
}
