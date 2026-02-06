package com.augmentalis.avacode.forms

/**
 * Validation rules for form fields.
 *
 * Each rule can validate a value and return a validation result.
 * Rules are composable and can be combined in field definitions.
 *
 * @since 1.2.0
 */
sealed class ValidationRule {
    /**
     * Validate a value against this rule.
     */
    abstract fun validate(value: Any?): ValidationRuleResult

    /**
     * Field is required (cannot be null or empty).
     */
    object Required : ValidationRule() {
        override fun validate(value: Any?): ValidationRuleResult {
            return when {
                value == null -> ValidationRuleResult.Invalid("Field is required")
                value is String && value.isBlank() -> ValidationRuleResult.Invalid("Field cannot be empty")
                else -> ValidationRuleResult.Valid
            }
        }
    }

    /**
     * String minimum length validation.
     */
    data class MinLength(val length: Int) : ValidationRule() {
        override fun validate(value: Any?): ValidationRuleResult {
            if (value == null) return ValidationRuleResult.Valid // Optional field
            if (value !is String) return ValidationRuleResult.Invalid("Value must be a string")
            return if (value.length >= length) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid("Must be at least $length characters")
            }
        }
    }

    /**
     * String maximum length validation.
     */
    data class MaxLength(val length: Int) : ValidationRule() {
        override fun validate(value: Any?): ValidationRuleResult {
            if (value == null) return ValidationRuleResult.Valid
            if (value !is String) return ValidationRuleResult.Invalid("Value must be a string")
            return if (value.length <= length) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid("Must be at most $length characters")
            }
        }
    }

    /**
     * Regular expression pattern validation.
     */
    data class Pattern(val regex: Regex, val errorMessage: String? = null) : ValidationRule() {
        override fun validate(value: Any?): ValidationRuleResult {
            if (value == null) return ValidationRuleResult.Valid
            if (value !is String) return ValidationRuleResult.Invalid("Value must be a string")
            return if (regex.matches(value)) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid(errorMessage ?: "Invalid format")
            }
        }
    }

    /**
     * Email format validation.
     */
    object Email : ValidationRule() {
        private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        override fun validate(value: Any?): ValidationRuleResult {
            if (value == null) return ValidationRuleResult.Valid
            if (value !is String) return ValidationRuleResult.Invalid("Email must be a string")
            return if (emailRegex.matches(value)) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid("Invalid email format")
            }
        }
    }

    /**
     * Number minimum value validation.
     */
    data class Min(val min: Number) : ValidationRule() {
        override fun validate(value: Any?): ValidationRuleResult {
            if (value == null) return ValidationRuleResult.Valid
            val numValue = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull() ?: return ValidationRuleResult.Invalid("Invalid number")
                else -> return ValidationRuleResult.Invalid("Value must be a number")
            }
            return if (numValue >= min.toDouble()) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid("Must be at least $min")
            }
        }
    }

    /**
     * Number maximum value validation.
     */
    data class Max(val max: Number) : ValidationRule() {
        override fun validate(value: Any?): ValidationRuleResult {
            if (value == null) return ValidationRuleResult.Valid
            val numValue = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull() ?: return ValidationRuleResult.Invalid("Invalid number")
                else -> return ValidationRuleResult.Invalid("Value must be a number")
            }
            return if (numValue <= max.toDouble()) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid("Must be at most $max")
            }
        }
    }

    /**
     * Number range validation (min and max).
     */
    data class Range(val min: Number, val max: Number) : ValidationRule() {
        override fun validate(value: Any?): ValidationRuleResult {
            if (value == null) return ValidationRuleResult.Valid
            val numValue = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull() ?: return ValidationRuleResult.Invalid("Invalid number")
                else -> return ValidationRuleResult.Invalid("Value must be a number")
            }
            return if (numValue >= min.toDouble() && numValue <= max.toDouble()) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid("Must be between $min and $max")
            }
        }
    }

    /**
     * Value must be in a list of allowed values.
     */
    data class InList(val allowedValues: List<String>) : ValidationRule() {
        override fun validate(value: Any?): ValidationRuleResult {
            if (value == null) return ValidationRuleResult.Valid
            val strValue = value.toString()
            return if (strValue in allowedValues) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid("Must be one of: ${allowedValues.joinToString()}")
            }
        }
    }

    /**
     * Date minimum validation (ISO 8601 format).
     */
    data class MinDate(val minDate: String) : ValidationRule() {
        override fun validate(value: Any?): ValidationRuleResult {
            if (value == null) return ValidationRuleResult.Valid
            if (value !is String) return ValidationRuleResult.Invalid("Date must be a string")
            return if (value >= minDate) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid("Date must be on or after $minDate")
            }
        }
    }

    /**
     * Date maximum validation (ISO 8601 format).
     */
    data class MaxDate(val maxDate: String) : ValidationRule() {
        override fun validate(value: Any?): ValidationRuleResult {
            if (value == null) return ValidationRuleResult.Valid
            if (value !is String) return ValidationRuleResult.Invalid("Date must be a string")
            return if (value <= maxDate) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid("Date must be on or before $maxDate")
            }
        }
    }

    /**
     * Password requires uppercase letter.
     */
    object RequireUppercase : ValidationRule() {
        override fun validate(value: Any?): ValidationRuleResult {
            if (value == null) return ValidationRuleResult.Valid
            if (value !is String) return ValidationRuleResult.Invalid("Password must be a string")
            return if (value.any { it.isUpperCase() }) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid("Must contain at least one uppercase letter")
            }
        }
    }

    /**
     * Password requires lowercase letter.
     */
    object RequireLowercase : ValidationRule() {
        override fun validate(value: Any?): ValidationRuleResult {
            if (value == null) return ValidationRuleResult.Valid
            if (value !is String) return ValidationRuleResult.Invalid("Password must be a string")
            return if (value.any { it.isLowerCase() }) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid("Must contain at least one lowercase letter")
            }
        }
    }

    /**
     * Password requires number.
     */
    object RequireNumber : ValidationRule() {
        override fun validate(value: Any?): ValidationRuleResult {
            if (value == null) return ValidationRuleResult.Valid
            if (value !is String) return ValidationRuleResult.Invalid("Password must be a string")
            return if (value.any { it.isDigit() }) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid("Must contain at least one number")
            }
        }
    }

    /**
     * Password requires special character.
     */
    object RequireSpecialChar : ValidationRule() {
        private val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"

        override fun validate(value: Any?): ValidationRuleResult {
            if (value == null) return ValidationRuleResult.Valid
            if (value !is String) return ValidationRuleResult.Invalid("Password must be a string")
            return if (value.any { it in specialChars }) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid("Must contain at least one special character")
            }
        }
    }

    /**
     * Custom validation with lambda.
     */
    data class Custom(
        val validator: (Any?) -> Boolean,
        val errorMessage: String
    ) : ValidationRule() {
        override fun validate(value: Any?): ValidationRuleResult {
            return if (validator(value)) {
                ValidationRuleResult.Valid
            } else {
                ValidationRuleResult.Invalid(errorMessage)
            }
        }
    }
}

/**
 * Result of validating a value against a rule.
 */
sealed class ValidationRuleResult {
    object Valid : ValidationRuleResult()
    data class Invalid(val errorMessage: String) : ValidationRuleResult()

    val isValid: Boolean get() = this is Valid
    val isInvalid: Boolean get() = this is Invalid
}
