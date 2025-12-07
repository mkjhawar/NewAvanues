/**
 * RecognitionStatus.kt - Test double for speech recognition status enum
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Test enum for recognition status, used in unit tests to track
 * the current state of speech recognition operations.
 */
package com.augmentalis.speechrecognition

/**
 * Test enum representing different states of speech recognition
 * This is a test double that provides status tracking for testing
 */
enum class RecognitionStatus {
    /**
     * Recognition system is idle and not performing any operations
     */
    IDLE,
    
    /**
     * Recognition system is initializing
     */
    INITIALIZING,
    
    /**
     * Recognition system is ready to start listening
     */
    READY,
    
    /**
     * Recognition system is actively listening for speech
     */
    LISTENING,
    
    /**
     * Recognition system is processing captured audio
     */
    PROCESSING,
    
    /**
     * Recognition system has encountered an error
     */
    ERROR,
    
    /**
     * Recognition system has timed out waiting for speech
     */
    TIMEOUT,
    
    /**
     * Recognition system is being destroyed/cleaned up
     */
    DESTROYING
}