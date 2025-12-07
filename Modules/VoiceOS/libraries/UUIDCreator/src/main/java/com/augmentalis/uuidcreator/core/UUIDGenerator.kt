package com.augmentalis.uuidcreator.core

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * UUID Generation utility
 * Provides various UUID generation strategies
 */
object UUIDGenerator {
    
    private val sequenceCounter = AtomicLong(0)
    
    /**
     * Generate standard random UUID
     */
    fun generate(): String = UUID.randomUUID().toString()
    
    /**
     * Generate UUID with prefix
     */
    fun generateWithPrefix(prefix: String): String = "${prefix}-${UUID.randomUUID()}"
    
    /**
     * Generate sequential UUID for predictable ordering
     */
    fun generateSequential(prefix: String = "seq"): String {
        val sequence = sequenceCounter.incrementAndGet()
        return "${prefix}-${sequence}-${System.currentTimeMillis()}"
    }
    
    /**
     * Generate UUID based on content hash
     */
    fun generateFromContent(content: String): String {
        val hash = content.hashCode().toString(16)
        return "content-${hash}-${System.currentTimeMillis()}"
    }
    
    /**
     * Generate UUID for specific element type
     */
    fun generateForType(type: String, name: String? = null): String {
        val suffix = name?.let { "-${it.replace(" ", "-").lowercase()}" } ?: ""
        return "${type.lowercase()}${suffix}-${UUID.randomUUID().toString().takeLast(8)}"
    }
    
    /**
     * Validate UUID format
     */
    fun isValidUUID(uuid: String): Boolean {
        return try {
            UUID.fromString(uuid.replace(Regex("^[^-]+-"), ""))
            true
        } catch (e: IllegalArgumentException) {
            uuid.matches(Regex("^[a-zA-Z0-9-_]+$"))
        }
    }
}