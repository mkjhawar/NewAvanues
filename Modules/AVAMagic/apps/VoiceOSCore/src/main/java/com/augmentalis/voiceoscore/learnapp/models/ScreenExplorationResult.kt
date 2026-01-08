/**
 * ScreenExplorationResult.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 */
package com.augmentalis.voiceoscore.learnapp.models

sealed class ScreenExplorationResult {
    data class Success(
        val screenState: ScreenState,
        val elements: List<Any> = emptyList(),
        val commandsGenerated: Int = 0
    ) : ScreenExplorationResult()

    data class Failure(
        val error: String,
        val throwable: Throwable? = null
    ) : ScreenExplorationResult()
}
