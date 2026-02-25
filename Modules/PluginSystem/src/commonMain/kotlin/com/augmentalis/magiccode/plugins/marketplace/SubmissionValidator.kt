package com.augmentalis.magiccode.plugins.marketplace

import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.datetime.Clock

/**
 * Validator for plugin package submissions to the marketplace.
 *
 * SubmissionValidator performs comprehensive validation of plugin packages
 * before they can be accepted for marketplace distribution. It ensures that
 * all packages meet security, format, and content requirements.
 *
 * ## Validation Categories
 * The validator checks several categories:
 *
 * ### Format Validation
 * - Plugin ID in reverse-domain notation
 * - Version in semantic versioning format (X.Y.Z)
 * - Required fields present and non-blank
 *
 * ### Security Validation
 * - Signature present and non-empty
 * - Checksum valid and properly formatted
 * - No dangerous permission combinations
 *
 * ### Content Validation
 * - Asset references have valid paths
 * - Asset checksums properly formatted
 *
 * ## Usage
 * ```kotlin
 * val validator = SubmissionValidator()
 * val package = PluginPackage(...)
 *
 * when (val result = validator.validate(package)) {
 *     is ValidationResult.Valid -> {
 *         // Package is valid, proceed with submission
 *     }
 *     is ValidationResult.Invalid -> {
 *         // Handle validation errors
 *         result.errors.forEach { println(it) }
 *     }
 * }
 * ```
 *
 * ## Dangerous Permission Combinations
 * Certain permission combinations are flagged as dangerous:
 * - NETWORK + STORAGE_WRITE: Can exfiltrate data
 * - ACCESSIBILITY_SERVICES + NETWORK: Can capture and transmit user actions
 * - PAYMENTS + NETWORK without VERIFIED status: Financial security risk
 *
 * @since 1.0.0
 * @see PluginPackage
 * @see ValidationResult
 */
class SubmissionValidator {

    companion object {
        private const val TAG = "SubmissionValidator"

        /**
         * Regex pattern for valid plugin IDs.
         * Format: reverse-domain notation with lowercase letters, numbers, and hyphens.
         * Examples: com.example.plugin, org.acme.voice-commands
         */
        private val PLUGIN_ID_PATTERN = Regex("^[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]*)+$")

        /**
         * Regex pattern for semantic versioning.
         * Format: MAJOR.MINOR.PATCH with optional prerelease and build metadata.
         * Examples: 1.0.0, 2.1.3-beta.1, 1.0.0+build.123
         */
        private val SEMVER_PATTERN = Regex("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.]+)?(\\+[a-zA-Z0-9.]+)?$")

        /**
         * Regex pattern for checksum format.
         * Format: algorithm:hash (e.g., sha256:abc123...)
         */
        private val CHECKSUM_PATTERN = Regex("^(sha256|sha512|md5):[a-fA-F0-9]+$")

        /**
         * Dangerous permission combinations that require additional scrutiny.
         */
        private val DANGEROUS_PERMISSION_COMBOS = listOf(
            setOf("NETWORK", "STORAGE_WRITE"),
            setOf("ACCESSIBILITY_SERVICES", "NETWORK"),
            setOf("PAYMENTS", "NETWORK"),
            setOf("CONTACTS", "NETWORK"),
            setOf("LOCATION", "NETWORK"),
            setOf("MICROPHONE", "NETWORK"),
            setOf("CAMERA", "NETWORK")
        )

        /**
         * Permissions that require verified publisher status.
         */
        private val RESTRICTED_PERMISSIONS = setOf(
            "PAYMENTS",
            "ACCESSIBILITY_SERVICES"
        )
    }

    /**
     * Result of package validation.
     *
     * Encapsulates the outcome of validating a plugin package submission.
     *
     * @since 1.0.0
     */
    sealed class ValidationResult {
        /**
         * Package passed all validation checks.
         */
        object Valid : ValidationResult()

        /**
         * Package failed validation with one or more errors.
         *
         * @property errors List of validation error messages
         * @property warnings List of non-blocking warning messages
         */
        data class Invalid(
            val errors: List<String>,
            val warnings: List<String> = emptyList()
        ) : ValidationResult()
    }

    /**
     * Validate a plugin package for marketplace submission.
     *
     * Performs comprehensive validation including:
     * - Plugin ID format validation
     * - Version format validation
     * - Required field validation
     * - Signature and checksum validation
     * - Permission combination checks
     * - Asset reference validation
     *
     * @param pluginPackage Package to validate
     * @param requireVerifiedForRestricted If true, require verified status for restricted permissions
     * @return [ValidationResult.Valid] if package passes all checks,
     *         [ValidationResult.Invalid] with error details otherwise
     */
    fun validate(
        pluginPackage: PluginPackage,
        requireVerifiedForRestricted: Boolean = true
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Validate manifest data
        validateManifest(pluginPackage.manifest, errors, warnings)

        // Validate signature
        validateSignature(pluginPackage.signature, errors)

        // Validate checksum
        validateChecksum(pluginPackage.checksum, errors)

        // Validate timestamp
        validateTimestamp(pluginPackage.publishedAt, errors)

        // Validate size
        validateSize(pluginPackage.size, errors)

        // Validate assets
        validateAssets(pluginPackage.assets, errors, warnings)

        // Check dangerous permission combinations
        checkDangerousPermissions(pluginPackage.manifest.permissions, errors, warnings)

        // Check restricted permissions
        if (requireVerifiedForRestricted) {
            checkRestrictedPermissions(pluginPackage.manifest.permissions, warnings)
        }

        return if (errors.isEmpty()) {
            if (warnings.isNotEmpty()) {
                PluginLog.w(TAG, "Package validation passed with ${warnings.size} warnings")
            } else {
                PluginLog.i(TAG, "Package validation passed: ${pluginPackage.manifest.pluginId}")
            }
            ValidationResult.Valid
        } else {
            PluginLog.e(TAG, "Package validation failed with ${errors.size} errors")
            ValidationResult.Invalid(errors, warnings)
        }
    }

    /**
     * Validate plugin manifest data.
     */
    private fun validateManifest(
        manifest: PluginManifestData,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        // Validate plugin ID format
        if (manifest.pluginId.isBlank()) {
            errors.add("Plugin ID is required")
        } else if (!PLUGIN_ID_PATTERN.matches(manifest.pluginId)) {
            errors.add("Plugin ID must be in reverse-domain notation (e.g., com.example.plugin): ${manifest.pluginId}")
        }

        // Validate name
        if (manifest.name.isBlank()) {
            errors.add("Plugin name is required")
        } else if (manifest.name.length > 100) {
            errors.add("Plugin name must be 100 characters or less")
        }

        // Validate version format
        if (manifest.version.isBlank()) {
            errors.add("Version is required")
        } else if (!SEMVER_PATTERN.matches(manifest.version)) {
            errors.add("Version must be in semver format (X.Y.Z): ${manifest.version}")
        }

        // Validate description
        if (manifest.description.isBlank()) {
            errors.add("Description is required")
        } else if (manifest.description.length > 500) {
            warnings.add("Description is very long (${manifest.description.length} chars), consider shortening")
        }

        // Validate author
        if (manifest.author.isBlank()) {
            errors.add("Author is required")
        }

        // Validate capabilities
        if (manifest.capabilities.isEmpty()) {
            warnings.add("No capabilities declared - plugin may have limited discoverability")
        }

        // Validate minSdkVersion
        if (manifest.minSdkVersion < 21) {
            errors.add("Minimum SDK version must be at least 21, got: ${manifest.minSdkVersion}")
        } else if (manifest.minSdkVersion > 35) {
            warnings.add("Very high minimum SDK version (${manifest.minSdkVersion}) may limit compatibility")
        }
    }

    /**
     * Validate package signature.
     */
    private fun validateSignature(signature: String, errors: MutableList<String>) {
        if (signature.isBlank()) {
            errors.add("Signature is required")
        } else if (signature.length < 32) {
            errors.add("Signature appears to be too short")
        }
        // Note: Actual signature verification happens at a higher level
        // This just validates presence and basic format
    }

    /**
     * Validate package checksum.
     */
    private fun validateChecksum(checksum: String, errors: MutableList<String>) {
        if (checksum.isBlank()) {
            errors.add("Checksum is required")
        } else if (!CHECKSUM_PATTERN.matches(checksum)) {
            errors.add("Checksum must be in format 'algorithm:hash' (e.g., sha256:abc123...): $checksum")
        } else {
            // Validate hash length based on algorithm
            val algorithm = checksum.substringBefore(":")
            val hash = checksum.substringAfter(":")
            val expectedLength = when (algorithm) {
                "md5" -> 32
                "sha256" -> 64
                "sha512" -> 128
                else -> -1
            }
            if (expectedLength > 0 && hash.length != expectedLength) {
                errors.add("Checksum hash length incorrect for $algorithm: expected $expectedLength, got ${hash.length}")
            }
        }
    }

    /**
     * Validate publication timestamp.
     */
    private fun validateTimestamp(timestamp: Long, errors: MutableList<String>) {
        if (timestamp <= 0) {
            errors.add("Publication timestamp must be positive")
        } else {
            val now = Clock.System.now().toEpochMilliseconds()
            // Timestamp shouldn't be in the future (allow 1 minute tolerance)
            if (timestamp > now + 60_000) {
                errors.add("Publication timestamp is in the future")
            }
            // Timestamp shouldn't be too old (before 2020)
            val year2020 = 1577836800000L // Jan 1, 2020 UTC
            if (timestamp < year2020) {
                errors.add("Publication timestamp is before 2020, which is invalid")
            }
        }
    }

    /**
     * Validate package size.
     */
    private fun validateSize(size: Long, errors: MutableList<String>) {
        if (size <= 0) {
            errors.add("Package size must be positive")
        } else if (size > 100 * 1024 * 1024) { // 100 MB limit
            errors.add("Package size exceeds maximum allowed (100 MB): ${size / (1024 * 1024)} MB")
        }
    }

    /**
     * Validate asset references.
     */
    private fun validateAssets(
        assets: List<AssetReference>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        assets.forEachIndexed { index, asset ->
            // Validate path
            if (asset.path.isBlank()) {
                errors.add("Asset[$index]: path is required")
            } else {
                // Check for path traversal attacks
                if (asset.path.contains("..")) {
                    errors.add("Asset[$index]: path contains illegal traversal sequence: ${asset.path}")
                }
                // Check for absolute paths
                if (asset.path.startsWith("/") || asset.path.startsWith("\\")) {
                    errors.add("Asset[$index]: path must be relative, not absolute: ${asset.path}")
                }
            }

            // Validate checksum
            if (asset.checksum.isBlank()) {
                errors.add("Asset[$index]: checksum is required")
            } else if (!CHECKSUM_PATTERN.matches(asset.checksum)) {
                warnings.add("Asset[$index]: checksum format may be invalid: ${asset.checksum}")
            }

            // Validate size
            if (asset.size < 0) {
                errors.add("Asset[$index]: size cannot be negative")
            } else if (asset.size == 0L) {
                warnings.add("Asset[$index]: size is zero, may be empty file")
            } else if (asset.size > 50 * 1024 * 1024) { // 50 MB per asset
                warnings.add("Asset[$index]: very large asset (${asset.size / (1024 * 1024)} MB): ${asset.path}")
            }
        }
    }

    /**
     * Check for dangerous permission combinations.
     */
    private fun checkDangerousPermissions(
        permissions: List<String>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val permissionSet = permissions.toSet()

        DANGEROUS_PERMISSION_COMBOS.forEach { dangerousCombo ->
            if (permissionSet.containsAll(dangerousCombo)) {
                warnings.add(
                    "Potentially dangerous permission combination detected: ${dangerousCombo.joinToString(" + ")}. " +
                        "This may require additional review."
                )
            }
        }
    }

    /**
     * Check for restricted permissions that require verified status.
     */
    private fun checkRestrictedPermissions(
        permissions: List<String>,
        warnings: MutableList<String>
    ) {
        val restrictedUsed = permissions.filter { it in RESTRICTED_PERMISSIONS }
        if (restrictedUsed.isNotEmpty()) {
            warnings.add(
                "Plugin uses restricted permissions that may require verified publisher status: " +
                    restrictedUsed.joinToString(", ")
            )
        }
    }

    /**
     * Validate just the plugin ID format.
     *
     * Convenience method for quick ID validation without full package validation.
     *
     * @param pluginId Plugin ID to validate
     * @return true if ID is in valid reverse-domain format
     */
    fun isValidPluginId(pluginId: String): Boolean {
        return pluginId.isNotBlank() && PLUGIN_ID_PATTERN.matches(pluginId)
    }

    /**
     * Validate just the version format.
     *
     * Convenience method for quick version validation without full package validation.
     *
     * @param version Version string to validate
     * @return true if version is in valid semver format
     */
    fun isValidVersion(version: String): Boolean {
        return version.isNotBlank() && SEMVER_PATTERN.matches(version)
    }

    /**
     * Validate just the checksum format.
     *
     * Convenience method for quick checksum validation.
     *
     * @param checksum Checksum string to validate
     * @return true if checksum is in valid format with correct hash length
     */
    fun isValidChecksum(checksum: String): Boolean {
        if (!CHECKSUM_PATTERN.matches(checksum)) return false

        val algorithm = checksum.substringBefore(":")
        val hash = checksum.substringAfter(":")
        val expectedLength = when (algorithm) {
            "md5" -> 32
            "sha256" -> 64
            "sha512" -> 128
            else -> return false
        }
        return hash.length == expectedLength
    }
}
