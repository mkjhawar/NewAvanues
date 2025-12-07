/**
 * RecognitionLearning.kt - ObjectBox entity for speech recognition learning data
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-08-29
 * 
 * Replaces JSON file-based learning with ObjectBox database storage
 * Supports all 5 speech engines: Vivoka, AndroidSTT, Vosk, GoogleCloud, Whisper
 */
package com.augmentalis.datamanager.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * ObjectBox entity for storing speech recognition learning data
 * Replaces the JSON files used by all speech engines
 */
@Entity(
    tableName = "recognition_learning",
    indices = [
        Index(value = ["engine"]),
        Index(value = ["type"]),
        Index(value = ["keyValue"])
    ]
)
data class RecognitionLearning(
    
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    
    /**
     * Engine that created this learning entry
     * Values: "vivoka", "androidstt", "vosk", "googlecloud", "whisper"
     */
    var engine: String = "",
    
    /**
     * Type of learning data
     * Values: "learned_command", "vocabulary_cache"
     */
    var type: String = "",
    
    /**
     * The key - original recognized text or vocabulary term
     */
    var keyValue: String = "",
    
    /**
     * The value - matched command for learned_command type, 
     * boolean as string for vocabulary_cache type
     */
    var mappedValue: String = "",
    
    /**
     * Confidence score when this learning was created (0.0 - 1.0)
     */
    var confidence: Float = 0.0f,
    
    /**
     * Timestamp when this learning entry was created
     */
    var timestamp: Long = System.currentTimeMillis(),
    
    /**
     * Timestamp when this learning entry was last used
     */
    var lastUsed: Long = System.currentTimeMillis(),
    
    /**
     * Number of times this learning has been used
     */
    var usageCount: Int = 0,
    
    /**
     * Additional metadata as JSON string if needed
     */
    var metadata: String = ""
)

// LearningType is now defined in a separate file

