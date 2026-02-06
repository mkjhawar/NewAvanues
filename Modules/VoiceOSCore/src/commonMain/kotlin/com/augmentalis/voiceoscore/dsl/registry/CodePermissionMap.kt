package com.augmentalis.voiceoscore.dsl.registry

import com.augmentalis.voiceoscore.dsl.plugin.PluginPermission

/**
 * Maps AVU wire protocol 3-letter codes to required [PluginPermission] sets.
 *
 * Used by the plugin system to validate that a plugin's declared codes are
 * covered by its granted permissions. System codes (QRY, CHT, TTS) require
 * no special permissions; platform-affecting codes (AAC, CAM, etc.) do.
 */
object CodePermissionMap {

    private val codePermissions: Map<String, Set<PluginPermission>> = mapOf(
        // Voice commands
        "VCM" to setOf(PluginPermission.GESTURES),
        // Accessibility actions
        "AAC" to setOf(PluginPermission.ACCESSIBILITY, PluginPermission.GESTURES),
        // App launch/management
        "APL" to setOf(PluginPermission.APPS),
        "APS" to setOf(PluginPermission.APPS),
        // Screen queries (read-only, no permission needed)
        "QRY" to emptySet(),
        // Notifications
        "NTF" to setOf(PluginPermission.NOTIFICATIONS),
        "NRD" to setOf(PluginPermission.NOTIFICATIONS),
        // Chat/TTS (output only, no permission needed)
        "CHT" to emptySet(),
        "TTS" to emptySet(),
        // Navigation gestures
        "NAV" to setOf(PluginPermission.GESTURES),
        "SCL" to setOf(PluginPermission.GESTURES),
        // System
        "SYS" to setOf(PluginPermission.SYSTEM),
        "VOL" to setOf(PluginPermission.SYSTEM),
        "BRT" to setOf(PluginPermission.SYSTEM),
        // Media
        "MDA" to setOf(PluginPermission.APPS),
        "CAM" to setOf(PluginPermission.CAMERA),
        "MIC" to setOf(PluginPermission.MICROPHONE),
        // Network
        "NET" to setOf(PluginPermission.NETWORK),
        "HTT" to setOf(PluginPermission.NETWORK),
        // Storage
        "FIO" to setOf(PluginPermission.STORAGE),
        // Location
        "LOC" to setOf(PluginPermission.LOCATION),
        "GEO" to setOf(PluginPermission.LOCATION),
        // Contacts
        "CNT" to setOf(PluginPermission.CONTACTS),
        // Calendar
        "CAL" to setOf(PluginPermission.CALENDAR),
        // SMS/Phone
        "SMS" to setOf(PluginPermission.SMS),
        "PHN" to setOf(PluginPermission.PHONE),
        // Sync codes (require network)
        "SCR" to setOf(PluginPermission.NETWORK),
        "SUP" to setOf(PluginPermission.NETWORK),
        "SDL" to setOf(PluginPermission.NETWORK),
        "SQR" to setOf(PluginPermission.NETWORK)
    )

    private val customMappings = mutableMapOf<String, Set<PluginPermission>>()

    /**
     * Get the permissions required to invoke a code.
     * Returns empty set for unregistered codes (permissive for unknown system codes).
     */
    fun getRequiredPermissions(code: String): Set<PluginPermission> =
        customMappings[code] ?: codePermissions[code] ?: emptySet()

    /**
     * Validate that a set of granted permissions covers all declared codes.
     */
    fun validateCodePermissions(
        declaredCodes: Set<String>,
        grantedPermissions: Set<PluginPermission>
    ): CodePermissionValidation {
        val violations = mutableListOf<CodePermissionViolation>()

        for (code in declaredCodes) {
            val required = getRequiredPermissions(code)
            val missing = required - grantedPermissions
            if (missing.isNotEmpty()) {
                violations.add(CodePermissionViolation(code, required, missing))
            }
        }

        return CodePermissionValidation(
            isValid = violations.isEmpty(),
            violations = violations
        )
    }

    /**
     * Register a custom code-to-permission mapping.
     * Used for plugin-declared custom codes.
     */
    fun registerCustomMapping(code: String, permissions: Set<PluginPermission>) {
        customMappings[code] = permissions
    }

    fun clearCustomMappings() {
        customMappings.clear()
    }
}

data class CodePermissionViolation(
    val code: String,
    val requiredPermissions: Set<PluginPermission>,
    val missingPermissions: Set<PluginPermission>
)

data class CodePermissionValidation(
    val isValid: Boolean,
    val violations: List<CodePermissionViolation>
)
