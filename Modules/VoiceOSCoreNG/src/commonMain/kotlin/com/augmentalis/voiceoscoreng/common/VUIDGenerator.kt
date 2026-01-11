package com.augmentalis.voiceoscoreng.common

import kotlin.random.Random

/**
 * VUID Type Codes for Voice Unique Identifiers
 *
 * Each UI element type has a single-character code for compact representation.
 */
enum class VUIDTypeCode(val code: Char, val abbrev: String) {
    BUTTON('b', "btn"),
    INPUT('i', "inp"),
    SCROLL('s', "scr"),
    TEXT('t', "txt"),
    ELEMENT('e', "elm"),  // Generic fallback
    CARD('c', "crd"),
    LAYOUT('l', "lay"),
    MENU('m', "mnu"),
    DIALOG('d', "dlg"),
    IMAGE('g', "img"),
    CHECKBOX('k', "chk"),
    SWITCH('w', "swt"),
    LIST('z', "lst"),
    SLIDER('r', "sld"),
    TAB('a', "tab");

    companion object {
        private val codeMap = entries.associateBy { it.code }
        private val abbrevMap = entries.associateBy { it.abbrev }

        /**
         * Get VUIDTypeCode from character code
         * @return VUIDTypeCode or null if code is not recognized
         */
        fun fromCode(code: Char): VUIDTypeCode? = codeMap[code]

        /**
         * Get VUIDTypeCode from 3-char abbreviation
         */
        fun fromAbbrev(abbrev: String): VUIDTypeCode? = abbrevMap[abbrev.lowercase()]

        /**
         * Get all valid type code characters
         */
        fun validCodes(): Set<Char> = codeMap.keys
    }
}

/**
 * Module identifiers for internal entities
 */
object VUIDModule {
    const val VOICEOS = "vos"
    const val AVA = "ava"
    const val WEBAVANUE = "web"
    const val NLU = "nlu"
    const val COCKPIT = "cpt"
    const val COMMON = "cmn"
}

/**
 * Parsed components of a VUID
 */
data class VUIDComponents(
    val packageHash: String,
    val typeCode: VUIDTypeCode,
    val elementHash: String
) {
    /**
     * Reconstruct the VUID string from components
     */
    fun toVUID(): String = "$packageHash-${typeCode.code}$elementHash"
}

/**
 * VUIDGenerator - Voice Unique Identifier Generator
 *
 * VUID is a compact identifier format (16 chars) replacing UUID for voice UI elements.
 *
 * Format: {pkgHash6}-{typeCode}{hash8}
 * Example: a3f2e1-b917cc9dc (16 chars total)
 *
 * Components:
 * - pkgHash6: 6-character lowercase hex hash of package name
 * - typeCode: Single character representing element type (b=button, i=input, etc.)
 * - hash8: 8-character lowercase hex hash of element identifier
 */
object VUIDGenerator {

    private const val PACKAGE_HASH_LENGTH = 6
    private const val ELEMENT_HASH_LENGTH = 8
    private const val VUID_TOTAL_LENGTH = 16  // 6 + 1 (hyphen) + 1 (type) + 8 = 16

    // Regex pattern for valid VUID: 6 hex chars, hyphen, type code, 8 hex chars
    private val vuidPattern = Regex("^[0-9a-f]{6}-[bisteclmdgkwzra][0-9a-f]{8}$")

    // Regex pattern for simple format VUID: module:abbrev:hash8
    private val simpleFormatPattern = Regex("^[a-z]{3}:[a-z]{3}:[0-9a-f]{8}$")

    private const val HEX_CHARS = "0123456789abcdef"

    /**
     * Generate a VUID for a UI element
     *
     * @param packageName The application package name (e.g., "com.example.app")
     * @param typeCode The type of UI element
     * @param elementHash A unique identifier for the element (e.g., resource ID, content hash)
     * @return A 16-character VUID string
     */
    fun generate(packageName: String, typeCode: VUIDTypeCode, elementHash: String): String {
        val pkgHash = generatePackageHash(packageName)
        val elemHash = generateElementHash(elementHash)
        return "$pkgHash-${typeCode.code}$elemHash"
    }

    /**
     * Generate a 6-character lowercase hex hash from a package name
     *
     * @param packageName The package name to hash
     * @return 6-character lowercase hex string
     */
    fun generatePackageHash(packageName: String): String {
        return hashToHex(packageName, PACKAGE_HASH_LENGTH)
    }

    /**
     * Validate if a string is a valid VUID format
     *
     * @param vuid The string to validate
     * @return true if valid VUID format, false otherwise
     */
    fun isValidVUID(vuid: String): Boolean {
        if (vuid.length != VUID_TOTAL_LENGTH) return false
        return vuidPattern.matches(vuid)
    }

    /**
     * Parse a VUID string into its component parts
     *
     * @param vuid The VUID string to parse
     * @return VUIDComponents if valid, null otherwise
     */
    fun parseVUID(vuid: String): VUIDComponents? {
        if (!isValidVUID(vuid)) return null

        val parts = vuid.split("-")
        if (parts.size != 2) return null

        val packageHash = parts[0]
        val typeCodeChar = parts[1][0]
        val elementHash = parts[1].substring(1)

        val typeCode = VUIDTypeCode.fromCode(typeCodeChar) ?: return null

        return VUIDComponents(
            packageHash = packageHash,
            typeCode = typeCode,
            elementHash = elementHash
        )
    }

    /**
     * Determine the appropriate type code for a given UI class name
     *
     * @param className The simple class name of the UI element (e.g., "Button", "EditText")
     * @return The corresponding VUIDTypeCode, defaults to ELEMENT if unknown
     */
    fun getTypeCode(className: String): VUIDTypeCode {
        return TypePatternRegistry.getTypeCode(className)
    }

    /**
     * Generate 8-character random hex hash
     */
    fun generateRandomHash8(): String {
        return buildString {
            repeat(8) {
                append(HEX_CHARS[Random.nextInt(16)])
            }
        }
    }

    /**
     * Generate a simple format VUID for internal module entities.
     *
     * Simple format: {module}:{abbrev}:{hash8}
     * Example: ava:elm:a7f3e2c1
     *
     * Used for VoiceOS internal entities rather than external app elements.
     *
     * @param module The module identifier (e.g., VUIDModule.AVA, VUIDModule.VOICEOS)
     * @param typeCode The type of element
     * @return A simple format VUID string
     */
    fun generateSimple(module: String, typeCode: VUIDTypeCode): String {
        val hash = generateRandomHash8()
        return "$module:${typeCode.abbrev}:$hash"
    }

    /**
     * Check if a string is in simple VUID format.
     *
     * Simple format: {module}:{abbrev}:{hash8}
     * Example: ava:elm:a7f3e2c1
     *
     * @param vuid The string to check
     * @return true if in simple format, false otherwise
     */
    fun isSimpleFormat(vuid: String): Boolean {
        return simpleFormatPattern.matches(vuid)
    }

    /**
     * Generate an 8-character lowercase hex hash from an element identifier
     */
    private fun generateElementHash(elementId: String): String {
        return hashToHex(elementId, ELEMENT_HASH_LENGTH)
    }

    /**
     * Generate a lowercase hex hash of specified length from input string
     * Uses a simple but effective hash algorithm suitable for KMP
     */
    private fun hashToHex(input: String, length: Int): String {
        // Use a simple multiplicative hash that works across KMP targets
        var hash1 = 0L
        var hash2 = 0L

        input.forEachIndexed { index, char ->
            val charValue = char.code.toLong()
            // Two independent hash computations for better distribution
            hash1 = (hash1 * 31 + charValue) and 0xFFFFFFFFL
            hash2 = (hash2 * 37 + charValue * (index + 1)) and 0xFFFFFFFFL
        }

        // Combine hashes and convert to hex
        val combined = ((hash1 shl 16) xor hash2) and 0xFFFFFFFFFFFFL

        // Convert to hex string, pad with zeros, and take required length
        return combined.toString(16)
            .padStart(12, '0')
            .takeLast(length)
            .lowercase()
    }

}
