/**
 * ContextMatcher.kt - Context matching and scoring system
 * Matches commands to contexts with relevance scoring
 *
 * Created: 2025-10-09 12:37:32 PDT
 * Part of Week 4 - Context-Aware Commands implementation
 */

package com.augmentalis.voiceoscore.managers.commandmanager.context

import com.augmentalis.voiceoscore.CommandDefinition

/**
 * Context matcher for matching commands to contexts
 * Calculates match scores from 0.0 (no match) to 1.0 (perfect match)
 */
class ContextMatcher {

    companion object {
        private const val TAG = "ContextMatcher"

        // Scoring weights
        private const val WEIGHT_APP_EXACT = 1.0f
        private const val WEIGHT_APP_CATEGORY = 0.7f
        private const val WEIGHT_SCREEN = 0.8f
        private const val WEIGHT_TIME = 0.6f
        private const val WEIGHT_LOCATION = 0.7f
        private const val WEIGHT_ACTIVITY = 0.7f

        // Match thresholds
        private const val MIN_MATCH_SCORE = 0.3f
        private const val HIGH_MATCH_SCORE = 0.7f
    }

    /**
     * Check if a command is available in the given context
     * A command is available if its match score is above the minimum threshold
     *
     * @param command Command to check
     * @param currentContext Current context to match against
     * @param rules Optional context rules to apply
     * @return True if command is available, false otherwise
     */
    fun isCommandAvailable(
        command: CommandDefinition,
        currentContext: CommandContext,
        rules: List<ContextRule> = emptyList()
    ): Boolean {
        // If command has no context requirements, it's always available
        if (command.requiredContext.isEmpty()) {
            return true
        }

        // Calculate match score
        val score = calculateMatchScore(command, currentContext)

        // Check if score meets minimum threshold
        if (score < MIN_MATCH_SCORE) {
            return false
        }

        // Apply context rules
        for (rule in rules) {
            if (!rule.evaluate(command, currentContext)) {
                android.util.Log.v(TAG, "Command ${command.id} blocked by rule: ${rule.id}")
                return false
            }
        }

        return true
    }

    /**
     * Calculate context match score for a command
     * Returns a score from 0.0 (no match) to 1.0 (perfect match)
     *
     * @param command Command to score
     * @param currentContext Current context
     * @return Match score (0.0 to 1.0)
     */
    fun calculateMatchScore(command: CommandDefinition, currentContext: CommandContext): Float {
        // Flatten composite contexts
        val contexts = if (currentContext is CommandContext.Composite) {
            currentContext.flatten()
        } else {
            listOf(currentContext)
        }

        var totalScore = 0f
        var weightSum = 0f

        // Score each context type
        for (context in contexts) {
            val (score, weight) = when (context) {
                is CommandContext.App -> scoreAppContext(command, context)
                is CommandContext.Screen -> scoreScreenContext(command, context)
                is CommandContext.Time -> scoreTimeContext(command, context)
                is CommandContext.Location -> scoreLocationContext(command, context)
                is CommandContext.Activity -> scoreActivityContext(command, context)
                is CommandContext.Composite -> continue // Already flattened
            }

            totalScore += score * weight
            weightSum += weight
        }

        // Return normalized score
        return if (weightSum > 0) {
            (totalScore / weightSum).coerceIn(0f, 1f)
        } else {
            // No context requirements = always available with neutral score
            0.5f
        }
    }

    /**
     * Score app context match
     * Returns (score, weight) pair
     */
    private fun scoreAppContext(command: CommandDefinition, appContext: CommandContext.App): Pair<Float, Float> {
        // Check for exact package match in required context
        val packageRequirement = "package:${appContext.packageName}"
        if (packageRequirement in command.requiredContext) {
            return Pair(1.0f, WEIGHT_APP_EXACT)
        }

        // Check for category match
        val categoryRequirement = "category:${appContext.appCategory.name.lowercase()}"
        if (categoryRequirement in command.requiredContext) {
            return Pair(0.8f, WEIGHT_APP_CATEGORY)
        }

        // Check for any app requirement
        if ("any_app" in command.requiredContext) {
            return Pair(0.5f, WEIGHT_APP_EXACT)
        }

        // No app context requirement
        return Pair(0f, 0f)
    }

    /**
     * Score screen context match
     * Returns (score, weight) pair
     */
    private fun scoreScreenContext(command: CommandDefinition, screenContext: CommandContext.Screen): Pair<Float, Float> {
        var score = 0f
        var matches = 0

        // Check for screen ID match
        val screenRequirement = "screen:${screenContext.screenId}"
        if (screenRequirement in command.requiredContext) {
            score += 1.0f
            matches++
        }

        // Check for editable field requirement
        if ("editable_field" in command.requiredContext) {
            score += if (screenContext.hasEditableFields) 1.0f else 0f
            matches++
        }

        // Check for scrollable content requirement
        if ("scrollable" in command.requiredContext) {
            score += if (screenContext.hasScrollableContent) 1.0f else 0f
            matches++
        }

        // Check for clickable elements requirement
        if ("clickable" in command.requiredContext) {
            score += if (screenContext.hasClickableElements) 1.0f else 0f
            matches++
        }

        // Return average score if any matches
        return if (matches > 0) {
            Pair(score / matches, WEIGHT_SCREEN)
        } else {
            Pair(0f, 0f)
        }
    }

    /**
     * Score time context match
     * Returns (score, weight) pair
     */
    private fun scoreTimeContext(command: CommandDefinition, timeContext: CommandContext.Time): Pair<Float, Float> {
        var score = 0f
        var matches = 0

        // Check for time of day match
        val timeOfDayRequirement = "time:${timeContext.timeOfDay.name.lowercase()}"
        if (timeOfDayRequirement in command.requiredContext) {
            score += 1.0f
            matches++
        }

        // Check for weekday/weekend match
        if ("weekday" in command.requiredContext) {
            score += if (timeContext.isWeekday()) 1.0f else 0f
            matches++
        }

        if ("weekend" in command.requiredContext) {
            score += if (timeContext.isWeekend()) 1.0f else 0f
            matches++
        }

        // Check for time range matches
        for (requirement in command.requiredContext) {
            if (requirement.startsWith("time_range:")) {
                val range = parseTimeRange(requirement)
                if (range != null && timeContext.isInRange(range)) {
                    score += 1.0f
                    matches++
                }
            }
        }

        return if (matches > 0) {
            Pair(score / matches, WEIGHT_TIME)
        } else {
            Pair(0f, 0f)
        }
    }

    /**
     * Score location context match
     * Returns (score, weight) pair
     */
    private fun scoreLocationContext(command: CommandDefinition, locationContext: CommandContext.Location): Pair<Float, Float> {
        val locationRequirement = "location:${locationContext.type.name.lowercase()}"
        if (locationRequirement in command.requiredContext) {
            // Weight by confidence
            val confidenceAdjustedScore = 1.0f * locationContext.confidence
            return Pair(confidenceAdjustedScore, WEIGHT_LOCATION)
        }

        return Pair(0f, 0f)
    }

    /**
     * Score activity context match
     * Returns (score, weight) pair
     */
    private fun scoreActivityContext(command: CommandDefinition, activityContext: CommandContext.Activity): Pair<Float, Float> {
        val activityRequirement = "activity:${activityContext.type.name.lowercase()}"
        if (activityRequirement in command.requiredContext) {
            // Weight by confidence
            val confidenceAdjustedScore = 1.0f * activityContext.confidence
            return Pair(confidenceAdjustedScore, WEIGHT_ACTIVITY)
        }

        return Pair(0f, 0f)
    }

    /**
     * Parse time range from requirement string
     * Format: "time_range:HH-HH" (e.g., "time_range:9-17")
     */
    private fun parseTimeRange(requirement: String): TimeRange? {
        return try {
            val rangeStr = requirement.removePrefix("time_range:")
            val parts = rangeStr.split("-")
            if (parts.size == 2) {
                val startHour = parts[0].toInt()
                val endHour = parts[1].toInt()
                TimeRange(startHour, endHour)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to parse time range: $requirement", e)
            null
        }
    }

    /**
     * Find best matching commands for current context
     * Returns commands sorted by match score (highest first)
     *
     * @param commands List of all available commands
     * @param currentContext Current context
     * @param rules Optional context rules to apply
     * @param limit Maximum number of commands to return
     * @return List of (command, score) pairs sorted by score
     */
    fun findBestMatches(
        commands: List<CommandDefinition>,
        currentContext: CommandContext,
        rules: List<ContextRule> = emptyList(),
        limit: Int = 10
    ): List<Pair<CommandDefinition, Float>> {
        val matches = mutableListOf<Pair<CommandDefinition, Float>>()

        for (command in commands) {
            if (isCommandAvailable(command, currentContext, rules)) {
                val score = calculateMatchScore(command, currentContext)
                if (score >= MIN_MATCH_SCORE) {
                    matches.add(Pair(command, score))
                }
            }
        }

        // Sort by score (descending) and limit results
        return matches
            .sortedByDescending { it.second }
            .take(limit)
    }

    /**
     * Check if a context matches a wildcard pattern
     * Supports patterns like "com.google.*", "any_app", "any_time"
     *
     * @param pattern Wildcard pattern
     * @param context Context to match
     * @return True if matches, false otherwise
     */
    fun matchesWildcard(pattern: String, context: CommandContext): Boolean {
        return when {
            pattern == "any_app" && context is CommandContext.App -> true
            pattern == "any_screen" && context is CommandContext.Screen -> true
            pattern == "any_time" && context is CommandContext.Time -> true
            pattern == "any_location" && context is CommandContext.Location -> true
            pattern == "any_activity" && context is CommandContext.Activity -> true

            pattern.startsWith("package:") && context is CommandContext.App -> {
                val packagePattern = pattern.removePrefix("package:")
                if (packagePattern.endsWith("*")) {
                    // Wildcard match (e.g., "com.google.*")
                    val prefix = packagePattern.removeSuffix("*")
                    context.packageName.startsWith(prefix)
                } else {
                    // Exact match
                    context.packageName == packagePattern
                }
            }

            pattern.startsWith("category:") && context is CommandContext.App -> {
                val category = pattern.removePrefix("category:")
                context.appCategory.name.equals(category, ignoreCase = true)
            }

            else -> false
        }
    }

    /**
     * Check if context hierarchy matches
     * Supports hierarchical matching: specific > category > wildcard
     *
     * @param command Command with context requirements
     * @param currentContext Current context
     * @return Match level (0 = no match, 1 = wildcard, 2 = category, 3 = specific)
     */
    fun getMatchLevel(command: CommandDefinition, currentContext: CommandContext): Int {
        val contexts = if (currentContext is CommandContext.Composite) {
            currentContext.flatten()
        } else {
            listOf(currentContext)
        }

        var maxLevel = 0

        for (context in contexts) {
            when (context) {
                is CommandContext.App -> {
                    // Check for specific package match (level 3)
                    if ("package:${context.packageName}" in command.requiredContext) {
                        maxLevel = maxOf(maxLevel, 3)
                    }
                    // Check for category match (level 2)
                    else if ("category:${context.appCategory.name.lowercase()}" in command.requiredContext) {
                        maxLevel = maxOf(maxLevel, 2)
                    }
                    // Check for wildcard match (level 1)
                    else if ("any_app" in command.requiredContext) {
                        maxLevel = maxOf(maxLevel, 1)
                    }
                }

                is CommandContext.Screen -> {
                    if ("screen:${context.screenId}" in command.requiredContext) {
                        maxLevel = maxOf(maxLevel, 3)
                    } else if ("any_screen" in command.requiredContext) {
                        maxLevel = maxOf(maxLevel, 1)
                    }
                }

                is CommandContext.Time -> {
                    if ("time:${context.timeOfDay.name.lowercase()}" in command.requiredContext) {
                        maxLevel = maxOf(maxLevel, 3)
                    } else if ("any_time" in command.requiredContext) {
                        maxLevel = maxOf(maxLevel, 1)
                    }
                }

                is CommandContext.Location -> {
                    if ("location:${context.type.name.lowercase()}" in command.requiredContext) {
                        maxLevel = maxOf(maxLevel, 3)
                    } else if ("any_location" in command.requiredContext) {
                        maxLevel = maxOf(maxLevel, 1)
                    }
                }

                is CommandContext.Activity -> {
                    if ("activity:${context.type.name.lowercase()}" in command.requiredContext) {
                        maxLevel = maxOf(maxLevel, 3)
                    } else if ("any_activity" in command.requiredContext) {
                        maxLevel = maxOf(maxLevel, 1)
                    }
                }

                is CommandContext.Composite -> continue
            }
        }

        return maxLevel
    }
}
