/**
 * FlutterIdentifierExtractor.kt - Extract stable identifiers from Flutter 3.19+ apps
 *
 * Flutter 3.19 (Feb 2024) introduced SemanticsProperties.identifier which maps to
 * Android's resource-id (viewIdResourceName). This provides stable element identification
 * vs content-based hashing that breaks when UI text changes.
 *
 * Author: AI Code Assistant
 * Created: 2025-12-27
 * Related: VoiceOS-CrossPlatform-Scraping-Analysis-251227-V1.md
 */
package com.augmentalis.avidcreator.flutter

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Extracts stable identifiers from Flutter 3.19+ apps.
 *
 * Flutter 3.19+ maps SemanticsProperties.identifier to Android's viewIdResourceName:
 * - Pattern: "flutter_semantics_<id>" or custom developer-defined identifiers
 * - These are stable across app sessions (unlike content-based hashes)
 *
 * Usage:
 * ```kotlin
 * val identifier = FlutterIdentifierExtractor.extract(node)
 * if (identifier != null) {
 *     // Use stable identifier for VUID generation
 *     val vuid = AvidGenerator.generateFromFlutterIdentifier(identifier)
 * }
 * ```
 */
object FlutterIdentifierExtractor {

    /**
     * Flutter identifier patterns in viewIdResourceName
     *
     * Flutter 3.19+ sets resource-id from Semantics.identifier:
     * - "flutter_semantics_<number>" - auto-generated IDs
     * - "flutter_<custom_id>" - developer-defined IDs via Semantics(identifier: "custom_id")
     * - "<package>:id/flutter_<id>" - fully qualified resource IDs
     */
    private val FLUTTER_ID_PATTERNS = listOf(
        "flutter_semantics_",      // Auto-generated semantics IDs
        "flutter_",                // Custom developer-defined IDs
        ":id/flutter_",            // Fully qualified resource ID format
        "SemanticsId"              // Alternative semantics marker
    )

    /**
     * Patterns indicating Flutter 3.19+ identifier (stable)
     *
     * Pre-3.19 Flutter apps may have flutter_ prefixes but without stable identifier support.
     * We detect 3.19+ by looking for semantics-specific patterns.
     */
    private val FLUTTER_319_PATTERNS = listOf(
        "flutter_semantics_",
        "flutter_id_",
        ":id/flutter_semantics_"
    )

    /**
     * Extract Flutter identifier from AccessibilityNodeInfo
     *
     * @param node The accessibility node to extract identifier from
     * @return Flutter identifier if present and stable, null otherwise
     */
    fun extract(node: AccessibilityNodeInfo): FlutterIdentifier? {
        val resourceId = node.viewIdResourceName ?: return null

        // Check if this is a Flutter identifier
        val isFlutterIdentifier = FLUTTER_ID_PATTERNS.any { pattern ->
            resourceId.contains(pattern, ignoreCase = true)
        }

        if (!isFlutterIdentifier) return null

        // Determine if this is Flutter 3.19+ (stable identifier)
        val isFlutter319 = FLUTTER_319_PATTERNS.any { pattern ->
            resourceId.contains(pattern, ignoreCase = true)
        }

        // Extract the actual identifier value
        val identifierValue = extractIdentifierValue(resourceId)

        return FlutterIdentifier(
            rawResourceId = resourceId,
            identifierValue = identifierValue,
            isStable = isFlutter319,
            source = if (isFlutter319) IdentifierSource.FLUTTER_319_SEMANTICS else IdentifierSource.FLUTTER_LEGACY
        )
    }

    /**
     * Check if node has a Flutter 3.19+ stable identifier
     *
     * Quick check without full extraction, useful for filtering.
     *
     * @param node The accessibility node to check
     * @return true if node has a stable Flutter identifier
     */
    fun hasStableIdentifier(node: AccessibilityNodeInfo): Boolean {
        val resourceId = node.viewIdResourceName ?: return false
        return FLUTTER_319_PATTERNS.any { pattern ->
            resourceId.contains(pattern, ignoreCase = true)
        }
    }

    /**
     * Check if node appears to be from a Flutter app
     *
     * @param node The accessibility node to check
     * @return true if node appears to be from Flutter
     */
    fun isFlutterNode(node: AccessibilityNodeInfo): Boolean {
        val resourceId = node.viewIdResourceName ?: return false
        return FLUTTER_ID_PATTERNS.any { pattern ->
            resourceId.contains(pattern, ignoreCase = true)
        }
    }

    /**
     * Extract the identifier value from a Flutter resource ID
     *
     * Examples:
     * - "com.example:id/flutter_semantics_42" -> "flutter_semantics_42"
     * - "flutter_login_button" -> "flutter_login_button"
     * - "flutter_semantics_123" -> "flutter_semantics_123"
     */
    private fun extractIdentifierValue(resourceId: String): String {
        // Remove package prefix if present (e.g., "com.example:id/")
        val withoutPackage = if (resourceId.contains(":id/")) {
            resourceId.substringAfter(":id/")
        } else {
            resourceId
        }

        return withoutPackage.trim()
    }
}

/**
 * Represents a Flutter identifier extracted from an accessibility node
 */
data class FlutterIdentifier(
    /** Raw resource ID from viewIdResourceName */
    val rawResourceId: String,

    /** Extracted identifier value (without package prefix) */
    val identifierValue: String,

    /** Whether this is a stable Flutter 3.19+ identifier */
    val isStable: Boolean,

    /** Source/type of the identifier */
    val source: IdentifierSource
) {
    /**
     * Generate a stable hash for VUID generation
     *
     * Uses SHA-256 for collision resistance.
     */
    fun toStableHash(): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(identifierValue.toByteArray())
        return hashBytes.take(16).joinToString("") { "%02x".format(it) }
    }
}

/**
 * Source of the Flutter identifier
 */
enum class IdentifierSource {
    /** Flutter 3.19+ SemanticsProperties.identifier - stable across sessions */
    FLUTTER_319_SEMANTICS,

    /** Legacy Flutter identifier - may not be stable */
    FLUTTER_LEGACY,

    /** Developer-defined custom identifier */
    FLUTTER_CUSTOM
}
