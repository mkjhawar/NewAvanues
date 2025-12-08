package com.augmentalis.avanues.avacode.forms.examples

import com.augmentalis.avanues.avacode.forms.*

/**
 * Example: Simple contact form.
 *
 * Demonstrates:
 * - Basic field types
 * - Simple validation
 * - Text area fields
 * - Optional fields
 */
val contactForm = form("contact_form", FormMetadata(
    title = "Contact Us",
    description = "Send us a message",
    allowDraft = true,
    autoSave = true
)) {
    textField("name") {
        label("Your Name")
        required()
        minLength(2)
        maxLength(50)
    }

    emailField("email") {
        label("Email Address")
        required()
    }

    textField("phone") {
        label("Phone Number")
        pattern("\\+?[0-9\\s\\-\\(\\)]+")
    }

    selectField("subject", listOf(
        "General Inquiry",
        "Technical Support",
        "Billing Question",
        "Feature Request",
        "Other"
    )) {
        label("Subject")
        required()
    }

    textAreaField("message") {
        label("Message")
        required()
        minLength(10)
        maxLength(1000)
    }
}

/**
 * Example: Product review form.
 */
val productReviewForm = form("product_review") {
    numberField("rating") {
        label("Rating")
        required()
        range(1, 5)
    }

    textField("title") {
        label("Review Title")
        required()
        maxLength(100)
    }

    textAreaField("review_text") {
        label("Your Review")
        required()
        minLength(20)
        maxLength(500)
    }

    textField("reviewer_name") {
        label("Your Name")
        required()
    }

    emailField("reviewer_email") {
        label("Email")
        required()
    }

    booleanField("verified_purchase") {
        label("Verified Purchase")
        defaultValue(false)
    }

    booleanField("recommend") {
        label("Would you recommend this product?")
        required()
    }
}

/**
 * Example: Survey form with multiple field types.
 */
val customerSurveyForm = form("customer_survey") {
    textField("customer_id") {
        label("Customer ID")
        required()
        pattern("[A-Z0-9]{6,10}")
    }

    selectField("satisfaction", listOf(
        "Very Satisfied",
        "Satisfied",
        "Neutral",
        "Dissatisfied",
        "Very Dissatisfied"
    )) {
        label("Overall Satisfaction")
        required()
    }

    numberField("nps_score") {
        label("How likely are you to recommend us? (0-10)")
        required()
        range(0, 10)
    }

    textAreaField("feedback") {
        label("Additional Comments")
        maxLength(500)
    }

    booleanField("contact_allowed") {
        label("May we contact you about your feedback?")
        defaultValue(false)
    }
}

/**
 * Example: E-commerce checkout form.
 */
val checkoutForm = form("checkout") {
    // Contact information
    emailField("email") {
        label("Email")
        required()
    }

    // Shipping address
    textField("shipping_name") {
        label("Full Name")
        required()
    }

    textField("shipping_address1") {
        label("Address Line 1")
        required()
    }

    textField("shipping_address2") {
        label("Address Line 2 (Optional)")
    }

    textField("shipping_city") {
        label("City")
        required()
    }

    textField("shipping_state") {
        label("State/Province")
        required()
    }

    textField("shipping_postal_code") {
        label("Postal Code")
        required()
        pattern("[A-Z0-9\\s\\-]+")
    }

    selectField("shipping_country", listOf("US", "CA", "UK", "AU")) {
        label("Country")
        required()
    }

    // Payment information (note: in production, use secure payment gateway)
    textField("card_name") {
        label("Name on Card")
        required()
    }

    booleanField("billing_same_as_shipping") {
        label("Billing address same as shipping")
        defaultValue(true)
    }

    booleanField("terms_accepted") {
        label("I agree to the terms and conditions")
        required()
    }
}
