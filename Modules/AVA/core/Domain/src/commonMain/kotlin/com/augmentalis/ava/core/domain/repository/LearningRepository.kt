package com.augmentalis.ava.core.domain.repository

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.FeedbackType
import com.augmentalis.ava.core.domain.model.Learning
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for learning operations
 */
interface LearningRepository {

    suspend fun logFeedback(learning: Learning): Result<Learning>

    fun getLearningForDecision(decisionId: String): Flow<List<Learning>>

    fun getLearningByFeedbackType(feedbackType: FeedbackType): Flow<List<Learning>>

    fun getCorrections(): Flow<List<Learning>>
}
