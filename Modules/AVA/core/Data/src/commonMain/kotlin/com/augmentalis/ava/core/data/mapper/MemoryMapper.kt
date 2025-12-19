package com.augmentalis.ava.core.data.mapper

import com.augmentalis.ava.core.domain.model.Memory
import com.augmentalis.ava.core.domain.model.MemoryType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.augmentalis.ava.core.data.db.Memory as DbMemory

private const val TAG = "MemoryMapper"

/**
 * Mapper functions for Domain Memory <-> SQLDelight Memory
 * Updated to use SQLDelight generated classes (Room removed)
 *
 * Uses binary BLOB for embeddings (60% space savings vs JSON)
 *
 * Issue 2.2 Fix: Added validation for corrupted BLOB data and MemoryType parsing
 */

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Convert SQLDelight Memory to Domain Memory.
 *
 * Issue 2.2 Fix: Added error handling for MemoryType parsing and BLOB validation.
 */
fun DbMemory.toDomain(): Memory {
    // Safely parse MemoryType with fallback
    val parsedMemoryType: MemoryType = try {
        MemoryType.valueOf(memory_type)
    } catch (e: IllegalArgumentException) {
        println("$TAG: Unknown memory_type '$memory_type' for memory $id, defaulting to CONTEXT")
        MemoryType.CONTEXT
    }

    return Memory(
        id = id,
        memoryType = parsedMemoryType,
        content = content,
        embedding = embedding?.let { bytesToFloatListSafe(it, id) },
        importance = importance.toFloat(),
        createdAt = created_at,
        lastAccessed = last_accessed,
        accessCount = access_count.toInt(),
        metadata = metadata?.let {
            try {
                json.decodeFromString<Map<String, String>>(it)
            } catch (e: Exception) {
                println("$TAG: Failed to parse metadata for memory $id: ${e.message}")
                null
            }
        }
    )
}

/**
 * Convert Domain Memory to SQLDelight insert parameters
 */
fun Memory.toInsertParams(): MemoryInsertParams {
    return MemoryInsertParams(
        id = id,
        memory_type = memoryType.name,
        content = content,
        embedding = embedding?.let { floatListToBytes(it) },
        importance = importance.toDouble(),
        created_at = createdAt,
        last_accessed = lastAccessed,
        access_count = accessCount.toLong(),
        metadata = metadata?.let { json.encodeToString(it) }
    )
}

/**
 * Parameters for inserting a memory via SQLDelight
 */
data class MemoryInsertParams(
    val id: String,
    val memory_type: String,
    val content: String,
    val embedding: ByteArray?,
    val importance: Double,
    val created_at: Long,
    val last_accessed: Long,
    val access_count: Long,
    val metadata: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as MemoryInsertParams
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

// Binary embedding conversion utilities

/**
 * Convert ByteArray (BLOB) to List<Float> with validation.
 * Each float is stored as 4 bytes in little-endian format.
 *
 * Issue 2.2 Fix: Added validation for:
 * - BLOB size divisibility by 4
 * - NaN/Infinity detection (indicates corruption)
 * - Exception handling for malformed data
 *
 * @param bytes The raw BLOB data
 * @param memoryId Memory ID for error logging
 * @return List of floats, or null if corrupted
 */
private fun bytesToFloatListSafe(bytes: ByteArray, memoryId: String): List<Float>? {
    if (bytes.isEmpty()) return emptyList()

    // Validate: must be divisible by 4 (float = 4 bytes)
    if (bytes.size % 4 != 0) {
        println("$TAG: Invalid embedding BLOB size ${bytes.size} for memory $memoryId (not divisible by 4)")
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
                println("$TAG: Invalid float value at index $i for memory $memoryId: $floatValue")
                return null
            }

            result.add(floatValue)
        }

        result
    } catch (e: Exception) {
        println("$TAG: Failed to parse embedding BLOB for memory $memoryId: ${e.message}")
        null
    }
}

/**
 * Convert List<Float> to ByteArray (BLOB)
 * Each float is stored as 4 bytes in little-endian format
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
