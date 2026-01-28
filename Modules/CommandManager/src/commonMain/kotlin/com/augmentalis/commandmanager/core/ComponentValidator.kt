/**
 * ComponentValidator.kt - Validation for component definitions
 *
 * Validates ComponentDefinition instances for correctness and completeness.
 * Reports errors (blocking) and warnings (non-blocking).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 * Refactored: 2026-01-08 (SRP extraction from ComponentFactory.kt)
 */
package com.augmentalis.commandmanager

/**
 * Validator for ComponentDefinition.
 */
object ComponentValidator {

    /**
     * Validate a component definition.
     */
    fun validate(definition: ComponentDefinition): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()

        // Validate component metadata
        if (definition.component.name.isBlank()) {
            errors.add(ValidationError("component.name", "Component name is required"))
        }

        // Validate layout
        validateLayout(definition.layout, "layout", errors, warnings)

        // Validate data bindings
        definition.data.forEach { (name, binding) ->
            if (binding.type.isBlank()) {
                warnings.add(ValidationWarning("data.$name.type", "Type not specified"))
            }
        }

        // Check for required bindings used in layout
        val usedBindings = collectBindings(definition.layout)
        definition.data.filter { it.value.required }.forEach { (name, _) ->
            if (name !in usedBindings) {
                warnings.add(ValidationWarning("data.$name", "Required binding not used in layout"))
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateLayout(
        layout: LayoutDefinition,
        path: String,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        layout.children.forEachIndexed { index, widget ->
            validateWidget(widget, "$path.children[$index]", errors, warnings)
        }

        layout.template?.let { template ->
            if (template.forEach.isBlank()) {
                errors.add(ValidationError("$path.template.forEach", "forEach expression required"))
            }
            template.render.forEachIndexed { index, widget ->
                validateWidget(widget, "$path.template.render[$index]", errors, warnings)
            }
        }
    }

    private fun validateWidget(
        widget: WidgetDefinition,
        path: String,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        // Check for CUSTOM type (unknown widget)
        if (widget.widget == WidgetType.CUSTOM) {
            warnings.add(ValidationWarning(path, "Unknown widget type"))
        }

        // Validate children recursively
        widget.children.forEachIndexed { index, child ->
            validateWidget(child, "$path.children[$index]", errors, warnings)
        }
    }

    private fun collectBindings(layout: LayoutDefinition): Set<String> {
        val bindings = mutableSetOf<String>()
        val bindingPattern = Regex("""\$\{([^}]+)}""")

        fun collectFromWidget(widget: WidgetDefinition) {
            // Collect from props
            listOfNotNull(
                widget.props.text,
                widget.props.color,
                widget.props.background,
                widget.props.icon,
                widget.props.state,
                widget.condition
            ).forEach { value ->
                bindingPattern.findAll(value).forEach { match ->
                    bindings.add(match.groupValues[1].split('.').first())
                }
            }

            // Recurse to children
            widget.children.forEach { collectFromWidget(it) }
        }

        layout.children.forEach { collectFromWidget(it) }
        layout.template?.let { template ->
            bindings.add(template.forEach.removePrefix("\${").removeSuffix("}").split('.').first())
            template.render.forEach { collectFromWidget(it) }
        }

        return bindings
    }
}

/**
 * Validation result.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError>,
    val warnings: List<ValidationWarning>
) {
    override fun toString(): String {
        return if (isValid && warnings.isEmpty()) {
            "Component validation passed"
        } else {
            buildString {
                if (errors.isNotEmpty()) {
                    appendLine("Errors:")
                    errors.forEach { appendLine("  - ${it.path}: ${it.message}") }
                }
                if (warnings.isNotEmpty()) {
                    appendLine("Warnings:")
                    warnings.forEach { appendLine("  - ${it.path}: ${it.message}") }
                }
            }
        }
    }
}

/**
 * Validation error (blocking).
 */
data class ValidationError(
    val path: String,
    val message: String
)

/**
 * Validation warning (non-blocking).
 */
data class ValidationWarning(
    val path: String,
    val message: String
)
