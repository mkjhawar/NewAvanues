package com.augmentalis.ava.core.data.repository

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.db.DecisionQueries
import com.augmentalis.ava.core.data.mapper.toDomain
import com.augmentalis.ava.core.data.mapper.toInsertParams
import com.augmentalis.ava.core.domain.model.Decision
import com.augmentalis.ava.core.domain.model.DecisionType
import com.augmentalis.ava.core.domain.repository.DecisionRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementation of DecisionRepository using SQLDelight
 *
 * Updated: Room removed, now uses SQLDelight queries directly
 */
class DecisionRepositoryImpl(
    private val decisionQueries: DecisionQueries
) : DecisionRepository {

    override suspend fun logDecision(decision: Decision): Result<Decision> = withContext(Dispatchers.IO) {
        try {
            val params = decision.toInsertParams()
            decisionQueries.insert(
                id = params.id,
                conversation_id = params.conversation_id,
                decision_type = params.decision_type,
                input_data = params.input_data,
                output_data = params.output_data,
                confidence = params.confidence,
                timestamp = params.timestamp,
                reasoning = params.reasoning
            )
            Result.Success(decision)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to log decision: ${e.message}"
            )
        }
    }

    override fun getDecisionsForConversation(conversationId: String): Flow<List<Decision>> {
        return decisionQueries.selectByConversationId(conversationId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { decisions -> decisions.map { it.toDomain() } }
    }

    override fun getDecisionsByType(decisionType: DecisionType): Flow<List<Decision>> {
        return decisionQueries.selectByDecisionType(decisionType.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { decisions -> decisions.map { it.toDomain() } }
    }

    override suspend fun getDecisionById(id: String): Result<Decision> = withContext(Dispatchers.IO) {
        try {
            val decision = decisionQueries.selectById(id).executeAsOneOrNull()
            if (decision != null) {
                Result.Success(decision.toDomain())
            } else {
                Result.Error(
                    exception = NoSuchElementException("Decision not found"),
                    message = "Decision with id $id not found"
                )
            }
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to get decision: ${e.message}"
            )
        }
    }

    override fun getLowConfidenceDecisions(threshold: Float): Flow<List<Decision>> {
        return decisionQueries.selectLowConfidence(threshold.toDouble())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { decisions -> decisions.map { it.toDomain() } }
    }
}
