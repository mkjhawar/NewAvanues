/**
 * SemanticInferenceHelper.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Provides semantic analysis for UI elements and voice commands.
 * Uses heuristics and pattern matching to infer intent and context.
 */
package com.augmentalis.voiceoscore.learnapp.ai

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Helper for semantic inference and context analysis
 */
class SemanticInferenceHelper {

    companion object {
        private const val TAG = "SemanticInferenceHelper"

        // Common UI patterns and their intents
        private val INTENT_PATTERNS = mapOf(
            "login" to listOf("sign in", "log in", "login", "authentication"),
            "signup" to listOf("sign up", "register", "create account", "join"),
            "submit" to listOf("submit", "send", "confirm", "ok", "done"),
            "cancel" to listOf("cancel", "dismiss", "close", "back"),
            "search" to listOf("search", "find", "query", "lookup"),
            "filter" to listOf("filter", "sort", "refine"),
            "settings" to listOf("settings", "preferences", "options", "configure"),
            "help" to listOf("help", "support", "faq", "about")
        )

        // Scoring weights for different matching strategies
        private const val EXACT_MATCH_SCORE = 1.0f
        private const val PARTIAL_MATCH_SCORE = 0.7f
        private const val CONTEXT_MATCH_SCORE = 0.5f
        private const val SEMANTIC_MATCH_SCORE = 0.6f
    }

    /**
     * Infer the intent of a UI element based on its properties
     *
     * @param text Element text content
     * @param contentDescription Element content description
     * @param className Element class name
     * @param resourceId Element resource ID
     * @return Inferred intent or null if cannot determine
     */
    fun inferIntent(
        text: String?,
        contentDescription: String?,
        className: String?,
        resourceId: String?
    ): String? {
        val combinedText = listOfNotNull(text, contentDescription, resourceId)
            .joinToString(" ")
            .lowercase()

        if (combinedText.isEmpty()) {
            return null
        }

        // Try to match against known intent patterns
        INTENT_PATTERNS.forEach { (intent, patterns) ->
            patterns.forEach { pattern ->
                if (combinedText.contains(pattern)) {
                    Log.d(TAG, "Inferred intent '$intent' from pattern '$pattern'")
                    return intent
                }
            }
        }

        // Try to infer from class name
        className?.let { cls ->
            when {
                cls.contains("Button") -> return "action"
                cls.contains("EditText") -> return "input"
                cls.contains("CheckBox") || cls.contains("Switch") -> return "toggle"
                cls.contains("Spinner") || cls.contains("Picker") -> return "select"
                else -> Unit // No inference from class name
            }
        }

        return null
    }

    /**
     * Analyze the context of a UI element within its hierarchy
     *
     * @param node The accessibility node to analyze
     * @return Context analysis result
     */
    fun analyzeContext(node: AccessibilityNodeInfo?): ContextAnalysis {
        if (node == null) {
            return ContextAnalysis(
                isInForm = false,
                isInDialog = false,
                isInList = false,
                hierarchyDepth = 0,
                siblingCount = 0
            )
        }

        var isInForm = false
        var isInDialog = false
        var isInList = false
        var depth = 0
        var siblingCount = 0

        // Traverse up the hierarchy to gather context
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            depth++

            current.className?.toString()?.let { className ->
                when {
                    className.contains("Dialog") -> isInDialog = true
                    className.contains("ListView") || className.contains("RecyclerView") -> isInList = true
                    className.contains("EditText") -> isInForm = true
                    else -> Unit // No context from this class name
                }
            }

            val parent = current.parent
            if (parent != null && current == node) {
                siblingCount = parent.childCount
            }

            current.recycle()
            current = parent
        }

        return ContextAnalysis(
            isInForm = isInForm,
            isInDialog = isInDialog,
            isInList = isInList,
            hierarchyDepth = depth,
            siblingCount = siblingCount
        )
    }

    /**
     * Score the match between a voice command and element properties
     *
     * @param voiceCommand The voice command to match
     * @param elementText Element text content
     * @param elementDescription Element content description
     * @param elementId Element resource ID
     * @return Match score from 0.0 (no match) to 1.0 (perfect match)
     */
    fun scoreMatch(
        voiceCommand: String,
        elementText: String?,
        elementDescription: String?,
        elementId: String?
    ): Float {
        val normalizedCommand = voiceCommand.trim().lowercase()
        if (normalizedCommand.isEmpty()) {
            return 0.0f
        }

        var maxScore = 0.0f

        // Check exact match with text
        elementText?.let { text ->
            val normalizedText = text.trim().lowercase()
            when {
                normalizedText == normalizedCommand -> {
                    maxScore = maxScore.coerceAtLeast(EXACT_MATCH_SCORE)
                }
                normalizedText.contains(normalizedCommand) || normalizedCommand.contains(normalizedText) -> {
                    maxScore = maxScore.coerceAtLeast(PARTIAL_MATCH_SCORE)
                }
            }
        }

        // Check match with content description
        elementDescription?.let { desc ->
            val normalizedDesc = desc.trim().lowercase()
            when {
                normalizedDesc == normalizedCommand -> {
                    maxScore = maxScore.coerceAtLeast(EXACT_MATCH_SCORE)
                }
                normalizedDesc.contains(normalizedCommand) || normalizedCommand.contains(normalizedDesc) -> {
                    maxScore = maxScore.coerceAtLeast(PARTIAL_MATCH_SCORE)
                }
            }
        }

        // Check match with resource ID
        elementId?.let { id ->
            val idParts = id.split("/").lastOrNull()?.split("_") ?: emptyList()
            idParts.forEach { part ->
                if (normalizedCommand.contains(part.lowercase()) || part.lowercase().contains(normalizedCommand)) {
                    maxScore = maxScore.coerceAtLeast(CONTEXT_MATCH_SCORE)
                }
            }
        }

        // Check semantic similarity (simple word matching)
        val commandWords = normalizedCommand.split(" ")
        val allWords = listOfNotNull(
            elementText?.lowercase(),
            elementDescription?.lowercase()
        ).flatMap { it.split(" ") }

        val matchingWords = commandWords.count { cmdWord ->
            allWords.any { word -> word.contains(cmdWord) || cmdWord.contains(word) }
        }

        if (matchingWords > 0) {
            val semanticScore = (matchingWords.toFloat() / commandWords.size) * SEMANTIC_MATCH_SCORE
            maxScore = maxScore.coerceAtLeast(semanticScore)
        }

        return maxScore
    }

    /**
     * Infer the likely function/purpose of an element based on multiple signals
     *
     * @param node The accessibility node to analyze
     * @return ElementFunction describing the likely purpose
     */
    fun inferElementFunction(node: AccessibilityNodeInfo): ElementFunction {
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        val className = node.className?.toString()
        val resourceId = node.viewIdResourceName

        // Determine if it's actionable
        val isActionable = node.isClickable || node.isLongClickable || node.isFocusable

        // Infer intent
        val intent = inferIntent(text, desc, className, resourceId)

        // Analyze context
        val context = analyzeContext(node)

        return ElementFunction(
            intent = intent,
            isActionable = isActionable,
            context = context,
            suggestedCommandType = when {
                className?.contains("Button") == true -> "tap"
                className?.contains("EditText") == true -> "type"
                className?.contains("Switch") == true || className?.contains("CheckBox") == true -> "toggle"
                isActionable -> "interact"
                else -> "read"
            }
        )
    }
}

/**
 * Result of context analysis for a UI element
 */
data class ContextAnalysis(
    val isInForm: Boolean,
    val isInDialog: Boolean,
    val isInList: Boolean,
    val hierarchyDepth: Int,
    val siblingCount: Int
)

/**
 * Inferred function/purpose of a UI element
 */
data class ElementFunction(
    val intent: String?,
    val isActionable: Boolean,
    val context: ContextAnalysis,
    val suggestedCommandType: String
)
