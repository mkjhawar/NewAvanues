/**
 * VUIDCreatorServiceBinder.kt - IPC service binder implementation for UUIDCreator
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Code Quality Expert
 * Created: 2025-11-10
 */
package com.augmentalis.uuidcreator

import android.util.Log
import com.google.gson.Gson
import com.augmentalis.uuidcreator.models.UUIDCommandResultData
import com.augmentalis.uuidcreator.models.UUIDElementData
import kotlinx.coroutines.runBlocking

/**
 * VUID Creator Service AIDL implementation
 *
 * Exposes UUIDCreator functionality via IPC for use by external apps.
 * This binder wraps the UUIDCreator instance and provides thread-safe
 * access to UUID management and targeting functionality across process boundaries.
 *
 * Features:
 * - Thread-safe access to UUIDCreator instance
 * - Automatic conversion between Parcelable and internal types
 * - Coroutine support for async operations (using runBlocking for AIDL)
 * - Error handling and logging
 * - JSON serialization for complex data
 *
 * Note on Threading:
 * AIDL methods are synchronous, but UUIDCreator uses coroutines internally.
 * We use runBlocking to bridge sync AIDL calls with async UUIDCreator operations.
 *
 * Usage:
 * ```kotlin
 * // In service's onBind()
 * private var uuidBinder: UUIDCreatorServiceBinder? = null
 *
 * override fun onBind(intent: Intent?): IBinder? {
 *     return when (intent?.action) {
 *         "com.augmentalis.uuidcreator.BIND_IPC" -> {
 *             val uuidCreator = UUIDCreator.getInstance()
 *             uuidBinder = UUIDCreatorServiceBinder(uuidCreator)
 *             uuidBinder!!.asBinder()
 *         }
 *         else -> super.onBind(intent)
 *     }
 * }
 * ```
 */
class VUIDCreatorServiceBinder(
    private val uuidCreator: VUIDCreator
) : IVUIDCreatorService.Stub() {

    companion object {
        private const val TAG = "UUIDCreatorServiceBinder"
    }

    /**
     * JSON serializer for stats and parameters
     */
    private val gson = Gson()

    /**
     * Generate a new UUID
     *
     * @return Newly generated UUID string
     */
    override fun generateUUID(): String {
        Log.d(TAG, "IPC: generateUUID() called")
        return try {
            uuidCreator.generateUUID()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating UUID", e)
            ""
        }
    }

    /**
     * Register an element with automatic UUID generation
     *
     * @param elementData Element data to register
     * @return Generated UUID for the registered element
     */
    override fun registerElement(elementData: UUIDElementData?): String {
        Log.d(TAG, "IPC: registerElement(elementData=${elementData?.name})")

        if (elementData == null) {
            Log.w(TAG, "registerElement called with null elementData")
            return ""
        }

        return try {
            val element = elementData.toUUIDElement()
            uuidCreator.registerElement(element)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering element", e)
            ""
        }
    }

    /**
     * Unregister an element by UUID
     *
     * @param vuid UUID of element to unregister
     * @return true if element was unregistered, false if not found
     */
    override fun unregisterElement(uuid: String?): Boolean {
        Log.d(TAG, "IPC: unregisterElement(uuid=$uuid)")

        if (uuid.isNullOrBlank()) {
            Log.w(TAG, "unregisterElement called with null/empty UUID")
            return false
        }

        return try {
            uuidCreator.unregisterElement(uuid)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering element", e)
            false
        }
    }

    /**
     * Find element by UUID
     *
     * @param vuid UUID to search for
     * @return Element data if found, null otherwise
     */
    override fun findByUUID(uuid: String?): UUIDElementData? {
        if (uuid.isNullOrBlank()) {
            Log.w(TAG, "findByUUID called with null/empty UUID")
            return null
        }

        return try {
            val element = uuidCreator.findByUUID(uuid) ?: return null
            UUIDElementData.fromUUIDElement(element)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding element by UUID", e)
            null
        }
    }

    /**
     * Find elements by name
     *
     * @param name Element name to search for
     * @return List of matching elements
     */
    override fun findByName(name: String?): List<UUIDElementData> {
        if (name.isNullOrBlank()) {
            Log.w(TAG, "findByName called with null/empty name")
            return emptyList()
        }

        return try {
            val elements = uuidCreator.findByName(name)
            elements.map { UUIDElementData.fromUUIDElement(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding elements by name", e)
            emptyList()
        }
    }

    /**
     * Find elements by type
     *
     * @param type Element type to search for
     * @return List of matching elements
     */
    override fun findByType(type: String?): List<UUIDElementData> {
        if (type.isNullOrBlank()) {
            Log.w(TAG, "findByType called with null/empty type")
            return emptyList()
        }

        return try {
            val elements = uuidCreator.findByType(type)
            elements.map { UUIDElementData.fromUUIDElement(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding elements by type", e)
            emptyList()
        }
    }

    /**
     * Find element by position (1-indexed)
     *
     * @param position Position number (1 = first, 2 = second, -1 = last)
     * @return Element data if found at position, null otherwise
     */
    override fun findByPosition(position: Int): UUIDElementData? {
        Log.d(TAG, "IPC: findByPosition(position=$position)")

        return try {
            val element = uuidCreator.findByPosition(position) ?: return null
            UUIDElementData.fromUUIDElement(element)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding element by position", e)
            null
        }
    }

    /**
     * Find element in direction from current element
     *
     * Directions: "left", "right", "up", "down", "forward", "backward",
     *             "next", "previous", "first", "last"
     *
     * @param fromUUID Starting element UUID
     * @param direction Direction to search
     * @return Element data if found, null otherwise
     */
    override fun findInDirection(fromUUID: String?, direction: String?): UUIDElementData? {
        Log.d(TAG, "IPC: findInDirection(fromUUID=$fromUUID, direction=$direction)")

        if (fromUUID.isNullOrBlank() || direction.isNullOrBlank()) {
            Log.w(TAG, "findInDirection called with null/empty parameters")
            return null
        }

        return try {
            val element = uuidCreator.findInDirection(fromUUID, direction) ?: return null
            UUIDElementData.fromUUIDElement(element)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding element in direction", e)
            null
        }
    }

    /**
     * Execute action on element
     *
     * Note: This method uses runBlocking to bridge AIDL's synchronous nature
     * with UUIDCreator's async executeAction method.
     *
     * @param vuid Target element UUID
     * @param action Action name (e.g., "click", "focus", "select")
     * @param parametersJson JSON string containing action parameters
     * @return true if action executed successfully, false otherwise
     */
    override fun executeAction(uuid: String?, action: String?, parametersJson: String?): Boolean {
        Log.d(TAG, "IPC: executeAction(uuid=$uuid, action=$action)")

        if (uuid.isNullOrBlank() || action.isNullOrBlank()) {
            Log.w(TAG, "executeAction called with null/empty parameters")
            return false
        }

        return try {
            // Parse JSON parameters
            val parameters = if (!parametersJson.isNullOrBlank()) {
                gson.fromJson(parametersJson, Map::class.java) as? Map<String, Any> ?: emptyMap()
            } else {
                emptyMap()
            }

            // Execute action (uses runBlocking to bridge sync AIDL with async UUIDCreator)
            runBlocking {
                uuidCreator.executeAction(uuid, action, parameters)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action", e)
            false
        }
    }

    /**
     * Process voice command
     *
     * Parses voice command and executes on matching element.
     *
     * Commands:
     * - "click element <name>" - Click element by name
     * - "select first/second/third" - Select element by position
     * - "move left/right/up/down" - Navigate spatially
     *
     * Note: Uses runBlocking to bridge sync AIDL with async UUIDCreator.
     *
     * @param command Voice command string
     * @return Command result with success status and details
     */
    override fun processVoiceCommand(command: String?): UUIDCommandResultData {
        Log.d(TAG, "IPC: processVoiceCommand(command=$command)")

        if (command.isNullOrBlank()) {
            Log.w(TAG, "processVoiceCommand called with null/empty command")
            return UUIDCommandResultData.failure("Command cannot be null or empty")
        }

        return try {
            // Process command (uses runBlocking to bridge sync AIDL with async UUIDCreator)
            val result = runBlocking {
                uuidCreator.processVoiceCommand(command)
            }

            UUIDCommandResultData.fromUUIDCommandResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing voice command", e)
            UUIDCommandResultData.failure(
                error = e.message ?: "Unknown error",
                action = "processVoiceCommand"
            )
        }
    }

    /**
     * Get all registered elements
     *
     * @return List of all registered elements
     */
    override fun getAllElements(): List<UUIDElementData> {
        Log.d(TAG, "IPC: getAllElements() called")

        return try {
            val elements = uuidCreator.getAllElements()
            elements.map { UUIDElementData.fromUUIDElement(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all elements", e)
            emptyList()
        }
    }

    /**
     * Get registry statistics
     *
     * Returns JSON with:
     * - count: total element count
     * - types: map of type -> count
     * - named: count of elements with names
     * - positioned: count of elements with positions
     *
     * @return JSON string with registry stats (count, types, etc.)
     */
    override fun getRegistryStats(): String {
        Log.d(TAG, "IPC: getRegistryStats() called")

        return try {
            val stats = uuidCreator.getStats()

            val statsMap = mapOf(
                "totalElements" to stats.totalElements,
                "enabledElements" to stats.enabledElements,
                "typeBreakdown" to stats.typeBreakdown,
                "nameIndexSize" to stats.nameIndexSize,
                "hierarchyIndexSize" to stats.hierarchyIndexSize,
                "timestamp" to System.currentTimeMillis()
            )

            gson.toJson(statsMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting registry stats", e)
            gson.toJson(mapOf(
                "error" to (e.message ?: "Unknown error"),
                "count" to 0
            ))
        }
    }

    /**
     * Clear all registered elements
     *
     * Warning: This removes all elements from the registry.
     * Use with caution.
     */
    override fun clearAll() {
        Log.d(TAG, "IPC: clearAll() called")

        try {
            uuidCreator.clearAll()
            Log.i(TAG, "All elements cleared from registry")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing registry", e)
        }
    }
}
