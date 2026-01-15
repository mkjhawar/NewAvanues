package com.augmentalis.avidcreator.api

import com.augmentalis.avidcreator.models.*

/**
 * Public API interface for AvidManager library
 * Framework-agnostic VUID (VoiceUniqueID) management and targeting system
 */
interface IAvidManager {

    /**
     * Generate a new VUID
     */
    fun generateVUID(): String

    /**
     * Register an element with VUID
     */
    fun registerElement(element: AvidElement): String

    /**
     * Unregister an element
     */
    fun unregisterElement(vuid: String): Boolean

    /**
     * Find element by VUID
     */
    fun findByVUID(vuid: String): AvidElement?

    /**
     * Find elements by name
     */
    fun findByName(name: String): List<AvidElement>

    /**
     * Find elements by type
     */
    fun findByType(type: String): List<AvidElement>

    /**
     * Find element by position
     */
    fun findByPosition(position: Int): AvidElement?

    /**
     * Find elements in direction
     */
    fun findInDirection(fromVUID: String, direction: String): AvidElement?

    /**
     * Execute action on element
     */
    suspend fun executeAction(vuid: String, action: String, parameters: Map<String, Any> = emptyMap()): Boolean

    /**
     * Process voice command
     */
    suspend fun processVoiceCommand(command: String): AvidCommandResult

    /**
     * Get all registered elements
     */
    fun getAllElements(): List<AvidElement>

    /**
     * Clear all registrations
     */
    fun clearAll()
}