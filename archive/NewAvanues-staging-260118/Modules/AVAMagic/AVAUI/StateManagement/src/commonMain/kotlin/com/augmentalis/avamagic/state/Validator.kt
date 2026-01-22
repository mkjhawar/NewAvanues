package com.augmentalis.avamagic.state

/**
 * Base interface for value validation
 */
interface Validator<T> {
    /**
     * Validate a value and return the result
     */
    fun validate(value: T): ValidationResult
}

/**
 * Result of a validation operation
 */
sealed class ValidationResult {
    /**
     * Validation passed
     */
    object Valid : ValidationResult()

    /**
     * Validation failed with one or more errors
     */
    data class Invalid(val errors: List<String>) : ValidationResult() {
        constructor(error: String) : this(listOf(error))

        /**
         * Get the first error message
         */
        val firstError: String? get() = errors.firstOrNull()
    }

    /**
     * Check if validation passed
     */
    val isValid: Boolean get() = this is Valid

    /**
     * Check if validation failed
     */
    val isInvalid: Boolean get() = this is Invalid
}

/**
 * A single validation rule with a predicate and error message
 */
class ValidationRule<T>(
    val message: String,
    val predicate: (T) -> Boolean
) : Validator<T> {
    override fun validate(value: T): ValidationResult {
        return if (predicate(value)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(message)
        }
    }
}

/**
 * Composite validator that runs multiple validators
 */
class CompositeValidator<T>(
    private val validators: List<Validator<T>>
) : Validator<T> {
    override fun validate(value: T): ValidationResult {
        val errors = mutableListOf<String>()

        validators.forEach { validator ->
            val result = validator.validate(value)
            if (result is ValidationResult.Invalid) {
                errors.addAll(result.errors)
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}

/**
 * Built-in validators for common use cases
 */
object Validators {

    /**
     * Validates that a string is not empty
     */
    fun required(message: String = "This field is required"): Validator<String> {
        return ValidationRule(message) { it.isNotBlank() }
    }

    /**
     * Validates that a value is not null
     */
    fun <T> notNull(message: String = "This field cannot be null"): Validator<T?> {
        return ValidationRule(message) { it != null }
    }

    /**
     * Validates email format
     */
    fun email(message: String = "Invalid email address"): Validator<String> {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return ValidationRule(message) { it.matches(emailRegex) }
    }

    /**
     * Validates minimum length
     */
    fun minLength(length: Int, message: String = "Minimum length is $length characters"): Validator<String> {
        return ValidationRule(message) { it.length >= length }
    }

    /**
     * Validates maximum length
     */
    fun maxLength(length: Int, message: String = "Maximum length is $length characters"): Validator<String> {
        return ValidationRule(message) { it.length <= length }
    }

    /**
     * Validates exact length
     */
    fun exactLength(length: Int, message: String = "Length must be exactly $length characters"): Validator<String> {
        return ValidationRule(message) { it.length == length }
    }

    /**
     * Validates against a regex pattern
     */
    fun pattern(regex: Regex, message: String = "Invalid format"): Validator<String> {
        return ValidationRule(message) { it.matches(regex) }
    }

    /**
     * Validates numeric range
     */
    fun range(min: Int, max: Int, message: String = "Value must be between $min and $max"): Validator<Int> {
        return ValidationRule(message) { it in min..max }
    }

    /**
     * Validates numeric range (Double)
     */
    fun rangeDouble(min: Double, max: Double, message: String = "Value must be between $min and $max"): Validator<Double> {
        return ValidationRule(message) { it in min..max }
    }

    /**
     * Validates minimum value
     */
    fun min(minValue: Int, message: String = "Minimum value is $minValue"): Validator<Int> {
        return ValidationRule(message) { it >= minValue }
    }

    /**
     * Validates maximum value
     */
    fun max(maxValue: Int, message: String = "Maximum value is $maxValue"): Validator<Int> {
        return ValidationRule(message) { it <= maxValue }
    }

    /**
     * Validates that string contains only letters
     */
    fun alphabetic(message: String = "Only letters are allowed"): Validator<String> {
        return ValidationRule(message) { it.all { char -> char.isLetter() } }
    }

    /**
     * Validates that string contains only numbers
     */
    fun numeric(message: String = "Only numbers are allowed"): Validator<String> {
        return ValidationRule(message) { it.all { char -> char.isDigit() } }
    }

    /**
     * Validates that string contains only alphanumeric characters
     */
    fun alphanumeric(message: String = "Only letters and numbers are allowed"): Validator<String> {
        return ValidationRule(message) { it.all { char -> char.isLetterOrDigit() } }
    }

    /**
     * Validates URL format
     */
    fun url(message: String = "Invalid URL"): Validator<String> {
        val urlRegex = "^https?://[A-Za-z0-9.-]+(:[0-9]+)?(/.*)?$".toRegex()
        return ValidationRule(message) { it.matches(urlRegex) }
    }

    /**
     * Validates phone number (basic format)
     */
    fun phone(message: String = "Invalid phone number"): Validator<String> {
        val phoneRegex = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$".toRegex()
        return ValidationRule(message) { it.matches(phoneRegex) }
    }

    /**
     * Validates password strength
     * At least 8 characters, 1 uppercase, 1 lowercase, 1 number
     */
    fun strongPassword(message: String = "Password must be at least 8 characters with uppercase, lowercase, and numbers"): Validator<String> {
        return ValidationRule(message) {
            it.length >= 8 &&
                    it.any { char -> char.isUpperCase() } &&
                    it.any { char -> char.isLowerCase() } &&
                    it.any { char -> char.isDigit() }
        }
    }

    /**
     * Validates that two values match
     */
    fun <T> matches(other: T, message: String = "Values do not match"): Validator<T> {
        return ValidationRule(message) { it == other }
    }

    /**
     * Validates that value is in a set
     */
    fun <T> oneOf(values: Set<T>, message: String = "Invalid value"): Validator<T> {
        return ValidationRule(message) { it in values }
    }

    /**
     * Custom validator with a predicate
     */
    fun <T> custom(message: String, predicate: (T) -> Boolean): Validator<T> {
        return ValidationRule(message, predicate)
    }

    /**
     * Combine multiple validators
     */
    fun <T> combine(vararg validators: Validator<T>): Validator<T> {
        return CompositeValidator(validators.toList())
    }
}

/**
 * Extension functions for validators
 */

/**
 * Chain validators together
 */
infix fun <T> Validator<T>.and(other: Validator<T>): Validator<T> {
    return CompositeValidator(listOf(this, other))
}

/**
 * Create a validator that runs only if a condition is met
 */
fun <T> Validator<T>.onlyIf(condition: (T) -> Boolean): Validator<T> {
    return object : Validator<T> {
        override fun validate(value: T): ValidationResult {
            return if (condition(value)) {
                this@onlyIf.validate(value)
            } else {
                ValidationResult.Valid
            }
        }
    }
}

/**
 * Transform error message
 */
fun <T> Validator<T>.withMessage(message: String): Validator<T> {
    return object : Validator<T> {
        override fun validate(value: T): ValidationResult {
            val result = this@withMessage.validate(value)
            return if (result is ValidationResult.Invalid) {
                ValidationResult.Invalid(message)
            } else {
                result
            }
        }
    }
}
