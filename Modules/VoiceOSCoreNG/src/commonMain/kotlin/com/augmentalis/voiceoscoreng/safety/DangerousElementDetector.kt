/**
 * DangerousElementDetector.kt - Safety detection for UI elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * KMP migration from VoiceOSCore.
 * Detects potentially dangerous UI elements that require confirmation before click.
 */
package com.augmentalis.voiceoscoreng.safety

/**
 * Detects dangerous UI elements that require user confirmation.
 *
 * Dangerous elements include:
 * - Delete/Remove actions
 * - Purchase/Payment actions
 * - Account/Security modifications
 * - Financial transactions
 * - Download actions (skip click but generate command)
 *
 * Usage:
 * ```kotlin
 * val detector = DangerousElementDetector()
 * val result = detector.analyze(text, contentDescription)
 * if (result.isDangerous) {
 *     // Require confirmation before click
 * }
 * ```
 */
class DangerousElementDetector {

    /**
     * Result of danger analysis.
     */
    data class DetectionResult(
        val isDangerous: Boolean,
        val dangerType: DangerType?,
        val reason: String,
        val skipAutoClick: Boolean = false,
        val generateCommand: Boolean = true
    )

    /**
     * Types of dangerous actions.
     */
    enum class DangerType {
        DELETE,
        PURCHASE,
        SECURITY,
        FINANCIAL,
        DOWNLOAD,
        DESTRUCTIVE,
        PERMISSION,
        UNKNOWN
    }

    /**
     * Analyze element text for dangerous patterns.
     *
     * @param text Element text content
     * @param contentDescription Accessibility content description
     * @param resourceId Optional resource ID for context
     * @return DetectionResult with analysis
     */
    fun analyze(
        text: String?,
        contentDescription: String?,
        resourceId: String? = null
    ): DetectionResult {
        val combinedText = buildString {
            text?.let { append(it.lowercase()) }
            append(" ")
            contentDescription?.let { append(it.lowercase()) }
            append(" ")
            resourceId?.let { append(it.lowercase()) }
        }

        // Check patterns in priority order
        for ((pattern, info) in DANGEROUS_PATTERNS) {
            if (pattern.containsMatchIn(combinedText)) {
                return DetectionResult(
                    isDangerous = info.isDangerous,
                    dangerType = info.type,
                    reason = info.reason,
                    skipAutoClick = info.skipAutoClick,
                    generateCommand = info.generateCommand
                )
            }
        }

        return DetectionResult(
            isDangerous = false,
            dangerType = null,
            reason = "No dangerous patterns detected",
            skipAutoClick = false,
            generateCommand = true
        )
    }

    /**
     * Check if an action verb indicates danger.
     */
    fun isActionDangerous(action: String): Boolean {
        val lower = action.lowercase()
        return DANGEROUS_VERBS.any { lower.contains(it) }
    }

    /**
     * Get danger info for an action.
     */
    fun getDangerInfo(action: String): Pair<DangerType, String>? {
        val lower = action.lowercase()
        return ACTION_DANGER_INFO[lower]
    }

    companion object {
        /**
         * Pattern info for dangerous elements.
         */
        private data class PatternInfo(
            val type: DangerType,
            val reason: String,
            val isDangerous: Boolean = true,
            val skipAutoClick: Boolean = true,
            val generateCommand: Boolean = true
        )

        /**
         * Dangerous patterns with associated metadata.
         */
        private val DANGEROUS_PATTERNS: List<Pair<Regex, PatternInfo>> = listOf(
            // Delete/Remove patterns
            Regex("\\bdelete\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.DELETE,
                "Delete action"
            ),
            Regex("\\bremove\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.DELETE,
                "Remove action"
            ),
            Regex("\\bclear all\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.DELETE,
                "Clear all action"
            ),
            Regex("\\berase\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.DELETE,
                "Erase action"
            ),
            Regex("\\buninstall\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.DELETE,
                "Uninstall action"
            ),

            // Purchase/Payment patterns
            Regex("\\bbuy\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.PURCHASE,
                "Purchase action"
            ),
            Regex("\\bpurchase\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.PURCHASE,
                "Purchase action"
            ),
            Regex("\\bpay\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.PURCHASE,
                "Payment action"
            ),
            Regex("\\bsubscribe\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.PURCHASE,
                "Subscription action"
            ),
            Regex("\\bcheckout\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.PURCHASE,
                "Checkout action"
            ),
            Regex("\\$\\d+", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.PURCHASE,
                "Price detected"
            ),

            // Security patterns
            Regex("\\blogout\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.SECURITY,
                "Logout action"
            ),
            Regex("\\bsign out\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.SECURITY,
                "Sign out action"
            ),
            Regex("\\bchange password\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.SECURITY,
                "Password change"
            ),
            Regex("\\breset\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.SECURITY,
                "Reset action"
            ),
            Regex("\\bfactory reset\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.SECURITY,
                "Factory reset"
            ),

            // Financial patterns
            Regex("\\btransfer\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.FINANCIAL,
                "Transfer action"
            ),
            Regex("\\bwithdraw\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.FINANCIAL,
                "Withdraw action"
            ),
            Regex("\\bdonate\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.FINANCIAL,
                "Donate action"
            ),

            // Download actions (skip click but generate command)
            Regex("\\bdownload\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.DOWNLOAD,
                "Download action",
                isDangerous = true,
                skipAutoClick = true,
                generateCommand = true
            ),

            // Permission patterns
            Regex("\\ballow\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.PERMISSION,
                "Permission allow",
                isDangerous = true,
                skipAutoClick = true,
                generateCommand = true
            ),
            Regex("\\bgrant\\b", RegexOption.IGNORE_CASE) to PatternInfo(
                DangerType.PERMISSION,
                "Permission grant",
                isDangerous = true,
                skipAutoClick = true,
                generateCommand = true
            )
        )

        /**
         * Simple list of dangerous verbs for quick checks.
         */
        private val DANGEROUS_VERBS = setOf(
            "delete", "remove", "erase", "clear", "uninstall",
            "buy", "purchase", "pay", "subscribe", "checkout",
            "logout", "sign out", "reset",
            "transfer", "withdraw", "donate",
            "download", "allow", "grant"
        )

        /**
         * Action to danger type mapping.
         */
        private val ACTION_DANGER_INFO: Map<String, Pair<DangerType, String>> = mapOf(
            "delete" to (DangerType.DELETE to "Delete action"),
            "remove" to (DangerType.DELETE to "Remove action"),
            "erase" to (DangerType.DELETE to "Erase action"),
            "uninstall" to (DangerType.DELETE to "Uninstall action"),
            "clear" to (DangerType.DELETE to "Clear action"),
            "buy" to (DangerType.PURCHASE to "Purchase action"),
            "purchase" to (DangerType.PURCHASE to "Purchase action"),
            "pay" to (DangerType.PURCHASE to "Payment action"),
            "logout" to (DangerType.SECURITY to "Logout action"),
            "sign out" to (DangerType.SECURITY to "Sign out action"),
            "reset" to (DangerType.SECURITY to "Reset action"),
            "transfer" to (DangerType.FINANCIAL to "Transfer action"),
            "withdraw" to (DangerType.FINANCIAL to "Withdraw action"),
            "donate" to (DangerType.FINANCIAL to "Donate action"),
            "download" to (DangerType.DOWNLOAD to "Download action")
        )
    }
}
