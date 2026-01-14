package com.augmentalis.ava.core.domain.repository

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.Decision
import com.augmentalis.ava.core.domain.model.DecisionType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for decision operations
 */
interface DecisionRepository {

    suspend fun logDecision(decision: Decision): Result<Decision>

    fun getDecisionsForConversation(conversationId: String): Flow<List<Decision>>

    fun getDecisionsByType(decisionType: DecisionType): Flow<List<Decision>>

    suspend fun getDecisionById(id: String): Result<Decision>

    fun getLowConfidenceDecisions(threshold: Float): Flow<List<Decision>>
}
