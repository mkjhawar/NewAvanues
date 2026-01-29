/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * DoNotClickReason.kt - Enumeration of reasons why elements should not be clicked
 *
 * Part of VoiceOSCoreNG Safety System.
 * Defines categories of dangerous UI interactions that should be avoided
 * during automated exploration.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Migrated to KMP: 2026-01-16
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md Section 5.1
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.voiceoscore

/**
 * Reason why an element should not be clicked during exploration.
 *
 * Used by DoNotClickList to classify dangerous elements and by
 * AVU export to generate DNC (Do Not Click) entries.
 *
 * IPC Format: DNC:element_id:label:type:reason
 * Example: DNC:btn-end:End Call:Button:CALL_ACTION
 *
 * @property ipcCode The 3-letter code used in AVU format
 * @property description Human-readable description for logs
 */
enum class DoNotClickReason(val ipcCode: String, val description: String) {
    /**
     * Could initiate or end phone/video calls.
     * Examples: "Call", "End Call", "Hang Up", "Decline", "Answer"
     */
    CALL_ACTION("CAL", "Could initiate or end calls"),

    /**
     * Could create posts, messages, or content.
     * Examples: "Post", "Send", "Share", "Tweet", "Comment"
     */
    CONTENT_CREATION("CRT", "Could create posts or messages"),

    /**
     * Could close the app or end the session.
     * Examples: "Exit", "Quit", "Close", "Leave", "Sign Out"
     */
    EXIT_ACTION("EXT", "Could close app or session"),

    /**
     * Could expose or modify authentication credentials.
     * Examples: "Login", "Password", "Sign In", "OTP"
     */
    AUTH_ACTION("AUT", "Could expose credentials"),

    /**
     * Could trigger purchases or financial transactions.
     * Examples: "Pay", "Purchase", "Subscribe", "Checkout"
     */
    PAYMENT_ACTION("PAY", "Could trigger purchases"),

    /**
     * Matched a dangerous resource ID pattern.
     * Used when resourceId contains known dangerous patterns.
     */
    DANGEROUS_ID("DID", "Matched dangerous resource ID pattern"),

    /**
     * Element is a password input field.
     * Never interact with password fields during exploration.
     */
    PASSWORD_FIELD("PWD", "Password input field"),

    /**
     * User-defined exclusion via configuration.
     * Allows custom element exclusions per app.
     */
    USER_DEFINED("USR", "User-defined exclusion");

    companion object {
        /**
         * Parse reason from IPC code string.
         *
         * @param code 3-letter IPC code (e.g., "CAL", "CRT")
         * @return Matching reason or null if not found
         */
        fun fromIpcCode(code: String): DoNotClickReason? {
            return entries.find { it.ipcCode.equals(code, ignoreCase = true) }
        }

        /**
         * Parse reason from AVU DNC line.
         *
         * Input: "DNC:btn-end:End Call:Button:CALL_ACTION"
         * Returns: CALL_ACTION
         *
         * @param line Full DNC line from AVU file
         * @return Matching reason or null if invalid
         */
        fun fromAvuLine(line: String): DoNotClickReason? {
            if (!line.startsWith("DNC:")) return null
            val parts = line.split(":")
            if (parts.size < 5) return null
            val reasonName = parts[4]
            return entries.find { it.name.equals(reasonName, ignoreCase = true) }
        }
    }
}
