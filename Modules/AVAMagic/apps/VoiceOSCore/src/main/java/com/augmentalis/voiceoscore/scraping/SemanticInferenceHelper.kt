/**
 * SemanticInferenceHelper.kt - AI context inference for scraped elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-18
 */
package com.augmentalis.voiceoscore.scraping

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Semantic Inference Helper
 *
 * Infers semantic context from UI elements for AI-powered features.
 * Provides Phase 1 context inference: semantic role, input type, visual weight, required status.
 *
 * Usage:
 * ```kotlin
 * val helper = SemanticInferenceHelper()
 * val semanticRole = helper.inferSemanticRole(node, resourceId, text, contentDesc)
 * val inputType = helper.inferInputType(node, resourceId, text)
 * val visualWeight = helper.inferVisualWeight(resourceId, text, className)
 * val isRequired = helper.inferIsRequired(contentDesc, text)
 * ```
 */
class SemanticInferenceHelper {

    companion object {
        // Semantic role keywords for button actions
        private val SUBMIT_KEYWORDS = setOf("submit", "send", "post", "publish", "confirm", "ok", "done", "save", "apply", "continue")
        private val LOGIN_KEYWORDS = setOf("login", "log in", "sign in", "signin")
        private val SIGNUP_KEYWORDS = setOf("signup", "sign up", "register", "create account")
        private val PAYMENT_KEYWORDS = setOf("pay", "checkout", "purchase", "buy", "order")
        private val CANCEL_KEYWORDS = setOf("cancel", "close", "dismiss", "skip", "back")
        private val DELETE_KEYWORDS = setOf("delete", "remove", "clear", "trash")
        private val SEARCH_KEYWORDS = setOf("search", "find", "query")
        private val SHARE_KEYWORDS = setOf("share", "forward", "send")
        private val LIKE_KEYWORDS = setOf("like", "favorite", "heart", "upvote")
        private val COMMENT_KEYWORDS = setOf("comment", "reply", "respond")
        private val NAVIGATE_KEYWORDS = setOf("next", "previous", "back", "forward", "home", "menu")

        // Input type keywords
        private val EMAIL_KEYWORDS = setOf("email", "e-mail", "mail")
        private val PASSWORD_KEYWORDS = setOf("password", "pwd", "passcode", "pin")
        private val PHONE_KEYWORDS = setOf("phone", "mobile", "telephone", "tel")
        private val NAME_KEYWORDS = setOf("name", "username", "user name")
        private val ADDRESS_KEYWORDS = setOf("address", "street", "city", "zip", "postal")
        private val URL_KEYWORDS = setOf("url", "website", "link", "web")
        private val NUMBER_KEYWORDS = setOf("number", "amount", "quantity", "count")
        private val DATE_KEYWORDS = setOf("date", "birthday", "dob", "day", "month", "year")
        private val SEARCH_INPUT_KEYWORDS = setOf("search", "query", "find")

        // Visual weight keywords
        private val PRIMARY_KEYWORDS = setOf("primary", "main", "submit", "confirm", "continue", "next", "save", "done")
        private val DANGER_KEYWORDS = setOf("delete", "remove", "cancel", "logout", "sign out", "clear")

        // Required field indicators
        private val REQUIRED_INDICATORS = setOf("required", "mandatory", "*", "必須")
    }

    /**
     * Infer semantic role from element properties
     *
     * Returns role like: "submit_login", "input_email", "navigate_back", "toggle_like"
     */
    fun inferSemanticRole(
        node: AccessibilityNodeInfo?,
        resourceId: String?,
        text: String?,
        contentDescription: String?,
        className: String
    ): String? {
        val lowerResourceId = resourceId?.lowercase() ?: ""
        val lowerText = text?.lowercase() ?: ""
        val lowerDesc = contentDescription?.lowercase() ?: ""
        val combined = "$lowerResourceId $lowerText $lowerDesc"

        // Buttons - infer action intent
        if (className.contains("Button", ignoreCase = true)) {
            return when {
                // Authentication
                containsAny(combined, LOGIN_KEYWORDS) -> "submit_login"
                containsAny(combined, SIGNUP_KEYWORDS) -> "submit_signup"

                // Transactions
                containsAny(combined, PAYMENT_KEYWORDS) -> "submit_payment"

                // General submissions
                containsAny(combined, SUBMIT_KEYWORDS) -> "submit_form"

                // Navigation
                containsAny(combined, NAVIGATE_KEYWORDS) && combined.contains("back") -> "navigate_back"
                containsAny(combined, NAVIGATE_KEYWORDS) && combined.contains("next") -> "navigate_next"
                containsAny(combined, NAVIGATE_KEYWORDS) && combined.contains("home") -> "navigate_home"
                containsAny(combined, NAVIGATE_KEYWORDS) && combined.contains("menu") -> "navigate_menu"

                // Social actions
                containsAny(combined, LIKE_KEYWORDS) -> "toggle_like"
                containsAny(combined, SHARE_KEYWORDS) -> "share_content"
                containsAny(combined, COMMENT_KEYWORDS) -> "add_comment"

                // Destructive actions
                containsAny(combined, DELETE_KEYWORDS) -> "delete_item"
                containsAny(combined, CANCEL_KEYWORDS) -> "cancel_action"

                // Search
                containsAny(combined, SEARCH_KEYWORDS) -> "submit_search"

                else -> null
            }
        }

        // EditText - infer input purpose
        if (className.contains("EditText", ignoreCase = true)) {
            return when {
                containsAny(combined, EMAIL_KEYWORDS) -> "input_email"
                containsAny(combined, PASSWORD_KEYWORDS) -> "input_password"
                containsAny(combined, PHONE_KEYWORDS) -> "input_phone"
                containsAny(combined, NAME_KEYWORDS) -> "input_name"
                containsAny(combined, ADDRESS_KEYWORDS) -> "input_address"
                containsAny(combined, URL_KEYWORDS) -> "input_url"
                containsAny(combined, SEARCH_INPUT_KEYWORDS) -> "input_search"
                containsAny(combined, COMMENT_KEYWORDS) -> "input_comment"
                else -> "input_text"
            }
        }

        // CheckBox/Switch - infer toggle purpose
        if (className.contains("CheckBox", ignoreCase = true) || className.contains("Switch", ignoreCase = true)) {
            return when {
                combined.contains("remember") || combined.contains("keep me logged in") -> "toggle_remember"
                combined.contains("agree") || combined.contains("accept") -> "toggle_agreement"
                combined.contains("subscribe") || combined.contains("notification") -> "toggle_subscription"
                else -> "toggle_option"
            }
        }

        // ImageButton/ImageView - might be action buttons
        if (className.contains("ImageButton", ignoreCase = true) || className.contains("ImageView", ignoreCase = true)) {
            if (node?.isClickable == true) {
                return when {
                    containsAny(combined, LIKE_KEYWORDS) -> "toggle_like"
                    containsAny(combined, SHARE_KEYWORDS) -> "share_content"
                    containsAny(combined, NAVIGATE_KEYWORDS) && combined.contains("back") -> "navigate_back"
                    containsAny(combined, NAVIGATE_KEYWORDS) && combined.contains("menu") -> "navigate_menu"
                    else -> null
                }
            }
        }

        return null
    }

    /**
     * Infer input type from EditText element
     *
     * Returns: "email", "password", "phone", "url", "number", "date", "text"
     */
    fun inferInputType(
        node: AccessibilityNodeInfo?,
        resourceId: String?,
        text: String?,
        contentDescription: String?
    ): String? {
        // Only infer for editable fields
        if (node?.isEditable != true) return null

        val lowerResourceId = resourceId?.lowercase() ?: ""
        val lowerDesc = contentDescription?.lowercase() ?: ""
        val combined = "$lowerResourceId $lowerDesc"

        return when {
            // Use AccessibilityNodeInfo.inputType if available
            node.isPassword -> "password"

            // Infer from resource ID and content description
            containsAny(combined, EMAIL_KEYWORDS) -> "email"
            containsAny(combined, PASSWORD_KEYWORDS) -> "password"
            containsAny(combined, PHONE_KEYWORDS) -> "phone"
            containsAny(combined, URL_KEYWORDS) -> "url"
            containsAny(combined, NUMBER_KEYWORDS) -> "number"
            containsAny(combined, DATE_KEYWORDS) -> "date"
            containsAny(combined, SEARCH_INPUT_KEYWORDS) -> "search"

            else -> "text"  // Default to generic text input
        }
    }

    /**
     * Infer visual weight/emphasis from element properties
     *
     * Returns: "primary", "secondary", "tertiary", "danger"
     */
    fun inferVisualWeight(
        resourceId: String?,
        text: String?,
        className: String
    ): String? {
        // Only infer for clickable elements (buttons, etc.)
        if (!className.contains("Button", ignoreCase = true)) return null

        val lowerResourceId = resourceId?.lowercase() ?: ""
        val lowerText = text?.lowercase() ?: ""
        val combined = "$lowerResourceId $lowerText"

        return when {
            // Danger actions (destructive)
            containsAny(combined, DANGER_KEYWORDS) -> "danger"

            // Primary actions (main CTA)
            containsAny(combined, PRIMARY_KEYWORDS) -> "primary"

            // Cancel/secondary actions
            containsAny(combined, CANCEL_KEYWORDS) -> "secondary"

            // Default to secondary for buttons we can't classify
            else -> "secondary"
        }
    }

    /**
     * Infer if field is required based on text indicators
     *
     * Returns: true if required indicators found, null if uncertain
     */
    fun inferIsRequired(
        contentDescription: String?,
        text: String?,
        resourceId: String?
    ): Boolean? {
        val lowerDesc = contentDescription?.lowercase() ?: ""
        val lowerText = text?.lowercase() ?: ""
        val lowerResourceId = resourceId?.lowercase() ?: ""
        val combined = "$lowerDesc $lowerText $lowerResourceId"

        return when {
            // Explicit required indicators
            containsAny(combined, REQUIRED_INDICATORS) -> true

            // Asterisk in text/description (common required indicator)
            contentDescription?.contains("*") == true -> true
            text?.contains("*") == true -> true

            // Email and password fields are typically required for login/signup
            combined.contains("email") && (combined.contains("login") || combined.contains("signup")) -> true
            combined.contains("password") && (combined.contains("login") || combined.contains("signup")) -> true

            // Can't determine - return null
            else -> null
        }
    }

    /**
     * Helper: Check if text contains any keywords from set
     */
    private fun containsAny(text: String, keywords: Set<String>): Boolean {
        return keywords.any { keyword -> text.contains(keyword) }
    }
}
