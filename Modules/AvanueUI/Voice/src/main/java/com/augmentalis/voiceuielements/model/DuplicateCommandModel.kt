/**
 * DuplicateCommandModel.kt - Data model for command disambiguation
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-09-04
 * 
 * Represents a command option in disambiguation overlay with position and metadata.
 */
package com.augmentalis.voiceuielements.model

import android.graphics.RectF
import androidx.compose.runtime.Immutable

/**
 * Command disambiguation model for numbered selection
 */
@Immutable
data class DuplicateCommandModel(
    val number: Int,
    val displayText: String,
    val command: String,
    val screenBounds: RectF,
    val selectionCommands: List<String>,
    val isClickable: Boolean = true,
    val description: String = "",
    val confidence: Float = 1.0f
) {
    companion object {
        /**
         * Create disambiguation model from voice command
         */
        fun fromVoiceCommand(
            number: Int,
            command: String,
            bounds: RectF,
            description: String = "",
            isClickable: Boolean = true
        ): DuplicateCommandModel {
            return DuplicateCommandModel(
                number = number,
                displayText = number.toString(),
                command = command,
                screenBounds = bounds,
                selectionCommands = generateSelectionCommands(number),
                isClickable = isClickable,
                description = description
            )
        }
        
        /**
         * Generate selection commands for a number
         */
        private fun generateSelectionCommands(number: Int): List<String> {
            return listOf(
                "select $number",
                "tap $number", 
                "click $number",
                "choose $number",
                "$number"
            )
        }
    }
    
    /**
     * Check if spoken command matches this option
     */
    fun matchesCommand(spokenCommand: String): Boolean {
        return selectionCommands.any { 
            spokenCommand.equals(it, ignoreCase = true) 
        }
    }
    
    /**
     * Get center point of the command bounds
     */
    fun getCenterPoint(): Pair<Float, Float> {
        return Pair(screenBounds.centerX(), screenBounds.centerY())
    }
    
    /**
     * Get optimal label position (top-left of bounds with offset)
     */
    fun getLabelPosition(offsetX: Float = 10f, offsetY: Float = 10f): Pair<Float, Float> {
        return Pair(
            screenBounds.left + offsetX,
            screenBounds.top + offsetY
        )
    }
}

/**
 * Command disambiguation result
 */
sealed class DisambiguationResult {
    object NotFound : DisambiguationResult()
    object Cancelled : DisambiguationResult()
    data class Selected(val model: DuplicateCommandModel) : DisambiguationResult()
}

/**
 * Command disambiguation state
 */
data class DisambiguationState(
    val commands: List<DuplicateCommandModel> = emptyList(),
    val isVisible: Boolean = false,
    val timeoutSeconds: Int = 30,
    val allowClickDismiss: Boolean = true
) {
    val hasCommands: Boolean get() = commands.isNotEmpty()
    val commandCount: Int get() = commands.size
}