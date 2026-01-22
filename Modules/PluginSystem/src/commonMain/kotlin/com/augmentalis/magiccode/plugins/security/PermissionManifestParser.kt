/**
 * PermissionManifestParser.kt - Parse and validate plugin permission manifests
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Parses permission declarations from plugin manifest files and validates
 * them for syntax, semantics, and dangerous permission combinations.
 */
package com.augmentalis.magiccode.plugins.security

import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Represents a parsed permission declaration from a plugin manifest.
 *
 * Contains the permission type along with optional rationale explaining
 * why the plugin needs this permission.
 *
 * @param permission The permission being requested
 * @param rationale Optional human-readable explanation of why the permission is needed
 * @param optional Whether the plugin can function without this permission
 * @since 2.0.0
 */
@Serializable
data class PermissionDeclaration(
    val permission: PluginPermission,
    val rationale: String? = null,
    val optional: Boolean = false
)

/**
 * Result of parsing a permission manifest.
 *
 * Contains the list of parsed permissions along with any warnings or errors
 * encountered during parsing.
 *
 * @param permissions List of successfully parsed permission declarations
 * @param warnings Non-fatal issues found during parsing
 * @param errors Fatal issues that may prevent the plugin from loading
 * @param dangerousCombinations Detected dangerous permission combinations
 * @since 2.0.0
 */
data class PermissionParseResult(
    val permissions: List<PermissionDeclaration>,
    val warnings: List<String>,
    val errors: List<String>,
    val dangerousCombinations: List<DangerousPermissionCombination>
) {
    /**
     * Whether parsing was successful (no errors).
     */
    val isValid: Boolean get() = errors.isEmpty()

    /**
     * Whether the result has any warnings or dangerous combinations.
     */
    val hasWarnings: Boolean get() = warnings.isNotEmpty() || dangerousCombinations.isNotEmpty()

    /**
     * Get all permission types from the parsed declarations.
     */
    val permissionSet: Set<PluginPermission>
        get() = permissions.map { it.permission }.toSet()

    companion object {
        /**
         * Create a successful parse result with no warnings.
         */
        fun success(permissions: List<PermissionDeclaration>): PermissionParseResult {
            return PermissionParseResult(
                permissions = permissions,
                warnings = emptyList(),
                errors = emptyList(),
                dangerousCombinations = emptyList()
            )
        }

        /**
         * Create a failed parse result with errors.
         */
        fun failure(errors: List<String>): PermissionParseResult {
            return PermissionParseResult(
                permissions = emptyList(),
                warnings = emptyList(),
                errors = errors,
                dangerousCombinations = emptyList()
            )
        }
    }
}

/**
 * Represents a dangerous combination of permissions.
 *
 * Some permission combinations create security risks when granted together.
 * This class documents these combinations and the associated risks.
 *
 * @param permissions The set of permissions that form the dangerous combination
 * @param riskLevel Severity of the security risk
 * @param description Human-readable explanation of the risk
 * @param recommendation Suggested action to mitigate the risk
 * @since 2.0.0
 */
data class DangerousPermissionCombination(
    val permissions: Set<PluginPermission>,
    val riskLevel: RiskLevel,
    val description: String,
    val recommendation: String
)

/**
 * Risk severity levels for permission combinations.
 *
 * @since 2.0.0
 */
enum class RiskLevel {
    /**
     * Low risk - Monitor but generally safe.
     */
    LOW,

    /**
     * Medium risk - Requires user awareness.
     */
    MEDIUM,

    /**
     * High risk - Requires explicit user consent with warning.
     */
    HIGH,

    /**
     * Critical risk - Should be blocked or require special approval.
     */
    CRITICAL
}

/**
 * Parser for plugin permission manifests.
 *
 * Parses permission declarations from various manifest formats (JSON, YAML)
 * and validates them for correctness and security. Detects dangerous
 * permission combinations that could enable malicious behavior.
 *
 * ## Supported Manifest Formats
 * The parser supports permissions declared in these formats:
 *
 * ### Simple array format
 * ```json
 * {
 *   "permissions": ["NETWORK_ACCESS", "FILE_SYSTEM_READ"]
 * }
 * ```
 *
 * ### Detailed object format
 * ```json
 * {
 *   "permissions": [
 *     {
 *       "permission": "NETWORK_ACCESS",
 *       "rationale": "Required to fetch remote data",
 *       "optional": false
 *     }
 *   ]
 * }
 * ```
 *
 * ## Dangerous Combinations
 * The parser detects these risky permission combinations:
 * - **FILE_SYSTEM_WRITE + NETWORK_ACCESS**: Data exfiltration/malware download
 * - **ACCESSIBILITY_DATA + NETWORK_ACCESS**: Keylogging/screen scraping
 * - **BACKGROUND_EXECUTION + NETWORK_ACCESS + FILE_SYSTEM_WRITE**: Botnet risk
 *
 * ## Usage Example
 * ```kotlin
 * val parser = PermissionManifestParser()
 * val result = parser.parseJson(manifestJson)
 *
 * if (!result.isValid) {
 *     result.errors.forEach { println("ERROR: $it") }
 *     return
 * }
 *
 * if (result.dangerousCombinations.isNotEmpty()) {
 *     result.dangerousCombinations.forEach { combo ->
 *         println("WARNING: ${combo.description}")
 *     }
 * }
 *
 * val permissions = result.permissionSet
 * ```
 *
 * @since 2.0.0
 */
class PermissionManifestParser {

    companion object {
        private const val TAG = "PermissionManifestParser"

        /**
         * Key for permissions array in manifest JSON.
         */
        const val PERMISSIONS_KEY = "permissions"

        /**
         * Key for permission rationales map in manifest JSON.
         */
        const val RATIONALES_KEY = "permissionRationales"

        /**
         * Known dangerous permission combinations.
         */
        val DANGEROUS_COMBINATIONS: List<DangerousPermissionCombination> = listOf(
            DangerousPermissionCombination(
                permissions = setOf(PluginPermission.FILE_SYSTEM_WRITE, PluginPermission.NETWORK_ACCESS),
                riskLevel = RiskLevel.HIGH,
                description = "FILE_SYSTEM_WRITE + NETWORK_ACCESS enables data exfiltration or malware download",
                recommendation = "Ensure plugin has legitimate need for both. Review network endpoints and file operations."
            ),
            DangerousPermissionCombination(
                permissions = setOf(PluginPermission.ACCESSIBILITY_DATA, PluginPermission.NETWORK_ACCESS),
                riskLevel = RiskLevel.HIGH,
                description = "ACCESSIBILITY_DATA + NETWORK_ACCESS enables screen scraping and keylogging",
                recommendation = "Verify plugin does not transmit sensitive screen content. Review data handling."
            ),
            DangerousPermissionCombination(
                permissions = setOf(
                    PluginPermission.BACKGROUND_EXECUTION,
                    PluginPermission.NETWORK_ACCESS,
                    PluginPermission.FILE_SYSTEM_WRITE
                ),
                riskLevel = RiskLevel.CRITICAL,
                description = "BACKGROUND_EXECUTION + NETWORK_ACCESS + FILE_SYSTEM_WRITE enables botnet behavior",
                recommendation = "This combination should only be granted to highly trusted plugins. Requires explicit approval."
            ),
            DangerousPermissionCombination(
                permissions = setOf(PluginPermission.ACCESSIBILITY_DATA, PluginPermission.FILE_SYSTEM_WRITE),
                riskLevel = RiskLevel.MEDIUM,
                description = "ACCESSIBILITY_DATA + FILE_SYSTEM_WRITE enables local logging of sensitive data",
                recommendation = "Verify plugin does not log passwords or sensitive input."
            ),
            DangerousPermissionCombination(
                permissions = setOf(
                    PluginPermission.INTER_PLUGIN_COMMUNICATION,
                    PluginPermission.NETWORK_ACCESS
                ),
                riskLevel = RiskLevel.MEDIUM,
                description = "INTER_PLUGIN_COMMUNICATION + NETWORK_ACCESS enables privilege relay through other plugins",
                recommendation = "Ensure plugin does not act as a proxy for less-privileged plugins to access network."
            )
        )
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parse permissions from a JSON manifest string.
     *
     * @param jsonString The JSON manifest content
     * @return Parse result containing permissions and any warnings/errors
     */
    fun parseJson(jsonString: String): PermissionParseResult {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            parseJsonObject(jsonObject)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to parse manifest JSON", e)
            PermissionParseResult.failure(listOf("Invalid JSON: ${e.message}"))
        }
    }

    /**
     * Parse permissions from a parsed JSON object.
     *
     * @param jsonObject The parsed JSON object
     * @return Parse result containing permissions and any warnings/errors
     */
    fun parseJsonObject(jsonObject: JsonObject): PermissionParseResult {
        val permissions = mutableListOf<PermissionDeclaration>()
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        // Get permissions array
        val permissionsElement = jsonObject[PERMISSIONS_KEY]
        if (permissionsElement == null) {
            // No permissions declared - this is valid (plugin requests no permissions)
            PluginLog.d(TAG, "No permissions declared in manifest")
            return PermissionParseResult.success(emptyList())
        }

        // Get rationales map (optional)
        val rationales = jsonObject[RATIONALES_KEY]?.jsonObject?.let { rationalesObj ->
            rationalesObj.entries.associate { (key, value) ->
                key to value.jsonPrimitive.content
            }
        } ?: emptyMap()

        // Parse permissions array
        try {
            val permissionsArray = permissionsElement.jsonArray
            for (element in permissionsArray) {
                try {
                    val declaration = parsePermissionElement(element.jsonPrimitive.content, rationales)
                    if (declaration != null) {
                        permissions.add(declaration)
                    } else {
                        warnings.add("Unknown permission: ${element.jsonPrimitive.content}")
                    }
                } catch (e: Exception) {
                    // Try parsing as object format
                    try {
                        val obj = element.jsonObject
                        val permName = obj["permission"]?.jsonPrimitive?.content
                            ?: obj["name"]?.jsonPrimitive?.content
                        if (permName != null) {
                            val declaration = parsePermissionObject(permName, obj, rationales)
                            if (declaration != null) {
                                permissions.add(declaration)
                            } else {
                                warnings.add("Unknown permission: $permName")
                            }
                        } else {
                            errors.add("Permission entry missing 'permission' or 'name' field")
                        }
                    } catch (e2: Exception) {
                        errors.add("Invalid permission entry: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            errors.add("Invalid permissions array format: ${e.message}")
        }

        // Validate syntax
        val syntaxErrors = validatePermissionSyntax(permissions)
        errors.addAll(syntaxErrors)

        // Detect dangerous combinations
        val permissionSet = permissions.map { it.permission }.toSet()
        val dangerousCombinations = detectDangerousCombinations(permissionSet)

        if (dangerousCombinations.isNotEmpty()) {
            PluginLog.w(TAG, "Detected ${dangerousCombinations.size} dangerous permission combination(s)")
        }

        return PermissionParseResult(
            permissions = permissions,
            warnings = warnings,
            errors = errors,
            dangerousCombinations = dangerousCombinations
        )
    }

    /**
     * Parse a permission from a string name.
     *
     * @param permissionName The permission name (e.g., "NETWORK_ACCESS")
     * @param rationales Map of permission names to rationales
     * @return PermissionDeclaration or null if unknown permission
     */
    private fun parsePermissionElement(
        permissionName: String,
        rationales: Map<String, String>
    ): PermissionDeclaration? {
        val permission = parsePermissionName(permissionName) ?: return null
        return PermissionDeclaration(
            permission = permission,
            rationale = rationales[permissionName],
            optional = false
        )
    }

    /**
     * Parse a permission from an object format.
     *
     * @param permissionName The permission name
     * @param obj The JSON object containing permission details
     * @param rationales Map of permission names to rationales (fallback)
     * @return PermissionDeclaration or null if unknown permission
     */
    private fun parsePermissionObject(
        permissionName: String,
        obj: JsonObject,
        rationales: Map<String, String>
    ): PermissionDeclaration? {
        val permission = parsePermissionName(permissionName) ?: return null
        val rationale = obj["rationale"]?.jsonPrimitive?.content ?: rationales[permissionName]
        val optional = obj["optional"]?.jsonPrimitive?.content?.toBoolean() ?: false

        return PermissionDeclaration(
            permission = permission,
            rationale = rationale,
            optional = optional
        )
    }

    /**
     * Parse a permission enum from its string name.
     *
     * Supports multiple naming conventions:
     * - Exact enum name: "NETWORK_ACCESS"
     * - Lower case: "network_access"
     * - Kebab case: "network-access"
     *
     * @param name The permission name string
     * @return The PluginPermission or null if not found
     */
    fun parsePermissionName(name: String): PluginPermission? {
        val normalized = name.trim().uppercase().replace("-", "_")
        return try {
            PluginPermission.valueOf(normalized)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Validate permission syntax and semantics.
     *
     * @param permissions List of permission declarations to validate
     * @return List of error messages (empty if valid)
     */
    fun validatePermissionSyntax(permissions: List<PermissionDeclaration>): List<String> {
        val errors = mutableListOf<String>()

        // Check for duplicates
        val seen = mutableSetOf<PluginPermission>()
        for (declaration in permissions) {
            if (!seen.add(declaration.permission)) {
                errors.add("Duplicate permission: ${declaration.permission}")
            }
        }

        // Validate rationales are not too long
        for (declaration in permissions) {
            declaration.rationale?.let { rationale ->
                if (rationale.length > 500) {
                    errors.add("Permission rationale too long (>500 chars): ${declaration.permission}")
                }
            }
        }

        return errors
    }

    /**
     * Detect dangerous permission combinations.
     *
     * @param permissions Set of permissions to check
     * @return List of dangerous combinations found
     */
    fun detectDangerousCombinations(
        permissions: Set<PluginPermission>
    ): List<DangerousPermissionCombination> {
        return DANGEROUS_COMBINATIONS.filter { combination ->
            permissions.containsAll(combination.permissions)
        }
    }

    /**
     * Validate that all required permissions for a plugin are present.
     *
     * @param declared Permissions declared in manifest
     * @param required Permissions required by the plugin's capabilities
     * @return List of missing permissions
     */
    fun findMissingPermissions(
        declared: Set<PluginPermission>,
        required: Set<PluginPermission>
    ): Set<PluginPermission> {
        return required - declared
    }

    /**
     * Get permissions not used by any declared capability.
     *
     * @param declared Permissions declared in manifest
     * @param usedByCapabilities Permissions actually used by plugin capabilities
     * @return Set of excessive permissions
     */
    fun findExcessivePermissions(
        declared: Set<PluginPermission>,
        usedByCapabilities: Set<PluginPermission>
    ): Set<PluginPermission> {
        return declared - usedByCapabilities
    }

    /**
     * Create a permission manifest JSON string from declarations.
     *
     * @param declarations List of permission declarations
     * @return JSON string representation
     */
    fun toJson(declarations: List<PermissionDeclaration>): String {
        val simplePermissions = declarations.map { it.permission.name }
        val rationales = declarations.filter { it.rationale != null }
            .associate { it.permission.name to it.rationale!! }

        return buildString {
            append("{\n")
            append("  \"permissions\": [")
            append(simplePermissions.joinToString(", ") { "\"$it\"" })
            append("]")
            if (rationales.isNotEmpty()) {
                append(",\n  \"permissionRationales\": {\n")
                append(rationales.entries.joinToString(",\n") { (k, v) ->
                    "    \"$k\": \"$v\""
                })
                append("\n  }")
            }
            append("\n}")
        }
    }
}
