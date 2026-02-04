/**
 * ConflictInfo.kt - Data models for command conflict detection and resolution
 *
 * Part of Week 4 implementation - Dynamic Command Registration
 * Provides detailed information about command conflicts and resolution strategies
 *
 * @since VOS4 Week 4
 * @author VOS4 Development Team
 */

package com.augmentalis.voiceoscore.managers.commandmanager.dynamic

/**
 * Information about a command conflict
 *
 * @property conflictType Type of conflict detected
 * @property affectedCommands List of commands involved in the conflict
 * @property conflictingPhrases Specific phrases that overlap
 * @property severity How severe the conflict is
 * @property detectedAt Timestamp when conflict was detected
 * @property resolutionSuggestions Suggested ways to resolve the conflict
 */
data class ConflictInfo(
    val conflictType: ConflictType,
    val affectedCommands: List<String>, // Command IDs
    val conflictingPhrases: List<String>,
    val severity: ConflictSeverity,
    val detectedAt: Long = System.currentTimeMillis(),
    val resolutionSuggestions: List<ResolutionSuggestion> = emptyList()
) {
    /**
     * Get a human-readable description of the conflict
     */
    fun getDescription(): String = when (conflictType) {
        ConflictType.EXACT_MATCH ->
            "Multiple commands use identical phrases: ${conflictingPhrases.joinToString(", ")}"
        ConflictType.SIMILAR_PHRASE ->
            "Commands have very similar phrases that may be confused: ${conflictingPhrases.joinToString(", ")}"
        ConflictType.PRIORITY_CONFLICT ->
            "Commands with same priority compete for same phrases: ${conflictingPhrases.joinToString(", ")}"
        ConflictType.NAMESPACE_OVERLAP ->
            "Commands from different namespaces share phrases: ${conflictingPhrases.joinToString(", ")}"
        ConflictType.SUBSTRING_MATCH ->
            "One phrase is a substring of another: ${conflictingPhrases.joinToString(", ")}"
    }

    /**
     * Check if this is a critical conflict that must be resolved
     */
    fun isCritical(): Boolean = severity == ConflictSeverity.CRITICAL

    /**
     * Check if auto-resolution is recommended
     */
    fun canAutoResolve(): Boolean =
        resolutionSuggestions.any { it.confidence >= 0.8f && it.autoApplicable }
}

/**
 * Types of conflicts that can occur between commands
 */
enum class ConflictType {
    /**
     * Two or more commands use exactly the same phrase
     * Example: "go back" used by multiple commands
     */
    EXACT_MATCH,

    /**
     * Commands use very similar phrases that could be confused
     * Example: "go home" vs "go to home"
     */
    SIMILAR_PHRASE,

    /**
     * Multiple commands with same priority compete for same phrase
     * Resolution requires priority adjustment
     */
    PRIORITY_CONFLICT,

    /**
     * Commands from different namespaces share phrases
     * May or may not be a problem depending on namespace isolation
     */
    NAMESPACE_OVERLAP,

    /**
     * One phrase is a substring of another
     * Example: "back" vs "go back"
     */
    SUBSTRING_MATCH
}

/**
 * Severity levels for conflicts
 */
enum class ConflictSeverity {
    /**
     * Critical conflict - must be resolved before registration
     * Example: Exact match in same namespace with same priority
     */
    CRITICAL,

    /**
     * High severity - should be resolved but not blocking
     * Example: Very similar phrases with close priorities
     */
    HIGH,

    /**
     * Medium severity - recommended to resolve
     * Example: Substring matches with different priorities
     */
    MEDIUM,

    /**
     * Low severity - informational only
     * Example: Similar phrases in different namespaces
     */
    LOW,

    /**
     * Info only - no action needed
     * Example: Different phrases with some word overlap
     */
    INFO
}

/**
 * Suggested resolution for a conflict
 *
 * @property strategy The resolution strategy to apply
 * @property description Human-readable description of the resolution
 * @property confidence How confident we are this will resolve the issue (0.0 - 1.0)
 * @property autoApplicable Whether this can be automatically applied
 * @property parameters Additional parameters needed for resolution
 */
data class ResolutionSuggestion(
    val strategy: ResolutionStrategy,
    val description: String,
    val confidence: Float,
    val autoApplicable: Boolean = false,
    val parameters: Map<String, Any> = emptyMap()
) {
    init {
        require(confidence in 0f..1f) { "Confidence must be between 0.0 and 1.0" }
    }
}

/**
 * Strategies for resolving command conflicts
 */
enum class ResolutionStrategy {
    /**
     * Increase priority of one command
     */
    INCREASE_PRIORITY,

    /**
     * Decrease priority of one command
     */
    DECREASE_PRIORITY,

    /**
     * Rename one or more phrases to make them distinct
     */
    RENAME_PHRASE,

    /**
     * Disable one of the conflicting commands
     */
    DISABLE_COMMAND,

    /**
     * Move command to different namespace
     */
    CHANGE_NAMESPACE,

    /**
     * Remove the conflicting phrase from one command
     */
    REMOVE_PHRASE,

    /**
     * Add disambiguation prefix/suffix to phrases
     */
    ADD_DISAMBIGUATION,

    /**
     * Merge commands into a single command with parameters
     */
    MERGE_COMMANDS,

    /**
     * Keep both commands and use priority-based resolution at runtime
     */
    PRIORITY_RESOLUTION,

    /**
     * Keep both commands and ask user at runtime
     */
    USER_DISAMBIGUATION
}

/**
 * Result of conflict detection
 */
sealed class ConflictDetectionResult {
    /**
     * No conflicts detected
     */
    object NoConflict : ConflictDetectionResult()

    /**
     * Conflicts detected
     *
     * @property conflicts List of detected conflicts
     * @property criticalCount Number of critical conflicts
     * @property canProceed Whether registration can proceed despite conflicts
     */
    data class ConflictsDetected(
        val conflicts: List<ConflictInfo>,
        val criticalCount: Int,
        val canProceed: Boolean
    ) : ConflictDetectionResult() {
        /**
         * Get conflicts by severity
         */
        fun getConflictsBySeverity(severity: ConflictSeverity): List<ConflictInfo> =
            conflicts.filter { it.severity == severity }

        /**
         * Get all critical conflicts
         */
        fun getCriticalConflicts(): List<ConflictInfo> =
            conflicts.filter { it.isCritical() }

        /**
         * Get auto-resolvable conflicts
         */
        fun getAutoResolvableConflicts(): List<ConflictInfo> =
            conflicts.filter { it.canAutoResolve() }
    }
}

/**
 * Conflict resolution action
 *
 * @property commandId ID of command to modify
 * @property action Action to perform
 * @property newValue New value to apply (priority, phrase, namespace, etc.)
 */
data class ConflictResolutionAction(
    val commandId: String,
    val action: ResolutionStrategy,
    val newValue: Any
)

/**
 * Result of applying conflict resolution
 */
sealed class ConflictResolutionResult {
    /**
     * Conflict successfully resolved
     */
    data class Resolved(val appliedActions: List<ConflictResolutionAction>) : ConflictResolutionResult()

    /**
     * Failed to resolve conflict
     *
     * @property reason Why resolution failed
     * @property remainingConflicts Conflicts that still exist after attempted resolution
     */
    data class Failed(
        val reason: String,
        val remainingConflicts: List<ConflictInfo> = emptyList()
    ) : ConflictResolutionResult()

    /**
     * Partial resolution - some conflicts resolved, some remain
     *
     * @property resolvedConflicts Conflicts that were resolved
     * @property remainingConflicts Conflicts that still exist
     */
    data class Partial(
        val resolvedConflicts: List<ConflictInfo>,
        val remainingConflicts: List<ConflictInfo>
    ) : ConflictResolutionResult()
}

/**
 * Configuration for conflict detection
 *
 * @property similarityThreshold Minimum similarity (0.0-1.0) to consider phrases conflicting
 * @property checkSubstrings Whether to check for substring matches
 * @property checkCrossNamespace Whether to check conflicts across namespaces
 * @property priorityToleranceRange Commands within this priority range are considered conflicting
 */
data class ConflictDetectionConfig(
    val similarityThreshold: Float = 0.85f,
    val checkSubstrings: Boolean = true,
    val checkCrossNamespace: Boolean = false,
    val priorityToleranceRange: Int = 5
) {
    init {
        require(similarityThreshold in 0f..1f) {
            "Similarity threshold must be between 0.0 and 1.0"
        }
        require(priorityToleranceRange >= 0) {
            "Priority tolerance range must be non-negative"
        }
    }
}

/**
 * Statistics about conflicts in the registry
 *
 * @property totalConflicts Total number of conflicts detected
 * @property criticalConflicts Number of critical conflicts
 * @property highSeverityConflicts Number of high severity conflicts
 * @property mediumSeverityConflicts Number of medium severity conflicts
 * @property lowSeverityConflicts Number of low severity conflicts
 * @property autoResolvableCount Number of conflicts that can be auto-resolved
 * @property conflictsByType Map of conflict types to their counts
 */
data class ConflictStatistics(
    val totalConflicts: Int,
    val criticalConflicts: Int,
    val highSeverityConflicts: Int,
    val mediumSeverityConflicts: Int,
    val lowSeverityConflicts: Int,
    val autoResolvableCount: Int,
    val conflictsByType: Map<ConflictType, Int>
) {
    /**
     * Check if registry is in a healthy state
     */
    fun isHealthy(): Boolean = criticalConflicts == 0 && highSeverityConflicts == 0

    /**
     * Get overall health score (0.0 - 1.0)
     */
    fun getHealthScore(): Float {
        if (totalConflicts == 0) return 1.0f

        val weightedConflicts = (criticalConflicts * 4f) +
                               (highSeverityConflicts * 2f) +
                               (mediumSeverityConflicts * 1f) +
                               (lowSeverityConflicts * 0.5f)

        val maxPossibleScore = totalConflicts * 4f
        return 1f - (weightedConflicts / maxPossibleScore).coerceIn(0f, 1f)
    }
}
