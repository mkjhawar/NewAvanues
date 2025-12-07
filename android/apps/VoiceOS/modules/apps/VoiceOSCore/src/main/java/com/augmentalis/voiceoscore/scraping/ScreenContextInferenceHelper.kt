/**
 * ScreenContextInferenceHelper.kt - Screen-level context inference
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-18
 */
package com.augmentalis.voiceoscore.scraping

import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity

/**
 * Screen Context Inference Helper
 *
 * Infers screen-level context for AI-powered features (Phase 2).
 * Analyzes screen structure, content, and element patterns to classify screen type.
 *
 * Usage:
 * ```kotlin
 * val helper = ScreenContextInferenceHelper()
 * val screenType = helper.inferScreenType(windowTitle, elements)
 * val formContext = helper.inferFormContext(elements)
 * val primaryAction = helper.inferPrimaryAction(elements)
 * ```
 */
class ScreenContextInferenceHelper {

    companion object {
        // Screen type keywords
        private val LOGIN_KEYWORDS = setOf("login", "log in", "sign in", "signin", "authentication", "auth")
        private val SIGNUP_KEYWORDS = setOf("signup", "sign up", "register", "registration", "create account", "join")
        private val CHECKOUT_KEYWORDS = setOf("checkout", "payment", "billing", "order", "purchase", "buy")
        private val SETTINGS_KEYWORDS = setOf("settings", "preferences", "configuration", "options")
        private val HOME_KEYWORDS = setOf("home", "main", "dashboard", "feed", "timeline")
        private val SEARCH_KEYWORDS = setOf("search", "find", "explore", "discover")
        private val PROFILE_KEYWORDS = setOf("profile", "account", "my account", "user")
        private val FORM_KEYWORDS = setOf("form", "submit", "complete", "fill")
        private val CART_KEYWORDS = setOf("cart", "basket", "bag", "shopping")
        private val DETAIL_KEYWORDS = setOf("detail", "details", "info", "information")
        private val LIST_KEYWORDS = setOf("list", "results", "browse")

        // Form context keywords
        private val REGISTRATION_KEYWORDS = setOf("register", "signup", "create account")
        private val PAYMENT_KEYWORDS = setOf("payment", "billing", "credit card", "debit card")
        private val ADDRESS_KEYWORDS = setOf("address", "shipping", "delivery")
        private val CONTACT_KEYWORDS = setOf("contact", "phone", "email")
        private val FEEDBACK_KEYWORDS = setOf("feedback", "review", "comment", "rating")

        // Primary action keywords
        private val SUBMIT_ACTION_KEYWORDS = setOf("submit", "send", "post", "save")
        private val SEARCH_ACTION_KEYWORDS = setOf("search", "find", "query")
        private val BROWSE_ACTION_KEYWORDS = setOf("browse", "explore", "discover")
        private val PURCHASE_ACTION_KEYWORDS = setOf("buy", "purchase", "checkout", "order")
    }

    /**
     * Infer screen type from window title and elements
     *
     * Returns: "login", "signup", "checkout", "settings", "home", "search", "profile", "form", "cart", "detail", "list", or null
     */
    fun inferScreenType(
        windowTitle: String?,
        activityName: String?,
        elements: List<ScrapedElementEntity>
    ): String? {
        val lowerTitle = windowTitle?.lowercase() ?: ""
        val lowerActivity = activityName?.lowercase() ?: ""
        val combined = "$lowerTitle $lowerActivity"

        // Collect all text from elements for analysis
        val elementTexts = elements.mapNotNull { it.text?.lowercase() } +
                          elements.mapNotNull { it.contentDescription?.lowercase() }
        val allText = "$combined ${elementTexts.joinToString(" ")}"

        return when {
            // Authentication screens
            containsAny(allText, LOGIN_KEYWORDS) && !containsAny(allText, SIGNUP_KEYWORDS) -> "login"
            containsAny(allText, SIGNUP_KEYWORDS) -> "signup"

            // Commerce screens
            containsAny(allText, CHECKOUT_KEYWORDS) -> "checkout"
            containsAny(allText, CART_KEYWORDS) -> "cart"

            // Navigation screens
            containsAny(allText, SETTINGS_KEYWORDS) -> "settings"
            containsAny(allText, HOME_KEYWORDS) && lowerTitle.contains("home") -> "home"
            containsAny(allText, SEARCH_KEYWORDS) -> "search"
            containsAny(allText, PROFILE_KEYWORDS) -> "profile"

            // Content screens
            containsAny(allText, DETAIL_KEYWORDS) -> "detail"
            containsAny(allText, LIST_KEYWORDS) -> "list"

            // Form screen (generic - check for multiple input fields)
            hasMultipleInputFields(elements) && containsAny(allText, FORM_KEYWORDS) -> "form"

            else -> null
        }
    }

    /**
     * Infer form-specific context
     *
     * Returns: "registration", "payment", "address", "contact", "feedback", "search", or null
     */
    fun inferFormContext(elements: List<ScrapedElementEntity>): String? {
        val elementTexts = elements.mapNotNull { it.text?.lowercase() } +
                          elements.mapNotNull { it.contentDescription?.lowercase() } +
                          elements.mapNotNull { it.viewIdResourceName?.lowercase() }
        val allText = elementTexts.joinToString(" ")

        return when {
            containsAny(allText, REGISTRATION_KEYWORDS) -> "registration"
            containsAny(allText, PAYMENT_KEYWORDS) -> "payment"
            containsAny(allText, ADDRESS_KEYWORDS) -> "address"
            containsAny(allText, CONTACT_KEYWORDS) -> "contact"
            containsAny(allText, FEEDBACK_KEYWORDS) -> "feedback"
            containsAny(allText, SEARCH_ACTION_KEYWORDS) && hasInputFields(elements) -> "search"
            else -> null
        }
    }

    /**
     * Infer primary user action for this screen
     *
     * Returns: "submit", "search", "browse", "purchase", "view", or null
     */
    fun inferPrimaryAction(elements: List<ScrapedElementEntity>): String? {
        // Find buttons and their text
        val buttonTexts = elements
            .filter { it.className.contains("Button", ignoreCase = true) && it.isClickable }
            .mapNotNull { it.text?.lowercase() }
            .joinToString(" ")

        return when {
            containsAny(buttonTexts, SUBMIT_ACTION_KEYWORDS) -> "submit"
            containsAny(buttonTexts, SEARCH_ACTION_KEYWORDS) -> "search"
            containsAny(buttonTexts, PURCHASE_ACTION_KEYWORDS) -> "purchase"
            containsAny(buttonTexts, BROWSE_ACTION_KEYWORDS) -> "browse"
            elements.any { it.isScrollable } -> "browse"
            else -> "view"
        }
    }

    /**
     * Calculate navigation level based on back button presence and screen depth
     *
     * Returns: 0 for main screen, 1+ for nested screens
     */
    fun inferNavigationLevel(hasBackButton: Boolean, windowTitle: String?): Int {
        return when {
            !hasBackButton && windowTitle?.lowercase()?.contains("home") == true -> 0
            !hasBackButton -> 0
            hasBackButton -> 1
            else -> 0  // Default to main screen
        }
    }

    /**
     * Infer placeholder text from AccessibilityNodeInfo
     */
    fun extractPlaceholderText(node: AccessibilityNodeInfo?): String? {
        // Try to get hint text (placeholder)
        return node?.hintText?.toString()
    }

    /**
     * Infer validation pattern from input type, Android inputType flags, and resource ID
     * Enhanced in Phase 2.5 to use AccessibilityNodeInfo.inputType
     */
    fun inferValidationPattern(
        node: AccessibilityNodeInfo?,
        resourceId: String?,
        inputType: String?,
        className: String
    ): String? {
        if (!className.contains("EditText", ignoreCase = true)) return null

        // Strategy 1: Check Android inputType flags (most reliable)
        node?.inputType?.let { androidInputType ->
            // Import android.text.InputType constants
            val TYPE_CLASS_TEXT = 0x00000001
            val TYPE_CLASS_NUMBER = 0x00000002
            val TYPE_CLASS_PHONE = 0x00000003
            val TYPE_CLASS_DATETIME = 0x00000004

            val TYPE_TEXT_VARIATION_EMAIL_ADDRESS = 0x00000020
            val TYPE_TEXT_VARIATION_PASSWORD = 0x00000080
            val TYPE_TEXT_VARIATION_URI = 0x00000010
            val TYPE_TEXT_VARIATION_POSTAL_ADDRESS = 0x00000070
            val TYPE_NUMBER_VARIATION_PASSWORD = 0x00000010

            // Check for specific input types
            when {
                (androidInputType and TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0 -> return "email"
                (androidInputType and TYPE_TEXT_VARIATION_PASSWORD) != 0 -> return "password"
                (androidInputType and TYPE_NUMBER_VARIATION_PASSWORD) != 0 -> return "password"
                (androidInputType and TYPE_TEXT_VARIATION_URI) != 0 -> return "url"
                (androidInputType and TYPE_TEXT_VARIATION_POSTAL_ADDRESS) != 0 -> return "zip_code"
                (androidInputType and 0x0000000f) == TYPE_CLASS_PHONE -> return "phone"
                (androidInputType and 0x0000000f) == TYPE_CLASS_NUMBER -> return "number"
                (androidInputType and 0x0000000f) == TYPE_CLASS_DATETIME -> return "date"
                else -> {}  // Fall through to other strategies
            }
        }

        // Strategy 2: Use inferred input type from Phase 1
        if (inputType != null) {
            return when (inputType) {
                "email" -> "email"
                "password" -> "password"
                "phone" -> "phone"
                "url" -> "url"
                "number" -> "number"
                "date" -> "date"
                else -> null
            }
        }

        // Strategy 3: Fall back to resource ID keyword matching
        val lowerResourceId = resourceId?.lowercase() ?: ""

        return when {
            lowerResourceId.contains("email") -> "email"
            lowerResourceId.contains("phone") -> "phone"
            lowerResourceId.contains("url") || lowerResourceId.contains("website") -> "url"
            lowerResourceId.contains("zip") || lowerResourceId.contains("postal") -> "zip_code"
            lowerResourceId.contains("credit") || lowerResourceId.contains("card") -> "credit_card"
            lowerResourceId.contains("ssn") || lowerResourceId.contains("social") -> "ssn"
            lowerResourceId.contains("date") || lowerResourceId.contains("dob") -> "date"
            else -> null
        }
    }

    /**
     * Extract background color from AccessibilityNodeInfo
     */
    fun extractBackgroundColor(node: AccessibilityNodeInfo?): String? {
        // Note: AccessibilityNodeInfo doesn't directly expose background color
        // This would require additional integration with View properties
        // For now, return null - can be enhanced later
        return null
    }

    /**
     * Generate form group ID for related form elements
     *
     * Groups elements that appear sequentially and are of similar type
     */
    fun generateFormGroupId(
        packageName: String,
        screenHash: String,
        elementDepth: Int,
        formContext: String?
    ): String? {
        // Only group form elements
        if (formContext == null) return null

        // Generate a stable group ID based on screen and context
        return "${packageName}_${screenHash.take(8)}_${formContext}_depth${elementDepth}"
    }

    /**
     * Helper: Check if screen has multiple input fields (indicates form)
     */
    private fun hasMultipleInputFields(elements: List<ScrapedElementEntity>): Boolean {
        val inputCount = elements.count {
            it.isEditable || it.className.contains("EditText", ignoreCase = true)
        }
        return inputCount >= 2
    }

    /**
     * Helper: Check if screen has any input fields
     */
    private fun hasInputFields(elements: List<ScrapedElementEntity>): Boolean {
        return elements.any {
            it.isEditable || it.className.contains("EditText", ignoreCase = true)
        }
    }

    /**
     * Helper: Check if text contains any keywords from set
     */
    private fun containsAny(text: String, keywords: Set<String>): Boolean {
        return keywords.any { keyword -> text.contains(keyword) }
    }
}
