package com.augmentalis.avanues.avacode.workflows.examples

import com.augmentalis.avanues.avacode.forms.*
import com.augmentalis.avanues.avacode.workflows.*

/**
 * Example: E-commerce checkout workflow.
 *
 * Demonstrates:
 * - Shopping cart review
 * - Shipping information
 * - Payment processing
 * - Order confirmation
 * - Back navigation
 */

// Step 1: Cart Review (no form, just display)
// Step 2: Shipping Form
val shippingForm = form("shipping_info") {
    textField("shipping_name") {
        label("Full Name")
        required()
    }

    emailField("shipping_email") {
        label("Email")
        required()
    }

    textField("shipping_address1") {
        label("Address Line 1")
        required()
    }

    textField("shipping_address2") {
        label("Address Line 2")
    }

    textField("shipping_city") {
        label("City")
        required()
    }

    textField("shipping_state") {
        label("State/Province")
        required()
    }

    textField("shipping_postal") {
        label("Postal Code")
        required()
    }

    selectField("shipping_country", listOf("US", "CA", "UK", "AU")) {
        label("Country")
        required()
    }
}

// Step 3: Payment Form
val checkoutPaymentForm = form("payment") {
    textField("card_name") {
        label("Name on Card")
        required()
    }

    booleanField("same_as_shipping") {
        label("Billing address same as shipping")
        defaultValue(true)
    }
}

// Step 4: Confirmation (no form)

/**
 * E-commerce checkout workflow.
 */
val checkoutWorkflow = workflow("checkout", WorkflowMetadata(
    title = "Checkout",
    description = "Complete your purchase",
    allowBack = true,
    allowSkip = false
)) {
    step("cart_review") {
        title("Review Cart")
        description("Review items in your cart")
        // No form - just display cart items
        onEnter { data ->
            println("Reviewing cart...")
        }
    }

    step("shipping") {
        title("Shipping Information")
        description("Where should we send your order?")
        form(shippingForm)
        onComplete { data ->
            println("Shipping to: ${data["shipping_city"]}, ${data["shipping_state"]}")
        }
    }

    step("payment") {
        title("Payment")
        description("Enter payment details")
        form(checkoutPaymentForm)
        allowBack(false) // Cannot go back from payment for security
        onComplete { data ->
            println("Processing payment for: ${data["card_name"]}")
            // In real app: Process payment, create order
        }
    }

    step("confirmation") {
        title("Order Confirmation")
        description("Your order has been placed!")
        allowBack(false) // Cannot go back from confirmation
        onEnter { data ->
            println("Order confirmed! Check ${data["shipping_email"]} for details.")
        }
    }
}

/**
 * Example: Survey workflow with skip logic.
 */
val surveyWorkflow = workflow("customer_survey", WorkflowMetadata(
    title = "Customer Survey",
    description = "Help us improve",
    allowSkip = true
)) {
    step("satisfaction") {
        title("Overall Satisfaction")
        form(form("satisfaction") {
            numberField("rating") {
                label("How satisfied are you? (1-5)")
                required()
                range(1, 5)
            }
        })
    }

    step("detailed_feedback") {
        title("Detailed Feedback")
        // Only show if rating <= 3
        condition { data ->
            val rating = (data["rating"] as? Number)?.toInt() ?: 5
            rating <= 3
        }
        form(form("feedback") {
            textAreaField("comments") {
                label("What can we improve?")
                required()
                minLength(20)
            }
        })
    }

    step("contact") {
        title("Contact Information")
        allowSkip(true)
        form(form("contact") {
            emailField("email") {
                label("Email (optional for follow-up)")
            }
        })
    }
}

/**
 * Example usage of checkout workflow.
 */
fun checkoutWorkflowExample() {
    println("=== Checkout Workflow ===\n")

    var instance = checkoutWorkflow.createInstance()

    // Step 1: Cart Review
    println("Step ${instance.currentStepIndex + 1}: ${instance.currentStep?.title}")
    when (val result = instance.next()) {
        is WorkflowResult.Success -> instance = result.instance
        else -> println("Error")
    }

    // Step 2: Shipping
    println("\nStep ${instance.currentStepIndex + 1}: ${instance.currentStep?.title}")
    println("Can go back: ${instance.canGoBack}")

    val shippingData = mapOf(
        "shipping_name" to "Jane Smith",
        "shipping_email" to "jane@example.com",
        "shipping_address1" to "456 Oak Ave",
        "shipping_city" to "Portland",
        "shipping_state" to "OR",
        "shipping_postal" to "97201",
        "shipping_country" to "US"
    )

    when (val result = instance.next(shippingData)) {
        is WorkflowResult.Success -> instance = result.instance
        is WorkflowResult.ValidationFailed -> println("Validation failed: ${result.errors}")
        else -> println("Error")
    }

    // Step 3: Payment
    println("\nStep ${instance.currentStepIndex + 1}: ${instance.currentStep?.title}")
    println("Can go back: ${instance.canGoBack}") // Should be false

    val paymentData = mapOf(
        "card_name" to "Jane Smith",
        "same_as_shipping" to true
    )

    when (val result = instance.next(paymentData)) {
        is WorkflowResult.Success -> instance = result.instance
        else -> println("Error")
    }

    // Step 4: Confirmation
    println("\nStep ${instance.currentStepIndex + 1}: ${instance.currentStep?.title}")
    println("Workflow complete: ${instance.isComplete}")

    val progress = instance.getProgress()
    println("\n=== Final Progress ===")
    println("Percentage: ${progress.percentage}%")
    println("Completed: ${progress.completedSteps}/${progress.totalSteps}")
}
