/**
 * DangerousElementDetector.kt - Detects dangerous UI elements
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/elements/DangerousElementDetector.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Detects dangerous elements that should be skipped during exploration
 */

package com.augmentalis.voiceoscore.learnapp.elements

import com.augmentalis.voiceoscore.learnapp.models.ElementInfo

/**
 * Dangerous Element Detector
 *
 * Detects UI elements that should NOT be clicked during exploration.
 * Uses pattern matching on text, contentDescription, and resourceId.
 *
 * ## Dangerous Patterns
 *
 * - Account deletion (delete account, remove account, close account)
 * - Sign out / logout
 * - Purchases / payments (buy now, checkout, payment, purchase)
 * - Data deletion (delete all, clear data, reset)
 * - Sending / sharing (send message, post, share, publish)
 *
 * ## Usage Example
 *
 * ```kotlin
 * val detector = DangerousElementDetector()
 *
 * val element = ElementInfo(text = "Delete Account")
 * val (isDangerous, reason) = detector.isDangerous(element)
 *
 * if (isDangerous) {
 *     println("Skipping: $reason")
 * }
 * ```
 *
 * @since 1.0.0
 */
class DangerousElementDetector {

    /**
     * Check if element is dangerous
     *
     * @param element Element to check
     * @return Pair of (isDangerous, reason)
     */
    fun isDangerous(element: ElementInfo): Pair<Boolean, String> {
        // Check text
        val textResult = checkText(element.text)
        if (textResult.first) {
            return textResult
        }

        // Check content description
        val descResult = checkText(element.contentDescription)
        if (descResult.first) {
            return descResult
        }

        // Check resource ID
        val resourceResult = checkResourceId(element.resourceId)
        if (resourceResult.first) {
            return resourceResult
        }

        return Pair(false, "")
    }

    /**
     * Check text for dangerous patterns
     *
     * @param text Text to check
     * @return Pair of (isDangerous, reason)
     */
    private fun checkText(text: String): Pair<Boolean, String> {
        if (text.isBlank()) {
            return Pair(false, "")
        }

        val lowerText = text.lowercase()

        // Check each dangerous pattern
        for ((pattern, reason) in DANGEROUS_TEXT_PATTERNS) {
            if (pattern.containsMatchIn(lowerText)) {
                return Pair(true, reason)
            }
        }

        return Pair(false, "")
    }

    /**
     * Check resource ID for dangerous patterns
     *
     * @param resourceId Resource ID to check
     * @return Pair of (isDangerous, reason)
     */
    private fun checkResourceId(resourceId: String): Pair<Boolean, String> {
        if (resourceId.isBlank()) {
            return Pair(false, "")
        }

        val lowerResourceId = resourceId.lowercase()

        // Extract ID part (after last '/')
        val idPart = lowerResourceId.substringAfterLast('/')

        // Check each dangerous resource ID pattern
        for ((keyword, reason) in DANGEROUS_RESOURCE_IDS) {
            if (keyword in idPart) {
                return Pair(true, reason)
            }
        }

        return Pair(false, "")
    }

    /**
     * Get all dangerous patterns (for debugging)
     *
     * @return List of pattern descriptions
     */
    fun getAllPatterns(): List<String> {
        return DANGEROUS_TEXT_PATTERNS.map { it.second } +
               DANGEROUS_RESOURCE_IDS.map { it.second }
    }

    companion object {
        /**
         * Dangerous text patterns (regex + reason)
         */
        private val DANGEROUS_TEXT_PATTERNS = listOf(
            // CRITICAL: System power actions (2025-12-05)
            Regex("power\\s*off", RegexOption.IGNORE_CASE) to "Power off (CRITICAL)",
            Regex("shut\\s*down", RegexOption.IGNORE_CASE) to "Shutdown (CRITICAL)",
            Regex("restart", RegexOption.IGNORE_CASE) to "Restart (CRITICAL)",
            Regex("reboot", RegexOption.IGNORE_CASE) to "Reboot (CRITICAL)",
            Regex("sleep", RegexOption.IGNORE_CASE) to "Sleep (CRITICAL)",
            Regex("hibernate", RegexOption.IGNORE_CASE) to "Hibernate (CRITICAL)",
            Regex("turn\\s*off", RegexOption.IGNORE_CASE) to "Turn off (CRITICAL)",
            Regex("power\\s*down", RegexOption.IGNORE_CASE) to "Power down (CRITICAL)",

            // CRITICAL: App termination (2025-12-05)
            Regex("^exit$", RegexOption.IGNORE_CASE) to "Exit (CRITICAL)",
            Regex("^quit$", RegexOption.IGNORE_CASE) to "Quit (CRITICAL)",
            Regex("force\\s*stop", RegexOption.IGNORE_CASE) to "Force stop (CRITICAL)",
            Regex("force\\s*close", RegexOption.IGNORE_CASE) to "Force close (CRITICAL)",

            // CRITICAL: Communication actions (2025-12-05)
            Regex("audio\\s*call", RegexOption.IGNORE_CASE) to "Audio call (CRITICAL)",
            Regex("video\\s*call", RegexOption.IGNORE_CASE) to "Video call (CRITICAL)",
            Regex("make\\s*call", RegexOption.IGNORE_CASE) to "Make call (CRITICAL)",
            Regex("start\\s*call", RegexOption.IGNORE_CASE) to "Start call (CRITICAL)",
            Regex("dial", RegexOption.IGNORE_CASE) to "Dial (CRITICAL)",
            Regex("call\\s*now", RegexOption.IGNORE_CASE) to "Call now (CRITICAL)",

            // CRITICAL: Audio/microphone settings (2025-12-05)
            Regex("^microphone$", RegexOption.IGNORE_CASE) to "Microphone (CRITICAL)",
            Regex("wired\\s*microphone", RegexOption.IGNORE_CASE) to "Wired Microphone (CRITICAL)",
            Regex("^dictation$", RegexOption.IGNORE_CASE) to "Dictation (CRITICAL)",
            Regex("voice\\s*recording", RegexOption.IGNORE_CASE) to "Voice recording (CRITICAL)",
            Regex("record\\s*audio", RegexOption.IGNORE_CASE) to "Record audio (CRITICAL)",

            // CRITICAL: Admin/role actions (2025-12-05)
            Regex("demote", RegexOption.IGNORE_CASE) to "Demote (CRITICAL)",
            Regex("promote", RegexOption.IGNORE_CASE) to "Promote (CRITICAL)",
            Regex("remove.*member", RegexOption.IGNORE_CASE) to "Remove member (CRITICAL)",
            Regex("add.*owner", RegexOption.IGNORE_CASE) to "Add owner (CRITICAL)",
            Regex("remove.*owner", RegexOption.IGNORE_CASE) to "Remove owner (CRITICAL)",
            Regex("change.*role", RegexOption.IGNORE_CASE) to "Change role (CRITICAL)",
            Regex("make.*admin", RegexOption.IGNORE_CASE) to "Make admin (CRITICAL)",
            Regex("remove.*admin", RegexOption.IGNORE_CASE) to "Remove admin (CRITICAL)",

            // Account deletion
            Regex("delete.*account", RegexOption.IGNORE_CASE) to "Delete account",
            Regex("remove.*account", RegexOption.IGNORE_CASE) to "Remove account",
            Regex("close.*account", RegexOption.IGNORE_CASE) to "Close account",
            Regex("deactivate.*account", RegexOption.IGNORE_CASE) to "Deactivate account",

            // Sign out / logout
            Regex("sign\\s*out", RegexOption.IGNORE_CASE) to "Sign out",
            Regex("log\\s*out", RegexOption.IGNORE_CASE) to "Log out",
            Regex("logout", RegexOption.IGNORE_CASE) to "Logout",

            // Purchases / payments
            Regex("purchase", RegexOption.IGNORE_CASE) to "Purchase",
            Regex("buy\\s*now", RegexOption.IGNORE_CASE) to "Buy now",
            Regex("checkout", RegexOption.IGNORE_CASE) to "Checkout",
            Regex("payment", RegexOption.IGNORE_CASE) to "Payment",
            Regex("confirm\\s*order", RegexOption.IGNORE_CASE) to "Confirm order",
            Regex("pay\\s*\\$", RegexOption.IGNORE_CASE) to "Payment",
            Regex("subscribe", RegexOption.IGNORE_CASE) to "Subscription",

            // Data deletion
            Regex("delete\\s*all", RegexOption.IGNORE_CASE) to "Delete all",
            Regex("clear\\s*data", RegexOption.IGNORE_CASE) to "Clear data",
            Regex("reset", RegexOption.IGNORE_CASE) to "Reset",
            Regex("erase", RegexOption.IGNORE_CASE) to "Erase",

            // Sending / sharing / posting
            Regex("send\\s*message", RegexOption.IGNORE_CASE) to "Send message",
            Regex("send\\s*email", RegexOption.IGNORE_CASE) to "Send email",
            Regex("post", RegexOption.IGNORE_CASE) to "Post",
            Regex("share", RegexOption.IGNORE_CASE) to "Share",
            Regex("publish", RegexOption.IGNORE_CASE) to "Publish",
            Regex("tweet", RegexOption.IGNORE_CASE) to "Tweet",

            // Permissions / access
            Regex("grant.*permission", RegexOption.IGNORE_CASE) to "Grant permission",
            Regex("allow.*access", RegexOption.IGNORE_CASE) to "Allow access",

            // Dangerous settings
            Regex("factory\\s*reset", RegexOption.IGNORE_CASE) to "Factory reset",
            Regex("uninstall", RegexOption.IGNORE_CASE) to "Uninstall",
            Regex("disable", RegexOption.IGNORE_CASE) to "Disable",

            // Financial
            Regex("transfer", RegexOption.IGNORE_CASE) to "Transfer",
            Regex("withdraw", RegexOption.IGNORE_CASE) to "Withdraw",
            Regex("donate", RegexOption.IGNORE_CASE) to "Donate",

            // Download actions (2025-12-07) - skip click but generate UUID/command
            Regex("download", RegexOption.IGNORE_CASE) to "Download"
        )

        /**
         * Dangerous resource ID keywords (keyword + reason)
         */
        private val DANGEROUS_RESOURCE_IDS = listOf(
            // CRITICAL: System/app termination (2025-12-05)
            "poweroff" to "Power off (CRITICAL)",
            "shutdown" to "Shutdown (CRITICAL)",
            "restart" to "Restart (CRITICAL)",
            "reboot" to "Reboot (CRITICAL)",
            "sleep" to "Sleep (CRITICAL)",
            "power_down" to "Power down (CRITICAL)",
            "powerdown" to "Power down (CRITICAL)",
            "exit" to "Exit (CRITICAL)",
            "quit" to "Quit (CRITICAL)",
            "force_stop" to "Force stop (CRITICAL)",
            "force_close" to "Force close (CRITICAL)",
            // Communication actions (2025-12-05)
            "audio_call" to "Audio call (CRITICAL)",
            "video_call" to "Video call (CRITICAL)",
            "audiocall" to "Audio call (CRITICAL)",
            "videocall" to "Video call (CRITICAL)",
            "make_call" to "Make call (CRITICAL)",
            "start_call" to "Start call (CRITICAL)",
            "dial" to "Dial (CRITICAL)",
            // Audio/microphone settings (2025-12-05)
            "microphone" to "Microphone (CRITICAL)",
            "dictation" to "Dictation (CRITICAL)",
            "wired_microphone" to "Wired Microphone (CRITICAL)",
            "record_audio" to "Record audio (CRITICAL)",
            // Admin actions (2025-12-05)
            "demote" to "Demote (CRITICAL)",
            "promote" to "Promote (CRITICAL)",
            "change_role" to "Change role (CRITICAL)",
            "remove_member" to "Remove member (CRITICAL)",
            "add_owner" to "Add owner (CRITICAL)",
            "remove_owner" to "Remove owner (CRITICAL)",
            "make_admin" to "Make admin (CRITICAL)",
            // Standard dangerous actions
            "delete" to "Delete action",
            "remove" to "Remove action",
            "logout" to "Logout action",
            "signout" to "Sign out action",
            "sign_out" to "Sign out action",
            "log_out" to "Log out action",
            "purchase" to "Purchase action",
            "buy" to "Buy action",
            "checkout" to "Checkout action",
            "payment" to "Payment action",
            "send" to "Send action",
            "post" to "Post action",
            "share" to "Share action",
            "publish" to "Publish action",
            "reset" to "Reset action",
            "clear" to "Clear action",
            "erase" to "Erase action",
            "uninstall" to "Uninstall action",
            // Download actions (2025-12-07) - skip click but generate UUID/command
            "download" to "Download action"
        )
    }
}
