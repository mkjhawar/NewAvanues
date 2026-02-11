/*
 * Copyright (c) 2026 Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 * Created: 2026-02-11
 */

package com.augmentalis.database.dto

data class PhraseSuggestionDTO(
    val id: Long,
    val commandId: String,
    val originalPhrase: String,
    val suggestedPhrase: String,
    val locale: String,
    val createdAt: Long,
    val status: String,
    val source: String
)
