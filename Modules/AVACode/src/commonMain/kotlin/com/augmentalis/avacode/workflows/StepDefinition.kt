package com.augmentalis.avacode.workflows

import com.augmentalis.avacode.forms.FormDefinition
import com.augmentalis.avacode.forms.ValidationResult

/**
 * Step definition within a workflow.
 *
 * Each step can contain a form, validation logic, conditional display,
 * and lifecycle callbacks.
 *
 * @property id Step identifier
 * @property title Step title for display
 * @property form Optional form for this step
 * @property condition Condition to show this step (null = always show)
 * @property skipCondition Condition to skip this step (null = never skip)
 * @property validation Custom validation beyond form validation
 * @property onEnter Callback when step is entered
 * @property onComplete Callback when step is completed
 * @property onSkip Callback when step is skipped
 * @since 1.3.0
 */
data class StepDefinition(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val form: FormDefinition? = null,
    val condition: ((Map<String, Any?>) -> Boolean)? = null,
    val skipCondition: ((Map<String, Any?>) -> Boolean)? = null,
    val validation: ((Map<String, Any?>) -> ValidationResult)? = null,
    val onEnter: ((Map<String, Any?>) -> Unit)? = null,
    val onComplete: ((Map<String, Any?>) -> Unit)? = null,
    val onSkip: ((Map<String, Any?>) -> Unit)? = null,
    val allowBack: Boolean = true,
    val allowSkip: Boolean = false
) {
    init {
        require(id.isNotBlank()) { "Step ID cannot be blank" }
        require(id.matches(Regex("[a-z][a-z0-9_]*"))) {
            "Step ID must be lowercase alphanumeric with underscores"
        }
    }

    /**
     * Check if this step should be shown based on workflow data.
     */
    fun shouldShow(data: Map<String, Any?>): Boolean {
        return condition?.invoke(data) ?: true
    }

    /**
     * Check if this step should be skipped based on workflow data.
     */
    fun shouldSkip(data: Map<String, Any?>): Boolean {
        return skipCondition?.invoke(data) ?: false
    }

    /**
     * Validate step data (form validation + custom validation).
     */
    fun validate(data: Map<String, Any?>): ValidationResult {
        // First validate form if present
        val formResult = form?.validate(data)
        if (formResult is ValidationResult.Failure) {
            return formResult
        }

        // Then run custom validation
        return validation?.invoke(data) ?: ValidationResult.Success
    }
}

/**
 * Step builder DSL implementation.
 */
class StepBuilder(private val id: String) {
    private var title: String? = null
    private var description: String? = null
    private var form: FormDefinition? = null
    private var condition: ((Map<String, Any?>) -> Boolean)? = null
    private var skipCondition: ((Map<String, Any?>) -> Boolean)? = null
    private var validation: ((Map<String, Any?>) -> ValidationResult)? = null
    private var onEnter: ((Map<String, Any?>) -> Unit)? = null
    private var onComplete: ((Map<String, Any?>) -> Unit)? = null
    private var onSkip: ((Map<String, Any?>) -> Unit)? = null
    private var allowBack: Boolean = true
    private var allowSkip: Boolean = false

    fun title(value: String) {
        title = value
    }

    fun description(value: String) {
        description = value
    }

    fun form(value: FormDefinition) {
        form = value
    }

    fun condition(predicate: (Map<String, Any?>) -> Boolean) {
        condition = predicate
    }

    fun skipIf(predicate: (Map<String, Any?>) -> Boolean) {
        skipCondition = predicate
    }

    fun validate(validator: (Map<String, Any?>) -> ValidationResult) {
        validation = validator
    }

    fun onEnter(callback: (Map<String, Any?>) -> Unit) {
        onEnter = callback
    }

    fun onComplete(callback: (Map<String, Any?>) -> Unit) {
        onComplete = callback
    }

    fun onSkip(callback: (Map<String, Any?>) -> Unit) {
        onSkip = callback
    }

    fun allowBack(value: Boolean) {
        allowBack = value
    }

    fun allowSkip(value: Boolean) {
        allowSkip = value
    }

    internal fun build(): StepDefinition = StepDefinition(
        id = id,
        title = title,
        description = description,
        form = form,
        condition = condition,
        skipCondition = skipCondition,
        validation = validation,
        onEnter = onEnter,
        onComplete = onComplete,
        onSkip = onSkip,
        allowBack = allowBack,
        allowSkip = allowSkip
    )
}
