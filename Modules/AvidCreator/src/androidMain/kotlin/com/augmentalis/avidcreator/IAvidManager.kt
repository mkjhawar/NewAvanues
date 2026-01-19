package com.augmentalis.avidcreator.api

import com.augmentalis.avidcreator.*

/**
 * Public API interface for AvidManager library
 * Framework-agnostic AVID (Augmentalis Voice ID) management and targeting system
 */
interface IAvidManager {

    /**
     * Generate a new AVID
     */
    fun generateAvid(): String

    /**
     * Register an element with AVID
     */
    fun registerElement(element: AvidElement): String

    /**
     * Unregister an element
     */
    fun unregisterElement(avid: String): Boolean

    /**
     * Find element by AVID
     */
    fun findByAvid(avid: String): AvidElement?

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
    fun findInDirection(fromAvid: String, direction: String): AvidElement?

    /**
     * Execute action on element
     */
    suspend fun executeAction(avid: String, action: String, parameters: Map<String, Any> = emptyMap()): Boolean

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