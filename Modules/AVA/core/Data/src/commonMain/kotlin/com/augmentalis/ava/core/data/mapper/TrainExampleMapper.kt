package com.augmentalis.ava.core.data.mapper

import com.augmentalis.ava.core.domain.model.TrainExample
import com.augmentalis.ava.core.domain.model.TrainExampleSource
import com.augmentalis.ava.core.data.db.Train_example as DbTrainExample

private const val TAG = "TrainExampleMapper"

/**
 * Mapper functions for Domain TrainExample <-> SQLDelight Train_example
 * Updated to use SQLDelight generated classes (Room removed)
 *
 * Issue 2.1/2.3 Fix: Uses fromDbValue() for source parsing, includes all ADR-013 fields
 */

/**
 * Convert SQLDelight Train_example to Domain TrainExample.
 *
 * Issue 2.1 Fix: Uses TrainExampleSource.fromDbValue() for backwards-compatible parsing.
 * Issue 2.3 Fix: Maps all ADR-013 self-learning fields.
 */
fun DbTrainExample.toDomain(): TrainExample {
    return TrainExample(
        id = id,
        exampleHash = example_hash,
        utterance = utterance,
        intent = intent,
        locale = locale,
        source = TrainExampleSource.fromDbValue(source),
        createdAt = created_at,
        usageCount = usage_count.toInt(),
        lastUsed = last_used,
        // ADR-013: Self-learning fields
        confidence = confidence.toFloat(),
        userConfirmed = user_confirmed != 0L,
        timesMatched = times_matched.toInt(),
        embeddingVector = embedding_vector?.let { bytesToFloatList(it) },
        embeddingDimension = embedding_dimension.toInt()
    )
}

/**
 * Convert Domain TrainExample to SQLDelight insert parameters.
 *
 * Issue 2.1 Fix: Uses dbValue for database storage.
 * Issue 2.3 Fix: Includes all ADR-013 self-learning fields.
 */
fun TrainExample.toInsertParams(): TrainExampleInsertParams {
    return TrainExampleInsertParams(
        example_hash = exampleHash,
        utterance = utterance,
        intent = intent,
        locale = locale,
        source = source.dbValue,
        created_at = createdAt,
        usage_count = usageCount.toLong(),
        last_used = lastUsed,
        // ADR-013: Self-learning fields
        confidence = confidence.toDouble(),
        user_confirmed = if (userConfirmed) 1L else 0L,
        times_matched = timesMatched.toLong(),
        embedding_vector = embeddingVector?.let { floatListToBytes(it) },
        embedding_dimension = embeddingDimension.toLong()
    )
}

/**
 * Parameters for inserting a train example via SQLDelight.
 * Includes all ADR-013 self-learning fields.
 */
data class TrainExampleInsertParams(
    val example_hash: String,
    val utterance: String,
    val intent: String,
    val locale: String,
    val source: String,
    val created_at: Long,
    val usage_count: Long,
    val last_used: Long?,
    // ADR-013: Self-learning fields
    val confidence: Double,
    val user_confirmed: Long,
    val times_matched: Long,
    val embedding_vector: ByteArray?,
    val embedding_dimension: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as TrainExampleInsertParams
        return example_hash == other.example_hash
    }

    override fun hashCode(): Int = example_hash.hashCode()
}

// ==================== Binary Embedding Utilities ====================

/**
 * Convert ByteArray (BLOB) to List<Float>.
 * Each float is stored as 4 bytes in little-endian format.
 *
 * Issue 2.2 Fix: Added validation for corrupted BLOB data.
 */
private fun bytesToFloatList(bytes: ByteArray): List<Float>? {
    if (bytes.isEmpty()) return emptyList()

    // Validate: must be divisible by 4 (float = 4 bytes)
    if (bytes.size % 4 != 0) {
        println("$TAG: Invalid embedding BLOB size ${bytes.size} (not divisible by 4)")
        return null
    }

    return try {
        val floatCount = bytes.size / 4
        val result = mutableListOf<Float>()

        for (i in 0 until floatCount) {
            val offset = i * 4
            val bits = (bytes[offset].toInt() and 0xFF) or
                       ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                       ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                       ((bytes[offset + 3].toInt() and 0xFF) shl 24)
            val floatValue = Float.fromBits(bits)

            // Validate: check for NaN/Infinity which indicate corruption
            if (floatValue.isNaN() || floatValue.isInfinite()) {
                println("$TAG: Invalid float value at index $i: $floatValue")
                return null
            }

            result.add(floatValue)
        }

        result
    } catch (e: Exception) {
        println("$TAG: Failed to parse embedding BLOB: ${e.message}")
        null
    }
}

/**
 * Convert List<Float> to ByteArray (BLOB).
 * Each float is stored as 4 bytes in little-endian format.
 */
private fun floatListToBytes(floats: List<Float>): ByteArray {
    if (floats.isEmpty()) return ByteArray(0)
    val bytes = ByteArray(floats.size * 4)

    floats.forEachIndexed { i, float ->
        val bits = float.toRawBits()
        val offset = i * 4
        bytes[offset] = (bits and 0xFF).toByte()
        bytes[offset + 1] = ((bits shr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((bits shr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((bits shr 24) and 0xFF).toByte()
    }

    return bytes
}
