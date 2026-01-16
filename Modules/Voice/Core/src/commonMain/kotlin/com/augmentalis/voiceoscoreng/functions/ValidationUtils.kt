package com.augmentalis.voiceoscoreng.functions

import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.Bounds

/**
 * Utility functions for validating input data in VoiceOSCoreNG.
 *
 * Provides validation for:
 * - VUIDs (Voice Unique Identifiers)
 * - Package names
 * - Element bounds
 * - Resource IDs
 */
object ValidationUtils {

    /**
     * Maximum allowed VUID length (compact format is 16 chars).
     */
    const val MAX_VUID_LENGTH = 24 // With margin for legacy format

    /**
     * Regex pattern for compact VUID format: {pkgHash6}-{typeCode}{hash8}
     * Example: a3f2e1-b917cc9dc
     */
    private val COMPACT_VUID_PATTERN = Regex("^[a-f0-9]{6}-[a-z][a-f0-9]{8}$")

    /**
     * Regex pattern for legacy VUID format.
     * Example: com.instagram.android.v12.0.0.button-a7f3e2c1d4b5
     */
    private val LEGACY_VUID_PATTERN = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+\\.v[0-9.]+\\.[a-z]+-[a-f0-9]{12}$")

    /**
     * Valid type codes for compact VUID format.
     */
    private val VALID_TYPE_CODES = setOf('b', 'i', 's', 't', 'e', 'c', 'l', 'm', 'd', 'g')

    /**
     * Validate a VUID (supports both compact and legacy formats).
     *
     * @param vuid The VUID to validate
     * @return true if valid
     */
    fun isValidVUID(vuid: String): Boolean {
        if (vuid.isEmpty() || vuid.length > MAX_VUID_LENGTH) {
            return false
        }
        return isCompactVUID(vuid) || isLegacyVUID(vuid)
    }

    /**
     * Check if VUID is in compact format.
     *
     * @param vuid The VUID to check
     * @return true if compact format
     */
    fun isCompactVUID(vuid: String): Boolean {
        return COMPACT_VUID_PATTERN.matches(vuid)
    }

    /**
     * Check if VUID is in legacy format.
     *
     * @param vuid The VUID to check
     * @return true if legacy format
     */
    fun isLegacyVUID(vuid: String): Boolean {
        return LEGACY_VUID_PATTERN.matches(vuid)
    }

    /**
     * Validate a package name.
     *
     * @param packageName The package name to validate
     * @return true if valid Android package name format
     */
    fun isValidPackageName(packageName: String): Boolean {
        if (packageName.isEmpty() || packageName.length > 256) {
            return false
        }

        // Must have at least one dot
        if (!packageName.contains('.')) {
            return false
        }

        // Split by dots and validate each segment
        val segments = packageName.split('.')
        if (segments.any { it.isEmpty() }) {
            return false
        }

        // Each segment must start with letter/underscore
        // and contain only alphanumeric/underscore
        val segmentPattern = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
        return segments.all { segmentPattern.matches(it) }
    }

    /**
     * Validate a resource ID format.
     *
     * @param resourceId The resource ID to validate
     * @return true if valid format (e.g., "com.app:id/button_name")
     */
    fun isValidResourceId(resourceId: String): Boolean {
        if (resourceId.isEmpty()) {
            return true // Empty is valid (optional field)
        }

        // Format: package:type/name
        val pattern = Regex("^[a-z][a-z0-9_.]*:[a-z]+/[a-z_][a-z0-9_]*$")
        return pattern.matches(resourceId)
    }

    /**
     * Validate element bounds.
     *
     * @param bounds The bounds to validate
     * @return true if valid (non-negative, right > left, bottom > top)
     */
    fun isValidBounds(bounds: Bounds): Boolean {
        return bounds.left >= 0 &&
               bounds.top >= 0 &&
               bounds.right > bounds.left &&
               bounds.bottom > bounds.top
    }

    /**
     * Validate element bounds from string format.
     *
     * @param boundsStr The bounds string (e.g., "10,20,100,50")
     * @return true if valid format and values
     */
    fun isValidBoundsString(boundsStr: String): Boolean {
        val bounds = Bounds.fromString(boundsStr) ?: return false
        return isValidBounds(bounds)
    }

    /**
     * Validate an ElementInfo object.
     *
     * @param element The element to validate
     * @return ValidationResult with any issues found
     */
    fun validateElement(element: ElementInfo): ValidationResult {
        val issues = mutableListOf<String>()

        if (element.className.isEmpty()) {
            issues.add("className is required")
        }

        if (element.resourceId.isNotEmpty() && !isValidResourceId(element.resourceId)) {
            issues.add("Invalid resourceId format")
        }

        if (element.bounds != Bounds.EMPTY && !isValidBounds(element.bounds)) {
            issues.add("Invalid bounds: must have positive dimensions")
        }

        if (element.packageName.isNotEmpty() && !isValidPackageName(element.packageName)) {
            issues.add("Invalid packageName format")
        }

        return ValidationResult(
            isValid = issues.isEmpty(),
            issues = issues
        )
    }

    /**
     * Validate a voice label for command generation.
     *
     * @param label The voice label
     * @return true if suitable for voice commands
     */
    fun isValidVoiceLabel(label: String): Boolean {
        if (label.isBlank()) return false
        if (label.length > 100) return false // Too long for voice command

        // Should contain at least one pronounceable character
        return label.any { it.isLetter() }
    }

    /**
     * Sanitize a string for safe use in commands/database.
     * Removes potentially dangerous characters.
     *
     * @param input The input string
     * @return Sanitized string
     */
    fun sanitize(input: String): String {
        // Remove control characters and dangerous SQL/shell characters
        return input
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "") // Control chars
            .replace(Regex("[;'\"\\\\]"), "") // SQL injection chars
            .replace(Regex("[|&\$`]"), "") // Shell injection chars
            .trim()
    }

    /**
     * Check if input contains potentially dangerous content.
     *
     * @param input The input to check
     * @return true if potentially dangerous
     */
    fun containsDangerousContent(input: String): Boolean {
        val dangerousPatterns = listOf(
            // SQL injection patterns - require SQL-like context
            Regex("(?i)(drop|delete|insert|update)\\s+(table|from|into|set)\\s+", RegexOption.IGNORE_CASE),
            // SQL comment injection
            Regex("['\";].*--"),
            // Template/variable injection
            Regex("\\$\\{.*\\}"),
            // Backtick command execution
            Regex("`.*`")
        )

        return dangerousPatterns.any { it.containsMatchIn(input) }
    }
}

/**
 * Result of validation containing validity status and any issues found.
 */
data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String> = emptyList()
) {
    val hasIssues: Boolean get() = issues.isNotEmpty()

    fun getIssuesString(separator: String = "; "): String {
        return issues.joinToString(separator)
    }
}
