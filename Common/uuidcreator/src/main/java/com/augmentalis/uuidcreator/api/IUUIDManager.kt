package com.augmentalis.uuidcreator.api

import com.augmentalis.uuidcreator.models.*

/**
 * Public API interface for UUIDManager library
 * Framework-agnostic UUID management and targeting system
 */
interface IUUIDManager {

    /**
     * Generate a new UUID
     */
    fun generateUUID(): String

    /**
     * Register an element with UUID
     */
    suspend fun registerElement(element: UUIDElement): String

    /**
     * Unregister an element
     */
    suspend fun unregisterElement(uuid: String): Boolean

    /**
     * Find element by UUID
     */
    fun findByUUID(uuid: String): UUIDElement?

    /**
     * Find elements by name
     */
    fun findByName(name: String): List<UUIDElement>

    /**
     * Find elements by type
     */
    fun findByType(type: String): List<UUIDElement>

    /**
     * Find element by position
     */
    fun findByPosition(position: Int): UUIDElement?

    /**
     * Find elements in direction
     */
    fun findInDirection(fromUUID: String, direction: String): UUIDElement?

    /**
     * Execute action on element
     */
    suspend fun executeAction(uuid: String, action: String, parameters: Map<String, Any> = emptyMap()): Boolean

    /**
     * Process voice command
     */
    suspend fun processVoiceCommand(command: String): UUIDCommandResult

    /**
     * Get all registered elements
     */
    fun getAllElements(): List<UUIDElement>

    /**
     * Clear all registrations
     */
    suspend fun clearAll()
}