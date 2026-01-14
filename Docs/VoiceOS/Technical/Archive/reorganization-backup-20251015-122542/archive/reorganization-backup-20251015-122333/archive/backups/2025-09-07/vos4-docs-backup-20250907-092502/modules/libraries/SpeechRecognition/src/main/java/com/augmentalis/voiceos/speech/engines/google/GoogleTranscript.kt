/**
 * GoogleTranscript.kt - Google Cloud Speech transcript processing and command matching
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * Handles transcript processing, command matching, learning system, and result validation
 * for Google Cloud Speech Recognition
 */
package com.augmentalis.voiceos.speech.engines.google

import android.content.Context
import android.util.Log
import com.augmentalis.voiceos.speech.api.RecognitionResult
import com.augmentalis.speechrecognition.SpeechEngine
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceos.speech.engines.common.CommandCache
import com.augmentalis.voiceos.speech.engines.common.LearningSystem
import com.augmentalis.datamanager.repositories.RecognitionLearningRepository
import com.augmentalis.datamanager.entities.EngineType
import com.augmentalis.voiceos.speech.engines.common.ResultProcessor
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.Collections

/**
 * Processes and enhances recognition transcripts from Google Cloud Speech.
 * Handles command matching, learning system, and result validation.
 */
class GoogleTranscript(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleTranscript"
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.7f
    }
    
    // Shared components
    private val commandCache = CommandCache()
    private val resultProcessor = ResultProcessor(commandCache)
    
    // Enhanced caching - Room based
    private val learnedCommands = ConcurrentHashMap<String, String>()
    private val vocabularyCache = ConcurrentHashMap<String, Boolean>()
    private val staticCommands = Collections.synchronizedList(arrayListOf<String>())
    private val knownPhrases = Collections.synchronizedList(arrayListOf<String>())
    private val unknownPhrases = Collections.synchronizedList(arrayListOf<String>())
    private lateinit var learningRepository: RecognitionLearningRepository
    
    // Current mode
    private var currentMode = SpeechMode.DYNAMIC_COMMAND
    
    // Coroutine scope
    private val transcriptScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + 
        CoroutineName("GoogleTranscript")
    )
    
    /**
     * Initialize transcript processor with Room learning system
     */
    suspend fun initialize(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Initializing transcript processor with Room learning system...")
                
                // Initialize Room learning system
                learningRepository = RecognitionLearningRepository.getInstance(context)
                learningRepository.initialize()
                
                // Initialize enhanced caching from Room
                loadLearnedCommands()
                loadVocabularyCache()
                
                Log.i(TAG, "Transcript processor initialized successfully")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize transcript processor", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Process recognition result with enhanced multi-tier matching
     */
    suspend fun processRecognitionResult(
        text: String,
        confidence: Float,
        isFinal: Boolean,
        alternatives: List<String> = emptyList(),
        metadata: Map<String, Any> = emptyMap()
    ): RecognitionResult? {
        try {
            // Skip empty results
            if (text.isBlank()) return null
            
            Log.d(TAG, "Processing result: '$text' (confidence: $confidence, final: $isFinal)")
            
            // Enhanced processing based on mode with multi-tier matching
            var finalText = text
            var finalConfidence = confidence
            
            if (currentMode != SpeechMode.DICTATION) {
                // Command mode - enhanced multi-tier matching
                val normalizedText = resultProcessor.normalizeText(text.lowercase())
                
                // Tier 1: Exact command cache match
                commandCache.findMatch(normalizedText)?.let { match ->
                    finalText = match
                    finalConfidence = 0.95f
                    Log.d(TAG, "Tier 1 - Command cache matched: $match")
                    return@let
                }
                
                // Tier 2: Learned command match
                learnedCommands[normalizedText]?.let { match ->
                    finalText = match
                    finalConfidence = 0.90f
                    Log.d(TAG, "Tier 2 - Learned command matched: $match")
                    return@let
                }
                
                // Tier 3: Similarity matching with all available commands
                findSimilarCommand(normalizedText, commandCache.getAllCommands())?.let { match ->
                    finalText = match
                    finalConfidence = 0.85f
                    Log.d(TAG, "Tier 3 - Similarity matched: '$normalizedText' → '$match'")
                    return@let
                }
                
                Log.d(TAG, "No command match found for: '$normalizedText'")
            }
            
            // Create final recognition result
            val result = RecognitionResult(
                text = finalText,
                originalText = text,
                confidence = finalConfidence,
                timestamp = System.currentTimeMillis(),
                isPartial = !isFinal,
                isFinal = isFinal,
                alternatives = alternatives,
                engine = SpeechEngine.GOOGLE_CLOUD.name,
                mode = currentMode.name,
                metadata = metadata + mapOf("processed" to true)
            )
            
            // Check if should accept
            return if (resultProcessor.shouldAccept(result)) {
                result
            } else {
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing recognition result", e)
            return null
        }
    }
    
    /**
     * Set static commands for command matching with enhanced caching
     */
    fun setStaticCommands(commands: List<String>) {
        Log.i(TAG, "Setting ${commands.size} static commands for enhanced caching")
        
        // Store in both cache systems
        commandCache.setStaticCommands(commands)
        
        staticCommands.clear()
        staticCommands.addAll(commands.map { it.lowercase().trim() }.filter { it.isNotBlank() })
        
        // Pre-test and categorize commands
        transcriptScope.launch(Dispatchers.IO) {
            categorizeCommands(staticCommands)
            preTestStaticCommands()
        }
        
        Log.d(TAG, "Set ${commands.size} static commands with enhanced phrase hints")
    }
    
    /**
     * Set dynamic commands from UI with advanced processing
     */
    fun setDynamicCommands(commands: List<String>) {
        Log.i(TAG, "GoogleCloud setDynamicCommands ENTRY - Received ${commands.size} commands")
        
        // AUTO-LOWERCASE ALL INCOMING COMMANDS (like Vosk)
        val lowercasedCommands = commands.map { it.lowercase().trim() }.filter { it.isNotBlank() }
        
        commandCache.setDynamicCommands(lowercasedCommands)
        
        // Categorize into known/unknown for enhanced processing
        transcriptScope.launch(Dispatchers.IO) {
            categorizeCommands(lowercasedCommands)
        }
        
        Log.i(TAG, "Set ${commands.size} dynamic commands with enhanced phrase hints")
    }
    
    /**
     * Set recognition mode
     */
    fun setMode(mode: SpeechMode) {
        currentMode = mode
        resultProcessor.setMode(mode)
    }
    
    /**
     * Enhanced command categorization (like Vosk)
     */
    private suspend fun categorizeCommands(commands: List<String>) {
        knownPhrases.clear()
        unknownPhrases.clear()
        
        Log.i(TAG, "Categorizing ${commands.size} commands for phrase optimization...")
        
        commands.forEach { command ->
            if (isInVocabulary(command)) {
                knownPhrases.add(command)
            } else {
                unknownPhrases.add(command)
                Log.d(TAG, "Unknown phrase for enhanced processing: '$command'")
            }
        }
        
        Log.i(TAG, "Known phrases: ${knownPhrases.size}, Unknown phrases: ${unknownPhrases.size}")
    }
    
    /**
     * Test if phrase exists in Cloud Speech vocabulary (simulated)
     */
    private suspend fun isInVocabulary(phrase: String): Boolean {
        return vocabularyCache[phrase] ?: run {
            // Simulate vocabulary testing (in real implementation, this could use Cloud Speech API)
            val result = testPhraseVocabulary(phrase)
            vocabularyCache[phrase] = result
            saveVocabularyCache()
            Log.d(TAG, "Vocabulary test cached: '$phrase' → $result")
            result
        }
    }
    
    /**
     * Simulate phrase vocabulary testing
     */
    private fun testPhraseVocabulary(phrase: String): Boolean {
        // Enhanced heuristics for Cloud Speech vocabulary
        return when {
            phrase.length < 2 -> false
            phrase.contains("[unk]") -> false
            phrase.matches(Regex("^[a-zA-Z\\s]+$")) -> true  // Basic English words
            phrase.contains("@") || phrase.contains(".") -> false  // Email/domain patterns
            phrase.length > 50 -> false  // Too long for single phrase
            else -> true  // Default to true for Cloud Speech flexibility
        }
    }
    
    /**
     * Enhanced similarity matching (from Vosk implementation)
     */
    private fun findSimilarCommand(text: String, commands: List<String>): String? {
        if (commands.isEmpty()) return null
        
        val normalizedText = text.lowercase().trim()
        
        // First check learned commands cache
        learnedCommands[normalizedText]?.let { return it }
        
        // Simple similarity matching (enhanced version would use Levenshtein distance)
        commands.forEach { command ->
            val normalizedCommand = command.lowercase().trim()
            
            // Exact match
            if (normalizedText == normalizedCommand) {
                return command
            }
            
            // Contains match
            if (normalizedText.contains(normalizedCommand) || normalizedCommand.contains(normalizedText)) {
                saveLearnedCommand(normalizedText, command)
                return command
            }
            
            // Word boundary match
            val textWords = normalizedText.split("\\s+")
            val commandWords = normalizedCommand.split("\\s+")
            
            if (textWords.intersect(commandWords.toSet()).size >= maxOf(1, minOf(textWords.size, commandWords.size) / 2)) {
                saveLearnedCommand(normalizedText, command)
                return command
            }
        }
        
        return null
    }
    
    /**
     * Load learned commands from Room database
     */
    private suspend fun loadLearnedCommands() {
        try {
            val loadedCommands = learningRepository.getLearnedCommands(EngineType.GOOGLE_CLOUD)
            learnedCommands.clear()
            learnedCommands.putAll(loadedCommands)
            Log.i(TAG, "🧠 GoogleCloud: Loaded ${learnedCommands.size} learned commands from Room")
        } catch (e: Exception) {
            Log.e(TAG, "🧠 GoogleCloud: Failed to load learned commands from Room: ${e.message}")
        }
    }
    
    /**
     * Save learned command to Room database
     */
    private fun saveLearnedCommand(recognized: String, matched: String) {
        learnedCommands[recognized] = matched
        Log.d(TAG, "🧠 GoogleCloud Learning: '$recognized' → '$matched'")
        
        transcriptScope.launch(Dispatchers.IO) {
            try {
                learningRepository.saveLearnedCommand(EngineType.GOOGLE_CLOUD, recognized, matched)
                Log.d(TAG, "🧠 GoogleCloud: Saved learned command to Room")
            } catch (e: Exception) {
                Log.e(TAG, "🧠 GoogleCloud: Failed to save learned command to Room: ${e.message}")
            }
        }
    }
    
    /**
     * Load vocabulary cache from Room database
     */
    private suspend fun loadVocabularyCache() {
        try {
            val loadedCache = learningRepository.getVocabularyCache(EngineType.GOOGLE_CLOUD)
            vocabularyCache.clear()
            vocabularyCache.putAll(loadedCache)
            Log.i(TAG, "🧠 GoogleCloud: Loaded ${vocabularyCache.size} vocabulary cache entries from Room")
        } catch (e: Exception) {
            Log.e(TAG, "🧠 GoogleCloud: Failed to load vocabulary cache from Room: ${e.message}")
        }
    }
    
    /**
     * Save vocabulary cache to Room database
     */
    private suspend fun saveVocabularyCache() {
        try {
            learningRepository.saveVocabularyCache(EngineType.GOOGLE_CLOUD, vocabularyCache.toMap())
            Log.d(TAG, "🧠 GoogleCloud: Saved vocabulary cache to Room")
        } catch (e: Exception) {
            Log.e(TAG, "🧠 GoogleCloud: Failed to save vocabulary cache to Room: ${e.message}")
        }
    }
    
    /**
     * Pre-test static commands for vocabulary optimization
     */
    private suspend fun preTestStaticCommands() {
        try {
            Log.i(TAG, "Pre-testing ${staticCommands.size} static commands...")
            var newCacheEntries = 0
            
            staticCommands.forEach { command ->
                if (!vocabularyCache.containsKey(command)) {
                    val isKnown = testPhraseVocabulary(command)
                    vocabularyCache[command] = isKnown
                    newCacheEntries++
                    Log.d(TAG, "Cached static command: '$command' → $isKnown")
                }
            }
            
            if (newCacheEntries > 0) {
                saveVocabularyCache()
                Log.i(TAG, "Added $newCacheEntries new vocabulary cache entries")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pre-testing static commands: ${e.message}")
        }
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "commandCache" to commandCache.getStats(),
            "learnedCommands" to learnedCommands.size,
            "vocabularyCache" to vocabularyCache.size,
            "staticCommands" to staticCommands.size,
            "knownPhrases" to knownPhrases.size,
            "unknownPhrases" to unknownPhrases.size,
            "currentMode" to currentMode.name
        )
    }
    
    /**
     * Clear all caches
     */
    fun clearCache() {
        commandCache.clear()
        learnedCommands.clear()
        vocabularyCache.clear()
        staticCommands.clear()
        knownPhrases.clear()
        unknownPhrases.clear()
        Log.i(TAG, "All caches cleared")
    }
    
    /**
     * Update configuration for language change
     */
    suspend fun updateForLanguageChange(): Result<Unit> {
        return try {
            Log.i(TAG, "Updating transcript processor for language change")
            
            // Clear vocabulary cache for new language
            vocabularyCache.clear()
            loadVocabularyCache()
            
            // Re-test static commands for new language
            if (staticCommands.isNotEmpty()) {
                transcriptScope.launch(Dispatchers.IO) {
                    preTestStaticCommands()
                }
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update for language change", e)
            Result.failure(e)
        }
    }
    
    /**
     * Shutdown transcript processor
     */
    fun shutdown() {
        Log.i(TAG, "Shutting down transcript processor...")
        
        // Cancel coroutines
        transcriptScope.cancel()
        
        // Close learning repository
        if (::learningRepository.isInitialized) {
            // Room handles connection management automatically
        }
        
        // Clear all caches
        clearCache()
        
        Log.i(TAG, "Transcript processor shutdown complete")
    }
}