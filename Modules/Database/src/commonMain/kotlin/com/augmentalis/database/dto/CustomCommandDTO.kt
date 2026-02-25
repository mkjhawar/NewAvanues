// Author: Manoj Jhawar

package com.augmentalis.database.dto

import kotlinx.datetime.Clock

/**
 * Data Transfer Object for CustomCommand.
 * Platform-agnostic representation used by repository layer.
 */
data class CustomCommandDTO(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val phrases: List<String>,
    val action: String,
    val parameters: String? = null,
    val language: String = "en",
    val isActive: Boolean = true,
    val usageCount: Long = 0,
    val lastUsed: Long? = null,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Extension to convert SQLDelight entity to DTO.
 */
fun com.augmentalis.database.Custom_command.toCustomCommandDTO(): CustomCommandDTO = CustomCommandDTO(
    id = id,
    name = name,
    description = description,
    phrases = phrases.split("|").filter { it.isNotEmpty() },
    action = action,
    parameters = parameters,
    language = language,
    isActive = isActive == 1L,
    usageCount = usageCount,
    lastUsed = lastUsed,
    createdAt = createdAt,
    updatedAt = updatedAt
)
