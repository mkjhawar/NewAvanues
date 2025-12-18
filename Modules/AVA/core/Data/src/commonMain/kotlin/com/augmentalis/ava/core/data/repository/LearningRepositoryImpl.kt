package com.augmentalis.ava.core.data.repository

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.db.LearningQueries
import com.augmentalis.ava.core.data.mapper.toDomain
import com.augmentalis.ava.core.data.mapper.toInsertParams
import com.augmentalis.ava.core.domain.model.FeedbackType
import com.augmentalis.ava.core.domain.model.Learning
import com.augmentalis.ava.core.domain.repository.LearningRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementation of LearningRepository using SQLDelight
 *
 * Updated: Room removed, now uses SQLDelight queries directly
 */
class LearningRepositoryImpl(
    private val learningQueries: LearningQueries
) : LearningRepository {

    override suspend fun logFeedback(learning: Learning): Result<Learning> = withContext(Dispatchers.IO) {
        try {
            val params = learning.toInsertParams()
            learningQueries.insert(
                id = params.id,
                decision_id = params.decision_id,
                feedback_type = params.feedback_type,
                user_correction = params.user_correction,
                timestamp = params.timestamp,
                outcome = params.outcome,
                notes = params.notes
            )
            Result.Success(learning)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to log feedback: ${e.message}"
            )
        }
    }

    override fun getLearningForDecision(decisionId: String): Flow<List<Learning>> {
        return learningQueries.selectByDecisionId(decisionId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { learnings -> learnings.map { it.toDomain() } }
    }

    override fun getLearningByFeedbackType(feedbackType: FeedbackType): Flow<List<Learning>> {
        return learningQueries.selectByFeedbackType(feedbackType.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { learnings -> learnings.map { it.toDomain() } }
    }

    override fun getCorrections(): Flow<List<Learning>> {
        return learningQueries.selectWithCorrections()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { learnings -> learnings.map { it.toDomain() } }
    }
}
