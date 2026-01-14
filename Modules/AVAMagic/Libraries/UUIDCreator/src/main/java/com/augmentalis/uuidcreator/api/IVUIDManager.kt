package com.augmentalis.uuidcreator.api

import com.augmentalis.uuidcreator.models.*

/**
 * Public API interface for VUIDManager library
 * Framework-agnostic VUID (VoiceUniqueID) management and targeting system
 */
interface IVUIDManager {

    /**
     * Generate a new VUID
     */
    fun generateVUID(): String

    /**
     * Register an element with VUID
     */
    fun registerElement(element: VUIDElement): String

    /**
     * Unregister an element
     */
    fun unregisterElement(vuid: String): Boolean

    /**
     * Find element by VUID
     */
    fun findByVUID(vuid: String): VUIDElement?

    /**
     * Find elements by name
     */
    fun findByName(name: String): List<VUIDElement>

    /**
     * Find elements by type
     */
    fun findByType(type: String): List<VUIDElement>

    /**
     * Find element by position
     */
    fun findByPosition(position: Int): VUIDElement?

    /**
     * Find elements in direction
     */
    fun findInDirection(fromVUID: String, direction: String): VUIDElement?

    /**
     * Execute action on element
     */
    suspend fun executeAction(vuid: String, action: String, parameters: Map<String, Any> = emptyMap()): Boolean

    /**
     * Process voice command
     */
    suspend fun processVoiceCommand(command: String): UUIDCommandResult

    /**
     * Get all registered elements
     */
    fun getAllElements(): List<VUIDElement>

    /**
     * Clear all registrations
     */
    fun clearAll()
}