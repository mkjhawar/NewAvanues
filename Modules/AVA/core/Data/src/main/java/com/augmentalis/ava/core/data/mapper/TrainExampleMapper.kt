package com.augmentalis.ava.core.data.mapper

import com.augmentalis.ava.core.domain.model.TrainExample
import com.augmentalis.ava.core.domain.model.TrainExampleSource
import com.augmentalis.ava.core.data.db.Train_example as DbTrainExample

/**
 * Mapper functions for Domain TrainExample <-> SQLDelight Train_example
 * Updated to use SQLDelight generated classes (Room removed)
 */

/**
 * Convert SQLDelight Train_example to Domain TrainExample
 */
fun DbTrainExample.toDomain(): TrainExample {
    return TrainExample(
        id = id,
        exampleHash = example_hash,
        utterance = utterance,
        intent = intent,
        locale = locale,
        source = TrainExampleSource.valueOf(source),
        createdAt = created_at,
        usageCount = usage_count.toInt(),
        lastUsed = last_used
    )
}

/**
 * Convert Domain TrainExample to SQLDelight insert parameters
 */
fun TrainExample.toInsertParams(): TrainExampleInsertParams {
    return TrainExampleInsertParams(
        example_hash = exampleHash,
        utterance = utterance,
        intent = intent,
        locale = locale,
        source = source.name,
        created_at = createdAt,
        usage_count = usageCount.toLong(),
        last_used = lastUsed
    )
}

/**
 * Parameters for inserting a train example via SQLDelight
 */
data class TrainExampleInsertParams(
    val example_hash: String,
    val utterance: String,
    val intent: String,
    val locale: String,
    val source: String,
    val created_at: Long,
    val usage_count: Long,
    val last_used: Long?
)
