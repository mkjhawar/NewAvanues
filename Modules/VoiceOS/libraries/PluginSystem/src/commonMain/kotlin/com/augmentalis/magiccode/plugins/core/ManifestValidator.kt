package com.augmentalis.magiccode.plugins.core

/**
 * Plugin manifest validator.
 *
 * Validates plugin manifests against comprehensive schema rules and constraints,
 * ensuring that manifests are well-formed and contain valid data before plugin
 * loading proceeds. Validation includes format checks, field constraints, and
 * semantic correctness.
 *
 * ## Validation Rules
 * The validator enforces the following rules:
 *
 * ### Required Fields
 * - **id**: Reverse-domain notation (e.g., com.example.plugin)
 * - **name**: 1-100 alphanumeric characters plus spaces, hyphens, underscores
 * - **version**: Valid semantic version (e.g., 1.2.3)
 * - **author**: 1-200 characters
 * - **entrypoint**: Valid Kotlin class path
 * - **source**: One of PRE_BUNDLED, APPAVENUE_STORE, THIRD_PARTY
 * - **verificationLevel**: One of VERIFIED, REGISTERED, UNVERIFIED
 *
 * ### Optional Fields
 * - **description**: Plugin description
 * - **capabilities**: List of capability identifiers
 * - **dependencies**: List of plugin dependencies with version constraints
 * - **permissions**: List of required permissions
 * - **permissionRationales**: Explanations for permission requests
 * - **assets**: Asset declarations by category
 * - **manifestVersion**: Schema version (default: "1.0")
 * - **homepage**: Plugin homepage URL
 * - **license**: License identifier
 *
 * ## Error Severity
 * Validation errors are classified as:
 * - **ERROR**: Blocking errors that prevent plugin loading
 * - **WARNING**: Non-blocking issues that are logged but allow loading
 *
 * The [PluginConfig.strictManifestValidation] setting controls whether
 * warnings are treated as errors.
 *
 * ## Usage
 * ```kotlin
 * val validator = ManifestValidator(config)
 * val manifest = parseManifest(yamlContent)
 *
 * when (val result = validator.validate(manifest)) {
 *     is ValidationResult.Valid -> {
 *         // Manifest is valid, proceed with loading
 *     }
 *     is ValidationResult.Invalid -> {
 *         // Handle validation errors
 *         result.errors.forEach { error ->
 *             println("${error.field}: ${error.message}")
 *         }
 *     }
 * }
 * ```
 *
 * @property config Plugin system configuration affecting validation behavior
 * @since 1.0.0
 * @see PluginManifest
 * @see PluginLoader
 * @see PluginConfig
 */
class ManifestValidator(private val config: PluginConfig = PluginConfig()) {

    companion object {
        private const val TAG = "ManifestValidator"

        // Regex patterns for validation
        private val PLUGIN_ID_PATTERN = Regex("^[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]*)+$")
        private val SEMVER_PATTERN = Regex("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.]+)?(\\+[a-zA-Z0-9.]+)?$")
        private val CLASS_PATH_PATTERN = Regex("^([a-zA-Z][a-zA-Z0-9]*\\.)*[A-Z][a-zA-Z0-9]*$")
        private val NAME_PATTERN = Regex("^[a-zA-Z0-9 _-]+$")
    }

    /**
     * Result of manifest validation.
     *
     * Encapsulates the outcome of validating a plugin manifest, providing
     * either success confirmation or a list of validation errors.
     *
     * @since 1.0.0
     * @see validate
     */
    sealed class ValidationResult {
        /**
         * Validation succeeded with no errors.
         */
        object Valid : ValidationResult()

        /**
         * Validation failed with one or more errors.
         *
         * @property errors List of validation errors that were found
         */
        data class Invalid(val errors: List<ValidationError>) : ValidationResult()
    }

    /**
     * Details about a single validation error.
     *
     * Contains information about which field failed validation and why,
     * along with severity classification.
     *
     * @property field The manifest field that failed validation
     * @property message Human-readable description of the validation failure
     * @property severity Error severity (ERROR or WARNING)
     * @since 1.0.0
     */
    data class ValidationError(
        val field: String,
        val message: String,
        val severity: Severity = Severity.ERROR
    ) {
        /**
         * Validation error severity levels.
         *
         * @since 1.0.0
         */
        enum class Severity {
            /** Blocking error that prevents plugin loading */
            ERROR,
            /** Non-blocking warning that is logged but allows loading */
            WARNING
        }
    }

    /**
     * Validate plugin manifest against all schema rules.
     *
     * Performs comprehensive validation of all manifest fields, checking:
     * - Required field presence and format
     * - Field value constraints (length, pattern, enum values)
     * - Dependency declaration correctness
     * - Permission validity
     * - Manifest version compatibility
     *
     * Warnings are logged but may not block validation depending on the
     * [PluginConfig.strictManifestValidation] setting.
     *
     * ## Validation Checks
     * 1. Plugin ID format (reverse-domain notation)
     * 2. Name length and character constraints
     * 3. Version semantic versioning format
     * 4. Author field constraints
     * 5. Entrypoint class path format
     * 6. Source enum value validity
     * 7. Verification level enum value validity
     * 8. Permission enum value validity (warnings)
     * 9. Dependency field completeness
     * 10. Manifest version presence
     *
     * @param manifest Plugin manifest to validate
     * @return [ValidationResult.Valid] if all checks pass, or [ValidationResult.Invalid]
     *         with detailed error information if any checks fail
     * @see ValidationResult
     * @see ValidationError
     */
    fun validate(manifest: PluginManifest): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Validate plugin ID
        if (!PLUGIN_ID_PATTERN.matches(manifest.id)) {
            errors.add(
                ValidationError(
                    "id",
                    "Plugin ID must be in reverse-domain notation (e.g., com.example.plugin): ${manifest.id}"
                )
            )
        }

        // Validate plugin name
        if (manifest.name.isBlank()) {
            errors.add(ValidationError("name", "Plugin name cannot be blank"))
        } else if (manifest.name.length > 100) {
            errors.add(ValidationError("name", "Plugin name must be 100 characters or less"))
        } else if (!NAME_PATTERN.matches(manifest.name)) {
            errors.add(
                ValidationError(
                    "name",
                    "Plugin name can only contain letters, numbers, spaces, hyphens, and underscores"
                )
            )
        }

        // Validate version
        if (!SEMVER_PATTERN.matches(manifest.version)) {
            errors.add(
                ValidationError(
                    "version",
                    "Plugin version must be valid semver (e.g., 1.2.3): ${manifest.version}"
                )
            )
        }

        // Validate author
        if (manifest.author.isBlank()) {
            errors.add(ValidationError("author", "Plugin author cannot be blank"))
        } else if (manifest.author.length > 200) {
            errors.add(ValidationError("author", "Plugin author must be 200 characters or less"))
        }

        // Validate entrypoint
        if (!CLASS_PATH_PATTERN.matches(manifest.entrypoint)) {
            errors.add(
                ValidationError(
                    "entrypoint",
                    "Entrypoint must be a valid Kotlin class path (e.g., com.example.MyPlugin): ${manifest.entrypoint}"
                )
            )
        }

        // Validate source
        try {
            PluginSource.valueOf(manifest.source)
        } catch (e: IllegalArgumentException) {
            errors.add(
                ValidationError(
                    "source",
                    "Invalid source value: ${manifest.source}. Must be PRE_BUNDLED, APPAVENUE_STORE, or THIRD_PARTY"
                )
            )
        }

        // Validate verification level
        try {
            DeveloperVerificationLevel.valueOf(manifest.verificationLevel)
        } catch (e: IllegalArgumentException) {
            errors.add(
                ValidationError(
                    "verificationLevel",
                    "Invalid verification level: ${manifest.verificationLevel}. Must be VERIFIED, REGISTERED, or UNVERIFIED"
                )
            )
        }

        // Validate permissions
        manifest.permissions.forEach { permission ->
            try {
                Permission.valueOf(permission)
            } catch (e: IllegalArgumentException) {
                errors.add(
                    ValidationError(
                        "permissions",
                        "Invalid permission: $permission",
                        ValidationError.Severity.WARNING
                    )
                )
            }
        }

        // Validate dependencies
        manifest.dependencies.forEachIndexed { index, dep ->
            if (dep.pluginId.isBlank()) {
                errors.add(ValidationError("dependencies[$index].pluginId", "Dependency plugin ID cannot be blank"))
            }
            if (dep.version.isBlank()) {
                errors.add(ValidationError("dependencies[$index].version", "Dependency version constraint cannot be blank"))
            }
        }

        // Validate manifest version
        if (manifest.manifestVersion.isBlank()) {
            errors.add(
                ValidationError(
                    "manifestVersion",
                    "Manifest version cannot be blank",
                    ValidationError.Severity.WARNING
                )
            )
        }

        // Filter errors based on strict validation setting
        val finalErrors = if (config.strictManifestValidation) {
            errors
        } else {
            errors.filter { it.severity == ValidationError.Severity.ERROR }
        }

        // Log warnings
        errors.filter { it.severity == ValidationError.Severity.WARNING }.forEach { warning ->
            PluginLog.w(TAG, "Manifest validation warning: ${warning.field} - ${warning.message}")
        }

        return if (finalErrors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(finalErrors)
        }
    }

    /**
     * Validate version constraint format.
     *
     * Checks if a version constraint string is well-formed according to
     * supported semantic versioning constraint formats.
     *
     * ## Supported Formats
     * - **Exact version**: `"1.2.3"`
     * - **Caret range**: `"^1.2.3"` (compatible with, allows minor/patch updates)
     * - **Tilde range**: `"~1.2.3"` (approximately, allows patch updates)
     * - **Comparison operators**: `">=1.0.0"`, `"<2.0.0"`, `">=1.5.0 <2.0.0"`
     * - **Wildcard**: `"1.2.*"`, `"1.*"`
     *
     * Note: This performs basic format validation only. Actual version
     * resolution uses a semver library for precise matching.
     *
     * @param constraint Version constraint string to validate
     * @return true if constraint format is valid, false otherwise
     * @see PluginDependency
     */
    fun validateVersionConstraint(constraint: String): Boolean {
        if (constraint.isBlank()) return false

        // Support various constraint formats
        return when {
            // Exact version
            SEMVER_PATTERN.matches(constraint) -> true
            // Caret range (^1.2.3)
            constraint.startsWith("^") && SEMVER_PATTERN.matches(constraint.substring(1)) -> true
            // Tilde range (~1.2.3)
            constraint.startsWith("~") && SEMVER_PATTERN.matches(constraint.substring(1)) -> true
            // Range operators (>=1.0.0 <2.0.0)
            constraint.contains(">=") || constraint.contains("<=") || constraint.contains(">") || constraint.contains("<") -> {
                // Basic validation - detailed parsing done by semver library
                true
            }
            // Wildcard (1.2.*)
            constraint.matches(Regex("^\\d+(\\.\\d+)?(\\.\\*)?$")) -> true
            else -> false
        }
    }
}
