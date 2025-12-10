/**
 * UserInteractionDTO.kt - DTO for user interaction tracking
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 *
 * Data Transfer Object for user interactions.
 * Maps to UserInteraction.sq schema.
 */

package com.avanues.database.dto

import com.avanues.database.User_interaction

/**
 * DTO for user interactions
 *
 * Tracks user interactions with UI elements for analytics and learning.
 */
data class UserInteractionDTO(
    val id: Long,
    val elementHash: String,
    val screenHash: String,
    val interactionType: String,
    val interactionTime: Long,
    val visibilityStart: Long?,
    val visibilityDuration: Long?
)

/**
 * Extension to convert SQLDelight generated type to DTO
 */
fun User_interaction.toUserInteractionDTO(): UserInteractionDTO {
    return UserInteractionDTO(
        id = id,
        elementHash = elementHash,
        screenHash = screenHash,
        interactionType = interactionType,
        interactionTime = interactionTime,
        visibilityStart = visibilityStart,
        visibilityDuration = visibilityDuration
    )
}
