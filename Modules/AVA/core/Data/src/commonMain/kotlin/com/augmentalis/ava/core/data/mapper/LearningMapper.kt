package com.augmentalis.ava.core.data.mapper

import com.augmentalis.ava.core.domain.model.FeedbackType
import com.augmentalis.ava.core.domain.model.Learning
import com.augmentalis.ava.core.domain.model.Outcome
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.augmentalis.ava.core.data.db.Learning as DbLearning
import com.augmentalis.ava.core.data.db.SelectWithCorrections

/**
 * Mapper functions for Domain Learning <-> SQLDelight Learning
 * Updated to use SQLDelight generated classes (Room removed)
 */

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Convert SQLDelight Learning to Domain Learning
 */
fun DbLearning.toDomain(): Learning {
    return Learning(
        id = id,
        decisionId = decision_id,
        feedbackType = FeedbackType.valueOf(feedback_type),
        userCorrection = user_correction?.let {
            try {
                json.decodeFromString<Map<String, String>>(it)
            } catch (e: Exception) {
                null
            }
        },
        timestamp = timestamp,
        outcome = Outcome.valueOf(outcome),
        notes = notes
    )
}

/**
 * Convert SQLDelight SelectWithCorrections to Domain Learning
 * This handles the query result where user_correction is non-null
 */
fun SelectWithCorrections.toDomain(): Learning {
    return Learning(
        id = id,
        decisionId = decision_id,
        feedbackType = FeedbackType.valueOf(feedback_type),
        userCorrection = try {
            json.decodeFromString<Map<String, String>>(user_correction)
        } catch (e: Exception) {
            null
        },
        timestamp = timestamp,
        outcome = Outcome.valueOf(outcome),
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
