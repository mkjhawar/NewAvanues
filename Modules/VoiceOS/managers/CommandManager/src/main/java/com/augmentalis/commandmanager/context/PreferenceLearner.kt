/**
 * PreferenceLearner.kt - Machine learning for command preferences
 * Uses Bayesian probability to learn user preferences and adapt command priorities
 *
 * Created: 2025-10-09 12:37:32 PDT
 * Part of Week 4 - Context-Aware Commands implementation
 *
 * MIGRATED TO SQLDELIGHT - Uses VoiceOSDatabaseManager
 */

package com.augmentalis.commandmanager.context

import android.content.Context
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.repositories.CommandStats
import com.augmentalis.database.repositories.ICommandUsageRepository
import com.augmentalis.database.repositories.IContextPreferenceRepository
import com.augmentalis.commandmanager.CommandDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Preference learner using Bayesian probability
 * Learns user preferences and adapts command priorities based on usage patterns
 *
 * Algorithm:
 * - Success Rate = successful_executions / total_executions
 * - Context Relevance = usage_in_context / total_usage
 * - Priority Adjustment = base_priority + (success_rate * 10) + (context_relevance * 10)
 * - Bayesian Update: P(command | context) = P(context | command) * P(command) / P(context)
 */
class PreferenceLearner(
    private val context: Context,
    private val commandUsageRepository: ICommandUsageRepository,
    private val contextPreferenceRepository: IContextPreferenceRepository
) {

    companion object {
        private const val TAG = "PreferenceLearner"

        /**
         * Convenience factory method
         */
        fun create(context: Context): PreferenceLearner {
            val databaseManager = VoiceOSDatabaseManager.getInstance(DatabaseDriverFactory(context))
            return PreferenceLearner(
                context,
                databaseManager.commandUsage,
                databaseManager.contextPreferences
            )
        }

        // Learning parameters
        private const val MIN_SAMPLES_FOR_LEARNING = 5 // Minimum usage before learning kicks in
        private const val LEARNING_RATE = 0.1f // How quickly to adapt (0.0 to 1.0)
        private const val PRIORITY_WEIGHT_SUCCESS = 10f // Weight for success rate
        private const val PRIORITY_WEIGHT_RELEVANCE = 10f // Weight for context relevance
        private const val MAX_PRIORITY_ADJUSTMENT = 30f // Maximum priority boost

        // Confidence thresholds
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.8f
        private const val LOW_CONFIDENCE_THRESHOLD = 0.3f

        // Decay factor for old data (exponential decay)
        private const val TIME_DECAY_FACTOR = 0.001f // Decay per day
    }

    // In-memory cache for performance (using mutable version)
    private val commandStatsCache = mutableMapOf<String, MutableCommandStats>()
    private val contextPreferenceCache = mutableMapOf<String, MutableMap<String, ContextPreference>>()

    /**
     * Record successful command execution
     * Updates learning statistics
     *
     * @param command Command that was executed
     * @param commandContext Context in which it was executed
     */
    suspend fun recordSuccess(command: CommandDefinition, commandContext: CommandContext) {
        withContext(Dispatchers.IO) {
            try {
                val contextKey = generateContextKey(commandContext)
                val timestamp = System.currentTimeMillis()

                // Record usage via repository
                commandUsageRepository.recordUsage(
                    commandId = command.id,
                    locale = java.util.Locale.getDefault().toLanguageTag(),
                    timestamp = timestamp,
                    userInput = "",  // Not tracked in this path
                    matchType = "SUCCESS",
                    success = true,
                    executionTimeMs = 0,
                    contextApp = when (commandContext) {
                        is CommandContext.App -> commandContext.packageName
                        else -> null
                    }
                )

                // Update cache
                updateCommandStatsCache(command.id, success = true)
                updateContextPreferenceCache(command.id, contextKey, success = true)

                android.util.Log.v(TAG, "Recorded success: ${command.id} in context $contextKey")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to record success", e)
            }
        }
    }

    /**
     * Record failed command execution
     * Updates learning statistics
     *
     * @param command Command that failed
     * @param context Context in which it failed
     */
    suspend fun recordFailure(command: CommandDefinition, context: CommandContext) {
        withContext(Dispatchers.IO) {
            try {
                val contextKey = generateContextKey(context)
                val timestamp = System.currentTimeMillis()

                // Record usage via repository
                commandUsageRepository.recordUsage(
                    commandId = command.id,
                    locale = java.util.Locale.getDefault().toLanguageTag(),
                    timestamp = timestamp,
                    userInput = "",  // Not tracked in this path
                    matchType = "FAILURE",
                    success = false,
                    executionTimeMs = 0,
                    contextApp = when (context) {
                        is CommandContext.App -> context.packageName
                        else -> null
                    }
                )

                // Update cache
                updateCommandStatsCache(command.id, success = false)
                updateContextPreferenceCache(command.id, contextKey, success = false)

                android.util.Log.v(TAG, "Recorded failure: ${command.id} in context $contextKey")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to record failure", e)
            }
        }
    }

    /**
     * Calculate adjusted priority for command based on learning
     * Uses success rate and context relevance
     *
     * @param command Command to calculate priority for
     * @param context Current context
     * @param basePriority Base priority of command (0-100)
     * @return Adjusted priority (0-100)
     */
    suspend fun calculateAdjustedPriority(
        command: CommandDefinition,
        context: CommandContext,
        basePriority: Int
    ): Int {
        return withContext(Dispatchers.Default) {
            try {
                val stats = getCommandStats(command.id)

                // Not enough data for learning yet
                if (stats.totalExecutions < MIN_SAMPLES_FOR_LEARNING) {
                    return@withContext basePriority
                }

                // Calculate success rate
                val successRate = stats.successfulExecutions.toFloat() / stats.totalExecutions

                // Calculate context relevance
                val contextKey = generateContextKey(context)
                val contextRelevance = calculateContextRelevance(command.id, contextKey)

                // Calculate priority adjustment
                val adjustment = (successRate * PRIORITY_WEIGHT_SUCCESS) +
                                (contextRelevance * PRIORITY_WEIGHT_RELEVANCE)

                // Apply learning rate and cap adjustment
                val cappedAdjustment = min(adjustment * LEARNING_RATE, MAX_PRIORITY_ADJUSTMENT)

                // Return adjusted priority, clamped to 0-100
                val adjustedPriority = (basePriority + cappedAdjustment).toInt()
                adjustedPriority.coerceIn(0, 100)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to calculate adjusted priority", e)
                basePriority
            }
        }
    }

    /**
     * Calculate Bayesian probability: P(command | context)
     * Formula: P(command | context) = P(context | command) * P(command) / P(context)
     *
     * @param command Command to calculate probability for
     * @param context Context to evaluate
     * @return Probability (0.0 to 1.0)
     */
    suspend fun calculateBayesianProbability(
        command: CommandDefinition,
        context: CommandContext
    ): Float {
        return withContext(Dispatchers.Default) {
            try {
                val contextKey = generateContextKey(context)

                // Get statistics
                val commandStats = getCommandStats(command.id)
                val contextPreference = getContextPreference(command.id, contextKey)

                // Not enough data
                if (commandStats.totalExecutions < MIN_SAMPLES_FOR_LEARNING) {
                    return@withContext 0.5f // Neutral probability
                }

                // P(command) = prior probability of command being used
                val priorCommandProb = calculatePriorCommandProbability(command.id)

                // P(context | command) = likelihood of this context given command was used
                val likelihood = if (commandStats.totalExecutions > 0) {
                    contextPreference.usageCount.toFloat() / commandStats.totalExecutions
                } else {
                    0f
                }

                // P(context) = probability of this context occurring
                val contextProb = calculateContextProbability(contextKey)

                // Bayesian update: P(command | context) = P(context | command) * P(command) / P(context)
                val posterior = if (contextProb > 0) {
                    (likelihood * priorCommandProb) / contextProb
                } else {
                    priorCommandProb
                }

                // Clamp to valid probability range
                posterior.coerceIn(0f, 1f)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to calculate Bayesian probability", e)
                0.5f
            }
        }
    }

    /**
     * Suggest commands for current context
     * Returns top N commands ranked by learned preferences
     *
     * @param context Current context
     * @param allCommands All available commands
     * @param limit Maximum number of suggestions
     * @return List of (command, score) pairs sorted by score
     */
    suspend fun suggestCommands(
        context: CommandContext,
        allCommands: List<CommandDefinition>,
        limit: Int = 5
    ): List<Pair<CommandDefinition, Float>> {
        return withContext(Dispatchers.Default) {
            try {
                val suggestions = mutableListOf<Pair<CommandDefinition, Float>>()

                for (command in allCommands) {
                    // Calculate suggestion score using Bayesian probability
                    val probability = calculateBayesianProbability(command, context)

                    // Only suggest if probability is above threshold
                    if (probability >= LOW_CONFIDENCE_THRESHOLD) {
                        suggestions.add(Pair(command, probability))
                    }
                }

                // Sort by probability (descending) and limit results
                suggestions
                    .sortedByDescending { it.second }
                    .take(limit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to suggest commands", e)
                emptyList()
            }
        }
    }

    /**
     * Update command priorities based on learned preferences
     * Applies learning to adjust command priorities system-wide
     */
    suspend fun updatePriorities() {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.i(TAG, "Updating command priorities based on learning...")

                // Get all commands with sufficient usage data
                val commandsWithStats = commandStatsCache.entries
                    .filter { it.value.totalExecutions >= MIN_SAMPLES_FOR_LEARNING }

                android.util.Log.d(TAG, "Updating priorities for ${commandsWithStats.size} commands")

                // Note: In a full implementation, this would update a persistent command registry
                // For now, we just log the potential updates
                for ((commandId, stats) in commandsWithStats) {
                    val successRate = stats.successfulExecutions.toFloat() / stats.totalExecutions
                    val priorityBoost = (successRate * PRIORITY_WEIGHT_SUCCESS * LEARNING_RATE).toInt()

                    android.util.Log.v(TAG, "Command $commandId: success rate=$successRate, boost=$priorityBoost")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to update priorities", e)
            }
        }
    }

    /**
     * Get command statistics
     * Returns cached stats or loads from database
     */
    private suspend fun getCommandStats(commandId: String): CommandStats {
        // Check cache first
        commandStatsCache[commandId]?.let {
            return CommandStats(
                totalExecutions = it.totalExecutions,
                successfulExecutions = it.successfulExecutions,
                failedExecutions = it.failedExecutions
            )
        }

        // Load from repository
        return withContext(Dispatchers.IO) {
            val stats = commandUsageRepository.getStatsForCommand(commandId)
            commandStatsCache[commandId] = MutableCommandStats(
                totalExecutions = stats.totalExecutions,
                successfulExecutions = stats.successfulExecutions,
                failedExecutions = stats.failedExecutions
            )
            stats
        }
    }

    /**
     * Get context preference for command
     * Returns cached preference or loads from database
     */
    private suspend fun getContextPreference(commandId: String, contextKey: String): ContextPreference {
        // Check cache first
        contextPreferenceCache[commandId]?.get(contextKey)?.let { return it }

        // Load from repository
        return withContext(Dispatchers.IO) {
            val preferenceDTO = contextPreferenceRepository.get(commandId, contextKey)
            val preference = if (preferenceDTO != null) {
                ContextPreference(
                    usageCount = preferenceDTO.usageCount.toInt(),
                    successCount = preferenceDTO.successCount.toInt()
                )
            } else {
                ContextPreference()
            }
            contextPreferenceCache.getOrPut(commandId) { mutableMapOf() }[contextKey] = preference
            preference
        }
    }

    /**
     * Update command stats cache
     */
    private fun updateCommandStatsCache(commandId: String, success: Boolean) {
        val stats = commandStatsCache.getOrPut(commandId) { MutableCommandStats() }
        stats.totalExecutions++
        if (success) {
            stats.successfulExecutions++
        } else {
            stats.failedExecutions++
        }
    }

    /**
     * Update context preference cache
     */
    private fun updateContextPreferenceCache(commandId: String, contextKey: String, success: Boolean) {
        val preferences = contextPreferenceCache.getOrPut(commandId) { mutableMapOf() }
        val preference = preferences.getOrPut(contextKey) { ContextPreference() }
        preference.usageCount++
        if (success) {
            preference.successCount++
        }
    }

    /**
     * Calculate prior probability of command being used
     * P(command) = command_usage / total_usage
     */
    private suspend fun calculatePriorCommandProbability(commandId: String): Float {
        return withContext(Dispatchers.IO) {
            val commandUsage = commandUsageRepository.countForCommand(commandId)
            val totalUsage = commandUsageRepository.countTotal()

            if (totalUsage > 0) {
                commandUsage.toFloat() / totalUsage
            } else {
                0.5f // Neutral prior
            }
        }
    }

    /**
     * Calculate probability of context occurring
     * P(context) = context_occurrences / total_occurrences
     */
    private suspend fun calculateContextProbability(contextKey: String): Float {
        return withContext(Dispatchers.IO) {
            val contextOccurrences = commandUsageRepository.countForContext(contextKey)
            val totalOccurrences = commandUsageRepository.countTotal()

            if (totalOccurrences > 0) {
                contextOccurrences.toFloat() / totalOccurrences
            } else {
                0.5f // Neutral prior
            }
        }
    }

    /**
     * Calculate context relevance for command
     * Returns how often command is used in this context vs other contexts
     */
    private suspend fun calculateContextRelevance(commandId: String, contextKey: String): Float {
        return withContext(Dispatchers.IO) {
            val contextUsage = commandUsageRepository.getForCommandInContext(commandId, contextKey).size.toLong()
            val totalUsage = commandUsageRepository.countForCommand(commandId)

            if (totalUsage > 0) {
                contextUsage.toFloat() / totalUsage
            } else {
                0f
            }
        }
    }

    /**
     * Generate context key for hashing
     * Converts CommandContext to a string key for database storage
     */
    private fun generateContextKey(context: CommandContext): String {
        return when (context) {
            is CommandContext.App -> "app:${context.packageName}"
            is CommandContext.Screen -> "screen:${context.screenId}"
            is CommandContext.Time -> "time:${context.timeOfDay}"
            is CommandContext.Location -> "location:${context.type}"
            is CommandContext.Activity -> "activity:${context.type}"
            is CommandContext.Composite -> {
                // Generate composite key from all contexts
                context.flatten().joinToString("|") { generateContextKey(it) }
            }
        }
    }

    /**
     * Apply time decay to old statistics
     * Older data has less weight in learning
     */
    suspend fun applyTimeDecay() {
        withContext(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                commandUsageRepository.applyTimeDecay(currentTime, TIME_DECAY_FACTOR)
                contextPreferenceRepository.applyTimeDecay(currentTime, TIME_DECAY_FACTOR)
                android.util.Log.d(TAG, "Applied time decay to learning data")

                // Clear cache to force reload with decayed values
                commandStatsCache.clear()
                contextPreferenceCache.clear()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to apply time decay", e)
            }
        }
    }

    /**
     * Clear all learning data
     * Useful for debugging or user-requested reset
     */
    suspend fun clearLearningData() {
        withContext(Dispatchers.IO) {
            try {
                commandUsageRepository.deleteAll()
                contextPreferenceRepository.deleteAll()
                commandStatsCache.clear()
                contextPreferenceCache.clear()
                android.util.Log.i(TAG, "Cleared all learning data")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to clear learning data", e)
            }
        }
    }

    /**
     * Get learning statistics summary
     * Returns overall learning metrics
     */
    suspend fun getLearningStatistics(): LearningStatistics {
        return withContext(Dispatchers.IO) {
            try {
                LearningStatistics(
                    totalCommands = contextPreferenceRepository.countCommands().toInt(),
                    totalExecutions = commandUsageRepository.countTotal().toInt(),
                    totalContexts = contextPreferenceRepository.countContexts().toInt(),
                    averageSuccessRate = contextPreferenceRepository.getAverageSuccessRate().toFloat(),
                    mostUsedCommands = contextPreferenceRepository.getMostUsedCommands(10).map {
                        it.first to it.second.toInt()
                    },
                    mostUsedContexts = contextPreferenceRepository.getMostUsedContexts(10).map {
                        it.first to it.second.toInt()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to get learning statistics", e)
                LearningStatistics()
            }
        }
    }
}

/**
 * Mutable version of CommandStats for caching
 */
data class MutableCommandStats(
    var totalExecutions: Int = 0,
    var successfulExecutions: Int = 0,
    var failedExecutions: Int = 0
) {
    val successRate: Float
        get() = if (totalExecutions > 0) {
            successfulExecutions.toFloat() / totalExecutions
        } else {
            0f
        }
}

/**
 * Context preference data class
 */
data class ContextPreference(
    var usageCount: Int = 0,
    var successCount: Int = 0
) {
    val successRate: Float
        get() = if (usageCount > 0) {
            successCount.toFloat() / usageCount
        } else {
            0f
        }
}

/**
 * Learning statistics summary
 */
data class LearningStatistics(
    val totalCommands: Int = 0,
    val totalExecutions: Int = 0,
    val totalContexts: Int = 0,
    val averageSuccessRate: Float = 0f,
    val mostUsedCommands: List<Pair<String, Int>> = emptyList(),
    val mostUsedContexts: List<Pair<String, Int>> = emptyList()
)
