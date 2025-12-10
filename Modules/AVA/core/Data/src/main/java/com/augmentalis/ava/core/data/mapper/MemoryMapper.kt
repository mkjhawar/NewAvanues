package com.augmentalis.ava.core.data.mapper

import com.augmentalis.ava.core.domain.model.Memory
import com.augmentalis.ava.core.domain.model.MemoryType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.augmentalis.ava.core.data.db.Memory as DbMemory

/**
 * Mapper functions for Domain Memory <-> SQLDelight Memory
 * Updated to use SQLDelight generated classes (Room removed)
 *
 * Uses binary BLOB for embeddings (60% space savings vs JSON)
 */

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Convert SQLDelight Memory to Domain Memory
 */
fun DbMemory.toDomain(): Memory {
    return Memory(
        id = id,
        memoryType = MemoryType.valueOf(memory_type),
        content = content,
        embedding = embedding?.let { bytesToFloatList(it) },
        importance = importance.toFloat(),
        createdAt = created_at,
        lastAccessed = last_accessed,
        accessCount = access_count.toInt(),
        metadata = metadata?.let {
            try {
                json.decodeFromString<Map<String, String>>(it)
            } catch (e: Exception) {
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
        if (other !is MemoryInsertParams) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

// Binary embedding conversion utilities

/**
 * Convert ByteArray (BLOB) to List<Float>
 * Each float is stored as 4 bytes in little-endian format
 */
private fun bytesToFloatList(bytes: ByteArray): List<Float> {
    if (bytes.isEmpty()) return emptyList()
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val floatCount = bytes.size / 4
    return List(floatCount) { buffer.getFloat() }
}

/**
 * Convert List<Float> to ByteArray (BLOB)
 * Each float is stored as 4 bytes in little-endian format
 */
private fun floatListToBytes(floats: List<Float>): ByteArray {
    if (floats.isEmpty()) return ByteArray(0)
    val buffer = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    floats.forEach { buffer.putFloat(it) }
    return buffer.array()
}
