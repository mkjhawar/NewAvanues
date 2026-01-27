/**
 * VivokaModel.kt - Model loading and management for Vivoka VSDK engine
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * Updated: 2026-01-27 - Migrated to KMP androidMain
 *
 * Extracted from monolithic VivokaEngine.kt as part of SOLID refactoring
 * Handles dynamic model creation, command compilation, and model switching
 */
package com.augmentalis.speechrecognition.vivoka

import android.content.Context
import android.util.Log
import com.augmentalis.speechrecognition.SpeechErrorCodes
import com.vivoka.vsdk.asr.DynamicModel
import com.vivoka.vsdk.asr.csdk.recognizer.Recognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections

/**
 * Manages dynamic models and command compilation for the Vivoka engine
 */
class VivokaModel(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {

    companion object {
        private const val TAG = "VivokaModel"
        private const val SDK_ASR_ITEM_NAME = "itemName"
        private const val MODEL_COMPILATION_TIMEOUT = 10000L // 10 seconds
        private const val MAX_COMMANDS_PER_MODEL = 1000
        private const val MODEL_RECOVERY_DELAY = 2000L
    }

    // Model components
    private var dynamicModel: DynamicModel? = null
    private var recognizer: Recognizer? = null

    // Model state
    private var currentModelPath: String? = null
    private var currentLanguage: String = ""
    private var isModelReady = false

    // Command management
    private val registeredCommands = Collections.synchronizedList(ArrayList<String>())
    private val compiledCommands = Collections.synchronizedSet(hashSetOf<String>())

    // Thread safety
    private val modelMutex = Mutex()
    private val compilationMutex = Mutex()

    // Model compilation state
    @Volatile
    private var isCompiling = false
    @Volatile
    private var lastCompilationTime = 0L
    @Volatile
    private var compilationErrorCount = 0

    // Error handling
    private var modelErrorListener: ((String, Int) -> Unit)? = null

    /**
     * Initialize dynamic model for the given language
     */
    suspend fun initializeModel(
        recognizer: Recognizer,
        language: String,
        asrModelName: String
    ): Boolean {
        return try {
            Log.d(TAG, "Initializing dynamic model for language: $language")

            this.recognizer = recognizer
            this.currentLanguage = language

            modelMutex.withLock {
                // Create dynamic model
                dynamicModel = com.vivoka.vsdk.asr.csdk.Engine.getInstance()
                    .getDynamicModel(asrModelName)

                if (dynamicModel == null) {
                    throw Exception("Failed to create dynamic model for ASR model: $asrModelName")
                }

                isModelReady = true
                compilationErrorCount = 0

                Log.i(TAG, "Dynamic model initialized successfully for $language")
                true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize dynamic model", e)
            modelErrorListener?.invoke("Model initialization failed: ${e.message}", SpeechErrorCodes.MODEL_LOADING_ERROR)
            isModelReady = false
            false
        }
    }

    /**
     * Set the current model path for the recognizer
     */
    suspend fun setModelPath(modelPath: String): Boolean {
        return try {
            Log.d(TAG, "Setting model path: $modelPath")

            if (recognizer == null) {
                Log.e(TAG, "Cannot set model path - recognizer not initialized")
                return false
            }

            modelMutex.withLock {
                recognizer?.setModel(modelPath, -1)
                currentModelPath = modelPath

                Log.d(TAG, "Model path set successfully: $modelPath")
                true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to set model path", e)
            modelErrorListener?.invoke("Model path setting failed: ${e.message}", SpeechErrorCodes.MODEL_LOADING_ERROR)
            false
        }
    }

    /**
     * Compile dynamic model with commands
     */
    suspend fun compileModelWithCommands(commands: List<String>): Boolean {
        Log.d(TAG, "SPEECH_TEST: compileModelWithCommands commands = $commands")
        if (isCompiling) {
            Log.w(TAG, "Model compilation already in progress")
            return false
        }

        return compilationMutex.withLock {
            try {
                isCompiling = true

                Log.d(TAG, "Compiling model with ${commands.size} commands")

                if (!isModelReady || dynamicModel == null) {
                    Log.e(TAG, "Cannot compile model - not ready")
                    return@withLock false
                }

                // Clear existing slot
                dynamicModel?.clearSlot(SDK_ASR_ITEM_NAME)
                compiledCommands.clear()

                // Process and validate commands
                val processedCommands = processCommandsForCompilation(commands)

                if (processedCommands.isEmpty()) {
                    Log.w(TAG, "No valid commands to compile")
                    return@withLock false
                }

                // Add commands to model
                var addedCount = 0
                for (command in processedCommands) {
                    try {
                        dynamicModel?.addData(SDK_ASR_ITEM_NAME, command, ArrayList<String>())
                        compiledCommands.add(command)
                        addedCount++

                        // Prevent model overload
                        if (addedCount >= MAX_COMMANDS_PER_MODEL) {
                            Log.w(TAG, "Reached maximum commands per model ($MAX_COMMANDS_PER_MODEL)")
                            break
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to add command '$command' to model", e)
                    }
                }

                if (addedCount == 0) {
                    Log.e(TAG, "No commands could be added to model")
                    return@withLock false
                }

                // Compile the model
                val compilationStart = System.currentTimeMillis()
                dynamicModel?.compile()
                val compilationTime = System.currentTimeMillis() - compilationStart

                lastCompilationTime = System.currentTimeMillis()
                compilationErrorCount = 0

                Log.i(TAG, "Model compiled successfully with $addedCount commands in ${compilationTime}ms")

                // Apply the compiled model to recognizer
                currentModelPath?.let { modelPath ->
                    recognizer?.setModel(modelPath, -1)
                }
                Log.d(TAG, "SPEECH_TEST: compileModelWithCommands commands = true")
                true

            } catch (e: Exception) {
                Log.e(TAG, "Model compilation failed", e)
                compilationErrorCount++
                modelErrorListener?.invoke("Model compilation failed: ${e.message}", SpeechErrorCodes.MODEL_LOADING_ERROR)
                false
            } finally {
                isCompiling = false
            }
        }
    }

    /**
     * Process commands for compilation - filter, validate, and deduplicate
     */
    private fun processCommandsForCompilation(commands: List<String>): List<String> {
        val safeCommands = commands.toList() // make a defensive copy
        return safeCommands.asSequence()
            .map { it.trim() }
            .filter { command ->
                // Filter out empty commands
                if (command.isEmpty()) return@filter false

                // Filter out commands with pipe characters (VSDK doesn't support)
                if (command.contains("|")) {
                    Log.w(TAG, "Filtering out command with pipe character: $command")
                    return@filter false
                }

                // Filter out excessively long commands
                if (command.length > 100) {
                    Log.w(TAG, "Filtering out excessively long command: ${command.take(50)}...")
                    return@filter false
                }

                // Filter out commands with invalid characters
                if (command.contains(Regex("[<>{}\\[\\]]"))) {
                    Log.w(TAG, "Filtering out command with invalid characters: $command")
                    return@filter false
                }

                true
            }
            .distinct()
            .take(MAX_COMMANDS_PER_MODEL) // Safety limit
            .toList()
    }

    /**
     * Switch to dictation model
     */
    suspend fun switchToDictationModel(dictationModelPath: String): Boolean {
        return try {
            Log.d(TAG, "Switching to dictation model: $dictationModelPath")

            modelMutex.withLock {
                if (recognizer == null) {
                    Log.e(TAG, "Cannot switch to dictation model - recognizer not available")
                    return@withLock false
                }

                recognizer?.setModel(dictationModelPath, -1)
                currentModelPath = dictationModelPath

                Log.d(TAG, "Switched to dictation model successfully")
                true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch to dictation model", e)
            modelErrorListener?.invoke("Dictation model switch failed: ${e.message}", SpeechErrorCodes.MODEL_LOADING_ERROR)
            false
        }
    }

    /**
     * Switch to command model
     */
    suspend fun switchToCommandModel(commandModelPath: String): Boolean {
        return try {
            Log.d(TAG, "Switching to command model: $commandModelPath")

            modelMutex.withLock {
                if (recognizer == null) {
                    Log.e(TAG, "Cannot switch to command model - recognizer not available")
                    return@withLock false
                }

                recognizer?.setModel(commandModelPath, -1)
                currentModelPath = commandModelPath

                Log.d(TAG, "Switched to command model successfully")
                true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch to command model", e)
            modelErrorListener?.invoke("Command model switch failed: ${e.message}", SpeechErrorCodes.MODEL_LOADING_ERROR)
            false
        }
    }

    /**
     * Register commands for the model
     */
    fun registerCommands(commands: List<String>) {
        Log.d(TAG, "Registering ${commands.size} commands")
        Log.d(TAG, "SPEECH_TEST: registerCommands commands = $commands")
        registeredCommands.clear()
        registeredCommands.addAll(commands)

        Log.d(TAG, "Commands registered successfully")
    }

    /**
     * Get currently registered commands
     */
    fun getRegisteredCommands(): List<String> {
        return ArrayList(registeredCommands)
    }

    /**
     * Get currently compiled commands
     */
    fun getCompiledCommands(): Set<String> {
        return HashSet(compiledCommands)
    }

    /**
     * Check if model is ready for use
     */
    fun isModelReady(): Boolean = isModelReady && dynamicModel != null

    /**
     * Check if model is currently compiling
     */
    fun isCompiling(): Boolean = isCompiling

    /**
     * Get current model path
     */
    fun getCurrentModelPath(): String? = currentModelPath

    /**
     * Get current language
     */
    fun getCurrentLanguage(): String = currentLanguage

    /**
     * Recover from model loading errors
     */
    suspend fun recoverModel(asrModelName: String): Boolean {
        if (recognizer == null) {
            Log.e(TAG, "Cannot recover model - recognizer not available")
            return false
        }

        return try {
            Log.i(TAG, "Attempting model recovery")

            // Wait for any ongoing operations to complete
            delay(MODEL_RECOVERY_DELAY)

            modelMutex.withLock {
                // Clear current state
                isModelReady = false
                dynamicModel = null
                compiledCommands.clear()

                // Re-initialize model
                dynamicModel = com.vivoka.vsdk.asr.csdk.Engine.getInstance()
                    .getDynamicModel(asrModelName)

                if (dynamicModel == null) {
                    Log.e(TAG, "Model recovery failed - could not create dynamic model")
                    return@withLock false
                }

                isModelReady = true
                compilationErrorCount = 0

                // Recompile with basic commands if available
                if (registeredCommands.isNotEmpty()) {
                    val basicCommands = registeredCommands.take(10) // Use first 10 commands
                    Log.d(TAG, "Recompiling with basic commands during recovery")

                    try {
                        compileModelWithCommands(basicCommands)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to recompile during recovery, continuing anyway", e)
                    }
                }

                Log.i(TAG, "Model recovery successful")
                true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Model recovery failed", e)
            false
        }
    }

    /**
     * Perform model diagnostics
     */
    fun performDiagnostics(): Map<String, Any> {
        return try {
            mapOf(
                "isModelReady" to isModelReady,
                "isCompiling" to isCompiling,
                "hasDynamicModel" to (dynamicModel != null),
                "hasRecognizer" to (recognizer != null),
                "currentModelPath" to (currentModelPath ?: "none"),
                "currentLanguage" to currentLanguage,
                "registeredCommandsCount" to registeredCommands.size,
                "compiledCommandsCount" to compiledCommands.size,
                "lastCompilationTime" to lastCompilationTime,
                "compilationErrorCount" to compilationErrorCount,
                "timeSinceLastCompilation" to (System.currentTimeMillis() - lastCompilationTime)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Model diagnostics failed", e)
            mapOf(
                "error" to "Diagnostics failed: ${e.message}",
                "isModelReady" to false
            )
        }
    }

    /**
     * Clear all model data and reset
     */
    suspend fun clearModel() {
        try {
            Log.d(TAG, "Clearing model data")

            modelMutex.withLock {
                // Clear dynamic model slot
                dynamicModel?.clearSlot(SDK_ASR_ITEM_NAME)

                // Clear command data
                registeredCommands.clear()
                compiledCommands.clear()

                // Reset state
                isModelReady = false
                isCompiling = false
                lastCompilationTime = 0L
                currentModelPath = null

                Log.d(TAG, "Model data cleared")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear model", e)
        }
    }

    /**
     * Force model recompilation with current commands
     */
    suspend fun forceRecompilation(): Boolean {
        Log.w(TAG, "Forcing model recompilation")

        if (registeredCommands.isEmpty()) {
            Log.w(TAG, "No commands available for recompilation")
            return false
        }

        return compileModelWithCommands(ArrayList(registeredCommands))
    }

    /**
     * Get model performance metrics
     */
    fun getModelMetrics(): Map<String, Any> {
        val compilationAge = if (lastCompilationTime > 0) {
            System.currentTimeMillis() - lastCompilationTime
        } else {
            -1L
        }

        return mapOf(
            "compilationSuccessRate" to if (compilationErrorCount > 0) {
                val attempts = compilationErrorCount + 1
                (1.0 / attempts) * 100
            } else {
                100.0
            },
            "compilationAge" to compilationAge,
            "modelEfficiency" to if (registeredCommands.isNotEmpty()) {
                (compiledCommands.size.toDouble() / registeredCommands.size) * 100
            } else {
                0.0
            },
            "averageCommandLength" to if (compiledCommands.isNotEmpty()) {
                compiledCommands.map { it.length }.average()
            } else {
                0.0
            }
        )
    }

    /**
     * Reset model to initial state
     */
    suspend fun reset() {
        Log.d(TAG, "Resetting model to initial state")

        try {
            clearModel()

            // Reset all state
            recognizer = null
            dynamicModel = null
            currentLanguage = ""
            compilationErrorCount = 0

            Log.d(TAG, "Model reset completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error during model reset", e)
        }
    }

    /**
     * Set error listener for model-related errors
     */
    fun setErrorListener(listener: (String, Int) -> Unit) {
        this.modelErrorListener = listener
    }

    /**
     * Get compilation timeout based on command count
     */
    private fun getCompilationTimeout(commandCount: Int): Long {
        // Base timeout + additional time for more commands
        return MODEL_COMPILATION_TIMEOUT + (commandCount * 50L)
    }

    /**
     * Validate command format
     */
    private fun isValidCommand(command: String): Boolean {
        return command.isNotEmpty() &&
               command.length <= 100 &&
               !command.contains("|") &&
               !command.contains(Regex("[<>{}\\[\\]]"))
    }
}
