/**
 * CustomAvidGenerator.kt - Generate UUIDs with custom prefixes
 * Path: libraries/AvidCreator/src/main/java/com/augmentalis/uuidcreator/formats/CustomAvidGenerator.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Custom UUID format generation with organizational prefixes
 */

package com.augmentalis.avidcreator.formats

import com.augmentalis.avidcreator.core.AvidGenerator
import java.util.UUID

/**
 * Custom UUID Generator
 *
 * Generates UUIDs with custom prefixes for organization and readability.
 *
 * ## Format Patterns
 *
 * 1. **Prefix Format**: `{prefix}-{uuid}`
 *    - Example: `btn-550e8400-e29b-41d4-a716-446655440000`
 *
 * 2. **Namespace Format**: `{namespace}.{prefix}-{uuid}`
 *    - Example: `com.myapp.btn-550e8400-e29b-41d4-a716-446655440000`
 *
 * ## Predefined Prefixes
 *
 * - `btn` - Button components
 * - `txt` - Text components
 * - `img` - Image components
 * - `input` - Input fields
 * - `layout` - Layout containers
 * - `menu` - Menu items
 * - `dialog` - Dialog windows
 * - `theme` - Theme configurations
 *
 * ## Usage Examples
 *
 * ```kotlin
 * val generator = CustomAvidGenerator()
 *
 * // Simple prefix
 * val buttonUuid = generator.generate("btn")
 * // Returns: "btn-550e8400-e29b-41d4-a716-446655440000"
 *
 * // Namespace + prefix
 * val nsUuid = generator.generate("com.myapp", "btn")
 * // Returns: "com.myapp.btn-550e8400-e29b-41d4-a716-446655440000"
 *
 * // By component type
 * val typeUuid = generator.generateByType("button")
 * // Returns: "btn-550e8400-e29b-41d4-a716-446655440000"
 *
 * // Parse custom UUID
 * val (prefix, baseUuid) = generator.parse(buttonUuid)
 * // Returns: ("btn", "550e8400-e29b-41d4-a716-446655440000")
 * ```
 *
 * ## RFC 4122 Compliance
 *
 * The base UUID portion remains RFC 4122 compliant. Only the prefix
 * is added for organizational purposes.
 *
 * @since 1.0.0
 */
object CustomAvidGenerator {

    /**
     * Predefined component prefixes
     */
    const val PREFIX_BUTTON = "btn"
    const val PREFIX_TEXT = "txt"
    const val PREFIX_IMAGE = "img"
    const val PREFIX_INPUT = "input"
    const val PREFIX_CONTAINER = "container"
    const val PREFIX_LAYOUT = "layout"
    const val PREFIX_MENU = "menu"
    const val PREFIX_DIALOG = "dialog"
    const val PREFIX_THEME = "theme"
    const val PREFIX_TAB = "tab"
    const val PREFIX_CARD = "card"
    const val PREFIX_LIST = "list"

    /**
     * Generate UUID with custom prefix
     *
     * Creates UUID in format: `{prefix}-{uuid}`
     *
     * @param prefix Custom prefix (alphanumeric + hyphens, 1-20 chars)
     * @return Formatted UUID string
     * @throws IllegalArgumentException if prefix invalid
     */
    fun generate(prefix: String): String {
        validatePrefix(prefix)
        @Suppress("DEPRECATION")
        val baseUuid = AvidGenerator.generate()
        return "$prefix-$baseUuid"
    }

    /**
     * Generate UUID with namespace and prefix
     *
     * Creates UUID in format: `{namespace}.{prefix}-{uuid}`
     *
     * @param namespace Namespace (reverse domain notation)
     * @param prefix Component prefix
     * @return Formatted UUID string
     */
    fun generate(namespace: String, prefix: String): String {
        validateNamespace(namespace)
        validatePrefix(prefix)
        @Suppress("DEPRECATION")
        val baseUuid = AvidGenerator.generate()
        return "$namespace.$prefix-$baseUuid"
    }

    /**
     * Generate UUID by component type
     *
     * Uses predefined prefixes for common component types.
     *
     * @param type Component type (button, text, image, etc.)
     * @param customPrefix Optional custom prefix (overrides type-based)
     * @return Formatted UUID string
     */
    fun generateByType(type: String, customPrefix: String? = null): String {
        val prefix = customPrefix ?: mapTypeToPrefix(type)
        return generate(prefix)
    }

    /**
     * Parse custom UUID format
     *
     * Extracts prefix and base UUID from formatted string.
     *
     * @param customUuid Custom formatted UUID
     * @return Pair of (prefix, baseUuid) or (null, uuid) if standard format
     */
    fun parse(customUuid: String): Pair<String?, String> {
        // Check for namespace format: namespace.prefix-uuid
        if (customUuid.contains(".")) {
            val dotIndex = customUuid.lastIndexOf('.')
            val beforeDot = customUuid.substring(0, dotIndex)
            val afterDot = customUuid.substring(dotIndex + 1)

            // Parse the part after dot
            val (prefix, baseUuid) = parseSimpleFormat(afterDot)
            return if (prefix != null) {
                ("$beforeDot.$prefix" to baseUuid)
            } else {
                (null to customUuid)
            }
        }

        // Simple format: prefix-uuid
        return parseSimpleFormat(customUuid)
    }

    /**
     * Parse simple prefix-uuid format
     */
    private fun parseSimpleFormat(str: String): Pair<String?, String> {
        val parts = str.split("-")

        // Standard UUID: 8-4-4-4-12
        if (parts.size == 5 && parts[0].length == 8) {
            return null to str
        }

        // Custom format: prefix-8-4-4-4-12
        if (parts.size == 6) {
            val prefix = parts[0]
            val baseUuid = parts.drop(1).joinToString("-")
            return prefix to baseUuid
        }

        // Invalid format
        return null to str
    }

    /**
     * Check if UUID has custom format
     *
     * @param uuid UUID string to check
     * @return true if custom format detected
     */
    fun isCustomFormat(uuid: String): Boolean {
        val (prefix, _) = parse(uuid)
        return prefix != null
    }

    /**
     * Extract prefix from custom UUID
     *
     * @param uuid Custom UUID
     * @return Prefix or null if standard format
     */
    fun extractPrefix(uuid: String): String? {
        return parse(uuid).first
    }

    /**
     * Extract base UUID from custom UUID
     *
     * @param uuid Custom UUID
     * @return Base UUID (RFC 4122 compliant)
     */
    fun extractBaseUuid(uuid: String): String {
        return parse(uuid).second
    }

    /**
     * Convert standard UUID to custom format
     *
     * @param standardUuid Standard UUID
     * @param prefix Prefix to add
     * @return Custom formatted UUID
     */
    fun addPrefix(standardUuid: String, prefix: String): String {
        validatePrefix(prefix)
        return "$prefix-$standardUuid"
    }

    /**
     * Remove prefix from custom UUID
     *
     * @param customUuid Custom UUID
     * @return Standard UUID
     */
    fun removePrefix(customUuid: String): String {
        return extractBaseUuid(customUuid)
    }

    /**
     * Map component type to prefix
     */
    private fun mapTypeToPrefix(type: String): String {
        return when (type.lowercase()) {
            "button", "imagebutton" -> PREFIX_BUTTON
            "text", "textview", "textfield" -> PREFIX_TEXT
            "image", "imageview" -> PREFIX_IMAGE
            "input", "edittext" -> PREFIX_INPUT
            "container", "viewgroup" -> PREFIX_CONTAINER
            "layout", "linearlayout", "relativelayout" -> PREFIX_LAYOUT
            "menu", "menuitem" -> PREFIX_MENU
            "dialog", "alertdialog" -> PREFIX_DIALOG
            "theme", "style" -> PREFIX_THEME
            "tab", "tablayout" -> PREFIX_TAB
            "card", "cardview" -> PREFIX_CARD
            "list", "listview", "recyclerview" -> PREFIX_LIST
            else -> type.lowercase().take(10)
        }
    }

    /**
     * Validate prefix format
     *
     * Rules:
     * - Alphanumeric + hyphens only
     * - 1-20 characters
     * - Cannot start/end with hyphen
     * - Cannot contain consecutive hyphens
     *
     * @param prefix Prefix to validate
     * @throws IllegalArgumentException if invalid
     */
    private fun validatePrefix(prefix: String) {
        require(prefix.isNotEmpty() && prefix.length <= 20) {
            "Prefix must be 1-20 characters, got: ${prefix.length}"
        }

        require(prefix.matches(Regex("^[a-zA-Z0-9]+(-[a-zA-Z0-9]+)*$"))) {
            "Prefix must be alphanumeric (single hyphens allowed between): $prefix"
        }
    }

    /**
     * Validate namespace format
     *
     * Rules:
     * - Reverse domain notation (e.g., com.myapp)
     * - Lowercase alphanumeric + dots
     * - At least 2 parts (e.g., com.app)
     *
     * @param namespace Namespace to validate
     * @throws IllegalArgumentException if invalid
     */
    private fun validateNamespace(namespace: String) {
        val parts = namespace.split(".")

        require(parts.size >= 2) {
            "Namespace must have at least 2 parts (e.g., com.app): $namespace"
        }

        require(namespace.matches(Regex("^[a-z0-9]+(\\.[a-z0-9]+)+$"))) {
            "Namespace must be lowercase alphanumeric with dots: $namespace"
        }
    }

    /**
     * Get all predefined prefixes
     *
     * @return List of standard prefixes
     */
    fun getPredefinedPrefixes(): List<String> {
        return listOf(
            PREFIX_BUTTON,
            PREFIX_TEXT,
            PREFIX_IMAGE,
            PREFIX_INPUT,
            PREFIX_CONTAINER,
            PREFIX_LAYOUT,
            PREFIX_MENU,
            PREFIX_DIALOG,
            PREFIX_THEME,
            PREFIX_TAB,
            PREFIX_CARD,
            PREFIX_LIST
        )
    }
}
