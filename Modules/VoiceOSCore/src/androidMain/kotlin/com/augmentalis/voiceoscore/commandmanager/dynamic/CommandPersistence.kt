/**
 * CommandPersistence.kt - Room database entities for command persistence
 *
 * Part of Week 4 implementation - Dynamic Command Registration
 * Provides persistent storage for dynamically registered commands
 *
 * Note: This is a stub implementation showing the structure.
 * Full Room integration requires KSP plugin configuration in build.gradle.kts
 *
 * @since VOS4 Week 4
 */

package com.augmentalis.voiceoscore.commandmanager.dynamic

/**
 * Database entity for persisting voice commands
 *
 * This would be a Room @Entity in full implementation
 * For now, it's a data class that can be used with preferences or JSON storage
 */
data class CommandEntity(
    val id: String,
    val phrases: String, // JSON-encoded list
    val priority: Int,
    val namespace: String,
    val description: String,
    val category: String,
    val enabled: Boolean,
    val createdAt: Long,
    val lastUsed: Long,
    val usageCount: Long,
    val metadata: String // JSON-encoded map
) {
    companion object {
        /**
         * Convert VoiceCommandData to entity
         */
        fun fromVoiceCommandData(data: VoiceCommandData): CommandEntity {
            return CommandEntity(
                id = data.id,
                phrases = data.phrases.joinToString("|"),
                priority = data.priority,
                namespace = data.namespace,
                description = data.description,
                category = data.category.name,
                enabled = data.enabled,
                createdAt = data.createdAt,
                lastUsed = data.lastUsed,
                usageCount = data.usageCount,
                metadata = data.metadata.entries.joinToString("|") { "${it.key}:${it.value}" }
            )
        }
    }

    /**
     * Convert entity to VoiceCommandData
     */
    fun toVoiceCommandData(): VoiceCommandData {
        val phraseList = phrases.split("|").filter { it.isNotBlank() }
        val metadataMap = metadata.split("|")
            .filter { it.isNotBlank() && it.contains(":") }
            .associate {
                val (key, value) = it.split(":", limit = 2)
                key to value
            }

        return VoiceCommandData(
            id = id,
            phrases = phraseList,
            priority = priority,
            namespace = namespace,
            description = description,
            category = CommandCategory.valueOf(category),
            enabled = enabled,
            createdAt = createdAt,
            lastUsed = lastUsed,
            usageCount = usageCount,
            metadata = metadataMap
        )
    }
}

/**
 * Room database-based storage for commands
 *
 * Uses CommandDatabase for persistent storage of dynamic commands
 */
class CommandStorage(
    private val context: android.content.Context,
    private val preferencesName: String = "dynamic_commands"
) {
    private val database by lazy {
        com.augmentalis.voiceoscore.commandmanager.database.CommandDatabase.getInstance(context)
    }

    private val dao by lazy {
        database.voiceCommandDao()
    }

    /**
     * Save a command to storage
     */
    suspend fun saveCommand(command: VoiceCommandData): Result<Unit> {
        return try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // Convert VoiceCommandData to VoiceCommandEntity
                val entity = com.augmentalis.voiceoscore.commandmanager.database.sqldelight.VoiceCommandEntity(
                    id = command.id,
                    locale = java.util.Locale.getDefault().toLanguageTag(),
                    primaryText = command.phrases.firstOrNull() ?: "",
                    synonyms = org.json.JSONArray(command.phrases.drop(1)).toString(),
                    description = command.description,
                    category = command.category.name,
                    priority = command.priority,
                    isFallback = false,
                    createdAt = command.createdAt
                )

                dao.insert(entity)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("CommandStorage", "Failed to save command: ${command.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Load all commands from storage
     */
    suspend fun loadAllCommands(): Result<List<VoiceCommandData>> {
        return try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val deviceLocale = java.util.Locale.getDefault().toLanguageTag()
                val entities = dao.getCommandsForLocale(deviceLocale)

                val commands = entities.map { entity ->
                    val phrases = mutableListOf(entity.primaryText)
                    phrases.addAll(com.augmentalis.voiceoscore.commandmanager.database.sqldelight.VoiceCommandEntity.parseSynonyms(entity.synonyms))

                    VoiceCommandData(
                        id = entity.id,
                        phrases = phrases,
                        priority = entity.priority,
                        namespace = "custom", // Default namespace for stored commands
                        description = entity.description,
                        category = CommandCategory.valueOf(entity.category),
                        enabled = true,
                        createdAt = entity.createdAt,
                        lastUsed = 0L,
                        usageCount = 0L,
                        metadata = emptyMap()
                    )
                }

                Result.success(commands)
            }
        } catch (e: Exception) {
            android.util.Log.e("CommandStorage", "Failed to load commands", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a command from storage
     */
    suspend fun deleteCommand(commandId: String): Result<Unit> {
        return try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val deviceLocale = java.util.Locale.getDefault().toLanguageTag()
                val deletedCount = dao.deleteCommand(commandId, deviceLocale)

                if (deletedCount > 0) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Command not found: $commandId"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CommandStorage", "Failed to delete command: $commandId", e)
            Result.failure(e)
        }
    }

    /**
     * Delete all commands in a namespace
     * Note: Current schema doesn't track namespace, so this deletes all custom commands
     */
    suspend fun deleteNamespace(namespace: String): Result<Int> {
        // Schema does not have a namespace column — namespace-scoped deletion not possible.
        // Return failure so callers know the operation did not execute.
        android.util.Log.w("CommandStorage", "deleteNamespace('$namespace') not supported — schema lacks namespace column")
        return Result.failure(UnsupportedOperationException("Namespace-scoped deletion not supported by current schema"))
    }

    /**
     * Update command usage statistics
     * Note: VoiceCommandEntity doesn't have usage stats, this is a no-op
     */
    suspend fun updateUsageStats(
        commandId: String,
        lastUsed: Long,
        usageCount: Long
    ): Result<Unit> {
        return try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // VoiceCommandEntity doesn't track usage stats
                // Usage stats are tracked in CommandUsageEntity via CommandUsageDao
                val usageDao = database.commandUsageDao()
                val usageEntity = com.augmentalis.voiceoscore.commandmanager.database.sqldelight.CommandUsageEntity(
                    commandId = commandId,
                    locale = java.util.Locale.getDefault().toLanguageTag(),
                    timestamp = lastUsed,
                    userInput = "",  // Not tracked in this path
                    matchType = "DIRECT",  // Direct usage update
                    success = true,
                    executionTimeMs = 0,  // Not tracked in this path
                    contextApp = null
                )
                usageDao.recordUsage(usageEntity)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("CommandStorage", "Failed to update usage stats: $commandId", e)
            Result.failure(e)
        }
    }

    /**
     * Check if a command exists
     */
    suspend fun commandExists(commandId: String): Result<Boolean> {
        return try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val deviceLocale = java.util.Locale.getDefault().toLanguageTag()
                val entity = dao.getCommand(commandId, deviceLocale)
                Result.success(entity != null)
            }
        } catch (e: Exception) {
            android.util.Log.e("CommandStorage", "Failed to check command existence: $commandId", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing command
     */
    suspend fun updateCommand(command: VoiceCommandData): Result<Unit> {
        return try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val deviceLocale = java.util.Locale.getDefault().toLanguageTag()
                val existing = dao.getCommand(command.id, deviceLocale)

                if (existing != null) {
                    // Update existing entity
                    val updated = existing.copy(
                        primaryText = command.phrases.firstOrNull() ?: existing.primaryText,
                        synonyms = org.json.JSONArray(command.phrases.drop(1)).toString(),
                        description = command.description,
                        category = command.category.name,
                        priority = command.priority
                    )
                    dao.update(updated)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Command not found: ${command.id}"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CommandStorage", "Failed to update command: ${command.id}", e)
            Result.failure(e)
        }
    }
}

/**
 * Export/Import helper for command backup and restore
 */
object CommandExporter {

    /**
     * Export commands to JSON string
     */
    fun exportToJson(commands: List<VoiceCommandData>): String {
        // Simple JSON serialization
        val commandsJson = commands.joinToString(",\n") { command ->
            """
            {
                "id": "${command.id}",
                "phrases": ${command.phrases.joinToString(",", "[", "]") { "\"$it\"" }},
                "priority": ${command.priority},
                "namespace": "${command.namespace}",
                "description": "${command.description}",
                "category": "${command.category}",
                "enabled": ${command.enabled},
                "createdAt": ${command.createdAt},
                "lastUsed": ${command.lastUsed},
                "usageCount": ${command.usageCount},
                "metadata": ${exportMetadata(command.metadata)}
            }
            """.trimIndent()
        }

        return "{\n  \"version\": \"1.0\",\n  \"commands\": [\n$commandsJson\n  ]\n}"
    }

    /**
     * Import commands from JSON string
     */
    fun importFromJson(json: String): Result<List<VoiceCommandData>> {
        return try {
            val jsonObj = org.json.JSONObject(json)
            val commandsArray = jsonObj.getJSONArray("commands")
            val commands = mutableListOf<VoiceCommandData>()

            for (i in 0 until commandsArray.length()) {
                val cmdObj = commandsArray.getJSONObject(i)
                val phrasesArray = cmdObj.getJSONArray("phrases")
                val phrases = (0 until phrasesArray.length()).map { phrasesArray.getString(it) }

                val metadataObj = cmdObj.optJSONObject("metadata")
                val metadata = if (metadataObj != null) {
                    metadataObj.keys().asSequence().associateWith { metadataObj.getString(it) }
                } else {
                    emptyMap()
                }

                commands.add(VoiceCommandData(
                    id = cmdObj.getString("id"),
                    phrases = phrases,
                    priority = cmdObj.optInt("priority", 0),
                    namespace = cmdObj.optString("namespace", "imported"),
                    description = cmdObj.optString("description", ""),
                    category = try {
                        CommandCategory.valueOf(cmdObj.optString("category", "CUSTOM"))
                    } catch (_: IllegalArgumentException) {
                        CommandCategory.CUSTOM
                    },
                    enabled = cmdObj.optBoolean("enabled", true),
                    createdAt = cmdObj.optLong("createdAt", System.currentTimeMillis()),
                    lastUsed = cmdObj.optLong("lastUsed", 0L),
                    usageCount = cmdObj.optLong("usageCount", 0L),
                    metadata = metadata
                ))
            }

            Result.success(commands)
        } catch (e: Exception) {
            android.util.Log.e("CommandExporter", "Failed to parse import JSON", e)
            Result.failure(e)
        }
    }

    private fun exportMetadata(metadata: Map<String, String>): String {
        if (metadata.isEmpty()) return "{}"

        val entries = metadata.entries.joinToString(",") { (key, value) ->
            "\"$key\": \"$value\""
        }
        return "{$entries}"
    }
}

/**
 * Persistent registry wrapper
 *
 * Adds persistence capabilities to DynamicCommandRegistry using composition
 */
class PersistentCommandRegistry(
    private val context: android.content.Context,
    maxCommandsPerNamespace: Int = 1000,
    conflictDetectionConfig: ConflictDetectionConfig = ConflictDetectionConfig(),
    private val storage: CommandStorage = CommandStorage(context)
) {
    // Use composition instead of inheritance
    private val registry = DynamicCommandRegistry(maxCommandsPerNamespace, conflictDetectionConfig)

    /**
     * Save current registry state to persistent storage
     */
    suspend fun saveToStorage(): Result<Int> {
        return try {
            val allCommands = registry.getAllCommands(enabledOnly = false)
            val commandData = allCommands.map { VoiceCommandData.from(it) }

            var savedCount = 0
            for (data in commandData) {
                val result = storage.saveCommand(data)
                if (result.isSuccess) savedCount++
            }

            Result.success(savedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load registry state from persistent storage
     *
     * Note: This cannot fully restore command actions (suspend functions)
     * Commands loaded from storage will need their actions re-registered
     */
    suspend fun loadFromStorage(): Result<Int> {
        return try {
            val result = storage.loadAllCommands()
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error loading commands"))
            }

            val commandData = result.getOrNull() ?: emptyList()

            // Restore action handlers from command metadata using ActionFactory
            // Register commands with placeholder actions that log the expected action type.
            // The actual action handlers are resolved at runtime by the handler dispatch system
            // (StaticCommandRegistry → Handlers) rather than the dynamic command system.
            var restoredCount = 0
            for (data in commandData) {
                val voiceCommand = VoiceCommand(
                    id = data.id,
                    phrases = data.phrases,
                    priority = data.priority.coerceIn(1, 100),
                    namespace = data.namespace.ifBlank { "restored" },
                    description = data.description,
                    category = data.category,
                    enabled = data.enabled,
                    createdAt = data.createdAt,
                    lastUsed = data.lastUsed,
                    usageCount = data.usageCount,
                    metadata = data.metadata,
                    action = { ctx ->
                        // Dynamic commands restored from storage are dispatched through
                        // the handler chain (ActionCoordinator → HandlerFactory → IHandler),
                        // not through this action lambda. This is a fallback.
                        CommandResult.Error(
                            message = "Command '${data.id}' requires handler dispatch (category=${data.category})"
                        )
                    }
                )
                registry.registerCommand(voiceCommand)
                restoredCount++
            }

            android.util.Log.i("PersistentRegistry",
                "Restored $restoredCount/${commandData.size} commands from storage")
            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export commands to JSON file
     */
    suspend fun exportToJson(): Result<String> {
        return try {
            val allCommands = registry.getAllCommands(enabledOnly = false)
            val commandData = allCommands.map { VoiceCommandData.from(it) }
            val json = CommandExporter.exportToJson(commandData)

            Result.success(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import commands from JSON
     */
    suspend fun importFromJson(json: String): Result<Int> {
        return try {
            val result = CommandExporter.importFromJson(json)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error importing JSON"))
            }

            val commands = result.getOrNull() ?: emptyList()

            var importedCount = 0
            for (data in commands) {
                val voiceCommand = VoiceCommand(
                    id = data.id,
                    phrases = data.phrases,
                    priority = data.priority.coerceIn(1, 100),
                    namespace = data.namespace.ifBlank { "imported" },
                    description = data.description,
                    category = data.category,
                    enabled = data.enabled,
                    createdAt = data.createdAt,
                    metadata = data.metadata,
                    action = { ctx ->
                        CommandResult.Error(
                            message = "Imported command '${data.id}' requires handler dispatch"
                        )
                    }
                )
                registry.registerCommand(voiceCommand)
                storage.saveCommand(data)
                importedCount++
            }

            android.util.Log.i("PersistentRegistry",
                "Imported $importedCount/${commands.size} commands from JSON")
            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
