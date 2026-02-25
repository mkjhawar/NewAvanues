/**
 * ElementStateHistoryDTO.kt - DTO for element state history
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-25
 *
 * Data Transfer Object for element state changes.
 * Maps to ElementStateHistory.sq schema.
 */

package com.augmentalis.database.dto

import com.augmentalis.database.Element_state_history

/**
 * DTO for element state history
 *
 * Tracks state changes of UI elements over time.
 */
data class ElementStateHistoryDTO(
    val id: Long,
    val elementHash: String,
    val screenHash: String,
    val stateType: String,
    val oldValue: String?,
    val newValue: String?,
    val changedAt: Long,
    val triggeredBy: String
)

/**
 * Extension to convert SQLDelight generated type to DTO
 */
fun Element_state_history.toElementStateHistoryDTO(): ElementStateHistoryDTO {
    return ElementStateHistoryDTO(
        id = id,
        elementHash = elementHash,
        screenHash = screenHash,
        stateType = stateType,
        oldValue = oldValue,
        newValue = newValue,
        changedAt = changedAt,
        triggeredBy = triggeredBy
    )
}
