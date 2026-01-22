package com.augmentalis.avanues.avacode.workflows.examples

import com.augmentalis.avanues.avacode.forms.*
import com.augmentalis.avanues.avacode.workflows.*

/**
 * Example: User onboarding workflow with conditional steps.
 *
 * Demonstrates:
 * - Multi-step registration process
 * - Conditional steps based on account type
 * - Skip logic for free accounts
 * - Form integration
 * - Progress tracking
 */

// Step 1: Account Creation Form
val accountCreationForm = form("account_creation") {
    textField("username") {
        label("Username")
        required()
        minLength(3)
        maxLength(20)
        pattern("[a-zA-Z0-9_]+")
        unique()
    }

    emailField("email") {
        label("Email Address")
        required()
        unique()
    }

    passwordField("password") {
        label("Password")
        required()
        minLength(8)
        requireUppercase()
        requireNumber()
    }

    selectField("account_type", listOf("free", "premium", "enterprise")) {
        label("Account Type")
        required()
    }
}

// Step 2: Profile Setup Form
val profileSetupForm = form("profile_setup") {
    textField("full_name") {
        label("Full Name")
        required()
    }

    textField("company") {
        label("Company Name")
    }

    textField("phone") {
        label("Phone Number")
        pattern("\\+?[0-9\\s\\-\\(\\)]+")
    }

    textAreaField("bio") {
        label("Bio")
        maxLength(500)
    }
}

// Step 3: Payment Form (premium/enterprise only)
val paymentForm = form("payment_info") {
    textField("card_name") {
        label("Name on Card")
        required()
    }

    textField("billing_address") {
        label("Billing Address")
        required()
    }

    textField("billing_city") {
        label("City")
        required()
    }

    textField("billing_postal") {
        label("Postal Code")
        required()
    }
}

// Step 4: Preferences Form
val preferencesForm = form("preferences") {
    booleanField("newsletter") {
        label("Subscribe to newsletter")
        defaultValue(false)
    }

    booleanField("notifications") {
        label("Enable email notifications")
        defaultValue(true)
    }

    selectField("theme", listOf("light", "dark", "auto")) {
        label("Theme Preference")
        defaultValue("auto")
    }
}

/**
 * Complete user onboarding workflow.
 */
val userOnboardingWorkflow = workflow("user_onboarding", WorkflowMetadata(
    title = "User Onboarding",
    description = "Complete registration and setup process",
    allowBack = true,
    allowSkip = false,
    autoSave = true
)) {
    // Step 1: Account Creation (always shown)
    step("account_creation") {
        title("Create Account")
        description("Set up your account credentials")
        form(accountCreationForm)
        onComplete { data ->
            println("Account created: ${data["username"]}")
            // In real app: Send verification email, create user in database
        }
    }

    // Step 2: Profile Setup (always shown)
    step("profile_setup") {
        title("Profile Setup")
        description("Tell us about yourself")
        form(profileSetupForm)
        onComplete { data ->
            println("Profile created for: ${data["full_name"]}")
        }
    }

    // Step 3: Payment Info (conditional - only for premium/enterprise)
    step("payment_info") {
        title("Payment Information")
        description("Add payment method for your premium account")
        form(paymentForm)

        // Only show for paid accounts
        condition { data ->
            val accountType = data["account_type"] as? String
            accountType == "premium" || accountType == "enterprise"
        }

        onComplete { data ->
            println("Payment info added for card: ${data["card_name"]}")
            // In real app: Process payment, create subscription
        }
    }

    // Step 4: Preferences (always shown)
    step("preferences") {
        title("Preferences")
        description("Customize your experience")
        form(preferencesForm)
        allowSkip(true) // This step can be skipped
        onComplete { data ->
            println("Preferences saved: theme = ${data["theme"]}")
        }
        onSkip { data ->
            println("Using default preferences")
        }
    }
}

/**
 * Example usage of the onboarding workflow.
 */
fun onboardingWorkflowExample() {
    println("=== User Onboarding Workflow ===\n")

    // Create workflow instance
    var instance = userOnboardingWorkflow.createInstance()

    println("Starting workflow: ${userOnboardingWorkflow.metadata.title}")
    println("Total steps: ${userOnboardingWorkflow.steps.size}\n")

    // Step 1: Account Creation
    println("Current step: ${instance.currentStep?.title}")
    val accountData = mapOf(
        "username" to "johndoe",
        "email" to "john@example.com",
        "password" to "SecurePass123!",
        "account_type" to "premium"
    )

    when (val result = instance.next(accountData)) {
        is WorkflowResult.Success -> instance = result.instance
        is WorkflowResult.ValidationFailed -> println("Validation failed: ${result.errors}")
        is WorkflowResult.Error -> println("Error: ${result.message}")
    }

    var progress = instance.getProgress()
    println("Progress: ${progress.percentage}% (${progress.completedSteps}/${progress.totalSteps})\n")

    // Step 2: Profile Setup
    println("Current step: ${instance.currentStep?.title}")
    val profileData = mapOf(
        "full_name" to "John Doe",
        "company" to "Acme Corp",
        "phone" to "+1-555-0123"
    )

    when (val result = instance.next(profileData)) {
        is WorkflowResult.Success -> instance = result.instance
        is WorkflowResult.ValidationFailed -> println("Validation failed: ${result.errors}")
        is WorkflowResult.Error -> println("Error: ${result.message}")
    }

    progress = instance.getProgress()
    println("Progress: ${progress.percentage}% (${progress.completedSteps}/${progress.totalSteps})\n")

    // Step 3: Payment Info (shown because account_type == "premium")
    println("Current step: ${instance.currentStep?.title}")
    val paymentData = mapOf(
        "card_name" to "John Doe",
        "billing_address" to "123 Main St",
        "billing_city" to "San Francisco",
        "billing_postal" to "94102"
    )

    when (val result = instance.next(paymentData)) {
        is WorkflowResult.Success -> instance = result.instance
        is WorkflowResult.ValidationFailed -> println("Validation failed: ${result.errors}")
        is WorkflowResult.Error -> println("Error: ${result.message}")
    }

    progress = instance.getProgress()
    println("Progress: ${progress.percentage}% (${progress.completedSteps}/${progress.totalSteps})\n")

    // Step 4: Preferences (can be skipped)
    println("Current step: ${instance.currentStep?.title}")
    println("Can skip: ${instance.canSkip}")

    val prefsData = mapOf(
        "newsletter" to true,
        "notifications" to true,
        "theme" to "dark"
    )

    when (val result = instance.next(prefsData)) {
        is WorkflowResult.Success -> instance = result.instance
        is WorkflowResult.ValidationFailed -> println("Validation failed: ${result.errors}")
        is WorkflowResult.Error -> println("Error: ${result.message}")
    }

    progress = instance.getProgress()
    println("Progress: ${progress.percentage}% (${progress.completedSteps}/${progress.totalSteps})")
    println("Workflow complete: ${instance.isComplete}\n")

    // Print final data
    println("=== Final Data ===")
    instance.data.forEach { (key, value) ->
        println("$key: $value")
    }

    // Test persistence
    println("\n=== Persistence Test ===")
    val serialized = WorkflowPersistence.serialize(instance)
    println("Serialized keys: ${serialized.keys.joinToString()}")

    val deserialized = WorkflowPersistence.deserialize(serialized, userOnboardingWorkflow)
    println("Deserialized successfully: ${deserialized != null}")
    println("Data preserved: ${deserialized?.data == instance.data}")
}
