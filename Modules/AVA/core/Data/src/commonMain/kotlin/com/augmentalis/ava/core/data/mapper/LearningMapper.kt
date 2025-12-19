package com.augmentalis.ava.core.data.mapper

import com.augmentalis.ava.core.domain.model.FeedbackType
import com.augmentalis.ava.core.domain.model.Learning
import com.augmentalis.ava.core.domain.model.Outcome
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.augmentalis.ava.core.data.db.Learning as DbLearning
import com.augmentalis.ava.core.data.db.SelectWithCorrections

private const val TAG = "LearningMapper"

/**
 * Mapper functions for Domain Learning <-> SQLDelight Learning
 * Updated to use SQLDelight generated classes (Room removed)
 *
 * Issue 2.4 Fix: Added error logging for enum parsing failures
 */

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Convert SQLDelight Learning to Domain Learning.
 *
 * Issue 2.4 Fix: Added error handling for FeedbackType and Outcome parsing.
 */
fun DbLearning.toDomain(): Learning {
    // Safely parse FeedbackType with fallback
    val parsedFeedbackType: FeedbackType = try {
        FeedbackType.valueOf(feedback_type)
    } catch (e: IllegalArgumentException) {
        println("$TAG: Unknown feedback_type '$feedback_type' for learning $id, defaulting to CORRECTION")
        FeedbackType.CORRECTION
    }

    // Safely parse Outcome with fallback
    val parsedOutcome: Outcome = try {
        Outcome.valueOf(outcome)
    } catch (e: IllegalArgumentException) {
        println("$TAG: Unknown outcome '$outcome' for learning $id, defaulting to PARTIAL")
        Outcome.PARTIAL
    }

    return Learning(
        id = id,
        decisionId = decision_id,
        feedbackType = parsedFeedbackType,
        userCorrection = user_correction?.let {
            try {
                json.decodeFromString<Map<String, String>>(it)
            } catch (e: Exception) {
                println("$TAG: Failed to parse user_correction for learning $id: ${e.message}")
                null
            }
        },
        timestamp = timestamp,
        outcome = parsedOutcome,
        notes = notes
    )
}

/**
 * Convert SQLDelight SelectWithCorrections to Domain Learning.
 * This handles the query result where user_correction is non-null.
 *
 * Issue 2.4 Fix: Added error handling for FeedbackType and Outcome parsing.
 */
fun SelectWithCorrections.toDomain(): Learning {
    // Safely parse FeedbackType with fallback
    val parsedFeedbackType: FeedbackType = try {
        FeedbackType.valueOf(feedback_type)
    } catch (e: IllegalArgumentException) {
        println("$TAG: Unknown feedback_type '$feedback_type' for learning $id, defaulting to CORRECTION")
        FeedbackType.CORRECTION
    }

    // Safely parse Outcome with fallback
    val parsedOutcome: Outcome = try {
        Outcome.valueOf(outcome)
    } catch (e: IllegalArgumentException) {
        println("$TAG: Unknown outcome '$outcome' for learning $id, defaulting to PARTIAL")
        Outcome.PARTIAL
    }

    return Learning(
        id = id,
        decisionId = decision_id,
        feedbackType = parsedFeedbackType,
        userCorrection = try {
            json.decodeFromString<Map<String, String>>(user_correction)
        } catch (e: Exception) {
            println("$TAG: Failed to parse user_correction for learning $id: ${e.message}")
            null
        },
        timestamp = timestamp,
        outcome = parsedOutcome,
        notes = notes
    )
}

/**
 * Convert Domain Learning to SQLDelight insert parameters
 */
fun Learning.toInsertParams(): LearningInsertParams {
    return LearningInsertParams(
        id = id,
        decision_id = decisionId,
        feedback_type = feedbackType.name,
        user_correction = userCorrection?.let { json.encodeToString(it) },
        timestamp = timestamp,
        outcome = outcome.name,
        notes = notes
    )
}

/**
 * Parameters for inserting a learning record via SQLDelight
 */
data class LearningInsertParams(
    val id: String,
    val decision_id: String,
    val feedback_type: String,
    val user_correction: String?,
    val timestamp: Long,
    val outcome: String,
    val notes: String?
)
