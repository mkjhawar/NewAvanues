package com.augmentalis.avanues.avacode.forms.examples

import com.augmentalis.avanues.avacode.forms.*

/**
 * Example: User registration form with validation and database schema.
 *
 * Demonstrates:
 * - Required fields
 * - Email validation
 * - Password complexity rules
 * - Unique constraints
 * - Custom validation
 * - Database schema generation
 */
val userRegistrationForm = form("user_registration") {
    textField("username") {
        label("Username")
        placeholder("Enter username")
        required()
        minLength(3)
        maxLength(20)
        pattern("[a-zA-Z0-9_]+")
        unique()
        indexed()
    }

    emailField("email") {
        label("Email Address")
        placeholder("user@example.com")
        required()
        unique()
        indexed()
    }

    passwordField("password") {
        label("Password")
        placeholder("Enter secure password")
        required()
        minLength(8)
        requireUppercase()
        requireLowercase()
        requireNumber()
        requireSpecialChar()
    }

    textField("full_name") {
        label("Full Name")
        placeholder("John Doe")
        required()
        minLength(2)
        maxLength(100)
    }

    dateField("date_of_birth") {
        label("Date of Birth")
        required()
        maxDate("2010-01-01") // Must be at least 15 years old
    }

    selectField("country", listOf("US", "CA", "UK", "AU", "DE", "FR")) {
        label("Country")
        required()
    }

    booleanField("terms_accepted") {
        label("I accept the terms and conditions")
        required()
    }

    booleanField("newsletter_opt_in") {
        label("Send me newsletter updates")
        defaultValue(false)
    }
}

/**
 * Example usage of the user registration form.
 */
fun userRegistrationExample() {
    // Generate database schema
    val schema = userRegistrationForm.toSchema()
    val createTableSQL = schema.toSQL()
    println("=== Database Schema ===")
    println(createTableSQL)
    println()

    // Create form binding
    val binding = userRegistrationForm.bind()

    // Register change listener
    binding.onChange { fieldId, value ->
        println("Field '$fieldId' changed to: $value")
    }

    // Fill form data
    try {
        binding["username"] = "johndoe"
        binding["email"] = "john@example.com"
        binding["password"] = "SecureP@ss123"
        binding["full_name"] = "John Doe"
        binding["date_of_birth"] = "1990-05-15"
        binding["country"] = "US"
        binding["terms_accepted"] = true
        binding["newsletter_opt_in"] = false
    } catch (e: ValidationException) {
        println("Validation error: ${e.message}")
    }

    // Check completion status
    val completion = binding.getCompletion()
    println("\n=== Completion Status ===")
    println("Required complete: ${completion.requiredComplete}")
    println("Overall progress: ${completion.overallPercentage}%")
    println("Completed fields: ${completion.completedFields}/${completion.totalFields}")

    // Validate all fields
    val validationResult = binding.validate()
    println("\n=== Validation Result ===")
    when (validationResult) {
        is ValidationResult.Success -> println("All fields valid!")
        is ValidationResult.Failure -> {
            println("Validation errors:")
            validationResult.errors.forEach { (field, errors) ->
                println("  $field: ${errors.joinToString()}")
            }
        }
    }

    // Check for changes
    println("\n=== Change Tracking ===")
    println("Has changes: ${binding.hasChanges()}")
    binding.getChanges().forEach { (field, change) ->
        println("  $field: ${change.oldValue} -> ${change.newValue}")
    }
}
