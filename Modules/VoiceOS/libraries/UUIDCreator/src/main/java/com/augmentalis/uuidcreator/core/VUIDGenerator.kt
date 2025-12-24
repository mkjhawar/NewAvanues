package com.augmentalis.uuidcreator.core

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * VUID Generation utility
 *
 * Provides various VUID (VoiceUniqueID) generation strategies.
 * Uses standard UUID v4 format internally with Voice-specific terminology.
 *
 * Migration: UUID → VUID (VoiceUniqueID)
 * Created: 2025-12-23
 */
object VUIDGenerator {

    private val sequenceCounter = AtomicLong(0)

    /**
     * Generate standard random VUID (uses UUID v4 internally)
     */
    fun generate(): String = UUID.randomUUID().toString()

    /**
     * Generate VUID with prefix
     */
    fun generateWithPrefix(prefix: String): String = "${prefix}-${UUID.randomUUID()}"

    /**
     * Generate sequential VUID for predictable ordering
     */
    fun generateSequential(prefix: String = "seq"): String {
        val sequence = sequenceCounter.incrementAndGet()
        return "${prefix}-${sequence}-${System.currentTimeMillis()}"
    }

    /**
     * Generate VUID based on content hash
     */
    fun generateFromContent(content: String): String {
        val hash = content.hashCode().toString(16)
        return "content-${hash}-${System.currentTimeMillis()}"
    }

    /**
     * Generate VUID for specific element type
     */
    fun generateForType(type: String, name: String? = null): String {
        val suffix = name?.let { "-${it.replace(" ", "-").lowercase()}" } ?: ""
        return "${type.lowercase()}${suffix}-${UUID.randomUUID().toString().takeLast(8)}"
    }

    /**
     * Validate VUID format (accepts both VUID and UUID formats)
     */
    fun isValidVUID(vuid: String): Boolean {
        return try {
            UUID.fromString(vuid.replace(Regex("^[^-]+-"), ""))
            true
        } catch (e: IllegalArgumentException) {
            vuid.matches(Regex("^[a-zA-Z0-9-_]+$"))
        }
    }

    /**
     * Convert VUID to UUID format for backwards compatibility
     */
    @Deprecated("VUID and UUID use the same format")
    fun toUUID(vuid: String): String = vuid

    /**
     * Convert UUID to VUID format for migration
     */
    @Deprecated("UUID and VUID use the same format")
    fun fromUUID(uuid: String): String = uuid
}
