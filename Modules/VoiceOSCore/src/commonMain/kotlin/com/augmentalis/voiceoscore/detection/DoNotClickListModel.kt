/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * DoNotClickListModel.kt - Safety filter for dangerous UI elements
 *
 * Part of VoiceOSCoreNG Safety System.
 * Identifies UI elements that should NOT be clicked during automated exploration
 * to prevent destructive actions like initiating calls, creating posts, or triggering payments.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Migrated to KMP: 2026-01-16
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md Section 5.1
 *
 * ## Safety Categories:
 * - CALL_ACTION: Phone/video call controls
 * - CONTENT_CREATION: Post, send, share buttons
 * - EXIT_ACTION: Logout, exit, close buttons
 * - AUTH_ACTION: Login fields and buttons
 * - PAYMENT_ACTION: Purchase, subscribe buttons
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.ElementInfo

/**
 * Do Not Click List - Identifies dangerous UI elements
 *
 * Stateless object that checks if a UI element should be avoided during exploration.
 * Uses keyword matching on labels/text and regex patterns on resource IDs.
 *
 * ## Usage
 * ```kotlin
 * val element = ElementInfo(...)
 * val reason = DoNotClickList.shouldNotClick(element)
 * if (reason != null) {
 *     // Skip this element, log DNC entry
 *     logDnc(element, reason)
 * }
 * ```
 */
object DoNotClickList {

    // ============================================================
    // Category: CALL_ACTIONS
    // Elements that could initiate or end phone/video calls
    // ============================================================
    val CALL_KEYWORDS = setOf(
        "call",
        "dial",
        "ring",
        "phone",
        "end call",
        "hang up",
        "decline",
        "answer",
        "accept call",
        "reject",
        "video call",
        "voice call",
        "start call",
        "join call",
        "leave call",
        "mute call",
        "unmute"
    )

    // ============================================================
    // Category: CONTENT_CREATION
    // Elements that could create posts, messages, or content
    // ============================================================
    val POST_KEYWORDS = setOf(
        "post",
        "publish",
        "send",
        "submit",
        "share",
        "tweet",
        "comment",
        "reply",
        "create",
        "new post",
        "upload",
        "compose",
        "write",
        "send message",
        "send email",
        "forward",
        "retweet",
        "quote"
    )

    // ============================================================
    // Category: EXIT_ACTIONS
    // Elements that could close the app or end the session
    // ============================================================
    val EXIT_KEYWORDS = setOf(
        "exit",
        "quit",
        "close",
        "end",
        "leave",
        "sign out",
        "log out",
        "logout",
        "signout",
        "delete",
        "remove",
        "cancel",
        "discard",
        "clear all",
        "end session",
        "terminate"
    )

    // ============================================================
    // Category: AUTH_ACTIONS
    // Elements related to authentication (skip to avoid credential exposure)
    // ============================================================
    val AUTH_KEYWORDS = setOf(
        "login",
        "log in",
        "sign in",
        "signin",
        "password",
        "username",
        "email",
        "forgot password",
        "reset",
        "otp",
        "verification code",
        "2fa",
        "two-factor",
        "authenticate",
        "continue with",
        "sign up",
        "register",
        "create account"
    )

    // ============================================================
    // Category: PAYMENT_ACTIONS
    // Elements that could trigger purchases or financial transactions
    // ============================================================
    val PAYMENT_KEYWORDS = setOf(
        "pay",
        "purchase",
        "buy",
        "checkout",
        "subscribe",
        "upgrade",
        "premium",
        "credit card",
        "billing",
        "add to cart",
        "order",
        "place order",
        "confirm purchase",
        "pay now",
        "buy now",
        "donate",
        "tip",
        "in-app purchase"
    )

    // ============================================================
    // Resource ID patterns (regex) for dangerous elements
    // Matches against viewIdResourceName
    // ============================================================
    val DANGEROUS_RESOURCE_IDS = listOf(
        // Call-related
        Regex(".*end_call.*", RegexOption.IGNORE_CASE),
        Regex(".*hangup.*", RegexOption.IGNORE_CASE),
        Regex(".*decline_call.*", RegexOption.IGNORE_CASE),
        Regex(".*answer_call.*", RegexOption.IGNORE_CASE),

        // Content creation
        Regex(".*btn_post.*", RegexOption.IGNORE_CASE),
        Regex(".*btn_send.*", RegexOption.IGNORE_CASE),
        Regex(".*send_button.*", RegexOption.IGNORE_CASE),
        Regex(".*post_button.*", RegexOption.IGNORE_CASE),
        Regex(".*share_button.*", RegexOption.IGNORE_CASE),
        Regex(".*compose.*", RegexOption.IGNORE_CASE),

        // Exit/destructive
        Regex(".*delete.*", RegexOption.IGNORE_CASE),
        Regex(".*logout.*", RegexOption.IGNORE_CASE),
        Regex(".*sign_out.*", RegexOption.IGNORE_CASE),
        Regex(".*clear_all.*", RegexOption.IGNORE_CASE),

        // Payment
        Regex(".*payment.*", RegexOption.IGNORE_CASE),
        Regex(".*checkout.*", RegexOption.IGNORE_CASE),
        Regex(".*purchase.*", RegexOption.IGNORE_CASE),
        Regex(".*subscribe.*", RegexOption.IGNORE_CASE)
    )

    /**
     * Check if an element should NOT be clicked during exploration.
     *
     * Analyzes element label (text + contentDescription) and resourceId
     * against safety keyword sets and dangerous ID patterns.
     *
     * @param element UI element to check
     * @return DoNotClickReason if element is dangerous, null if safe to click
     */
    fun shouldNotClick(element: ElementInfo): DoNotClickReason? {
        // Check password field first (highest priority)
        // Note: ElementInfo in VoiceOSCoreNG doesn't have isPassword,
        // so we check class name for password-like fields
        if (isPasswordField(element)) {
            return DoNotClickReason.PASSWORD_FIELD
        }

        // Get normalized label (text or contentDescription)
        val label = element.voiceLabel.lowercase()
        val resourceId = element.resourceId.lowercase()

        // Check keyword categories
        return when {
            // Call actions
            CALL_KEYWORDS.any { keyword ->
                label.contains(keyword) || resourceId.contains(keyword.replace(" ", "_"))
            } -> DoNotClickReason.CALL_ACTION

            // Content creation
            POST_KEYWORDS.any { keyword ->
                label.contains(keyword) || resourceId.contains(keyword.replace(" ", "_"))
            } -> DoNotClickReason.CONTENT_CREATION

            // Exit actions
            EXIT_KEYWORDS.any { keyword ->
                label.contains(keyword) || resourceId.contains(keyword.replace(" ", "_"))
            } -> DoNotClickReason.EXIT_ACTION

            // Auth actions
            AUTH_KEYWORDS.any { keyword ->
                label.contains(keyword) || resourceId.contains(keyword.replace(" ", "_"))
            } -> DoNotClickReason.AUTH_ACTION

            // Payment actions
            PAYMENT_KEYWORDS.any { keyword ->
                label.contains(keyword) || resourceId.contains(keyword.replace(" ", "_"))
            } -> DoNotClickReason.PAYMENT_ACTION

            // Dangerous resource ID patterns
            DANGEROUS_RESOURCE_IDS.any { pattern ->
                pattern.matches(resourceId)
            } -> DoNotClickReason.DANGEROUS_ID

            // Safe to click
            else -> null
        }
    }

    /**
     * Check if element is a password field.
     *
     * Uses class name heuristics since ElementInfo doesn't have isPassword.
     */
    private fun isPasswordField(element: ElementInfo): Boolean {
        val className = element.className.lowercase()
        val resourceId = element.resourceId.lowercase()
        val label = element.voiceLabel.lowercase()

        return className.contains("password") ||
               resourceId.contains("password") ||
               resourceId.contains("pwd") ||
               (label.contains("password") && className.contains("edittext"))
    }

    /**
     * Check multiple elements and return filtered safe elements.
     *
     * @param elements List of elements to filter
     * @return Pair of (safe elements, dangerous elements with reasons)
     */
    fun filterElements(elements: List<ElementInfo>): Pair<List<ElementInfo>, List<Pair<ElementInfo, DoNotClickReason>>> {
        val safe = mutableListOf<ElementInfo>()
        val dangerous = mutableListOf<Pair<ElementInfo, DoNotClickReason>>()

        for (element in elements) {
            val reason = shouldNotClick(element)
            if (reason != null) {
                dangerous.add(element to reason)
            } else {
                safe.add(element)
            }
        }

        return safe to dangerous
    }

    /**
     * Generate DNC IPC line for AVU export.
     *
     * Format: DNC:element_id:label:type:reason
     *
     * @param element The dangerous element
     * @param reason Why it's dangerous
     * @return IPC-formatted DNC line
     */
    fun toDncLine(element: ElementInfo, reason: DoNotClickReason): String {
        val elementId = element.avid ?: element.stableId().take(20)
        val label = element.voiceLabel.take(30).replace(":", "_")
        val type = element.className.substringAfterLast(".").take(20)
        return "DNC:$elementId:$label:$type:${reason.name}"
    }

    /**
     * Check if element type is inherently dangerous.
     *
     * Some element types should always be checked more carefully:
     * - EditText (could be password field)
     * - Button with specific styling
     *
     * @param className Class name
     * @return true if type requires extra caution
     */
    fun isHighRiskType(className: String): Boolean {
        val lowerClass = className.lowercase()
        return lowerClass.contains("edittext") ||
               lowerClass.contains("textinputedittext") ||
               lowerClass.contains("password")
    }

    /**
     * Get all keywords for a specific reason category.
     *
     * Useful for UI display or configuration.
     *
     * @param reason The category to get keywords for
     * @return Set of keywords for that category
     */
    fun getKeywordsForReason(reason: DoNotClickReason): Set<String> {
        return when (reason) {
            DoNotClickReason.CALL_ACTION -> CALL_KEYWORDS
            DoNotClickReason.CONTENT_CREATION -> POST_KEYWORDS
            DoNotClickReason.EXIT_ACTION -> EXIT_KEYWORDS
            DoNotClickReason.AUTH_ACTION -> AUTH_KEYWORDS
            DoNotClickReason.PAYMENT_ACTION -> PAYMENT_KEYWORDS
            else -> emptySet()
        }
    }
}
