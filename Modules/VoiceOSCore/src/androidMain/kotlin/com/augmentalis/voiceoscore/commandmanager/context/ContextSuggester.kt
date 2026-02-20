/**
 * ContextSuggester.kt - Context-aware command suggestion system
 * Suggests commands based on current context and learned preferences
 *
 * Created: 2025-10-09 12:37:32 PDT
 * Part of Week 4 - Context-Aware Commands implementation
 */

package com.augmentalis.voiceoscore.commandmanager.context

import com.augmentalis.voiceoscore.CommandDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Context suggester for intelligent command recommendations
 * Suggests top N commands for current context based on:
 * - Context matching scores
 * - Learned user preferences (Bayesian probability)
 * - Command availability filters
 * - Real-time context updates
 */
class ContextSuggester(
    private val contextDetector: ContextDetector,
    private val contextMatcher: ContextMatcher,
    private val preferenceLearner: PreferenceLearner,
    private val ruleRegistry: ContextRuleRegistry
) {

    companion object {
        private const val TAG = "ContextSuggester"
        private const val DEFAULT_SUGGESTION_LIMIT = 5
        private const val MIN_SUGGESTION_SCORE = 0.3f
        private const val REFRESH_INTERVAL_MS = 2000L // 2 seconds
    }

    // Current suggestions
    private val _suggestions = MutableStateFlow<List<CommandSuggestion>>(emptyList())
    val suggestions: Flow<List<CommandSuggestion>> = _suggestions.asStateFlow()

    // All available commands
    private var allCommands: List<CommandDefinition> = emptyList()

    // Last refresh time
    private var lastRefreshTime = 0L

    /**
     * Initialize suggester with available commands
     */
    fun initialize(commands: List<CommandDefinition>) {
        this.allCommands = commands
        android.util.Log.i(TAG, "ContextSuggester initialized with ${commands.size} commands")
    }

    /**
     * Update suggestions based on current context
     * Combines context matching and learned preferences
     *
     * @param currentContext Current context (if null, will detect automatically)
     * @param limit Maximum number of suggestions
     * @return List of command suggestions
     */
    suspend fun updateSuggestions(
        currentContext: CommandContext? = null,
        limit: Int = DEFAULT_SUGGESTION_LIMIT
    ): List<CommandSuggestion> {
        return withContext(Dispatchers.Default) {
            try {
                // Get current context
                val context = currentContext ?: contextDetector.detectCurrentContext()

                // Get available commands in this context
                val availableCommands = filterAvailableCommands(context)

                // Score each command
                val scoredCommands = scoreCommands(availableCommands, context)

                // Sort by score and limit
                val topSuggestions = scoredCommands
                    .filter { it.score >= MIN_SUGGESTION_SCORE }
                    .sortedByDescending { it.score }
                    .take(limit)

                // Update state
                _suggestions.value = topSuggestions
                lastRefreshTime = System.currentTimeMillis()

                android.util.Log.d(TAG, "Updated suggestions: ${topSuggestions.size} commands suggested")

                topSuggestions
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to update suggestions", e)
                emptyList()
            }
        }
    }

    /**
     * Get suggestions for specific context without updating state
     * Useful for what-if analysis
     *
     * @param context Context to get suggestions for
     * @param limit Maximum number of suggestions
     * @return List of command suggestions
     */
    suspend fun getSuggestionsForContext(
        context: CommandContext,
        limit: Int = DEFAULT_SUGGESTION_LIMIT
    ): List<CommandSuggestion> {
        return withContext(Dispatchers.Default) {
            try {
                val availableCommands = filterAvailableCommands(context)
                val scoredCommands = scoreCommands(availableCommands, context)

                scoredCommands
                    .filter { it.score >= MIN_SUGGESTION_SCORE }
                    .sortedByDescending { it.score }
                    .take(limit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to get suggestions for context", e)
                emptyList()
            }
        }
    }

    /**
     * Refresh suggestions if enough time has passed
     * Automatically updates based on refresh interval
     */
    suspend fun refreshIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
            updateSuggestions()
        }
    }

    /**
     * Get top suggestion for current context
     * Returns the single best command to suggest
     */
    suspend fun getTopSuggestion(currentContext: CommandContext? = null): CommandSuggestion? {
        val suggestions = updateSuggestions(currentContext, limit = 1)
        return suggestions.firstOrNull()
    }

    /**
     * Get suggestions by category
     * Filters suggestions to specific command category
     *
     * @param category Command category to filter by
     * @param currentContext Current context
     * @param limit Maximum number of suggestions
     * @return List of suggestions in category
     */
    suspend fun getSuggestionsByCategory(
        category: String,
        currentContext: CommandContext? = null,
        limit: Int = DEFAULT_SUGGESTION_LIMIT
    ): List<CommandSuggestion> {
        return withContext(Dispatchers.Default) {
            try {
                val context = currentContext ?: contextDetector.detectCurrentContext()
                val availableCommands = filterAvailableCommands(context)
                    .filter { it.category == category }

                val scoredCommands = scoreCommands(availableCommands, context)

                scoredCommands
                    .filter { it.score >= MIN_SUGGESTION_SCORE }
                    .sortedByDescending { it.score }
                    .take(limit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to get suggestions by category", e)
                emptyList()
            }
        }
    }

    /**
     * Get explanation for why a command was suggested
     * Useful for debugging and user transparency
     *
     * @param command Command to explain
     * @param currentContext Current context
     * @return Human-readable explanation
     */
    suspend fun getExplanation(
        command: CommandDefinition,
        currentContext: CommandContext? = null
    ): String {
        return withContext(Dispatchers.Default) {
            try {
                val context = currentContext ?: contextDetector.detectCurrentContext()

                val matchScore = contextMatcher.calculateMatchScore(command, context)
                val bayesianProb = preferenceLearner.calculateBayesianProbability(command, context)
                @Suppress("UNUSED_VARIABLE") val stats = preferenceLearner.getLearningStatistics()

                buildString {
                    append("Suggested because:\n")
                    append("- Context match: ${(matchScore * 100).toInt()}%\n")
                    append("- Usage probability: ${(bayesianProb * 100).toInt()}%\n")

                    // Explain context matches
                    val contexts = if (context is CommandContext.Composite) {
                        context.flatten()
                    } else {
                        listOf(context)
                    }

                    for (ctx in contexts) {
                        when (ctx) {
                            is CommandContext.App -> append("- You're using ${ctx.packageName}\n")
                            is CommandContext.Time -> append("- Current time: ${ctx.timeOfDay}\n")
                            is CommandContext.Location -> append("- Location: ${ctx.type}\n")
                            is CommandContext.Activity -> append("- Activity: ${ctx.type}\n")
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to get explanation", e)
                "Explanation unavailable"
            }
        }
    }

    /**
     * Filter commands available in current context
     * Applies context rules and availability checks
     */
    private fun filterAvailableCommands(context: CommandContext): List<CommandDefinition> {
        val rules = ruleRegistry.getAllRules()

        return allCommands.filter { command ->
            // Check if command is available in this context
            val isAvailable = contextMatcher.isCommandAvailable(command, context, rules)

            // Additional check with rule registry
            val passesRules = ruleRegistry.evaluateRules(command, context)

            isAvailable && passesRules
        }
    }

    /**
     * Score commands for current context
     * Combines context matching and learned preferences
     */
    private suspend fun scoreCommands(
        commands: List<CommandDefinition>,
        context: CommandContext
    ): List<CommandSuggestion> {
        return commands.map { command ->
            // Calculate context match score (0.0 to 1.0)
            val matchScore = contextMatcher.calculateMatchScore(command, context)

            // Calculate Bayesian probability from learned preferences (0.0 to 1.0)
            val bayesianProb = preferenceLearner.calculateBayesianProbability(command, context)

            // Combine scores with weighting
            // 60% context match, 40% learned preference
            val combinedScore = (matchScore * 0.6f) + (bayesianProb * 0.4f)

            // Get match level for ranking
            val matchLevel = contextMatcher.getMatchLevel(command, context)

            CommandSuggestion(
                command = command,
                score = combinedScore,
                matchScore = matchScore,
                learningScore = bayesianProb,
                matchLevel = matchLevel,
                reason = generateReason(command, context, matchScore, bayesianProb)
            )
        }
    }

    /**
     * Generate human-readable reason for suggestion
     */
    private fun generateReason(
        @Suppress("UNUSED_PARAMETER") command: CommandDefinition,
        @Suppress("UNUSED_PARAMETER") context: CommandContext,
        matchScore: Float,
        learningScore: Float
    ): String {
        return when {
            matchScore > 0.8f && learningScore > 0.8f ->
                "Frequently used in this context"

            matchScore > 0.8f ->
                "Highly relevant to current context"

            learningScore > 0.8f ->
                "Based on your usage patterns"

            matchScore > 0.5f ->
                "Available in current context"

            else ->
                "May be useful"
        }
    }

    /**
     * Clear suggestions
     */
    fun clearSuggestions() {
        _suggestions.value = emptyList()
        android.util.Log.d(TAG, "Cleared suggestions")
    }

    /**
     * Get statistics about suggestions
     */
    fun getSuggestionStatistics(): SuggestionStatistics {
        val currentSuggestions = _suggestions.value

        return SuggestionStatistics(
            totalSuggestions = currentSuggestions.size,
            averageScore = currentSuggestions.map { it.score }.average().toFloat(),
            highConfidenceSuggestions = currentSuggestions.count { it.score > 0.7f },
            mediumConfidenceSuggestions = currentSuggestions.count { it.score in 0.4f..0.7f },
            lowConfidenceSuggestions = currentSuggestions.count { it.score < 0.4f }
        )
    }
}

/**
 * Command suggestion data class
 * Contains command and scoring information
 */
data class CommandSuggestion(
    val command: CommandDefinition,
    val score: Float,
    val matchScore: Float,
    val learningScore: Float,
    val matchLevel: Int,
    val reason: String
) {
    /**
     * Check if suggestion is high confidence
     */
    fun isHighConfidence(): Boolean {
        return score > 0.7f
    }

    /**
     * Check if suggestion is medium confidence
     */
    fun isMediumConfidence(): Boolean {
        return score in 0.4f..0.7f
    }

    /**
     * Check if suggestion is low confidence
     */
    fun isLowConfidence(): Boolean {
        return score < 0.4f
    }

    /**
     * Get confidence level as string
     */
    fun getConfidenceLevel(): ConfidenceLevel {
        return when {
            isHighConfidence() -> ConfidenceLevel.HIGH
            isMediumConfidence() -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
    }
}

/**
 * Confidence level enum
 */
enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Suggestion statistics
 */
data class SuggestionStatistics(
    val totalSuggestions: Int,
    val averageScore: Float,
    val highConfidenceSuggestions: Int,
    val mediumConfidenceSuggestions: Int,
    val lowConfidenceSuggestions: Int
)

/**
 * Suggestion filter for advanced filtering
 */
data class SuggestionFilter(
    val minScore: Float = 0.3f,
    val maxSuggestions: Int = 5,
    val categories: List<String> = emptyList(),
    val excludeCommands: List<String> = emptyList(),
    val requireHighConfidence: Boolean = false
) {
    /**
     * Apply filter to suggestions
     */
    fun apply(suggestions: List<CommandSuggestion>): List<CommandSuggestion> {
        var filtered = suggestions

        // Filter by minimum score
        filtered = filtered.filter { it.score >= minScore }

        // Filter by categories if specified
        if (categories.isNotEmpty()) {
            filtered = filtered.filter { it.command.category in categories }
        }

        // Exclude specific commands
        if (excludeCommands.isNotEmpty()) {
            filtered = filtered.filter { it.command.id !in excludeCommands }
        }

        // Filter by confidence if required
        if (requireHighConfidence) {
            filtered = filtered.filter { it.isHighConfidence() }
        }

        // Limit results
        return filtered.take(maxSuggestions)
    }
}
