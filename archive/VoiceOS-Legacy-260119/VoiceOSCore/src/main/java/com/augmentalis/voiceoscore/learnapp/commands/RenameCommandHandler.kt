/**
 * RenameCommandHandler.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Handles on-demand command renaming via voice commands like "Rename X to Y".
 * Provides validation and TTS feedback for renaming operations.
 */
package com.augmentalis.voiceoscore.learnapp.commands

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Handles command renaming operations
 *
 * @param context Application context for TTS initialization
 */
class RenameCommandHandler(private val context: Context) {

    companion object {
        private const val TAG = "RenameCommandHandler"
        private const val MIN_COMMAND_LENGTH = 2
        private const val MAX_COMMAND_LENGTH = 50
    }

    private var tts: TextToSpeech? = null
    private var ttsInitialized = false

    init {
        // Initialize TTS for feedback
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsInitialized = true
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    /**
     * Handle a rename command request
     *
     * @param oldName Current command name
     * @param newName Desired new command name
     * @return RenameResult indicating success or failure
     */
    suspend fun handleRename(oldName: String, newName: String): RenameResult = withContext(Dispatchers.Default) {
        try {
            // Validate the old name
            if (!validateName(oldName)) {
                return@withContext RenameResult.Failure("Invalid old command name: $oldName")
            }

            // Validate the new name
            val validationResult = validateName(newName)
            if (!validationResult) {
                return@withContext RenameResult.Failure("Invalid new command name: $newName")
            }

            // Check if names are the same
            if (oldName.equals(newName, ignoreCase = true)) {
                return@withContext RenameResult.Failure("Old and new names are the same")
            }

            // Perform the rename (this is a placeholder - actual implementation would update database)
            Log.d(TAG, "Renaming command from '$oldName' to '$newName'")

            // Provide TTS feedback
            speak("Command renamed from $oldName to $newName")

            RenameResult.Success(oldName, newName)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling rename", e)
            RenameResult.Failure("Error: ${e.message}")
        }
    }

    /**
     * Validate a command name
     *
     * @param name Command name to validate
     * @return true if valid, false otherwise
     */
    fun validateName(name: String): Boolean {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> {
                Log.w(TAG, "Command name is empty")
                false
            }
            trimmed.length < MIN_COMMAND_LENGTH -> {
                Log.w(TAG, "Command name too short: $trimmed")
                false
            }
            trimmed.length > MAX_COMMAND_LENGTH -> {
                Log.w(TAG, "Command name too long: $trimmed")
                false
            }
            !trimmed.matches(Regex("^[a-zA-Z0-9\\s]+$")) -> {
                Log.w(TAG, "Command name contains invalid characters: $trimmed")
                false
            }
            else -> true
        }
    }

    /**
     * Update a command in the database
     *
     * This is a placeholder method - actual implementation would need access
     * to the database/repository to perform the update.
     *
     * @param oldName Old command name
     * @param newName New command name
     * @return true if update successful
     */
    suspend fun updateCommand(oldName: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        // TODO: Implement actual database update
        // This would need to:
        // 1. Find the command by oldName in the database
        // 2. Update its name to newName
        // 3. Update any related metadata
        Log.d(TAG, "updateCommand called for '$oldName' -> '$newName' (not yet implemented)")
        true
    }

    /**
     * Speak text using TTS if initialized
     *
     * @param text Text to speak
     */
    private fun speak(text: String) {
        if (ttsInitialized && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.w(TAG, "TTS not initialized, cannot speak: $text")
        }
    }

    /**
     * Shutdown TTS and cleanup resources
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsInitialized = false
        Log.d(TAG, "RenameCommandHandler shut down")
    }
}

/**
 * Result of a rename operation
 */
sealed class RenameResult {
    /**
     * Rename succeeded
     */
    data class Success(val oldName: String, val newName: String) : RenameResult()

    /**
     * Rename failed
     */
    data class Failure(val reason: String) : RenameResult()
}
