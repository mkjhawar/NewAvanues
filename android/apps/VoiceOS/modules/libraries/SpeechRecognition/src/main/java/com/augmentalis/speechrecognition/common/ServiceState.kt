/**
 * ServiceState.kt - Service state enum for speech recognition engines
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-04
 * 
 * Unified service state enum used by all speech recognition engines
 */
package com.augmentalis.speechrecognition.common

/**
 * Service states for speech recognition engines
 */
enum class ServiceState {
    /**
     * Engine not initialized
     */
    IDLE,
    
    /**
     * Engine is initializing
     */
    INITIALIZING,
    
    /**
     * Engine is ready to start recognition
     */
    READY,
    
    /**
     * Engine is actively listening for speech
     */
    LISTENING,
    
    /**
     * Engine is processing audio data
     */
    PROCESSING,
    
    /**
     * Engine is temporarily paused
     */
    PAUSED,
    
    /**
     * Engine encountered an error
     */
    ERROR
}