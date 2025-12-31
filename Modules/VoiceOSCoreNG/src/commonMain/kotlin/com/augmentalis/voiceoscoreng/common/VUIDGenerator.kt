package com.augmentalis.voiceoscoreng.common

/**
 * VUID Type Codes for Voice Unique Identifiers
 *
 * Each UI element type has a single-character code for compact representation.
 */
enum class VUIDTypeCode(val code: Char) {
    BUTTON('b'),
    INPUT('i'),
    SCROLL('s'),
    TEXT('t'),
    ELEMENT('e'),  // Generic fallback
    CARD('c'),
    LAYOUT('l'),
    MENU('m'),
    DIALOG('d'),
    IMAGE('g');

    companion object {
        private val codeMap = entries.associateBy { it.code }

        /**
         * Get VUIDTypeCode from character code
         * @return VUIDTypeCode or null if code is not recognized
         */
        fun fromCode(code: Char): VUIDTypeCode? = codeMap[code]

        /**
         * Get all valid type code characters
         */
        fun validCodes(): Set<Char> = codeMap.keys
    }
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
}
