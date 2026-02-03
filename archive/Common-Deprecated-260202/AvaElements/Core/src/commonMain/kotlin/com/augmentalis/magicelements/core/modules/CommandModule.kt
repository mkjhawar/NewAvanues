package com.augmentalis.magicelements.core.modules

import com.augmentalis.magicelements.core.base.BaseModule
import com.augmentalis.magicelements.core.base.ModuleTier

/**
 * Command module delegate interface.
 * Platform implementations provide the actual command execution logic.
 */
interface CommandModuleDelegate {
    suspend fun execute(command: String): Map<String, Any?>
    fun listCommands(): List<Map<String, Any?>>
    fun searchCommands(query: String): List<Map<String, Any?>>
    fun suggestCommands(partial: String): List<String>
    fun getHistory(limit: Int): List<Map<String, Any?>>
    fun registerCommand(config: Map<String, Any?>): String
    fun unregisterCommand(id: String): Boolean
    fun createMacro(config: Map<String, Any?>): String
    suspend fun runMacro(name: String): Map<String, Any?>
    fun listMacros(): List<Map<String, Any?>>
    fun deleteMacro(name: String): Boolean
}

/**
 * CommandModule provides voice command execution and management capabilities.
 *
 * Features:
 * - Execute voice commands
 * - List and search available commands
 * - Get command suggestions
 * - Access command history
 * - Register custom commands
 * - Create and run macros
 *
 * @property delegate Platform-specific command implementation
 */
class CommandModule(
    private val delegate: CommandModuleDelegate
) : BaseModule(
    name = "command",
    version = "1.0.0",
    minimumTier = ModuleTier.DATA
) {

    override fun initialize() {
        // Register DATA tier methods
        registerMethod(
            name = "execute",
            tier = ModuleTier.DATA,
            handler = { args ->
                val commandText = args["commandText"] as? String
                    ?: throw IllegalArgumentException("commandText is required")
                delegate.execute(commandText)
            }
        )

        registerMethod(
            name = "list",
            tier = ModuleTier.DATA,
            handler = {
                mapOf("commands" to delegate.listCommands())
            }
        )

        registerMethod(
            name = "search",
            tier = ModuleTier.DATA,
            handler = { args ->
                val query = args["query"] as? String
                    ?: throw IllegalArgumentException("query is required")
                mapOf("results" to delegate.searchCommands(query))
            }
        )

        registerMethod(
            name = "suggest",
            tier = ModuleTier.DATA,
            handler = { args ->
                val partial = args["partial"] as? String
                    ?: throw IllegalArgumentException("partial is required")
                mapOf("suggestions" to delegate.suggestCommands(partial))
            }
        )

        registerMethod(
            name = "history",
            tier = ModuleTier.DATA,
            handler = { args ->
                val limit = (args["limit"] as? Number)?.toInt() ?: 10
                mapOf("history" to delegate.getHistory(limit))
            }
        )

        // Register LOGIC tier methods
        registerMethod(
            name = "register",
            tier = ModuleTier.LOGIC,
            handler = { args ->
                @Suppress("UNCHECKED_CAST")
                val config = args["config"] as? Map<String, Any?>
                    ?: throw IllegalArgumentException("config is required")
                mapOf("id" to delegate.registerCommand(config))
            }
        )

        registerMethod(
            name = "unregister",
            tier = ModuleTier.LOGIC,
            handler = { args ->
                val id = args["id"] as? String
                    ?: throw IllegalArgumentException("id is required")
                mapOf("success" to delegate.unregisterCommand(id))
            }
        )

        registerMethod(
            name = "macro.create",
            tier = ModuleTier.LOGIC,
            handler = { args ->
                @Suppress("UNCHECKED_CAST")
                val config = args["config"] as? Map<String, Any?>
                    ?: throw IllegalArgumentException("config is required")
                mapOf("name" to delegate.createMacro(config))
            }
        )

        registerMethod(
            name = "macro.run",
            tier = ModuleTier.LOGIC,
            handler = { args ->
                val name = args["name"] as? String
                    ?: throw IllegalArgumentException("name is required")
                delegate.runMacro(name)
            }
        )

        registerMethod(
            name = "macro.list",
            tier = ModuleTier.DATA,
            handler = {
                mapOf("macros" to delegate.listMacros())
            }
        )

        registerMethod(
            name = "macro.delete",
            tier = ModuleTier.LOGIC,
            handler = { args ->
                val name = args["name"] as? String
                    ?: throw IllegalArgumentException("name is required")
                mapOf("success" to delegate.deleteMacro(name))
            }
        )
    }

    override fun cleanup() {
        // No cleanup required
    }
}
