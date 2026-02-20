/**
 * SpeechRecognitionService.kt - Common interface for speech recognition
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-01-18
 *
 * Defines the common interface for speech recognition across all platforms.
 */
package com.augmentalis.speechrecognition

import kotlinx.coroutines.flow.SharedFlow

/**
 * Common interface for speech recognition services.
 * Platform-specific implementations provide actual functionality.
 */
interface SpeechRecognitionService {
    /**
     * Current state of the service
     */
    val state: ServiceState

    /**
     * Flow of recognition results
     */
    val resultFlow: SharedFlow<RecognitionResult>

    /**
     * Flow of errors
     */
    val errorFlow: SharedFlow<SpeechError>

    /**
     * Flow of state changes
     */
    val stateFlow: SharedFlow<ServiceState>

    /**
     * Initialize the service with configuration
     */
    suspend fun initialize(config: SpeechConfig): Result<Unit>

    /**
     * Start listening for speech
     */
    suspend fun startListening(): Result<Unit>

    /**
     * Stop listening
     */
    suspend fun stopListening(): Result<Unit>

    /**
     * Pause listening (can be resumed)
     */
    suspend fun pause(): Result<Unit>

    /**
     * Resume listening after pause
     */
    suspend fun resume(): Result<Unit>

    /**
     * Update commands for matching
     */
    fun setCommands(staticCommands: List<String>, dynamicCommands: List<String> = emptyList())

    /**
     * Update recognition mode
     */
    fun setMode(mode: SpeechMode)

    /**
     * Update language
     */
    fun setLanguage(language: String)

    /**
     * Check if service is currently listening
     */
    fun isListening(): Boolean

    /**
     * Check if service is ready
     */
    fun isReady(): Boolean

    /**
     * Get current configuration
     */
    fun getConfig(): SpeechConfig

    /**
     * Release all resources
     */
    suspend fun release()
}

/**
 * Factory for creating platform-specific speech recognition service.
 * Implemented via expect/actual pattern.
 */
expect fun createSpeechRecognitionService(): SpeechRecognitionService
