package com.augmentalis.ava.core.data.mapper

import com.augmentalis.ava.core.domain.model.Decision
import com.augmentalis.ava.core.domain.model.DecisionType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.augmentalis.ava.core.data.db.Decision as DbDecision

/**
 * Mapper functions for Domain Decision <-> SQLDelight Decision
 * Updated to use SQLDelight generated classes (Room removed)
 */

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Convert SQLDelight Decision to Domain Decision
 */
fun DbDecision.toDomain(): Decision {
    return Decision(
        id = id,
        conversationId = conversation_id,
        decisionType = DecisionType.valueOf(decision_type),
        inputData = json.decodeFromString(input_data),
        outputData = json.decodeFromString(output_data),
        confidence = confidence.toFloat(),
        timestamp = timestamp,
        reasoning = reasoning
    )
}

/**
 * Convert Domain Decision to SQLDelight insert parameters
 */
fun Decision.toInsertParams(): DecisionInsertParams {
    return DecisionInsertParams(
        id = id,
        conversation_id = conversationId,
        decision_type = decisionType.name,
        input_data = json.encodeToString(inputData),
        output_data = json.encodeToString(outputData),
        confidence = confidence.toDouble(),
        timestamp = timestamp,
        reasoning = reasoning
    )
}

/**
 * Parameters for inserting a decision via SQLDelight
 */
data class DecisionInsertParams(
    val id: String,
    val conversation_id: String,
    val decision_type: String,
    val input_data: String,
    val output_data: String,
    val confidence: Double,
    val timestamp: Long,
    val reasoning: String?
)
