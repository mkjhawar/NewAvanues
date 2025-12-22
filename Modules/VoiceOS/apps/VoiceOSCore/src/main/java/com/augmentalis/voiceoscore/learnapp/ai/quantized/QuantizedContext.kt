/**
 * QuantizedContext.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 */
package com.augmentalis.voiceoscore.learnapp.ai.quantized

data class QuantizedContext(
    val features: List<Float> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
)
