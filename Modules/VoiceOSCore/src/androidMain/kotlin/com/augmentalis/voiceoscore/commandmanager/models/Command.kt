/**
 * Command.kt - CommandManager model
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-22
 *
 * Represents a command to be executed
 */
package com.augmentalis.voiceoscore.commandmanager.models

/**
 * A command to be executed by the CommandManager
 *
 * @param id Unique identifier for the command
 * @param text The command text
 * @param source The source of the command (voice, keyboard, etc.)
 * @param confidence Confidence level (0.0-1.0)
 * @param timestamp When the command was created
 * @param metadata Optional metadata associated with the command
 */
data class Command(
    val id: String,
    val text: String,
    val source: CommandSource,
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
)
