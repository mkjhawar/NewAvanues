/**
 * ConflictDetector.kt - Command conflict detection and resolution engine
 *
 * Part of Week 4 implementation - Dynamic Command Registration
 * Detects conflicts between commands and suggests resolutions
 *
 * @since VOS4 Week 4
 * @author VOS4 Development Team
 */

package com.augmentalis.voiceoscore.dynamic

import kotlin.math.abs

/**
 * Detects and analyzes conflicts between voice commands
 *
 * Provides:
 * - Exact match detection
 * - Similarity-based conflict detection
 * - Priority conflict detection
 * - Substring match detection
 * - Cross-namespace conflict checking
 * - Automated resolution suggestions
 *
 * Thread-safe for concurrent conflict detection
 */
class ConflictDetector(
    private val config: ConflictDetectionConfig = ConflictDetectionConfig()
) {

    companion object {
        private const val TAG = "ConflictDetector"

        // Similarity thresholds for different conflict levels
        private const val EXACT_MATCH_THRESHOLD = 1.0f
        private const val VERY_HIGH_SIMILARITY = 0.95f
        private const val HIGH_SIMILARITY = 0.85f
        private const val MEDIUM_SIMILARITY = 0.70f
    }

    /**
     * Detect conflicts between a new command and existing commands
     *
     * @param newCommand The command being added
     * @param existingCommands List of commands already registered
     * @param namespace Target namespace for the new command (null for all namespaces)
     * @return Conflict detection result
     */
    fun detectConflicts(
        newCommand: VoiceCommand,
        existingCommands: List<VoiceCommand>,
        namespace: String? = null
    ): ConflictDetectionResult {
        val conflicts = mutableListOf<ConflictInfo>()

        // Filter existing commands by namespace if specified
        val relevantCommands = if (namespace != null && !config.checkCrossNamespace) {
            existingCommands.filter { it.namespace == namespace }
        } else {
            existingCommands
        }

        // Check each phrase in the new command
        for (newPhrase in newCommand.phrases) {
            for (existingCommand in relevantCommands) {
                // Skip if same command (update scenario)
                if (existingCommand.id == newCommand.id) continue

                // Check each phrase in existing command
                for (existingPhrase in existingCommand.phrases) {
                    val conflict = detectPhraseConflict(
                        newPhrase = newPhrase,
                        existingPhrase = existingPhrase,
                        newCommand = newCommand,
                        existingCommand = existingCommand
                    )

                    if (conflict != null) {
                        conflicts.add(conflict)
                    }
                }
            }
        }

        // Deduplicate and merge similar conflicts
        val uniqueConflicts = deduplicateConflicts(conflicts)

        return if (uniqueConflicts.isEmpty()) {
            ConflictDetectionResult.NoConflict
        } else {
            val criticalCount = uniqueConflicts.count { it.isCritical() }
            val canProceed = criticalCount == 0

            ConflictDetectionResult.ConflictsDetected(
                conflicts = uniqueConflicts,
                criticalCount = criticalCount,
                canProceed = canProceed
            )
        }
    }

    /**
     * Detect conflict between two specific phrases
     *
     * @param newPhrase Phrase from new command
     * @param existingPhrase Phrase from existing command
     * @param newCommand The new command being added
     * @param existingCommand The existing command
     * @return ConflictInfo if conflict detected, null otherwise
     */
    private fun detectPhraseConflict(
        newPhrase: String,
        existingPhrase: String,
        newCommand: VoiceCommand,
        existingCommand: VoiceCommand
    ): ConflictInfo? {
        val normalizedNew = newPhrase.trim().lowercase()
        val normalizedExisting = existingPhrase.trim().lowercase()

        // Check for exact match
        if (normalizedNew == normalizedExisting) {
            return createExactMatchConflict(newCommand, existingCommand, newPhrase, existingPhrase)
        }

        // Check for substring match
        if (config.checkSubstrings) {
            val substringConflict = checkSubstringMatch(
                normalizedNew, normalizedExisting,
                newCommand, existingCommand,
                newPhrase, existingPhrase
            )
            if (substringConflict != null) return substringConflict
        }

        // Check for similarity-based conflict
        val similarity = calculateSimilarity(normalizedNew, normalizedExisting)
        if (similarity >= config.similarityThreshold) {
            return createSimilarityConflict(
                newCommand, existingCommand,
                newPhrase, existingPhrase,
                similarity
            )
        }

        return null
    }

    /**
     * Create exact match conflict
     */
    private fun createExactMatchConflict(
        newCommand: VoiceCommand,
        existingCommand: VoiceCommand,
        newPhrase: String,
        existingPhrase: String
    ): ConflictInfo {
        val severity = determineSeverity(newCommand, existingCommand, EXACT_MATCH_THRESHOLD)
        val suggestions = generateResolutionSuggestions(
            newCommand, existingCommand,
            ConflictType.EXACT_MATCH, severity
        )

        return ConflictInfo(
            conflictType = ConflictType.EXACT_MATCH,
            affectedCommands = listOf(newCommand.id, existingCommand.id),
            conflictingPhrases = listOf(newPhrase, existingPhrase),
            severity = severity,
            resolutionSuggestions = suggestions
        )
    }

    /**
     * Check for substring matches
     */
    private fun checkSubstringMatch(
        normalizedNew: String,
        normalizedExisting: String,
        newCommand: VoiceCommand,
        existingCommand: VoiceCommand,
        newPhrase: String,
        existingPhrase: String
    ): ConflictInfo? {
        val isSubstring = normalizedNew.contains(normalizedExisting) ||
                         normalizedExisting.contains(normalizedNew)

        if (!isSubstring) return null

        val severity = ConflictSeverity.MEDIUM
        val suggestions = generateResolutionSuggestions(
            newCommand, existingCommand,
            ConflictType.SUBSTRING_MATCH, severity
        )

        return ConflictInfo(
            conflictType = ConflictType.SUBSTRING_MATCH,
            affectedCommands = listOf(newCommand.id, existingCommand.id),
            conflictingPhrases = listOf(newPhrase, existingPhrase),
            severity = severity,
            resolutionSuggestions = suggestions
        )
    }

    /**
     * Create similarity-based conflict
     */
    private fun createSimilarityConflict(
        newCommand: VoiceCommand,
        existingCommand: VoiceCommand,
        newPhrase: String,
        existingPhrase: String,
        similarity: Float
    ): ConflictInfo {
        val severity = determineSeverity(newCommand, existingCommand, similarity)
        val suggestions = generateResolutionSuggestions(
            newCommand, existingCommand,
            ConflictType.SIMILAR_PHRASE, severity
        )

        return ConflictInfo(
            conflictType = ConflictType.SIMILAR_PHRASE,
            affectedCommands = listOf(newCommand.id, existingCommand.id),
            conflictingPhrases = listOf(newPhrase, existingPhrase),
            severity = severity,
            resolutionSuggestions = suggestions
        )
    }

    /**
     * Determine conflict severity based on similarity and priorities
     */
    private fun determineSeverity(
        newCommand: VoiceCommand,
        existingCommand: VoiceCommand,
        similarity: Float
    ): ConflictSeverity {
        val priorityDiff = abs(newCommand.priority - existingCommand.priority)
        val sameNamespace = newCommand.namespace == existingCommand.namespace

        return when {
            // Critical: Exact match in same namespace with same/similar priority
            similarity >= EXACT_MATCH_THRESHOLD &&
            sameNamespace &&
            priorityDiff <= config.priorityToleranceRange ->
                ConflictSeverity.CRITICAL

            // High: Very similar in same namespace
            similarity >= VERY_HIGH_SIMILARITY && sameNamespace ->
                ConflictSeverity.HIGH

            // High: Exact match in different namespace
            similarity >= EXACT_MATCH_THRESHOLD && !sameNamespace ->
                ConflictSeverity.HIGH

            // Medium: High similarity in same namespace
            similarity >= HIGH_SIMILARITY && sameNamespace ->
                ConflictSeverity.MEDIUM

            // Medium: Very similar in different namespace
            similarity >= VERY_HIGH_SIMILARITY && !sameNamespace ->
                ConflictSeverity.MEDIUM

            // Low: Medium similarity in same namespace
            similarity >= MEDIUM_SIMILARITY && sameNamespace ->
                ConflictSeverity.LOW

            // Info: Similar in different namespace
            else ->
                ConflictSeverity.INFO
        }
    }

    /**
     * Generate resolution suggestions for a conflict
     */
    private fun generateResolutionSuggestions(
        newCommand: VoiceCommand,
        existingCommand: VoiceCommand,
        conflictType: ConflictType,
        severity: ConflictSeverity
    ): List<ResolutionSuggestion> {
        val suggestions = mutableListOf<ResolutionSuggestion>()

        when (conflictType) {
            ConflictType.EXACT_MATCH -> {
                // Suggest priority adjustment
                if (newCommand.priority <= existingCommand.priority) {
                    suggestions.add(
                        ResolutionSuggestion(
                            strategy = ResolutionStrategy.INCREASE_PRIORITY,
                            description = "Increase priority of new command to ${existingCommand.priority + 10}",
                            confidence = 0.9f,
                            autoApplicable = severity != ConflictSeverity.CRITICAL,
                            parameters = mapOf(
                                "commandId" to newCommand.id,
                                "newPriority" to (existingCommand.priority + 10)
                            )
                        )
                    )
                }

                // Suggest renaming
                suggestions.add(
                    ResolutionSuggestion(
                        strategy = ResolutionStrategy.RENAME_PHRASE,
                        description = "Rename phrase to make it unique",
                        confidence = 0.8f,
                        autoApplicable = false
                    )
                )

                // Suggest namespace change
                if (newCommand.namespace == existingCommand.namespace) {
                    suggestions.add(
                        ResolutionSuggestion(
                            strategy = ResolutionStrategy.CHANGE_NAMESPACE,
                            description = "Move to different namespace",
                            confidence = 0.7f,
                            autoApplicable = false
                        )
                    )
                }
            }

            ConflictType.SIMILAR_PHRASE -> {
                // Suggest priority-based resolution
                suggestions.add(
                    ResolutionSuggestion(
                        strategy = ResolutionStrategy.PRIORITY_RESOLUTION,
                        description = "Use priority to resolve at runtime",
                        confidence = 0.8f,
                        autoApplicable = true
                    )
                )

                // Suggest adding disambiguation
                suggestions.add(
                    ResolutionSuggestion(
                        strategy = ResolutionStrategy.ADD_DISAMBIGUATION,
                        description = "Add prefix/suffix to distinguish phrases",
                        confidence = 0.7f,
                        autoApplicable = false
                    )
                )
            }

            ConflictType.SUBSTRING_MATCH -> {
                // Suggest priority adjustment
                suggestions.add(
                    ResolutionSuggestion(
                        strategy = ResolutionStrategy.PRIORITY_RESOLUTION,
                        description = "Use priority for substring matches",
                        confidence = 0.85f,
                        autoApplicable = true
                    )
                )
            }

            ConflictType.PRIORITY_CONFLICT -> {
                // Suggest priority adjustment
                suggestions.add(
                    ResolutionSuggestion(
                        strategy = ResolutionStrategy.INCREASE_PRIORITY,
                        description = "Adjust priority to resolve conflict",
                        confidence = 0.9f,
                        autoApplicable = true
                    )
                )
            }

            ConflictType.NAMESPACE_OVERLAP -> {
                // Suggest namespace isolation
                suggestions.add(
                    ResolutionSuggestion(
                        strategy = ResolutionStrategy.CHANGE_NAMESPACE,
                        description = "Isolate in separate namespace",
                        confidence = 0.75f,
                        autoApplicable = false
                    )
                )
            }
        }

        return suggestions
    }

    /**
     * Calculate similarity between two strings using Levenshtein distance
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0f

        val maxLen = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        return 1f - (distance.toFloat() / maxLen)
    }

    /**
     * Calculate Levenshtein distance
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[m][n]
    }

    /**
     * Deduplicate conflicts by merging similar ones
     */
    private fun deduplicateConflicts(conflicts: List<ConflictInfo>): List<ConflictInfo> {
        if (conflicts.size <= 1) return conflicts

        val uniqueConflicts = mutableListOf<ConflictInfo>()
        val processed = mutableSetOf<String>()

        for (conflict in conflicts) {
            val key = "${conflict.conflictType}-${conflict.affectedCommands.sorted().joinToString(",")}"
            if (!processed.contains(key)) {
                uniqueConflicts.add(conflict)
                processed.add(key)
            }
        }

        return uniqueConflicts
    }

    /**
     * Generate conflict report for all commands in a namespace
     *
     * @param commands All commands to check
     * @return Statistics about conflicts
     */
    fun generateConflictReport(commands: List<VoiceCommand>): ConflictStatistics {
        val allConflicts = mutableListOf<ConflictInfo>()

        // Check each command against all others
        for (i in commands.indices) {
            for (j in (i + 1) until commands.size) {
                val result = detectConflicts(commands[i], listOf(commands[j]))
                if (result is ConflictDetectionResult.ConflictsDetected) {
                    allConflicts.addAll(result.conflicts)
                }
            }
        }

        val uniqueConflicts = deduplicateConflicts(allConflicts)

        val criticalCount = uniqueConflicts.count { it.severity == ConflictSeverity.CRITICAL }
        val highCount = uniqueConflicts.count { it.severity == ConflictSeverity.HIGH }
        val mediumCount = uniqueConflicts.count { it.severity == ConflictSeverity.MEDIUM }
        val lowCount = uniqueConflicts.count { it.severity == ConflictSeverity.LOW }
        val autoResolvableCount = uniqueConflicts.count { it.canAutoResolve() }

        val conflictsByType = uniqueConflicts
            .groupBy { it.conflictType }
            .mapValues { it.value.size }

        return ConflictStatistics(
            totalConflicts = uniqueConflicts.size,
            criticalConflicts = criticalCount,
            highSeverityConflicts = highCount,
            mediumSeverityConflicts = mediumCount,
            lowSeverityConflicts = lowCount,
            autoResolvableCount = autoResolvableCount,
            conflictsByType = conflictsByType
        )
    }

    /**
     * Suggest resolution for a specific conflict
     *
     * @param conflict The conflict to resolve
     * @return Best resolution suggestion
     */
    fun suggestResolution(conflict: ConflictInfo): ResolutionSuggestion? {
        return conflict.resolutionSuggestions
            .maxByOrNull { it.confidence }
    }

    /**
     * Check if two commands would conflict
     *
     * @param command1 First command
     * @param command2 Second command
     * @return true if commands would conflict
     */
    fun wouldConflict(command1: VoiceCommand, command2: VoiceCommand): Boolean {
        val result = detectConflicts(command1, listOf(command2))
        return result is ConflictDetectionResult.ConflictsDetected
    }
}
