/**
 * AvidServiceBinder.kt - IPC service binder implementation for AvidCreator
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Code Quality Expert
 * Created: 2025-11-10
 * Updated: 2026-01-15 - Migrated to AVID naming
 */
package com.augmentalis.avidcreator

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking

/**
 * AVID Creator Service AIDL implementation
 *
 * Exposes AvidElementManager functionality via IPC for use by external apps.
 * This binder wraps the AvidElementManager instance and provides thread-safe
 * access to AVID management and targeting functionality across process boundaries.
 *
 * Features:
 * - Thread-safe access to AvidElementManager instance
 * - Automatic conversion between Parcelable and internal types
 * - Coroutine support for async operations (using runBlocking for AIDL)
 * - Error handling and logging
 * - JSON serialization for complex data
 *
 * Note on Threading:
 * AIDL methods are synchronous, but AvidElementManager uses coroutines internally.
 * We use runBlocking to bridge sync AIDL calls with async AvidElementManager operations.
 *
 * Usage:
 * ```kotlin
 * // In service's onBind()
 * private var avidBinder: AvidServiceBinder? = null
 *
 * override fun onBind(intent: Intent?): IBinder? {
 *     return when (intent?.action) {
 *         "com.augmentalis.avidcreator.BIND_IPC" -> {
 *             val avidManager = AvidElementManager.getInstance()
 *             avidBinder = AvidServiceBinder(avidManager)
 *             avidBinder!!.asBinder()
 *         }
 *         else -> super.onBind(intent)
 *     }
 * }
 * ```
 */
class AvidServiceBinder(
    private val avidManager: AvidElementManager
) : IAvidCreatorService.Stub() {

    companion object {
        private const val TAG = "AvidServiceBinder"
    }

    /**
     * JSON serializer for stats and parameters
     */
    private val gson = Gson()

    /**
     * Generate a new AVID
     *
     * @return Newly generated AVID string
     */
    override fun generateAvid(): String {
        Log.d(TAG, "IPC: generateAvid() called")
        return try {
            avidManager.generateAvid()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating AVID", e)
            ""
        }
    }

    /**
     * Register an element with automatic AVID generation
     *
     * @param elementData Element data to register
     * @return Generated AVID for the registered element
     */
    override fun registerElement(elementData: AvidElementData?): String {
        Log.d(TAG, "IPC: registerElement(elementData=${elementData?.name})")

        if (elementData == null) {
            Log.w(TAG, "registerElement called with null elementData")
            return ""
        }

        return try {
            val element = elementData.toAvidElement()
            avidManager.registerElement(element)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering element", e)
            ""
        }
    }

    /**
     * Unregister an element by AVID
     *
     * @param avid AVID of element to unregister
     * @return true if element was unregistered, false if not found
     */
    override fun unregisterElement(avid: String?): Boolean {
        Log.d(TAG, "IPC: unregisterElement(avid=$avid)")

        if (avid.isNullOrBlank()) {
            Log.w(TAG, "unregisterElement called with null/empty AVID")
            return false
        }

        return try {
            avidManager.unregisterElement(avid)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering element", e)
            false
        }
    }

    /**
     * Find element by AVID
     *
     * @param avid AVID to search for
     * @return Element data if found, null otherwise
     */
    override fun findByAvid(avid: String?): AvidElementData? {
        if (avid.isNullOrBlank()) {
            Log.w(TAG, "findByAvid called with null/empty AVID")
            return null
        }

        return try {
            val element = avidManager.findByAvid(avid) ?: return null
            AvidElementData.fromAvidElement(element)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding element by AVID", e)
            null
        }
    }

    /**
     * Find elements by name
     *
     * @param name Element name to search for
     * @return List of matching elements
     */
    override fun findByName(name: String?): List<AvidElementData> {
        if (name.isNullOrBlank()) {
            Log.w(TAG, "findByName called with null/empty name")
            return emptyList()
        }

        return try {
            val elements = avidManager.findByName(name)
            elements.map { AvidElementData.fromAvidElement(it) }
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
    override fun findByType(type: String?): List<AvidElementData> {
        if (type.isNullOrBlank()) {
            Log.w(TAG, "findByType called with null/empty type")
            return emptyList()
        }

        return try {
            val elements = avidManager.findByType(type)
            elements.map { AvidElementData.fromAvidElement(it) }
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
    override fun findByPosition(position: Int): AvidElementData? {
        Log.d(TAG, "IPC: findByPosition(position=$position)")

        return try {
            val element = avidManager.findByPosition(position) ?: return null
            AvidElementData.fromAvidElement(element)
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
     * @param fromAvid Starting element AVID
     * @param direction Direction to search
     * @return Element data if found, null otherwise
     */
    override fun findInDirection(fromAvid: String?, direction: String?): AvidElementData? {
        Log.d(TAG, "IPC: findInDirection(fromAvid=$fromAvid, direction=$direction)")

        if (fromAvid.isNullOrBlank() || direction.isNullOrBlank()) {
            Log.w(TAG, "findInDirection called with null/empty parameters")
            return null
        }

        return try {
            val element = avidManager.findInDirection(fromAvid, direction) ?: return null
            AvidElementData.fromAvidElement(element)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding element in direction", e)
            null
        }
    }

    /**
     * Execute action on element
     *
     * Note: This method uses runBlocking to bridge AIDL's synchronous nature
     * with AvidElementManager's async executeAction method.
     *
     * @param avid Target element AVID
     * @param action Action name (e.g., "click", "focus", "select")
     * @param parametersJson JSON string containing action parameters
     * @return true if action executed successfully, false otherwise
     */
    override fun executeAction(avid: String?, action: String?, parametersJson: String?): Boolean {
        Log.d(TAG, "IPC: executeAction(avid=$avid, action=$action)")

        if (avid.isNullOrBlank() || action.isNullOrBlank()) {
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

            // Execute action (uses runBlocking to bridge sync AIDL with async AvidElementManager)
            runBlocking {
                avidManager.executeAction(avid, action, parameters)
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
     * Note: Uses runBlocking to bridge sync AIDL with async AvidElementManager.
     *
     * @param command Voice command string
     * @return Command result with success status and details
     */
    override fun processVoiceCommand(command: String?): AvidCommandResultData {
        Log.d(TAG, "IPC: processVoiceCommand(command=$command)")

        if (command.isNullOrBlank()) {
            Log.w(TAG, "processVoiceCommand called with null/empty command")
            return AvidCommandResultData.failure("Command cannot be null or empty")
        }

        return try {
            // Process command (uses runBlocking to bridge sync AIDL with async AvidElementManager)
            val result = runBlocking {
                avidManager.processVoiceCommand(command)
            }

            AvidCommandResultData.fromAvidCommandResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing voice command", e)
            AvidCommandResultData.failure(
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
    override fun getAllElements(): List<AvidElementData> {
        Log.d(TAG, "IPC: getAllElements() called")

        return try {
            val elements = avidManager.getAllElements()
            elements.map { AvidElementData.fromAvidElement(it) }
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
            val stats = avidManager.getStats()

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
            avidManager.clearAll()
            Log.i(TAG, "All elements cleared from registry")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing registry", e)
        }
    }
}

// Backward compatibility alias
@Deprecated("Use AvidServiceBinder instead", ReplaceWith("AvidServiceBinder"))
typealias VuidServiceBinder = AvidServiceBinder
