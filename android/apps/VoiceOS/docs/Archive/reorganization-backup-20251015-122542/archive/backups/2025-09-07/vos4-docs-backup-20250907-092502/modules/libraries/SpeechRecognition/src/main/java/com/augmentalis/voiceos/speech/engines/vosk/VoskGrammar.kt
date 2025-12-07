/**
 * VoskGrammar.kt - Grammar generation and constraint management for VOSK engine
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * SOLID Principle: Single Responsibility
 * - Generates grammar JSON for VOSK recognizers
 * - Manages vocabulary testing and validation
 * - Handles command categorization (known/unknown)
 */
package com.augmentalis.voiceos.speech.engines.vosk

import android.util.Log
import com.augmentalis.voiceos.speech.engines.common.CommandCache
import com.google.gson.Gson
import org.vosk.Model
import org.vosk.Recognizer
import java.util.Collections

/**
 * Grammar manager for VOSK engine.
 * Handles grammar generation, vocabulary testing, and command categorization.
 */
class VoskGrammar(
    private val config: VoskConfig,
    private val commandCache: CommandCache
) {
    
    companion object {
        private const val TAG = "VoskGrammar"
        private const val SAMPLE_RATE = 16000.0f
        private const val MAX_GRAMMAR_COMMANDS = 200
        private const val MIN_ESSENTIAL_COMMANDS = 10
    }
    
    // Core state
    private val gson by lazy { Gson() }
    private var model: Model? = null
    
    // Command categorization
    private val knownCommands = Collections.synchronizedList(arrayListOf<String>())
    private val unknownCommands = Collections.synchronizedList(arrayListOf<String>())
    private val vocabularyCache = Collections.synchronizedMap(mutableMapOf<String, Boolean>())
    
    // Essential fallback commands
    private val essentialCommands = listOf(
        "hello", "yes", "no", "stop", "start", "ok", "cancel", 
        "back", "home", "menu", "[unk]"
    )
    
    // Grammar generation state
    private var lastGrammarJson: String? = null
    private var lastGrammarGenTime = 0L
    private var grammarGenerationCount = 0
    private val grammarHistory = mutableListOf<GrammarGeneration>()
    
    /**
     * Initialize grammar manager with model
     */
    fun initialize(voskModel: Model) {
        this.model = voskModel
        Log.i(TAG, "Grammar manager initialized")
    }
    
    /**
     * Generate grammar JSON from current commands
     */
    fun generateGrammarJson(): String {
        return try {
            grammarGenerationCount++
            val startTime = System.currentTimeMillis()
            
            Log.d(TAG, "Generating grammar JSON (generation #$grammarGenerationCount)...")
            
            val commandSet = linkedSetOf<String>()
            
            // Get all commands from cache
            val cachedCommands = commandCache.getAllCommands()
            Log.d(TAG, "Retrieved ${cachedCommands.size} commands from cache")
            
            // Categorize commands if enabled
            if (config.isVocabularyCacheEnabled()) {
                categorizeCommands(cachedCommands)
                
                // Add only known commands for grammar-constrained mode
                if (config.isGrammarConstraintsEnabled()) {
                    commandSet.addAll(knownCommands.take(MAX_GRAMMAR_COMMANDS))
                    Log.d(TAG, "Added ${knownCommands.size} known commands to grammar")
                } else {
                    // In unconstrained mode, add all commands
                    commandSet.addAll(cachedCommands.take(MAX_GRAMMAR_COMMANDS))
                    Log.d(TAG, "Added ${cachedCommands.size} commands to unconstrained grammar")
                }
            } else {
                // No vocabulary testing - add all commands
                commandSet.addAll(cachedCommands.take(MAX_GRAMMAR_COMMANDS))
                Log.d(TAG, "Added ${cachedCommands.size} commands without vocabulary testing")
            }
            
            // Ensure we have essential commands
            commandSet.addAll(essentialCommands)
            
            // Ensure minimum command count
            if (commandSet.size < MIN_ESSENTIAL_COMMANDS) {
                Log.w(TAG, "Grammar has only ${commandSet.size} commands, adding fallbacks...")
                commandSet.addAll(essentialCommands)
            }
            
            val jsonResult = gson.toJson(commandSet.toList())
            val generationTime = System.currentTimeMillis() - startTime
            
            // Record generation
            recordGrammarGeneration(commandSet.size, generationTime, null)
            
            lastGrammarJson = jsonResult
            lastGrammarGenTime = generationTime
            
            Log.i(TAG, "Grammar JSON generated successfully with ${commandSet.size} commands in ${generationTime}ms")
            jsonResult
            
        } catch (e: Exception) {
            val errorMsg = "Failed to generate grammar JSON: ${e.message}"
            recordGrammarGeneration(0, 0, errorMsg)
            Log.e(TAG, errorMsg, e)
            
            // Return safe fallback grammar
            createFallbackGrammar()
        }
    }
    
    /**
     * Categorize commands into known and unknown based on vocabulary testing
     */
    fun categorizeCommands(commands: List<String>) {
        if (!config.isVocabularyCacheEnabled() || model == null) {
            // Skip categorization if caching disabled or no model
            knownCommands.clear()
            unknownCommands.clear()
            knownCommands.addAll(commands)
            return
        }
        
        knownCommands.clear()
        unknownCommands.clear()
        
        Log.i(TAG, "Categorizing ${commands.size} commands...")
        var newCacheEntries = 0
        
        commands.forEach { command ->
            val normalized = command.lowercase().trim()
            
            if (normalized.isNotBlank()) {
                val isKnown = isInVocabulary(normalized)
                
                if (isKnown) {
                    knownCommands.add(normalized)
                } else {
                    unknownCommands.add(normalized)
                    Log.d(TAG, "Unknown command: '$normalized'")
                }
                
                // Count new cache entries
                if (!vocabularyCache.containsKey(normalized)) {
                    newCacheEntries++
                }
            }
        }
        
        Log.i(TAG, "Categorization complete: ${knownCommands.size} known, ${unknownCommands.size} unknown, ${newCacheEntries} new cache entries")
    }
    
    /**
     * Test if word is in VOSK vocabulary
     */
    private fun isInVocabulary(word: String): Boolean {
        // Check cache first
        vocabularyCache[word]?.let { return it }
        
        // Test vocabulary directly if model available
        val result = testVocabularyDirect(word)
        
        // Cache result
        vocabularyCache[word] = result
        
        Log.d(TAG, "Vocabulary test: '$word' → $result")
        return result
    }
    
    /**
     * Direct vocabulary testing with VOSK model
     */
    private fun testVocabularyDirect(word: String): Boolean {
        return try {
            model?.let { voskModel ->
                val testGrammar = gson.toJson(listOf(word, "[unk]"))
                val testRecognizer = Recognizer(voskModel, SAMPLE_RATE, testGrammar)
                testRecognizer.close()
                true
            } ?: false
        } catch (e: Exception) {
            Log.d(TAG, "Vocabulary test failed for '$word': ${e.message}")
            false
        }
    }
    
    /**
     * Pre-test vocabulary for a list of commands
     */
    fun preTestVocabulary(commands: List<String>): Map<String, Boolean> {
        if (!config.isVocabularyCacheEnabled() || model == null) {
            return commands.associateWith { true }
        }
        
        val results = mutableMapOf<String, Boolean>()
        var newTests = 0
        
        Log.d(TAG, "Pre-testing vocabulary for ${commands.size} commands...")
        
        commands.forEach { command ->
            val normalized = command.lowercase().trim()
            if (normalized.isNotBlank()) {
                val result = if (vocabularyCache.containsKey(normalized)) {
                    vocabularyCache[normalized]!!
                } else {
                    val testResult = testVocabularyDirect(normalized)
                    vocabularyCache[normalized] = testResult
                    newTests++
                    testResult
                }
                results[normalized] = result
            }
        }
        
        Log.d(TAG, "Pre-testing complete: ${newTests} new tests performed")
        return results
    }
    
    /**
     * Create fallback grammar for error cases
     */
    private fun createFallbackGrammar(): String {
        return try {
            val fallbackCommands = essentialCommands
            val jsonResult = gson.toJson(fallbackCommands)
            Log.w(TAG, "Using fallback grammar with ${fallbackCommands.size} essential commands")
            jsonResult
        } catch (e: Exception) {
            Log.e(TAG, "Even fallback grammar generation failed", e)
            """["hello", "yes", "no", "[unk]"]""" // Minimal safe fallback
        }
    }
    
    /**
     * Record grammar generation for diagnostics
     */
    private fun recordGrammarGeneration(commandCount: Int, generationTime: Long, error: String?) {
        val generation = GrammarGeneration(
            generationNumber = grammarGenerationCount,
            commandCount = commandCount,
            generationTime = generationTime,
            timestamp = System.currentTimeMillis(),
            error = error,
            grammarConstraintsEnabled = config.isGrammarConstraintsEnabled(),
            vocabularyCacheEnabled = config.isVocabularyCacheEnabled()
        )
        
        grammarHistory.add(generation)
        
        // Keep only last 20 generations
        if (grammarHistory.size > 20) {
            grammarHistory.removeAt(0)
        }
    }
    
    /**
     * Clear vocabulary cache
     */
    fun clearVocabularyCache() {
        vocabularyCache.clear()
        Log.d(TAG, "Vocabulary cache cleared")
    }
    
    /**
     * Get vocabulary cache size
     */
    fun getVocabularyCacheSize(): Int = vocabularyCache.size
    
    /**
     * Get cached vocabulary status for a word
     */
    fun getCachedVocabularyStatus(word: String): Boolean? {
        return vocabularyCache[word.lowercase().trim()]
    }
    
    /**
     * Add word to vocabulary cache
     */
    fun addToVocabularyCache(word: String, isValid: Boolean) {
        val normalized = word.lowercase().trim()
        if (normalized.isNotBlank()) {
            vocabularyCache[normalized] = isValid
            Log.d(TAG, "Added to vocabulary cache: '$normalized' → $isValid")
        }
    }
    
    /**
     * Update vocabulary cache from external data
     */
    fun updateVocabularyCache(updates: Map<String, Boolean>) {
        updates.forEach { (word, isValid) ->
            addToVocabularyCache(word, isValid)
        }
        Log.d(TAG, "Updated vocabulary cache with ${updates.size} entries")
    }
    
    // Getters for state and information
    fun getKnownCommands(): List<String> = knownCommands.toList()
    fun getUnknownCommands(): List<String> = unknownCommands.toList()
    fun getVocabularyCache(): Map<String, Boolean> = vocabularyCache.toMap()
    fun getLastGrammarJson(): String? = lastGrammarJson
    fun getLastGrammarGenTime(): Long = lastGrammarGenTime
    fun getGrammarGenerationCount(): Int = grammarGenerationCount
    
    /**
     * Get grammar status information
     */
    fun getGrammarStatus(): GrammarStatus {
        return GrammarStatus(
            isInitialized = model != null,
            knownCommandsCount = knownCommands.size,
            unknownCommandsCount = unknownCommands.size,
            vocabularyCacheSize = vocabularyCache.size,
            grammarGenerationCount = grammarGenerationCount,
            lastGrammarGenTime = lastGrammarGenTime,
            grammarConstraintsEnabled = config.isGrammarConstraintsEnabled(),
            vocabularyCacheEnabled = config.isVocabularyCacheEnabled()
        )
    }
    
    /**
     * Get diagnostic information
     */
    fun getDiagnostics(): Map<String, Any> {
        return mapOf(
            "initialized" to (model != null),
            "knownCommandsCount" to knownCommands.size,
            "unknownCommandsCount" to unknownCommands.size,
            "vocabularyCacheSize" to vocabularyCache.size,
            "grammarGenerationCount" to grammarGenerationCount,
            "lastGrammarGenTime" to lastGrammarGenTime,
            "grammarConstraintsEnabled" to config.isGrammarConstraintsEnabled(),
            "vocabularyCacheEnabled" to config.isVocabularyCacheEnabled(),
            "essentialCommandsCount" to essentialCommands.size,
            "maxGrammarCommands" to MAX_GRAMMAR_COMMANDS,
            "historySize" to grammarHistory.size
        )
    }
    
    /**
     * Get grammar generation history
     */
    fun getGrammarHistory(): List<GrammarGeneration> = grammarHistory.toList()
    
    /**
     * Data class for grammar status
     */
    data class GrammarStatus(
        val isInitialized: Boolean,
        val knownCommandsCount: Int,
        val unknownCommandsCount: Int,
        val vocabularyCacheSize: Int,
        val grammarGenerationCount: Int,
        val lastGrammarGenTime: Long,
        val grammarConstraintsEnabled: Boolean,
        val vocabularyCacheEnabled: Boolean
    )
    
    /**
     * Data class for grammar generation tracking
     */
    data class GrammarGeneration(
        val generationNumber: Int,
        val commandCount: Int,
        val generationTime: Long,
        val timestamp: Long,
        val error: String?,
        val grammarConstraintsEnabled: Boolean,
        val vocabularyCacheEnabled: Boolean
    )
}