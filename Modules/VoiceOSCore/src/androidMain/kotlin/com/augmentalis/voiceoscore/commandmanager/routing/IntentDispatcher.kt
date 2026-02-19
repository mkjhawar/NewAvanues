/**
 * IntentDispatcher.kt - Context-aware command routing with confidence scoring
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-10
 *
 * Smart dispatcher that routes commands based on intent analysis with confidence scoring
 * Based on Q4 Decision: Context-Aware Routing with Smart Dispatcher
 */
package com.augmentalis.voiceoscore.commandmanager.routing

import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscore.Command
import com.augmentalis.voiceoscore.ActionResult
import com.augmentalis.voiceoscore.CommandError
import com.augmentalis.voiceoscore.ErrorCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Intent-based command dispatcher with context awareness
 *
 * Features:
 * - Confidence scoring for handler selection
 * - Fallback chain: Primary → Context fallback → Generic
 * - User feedback loop for learning
 * - Analytics for fallback usage
 *
 * Q4 Enhancements:
 * - Confidence scoring algorithm
 * - User feedback tracking
 * - Context history integration
 * - Fallback analytics
 * - Dynamic fallback rules
 */
class IntentDispatcher(
    private val context: Context
) {
    companion object {
        private const val TAG = "IntentDispatcher"
        private const val MIN_CONFIDENCE_THRESHOLD = 0.3f
        private const val PREFS_NAME = "intent_dispatcher"
        private const val KEY_FALLBACK_ATTEMPTS = "fallback_attempts"
        private const val KEY_FALLBACK_SUCCESSES = "fallback_successes"
        private const val KEY_PRIMARY_SUCCESSES = "primary_successes"
        private const val KEY_ROUTING_RULES = "routing_rules"
        private const val MAX_FEEDBACK_HISTORY = 500 // Keep last 500 feedback entries
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Action registry (will be populated by CommandManager)
    // Using simplified structure for now - can be enhanced with BaseAction interface later
    private val actionHandlers = mutableMapOf<String, suspend (Command) -> ActionResult>()

    // Q4 Enhancement 2: User Feedback tracking
    private val feedbackDatabase = mutableListOf<UserFeedback>()

    // Q4 Enhancement 4: Fallback analytics (persisted)
    private var fallbackAttempts = prefs.getLong(KEY_FALLBACK_ATTEMPTS, 0L)
    private var fallbackSuccesses = prefs.getLong(KEY_FALLBACK_SUCCESSES, 0L)
    private var primarySuccesses = prefs.getLong(KEY_PRIMARY_SUCCESSES, 0L)

    // Q4 Enhancement 5: Dynamic routing rules (persisted)
    private val routingRules = mutableMapOf<String, RoutingRule>()
    private val failureThreshold = 0.7f // If failure rate > 70%, apply penalty
    private val minSampleSize = 5 // Minimum samples before applying rules

    init {
        loadRoutingRules()
    }

    /**
     * Register action handler for a category
     */
    fun registerHandler(category: String, handler: suspend (Command) -> ActionResult) {
        actionHandlers[category] = handler
        Log.d(TAG, "Registered handler for category: $category")
    }

    /**
     * Route command based on intent analysis
     * Q4 Enhancement 1: Confidence Scoring
     *
     * @param command Command to route
     * @param routingContext Current routing context (app, screen, etc.)
     * @return Command execution result
     */
    suspend fun routeCommand(command: Command, routingContext: RoutingContext): ActionResult {
        Log.d(TAG, "Routing command: ${command.id} in context: ${routingContext.currentApp}")

        // Find candidate handlers
        val candidates = findCandidateHandlers(command, routingContext)

        if (candidates.isEmpty()) {
            Log.w(TAG, "No handlers found for command: ${command.id}")
            return ActionResult(
                success = false,
                command = command,
                error = CommandError(ErrorCode.COMMAND_NOT_FOUND, "No handler found for command")
            )
        }

        // Score each candidate
        val scored = candidates.map { (category, handler) ->
            Triple(category, handler, calculateConfidence(category, command, routingContext))
        }.sortedByDescending { it.third }

        Log.d(TAG, "Scored ${scored.size} candidates for ${command.id}")

        // Try handlers in order of confidence
        for ((category, handler, confidence) in scored) {
            if (confidence < MIN_CONFIDENCE_THRESHOLD) {
                // Q4 Enhancement 4: Track fallback analytics
                trackFallbackAttempt(command, category, confidence, routingContext)
                Log.d(TAG, "Skipping handler $category: confidence too low ($confidence)")
                continue
            }

            try {
                Log.d(TAG, "Attempting handler $category with confidence $confidence")
                val result = handler(command)

                if (result.success) {
                    // Q4 Enhancement 2: User feedback loop
                    trackSuccess(command, category, routingContext, isPrimary = (scored.first().first == category))
                    Log.i(TAG, "Command executed successfully by $category")
                    return result
                } else {
                    Log.w(TAG, "Handler $category failed: ${result.error?.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Handler $category threw exception", e)
                continue // Try next handler
            }
        }

        // All handlers failed, use generic fallback
        fallbackAttempts++
        return useGenericFallback(command, routingContext)
    }

    /**
     * Find candidate handlers for a command
     */
    private fun findCandidateHandlers(
        @Suppress("UNUSED_PARAMETER") command: Command,
        @Suppress("UNUSED_PARAMETER") routingContext: RoutingContext
    ): List<Pair<String, suspend (Command) -> ActionResult>> {
        // For now, return all handlers that might handle this command
        // TODO: Implement more sophisticated matching based on command category, keywords, etc.

        return actionHandlers.entries.map { (category, handler) ->
            category to handler
        }
    }

    /**
     * Q4 Enhancement 1: Calculate confidence score (0.0-1.0)
     *
     * Scoring factors:
     * - Category match (40%)
     * - Current context match (30%)
     * - Historical success in this context (30%)
     */
    private fun calculateConfidence(
        category: String,
        command: Command,
        routingContext: RoutingContext
    ): Float {
        var confidence = 0f

        // Category match (simplified for now - command.id starts with category)
        if (command.id.startsWith(category, ignoreCase = true)) {
            confidence += 0.4f
        }

        // Current context match (check if handler supports this app)
        val currentApp = routingContext.currentApp
        if (currentApp != null && routingContext.hasAppContext()) {
            val supportsApp = handlerSupportsApp(category, currentApp)
            if (supportsApp) {
                confidence += 0.3f
            }
        } else {
            // No context, give partial credit for global handlers
            confidence += 0.15f
        }

        // Q4 Enhancement 3: Historical success in this context
        val historyScore = getContextHistory(category, routingContext)
        confidence += historyScore * 0.3f

        // Q4 Enhancement 5: Apply dynamic routing rules
        val ruleKey = "$category:${routingContext.currentApp ?: "global"}"
        routingRules[ruleKey]?.let { rule ->
            confidence *= rule.confidenceMultiplier
            Log.d(TAG, "Applied routing rule for $ruleKey: multiplier=${rule.confidenceMultiplier}")
        }

        return confidence.coerceIn(0f, 1f)
    }

    /**
     * Check if handler supports a specific app
     * TODO: Implement app-specific handler registry
     */
    private fun handlerSupportsApp(@Suppress("UNUSED_PARAMETER") category: String, @Suppress("UNUSED_PARAMETER") appPackage: String): Boolean {
        // For now, all handlers support all apps (global handlers)
        // This will be enhanced when app-specific handlers are implemented
        return true
    }

    /**
     * Q4 Enhancement 3: Context history tracking
     * Get success rate for this handler in this context
     */
    private fun getContextHistory(category: String, routingContext: RoutingContext): Float {
        if (!routingContext.hasAppContext()) return 0.5f // Neutral score for no context

        val appPackage = routingContext.currentApp ?: return 0.5f

        // Query feedback database for this handler's success rate in this app
        val relevantFeedback = feedbackDatabase.filter {
            it.handlerCategory == category && it.context.currentApp == appPackage
        }

        if (relevantFeedback.isEmpty()) return 0.5f // Neutral score for no history

        val successCount = relevantFeedback.count { it.successful }
        return successCount.toFloat() / relevantFeedback.size
    }

    /**
     * Q4 Enhancement 2: User Feedback Loop
     * Track successful command execution
     */
    private fun trackSuccess(
        command: Command,
        handlerCategory: String,
        routingContext: RoutingContext,
        isPrimary: Boolean
    ) {
        val feedback = UserFeedback(
            commandId = command.id,
            commandText = command.text,
            handlerCategory = handlerCategory,
            successful = true,
            context = routingContext,
            timestamp = System.currentTimeMillis()
        )

        feedbackDatabase.add(feedback)

        if (isPrimary) {
            primarySuccesses++
        } else {
            fallbackSuccesses++
        }

        Log.d(TAG, "Tracked success for $handlerCategory (primary=$isPrimary)")

        // Cleanup old feedback and persist periodically (every 10 commands)
        cleanupFeedbackHistory()
        if ((primarySuccesses + fallbackSuccesses) % 10 == 0L) {
            saveAnalytics()
        }
    }

    /**
     * Q4 Enhancement 4: Fallback Analytics
     * Track when fallback is attempted
     */
    private fun trackFallbackAttempt(
        @Suppress("UNUSED_PARAMETER") command: Command,
        handlerCategory: String,
        confidence: Float,
        @Suppress("UNUSED_PARAMETER") routingContext: RoutingContext
    ) {
        fallbackAttempts++

        Log.d(TAG, "Fallback attempt: $handlerCategory (confidence: $confidence)")

        // TODO: Export fallback metrics to analytics service
        // This helps identify commands that need better handlers
    }

    /**
     * Q4 Enhancement 2: Record user correction
     * When user explicitly corrects a command
     */
    fun recordUserCorrection(
        originalCommand: String,
        correctedCommand: String,
        context: RoutingContext
    ) {
        val feedback = UserFeedback(
            commandId = originalCommand,
            commandText = originalCommand,
            handlerCategory = "correction",
            successful = false,
            context = context,
            timestamp = System.currentTimeMillis(),
            correctedTo = correctedCommand
        )

        feedbackDatabase.add(feedback)

        Log.i(TAG, "User correction recorded: '$originalCommand' → '$correctedCommand'")

        // TODO: Persist to database when available
        // TODO: Use for ML model training
    }

    /**
     * Use generic fallback when all handlers fail
     */
    private fun useGenericFallback(command: Command, @Suppress("UNUSED_PARAMETER") routingContext: RoutingContext): ActionResult {
        Log.w(TAG, "Using generic fallback for command: ${command.id}")

        // Generic fallback: try to execute as global action
        // This would delegate to legacy command handling

        return ActionResult(
            success = false,
            command = command,
            error = CommandError(
                ErrorCode.EXECUTION_FAILED,
                "Command failed: no suitable handler found"
            )
        )
    }

    /**
     * Get routing analytics
     */
    fun getAnalytics(): RoutingAnalytics {
        val total = primarySuccesses + fallbackSuccesses + fallbackAttempts
        return RoutingAnalytics(
            primarySuccesses = primarySuccesses,
            fallbackSuccesses = fallbackSuccesses,
            fallbackAttempts = fallbackAttempts,
            primarySuccessRate = if (total > 0) primarySuccesses.toFloat() / total else 0f,
            fallbackSuccessRate = if (fallbackAttempts > 0) fallbackSuccesses.toFloat() / fallbackAttempts else 0f,
            totalCommands = total
        )
    }

    /**
     * Reset analytics
     */
    fun resetAnalytics() {
        primarySuccesses = 0
        fallbackSuccesses = 0
        fallbackAttempts = 0
        Log.d(TAG, "Routing analytics reset")
    }

    /**
     * Q4 Enhancement 5: Dynamic Fallback Rules
     * Analyze feedback patterns and adjust routing rules accordingly
     * e.g., if "click" fails in browser 90% of time, apply confidence penalty
     */
    fun updateRoutingRules() {
        Log.d(TAG, "Updating routing rules based on feedback patterns")

        // Group feedback by handler-context combinations
        val groupedFeedback = feedbackDatabase.groupBy { feedback ->
            "${feedback.handlerCategory}:${feedback.context.currentApp ?: "global"}"
        }

        var rulesCreated = 0
        var rulesUpdated = 0

        groupedFeedback.forEach { (key, feedbackList) ->
            // Only create rules with enough samples
            if (feedbackList.size < minSampleSize) return@forEach

            val failureCount = feedbackList.count { !it.successful }
            val failureRate = failureCount.toFloat() / feedbackList.size

            // Calculate confidence multiplier based on failure rate
            val multiplier = when {
                failureRate > 0.9f -> 0.1f  // 90%+ failure: severe penalty
                failureRate > failureThreshold -> 0.5f  // 70%+ failure: moderate penalty
                failureRate > 0.5f -> 0.8f  // 50%+ failure: slight penalty
                failureRate < 0.2f -> 1.2f  // <20% failure: boost confidence
                else -> 1.0f // Normal
            }

            val existingRule = routingRules[key]
            if (existingRule == null) {
                routingRules[key] = RoutingRule(
                    handlerCategory = key.substringBefore(":"),
                    appContext = key.substringAfter(":"),
                    confidenceMultiplier = multiplier,
                    failureRate = failureRate,
                    sampleSize = feedbackList.size,
                    lastUpdated = System.currentTimeMillis()
                )
                rulesCreated++
            } else if (existingRule.confidenceMultiplier != multiplier) {
                routingRules[key] = existingRule.copy(
                    confidenceMultiplier = multiplier,
                    failureRate = failureRate,
                    sampleSize = feedbackList.size,
                    lastUpdated = System.currentTimeMillis()
                )
                rulesUpdated++
            }

            if (multiplier < 1.0f) {
                Log.w(TAG, "Applied penalty to $key: failure rate ${(failureRate * 100).toInt()}%, multiplier $multiplier")
            } else if (multiplier > 1.0f) {
                Log.i(TAG, "Applied boost to $key: failure rate ${(failureRate * 100).toInt()}%, multiplier $multiplier")
            }
        }

        Log.i(TAG, "Routing rules updated: $rulesCreated created, $rulesUpdated updated, ${routingRules.size} total")

        // Persist updated rules
        saveRoutingRules()
    }

    /**
     * Get current routing rules for inspection
     */
    fun getRoutingRules(): Map<String, RoutingRule> = routingRules.toMap()

    /**
     * Clear all routing rules (reset to default behavior)
     */
    fun clearRoutingRules() {
        routingRules.clear()
        saveRoutingRules()
        Log.d(TAG, "Routing rules cleared")
    }

    /**
     * Get rules that have penalties applied
     */
    fun getPenalizedRoutes(): List<RoutingRule> {
        return routingRules.values.filter { it.confidenceMultiplier < 1.0f }
    }

    // ========== Persistence Methods ==========

    /**
     * Load routing rules from SharedPreferences
     */
    private fun loadRoutingRules() {
        try {
            val json = prefs.getString(KEY_ROUTING_RULES, null) ?: return
            val rulesArray = org.json.JSONArray(json)

            for (i in 0 until rulesArray.length()) {
                val obj = rulesArray.getJSONObject(i)
                val key = "${obj.getString("handlerCategory")}:${obj.getString("appContext")}"
                val rule = RoutingRule(
                    handlerCategory = obj.getString("handlerCategory"),
                    appContext = obj.getString("appContext"),
                    confidenceMultiplier = obj.getDouble("confidenceMultiplier").toFloat(),
                    failureRate = obj.getDouble("failureRate").toFloat(),
                    sampleSize = obj.getInt("sampleSize"),
                    lastUpdated = obj.getLong("lastUpdated")
                )
                routingRules[key] = rule
            }

            Log.d(TAG, "Loaded ${routingRules.size} routing rules from persistence")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load routing rules", e)
        }
    }

    /**
     * Save routing rules to SharedPreferences
     */
    private fun saveRoutingRules() {
        try {
            val rulesArray = org.json.JSONArray()
            for (rule in routingRules.values) {
                val obj = org.json.JSONObject().apply {
                    put("handlerCategory", rule.handlerCategory)
                    put("appContext", rule.appContext)
                    put("confidenceMultiplier", rule.confidenceMultiplier.toDouble())
                    put("failureRate", rule.failureRate.toDouble())
                    put("sampleSize", rule.sampleSize)
                    put("lastUpdated", rule.lastUpdated)
                }
                rulesArray.put(obj)
            }

            prefs.edit()
                .putString(KEY_ROUTING_RULES, rulesArray.toString())
                .apply()

            Log.d(TAG, "Saved ${routingRules.size} routing rules to persistence")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save routing rules", e)
        }
    }

    /**
     * Save analytics counters to SharedPreferences
     */
    private fun saveAnalytics() {
        prefs.edit()
            .putLong(KEY_FALLBACK_ATTEMPTS, fallbackAttempts)
            .putLong(KEY_FALLBACK_SUCCESSES, fallbackSuccesses)
            .putLong(KEY_PRIMARY_SUCCESSES, primarySuccesses)
            .apply()
    }

    /**
     * Cleanup old feedback entries to prevent unbounded growth
     */
    private fun cleanupFeedbackHistory() {
        if (feedbackDatabase.size > MAX_FEEDBACK_HISTORY) {
            val toRemove = feedbackDatabase.size - MAX_FEEDBACK_HISTORY
            repeat(toRemove) {
                feedbackDatabase.removeAt(0)
            }
            Log.d(TAG, "Cleaned up $toRemove old feedback entries")
        }
    }

    /**
     * Persist current state (call periodically or on app pause)
     */
    fun persistState() {
        saveRoutingRules()
        saveAnalytics()
        Log.d(TAG, "IntentDispatcher state persisted")
    }

    /**
     * Get current feedback database size
     */
    fun getFeedbackCount(): Int = feedbackDatabase.size
}

/**
 * Dynamic routing rule for handler-context combinations
 */
data class RoutingRule(
    val handlerCategory: String,
    val appContext: String,
    val confidenceMultiplier: Float,
    val failureRate: Float,
    val sampleSize: Int,
    val lastUpdated: Long
)

/**
 * User feedback data class
 */
data class UserFeedback(
    val commandId: String,
    val commandText: String,
    val handlerCategory: String,
    val successful: Boolean,
    val context: RoutingContext,
    val timestamp: Long,
    val correctedTo: String? = null
)

/**
 * Routing analytics data class
 */
data class RoutingAnalytics(
    val primarySuccesses: Long,
    val fallbackSuccesses: Long,
    val fallbackAttempts: Long,
    val primarySuccessRate: Float,
    val fallbackSuccessRate: Float,
    val totalCommands: Long
)
