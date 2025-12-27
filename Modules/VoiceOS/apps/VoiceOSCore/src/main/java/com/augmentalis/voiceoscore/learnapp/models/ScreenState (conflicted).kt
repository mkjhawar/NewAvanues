/**
 * ScreenState.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 */
package com.augmentalis.voiceoscore.learnapp.models

data class ScreenState(
    val hash: String,
    val packageName: String,
    val activityName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val elementCount: Int = 0,
    val isVisited: Boolean = false,
    val depth: Int = 0,
    val elements: List<Any> = emptyList(),
    val allElements: List<ElementInfo> = emptyList(),
    val totalElements: Int = 0
) {
    /**
     * Mark screen as visited
     *
     * @return New screen state with isVisited = true
     */
    fun markAsVisited(): ScreenState = copy(isVisited = true)
}
