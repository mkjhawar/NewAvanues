/**
 * DangerDetector.kt - Dangerous element detection
 *
 * Detects dangerous and critical dangerous UI elements that should not be
 * clicked during automated exploration. Pure string matching - KMP compatible.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.voiceoscoreng.exploration

import com.augmentalis.voiceoscoreng.common.ElementInfo

/**
 * Detects dangerous UI elements that should be handled carefully during exploration.
 *
 * This class identifies elements that:
 * - May cause navigation away from the app (dangerous)
 * - Should NEVER be clicked (critical dangerous)
 * - Could have destructive side effects
 *
 * ## Classification Levels
 *
 * ### Dangerous Elements (clicked last)
 * - Form submission buttons (Submit, Send, Confirm)
 * - Exit actions (Sign out, Close)
 * - Destructive actions (Delete, Remove)
 *
 * ### Critical Dangerous Elements (NEVER clicked)
 * - System power actions (Power off, Restart)
 * - App termination (Exit, Force stop)
 * - Session termination (Sign out, Log out)
 * - Call/meeting actions (Call, Join meeting)
 * - Message sending (Reply)
 *
 * ## Usage Example
 *
 * ```kotlin
 * val detector = DangerDetector()
 *
 * // Check if element is dangerous
 * if (detector.isDangerous(element)) {
 *     // Click last
 * }
 *
 * // Check if element is critical (never click)
 * if (detector.isCriticalDangerous(element)) {
 *     // Skip entirely
 * }
 * ```
 */
class DangerDetector {

    /**
     * Check if element is "dangerous" - likely to navigate away from app.
     *
     * Dangerous buttons are clicked last to maximize exploration before potential navigation.
     *
     * Dangerous patterns:
     * - Submit, Send, Confirm, Done, OK, Apply (form submissions)
     * - Sign out, Log out, Exit, Close, Quit (exit actions)
     * - Delete, Remove (destructive actions)
     *
     * @param element Element to check
     * @return true if element is potentially dangerous
     */
    fun isDangerous(element: ElementInfo): Boolean {
        val text = element.text?.lowercase() ?: ""
        val contentDesc = element.contentDescription?.lowercase() ?: ""
        val combinedText = "$text $contentDesc"

        return DANGEROUS_PATTERNS.any { pattern ->
            combinedText.contains(pattern)
        }
    }

    /**
     * Check if element is CRITICAL dangerous (should NEVER be clicked)
     *
     * These elements would cause severe side effects if clicked:
     * - Power off, Shutdown, Restart, Sleep (system actions)
     * - Exit, Quit, Close (app termination)
     * - Sign out, Log out, Logout (session termination)
     * - Delete account, Deactivate (destructive account actions)
     * - Call, Meeting actions (initiates communication)
     * - Reply (sends messages)
     *
     * @param element Element to check
     * @return true if element should never be clicked
     */
    fun isCriticalDangerous(element: ElementInfo): Boolean {
        val text = element.text?.lowercase() ?: ""
        val contentDesc = element.contentDescription?.lowercase() ?: ""
        val resourceId = element.resourceId?.lowercase() ?: ""
        val combinedText = "$text $contentDesc $resourceId"

        return CRITICAL_PATTERNS.any { pattern ->
            combinedText.contains(pattern)
        }
    }

    /**
     * Get the reason why an element is critically dangerous
     *
     * @param element Element to check
     * @return Reason string or null if not dangerous
     */
    fun getCriticalDangerReason(element: ElementInfo): String? {
        val text = element.text?.lowercase() ?: ""
        val contentDesc = element.contentDescription?.lowercase() ?: ""
        val resourceId = element.resourceId?.lowercase() ?: ""
        val combinedText = "$text $contentDesc $resourceId"

        for ((pattern, reason) in CRITICAL_PATTERNS_WITH_REASONS) {
            if (combinedText.contains(pattern)) {
                return reason
            }
        }

        return null
    }

    /**
     * Get danger level for sorting (higher = more dangerous)
     *
     * @param element Element to check
     * @return Danger level: 0 = safe, 1 = dangerous, 2 = critical
     */
    fun getDangerLevel(element: ElementInfo): Int {
        return when {
            isCriticalDangerous(element) -> 2
            isDangerous(element) -> 1
            else -> 0
        }
    }

    companion object {
        /**
         * Dangerous patterns that may navigate away or submit data
         * Elements matching these are clicked LAST
         */
        private val DANGEROUS_PATTERNS = listOf(
            // Form submission
            "submit", "send", "confirm", "done", "apply", "save",
            "post", "publish", "upload", "share",
            // Exit actions
            "sign out", "signout", "log out", "logout", "exit", "quit", "close",
            // Destructive
            "delete", "remove", "clear all", "reset",
            // Navigation that might leave app
            "continue", "proceed", "next", "finish"
        )

        /**
         * Critical patterns that should NEVER be clicked during exploration
         */
        private val CRITICAL_PATTERNS = listOf(
            // System power actions
            "power off", "poweroff", "shut down", "shutdown", "restart", "reboot",
            "sleep", "hibernate", "turn off",
            // App termination
            "exit", "quit", "close app", "force stop", "force close",
            // Session termination
            "sign out", "signout", "log out", "logout", "sign-out", "log-out",
            // Destructive account actions
            "delete account", "deactivate account", "remove account", "close account",
            // Factory/system reset
            "factory reset", "wipe data", "erase all", "format",
            // Call/meeting actions (NEVER initiate calls)
            "call", "make a call", "make call", "start call", "audio call", "video call",
            "dial", "answer", "join call", "join meeting", "new meeting", "schedule meeting",
            "instant meeting", "meet now", "call_control", "call_end", "calls_call",
            // Reply actions (can send messages)
            "reply"
        )

        /**
         * Critical patterns with their reasons for logging/display
         */
        private val CRITICAL_PATTERNS_WITH_REASONS = listOf(
            // System power actions
            "power off" to "Power off (CRITICAL)",
            "poweroff" to "Power off (CRITICAL)",
            "shut down" to "Shutdown (CRITICAL)",
            "shutdown" to "Shutdown (CRITICAL)",
            "restart" to "Restart (CRITICAL)",
            "reboot" to "Reboot (CRITICAL)",
            "sleep" to "Sleep (CRITICAL)",
            "hibernate" to "Hibernate (CRITICAL)",
            "turn off" to "Turn off (CRITICAL)",
            // App termination
            "exit" to "Exit (CRITICAL)",
            "quit" to "Quit (CRITICAL)",
            "close app" to "Close app (CRITICAL)",
            "force stop" to "Force stop (CRITICAL)",
            "force close" to "Force close (CRITICAL)",
            // Session termination
            "sign out" to "Sign out",
            "signout" to "Sign out",
            "log out" to "Log out",
            "logout" to "Log out",
            "sign-out" to "Sign out",
            "log-out" to "Log out",
            // Destructive account actions
            "delete account" to "Delete account",
            "deactivate account" to "Deactivate account",
            "remove account" to "Remove account",
            "close account" to "Close account",
            // Factory/system reset
            "factory reset" to "Factory reset (CRITICAL)",
            "wipe data" to "Wipe data (CRITICAL)",
            "erase all" to "Erase all (CRITICAL)",
            "format" to "Format (CRITICAL)",
            // Call/meeting actions
            "call" to "Call (CRITICAL)",
            "make a call" to "Make call (CRITICAL)",
            "make call" to "Make call (CRITICAL)",
            "start call" to "Start call (CRITICAL)",
            "audio call" to "Audio call (CRITICAL)",
            "video call" to "Video call (CRITICAL)",
            "dial" to "Dial (CRITICAL)",
            "answer" to "Answer (CRITICAL)",
            "join call" to "Join call (CRITICAL)",
            "join meeting" to "Join meeting (CRITICAL)",
            "new meeting" to "New meeting (CRITICAL)",
            "schedule meeting" to "Schedule meeting (CRITICAL)",
            "instant meeting" to "Instant meeting (CRITICAL)",
            "meet now" to "Meet now (CRITICAL)",
            "call_control" to "Call control (CRITICAL)",
            "call_end" to "End call (CRITICAL)",
            "calls_call" to "Call item (CRITICAL)",
            // Reply actions
            "reply" to "Reply (sends message)"
        )
    }
}
