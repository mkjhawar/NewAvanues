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
    private val vuidPattern = Regex("^[0-9a-f]{6}-[bisteclmdg][0-9a-f]{8}$")

    // Class name patterns for type code mapping
    private val buttonPatterns = setOf(
        "button", "appcompatbutton", "materialbutton", "imagebutton",
        "floatingactionbutton", "fab", "extendedFloatingActionButton"
    )

    private val inputPatterns = setOf(
        "edittext", "textinputedittext", "autocompletetextview", "searchview",
        "textfield", "textinputlayout", "searchbar"
    )

    private val scrollPatterns = setOf(
        "scrollview", "horizontalscrollview", "nestedscrollview", "recyclerview",
        "listview", "gridview", "lazycolumn", "lazyrow", "lazyverticalgrid",
        "lazyhorizontalgrid", "viewpager", "viewpager2"
    )

    private val textPatterns = setOf(
        "textview", "appcompattextview", "materialtextview", "text",
        "checkedtextview"
    )

    private val cardPatterns = setOf(
        "cardview", "materialcardview", "card"
    )

    private val layoutPatterns = setOf(
        "linearlayout", "relativelayout", "framelayout", "constraintlayout",
        "coordinatorlayout", "appbarlayout", "collapsibletoolbarlayout",
        "row", "column", "box", "surface", "scaffold"
    )

    private val menuPatterns = setOf(
        "menu", "popupmenu", "contextmenu", "dropdownmenu", "navigationmenu",
        "overflowmenu", "optionsmenu", "spinner"
    )

    private val dialogPatterns = setOf(
        "dialog", "alertdialog", "bottomsheetdialog", "dialogfragment",
        "bottomsheet", "modalbottomsheet", "datepickerdialog", "timepickerdialog"
    )

    private val imagePatterns = setOf(
        "imageview", "appcompatimageview", "image", "icon",
        "shapeableimageview", "circleimageview"
    )

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
        val normalizedName = className.trim().lowercase()

        if (normalizedName.isEmpty()) return VUIDTypeCode.ELEMENT

        return when {
            buttonPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.BUTTON
            inputPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.INPUT
            scrollPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.SCROLL
            textPatterns.any { normalizedName == it || normalizedName.endsWith(it) } -> VUIDTypeCode.TEXT
            cardPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.CARD
            layoutPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.LAYOUT
            menuPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.MENU
            dialogPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.DIALOG
            imagePatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.IMAGE
            else -> VUIDTypeCode.ELEMENT
        }
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

    // ==================== Simple Format Generation ====================

    /**
     * Generate simple VUID: {module}:{type}:{hash8}
     * Example: ava:msg:a7f3e2c1
     */
    fun generateSimple(module: String, typeCode: VUIDTypeCode): String {
        val hash8 = generateRandomHash8()
        return "$module:${typeCode.abbrev}:$hash8"
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

    private const val HEX_CHARS = "0123456789abcdef"

    // ==================== AVA Convenience Methods ====================

    fun generateMessageVuid(): String = generateSimple(VUIDModule.AVA, VUIDTypeCode.ELEMENT)
    fun generateConversationVuid(): String = generateSimple(VUIDModule.AVA, VUIDTypeCode.ELEMENT)
    fun generateDocumentVuid(): String = generateSimple(VUIDModule.AVA, VUIDTypeCode.ELEMENT)
    fun generateMemoryVuid(): String = generateSimple(VUIDModule.AVA, VUIDTypeCode.ELEMENT)

    // ==================== WebAvanue Convenience Methods ====================

    fun generateTabVuid(): String = generateSimple(VUIDModule.WEBAVANUE, VUIDTypeCode.TAB)
    fun generateFavoriteVuid(): String = generateSimple(VUIDModule.WEBAVANUE, VUIDTypeCode.ELEMENT)
    fun generateDownloadVuid(): String = generateSimple(VUIDModule.WEBAVANUE, VUIDTypeCode.ELEMENT)
    fun generateHistoryVuid(): String = generateSimple(VUIDModule.WEBAVANUE, VUIDTypeCode.ELEMENT)
    fun generateSessionVuid(): String = generateSimple(VUIDModule.WEBAVANUE, VUIDTypeCode.ELEMENT)
    fun generateGroupVuid(): String = generateSimple(VUIDModule.WEBAVANUE, VUIDTypeCode.LAYOUT)

    // ==================== Cockpit Convenience Methods ====================

    fun generateRequestVuid(): String = generateSimple(VUIDModule.COCKPIT, VUIDTypeCode.ELEMENT)
    fun generateWindowVuid(): String = generateSimple(VUIDModule.COCKPIT, VUIDTypeCode.ELEMENT)
    fun generateStreamVuid(): String = generateSimple(VUIDModule.COCKPIT, VUIDTypeCode.ELEMENT)
    fun generatePresetVuid(): String = generateSimple(VUIDModule.COCKPIT, VUIDTypeCode.ELEMENT)
    fun generateDeviceVuid(): String = generateSimple(VUIDModule.COCKPIT, VUIDTypeCode.ELEMENT)
    fun generateSyncVuid(): String = generateSimple(VUIDModule.COCKPIT, VUIDTypeCode.ELEMENT)

    // ==================== Simple Format Validation ====================

    private val SIMPLE_PATTERN = Regex("^[a-z]{3}:[a-z]{3}:[a-f0-9]{8}$")

    /**
     * Check if VUID is in simple format (module:type:hash8)
     */
    fun isSimpleFormat(vuid: String): Boolean = SIMPLE_PATTERN.matches(vuid)

    // ==================== Legacy Format Detection ====================

    /** Legacy UUID v4 pattern (RFC 4122) */
    private val LEGACY_UUID_PATTERN = Regex(
        "^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$",
        RegexOption.IGNORE_CASE
    )

    /** Legacy VoiceOS pattern: com.pkg.v1.0.0.type-hash12 */
    private val LEGACY_VOICEOS_PATTERN = Regex(
        "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+\\.v[0-9.]+\\.[a-z]+-[a-f0-9]{12}$",
        RegexOption.IGNORE_CASE
    )

    /**
     * Check if VUID is in legacy UUID v4 format
     */
    fun isLegacyUuid(vuid: String): Boolean = LEGACY_UUID_PATTERN.matches(vuid)

    /**
     * Check if VUID is in legacy VoiceOS format
     */
    fun isLegacyVoiceOS(vuid: String): Boolean = LEGACY_VOICEOS_PATTERN.matches(vuid)

    /**
     * Check if any valid VUID format (compact, simple, or legacy)
     */
    fun isValid(vuid: String): Boolean =
        isValidVUID(vuid) || isSimpleFormat(vuid) || isLegacyUuid(vuid) || isLegacyVoiceOS(vuid)

    // ==================== Migration Utilities ====================

    /**
     * VUID format types
     */
    enum class VuidFormat {
        COMPACT,        // a3f2e1-b917cc9dc (16 chars)
        SIMPLE,         // ava:msg:a7f3e2c1
        LEGACY_UUID,    // 550e8400-e29b-41d4-a716-446655440000
        LEGACY_VOICEOS, // com.pkg.v1.0.0.button-a7f3e2c1d4b5
        UNKNOWN
    }

    /**
     * Detect the format of a VUID
     */
    fun detectFormat(vuid: String): VuidFormat = when {
        isValidVUID(vuid) -> VuidFormat.COMPACT
        isSimpleFormat(vuid) -> VuidFormat.SIMPLE
        isLegacyUuid(vuid) -> VuidFormat.LEGACY_UUID
        isLegacyVoiceOS(vuid) -> VuidFormat.LEGACY_VOICEOS
        else -> VuidFormat.UNKNOWN
    }

    /**
     * Migrate legacy VoiceOS VUID to compact format
     *
     * @param legacyVuid Legacy VoiceOS VUID (com.pkg.v1.0.0.type-hash12)
     * @return Compact VUID or null if cannot migrate
     */
    fun migrateToCompact(legacyVuid: String): String? {
        if (!isLegacyVoiceOS(legacyVuid)) return null

        // Parse legacy format: com.pkg.v1.0.0.type-hash12
        val lastDotIdx = legacyVuid.lastIndexOf('.')
        val dashIdx = legacyVuid.indexOf('-', lastDotIdx)

        if (lastDotIdx < 0 || dashIdx < 0) return null

        val beforeType = legacyVuid.substring(0, lastDotIdx)
        val typeName = legacyVuid.substring(lastDotIdx + 1, dashIdx)
        val hash = legacyVuid.substring(dashIdx + 1).take(8)

        // Extract package name (before .v)
        val versionIdx = beforeType.indexOf(".v")
        val packageName = if (versionIdx > 0) beforeType.substring(0, versionIdx) else beforeType

        // Get type code from type name
        val typeCode = getTypeCode(typeName)

        // Generate compact VUID
        val pkgHash = generatePackageHash(packageName)
        return "$pkgHash-${typeCode.code}$hash"
    }

    /**
     * Extract hash from any VUID format
     * Useful for comparison/lookup across format changes
     *
     * @param vuid Any VUID format
     * @return 8-char hash or null if cannot extract
     */
    fun extractHash(vuid: String): String? = when (detectFormat(vuid)) {
        VuidFormat.COMPACT -> vuid.takeLast(8)
        VuidFormat.SIMPLE -> vuid.split(":").lastOrNull()
        VuidFormat.LEGACY_UUID -> vuid.replace("-", "").takeLast(8)
        VuidFormat.LEGACY_VOICEOS -> vuid.substringAfter("-").take(8)
        VuidFormat.UNKNOWN -> null
    }
}
