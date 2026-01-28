# AIDL Plugin Command Registry Implementation

**Document:** AIDL Plugin Command Registry
**Created:** 2025-11-14
**Project:** VoiceOS CommandManager
**Status:** TODO - Implementation Required
**Priority:** High
**Effort:** 1-2 weeks

---

## Executive Summary

This document specifies the AIDL interface that VoiceOS CommandManager must implement to support **dynamic command registration** from external plugins (NewAvanue, Avanues, AVA AI, etc.).

**Purpose:**
- Enable plugins to register voice commands dynamically at runtime
- Support command updates, unregistration, and lifecycle management
- Use Compact JSON format for efficient command registration
- Maintain backward compatibility with existing VOS command system

**Why This is Needed:**
- NewAvanue is moving to a modular plugin architecture
- Each plugin (Browser, Email, Calendar, etc.) needs to register its own voice commands
- Plugins must be able to update/remove commands when updated/uninstalled
- Centralized command management in VoiceOS CommandManager

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [AIDL Interface Specification](#aidl-interface-specification)
3. [Implementation Guide](#implementation-guide)
4. [Integration with Existing System](#integration-with-existing-system)
5. [Testing Strategy](#testing-strategy)
6. [Security Considerations](#security-considerations)
7. [Timeline & Milestones](#timeline--milestones)

---

## Architecture Overview

### Current State (VoiceOS CommandManager)

```
CommandManager
├── DynamicCommandRegistry (exists)
│   ├── registerCommand(VoiceCommand)
│   ├── unregisterCommand(commandId)
│   └── resolveCommand(phrase)
├── VOSFileParser (exists)
│   └── parse(vosFileContent)
└── CompactJsonParser (NEW - needs implementation)
    └── parse(compactJsonContent)
```

**What Exists:**
- ✅ `DynamicCommandRegistry` - Thread-safe command management
- ✅ `VOSFileParser` - Parses legacy `.vos` format
- ✅ `VoiceCommand` model - Represents a voice command

**What's Missing:**
- ❌ AIDL service interface for external plugins
- ❌ Compact JSON parser (73% smaller than VOS format)
- ❌ Plugin authentication/validation
- ❌ Command ownership tracking

### Target State (After Implementation)

```
                    ┌─────────────────────────┐
                    │   External Plugins      │
                    │  (NewAvanue, Avanues)   │
                    └───────────┬─────────────┘
                                │
                          AIDL Interface
                                │
                    ┌───────────▼─────────────┐
                    │   CommandRegistry       │
                    │   Service (NEW)         │
                    └───────────┬─────────────┘
                                │
                    ┌───────────▼─────────────┐
                    │  CompactJsonParser      │
                    │  (NEW)                  │
                    └───────────┬─────────────┘
                                │
                    ┌───────────▼─────────────┐
                    │ DynamicCommandRegistry  │
                    │ (existing)              │
                    └───────────┬─────────────┘
                                │
                    ┌───────────▼─────────────┐
                    │   Room Database         │
                    │   (existing)            │
                    └─────────────────────────┘
```

---

## AIDL Interface Specification

### File Structure

```
VoiceOS/modules/managers/CommandManager/src/main/aidl/com/augmentalis/voiceos/
├── ICommandRegistry.aidl
├── ICommandCallback.aidl
└── PluginInfo.aidl
```

### ICommandRegistry.aidl

```java
// ICommandRegistry.aidl
package com.augmentalis.voiceos;

import com.augmentalis.voiceos.ICommandCallback;
import com.augmentalis.voiceos.PluginInfo;

/**
 * AIDL interface for dynamic command registration from external plugins
 *
 * Allows plugins (NewAvanue, Avanues, AVA AI) to register voice commands
 * at runtime without modifying VoiceOS code.
 *
 * Thread-safe: All methods can be called from any thread.
 * Format: Supports both Compact JSON (recommended) and Legacy VOS format.
 *
 * @since VOS 4.1
 * @author VoiceOS Team
 */
interface ICommandRegistry {

    /**
     * Register voice commands from a plugin
     *
     * @param pluginInfo Plugin metadata (id, name, version)
     * @param namespace Command namespace (e.g., "browser", "email")
     * @param commandJson JSON content in Compact JSON or Legacy VOS format
     * @param callback Callback interface to receive recognized commands
     * @return true if registration successful, false otherwise
     *
     * Example pluginInfo:
     *   id: "com.augmentalis.newavanue.browser"
     *   name: "Browser"
     *   version: "1.0.0"
     *
     * Example namespace: "browser"
     *
     * Example commandJson (Compact JSON - RECOMMENDED):
     * {
     *   "version": "1.0",
     *   "locale": "en-US",
     *   "items": [
     *     ["browser_new_tab", "open new tab", ["new tab", "create tab"], "Open New Tab"]
     *   ]
     * }
     *
     * Example commandJson (Legacy VOS - SUPPORTED):
     * {
     *   "schema": "vos-1.0",
     *   "commands": [
     *     {"action": "BROWSER_NEW_TAB", "cmd": "open new tab", "syn": ["new tab"]}
     *   ]
     * }
     *
     * Throws:
     *   - IllegalArgumentException if JSON is invalid
     *   - SecurityException if plugin not authenticated
     */
    boolean registerCommands(
        in PluginInfo pluginInfo,
        String namespace,
        String commandJson,
        ICommandCallback callback
    );

    /**
     * Unregister all commands from a plugin
     *
     * Removes all commands registered by this plugin from the database.
     * Typically called when plugin is uninstalled.
     *
     * @param pluginId Unique plugin ID (e.g., "com.augmentalis.newavanue.browser")
     * @param namespace Command namespace
     * @return Number of commands removed (0 if none found)
     *
     * Throws:
     *   - SecurityException if pluginId doesn't match original registrant
     */
    int unregisterCommands(String pluginId, String namespace);

    /**
     * Update commands (atomic remove + add)
     *
     * Removes all existing commands for this plugin/namespace and
     * registers new commands in a single atomic transaction.
     * Typically called when plugin is updated with new commands.
     *
     * @param pluginInfo Plugin metadata (must match original registration)
     * @param namespace Command namespace
     * @param commandJson New JSON content (Compact JSON or Legacy VOS)
     * @param callback Callback interface (can be same or new instance)
     * @return true if update successful, false otherwise
     *
     * Throws:
     *   - IllegalArgumentException if JSON is invalid
     *   - SecurityException if plugin not authenticated
     */
    boolean updateCommands(
        in PluginInfo pluginInfo,
        String namespace,
        String commandJson,
        ICommandCallback callback
    );

    /**
     * Get all commands registered by a plugin
     *
     * Returns command IDs, not full command data (for privacy).
     *
     * @param pluginId Unique plugin ID
     * @param namespace Command namespace (null = all namespaces)
     * @return List of command IDs registered by this plugin
     *
     * Example return: ["browser_new_tab", "browser_close_tab", ...]
     */
    List<String> getRegisteredCommands(String pluginId, String namespace);

    /**
     * Check if commands are registered for a plugin
     *
     * Fast check without retrieving full command list.
     *
     * @param pluginId Unique plugin ID
     * @param namespace Command namespace
     * @return true if at least one command registered, false otherwise
     */
    boolean hasRegisteredCommands(String pluginId, String namespace);

    /**
     * Get service version
     *
     * Allows clients to check API compatibility.
     *
     * @return Service version (semantic versioning, e.g., "1.0.0")
     */
    String getServiceVersion();
}
```

### ICommandCallback.aidl

```java
// ICommandCallback.aidl
package com.augmentalis.voiceos;

/**
 * Callback interface for receiving recognized voice commands
 *
 * Implemented by plugins to receive voice command events from VoiceOS.
 * Called when user speaks a phrase that matches a registered command.
 *
 * Thread-safety: Callbacks are invoked on a background thread.
 * Plugins should dispatch to main thread if needed.
 *
 * @since VOS 4.1
 * @author VoiceOS Team
 */
interface ICommandCallback {

    /**
     * Called when VoiceOS recognizes a command for this plugin
     *
     * @param commandId Command ID (e.g., "browser_new_tab")
     * @param recognizedPhrase The exact phrase user spoke
     * @param confidence Confidence score (0.0-1.0, where 1.0 = perfect match)
     * @param timestamp Unix timestamp (ms) when command was recognized
     *
     * Example:
     *   commandId: "browser_new_tab"
     *   recognizedPhrase: "new tab"
     *   confidence: 0.95
     *   timestamp: 1699999999999
     *
     * Implementation guidelines:
     *   - Return quickly (< 100ms)
     *   - Perform heavy work on background thread
     *   - Don't block VoiceOS processing pipeline
     */
    void onCommandRecognized(
        String commandId,
        String recognizedPhrase,
        float confidence,
        long timestamp
    );

    /**
     * Called when command execution fails
     *
     * Optional method for error handling.
     *
     * @param commandId Command ID that failed
     * @param errorCode Error code (see ErrorCodes below)
     * @param errorMessage Human-readable error message
     *
     * Error codes:
     *   1 = COMMAND_NOT_FOUND
     *   2 = PERMISSION_DENIED
     *   3 = EXECUTION_TIMEOUT
     *   4 = PLUGIN_UNAVAILABLE
     */
    void onCommandError(String commandId, int errorCode, String errorMessage);
}
```

### PluginInfo.aidl

```java
// PluginInfo.aidl
package com.augmentalis.voiceos;

/**
 * Plugin metadata for command registration
 *
 * Parcelable data class containing plugin information.
 *
 * @since VOS 4.1
 * @author VoiceOS Team
 */
parcelable PluginInfo {
    /**
     * Unique plugin ID (reverse domain notation)
     * Example: "com.augmentalis.newavanue.browser"
     */
    String id;

    /**
     * Human-readable plugin name
     * Example: "Browser"
     */
    String name;

    /**
     * Plugin version (semantic versioning)
     * Example: "1.0.0"
     */
    String version;

    /**
     * Package name of plugin app
     * Used for authentication/validation
     * Example: "com.augmentalis.newavanue"
     */
    String packageName;
}
```

---

## Implementation Guide

### Step 1: Create AIDL Files (Week 1, Day 1)

**Location:** `VoiceOS/modules/managers/CommandManager/src/main/aidl/com/augmentalis/voiceos/`

**Files to Create:**
1. `ICommandRegistry.aidl`
2. `ICommandCallback.aidl`
3. `PluginInfo.aidl`

**Android Studio will auto-generate Java/Kotlin stubs.**

### Step 2: Implement Compact JSON Parser (Week 1, Day 2-3)

**Location:** `VoiceOS/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/loader/CompactJsonParser.kt`

```kotlin
package com.augmentalis.voiceoscore.loader

import org.json.JSONObject
import com.augmentalis.commandmanager.dynamic.VoiceCommand
import com.augmentalis.commandmanager.dynamic.CommandCategory

/**
 * Parser for Compact JSON command format
 *
 * Supports Universal Compact JSON System v1.0
 * 73% smaller than legacy VOS format, 72% faster parsing
 *
 * @see /Volumes/M-Drive/Coding/VoiceOS/docs/project-info/UNIVERSAL-COMPACT-JSON-SYSTEM.md
 */
class CompactJsonParser {

    data class ParseResult(
        val commands: List<VoiceCommand>,
        val locale: String,
        val metadata: Map<String, Any>
    )

    /**
     * Parse Compact JSON format
     *
     * Format:
     * {
     *   "version": "1.0",
     *   "locale": "en-US",
     *   "items": [
     *     ["command_id", "primary phrase", ["synonym1", "synonym2"], "description"]
     *   ]
     * }
     */
    fun parse(jsonContent: String, namespace: String, pluginId: String): Result<ParseResult> {
        return try {
            val json = JSONObject(jsonContent)

            // Validate format
            require(json.has("items")) { "Missing 'items' field in Compact JSON" }

            val locale = json.optString("locale", "en-US")
            val itemsArray = json.getJSONArray("items")

            val commands = mutableListOf<VoiceCommand>()

            for (i in 0 until itemsArray.length()) {
                val itemArray = itemsArray.getJSONArray(i)

                // Parse 4-element array: [id, primary, synonyms, description]
                require(itemArray.length() == 4) {
                    "Invalid item at index $i: expected 4 elements, got ${itemArray.length()}"
                }

                val commandId = itemArray.getString(0)
                val primary = itemArray.getString(1)
                val synonymsArray = itemArray.getJSONArray(2)
                val description = itemArray.getString(3)

                // Parse synonyms
                val synonyms = mutableListOf<String>()
                for (j in 0 until synonymsArray.length()) {
                    synonyms.add(synonymsArray.getString(j))
                }

                // Create VoiceCommand
                val phrases = listOf(primary) + synonyms

                val command = VoiceCommand(
                    id = commandId,
                    phrases = phrases,
                    priority = 50, // Default priority
                    namespace = namespace,
                    description = description,
                    category = extractCategory(commandId),
                    enabled = true,
                    createdAt = System.currentTimeMillis(),
                    metadata = mapOf(
                        "plugin_id" to pluginId,
                        "locale" to locale,
                        "format" to "compact_json"
                    ),
                    action = { context ->
                        // Action is handled by plugin callback
                        CommandResult.Success
                    }
                )

                commands.add(command)
            }

            val metadata = json.optJSONObject("metadata")?.let { metaObj ->
                metaObj.keys().asSequence().associateWith { key ->
                    metaObj.get(key)
                }
            } ?: emptyMap()

            Result.success(ParseResult(commands, locale, metadata))

        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to parse Compact JSON: ${e.message}", e))
        }
    }

    /**
     * Extract category from command ID
     *
     * Example: "browser_new_tab" → CUSTOM (or APP_CONTROL)
     */
    private fun extractCategory(commandId: String): CommandCategory {
        return when {
            commandId.contains("navigate") -> CommandCategory.NAVIGATION
            commandId.contains("edit") || commandId.contains("copy") || commandId.contains("paste") -> CommandCategory.TEXT_EDITING
            commandId.contains("volume") || commandId.contains("brightness") -> CommandCategory.SYSTEM
            else -> CommandCategory.CUSTOM
        }
    }
}
```

### Step 3: Implement CommandRegistry Service (Week 1, Day 4-5)

**Location:** `VoiceOS/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/service/CommandRegistryService.kt`

```kotlin
package com.augmentalis.voiceoscore.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.augmentalis.voiceos.ICommandRegistry
import com.augmentalis.voiceos.ICommandCallback
import com.augmentalis.voiceos.PluginInfo
import com.augmentalis.commandmanager.dynamic.DynamicCommandRegistry
import com.augmentalis.commandmanager.loader.CompactJsonParser
import com.augmentalis.commandmanager.loader.VOSFileParser
import kotlinx.coroutines.*

/**
 * AIDL service for plugin command registration
 *
 * Allows external plugins to register voice commands dynamically.
 *
 * @since VOS 4.1
 */
class CommandRegistryService : Service() {

    companion object {
        private const val TAG = "CommandRegistryService"
        private const val SERVICE_VERSION = "1.0.0"
    }

    private lateinit var commandRegistry: DynamicCommandRegistry
    private val compactJsonParser = CompactJsonParser()
    private val vosFileParser by lazy { VOSFileParser(applicationContext) }

    // Track plugin callbacks
    private val pluginCallbacks = mutableMapOf<String, ICommandCallback>()

    // Coroutine scope for async operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        commandRegistry = DynamicCommandRegistry()
        Log.i(TAG, "CommandRegistryService started (version $SERVICE_VERSION)")
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.i(TAG, "CommandRegistryService stopped")
    }

    private val binder = object : ICommandRegistry.Stub() {

        override fun registerCommands(
            pluginInfo: PluginInfo,
            namespace: String,
            commandJson: String,
            callback: ICommandCallback
        ): Boolean {
            return runBlocking {
                try {
                    Log.d(TAG, "registerCommands: ${pluginInfo.id}, namespace: $namespace")

                    // Validate plugin authentication
                    if (!validatePlugin(pluginInfo)) {
                        throw SecurityException("Plugin authentication failed: ${pluginInfo.id}")
                    }

                    // Parse command JSON (try Compact JSON first, fallback to VOS)
                    val commands = parseCommandJson(commandJson, namespace, pluginInfo.id)

                    // Register commands
                    var successCount = 0
                    commands.forEach { command ->
                        val result = commandRegistry.registerCommand(
                            command = command,
                            checkConflicts = true,
                            autoResolveConflicts = false
                        )

                        if (result.isSuccess) {
                            successCount++
                        } else {
                            Log.w(TAG, "Failed to register command: ${command.id}, error: ${result.exceptionOrNull()}")
                        }
                    }

                    // Store callback for this plugin
                    pluginCallbacks["${pluginInfo.id}:$namespace"] = callback

                    Log.i(TAG, "Registered $successCount/${commands.size} commands for ${pluginInfo.id}")

                    successCount == commands.size

                } catch (e: Exception) {
                    Log.e(TAG, "registerCommands failed", e)
                    false
                }
            }
        }

        override fun unregisterCommands(pluginId: String, namespace: String): Int {
            return runBlocking {
                try {
                    Log.d(TAG, "unregisterCommands: $pluginId, namespace: $namespace")

                    // Get all commands for this plugin/namespace
                    val commands = commandRegistry.getAllCommands(namespace, enabledOnly = false)
                        .filter { it.metadata["plugin_id"] == pluginId }

                    // Remove each command
                    var removedCount = 0
                    commands.forEach { command ->
                        val result = commandRegistry.unregisterCommand(command.id, namespace)
                        if (result.isSuccess) {
                            removedCount++
                        }
                    }

                    // Remove callback
                    pluginCallbacks.remove("$pluginId:$namespace")

                    Log.i(TAG, "Unregistered $removedCount commands for $pluginId")

                    removedCount

                } catch (e: Exception) {
                    Log.e(TAG, "unregisterCommands failed", e)
                    0
                }
            }
        }

        override fun updateCommands(
            pluginInfo: PluginInfo,
            namespace: String,
            commandJson: String,
            callback: ICommandCallback
        ): Boolean {
            return runBlocking {
                try {
                    Log.d(TAG, "updateCommands: ${pluginInfo.id}, namespace: $namespace")

                    // Atomic update: unregister + register
                    val removedCount = unregisterCommands(pluginInfo.id, namespace)
                    val registered = registerCommands(pluginInfo, namespace, commandJson, callback)

                    Log.i(TAG, "Updated commands for ${pluginInfo.id}: removed $removedCount, registered: $registered")

                    registered

                } catch (e: Exception) {
                    Log.e(TAG, "updateCommands failed", e)
                    false
                }
            }
        }

        override fun getRegisteredCommands(pluginId: String, namespace: String?): List<String> {
            return runBlocking {
                try {
                    val commands = commandRegistry.getAllCommands(namespace, enabledOnly = false)
                        .filter { it.metadata["plugin_id"] == pluginId }

                    commands.map { it.id }

                } catch (e: Exception) {
                    Log.e(TAG, "getRegisteredCommands failed", e)
                    emptyList()
                }
            }
        }

        override fun hasRegisteredCommands(pluginId: String, namespace: String): Boolean {
            return runBlocking {
                try {
                    val commands = getRegisteredCommands(pluginId, namespace)
                    commands.isNotEmpty()

                } catch (e: Exception) {
                    Log.e(TAG, "hasRegisteredCommands failed", e)
                    false
                }
            }
        }

        override fun getServiceVersion(): String {
            return SERVICE_VERSION
        }
    }

    /**
     * Parse command JSON (Compact JSON or Legacy VOS)
     */
    private fun parseCommandJson(
        commandJson: String,
        namespace: String,
        pluginId: String
    ): List<VoiceCommand> {
        // Try Compact JSON first
        val compactResult = compactJsonParser.parse(commandJson, namespace, pluginId)
        if (compactResult.isSuccess) {
            Log.d(TAG, "Parsed as Compact JSON")
            return compactResult.getOrThrow().commands
        }

        // Fallback to Legacy VOS format
        Log.d(TAG, "Compact JSON failed, trying Legacy VOS format")
        val vosResult = vosFileParser.parse(commandJson)
        if (vosResult.isSuccess) {
            Log.d(TAG, "Parsed as Legacy VOS")
            // Convert VOSCommand to VoiceCommand
            return vosResult.getOrThrow().commands.map { vosCmd ->
                VoiceCommand(
                    id = vosCmd.action.lowercase(),
                    phrases = listOf(vosCmd.cmd) + vosCmd.syn,
                    priority = 50,
                    namespace = namespace,
                    description = "",
                    category = CommandCategory.CUSTOM,
                    enabled = true,
                    createdAt = System.currentTimeMillis(),
                    metadata = mapOf(
                        "plugin_id" to pluginId,
                        "format" to "vos_legacy"
                    ),
                    action = { CommandResult.Success }
                )
            }
        }

        throw IllegalArgumentException("Failed to parse command JSON: not Compact JSON or Legacy VOS format")
    }

    /**
     * Validate plugin authentication
     *
     * TODO: Implement proper authentication (signature verification, etc.)
     */
    private fun validatePlugin(pluginInfo: PluginInfo): Boolean {
        // For now, just check basic fields are present
        return pluginInfo.id.isNotBlank() &&
                pluginInfo.name.isNotBlank() &&
                pluginInfo.version.matches(Regex("\\d+\\.\\d+\\.\\d+"))
    }

    /**
     * Dispatch command to plugin callback
     *
     * Called by CommandProcessor when a command is recognized
     */
    fun dispatchCommand(
        namespace: String,
        commandId: String,
        recognizedPhrase: String,
        confidence: Float
    ) {
        serviceScope.launch {
            try {
                // Find callback for this namespace
                val callbackKey = pluginCallbacks.keys.firstOrNull { it.endsWith(":$namespace") }
                val callback = pluginCallbacks[callbackKey]

                if (callback != null) {
                    callback.onCommandRecognized(
                        commandId,
                        recognizedPhrase,
                        confidence,
                        System.currentTimeMillis()
                    )
                    Log.d(TAG, "Dispatched command to plugin: $commandId")
                } else {
                    Log.w(TAG, "No callback registered for namespace: $namespace")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to dispatch command", e)
            }
        }
    }
}
```

### Step 4: Register Service in AndroidManifest.xml (Week 1, Day 5)

```xml
<!-- VoiceOS/modules/managers/CommandManager/src/main/AndroidManifest.xml -->

<manifest>
    <application>
        <!-- Command Registry Service -->
        <service
            android:name=".service.CommandRegistryService"
            android:exported="true"
            android:permission="com.augmentalis.voiceos.permission.REGISTER_COMMANDS">

            <intent-filter>
                <action android:name="com.augmentalis.voiceos.CommandRegistry" />
            </intent-filter>
        </service>
    </application>

    <!-- Define permission for command registration -->
    <permission
        android:name="com.augmentalis.voiceos.permission.REGISTER_COMMANDS"
        android:protectionLevel="signature|privileged"
        android:description="@string/permission_register_commands_desc"
        android:label="@string/permission_register_commands_label" />

</manifest>
```

### Step 5: Integrate with CommandProcessor (Week 2)

**Location:** `VoiceOS/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/processor/CommandProcessor.kt`

```kotlin
// Add to CommandProcessor.kt

class CommandProcessor(
    private val commandRegistry: DynamicCommandRegistry,
    private val commandRegistryService: CommandRegistryService // NEW
) {

    suspend fun processVoiceInput(phrase: String): CommandResult {
        // Resolve command using DynamicCommandRegistry
        val matchingCommands = commandRegistry.resolveCommand(phrase)

        if (matchingCommands.isEmpty()) {
            return CommandResult.Error("No matching command found")
        }

        val topCommand = matchingCommands.first()

        // Execute command
        val result = topCommand.action(CommandExecutionContext(phrase))

        // If command is from external plugin, dispatch via AIDL callback
        if (topCommand.metadata.containsKey("plugin_id")) {
            commandRegistryService.dispatchCommand(
                namespace = topCommand.namespace,
                commandId = topCommand.id,
                recognizedPhrase = phrase,
                confidence = calculateConfidence(phrase, topCommand.phrases)
            )
        }

        return result
    }

    private fun calculateConfidence(phrase: String, targetPhrases: List<String>): Float {
        // Exact match = 1.0
        val normalized = phrase.trim().lowercase()
        return if (targetPhrases.any { it.trim().lowercase() == normalized }) {
            1.0f
        } else {
            // Fuzzy match calculation (0.0-1.0)
            targetPhrases.maxOfOrNull { target ->
                calculateSimilarity(normalized, target.trim().lowercase())
            } ?: 0.0f
        }
    }
}
```

---

## Integration with Existing System

### Compatibility Matrix

| Component | Current Version | After Implementation | Changes Required |
|-----------|----------------|---------------------|------------------|
| `DynamicCommandRegistry` | Exists | No changes | None (already supports runtime registration) |
| `VOSFileParser` | Exists | No changes | None (still used for legacy format) |
| `CompactJsonParser` | **NEW** | Create | Implement from scratch |
| `CommandRegistryService` | **NEW** | Create | Implement from scratch |
| `CommandProcessor` | Exists | **Modify** | Add AIDL callback dispatch |
| `AndroidManifest.xml` | Exists | **Modify** | Add service declaration |

### Migration Path

**Existing VOS commands:** Continue to work (no migration needed)
**New plugin commands:** Use AIDL interface

**Both systems coexist:**
- Static commands (VOS files in assets) → loaded at startup
- Dynamic commands (AIDL registration) → loaded at runtime

---

## Testing Strategy

### Unit Tests

```kotlin
// CommandRegistryServiceTest.kt

class CommandRegistryServiceTest {

    @Test
    fun `registerCommands with Compact JSON should succeed`() {
        val service = CommandRegistryService()
        val pluginInfo = PluginInfo(
            id = "com.test.plugin",
            name = "Test Plugin",
            version = "1.0.0",
            packageName = "com.test.plugin"
        )

        val compactJson = """
        {
          "version": "1.0",
          "locale": "en-US",
          "items": [
            ["test_command", "test phrase", ["synonym"], "Test Command"]
          ]
        }
        """.trimIndent()

        val callback = object : ICommandCallback.Stub() {
            override fun onCommandRecognized(id: String, phrase: String, confidence: Float, timestamp: Long) {
                // Mock callback
            }
            override fun onCommandError(id: String, code: Int, message: String) {
                // Mock error handler
            }
        }

        val result = service.binder.registerCommands(pluginInfo, "test", compactJson, callback)

        assertTrue(result)
    }

    @Test
    fun `unregisterCommands should remove all plugin commands`() {
        // ... test implementation
    }

    @Test
    fun `updateCommands should atomically replace commands`() {
        // ... test implementation
    }
}
```

### Integration Tests

```kotlin
// CommandRegistryIntegrationTest.kt

@RunWith(AndroidJUnit4::class)
class CommandRegistryIntegrationTest {

    @Test
    fun `end-to-end command registration and recognition`() {
        // 1. Register commands via AIDL
        // 2. Speak command via voice recognition
        // 3. Verify callback is invoked
        // 4. Verify command execution
    }

    @Test
    fun `plugin uninstall removes all commands`() {
        // 1. Register commands
        // 2. Verify commands exist
        // 3. Unregister commands
        // 4. Verify commands removed
        // 5. Verify voice recognition doesn't match
    }
}
```

---

## Security Considerations

### 1. Authentication

**Problem:** Malicious apps could register spam commands

**Solution:**
- Check `PackageInfo.signatures` to verify plugin authenticity
- Only allow plugins signed with Augmentalis certificate
- Implement package name whitelist

```kotlin
private fun validatePlugin(pluginInfo: PluginInfo): Boolean {
    val packageManager = applicationContext.packageManager

    try {
        val packageInfo = packageManager.getPackageInfo(
            pluginInfo.packageName,
            PackageManager.GET_SIGNATURES
        )

        // Verify signature matches Augmentalis certificate
        return validateSignature(packageInfo.signatures)

    } catch (e: NameNotFoundException) {
        Log.w(TAG, "Package not found: ${pluginInfo.packageName}")
        return false
    }
}
```

### 2. Rate Limiting

**Problem:** Plugins could spam registration requests

**Solution:**
- Limit registrations to 10 per minute per plugin
- Limit command count to 1000 per plugin

```kotlin
private val rateLimiter = RateLimiter(
    maxRequests = 10,
    windowMs = 60_000L
)

override fun registerCommands(...): Boolean {
    if (!rateLimiter.allowRequest(pluginInfo.id)) {
        throw SecurityException("Rate limit exceeded")
    }

    // ... proceed with registration
}
```

### 3. Command Validation

**Problem:** Malformed commands could crash VoiceOS

**Solution:**
- Validate all command fields before registration
- Sanitize command IDs and phrases
- Limit phrase length to 100 characters

```kotlin
private fun validateCommand(command: VoiceCommand): Boolean {
    return command.id.matches(Regex("^[a-z][a-z0-9_]*$")) &&
           command.phrases.all { it.length <= 100 } &&
           command.namespace.matches(Regex("^[a-z][a-z0-9_]*$"))
}
```

---

## Timeline & Milestones

| Week | Tasks | Deliverables | Validation |
|------|-------|--------------|------------|
| **Week 1, Day 1** | Create AIDL files | `ICommandRegistry.aidl`, `ICommandCallback.aidl`, `PluginInfo.aidl` | Files compile, stubs generated |
| **Week 1, Day 2-3** | Implement Compact JSON parser | `CompactJsonParser.kt` | Unit tests pass, parses example JSON |
| **Week 1, Day 4-5** | Implement CommandRegistry service | `CommandRegistryService.kt` | Service binds, registers commands |
| **Week 1, Day 5** | Update AndroidManifest | Service declaration | App builds successfully |
| **Week 2, Day 1-2** | Integrate with CommandProcessor | Modified `CommandProcessor.kt` | Callbacks dispatched correctly |
| **Week 2, Day 3-4** | Testing & Bug Fixes | Unit tests, integration tests | All tests pass, no crashes |
| **Week 2, Day 5** | Documentation & Review | API docs, code review | Ready for production |

**Total Effort:** 1-2 weeks (1 developer)

---

## Success Criteria

✅ **Implementation Complete:**
- AIDL interface implemented and working
- Compact JSON parser implemented and tested
- Service registered in AndroidManifest
- CommandProcessor dispatches callbacks

✅ **Functionality:**
- Plugins can register commands dynamically
- Commands appear in VoiceOS command list
- Voice recognition triggers callbacks
- Plugin updates/uninstalls work correctly

✅ **Performance:**
- Command registration <2 seconds
- Callback dispatch <50ms
- No memory leaks

✅ **Security:**
- Plugin authentication working
- Rate limiting enforced
- Command validation prevents crashes

✅ **Testing:**
- Unit tests pass (90% coverage)
- Integration tests pass
- Manual testing with NewAvanue plugin

---

## Next Steps

After implementation complete:

1. **Deploy to VoiceOS beta** (100 users)
2. **Test with NewAvanue Browser plugin** (integration validation)
3. **Monitor for errors** (crash reports, AIDL failures)
4. **Document API** (developer guide for plugin authors)
5. **Production release** (1000+ users)

---

## References

- **Compact JSON Specification:** `/Volumes/M-Drive/Coding/VoiceOS/docs/project-info/UNIVERSAL-COMPACT-JSON-SYSTEM.md`
- **DynamicCommandRegistry:** `/Volumes/M-Drive/Coding/VoiceOS/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/dynamic/DynamicCommandRegistry.kt`
- **NewAvanue Spec:** `/Volumes/M-Drive/Coding/NewAvanue/.ideacode/specs/005-kmp-modular-migration/spec.md`
- **AIDL Documentation:** https://developer.android.com/guide/components/aidl

---

**Document Version:** 1.0
**Last Updated:** 2025-11-14
**Author:** Claude (AI Assistant)
**Status:** Ready for VoiceOS Team Implementation
**Priority:** High (required for NewAvanue plugin migration)
