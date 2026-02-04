package com.augmentalis.magicelements.core.modules

import com.augmentalis.magicelements.core.mel.functions.PluginTier

/**
 * Platform delegate interface for DataModule.
 * Platform implementations provide actual data storage/retrieval via VoiceDataManager.
 */
interface DataModuleDelegate {
    /**
     * Get stored value by key.
     * @param key Data key
     * @return Stored value or null if not found
     */
    suspend fun get(key: String): Any?

    /**
     * Store value with key.
     * @param key Data key
     * @param value Value to store
     */
    suspend fun set(key: String, value: Any?)

    /**
     * Remove value by key.
     * @param key Data key
     */
    suspend fun remove(key: String)

    /**
     * Check if key exists.
     * @param key Data key
     * @return True if key exists
     */
    suspend fun has(key: String): Boolean

    /**
     * List all keys.
     * @return List of all keys
     */
    suspend fun keys(): List<String>

    /**
     * Clear all data.
     */
    suspend fun clear()

    /**
     * Get encrypted value by key.
     * @param key Data key
     * @return Decrypted value or null if not found
     */
    suspend fun getSecure(key: String): Any?

    /**
     * Store encrypted value with key.
     * @param key Data key
     * @param value Value to encrypt and store
     */
    suspend fun setSecure(key: String, value: Any?)

    /**
     * Remove encrypted value by key.
     * @param key Data key
     */
    suspend fun removeSecure(key: String)

    /**
     * Get cached value by key.
     * @param key Cache key
     * @return Cached value or null if not found/expired
     */
    suspend fun getCached(key: String): Any?

    /**
     * Cache value with optional TTL.
     * @param key Cache key
     * @param value Value to cache
     * @param ttl Time-to-live in seconds (null for no expiration)
     */
    suspend fun setCached(key: String, value: Any?, ttl: Long?)

    /**
     * Clear all cached data.
     */
    suspend fun clearCache()

    /**
     * Export data in specified format.
     * @param format Export format ("json" or "csv")
     * @return Exported data as string
     */
    suspend fun export(format: String): String

    /**
     * Import data from string.
     * @param data Data to import (JSON or CSV format)
     */
    suspend fun import(data: String)
}

/**
 * DataModule - Wraps VoiceDataManager for AvaCode.
 *
 * Provides data storage, caching, and encryption capabilities to MEL plugins.
 *
 * Usage in MEL:
 * ```
 * # DATA tier methods
 * @data.get("user.name")                    # Get stored value
 * @data.set("user.name", "John")            # Store value
 * @data.remove("user.name")                 # Remove key
 * @data.has("user.name")                    # Check if key exists
 * @data.keys()                              # List all keys
 * @data.cache.get("temp.data")              # Get cached value
 * @data.cache.set("temp.data", value, 300)  # Cache with 5min TTL
 * @data.cache.clear()                       # Clear cache
 *
 * # LOGIC tier methods
 * @data.clear()                             # Clear all data
 * @data.secure.get("password")              # Get encrypted value
 * @data.secure.set("password", "secret")    # Store encrypted
 * @data.secure.remove("password")           # Remove encrypted
 * @data.export("json")                      # Export as JSON
 * @data.import(jsonData)                    # Import data
 * ```
 *
 * @param delegate Platform implementation (null for unsupported platforms)
 */
class DataModule(
    private val delegate: DataModuleDelegate?
) : BaseModule(
    name = "data",
    version = "1.0.0",
    minimumTier = PluginTier.DATA
) {

    init {
        // ========== DATA Tier Methods ==========

        registerMethod(
            name = "get",
            tier = PluginTier.DATA,
            description = "Get stored value by key",
            returnType = "Any?",
            parameters = listOf(
                MethodParameter(
                    name = "key",
                    type = "String",
                    required = true,
                    description = "Data key"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val key = args.argString(0, "key")
                delegate!!.get(key)
            }
        )

        registerMethod(
            name = "set",
            tier = PluginTier.DATA,
            description = "Store value with key",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "key",
                    type = "String",
                    required = true,
                    description = "Data key"
                ),
                MethodParameter(
                    name = "value",
                    type = "Any?",
                    required = true,
                    description = "Value to store"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val key = args.argString(0, "key")
                val value = args.getOrNull(1)
                delegate!!.set(key, value)
            }
        )

        registerMethod(
            name = "remove",
            tier = PluginTier.DATA,
            description = "Remove value by key",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "key",
                    type = "String",
                    required = true,
                    description = "Data key"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val key = args.argString(0, "key")
                delegate!!.remove(key)
            }
        )

        registerMethod(
            name = "has",
            tier = PluginTier.DATA,
            description = "Check if key exists",
            returnType = "Boolean",
            parameters = listOf(
                MethodParameter(
                    name = "key",
                    type = "String",
                    required = true,
                    description = "Data key"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val key = args.argString(0, "key")
                delegate!!.has(key)
            }
        )

        registerMethod(
            name = "keys",
            tier = PluginTier.DATA,
            description = "List all keys",
            returnType = "List<String>",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.keys()
            }
        )

        registerMethod(
            name = "cache.get",
            tier = PluginTier.DATA,
            description = "Get cached value by key",
            returnType = "Any?",
            parameters = listOf(
                MethodParameter(
                    name = "key",
                    type = "String",
                    required = true,
                    description = "Cache key"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val key = args.argString(0, "key")
                delegate!!.getCached(key)
            }
        )

        registerMethod(
            name = "cache.set",
            tier = PluginTier.DATA,
            description = "Cache value with optional TTL",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "key",
                    type = "String",
                    required = true,
                    description = "Cache key"
                ),
                MethodParameter(
                    name = "value",
                    type = "Any?",
                    required = true,
                    description = "Value to cache"
                ),
                MethodParameter(
                    name = "ttl",
                    type = "Long?",
                    required = false,
                    description = "Time-to-live in seconds (null for no expiration)"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val key = args.argString(0, "key")
                val value = args.getOrNull(1)
                val ttl = args.argOrNull<Number>(2)?.toLong()
                delegate!!.setCached(key, value, ttl)
            }
        )

        registerMethod(
            name = "cache.clear",
            tier = PluginTier.DATA,
            description = "Clear all cached data",
            returnType = "Unit",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.clearCache()
            }
        )

        // ========== LOGIC Tier Methods ==========

        registerMethod(
            name = "clear",
            tier = PluginTier.LOGIC,
            description = "Clear all data (requires LOGIC tier)",
            returnType = "Unit",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.clear()
            }
        )

        registerMethod(
            name = "secure.get",
            tier = PluginTier.LOGIC,
            description = "Get encrypted value by key",
            returnType = "Any?",
            parameters = listOf(
                MethodParameter(
                    name = "key",
                    type = "String",
                    required = true,
                    description = "Data key"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val key = args.argString(0, "key")
                delegate!!.getSecure(key)
            }
        )

        registerMethod(
            name = "secure.set",
            tier = PluginTier.LOGIC,
            description = "Store encrypted value with key",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "key",
                    type = "String",
                    required = true,
                    description = "Data key"
                ),
                MethodParameter(
                    name = "value",
                    type = "Any?",
                    required = true,
                    description = "Value to encrypt and store"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val key = args.argString(0, "key")
                val value = args.getOrNull(1)
                delegate!!.setSecure(key, value)
            }
        )

        registerMethod(
            name = "secure.remove",
            tier = PluginTier.LOGIC,
            description = "Remove encrypted value by key",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "key",
                    type = "String",
                    required = true,
                    description = "Data key"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val key = args.argString(0, "key")
                delegate!!.removeSecure(key)
            }
        )

        registerMethod(
            name = "export",
            tier = PluginTier.LOGIC,
            description = "Export data in specified format",
            returnType = "String",
            parameters = listOf(
                MethodParameter(
                    name = "format",
                    type = "String",
                    required = false,
                    defaultValue = "json",
                    description = "Export format (json or csv)"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val format = args.argOrDefault(0, "json")
                delegate!!.export(format)
            }
        )

        registerMethod(
            name = "import",
            tier = PluginTier.LOGIC,
            description = "Import data from string",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "data",
                    type = "String",
                    required = true,
                    description = "Data to import (JSON or CSV format)"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val data = args.argString(0, "data")
                delegate!!.import(data)
            }
        )
    }

    /**
     * Ensure delegate is available.
     * @throws ModuleException if delegate is null (unsupported platform)
     */
    private fun requireDelegate() {
        if (delegate == null) {
            throw ModuleException(
                name,
                "",
                "Data module not supported on this platform"
            )
        }
    }

    override suspend fun initialize() {
        // Delegate initialization happens at platform level
    }

    override suspend fun dispose() {
        // Cleanup happens at platform level
    }
}
