package com.augmentalis.avacode.plugins

/**
 * Validator for plugin functional requirements.
 *
 * Ensures plugins meet the functional requirements defined in the specification.
 */
class PluginRequirementValidator(private val fileIO: FileIO = FileIO()) {

    companion object {
        private const val TAG = "PluginRequirementValidator"

        // Manifest schema versions
        private val SUPPORTED_MANIFEST_VERSIONS = setOf("1", "1.0", "1.0.0")
        private const val CURRENT_MANIFEST_VERSION = "1.0.0"
    }

    /**
     * Validation result for functional requirements.
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val violations: List<RequirementViolation>) : ValidationResult()
    }

    /**
     * Requirement violation.
     */
    data class RequirementViolation(
        val requirementId: String,
        val field: String,
        val message: String
    )

    /**
     * Validate FR-001: Standardized plugin directory structure.
     *
     * Required structure:
     * - plugin-root/
     *   - plugin.yaml (manifest)
     *   - lib/ (libraries)
     *   - assets/ (optional: images/, fonts/, icons/)
     *   - themes/ (optional)
     *   - cache/ (created at runtime)
     *
     * @param pluginRoot Root directory of plugin
     * @param manifest Plugin manifest
     * @return ValidationResult
     */
    fun validateFR001DirectoryStructure(
        pluginRoot: String,
        manifest: PluginManifest
    ): ValidationResult {
        val violations = mutableListOf<RequirementViolation>()

        // Validate plugin root exists
        if (!fileIO.directoryExists(pluginRoot)) {
            violations.add(
                RequirementViolation(
                    "FR-001",
                    "plugin-root",
                    "Plugin root directory does not exist: $pluginRoot"
                )
            )
            return ValidationResult.Invalid(violations)
        }

        // Validate manifest file exists
        val manifestPath = "$pluginRoot/plugin.yaml"
        if (!fileIO.fileExists(manifestPath)) {
            violations.add(
                RequirementViolation(
                    "FR-001",
                    "plugin.yaml",
                    "Plugin manifest file not found: $manifestPath"
                )
            )
        }

        // Validate lib directory exists
        val libDir = "$pluginRoot/lib"
        if (!fileIO.directoryExists(libDir)) {
            violations.add(
                RequirementViolation(
                    "FR-001",
                    "lib/",
                    "Plugin library directory not found: $libDir"
                )
            )
        }

        // Validate optional directories if manifest declares assets
        if (manifest.assets != null) {
            val assetsDir = "$pluginRoot/assets"
            if (!fileIO.directoryExists(assetsDir)) {
                PluginLog.w(TAG, "FR-001 Warning: Manifest declares assets but assets/ directory not found")
            } else {
                // Check for specific asset subdirectories
                if (manifest.assets.images?.isNotEmpty() == true) {
                    val imagesDir = "$assetsDir/images"
                    if (!fileIO.directoryExists(imagesDir)) {
                        violations.add(
                            RequirementViolation(
                                "FR-001",
                                "assets/images/",
                                "Manifest declares images but directory not found: $imagesDir"
                            )
                        )
                    }
                }

                if (manifest.assets.fonts?.isNotEmpty() == true) {
                    val fontsDir = "$assetsDir/fonts"
                    if (!fileIO.directoryExists(fontsDir)) {
                        violations.add(
                            RequirementViolation(
                                "FR-001",
                                "assets/fonts/",
                                "Manifest declares fonts but directory not found: $fontsDir"
                            )
                        )
                    }
                }

                if (manifest.assets.icons?.isNotEmpty() == true) {
                    val iconsDir = "$assetsDir/icons"
                    if (!fileIO.directoryExists(iconsDir)) {
                        violations.add(
                            RequirementViolation(
                                "FR-001",
                                "assets/icons/",
                                "Manifest declares icons but directory not found: $iconsDir"
                            )
                        )
                    }
                }
            }

            // Check for themes directory if declared
            if (manifest.assets.themes?.isNotEmpty() == true) {
                val themesDir = "$pluginRoot/themes"
                if (!fileIO.directoryExists(themesDir)) {
                    violations.add(
                        RequirementViolation(
                            "FR-001",
                            "themes/",
                            "Manifest declares themes but directory not found: $themesDir"
                        )
                    )
                }
            }
        }

        return if (violations.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(violations)
        }
    }

    /**
     * Validate FR-002: Plugin manifest schema validation.
     *
     * Required fields:
     * - id (reverse-domain format)
     * - name
     * - version (semver)
     * - entrypoint (fully qualified class name)
     * - capabilities (list)
     * - dependencies (list with version constraints)
     * - permissions (list)
     *
     * @param manifest Plugin manifest
     * @return ValidationResult
     */
    fun validateFR002ManifestSchema(manifest: PluginManifest): ValidationResult {
        val violations = mutableListOf<RequirementViolation>()

        // Validate required fields are non-empty
        if (manifest.id.isBlank()) {
            violations.add(
                RequirementViolation(
                    "FR-002",
                    "id",
                    "Plugin ID must not be empty"
                )
            )
        }

        if (manifest.name.isBlank()) {
            violations.add(
                RequirementViolation(
                    "FR-002",
                    "name",
                    "Plugin name must not be empty"
                )
            )
        }

        if (manifest.version.isBlank()) {
            violations.add(
                RequirementViolation(
                    "FR-002",
                    "version",
                    "Plugin version must not be empty"
                )
            )
        }

        if (manifest.entrypoint.isBlank()) {
            violations.add(
                RequirementViolation(
                    "FR-002",
                    "entrypoint",
                    "Plugin entrypoint must not be empty"
                )
            )
        }

        // Validate capabilities field exists (can be empty list)
        // This is guaranteed by data class, but we document it

        // Validate dependencies have version constraints
        manifest.dependencies.forEach { dep ->
            if (dep.version.isBlank()) {
                violations.add(
                    RequirementViolation(
                        "FR-002",
                        "dependencies.${dep.pluginId}.version",
                        "Dependency version constraint must not be empty for ${dep.pluginId}"
                    )
                )
            }
        }

        // Validate permissions field exists (can be empty list)
        // This is guaranteed by data class

        // Validate source field
        if (manifest.source.isBlank()) {
            violations.add(
                RequirementViolation(
                    "FR-002",
                    "source",
                    "Plugin source must not be empty"
                )
            )
        }

        // Validate verificationLevel field
        if (manifest.verificationLevel.isBlank()) {
            violations.add(
                RequirementViolation(
                    "FR-002",
                    "verificationLevel",
                    "Plugin verificationLevel must not be empty"
                )
            )
        }

        return if (violations.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(violations)
        }
    }

    /**
     * Validate FR-018: Plugin manifest versioning for backward compatibility.
     *
     * Supports multiple manifest schema versions (v1, v2, etc.)
     *
     * @param manifest Plugin manifest
     * @return ValidationResult
     */
    fun validateFR018ManifestVersioning(manifest: PluginManifest): ValidationResult {
        val violations = mutableListOf<RequirementViolation>()

        // Get manifest schema version
        val manifestSchemaVersion = manifest.manifestVersion

        // Validate schema version is supported
        if (manifestSchemaVersion !in SUPPORTED_MANIFEST_VERSIONS) {
            violations.add(
                RequirementViolation(
                    "FR-018",
                    "manifestVersion",
                    "Unsupported manifest schema version: $manifestSchemaVersion. Supported versions: ${SUPPORTED_MANIFEST_VERSIONS.joinToString()}"
                )
            )
        }

        // Additional version-specific validation can be added here
        when (manifestSchemaVersion) {
            "1", "1.0", "1.0.0" -> {
                // Current version - all validations already done
                PluginLog.d(TAG, "Validating manifest schema version 1.0.0")
            }
            else -> {
                // Future versions would be handled here
                violations.add(
                    RequirementViolation(
                        "FR-018",
                        "manifestVersion",
                        "Unknown manifest schema version: $manifestSchemaVersion"
                    )
                )
            }
        }

        return if (violations.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(violations)
        }
    }

    /**
     * Validate all functional requirements.
     *
     * @param pluginRoot Plugin root directory
     * @param manifest Plugin manifest
     * @return ValidationResult with all violations
     */
    fun validateAll(pluginRoot: String, manifest: PluginManifest): ValidationResult {
        val allViolations = mutableListOf<RequirementViolation>()

        // FR-001: Directory structure
        when (val result = validateFR001DirectoryStructure(pluginRoot, manifest)) {
            is ValidationResult.Invalid -> allViolations.addAll(result.violations)
            is ValidationResult.Valid -> {}
        }

        // FR-002: Manifest schema
        when (val result = validateFR002ManifestSchema(manifest)) {
            is ValidationResult.Invalid -> allViolations.addAll(result.violations)
            is ValidationResult.Valid -> {}
        }

        // FR-018: Manifest versioning
        when (val result = validateFR018ManifestVersioning(manifest)) {
            is ValidationResult.Invalid -> allViolations.addAll(result.violations)
            is ValidationResult.Valid -> {}
        }

        return if (allViolations.isEmpty()) {
            PluginLog.i(TAG, "All functional requirement validations passed for ${manifest.id}")
            ValidationResult.Valid
        } else {
            PluginLog.w(TAG, "Functional requirement violations found: ${allViolations.size}")
            ValidationResult.Invalid(allViolations)
        }
    }
}
