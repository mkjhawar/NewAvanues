/**
 * LearningSystem.kt - Unified learning and command caching for all speech engines
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * 
 * Consolidated Room-based learning, vocabulary caching, and command matching
 * Reduces ~400 lines of duplicated code across engines to ~100 lines
 */
package com.augmentalis.voiceos.speech.engines.common

import android.content.Context
import android.util.Log
import com.augmentalis.datamanager.repositories.RecognitionLearningRepository
import com.augmentalis.datamanager.entities.EngineType
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * Centralized learning system for all speech recognition engines.
 * Handles command learning, vocabulary caching, and similarity matching.
 */
class LearningSystem(
    private val engineType: String,
    private val context: Context,
    private val repository: RecognitionLearningRepository? = null
) {
    
    companion object {
        private const val TAG = "LearningSystem"
        private const val MAX_LEARNED_COMMANDS = 500
        private const val MAX_VOCABULARY_CACHE = 1000
        private const val MIN_LEARNING_CONFIDENCE = 0.85f
        private const val SIMILARITY_THRESHOLD = 0.75f
        private const val CACHE_SAVE_INTERVAL = 30000L // 30 seconds
        private const val LEARNING_STATS_INTERVAL = 60000L // 1 minute
    }
    
    // Storage
    private val learnedCommands = ConcurrentHashMap<String, LearnedCommand>()
    private val vocabularyCache = ConcurrentHashMap<String, VocabularyEntry>()
    private var learningRepository: RecognitionLearningRepository? = null
    
    // Statistics
    private var totalLearningAttempts = 0
    private var successfulLearns = 0
    private var cacheHits = 0
    private var cacheMisses = 0
    
    // Coroutines
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var autoSaveJob: Job? = null
    
    data class LearnedCommand(
        val original: String,
        val learned: String,
        val confidence: Float,
        val useCount: Int,
        val lastUsed: Long,
        val createdAt: Long
    )
    
    data class VocabularyEntry(
        val word: String,
        val variations: Set<String>,
        val frequency: Int,
        val lastSeen: Long
    )
    
    data class MatchResult(
        val matched: String,
        val original: String,
        val confidence: Float,
        val source: MatchSource
    )
    
    enum class MatchSource {
        LEARNED_COMMAND,
        VOCABULARY_CACHE,
        SIMILARITY_MATCH,
        EXACT_MATCH,
        NO_MATCH
    }
    
    init {
        scope.launch {
            initializeLearningStore()
            loadPersistedData()
        }
        startAutoSave()
    }
    
    /**
     * Initialize Room learning repository
     */
    private suspend fun initializeLearningStore() {
        try {
            learningRepository = repository ?: RecognitionLearningRepository.getInstance(context)
            learningRepository?.initialize()
            Log.d(TAG, "[$engineType] Learning repository initialized")
        } catch (e: Exception) {
            Log.e(TAG, "[$engineType] Failed to initialize learning repository", e)
        }
    }
    
    /**
     * Load persisted learning data
     */
    private fun loadPersistedData() {
        scope.launch {
            loadLearnedCommands()
            loadVocabularyCache()
            Log.i(TAG, "[$engineType] Loaded ${learnedCommands.size} learned commands, ${vocabularyCache.size} vocabulary entries")
        }
    }
    
    /**
     * Load learned commands from Room
     */
    private suspend fun loadLearnedCommands() = withContext(Dispatchers.IO) {
        try {
            learningRepository?.let { repository ->
                val loadedCommands = repository.getLearnedCommands(when(engineType.uppercase()) {
                    "ANDROID_STT" -> EngineType.ANDROID_STT
                    "VIVOKA" -> EngineType.VIVOKA
                    "VOSK" -> EngineType.VOSK
                    "GOOGLE_CLOUD" -> EngineType.GOOGLE_CLOUD
                    "WHISPER" -> EngineType.WHISPER
                    else -> EngineType.ANDROID_STT
                })
                loadedCommands.forEach { (original, learned) ->
                    learnedCommands[original] = LearnedCommand(
                        original = original,
                        learned = learned,
                        confidence = 0.9f, // Default confidence for loaded commands
                        useCount = 1,
                        lastUsed = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$engineType] Error loading learned commands", e)
        }
    }
    
    /**
     * Load vocabulary cache from Room
     */
    private suspend fun loadVocabularyCache() = withContext(Dispatchers.IO) {
        try {
            learningRepository?.let { repository ->
                val loadedCache = repository.getVocabularyCache(when(engineType.uppercase()) {
                    "ANDROID_STT" -> EngineType.ANDROID_STT
                    "VIVOKA" -> EngineType.VIVOKA
                    "VOSK" -> EngineType.VOSK
                    "GOOGLE_CLOUD" -> EngineType.GOOGLE_CLOUD
                    "WHISPER" -> EngineType.WHISPER
                    else -> EngineType.ANDROID_STT
                })
                loadedCache.forEach { (word, _) ->
                    vocabularyCache[word] = VocabularyEntry(
                        word = word,
                        variations = setOf(word),
                        frequency = 1,
                        lastSeen = System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$engineType] Error loading vocabulary cache", e)
        }
    }
    
    /**
     * Process recognition with multi-tier matching
     */
    fun processWithLearning(
        recognized: String,
        commands: List<String>,
        confidence: Float = 1.0f
    ): MatchResult {
        val normalizedInput = recognized.lowercase().trim()
        
        // Tier 1: Check learned commands
        learnedCommands[normalizedInput]?.let { learned ->
            updateLearnedCommandUsage(learned)
            cacheHits++
            Log.d(TAG, "[$engineType] Learned command match: $normalizedInput -> ${learned.learned}")
            return MatchResult(
                matched = learned.learned,
                original = recognized,
                confidence = learned.confidence * confidence,
                source = MatchSource.LEARNED_COMMAND
            )
        }
        
        // Tier 2: Check vocabulary cache
        vocabularyCache[normalizedInput]?.let { vocab ->
            val matchedCommand = findBestCommandMatch(vocab.variations, commands)
            if (matchedCommand != null) {
                updateVocabularyUsage(vocab)
                cacheHits++
                Log.d(TAG, "[$engineType] Vocabulary cache match: $normalizedInput -> $matchedCommand")
                return MatchResult(
                    matched = matchedCommand,
                    original = recognized,
                    confidence = confidence * 0.9f,
                    source = MatchSource.VOCABULARY_CACHE
                )
            }
        }
        
        // Tier 3: Exact match with commands
        commands.find { it.equals(normalizedInput, ignoreCase = true) }?.let { command ->
            cacheMisses++
            // Learn this for future use
            if (confidence >= MIN_LEARNING_CONFIDENCE) {
                learnCommand(normalizedInput, command, confidence)
            }
            return MatchResult(
                matched = command,
                original = recognized,
                confidence = confidence,
                source = MatchSource.EXACT_MATCH
            )
        }
        
        // Tier 4: Similarity matching
        val similarMatch = findSimilarCommand(normalizedInput, commands)
        if (similarMatch != null && similarMatch.second >= SIMILARITY_THRESHOLD) {
            cacheMisses++
            // Learn if confidence is high
            if (confidence >= MIN_LEARNING_CONFIDENCE && similarMatch.second >= 0.85f) {
                learnCommand(normalizedInput, similarMatch.first, confidence * similarMatch.second)
            }
            return MatchResult(
                matched = similarMatch.first,
                original = recognized,
                confidence = confidence * similarMatch.second,
                source = MatchSource.SIMILARITY_MATCH
            )
        }
        
        // No match found
        cacheMisses++
        // Add to vocabulary for future analysis
        addToVocabulary(normalizedInput)
        
        return MatchResult(
            matched = recognized,
            original = recognized,
            confidence = confidence,
            source = MatchSource.NO_MATCH
        )
    }
    
    /**
     * Learn a new command mapping
     */
    fun learnCommand(original: String, learned: String, confidence: Float) {
        totalLearningAttempts++
        
        if (learnedCommands.size >= MAX_LEARNED_COMMANDS) {
            // Remove oldest/least used entries
            removeOldestLearnedCommands(10)
        }
        
        val existing = learnedCommands[original]
        val newCommand = if (existing != null) {
            // Update existing with better confidence or increment use count
            existing.copy(
                confidence = maxOf(existing.confidence, confidence),
                useCount = existing.useCount + 1,
                lastUsed = System.currentTimeMillis()
            )
        } else {
            LearnedCommand(
                original = original,
                learned = learned,
                confidence = confidence,
                useCount = 1,
                lastUsed = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
        }
        
        learnedCommands[original] = newCommand
        successfulLearns++
        
        // Persist to ObjectBox
        persistLearnedCommand(newCommand)
        
        Log.d(TAG, "[$engineType] Learned: $original -> $learned (confidence: $confidence)")
    }
    
    /**
     * Add word to vocabulary cache
     */
    private fun addToVocabulary(word: String) {
        if (vocabularyCache.size >= MAX_VOCABULARY_CACHE) {
            removeOldestVocabularyEntries(10)
        }
        
        val existing = vocabularyCache[word]
        val entry = if (existing != null) {
            existing.copy(
                frequency = existing.frequency + 1,
                lastSeen = System.currentTimeMillis()
            )
        } else {
            VocabularyEntry(
                word = word,
                variations = setOf(word),
                frequency = 1,
                lastSeen = System.currentTimeMillis()
            )
        }
        
        vocabularyCache[word] = entry
    }
    
    /**
     * Find similar command using Levenshtein distance
     */
    private fun findSimilarCommand(input: String, commands: List<String>): Pair<String, Float>? {
        return commands.map { command ->
            val similarity = calculateSimilarity(input, command.lowercase())
            command to similarity
        }.maxByOrNull { it.second }?.takeIf { it.second >= SIMILARITY_THRESHOLD }
    }
    
    /**
     * Calculate string similarity (0.0 to 1.0)
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        val distance = levenshteinDistance(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)
        return if (maxLength == 0) 1.0f else 1.0f - (distance.toFloat() / maxLength)
    }
    
    /**
     * Levenshtein distance calculation
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
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
     * Find best matching command from variations
     */
    private fun findBestCommandMatch(variations: Set<String>, commands: List<String>): String? {
        return commands.firstOrNull { command ->
            variations.any { it.equals(command, ignoreCase = true) }
        }
    }
    
    /**
     * Update usage statistics for learned command
     */
    private fun updateLearnedCommandUsage(command: LearnedCommand) {
        learnedCommands[command.original] = command.copy(
            useCount = command.useCount + 1,
            lastUsed = System.currentTimeMillis()
        )
    }
    
    /**
     * Update usage statistics for vocabulary entry
     */
    private fun updateVocabularyUsage(entry: VocabularyEntry) {
        vocabularyCache[entry.word] = entry.copy(
            frequency = entry.frequency + 1,
            lastSeen = System.currentTimeMillis()
        )
    }
    
    /**
     * Remove oldest learned commands
     */
    private fun removeOldestLearnedCommands(count: Int) {
        val toRemove = learnedCommands.entries
            .sortedBy { it.value.lastUsed }
            .take(count)
            .map { it.key }
        
        toRemove.forEach { learnedCommands.remove(it) }
    }
    
    /**
     * Remove oldest vocabulary entries
     */
    private fun removeOldestVocabularyEntries(count: Int) {
        val toRemove = vocabularyCache.entries
            .sortedBy { it.value.lastSeen }
            .take(count)
            .map { it.key }
        
        toRemove.forEach { vocabularyCache.remove(it) }
    }
    
    /**
     * Persist learned command to Room
     */
    private fun persistLearnedCommand(command: LearnedCommand) {
        scope.launch {
            try {
                learningRepository?.saveLearnedCommand(
                    when(engineType.uppercase()) {
                        "ANDROID_STT" -> EngineType.ANDROID_STT
                        "VIVOKA" -> EngineType.VIVOKA
                        "VOSK" -> EngineType.VOSK
                        "GOOGLE_CLOUD" -> EngineType.GOOGLE_CLOUD
                        "WHISPER" -> EngineType.WHISPER
                        else -> EngineType.ANDROID_STT
                    },
                    command.original,
                    command.learned
                )
            } catch (e: Exception) {
                Log.e(TAG, "[$engineType] Failed to persist learned command", e)
            }
        }
    }
    
    /**
     * Start auto-save job
     */
    private fun startAutoSave() {
        autoSaveJob = scope.launch {
            while (isActive) {
                delay(CACHE_SAVE_INTERVAL)
                saveAllData()
                logStatistics()
            }
        }
    }
    
    /**
     * Save all learning data
     */
    private suspend fun saveAllData() = withContext(Dispatchers.IO) {
        try {
            learningRepository?.let { repository ->
                // Save learned commands
                val commandMap = learnedCommands.mapValues { it.value.learned }
                repository.saveLearnedCommands(when(engineType.uppercase()) {
                    "ANDROID_STT" -> EngineType.ANDROID_STT
                    "VIVOKA" -> EngineType.VIVOKA
                    "VOSK" -> EngineType.VOSK
                    "GOOGLE_CLOUD" -> EngineType.GOOGLE_CLOUD
                    "WHISPER" -> EngineType.WHISPER
                    else -> EngineType.ANDROID_STT
                }, commandMap)
                
                // Save vocabulary cache
                val vocabMap = vocabularyCache.mapValues { true } // Convert to boolean map
                repository.saveVocabularyCache(when(engineType.uppercase()) {
                    "ANDROID_STT" -> EngineType.ANDROID_STT
                    "VIVOKA" -> EngineType.VIVOKA
                    "VOSK" -> EngineType.VOSK
                    "GOOGLE_CLOUD" -> EngineType.GOOGLE_CLOUD
                    "WHISPER" -> EngineType.WHISPER
                    else -> EngineType.ANDROID_STT
                }, vocabMap)
                
                Log.d(TAG, "[$engineType] Saved ${commandMap.size} commands, ${vocabMap.size} vocabulary entries")
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$engineType] Failed to save learning data", e)
        }
    }
    
    /**
     * Log learning statistics
     */
    private fun logStatistics() {
        val hitRate = if (cacheHits + cacheMisses > 0) {
            (cacheHits.toFloat() / (cacheHits + cacheMisses)) * 100
        } else 0f
        
        val learnRate = if (totalLearningAttempts > 0) {
            (successfulLearns.toFloat() / totalLearningAttempts) * 100
        } else 0f
        
        Log.i(TAG, """
            [$engineType] Learning Statistics:
            ├── Learned Commands: ${learnedCommands.size}
            ├── Vocabulary Cache: ${vocabularyCache.size}
            ├── Cache Hit Rate: ${hitRate.toInt()}%
            ├── Learning Success Rate: ${learnRate.toInt()}%
            └── Total Attempts: $totalLearningAttempts
        """.trimIndent())
    }
    
    /**
     * Clear all learning data
     */
    fun clearAllData() {
        learnedCommands.clear()
        vocabularyCache.clear()
        scope.launch {
            learningRepository?.let { repository ->
                val engineTypeValue = when(engineType.uppercase()) {
                    "ANDROID_STT" -> EngineType.ANDROID_STT
                    "VIVOKA" -> EngineType.VIVOKA
                    "VOSK" -> EngineType.VOSK
                    "GOOGLE_CLOUD" -> EngineType.GOOGLE_CLOUD
                    "WHISPER" -> EngineType.WHISPER
                    else -> EngineType.ANDROID_STT
                }
                repository.clearLearnedCommands(engineTypeValue)
                repository.clearVocabularyCache(engineTypeValue)
            }
        }
        Log.w(TAG, "[$engineType] All learning data cleared")
    }
    
    /**
     * Get learning statistics
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "learnedCommands" to learnedCommands.size,
            "vocabularyCache" to vocabularyCache.size,
            "cacheHitRate" to if (cacheHits + cacheMisses > 0) 
                (cacheHits.toFloat() / (cacheHits + cacheMisses)) else 0f,
            "totalLearningAttempts" to totalLearningAttempts,
            "successfulLearns" to successfulLearns
        )
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        autoSaveJob?.cancel()
        scope.launch {
            saveAllData()
            scope.cancel()
        }
        Log.i(TAG, "[$engineType] Learning system destroyed")
    }
}

