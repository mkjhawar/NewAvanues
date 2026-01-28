package com.augmentalis.voiceoscore

import com.augmentalis.avid.Fingerprint
import com.augmentalis.avid.TypeCode

/**
 * ElementFingerprint - Generates deterministic element identifiers
 *
 * Wraps the AVID module's Fingerprint and TypeCode for UI element identification.
 *
 * Format: {TypeCode}:{hash8}
 * Example: BTN:a3f2e1c9
 *
 * This is deterministic - the same element always gets the same ID.
 * Unlike sequential AVIDs (AVID-A-000001), fingerprints are hash-based.
 */
object ElementFingerprint {

    private const val SEPARATOR = ":"

    /**
     * Generate a fingerprint for a UI element
     *
     * @param className Element class name (for type inference)
     * @param packageName App package name
     * @param resourceId Android resource ID (optional)
     * @param text Element text content (optional)
     * @param contentDesc Accessibility content description (optional)
     * @return Fingerprint string in format "BTN:a3f2e1c9"
     */
    @Suppress("UNUSED_PARAMETER") // Reserved for future package-specific fingerprinting
    fun generate(
        className: String,
        packageName: String,
        resourceId: String = "",
        text: String = "",
        contentDesc: String = ""
    ): String {
        val typeCode = TypeCode.fromTypeName(className)
        val hash = Fingerprint.forElement(
            type = className,
            resourceId = resourceId.ifBlank { null },
            name = text.ifBlank { null },
            contentDesc = contentDesc.ifBlank { null }
        )
        return "$typeCode$SEPARATOR$hash"
    }

    /**
     * Generate a fingerprint from ElementInfo
     */
    fun fromElementInfo(
        element: ElementInfo,
        packageName: String
    ): String {
        return generate(
            className = element.className,
            packageName = packageName,
            resourceId = element.resourceId,
            text = element.text,
            contentDesc = element.contentDescription
        )
    }

    /**
     * Get the type code for a class name
     */
    fun getTypeCode(className: String): String {
        return TypeCode.fromTypeName(className)
    }

    /**
     * Parse a fingerprint into its components
     *
     * @param fingerprint Fingerprint string (e.g., "BTN:a3f2e1c9")
     * @return Pair of (typeCode, hash) or null if invalid
     */
    fun parse(fingerprint: String): Pair<String, String>? {
        val parts = fingerprint.split(SEPARATOR)
        if (parts.size != 2) return null
        return Pair(parts[0], parts[1])
    }

    /**
     * Check if a string is a valid element fingerprint
     */
    fun isValid(fingerprint: String): Boolean {
        val parts = fingerprint.split(SEPARATOR)
        if (parts.size != 2) return false
        val typeCode = parts[0]
        val hash = parts[1]
        // Type code is 3 uppercase chars, hash is 8 hex chars
        return typeCode.length == 3 &&
               typeCode.all { it.isUpperCase() } &&
               hash.length == 8 &&
               hash.all { it.isDigit() || it in 'a'..'f' }
    }

    /**
     * Generate a random 8-character hex hash
     */
    fun randomHash8(): String = Fingerprint.randomHash8()

    /**
     * Generate a deterministic hash of specified length
     */
    fun deterministicHash(input: String, length: Int): String {
        return Fingerprint.deterministicHash(input, length)
    }
}
