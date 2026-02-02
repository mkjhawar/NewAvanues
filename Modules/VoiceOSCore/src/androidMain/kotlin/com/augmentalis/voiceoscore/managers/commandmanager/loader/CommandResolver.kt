/**
 * CommandResolver.kt - Resolve user voice input to commands
 * 
 * Purpose: Match user speech to commands with fallback support
 * Resolution order: user locale → English fallback → null
 * 
 * Features:
 * - Exact matching
 * - Fuzzy matching (Levenshtein distance)
 * - Synonym matching
 * - Priority-based ranking
 */

package com.augmentalis.voiceoscore.managers.commandmanager.loader

import android.util.Log
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.CommandUsageDaoAdapter
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.CommandUsageEntity
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.VoiceCommandDaoAdapter
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.VoiceCommandEntity

/**
 * Command resolver with fallback support
 * Version 2: Added usage tracking for analytics and learning
 */
class CommandResolver(
    private val commandDao: VoiceCommandDaoAdapter,
    private val usageDao: CommandUsageDaoAdapter? = null
) {
    
    companion object {
        private const val TAG = "CommandResolver"
        private const val FALLBACK_LOCALE = "en-US"
        private const val FUZZY_MATCH_THRESHOLD = 3 // Max Levenshtein distance for fuzzy match
    }
    
    /**
     * Resolve user voice input to a command
     *
     * Resolution strategy:
     * 1. Try exact match in user locale
     * 2. Try fuzzy match in user locale
     * 3. Try exact match in English fallback
     * 4. Try fuzzy match in English fallback
     * 5. Return null if no match
     *
     * Version 2: Tracks usage for analytics and learning
     *
     * @param userInput Voice input from user
     * @param userLocale User's locale (e.g., "es-ES")
     * @param contextApp Optional app package for context tracking
     * @return Best matching command or null
     */
    suspend fun resolveCommand(
        userInput: String,
        userLocale: String,
        contextApp: String? = null
    ): ResolveResult {
        val startTime = System.currentTimeMillis()
        val normalized = userInput.lowercase().trim()

        if (normalized.isEmpty()) {
            return ResolveResult.NoMatch("Empty input")
        }

        Log.d(TAG, "Resolving command: '$normalized' (locale: $userLocale)")
        
        // Step 1: Try exact match in user locale
        val userLocaleExact = findExactMatches(normalized, userLocale)
        if (userLocaleExact.isNotEmpty()) {
            val best = selectBestMatch(userLocaleExact, normalized)
            Log.d(TAG, "✅ Exact match in $userLocale: ${best.id}")
            val result = ResolveResult.Match(best, MatchType.EXACT, userLocale)
            trackUsage(result, normalized, startTime, userLocale, contextApp)
            return result
        }

        // Step 2: Try fuzzy match in user locale
        if (userLocale != FALLBACK_LOCALE) {
            val userLocaleFuzzy = findFuzzyMatches(normalized, userLocale)
            if (userLocaleFuzzy.isNotEmpty()) {
                val best = selectBestMatch(userLocaleFuzzy, normalized)
                Log.d(TAG, "✅ Fuzzy match in $userLocale: ${best.id}")
                val result = ResolveResult.Match(best, MatchType.FUZZY, userLocale)
                trackUsage(result, normalized, startTime, userLocale, contextApp)
                return result
            }
        }

        // Step 3: Try exact match in English fallback
        val fallbackExact = findExactMatches(normalized, FALLBACK_LOCALE)
        if (fallbackExact.isNotEmpty()) {
            val best = selectBestMatch(fallbackExact, normalized)
            Log.d(TAG, "✅ Exact match in English fallback: ${best.id}")
            val result = ResolveResult.Match(best, MatchType.EXACT, FALLBACK_LOCALE)
            trackUsage(result, normalized, startTime, userLocale, contextApp)
            return result
        }

        // Step 4: Try fuzzy match in English fallback
        val fallbackFuzzy = findFuzzyMatches(normalized, FALLBACK_LOCALE)
        if (fallbackFuzzy.isNotEmpty()) {
            val best = selectBestMatch(fallbackFuzzy, normalized)
            Log.d(TAG, "✅ Fuzzy match in English fallback: ${best.id}")
            val result = ResolveResult.Match(best, MatchType.FUZZY, FALLBACK_LOCALE)
            trackUsage(result, normalized, startTime, userLocale, contextApp)
            return result
        }

        // Step 5: No match found
        Log.d(TAG, "❌ No match found for: '$normalized'")
        val result = ResolveResult.NoMatch("No matching command found")
        trackUsage(result, normalized, startTime, userLocale, contextApp)
        return result
    }

    /**
     * Track command usage (if usageDao is available)
     */
    private suspend fun trackUsage(
        result: ResolveResult,
        userInput: String,
        startTime: Long,
        userLocale: String,
        contextApp: String?
    ) {
        if (usageDao == null) return

        val executionTime = System.currentTimeMillis() - startTime

        try {
            val usageEntity = when (result) {
                is ResolveResult.Match -> {
                    CommandUsageEntity.success(
                        commandId = result.command.id,
                        locale = result.locale,
                        userInput = userInput,
                        matchType = result.matchType.name,
                        executionTimeMs = executionTime,
                        contextApp = contextApp
                    )
                }
                is ResolveResult.NoMatch -> {
                    CommandUsageEntity.failure(
                        userInput = userInput,
                        locale = userLocale,
                        executionTimeMs = executionTime,
                        contextApp = contextApp
                    )
                }
            }

            usageDao.recordUsage(usageEntity)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to track usage", e)
            // Don't fail command resolution if usage tracking fails
        }
    }
    
    /**
     * Find exact matches (case-insensitive)
     * Checks primary text and all synonyms
     */
    private suspend fun findExactMatches(input: String, locale: String): List<VoiceCommandEntity> {
        val commands = commandDao.getCommandsForLocale(locale)
        
        return commands.filter { command ->
            // Check primary text
            if (command.primaryText.equals(input, ignoreCase = true)) {
                return@filter true
            }
            
            // Check synonyms
            val synonyms = VoiceCommandEntity.parseSynonyms(command.synonyms)
            synonyms.any { it.equals(input, ignoreCase = true) }
        }
    }
    
    /**
     * Find fuzzy matches using Levenshtein distance
     */
    private suspend fun findFuzzyMatches(input: String, locale: String): List<VoiceCommandEntity> {
        val commands = commandDao.getCommandsForLocale(locale)
        
        return commands.filter { command ->
            // Check primary text
            if (levenshteinDistance(input, command.primaryText.lowercase()) <= FUZZY_MATCH_THRESHOLD) {
                return@filter true
            }
            
            // Check synonyms
            val synonyms = VoiceCommandEntity.parseSynonyms(command.synonyms)
            synonyms.any { 
                levenshteinDistance(input, it.lowercase()) <= FUZZY_MATCH_THRESHOLD
            }
        }
    }
    
    /**
     * Select best match from multiple candidates
     * Prioritizes:
     * 1. Primary text match over synonym match
     * 2. Higher priority commands
     * 3. Shorter command text (more specific)
     */
    private fun selectBestMatch(matches: List<VoiceCommandEntity>, input: String): VoiceCommandEntity {
        return matches.maxByOrNull { command ->
            var score = 0.0
            
            // Primary text match gets bonus
            if (command.primaryText.equals(input, ignoreCase = true)) {
                score += 100.0
            }
            
            // Priority
            score += command.priority.toDouble()
            
            // Shorter text is more specific (inverse length bonus)
            score += (1.0 / (command.primaryText.length + 1)) * 10
            
            score
        } ?: matches.first()
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     * (minimum number of single-character edits)
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) {
            dp[i][0] = i
        }
        for (j in 0..s2.length) {
            dp[0][j] = j
        }
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    /**
     * Get all matching commands (not just best)
     * Useful for debugging or showing alternatives
     */
    suspend fun findAllMatches(userInput: String, userLocale: String): List<MatchResult> {
        val normalized = userInput.lowercase().trim()
        val results = mutableListOf<MatchResult>()
        
        // User locale exact
        findExactMatches(normalized, userLocale).forEach {
            results.add(MatchResult(it, MatchType.EXACT, userLocale, 100))
        }
        
        // User locale fuzzy
        if (userLocale != FALLBACK_LOCALE) {
            findFuzzyMatches(normalized, userLocale).forEach {
                results.add(MatchResult(it, MatchType.FUZZY, userLocale, 75))
            }
        }
        
        // Fallback exact
        findExactMatches(normalized, FALLBACK_LOCALE).forEach {
            results.add(MatchResult(it, MatchType.EXACT, FALLBACK_LOCALE, 50))
        }
        
        // Fallback fuzzy
        findFuzzyMatches(normalized, FALLBACK_LOCALE).forEach {
            results.add(MatchResult(it, MatchType.FUZZY, FALLBACK_LOCALE, 25))
        }
        
        return results.sortedByDescending { it.confidence }
    }
    
    /**
     * Resolve result sealed class
     */
    sealed class ResolveResult {
        data class Match(
            val command: VoiceCommandEntity,
            val matchType: MatchType,
            val locale: String
        ) : ResolveResult()
        
        data class NoMatch(val reason: String) : ResolveResult()
    }
    
    /**
     * Match type enum
     */
    enum class MatchType {
        EXACT,  // Exact text match
        FUZZY   // Fuzzy match (Levenshtein distance <= threshold)
    }
    
    /**
     * Match result with confidence score
     */
    data class MatchResult(
        val command: VoiceCommandEntity,
        val matchType: MatchType,
        val locale: String,
        val confidence: Int // 0-100
    )
}
