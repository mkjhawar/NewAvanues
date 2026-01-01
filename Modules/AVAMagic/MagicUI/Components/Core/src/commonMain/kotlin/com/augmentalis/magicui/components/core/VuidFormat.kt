package com.augmentalis.magicui.components.core

/**
 * VUID (Voice Unique Identifier) format utilities
 *
 * Supports two formats:
 * 1. Legacy: Human-readable names like "button-submit", "login-email-field"
 * 2. Compact: Hash-based format like "a3f2e1-b917cc9dc" (68% smaller)
 *
 * Compact format structure:
 * - {pkgHash6}-{typeCode}{hash8}
 * - Total: 16 characters
 * - Example: a3f2e1-b917cc9dc
 */
object VuidFormat {

    /**
     * Type codes for compact VUIDs
     */
    object TypeCodes {
        const val BUTTON = 'b'
        const val INPUT = 'i'
        const val SCROLL = 's'
        const val TEXT = 't'
        const val ELEMENT = 'e'
        const val CARD = 'c'
        const val LAYOUT = 'l'
        const val MENU = 'm'
        const val DIALOG = 'd'
        const val IMAGE = 'g'

        fun fromType(type: String): Char = when (type.lowercase()) {
            "button" -> BUTTON
            "input", "textfield", "edittext" -> INPUT
            "scroll", "scrollview", "list" -> SCROLL
            "text", "label" -> TEXT
            "card" -> CARD
            "layout", "container", "column", "row", "box" -> LAYOUT
            "menu", "menuitem" -> MENU
            "dialog", "modal", "sheet" -> DIALOG
            "image", "icon" -> IMAGE
            else -> ELEMENT
        }

        fun toTypeName(code: Char): String? = when (code) {
            BUTTON -> "button"
            INPUT -> "input"
            SCROLL -> "scroll"
            TEXT -> "text"
            ELEMENT -> "element"
            CARD -> "card"
            LAYOUT -> "layout"
            MENU -> "menu"
            DIALOG -> "dialog"
            IMAGE -> "image"
            else -> null
        }
    }

    // Regex patterns
    private val LEGACY_PATTERN = Regex("^[a-z][a-z0-9-]*[a-z0-9]$")
    private val COMPACT_PATTERN = Regex("^[a-f0-9]{6}-[a-z][a-f0-9]{8}$")

    /**
     * Validate VUID format (legacy or compact)
     */
    fun isValid(vuid: String): Boolean = when {
        vuid.length < 3 -> false
        vuid.length > 64 -> false
        isCompact(vuid) -> true
        isLegacy(vuid) -> true
        else -> false
    }

    /**
     * Check if VUID is in compact format
     */
    fun isCompact(vuid: String): Boolean = COMPACT_PATTERN.matches(vuid)

    /**
     * Check if VUID is in legacy format
     */
    fun isLegacy(vuid: String): Boolean = LEGACY_PATTERN.matches(vuid)

    /**
     * Get type name from compact VUID
     */
    fun getTypeFromCompact(vuid: String): String? {
        if (!isCompact(vuid)) return null
        val typeCode = vuid.getOrNull(7) ?: return null
        return TypeCodes.toTypeName(typeCode)
    }

    /**
     * Get package hash from compact VUID (first 6 chars)
     */
    fun getPackageHash(vuid: String): String? {
        if (!isCompact(vuid)) return null
        return vuid.take(6)
    }

    /**
     * Get element hash from compact VUID (last 8 chars)
     */
    fun getElementHash(vuid: String): String? {
        if (!isCompact(vuid)) return null
        return vuid.takeLast(8)
    }

    /**
     * Create compact VUID from components
     */
    fun createCompact(packageHash: String, typeCode: Char, elementHash: String): String {
        require(packageHash.length == 6) { "Package hash must be 6 chars" }
        require(elementHash.length == 8) { "Element hash must be 8 chars" }
        return "$packageHash-$typeCode$elementHash"
    }
}
