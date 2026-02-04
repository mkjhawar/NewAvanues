package com.augmentalis.magicelements.core.mel

import com.augmentalis.magicelements.core.mel.functions.PluginTier

/**
 * Validator for plugin definitions.
 *
 * Validates plugin metadata, state schema, reducers, UI tree, and tier requirements.
 * Returns detailed validation errors for debugging.
 *
 * Usage:
 * ```kotlin
 * val validator = PluginValidator()
 * val errors = validator.validate(pluginDefinition)
 * if (errors.isNotEmpty()) {
 *     println("Validation errors:")
 *     errors.forEach { println("  - $it") }
 * }
 * ```
 */
class PluginValidator {
    /**
     * Validate a plugin definition.
     *
     * @param definition Plugin definition to validate
     * @param platform Target platform (for tier validation)
     * @return List of validation errors (empty if valid)
     */
    fun validate(definition: PluginDefinition, platform: Platform = TierDetector.detectPlatform()): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Validate metadata
        errors.addAll(validateMetadata(definition.metadata))

        // Validate state schema
        errors.addAll(validateState(definition.state))

        // Validate reducers
        errors.addAll(validateReducers(definition.reducers, definition.state))

        // Validate scripts (Tier 2 only)
        errors.addAll(validateScripts(definition.scripts, definition.tier))

        // Validate UI tree
        errors.addAll(validateUITree(definition.ui, definition.state, definition.reducers))

        // Validate tier requirements
        errors.addAll(validateTier(definition.tier, platform, definition.scripts))

        return errors
    }

    /**
     * Validate metadata.
     */
    private fun validateMetadata(metadata: PluginMetadataJson): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Validate ID format (reverse domain notation)
        if (!metadata.id.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"))) {
            errors.add(ValidationError(
                field = "metadata.id",
                message = "ID must be in reverse domain notation (e.g., com.example.plugin)",
                severity = ErrorSeverity.ERROR
            ))
        }

        // Validate name (non-empty)
        if (metadata.name.isBlank()) {
            errors.add(ValidationError(
                field = "metadata.name",
                message = "Name cannot be empty",
                severity = ErrorSeverity.ERROR
            ))
        }

        // Validate version (semantic versioning)
        if (!metadata.version.matches(Regex("^\\d+\\.\\d+\\.\\d+$"))) {
            errors.add(ValidationError(
                field = "metadata.version",
                message = "Version must follow semantic versioning (e.g., 1.0.0)",
                severity = ErrorSeverity.ERROR
            ))
        }

        // Validate SDK version
        if (!metadata.minSdkVersion.matches(Regex("^\\d+\\.\\d+\\.\\d+$"))) {
            errors.add(ValidationError(
                field = "metadata.minSdkVersion",
                message = "SDK version must follow semantic versioning (e.g., 1.0.0)",
                severity = ErrorSeverity.ERROR
            ))
        }

        return errors
    }

    /**
     * Validate state schema.
     */
    private fun validateState(state: Map<String, StateVariable>): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Check for reserved state variable names
        val reserved = setOf("length", "keys", "values", "toString", "constructor")
        state.keys.forEach { name ->
            if (name in reserved) {
                errors.add(ValidationError(
                    field = "state.$name",
                    message = "State variable name '$name' is reserved",
                    severity = ErrorSeverity.ERROR
                ))
            }
        }

        // Validate variable names (alphanumeric + underscore, must start with letter)
        state.keys.forEach { name ->
            if (!name.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))) {
                errors.add(ValidationError(
                    field = "state.$name",
                    message = "Variable name must start with letter and contain only alphanumeric and underscore",
                    severity = ErrorSeverity.ERROR
                ))
            }
        }

        return errors
    }

    /**
     * Validate reducers.
     */
    private fun validateReducers(
        reducers: Map<String, Reducer>,
        state: Map<String, StateVariable>
    ): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        reducers.forEach { (name, reducer) ->
            // Validate reducer name
            if (!name.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))) {
                errors.add(ValidationError(
                    field = "reducers.$name",
                    message = "Reducer name must start with letter and contain only alphanumeric and underscore",
                    severity = ErrorSeverity.ERROR
                ))
            }

            // Validate parameter names
            reducer.params.forEach { param ->
                if (!param.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))) {
                    errors.add(ValidationError(
                        field = "reducers.$name.params",
                        message = "Parameter name '$param' must start with letter and contain only alphanumeric and underscore",
                        severity = ErrorSeverity.ERROR
                    ))
                }
            }

            // Validate next_state references
            reducer.nextState.forEach { (stateVar, _) ->
                if (stateVar !in state) {
                    errors.add(ValidationError(
                        field = "reducers.$name.next_state.$stateVar",
                        message = "State variable '$stateVar' not defined in state schema",
                        severity = ErrorSeverity.ERROR
                    ))
                }
            }

            // Validate effects (must be null or empty for Tier 1)
            if (reducer.isTier2Only()) {
                errors.add(ValidationError(
                    field = "reducers.$name.effects",
                    message = "Reducer has Tier 2 features (effects), but tier not specified as LOGIC",
                    severity = ErrorSeverity.WARNING
                ))
            }
        }

        return errors
    }

    /**
     * Validate scripts.
     */
    private fun validateScripts(
        scripts: Map<String, Script>?,
        tier: PluginTier
    ): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (!scripts.isNullOrEmpty() && tier == PluginTier.DATA) {
            errors.add(ValidationError(
                field = "scripts",
                message = "Scripts are only allowed in Tier 2 (LOGIC) plugins",
                severity = ErrorSeverity.ERROR
            ))
        }

        scripts?.forEach { (name, script) ->
            // Validate script name
            if (!name.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))) {
                errors.add(ValidationError(
                    field = "scripts.$name",
                    message = "Script name must start with letter and contain only alphanumeric and underscore",
                    severity = ErrorSeverity.ERROR
                ))
            }

            // Validate parameter names
            script.params.forEach { param ->
                if (!param.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))) {
                    errors.add(ValidationError(
                        field = "scripts.$name.params",
                        message = "Parameter name '$param' must start with letter and contain only alphanumeric and underscore",
                        severity = ErrorSeverity.ERROR
                    ))
                }
            }

            // Validate body is not empty
            if (script.body.isBlank()) {
                errors.add(ValidationError(
                    field = "scripts.$name.body",
                    message = "Script body cannot be empty",
                    severity = ErrorSeverity.ERROR
                ))
            }
        }

        return errors
    }

    /**
     * Validate UI tree.
     */
    private fun validateUITree(
        ui: UINode,
        state: Map<String, StateVariable>,
        reducers: Map<String, Reducer>,
        path: String = "ui"
    ): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Validate component type is not empty
        if (ui.type.isBlank()) {
            errors.add(ValidationError(
                field = path,
                message = "Component type cannot be empty",
                severity = ErrorSeverity.ERROR
            ))
        }

        // Validate bindings reference valid state variables
        ui.bindings.forEach { (prop, expr) ->
            // TODO: Parse expression and validate state references
            // For now, just check if expression starts with $state
            if (expr.raw.startsWith("$state.")) {
                val stateRef = expr.raw.removePrefix("$state.").split(".")[0]
                if (stateRef !in state) {
                    errors.add(ValidationError(
                        field = "$path.bindings.$prop",
                        message = "Binding references undefined state variable: $stateRef",
                        severity = ErrorSeverity.WARNING
                    ))
                }
            }
        }

        // Validate event handlers reference valid reducers
        ui.events.forEach { (event, handler) ->
            if (handler.reducer !in reducers) {
                errors.add(ValidationError(
                    field = "$path.events.$event",
                    message = "Event handler references undefined reducer: ${handler.reducer}",
                    severity = ErrorSeverity.ERROR
                ))
            }

            // Validate event parameters match reducer params
            val reducer = reducers[handler.reducer]
            if (reducer != null) {
                val requiredParams = reducer.params.toSet()
                val providedParams = handler.params.keys
                val missing = requiredParams - providedParams
                val extra = providedParams - requiredParams

                if (missing.isNotEmpty()) {
                    errors.add(ValidationError(
                        field = "$path.events.$event",
                        message = "Missing required parameters for reducer '${handler.reducer}': ${missing.joinToString()}",
                        severity = ErrorSeverity.ERROR
                    ))
                }

                if (extra.isNotEmpty()) {
                    errors.add(ValidationError(
                        field = "$path.events.$event",
                        message = "Extra parameters for reducer '${handler.reducer}': ${extra.joinToString()}",
                        severity = ErrorSeverity.WARNING
                    ))
                }
            }
        }

        // Recursively validate children
        ui.children?.forEachIndexed { index, child ->
            errors.addAll(validateUITree(child, state, reducers, "$path.children[$index]"))
        }

        return errors
    }

    /**
     * Validate tier requirements.
     */
    private fun validateTier(
        tier: PluginTier,
        platform: Platform,
        scripts: Map<String, Script>?
    ): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val maxTier = TierDetector.getMaxTier(platform)

        // Warn if plugin will be downgraded
        if (tier == PluginTier.LOGIC && maxTier == PluginTier.DATA) {
            errors.add(ValidationError(
                field = "tier",
                message = "Plugin tier will be downgraded to DATA on ${TierDetector.getPlatformName(platform)} (Apple platform)",
                severity = ErrorSeverity.WARNING
            ))

            // Warn about scripts being ignored
            if (!scripts.isNullOrEmpty()) {
                errors.add(ValidationError(
                    field = "scripts",
                    message = "Scripts will be ignored on ${TierDetector.getPlatformName(platform)} (Tier 1 only)",
                    severity = ErrorSeverity.WARNING
                ))
            }
        }

        return errors
    }

    /**
     * Check if validation passed (no errors, warnings allowed).
     */
    fun isValid(errors: List<ValidationError>): Boolean {
        return errors.none { it.severity == ErrorSeverity.ERROR }
    }

    /**
     * Get only error-level issues.
     */
    fun getErrors(errors: List<ValidationError>): List<ValidationError> {
        return errors.filter { it.severity == ErrorSeverity.ERROR }
    }

    /**
     * Get only warning-level issues.
     */
    fun getWarnings(errors: List<ValidationError>): List<ValidationError> {
        return errors.filter { it.severity == ErrorSeverity.WARNING }
    }
}

/**
 * Validation error.
 *
 * @property field Field path where error occurred
 * @property message Error message
 * @property severity Error severity level
 * @property suggestion Optional suggestion for fixing the error
 */
data class ValidationError(
    val field: String,
    val message: String,
    val severity: ErrorSeverity,
    val suggestion: String? = null
) {
    override fun toString(): String {
        val prefix = when (severity) {
            ErrorSeverity.ERROR -> "ERROR"
            ErrorSeverity.WARNING -> "WARNING"
        }
        val suffix = if (suggestion != null) " (Suggestion: $suggestion)" else ""
        return "[$prefix] $field: $message$suffix"
    }
}

/**
 * Error severity levels.
 */
enum class ErrorSeverity {
    /** Critical error that prevents plugin from loading */
    ERROR,

    /** Warning that indicates potential issues but allows loading */
    WARNING
}
