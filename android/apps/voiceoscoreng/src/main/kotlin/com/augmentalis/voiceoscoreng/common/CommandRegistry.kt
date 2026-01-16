package com.augmentalis.voiceoscoreng.common

/**
 * Registry for managing voice commands.
 * Stores and retrieves commands for screens.
 */
object CommandRegistry {
    private val commands = mutableMapOf<String, List<QuantizedCommand>>()

    fun registerCommands(screenHash: String, commandList: List<QuantizedCommand>) {
        commands[screenHash] = commandList
    }

    fun getCommands(screenHash: String): List<QuantizedCommand> =
        commands[screenHash] ?: emptyList()

    fun clearCommands(screenHash: String) {
        commands.remove(screenHash)
    }

    fun clearAll() {
        commands.clear()
    }

    fun getCommandCount(): Int = commands.values.sumOf { it.size }
}
