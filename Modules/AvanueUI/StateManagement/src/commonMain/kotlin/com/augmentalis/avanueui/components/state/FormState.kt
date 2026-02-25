package com.augmentalis.avanueui.state

import kotlinx.coroutines.flow.*

/**
 * Specialized state management for forms with validation.
 *
 * FormState provides a structured way to manage form fields, validation,
 * error messages, and submission state.
 *
 * Usage:
 * ```kotlin
 * val form = FormState {
 *     field("email", "") {
 *         validator(EmailValidator())
 *         required(true)
 *     }
 *     field("password", "") {
 *         validator(MinLengthValidator(8))
 *         required(true)
 *     }
 * }
 * ```
 */
class FormState {
    private val fields = mutableMapOf<String, FieldState<*>>()
    private val _isValid = MutableStateFlow(true)
    val isValid: StateFlow<Boolean> = _isValid.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    /**
     * Create a form field with initial value
     */
    fun <T> field(
        name: String,
        initialValue: T,
        validators: List<Validator<T>> = emptyList(),
        required: Boolean = false
    ): FieldState<T> {
        val field = FieldState(initialValue, validators, required)
        fields[name] = field
        return field
    }

    /**
     * Get a field by name
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getField(name: String): FieldState<T>? {
        return fields[name] as? FieldState<T>
    }

    /**
     * Validate all fields
     */
    fun validate(): Boolean {
        var allValid = true
        fields.values.forEach { field ->
            if (!field.validate()) {
                allValid = false
            }
        }
        _isValid.value = allValid
        return allValid
    }

    /**
     * Validate a specific field
     */
    fun validateField(name: String): Boolean {
        return fields[name]?.validate() ?: true
    }

    /**
     * Reset all fields to their initial values
     */
    fun reset() {
        fields.values.forEach { it.reset() }
        _isValid.value = true
    }

    /**
     * Clear all error messages
     */
    fun clearErrors() {
        fields.values.forEach { it.clearError() }
    }

    /**
     * Get all field values as a map
     */
    fun getValues(): Map<String, Any?> {
        return fields.mapValues { (_, field) -> field.getValue() }
    }

    /**
     * Set submitting state
     */
    fun setSubmitting(submitting: Boolean) {
        _isSubmitting.value = submitting
    }

    /**
     * Submit the form with validation
     */
    suspend fun submit(onSubmit: suspend (Map<String, Any?>) -> Unit): Boolean {
        if (!validate()) {
            return false
        }

        return try {
            _isSubmitting.value = true
            onSubmit(getValues())
            true
        } catch (e: Exception) {
            false
        } finally {
            _isSubmitting.value = false
        }
    }
}

/**
 * State for a single form field
 */
class FieldState<T>(
    initialValue: T,
    val validators: List<Validator<T>> = emptyList(),
    val required: Boolean = false
) {
    private val _value = MutableStateFlow(initialValue)
    val value: StateFlow<T> = _value.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    private val _isTouched = MutableStateFlow(false)
    val isTouched: StateFlow<Boolean> = _isTouched.asStateFlow()

    private val initialValueStore = initialValue

    /**
     * Update field value
     */
    fun setValue(newValue: T) {
        _value.value = newValue
        _isDirty.value = newValue != initialValueStore
    }

    /**
     * Mark field as touched (focused and blurred)
     */
    fun setTouched(touched: Boolean = true) {
        _isTouched.value = touched
    }

    /**
     * Get current value
     */
    fun getValue(): T = _value.value

    /**
     * Validate the field
     */
    fun validate(): Boolean {
        // Check required
        if (required && isEmpty(_value.value)) {
            _error.value = "This field is required"
            return false
        }

        // Run validators
        validators.forEach { validator ->
            val result = validator.validate(_value.value)
            if (!result.isValid) {
                _error.value = result.message
                return false
            }
        }

        _error.value = null
        return true
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Reset to initial value
     */
    fun reset() {
        _value.value = initialValueStore
        _error.value = null
        _isDirty.value = false
        _isTouched.value = false
    }

    /**
     * Check if value is empty
     */
    private fun isEmpty(value: T): Boolean {
        return when (value) {
            is String -> value.isEmpty()
            is Collection<*> -> value.isEmpty()
            is Map<*, *> -> value.isEmpty()
            null -> true
            else -> false
        }
    }
}

/**
 * Validator interface for field validation
 */
interface Validator<T> {
    fun validate(value: T): ValidationResult
}

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val message: String? = null
) {
    companion object {
        fun valid() = ValidationResult(true)
        fun invalid(message: String) = ValidationResult(false, message)
    }
}

// ==================== Built-in Validators ====================

/**
 * Email validator
 */
class EmailValidator(
    private val message: String = "Invalid email address"
) : Validator<String> {
    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    override fun validate(value: String): ValidationResult {
        return if (value.isEmpty() || emailRegex.matches(value)) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(message)
        }
    }
}

/**
 * Minimum length validator
 */
class MinLengthValidator(
    private val minLength: Int,
    private val message: String? = null
) : Validator<String> {
    override fun validate(value: String): ValidationResult {
        return if (value.length >= minLength) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(
                message ?: "Minimum length is $minLength characters"
            )
        }
    }
}

/**
 * Maximum length validator
 */
class MaxLengthValidator(
    private val maxLength: Int,
    private val message: String? = null
) : Validator<String> {
    override fun validate(value: String): ValidationResult {
        return if (value.length <= maxLength) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(
                message ?: "Maximum length is $maxLength characters"
            )
        }
    }
}

/**
 * Pattern validator (regex)
 */
class PatternValidator(
    private val pattern: Regex,
    private val message: String = "Invalid format"
) : Validator<String> {
    override fun validate(value: String): ValidationResult {
        return if (value.isEmpty() || pattern.matches(value)) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(message)
        }
    }
}

/**
 * Range validator for numbers
 */
class RangeValidator<T : Comparable<T>>(
    private val min: T? = null,
    private val max: T? = null,
    private val message: String? = null
) : Validator<T> {
    override fun validate(value: T): ValidationResult {
        val isValid = (min == null || value >= min) && (max == null || value <= max)
        return if (isValid) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(
                message ?: "Value must be between ${min ?: "any"} and ${max ?: "any"}"
            )
        }
    }
}

/**
 * Custom validator
 */
class CustomValidator<T>(
    private val message: String,
    private val validationFn: (T) -> Boolean
) : Validator<T> {
    override fun validate(value: T): ValidationResult {
        return if (validationFn(value)) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(message)
        }
    }
}

/**
 * Match validator (e.g., password confirmation)
 */
class MatchValidator<T>(
    private val otherField: FieldState<T>,
    private val message: String = "Fields do not match"
) : Validator<T> {
    override fun validate(value: T): ValidationResult {
        return if (value == otherField.getValue()) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(message)
        }
    }
}

// ==================== DSL Builders ====================

/**
 * Form builder DSL
 */
class FormBuilder {
    private val form = FormState()

    fun <T> field(
        name: String,
        initialValue: T,
        builder: FieldBuilder<T>.() -> Unit = {}
    ): FieldState<T> {
        val fieldBuilder = FieldBuilder<T>()
        fieldBuilder.builder()
        return form.field(name, initialValue, fieldBuilder.validators, fieldBuilder.required)
    }

    fun build(): FormState = form
}

/**
 * Field builder DSL
 */
class FieldBuilder<T> {
    val validators = mutableListOf<Validator<T>>()
    var required: Boolean = false

    fun validator(validator: Validator<T>) {
        validators.add(validator)
    }

    fun required(isRequired: Boolean = true) {
        required = isRequired
    }
}

/**
 * Create a form using DSL
 */
fun buildForm(builder: FormBuilder.() -> Unit): FormState {
    return FormBuilder().apply(builder).build()
}

/**
 * Convenience function for email validation
 */
fun FieldBuilder<String>.email(message: String = "Invalid email address") {
    validator(EmailValidator(message))
}

/**
 * Convenience function for minimum length
 */
fun FieldBuilder<String>.minLength(length: Int, message: String? = null) {
    validator(MinLengthValidator(length, message))
}

/**
 * Convenience function for maximum length
 */
fun FieldBuilder<String>.maxLength(length: Int, message: String? = null) {
    validator(MaxLengthValidator(length, message))
}

/**
 * Convenience function for pattern matching
 */
fun FieldBuilder<String>.pattern(regex: Regex, message: String = "Invalid format") {
    validator(PatternValidator(regex, message))
}

/**
 * Convenience function for range validation
 */
fun <T : Comparable<T>> FieldBuilder<T>.range(min: T? = null, max: T? = null, message: String? = null) {
    validator(RangeValidator(min, max, message))
}

/**
 * Convenience function for custom validation
 */
fun <T> FieldBuilder<T>.custom(message: String, validationFn: (T) -> Boolean) {
    validator(CustomValidator(message, validationFn))
}

/**
 * Convenience function for match validation
 */
fun <T> FieldBuilder<T>.matches(otherField: FieldState<T>, message: String = "Fields do not match") {
    validator(MatchValidator(otherField, message))
}
