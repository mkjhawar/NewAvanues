/**
 * ScreenTransitionDTO.kt - DTO for screen navigation transitions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 *
 * Data Transfer Object for screen transitions.
 * Maps to ScreenTransition.sq schema.
 */

package com.avanues.database.dto

import com.avanues.database.Screen_transition

/**
 * DTO for screen transitions
 *
 * Tracks navigation patterns between screens.
 */
data class ScreenTransitionDTO(
    val id: Long,
    val fromScreenHash: String,
    val toScreenHash: String,
    val triggerElementHash: String?,
    val triggerAction: String,
    val transitionCount: Long = 1,
    val avgDurationMs: Long = 0,
    val lastTransitionAt: Long
)

/**
 * Extension to convert SQLDelight generated type to DTO
 */
fun Screen_transition.toScreenTransitionDTO(): ScreenTransitionDTO {
    return ScreenTransitionDTO(
        id = id,
        fromScreenHash = fromScreenHash,
        toScreenHash = toScreenHash,
        triggerElementHash = triggerElementHash,
        triggerAction = triggerAction,
        transitionCount = transitionCount,
        avgDurationMs = avgDurationMs,
        lastTransitionAt = lastTransitionAt
    )
}
