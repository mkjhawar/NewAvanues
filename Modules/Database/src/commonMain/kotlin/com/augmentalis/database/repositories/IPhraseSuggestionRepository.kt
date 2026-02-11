/*
 * Copyright (c) 2026 Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 * Created: 2026-02-11
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.PhraseSuggestionDTO

interface IPhraseSuggestionRepository {
    suspend fun getAll(): List<PhraseSuggestionDTO>
    suspend fun getPendingByLocale(locale: String): List<PhraseSuggestionDTO>
    suspend fun getForCommand(commandId: String): List<PhraseSuggestionDTO>
    suspend fun insert(dto: PhraseSuggestionDTO): Long
    suspend fun updateStatus(id: Long, status: String)
    suspend fun getPendingCount(): Long
    suspend fun deleteAll()
}
