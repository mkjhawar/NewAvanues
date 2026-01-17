/**
 * ServiceState.kt - Speech recognition service states
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 * Updated: 2026-01-18 - Migrated to KMP commonMain
 */
package com.augmentalis.speechrecognition

/**
 * Represents the current state of a speech recognition service.
 */
enum class ServiceState {
    /**
     * Service has not been initialized yet
     */
    UNINITIALIZED,

    /**
     * Service is currently initializing
     */
    INITIALIZING,

    /**
     * Service is ready to start recognition
     */
    READY,

    /**
     * Service is actively listening for speech
     */
    LISTENING,

    /**
     * Service is processing recognized audio
     */
    PROCESSING,

    /**
     * Service is paused (can be resumed)
     */
    PAUSED,

    /**
     * Service has stopped
     */
    STOPPED,

    /**
     * Service encountered an error
     */
    ERROR,

    /**
     * Service is being destroyed/released
     */
    DESTROYING;

    /**
     * Check if service is in an active state
     */
    fun isActive(): Boolean {
        return this in listOf(LISTENING, PROCESSING)
    }

    /**
     * Check if service can start recognition
     */
    fun canStart(): Boolean {
        return this in listOf(READY, PAUSED, STOPPED)
    }

    /**
     * Check if service is operational
     */
    fun isOperational(): Boolean {
        return this in listOf(READY, LISTENING, PROCESSING, PAUSED)
    }

    /**
     * Check if service needs initialization
     */
    fun needsInitialization(): Boolean {
        return this in listOf(UNINITIALIZED, ERROR, STOPPED)
    }

    /**
     * Get human-readable description
     */
    fun getDescription(): String {
        return when (this) {
            UNINITIALIZED -> "Not initialized"
            INITIALIZING -> "Initializing..."
            READY -> "Ready"
            LISTENING -> "Listening..."
            PROCESSING -> "Processing..."
            PAUSED -> "Paused"
            STOPPED -> "Stopped"
            ERROR -> "Error"
            DESTROYING -> "Shutting down..."
        }
    }
}
