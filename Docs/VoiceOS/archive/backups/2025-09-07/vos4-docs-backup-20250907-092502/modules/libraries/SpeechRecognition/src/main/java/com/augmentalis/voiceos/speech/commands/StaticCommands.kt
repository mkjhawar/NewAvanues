/**
 * StaticCommands.kt
 * Path: /libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/commands/StaticCommands.kt
 * 
 * Created: 2025-09-05
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Static voice commands that are always available
 * Module: SpeechRecognition
 */

package com.augmentalis.voiceos.speech.commands

/**
 * Static commands that are always recognized regardless of context
 */
object StaticCommands {
    
    // Help commands - always available
    const val HELP = "help"
    const val SHOW_HELP = "show help"
    const val HIDE_HELP = "hide help"
    const val SHOW_COMMANDS = "show commands"
    const val WHAT_CAN_I_SAY = "what can i say"
    
    // Menu commands
    const val SHOW_MENU = "show menu"
    const val HIDE_MENU = "hide menu"
    const val CURSOR_MENU = "cursor menu"
    
    // Emergency commands
    const val STOP = "stop"
    const val CANCEL = "cancel"
    const val CLOSE = "close"
    const val EXIT = "exit"
    
    // Navigation
    const val GO_BACK = "go back"
    const val GO_HOME = "go home"
    
    // Voice control
    const val STOP_LISTENING = "stop listening"
    const val START_LISTENING = "start listening"
    const val PAUSE_VOICE = "pause voice"
    const val RESUME_VOICE = "resume voice"
    
    /**
     * All static commands list
     */
    val ALL_COMMANDS = listOf(
        HELP, SHOW_HELP, HIDE_HELP, SHOW_COMMANDS, WHAT_CAN_I_SAY,
        SHOW_MENU, HIDE_MENU, CURSOR_MENU,
        STOP, CANCEL, CLOSE, EXIT,
        GO_BACK, GO_HOME,
        STOP_LISTENING, START_LISTENING, PAUSE_VOICE, RESUME_VOICE
    )
    
    /**
     * Command aliases mapping
     */
    val COMMAND_ALIASES = mapOf(
        "assistance" to HELP,
        "help me" to HELP,
        "commands" to SHOW_COMMANDS,
        "what commands" to SHOW_COMMANDS,
        "menu" to SHOW_MENU,
        "quit" to EXIT,
        "abort" to CANCEL,
        "back" to GO_BACK,
        "home" to GO_HOME,
        "stop voice" to STOP_LISTENING,
        "start voice" to START_LISTENING
    )
    
    /**
     * Check if a command is a help command
     */
    fun isHelpCommand(command: String): Boolean {
        val normalized = command.lowercase().trim()
        return normalized in listOf(HELP, SHOW_HELP, SHOW_COMMANDS, WHAT_CAN_I_SAY) ||
               COMMAND_ALIASES[normalized] in listOf(HELP, SHOW_HELP, SHOW_COMMANDS)
    }
    
    /**
     * Check if a command is a menu command
     */
    fun isMenuCommand(command: String): Boolean {
        val normalized = command.lowercase().trim()
        return normalized in listOf(SHOW_MENU, CURSOR_MENU) ||
               COMMAND_ALIASES[normalized] in listOf(SHOW_MENU, CURSOR_MENU)
    }
    
    /**
     * Get the canonical command from an alias
     */
    fun getCanonicalCommand(command: String): String {
        val normalized = command.lowercase().trim()
        return COMMAND_ALIASES[normalized] ?: normalized
    }
}