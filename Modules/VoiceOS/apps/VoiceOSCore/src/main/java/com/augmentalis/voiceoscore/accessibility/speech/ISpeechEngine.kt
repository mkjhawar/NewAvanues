/**
 * ISpeechEngine.kt - Speech Engine Interface (Factory Pattern Implementation)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-22
 *
 * Part of SOLID Refactoring Phase 2: Open/Closed Principle (Factory Pattern)
 * Plan: VoiceOS-Plan-SOLID-Refactoring-5221222-V1.md
 *
 * PURPOSE:
 * Unified interface for all speech recognition engines (Vivoka, Google, Azure).
 * Enables polymorphic engine switching without modifying SpeechEngineManager.
 *
 * BENEFITS:
 * - Open/Closed Principle: Add new engines without modifying existing code
 * - Single Responsibility: Each adapter handles one engine's integration
 * - Dependency Inversion: SpeechEngineManager depends on abstraction, not concrete engines
 */
package com.augmentalis.voiceoscore.accessibility.speech

import com.augmentalis.speechrecognition.SpeechConfig

/**
 * Unified interface for all speech recognition engines
 *
 * Defines the contract that all speech engine adapters must implement.
 * This abstraction allows SpeechEngineManager to work with any engine
 * implementation without knowing the underlying details.
 *
 * DESIGN DECISION:
 * - Suspend functions for lifecycle operations (initialize, start, stop)
 * - Synchronous for state queries (isRecognizing)
 * - Returns Boolean for operation success/failure
 *
 * @see VivokaEngineAdapter
 * @see GoogleEngineAdapter
 * @see AzureEngineAdapter
 */
interface ISpeechEngine {

    /**
     * Initialize the speech engine with configuration
     *
     * This is an asynchronous operation that may involve:
     * - Downloading language models
     * - Initializing SDK components
     * - Setting up audio pipelines
     *
     * @param config Speech configuration (language, mode, timeouts, etc.)
     * @return true if initialization succeeded, false otherwise
     *
     * @throws Never throws - returns false on failure
     */
    suspend fun initialize(config: SpeechConfig): Boolean

    /**
     * Start listening for speech input
     *
     * Begins audio capture and speech recognition. The engine will
     * invoke registered callbacks when results are available.
     *
     * PRECONDITION: Engine must be initialized (initialize() returned true)
     *
     * @throws IllegalStateException if engine not initialized
     */
    fun startListening()

    /**
     * Stop listening for speech input
     *
     * Stops audio capture and recognition processing. Safe to call
     * even if engine is not currently listening.
     */
    fun stopListening()

    /**
     * Update dynamic command list at runtime
     *
     * Replaces the current command vocabulary with a new set.
     * Used for context-sensitive voice commands based on current screen.
     *
     * NOTE: This operation may be expensive (100-500ms) for some engines
     * (e.g., Vivoka requires model recompilation). Call asynchronously
     * to avoid blocking the UI thread.
     *
     * @param commands List of voice commands to recognize
     *
     * @throws IllegalStateException if engine not initialized
     */
    suspend fun updateCommands(commands: List<String>)

    /**
     * Update engine configuration at runtime
     *
     * Allows changing settings like language, mode, or timeouts without
     * full reinitialization. Not all engines support all configuration
     * changes at runtime.
     *
     * @param config New configuration data
     *
     * @throws IllegalStateException if engine not initialized
     */
    fun updateConfiguration(config: SpeechConfigurationData)

    /**
     * Check if engine is currently recognizing speech
     *
     * @return true if actively listening, false otherwise
     */
    fun isRecognizing(): Boolean

    /**
     * Get the underlying engine instance
     *
     * Provides access to the wrapped engine for advanced use cases.
     * Use with caution - breaks abstraction and couples to specific engine.
     *
     * @return The underlying engine object (type varies by implementation)
     */
    fun getEngine(): Any?

    /**
     * Clean up engine resources
     *
     * Releases all resources held by the engine (audio, models, SDK).
     * Engine cannot be used after destroy() is called.
     */
    fun destroy()
}
