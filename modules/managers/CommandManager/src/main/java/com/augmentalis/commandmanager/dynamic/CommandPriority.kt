/**
 * CommandPriority.kt - Priority scoring and resolution system
 *
 * Part of Week 4 implementation - Dynamic Command Registration
 * Provides priority-based command resolution and conflict resolution
 *
 * Priority Scale:
 * - 1-25: Low priority (user-defined, experimental)
 * - 26-50: Normal priority (standard user commands)
 * - 51-75: High priority (frequently used, app-specific)
 * - 76-100: Critical priority (system-level, safety-critical)
 *
 * @since VOS4 Week 4
 * @author VOS4 Development Team
 */

package com.augmentalis.commandmanager.dynamic

import kotlin.math.abs

/**
 * Priority management and scoring system
 *
 * Provides:
 * - Priority-based command resolution
 * - Conflict detection based on priority overlap
 * - Automatic priority adjustment
 * - Priority recommendation based on usage patterns
 *
 * Thread-safe: All operations are stateless or use immutable data
 */
object CommandPriority {

    // Priority ranges
    const val PRIORITY_MIN = 1
    const val PRIORITY_MAX = 100
    const val PRIORITY_DEFAULT = 50

    // Priority level thresholds
    const val LOW_THRESHOLD = 25
    const val NORMAL_THRESHOLD = 50
    const val HIGH_THRESHOLD = 75

    // Conflict detection
    const val DEFAULT_CONFLICT_TOLERANCE = 5  // Commands within 5 priority points may conflict

    /**
     * Validate priority value
     *
     * @param priority The priority to validate
     * @return true if valid, false otherwise
     */
    fun isValid(priority: Int): Boolean {
        return priority in PRIORITY_MIN..PRIORITY_MAX
    }

    /**
     * Normalize priority to valid range
     *
     * @param priority The priority to normalize
     * @return Clamped priority value
     */
    fun normalize(priority: Int): Int {
        return priority.coerceIn(PRIORITY_MIN, PRIORITY_MAX)
    }

    /**
     * Get priority level from priority value
     *
     * @param priority The priority value
     * @return Corresponding priority level
     */
    fun getLevel(priority: Int): PriorityLevel {
        return when (priority) {
            in PRIORITY_MIN..LOW_THRESHOLD -> PriorityLevel.LOW
            in (LOW_THRESHOLD + 1)..NORMAL_THRESHOLD -> PriorityLevel.NORMAL
            in (NORMAL_THRESHOLD + 1)..HIGH_THRESHOLD -> PriorityLevel.HIGH
            in (HIGH_THRESHOLD + 1)..PRIORITY_MAX -> PriorityLevel.CRITICAL
            else -> PriorityLevel.NORMAL // Fallback
        }
    }

    /**
     * Get default priority for a category
     *
     * @param category The command category
     * @return Recommended default priority
     */
    fun getDefaultForCategory(category: CommandCategory): Int {
        return when (category) {
            CommandCategory.SYSTEM -> 80  // System commands are high priority
            CommandCategory.ACCESSIBILITY -> 85  // Accessibility is critical
            CommandCategory.NAVIGATION -> 70  // Navigation is important
            CommandCategory.TEXT_EDITING -> 60  // Text editing is high-normal
            CommandCategory.APP_CONTROL -> 55  // App control is normal-high
            CommandCategory.MEDIA -> 50  // Media is normal
            CommandCategory.VOICE -> 50  // Voice commands are normal
            CommandCategory.GESTURE -> 45  // Gestures are normal-low
            CommandCategory.CUSTOM -> 30  // Custom commands are low-normal
        }
    }

    /**
     * Resolve command priority conflicts
     *
     * Given multiple commands matching the same phrase, select the highest priority one
     *
     * @param commands List of commands to resolve
     * @return The command with highest priority, or null if list is empty
     */
    fun resolveConflict(commands: List<VoiceCommand>): VoiceCommand? {
        if (commands.isEmpty()) return null
        if (commands.size == 1) return commands.first()

        // Sort by priority (highest first), then by usage count
        return commands
            .filter { it.enabled }
            .sortedWith(
                compareByDescending<VoiceCommand> { it.priority }
                    .thenByDescending { it.usageCount }
                    .thenBy { it.createdAt } // Prefer older commands if all else equal
            )
            .firstOrNull()
    }

    /**
     * Resolve command conflicts with disambiguation
     *
     * Returns top N commands for user selection if priorities are too close
     *
     * @param commands List of commands to resolve
     * @param tolerance Priority difference tolerance for disambiguation
     * @param maxResults Maximum number of results to return
     * @return List of commands that should be presented for disambiguation
     */
    fun resolveWithDisambiguation(
        commands: List<VoiceCommand>,
        tolerance: Int = DEFAULT_CONFLICT_TOLERANCE,
        maxResults: Int = 3
    ): List<VoiceCommand> {
        if (commands.isEmpty()) return emptyList()
        if (commands.size == 1) return commands

        // Sort by priority and usage
        val sorted = commands
            .filter { it.enabled }
            .sortedWith(
                compareByDescending<VoiceCommand> { it.priority }
                    .thenByDescending { it.usageCount }
            )

        if (sorted.isEmpty()) return emptyList()

        val topCommand = sorted.first()
        val topPriority = topCommand.priority

        // Find all commands within tolerance of top priority
        val candidates = sorted.filter {
            abs(it.priority - topPriority) <= tolerance
        }

        return candidates.take(maxResults)
    }

    /**
     * Calculate priority score for a command based on multiple factors
     *
     * Factors considered:
     * - Base priority
     * - Usage count (more usage = higher effective priority)
     * - Recency (recently used = higher effective priority)
     * - Category importance
     *
     * @param command The command to score
     * @param usageWeight Weight for usage count factor (0.0-1.0)
     * @param recencyWeight Weight for recency factor (0.0-1.0)
     * @return Effective priority score (1-100)
     */
    fun calculateEffectivePriority(
        command: VoiceCommand,
        usageWeight: Float = 0.2f,
        recencyWeight: Float = 0.1f
    ): Float {
        var score = command.priority.toFloat()

        // Usage boost (up to +10 points based on usage)
        if (usageWeight > 0f && command.usageCount > 0) {
            val usageBoost = (minOf(command.usageCount, 100L) / 10f) * usageWeight
            score += usageBoost
        }

        // Recency boost (up to +5 points if used recently)
        if (recencyWeight > 0f && command.lastUsed > 0) {
            val hoursSinceLastUse = (System.currentTimeMillis() - command.lastUsed) / (1000f * 60f * 60f)
            val recencyBoost = if (hoursSinceLastUse < 24f) {
                (5f * (1f - hoursSinceLastUse / 24f)) * recencyWeight
            } else {
                0f
            }
            score += recencyBoost
        }

        return score.coerceIn(PRIORITY_MIN.toFloat(), PRIORITY_MAX.toFloat())
    }

    /**
     * Suggest priority adjustment based on usage patterns
     *
     * @param command The command to analyze
     * @return Suggested new priority, or null if no change recommended
     */
    fun suggestPriorityAdjustment(command: VoiceCommand): PrioritySuggestion? {
        val currentPriority = command.priority
        val usageCount = command.usageCount

        // High usage but low priority -> suggest increase
        if (usageCount > 50 && currentPriority < HIGH_THRESHOLD) {
            val suggested = minOf(currentPriority + 20, HIGH_THRESHOLD)
            return PrioritySuggestion(
                currentPriority = currentPriority,
                suggestedPriority = suggested,
                reason = "High usage ($usageCount) suggests increasing priority",
                confidence = 0.8f
            )
        }

        // Very high usage but not critical -> suggest critical
        if (usageCount > 100 && currentPriority < PRIORITY_MAX - 10) {
            val suggested = minOf(currentPriority + 25, PRIORITY_MAX - 10)
            return PrioritySuggestion(
                currentPriority = currentPriority,
                suggestedPriority = suggested,
                reason = "Very high usage ($usageCount) suggests critical priority",
                confidence = 0.9f
            )
        }

        // Low usage and high priority -> suggest decrease
        if (usageCount < 5 && command.createdAt < System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000) && currentPriority > NORMAL_THRESHOLD) {
            val suggested = maxOf(currentPriority - 15, NORMAL_THRESHOLD)
            return PrioritySuggestion(
                currentPriority = currentPriority,
                suggestedPriority = suggested,
                reason = "Low usage ($usageCount) after 30 days suggests reducing priority",
                confidence = 0.7f
            )
        }

        return null // No adjustment recommended
    }

    /**
     * Check if two priorities are likely to conflict
     *
     * @param priority1 First priority
     * @param priority2 Second priority
     * @param tolerance Priority difference tolerance
     * @return true if priorities are too close (potential conflict)
     */
    fun wouldConflict(
        priority1: Int,
        priority2: Int,
        tolerance: Int = DEFAULT_CONFLICT_TOLERANCE
    ): Boolean {
        return abs(priority1 - priority2) <= tolerance
    }

    /**
     * Find optimal priority to avoid conflicts
     *
     * @param existingPriorities List of existing priorities to avoid
     * @param preferredLevel Preferred priority level
     * @param tolerance Minimum gap to maintain from existing priorities
     * @return Suggested priority value
     */
    fun findOptimalPriority(
        existingPriorities: List<Int>,
        preferredLevel: PriorityLevel = PriorityLevel.NORMAL,
        tolerance: Int = DEFAULT_CONFLICT_TOLERANCE
    ): Int {
        if (existingPriorities.isEmpty()) {
            return getDefaultForLevel(preferredLevel)
        }

        val sorted = existingPriorities.sorted()
        val preferredBase = getDefaultForLevel(preferredLevel)

        // Try preferred value first
        if (!conflictsWithAny(preferredBase, sorted, tolerance)) {
            return preferredBase
        }

        // Find gap in the preferred range
        val rangeStart = when (preferredLevel) {
            PriorityLevel.LOW -> PRIORITY_MIN
            PriorityLevel.NORMAL -> LOW_THRESHOLD + 1
            PriorityLevel.HIGH -> NORMAL_THRESHOLD + 1
            PriorityLevel.CRITICAL -> HIGH_THRESHOLD + 1
        }

        val rangeEnd = when (preferredLevel) {
            PriorityLevel.LOW -> LOW_THRESHOLD
            PriorityLevel.NORMAL -> NORMAL_THRESHOLD
            PriorityLevel.HIGH -> HIGH_THRESHOLD
            PriorityLevel.CRITICAL -> PRIORITY_MAX
        }

        for (priority in rangeStart..rangeEnd) {
            if (!conflictsWithAny(priority, sorted, tolerance)) {
                return priority
            }
        }

        // If no gap found in preferred range, return middle of range
        return (rangeStart + rangeEnd) / 2
    }

    /**
     * Get default priority for a priority level
     *
     * @param level The priority level
     * @return Default priority value for that level
     */
    fun getDefaultForLevel(level: PriorityLevel): Int {
        return when (level) {
            PriorityLevel.LOW -> (PRIORITY_MIN + LOW_THRESHOLD) / 2
            PriorityLevel.NORMAL -> (LOW_THRESHOLD + NORMAL_THRESHOLD) / 2
            PriorityLevel.HIGH -> (NORMAL_THRESHOLD + HIGH_THRESHOLD) / 2
            PriorityLevel.CRITICAL -> (HIGH_THRESHOLD + PRIORITY_MAX) / 2
        }
    }

    /**
     * Check if a priority conflicts with any in a list
     */
    private fun conflictsWithAny(
        priority: Int,
        existing: List<Int>,
        tolerance: Int
    ): Boolean {
        return existing.any { abs(it - priority) <= tolerance }
    }

    /**
     * Create a priority breakdown report
     *
     * @param commands List of commands to analyze
     * @return Statistics about priority distribution
     */
    fun analyzeDistribution(commands: List<VoiceCommand>): PriorityDistribution {
        val byLevel = commands.groupBy { getLevel(it.priority) }
            .mapValues { it.value.size }

        val averagePriority = if (commands.isNotEmpty()) {
            commands.map { it.priority }.average().toFloat()
        } else {
            0f
        }

        val priorityGaps = findPriorityGaps(commands.map { it.priority })
        val conflicts = findPriorityConflicts(commands)

        return PriorityDistribution(
            totalCommands = commands.size,
            averagePriority = averagePriority,
            commandsByLevel = byLevel,
            availableGaps = priorityGaps,
            potentialConflicts = conflicts.size
        )
    }

    /**
     * Find gaps in priority values (good for new commands)
     */
    private fun findPriorityGaps(priorities: List<Int>): List<IntRange> {
        if (priorities.isEmpty()) return listOf(PRIORITY_MIN..PRIORITY_MAX)

        val sorted = priorities.sorted()
        val gaps = mutableListOf<IntRange>()

        // Gap before first priority
        if (sorted.first() > PRIORITY_MIN + DEFAULT_CONFLICT_TOLERANCE) {
            gaps.add(PRIORITY_MIN until (sorted.first() - DEFAULT_CONFLICT_TOLERANCE))
        }

        // Gaps between priorities
        for (i in 0 until sorted.size - 1) {
            val gap = sorted[i + 1] - sorted[i]
            if (gap > DEFAULT_CONFLICT_TOLERANCE * 2) {
                gaps.add((sorted[i] + DEFAULT_CONFLICT_TOLERANCE)..(sorted[i + 1] - DEFAULT_CONFLICT_TOLERANCE))
            }
        }

        // Gap after last priority
        if (sorted.last() < PRIORITY_MAX - DEFAULT_CONFLICT_TOLERANCE) {
            gaps.add((sorted.last() + DEFAULT_CONFLICT_TOLERANCE)..PRIORITY_MAX)
        }

        return gaps
    }

    /**
     * Find priority conflicts in a list of commands
     */
    private fun findPriorityConflicts(
        commands: List<VoiceCommand>,
        tolerance: Int = DEFAULT_CONFLICT_TOLERANCE
    ): List<Pair<VoiceCommand, VoiceCommand>> {
        val conflicts = mutableListOf<Pair<VoiceCommand, VoiceCommand>>()

        for (i in commands.indices) {
            for (j in (i + 1) until commands.size) {
                if (wouldConflict(commands[i].priority, commands[j].priority, tolerance)) {
                    conflicts.add(Pair(commands[i], commands[j]))
                }
            }
        }

        return conflicts
    }
}

/**
 * Priority adjustment suggestion
 */
data class PrioritySuggestion(
    val currentPriority: Int,
    val suggestedPriority: Int,
    val reason: String,
    val confidence: Float
) {
    init {
        require(confidence in 0f..1f) { "Confidence must be between 0.0 and 1.0" }
    }

    /**
     * Get priority change amount
     */
    fun getChange(): Int = suggestedPriority - currentPriority

    /**
     * Check if suggestion is to increase priority
     */
    fun isIncrease(): Boolean = suggestedPriority > currentPriority

    /**
     * Check if suggestion is high confidence
     */
    fun isHighConfidence(): Boolean = confidence >= 0.8f
}

/**
 * Priority distribution statistics
 */
data class PriorityDistribution(
    val totalCommands: Int,
    val averagePriority: Float,
    val commandsByLevel: Map<PriorityLevel, Int>,
    val availableGaps: List<IntRange>,
    val potentialConflicts: Int
) {
    /**
     * Check if distribution is balanced
     */
    fun isBalanced(): Boolean {
        if (totalCommands == 0) return true

        // Balanced if no single level has more than 60% of commands
        val maxPercent = commandsByLevel.values.maxOrNull()?.let {
            (it.toFloat() / totalCommands) * 100f
        } ?: 0f

        return maxPercent < 60f
    }

    /**
     * Get percentage of commands at each level
     */
    fun getLevelPercentages(): Map<PriorityLevel, Float> {
        if (totalCommands == 0) return emptyMap()

        return commandsByLevel.mapValues { (_, count) ->
            (count.toFloat() / totalCommands) * 100f
        }
    }

    /**
     * Check if there's room for more commands
     */
    fun hasCapacity(): Boolean = availableGaps.isNotEmpty()

    /**
     * Get health score (0.0-1.0)
     */
    fun getHealthScore(): Float {
        var score = 1.0f

        // Deduct for conflicts (max 0.3 deduction)
        val conflictPenalty = minOf(potentialConflicts * 0.05f, 0.3f)
        score -= conflictPenalty

        // Deduct for imbalance (max 0.2 deduction)
        if (!isBalanced()) {
            score -= 0.2f
        }

        // Deduct if no capacity (max 0.1 deduction)
        if (!hasCapacity()) {
            score -= 0.1f
        }

        return score.coerceIn(0f, 1f)
    }
}

// RegistryStatistics is defined in RegistrationListener.kt to avoid duplication
