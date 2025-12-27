package com.augmentalis.uuidcreator.core

import com.augmentalis.uuidcreator.flutter.FlutterIdentifier
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * VUID Generation utility
 *
 * Provides various VUID (VoiceUniqueID) generation strategies.
 * Uses standard UUID v4 format internally with Voice-specific terminology.
 *
 * ## VUID Priority Order
 *
 * When generating VUIDs for UI elements, prefer in this order:
 * 1. Flutter 3.19+ identifier (stable across sessions)
 * 2. Android resource-id (viewIdResourceName)
 * 3. Content-based hash (text, contentDescription)
 * 4. Spatial coordinates (for game engines)
 *
 * Migration: UUID → VUID (VoiceUniqueID)
 * Created: 2025-12-23
 * Updated: 2025-12-27 - Added Flutter 3.19+ identifier support
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

    // ========================================================================
    // Flutter 3.19+ Identifier Support
    // ========================================================================

    /**
     * Generate stable VUID from Flutter 3.19+ identifier
     *
     * Flutter 3.19 introduced SemanticsProperties.identifier which maps to
     * Android's resource-id. This provides stable identification across sessions.
     *
     * @param flutterIdentifier The Flutter identifier extracted from node
     * @return Stable VUID based on the identifier
     */
    fun generateFromFlutterIdentifier(flutterIdentifier: FlutterIdentifier): String {
        return if (flutterIdentifier.isStable) {
            // Use stable hash for Flutter 3.19+ identifiers
            "flutter-${flutterIdentifier.toStableHash()}"
        } else {
            // Fall back to content-based generation for legacy Flutter
            generateFromContent(flutterIdentifier.identifierValue)
        }
    }

    /**
     * Generate stable VUID from raw Flutter resource ID
     *
     * Convenience method when you have the raw resource ID string.
     *
     * @param resourceId The viewIdResourceName from AccessibilityNodeInfo
     * @return Stable VUID if Flutter identifier detected, null otherwise
     */
    fun generateFromFlutterResourceId(resourceId: String): String? {
        // Quick check for Flutter patterns
        if (!resourceId.contains("flutter", ignoreCase = true)) {
            return null
        }

        // Check if this is a stable Flutter 3.19+ identifier
        val isStable = resourceId.contains("flutter_semantics_") ||
                resourceId.contains("flutter_id_")

        // Extract identifier value (remove package prefix if present)
        val identifierValue = if (resourceId.contains(":id/")) {
            resourceId.substringAfter(":id/")
        } else {
            resourceId
        }

        return if (isStable) {
            // Generate stable hash for Flutter 3.19+
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(identifierValue.toByteArray())
            val hash = hashBytes.take(16).joinToString("") { "%02x".format(it) }
            "flutter-$hash"
        } else {
            // Use content-based generation for legacy Flutter
            generateFromContent(identifierValue)
        }
    }
}
