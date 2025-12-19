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

private const val TAG = "DecisionMapper"

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Convert SQLDelight Decision to Domain Decision.
 *
 * Issue 1.3 Fix: Added try-catch for JSON deserialization to prevent crashes
 * from malformed JSON in input_data or output_data fields.
 */
fun DbDecision.toDomain(): Decision {
    // Safely parse inputData with fallback to empty map
    val parsedInputData: Map<String, String> = try {
        json.decodeFromString(input_data)
    } catch (e: Exception) {
        // Log error in KMP-compatible way (platform-specific logging should be injected)
        println("$TAG: Failed to parse input_data for decision $id: ${e.message}")
        emptyMap()
    }

    // Safely parse outputData with fallback to empty map
    val parsedOutputData: Map<String, String> = try {
        json.decodeFromString(output_data)
    } catch (e: Exception) {
        println("$TAG: Failed to parse output_data for decision $id: ${e.message}")
        emptyMap()
    }

    // Safely parse decisionType with fallback to UNKNOWN
    val parsedDecisionType: DecisionType = try {
        DecisionType.valueOf(decision_type)
    } catch (e: IllegalArgumentException) {
        println("$TAG: Unknown decision_type '$decision_type' for decision $id, defaulting to UNKNOWN")
        DecisionType.UNKNOWN
    }

    return Decision(
        id = id,
        conversationId = conversation_id,
        decisionType = parsedDecisionType,
        inputData = parsedInputData,
        outputData = parsedOutputData,
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
