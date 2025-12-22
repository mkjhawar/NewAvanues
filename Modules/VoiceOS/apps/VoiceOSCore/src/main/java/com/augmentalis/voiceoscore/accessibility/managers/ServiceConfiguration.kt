/**
 * ServiceConfiguration.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 */
package com.augmentalis.voiceoscore.accessibility.managers

data class ServiceConfiguration(
    val enabled: Boolean = true,
    val verboseLogging: Boolean = false,
    val autoStart: Boolean = true,
    val features: Map<String, Boolean> = emptyMap()
)
