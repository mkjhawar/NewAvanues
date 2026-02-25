// Author: Manoj Jhawar

package com.augmentalis.database.dto

import kotlinx.datetime.Clock

/**
 * Data Transfer Object for UserPreference.
 * Key-value storage for user settings.
 */
data class UserPreferenceDTO(
    val key: String,
    val value: String,
    val type: String = "STRING",
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Extension to convert SQLDelight entity to DTO.
 */
fun com.augmentalis.database.User_preference.toUserPreferenceDTO(): UserPreferenceDTO = UserPreferenceDTO(
    key = key,
    value = value_,
    type = type,
    updatedAt = updatedAt
)
