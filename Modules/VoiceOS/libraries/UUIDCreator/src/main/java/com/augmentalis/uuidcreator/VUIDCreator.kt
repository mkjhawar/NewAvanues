/**
 * VUIDCreator.kt - Core VUID management class
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOS Development Team
 * Created: 2025-12-27
 *
 * Provides VUID (VoiceUniqueID) generation, element registration,
 * and targeting for voice-controlled UI interaction.
 */
package com.augmentalis.uuidcreator

import android.content.Context
import com.augmentalis.uuidcreator.core.VUIDGenerator
import com.augmentalis.uuidcreator.models.VUIDCommandResult
import com.augmentalis.uuidcreator.models.VUIDElement
import com.augmentalis.uuidcreator.models.VUIDPosition
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Core VUID management class for VoiceOS.
 *
 * Manages UI elements with voice-compatible unique identifiers,
 * providing registration, lookup, and command execution capabilities.
 *
 * Features:
 * - Element registration with automatic VUID generation
 * - Multi-index lookups (by VUID, name, type, position)
 * - Spatial navigation (left, right, up, down)
 * - Voice command processing
 * - Thread-safe operations via ConcurrentHashMap
 */
class VUIDCreator private constructor() {

    companion object {
        @Volatile
        private var instance: VUIDCreator? = null

        @Volatile
        private var applicationContext: Context? = null

        /**
         * Initialize VUIDCreator with application context.
         * This should be called once during app startup.
         *
         * @param context Application context
         */
        fun initialize(context: Context) {
            if (applicationContext == null) {
                synchronized(this) {
                    if (applicationContext == null) {
                        applicationContext = context.applicationContext
                        // Ensure instance is created
                        getInstance()
                    }
                }
            }
        }

        /**
         * Get singleton instance
         */
        fun getInstance(): VUIDCreator {
            return instance ?: synchronized(this) {
                instance ?: VUIDCreator().also { instance = it }
            }
        }
    }

    // Primary storage
    private val elements = ConcurrentHashMap<String, VUIDElement>()

    // Indexes for fast lookups
    private val nameIndex = ConcurrentHashMap<String, MutableSet<String>>()
    private val typeIndex = ConcurrentHashMap<String, MutableSet<String>>()
    private val hierarchyIndex = ConcurrentHashMap<String, MutableSet<String>>()

    // Position-ordered list for positional targeting
    private val orderedElements = mutableListOf<String>()

    // Command events flow for observers
    private val _commandEvents = MutableSharedFlow<VUIDCommandResult>()
    val commandEvents: SharedFlow<VUIDCommandResult> = _commandEvents.asSharedFlow()

    // ==================== Generation ====================

    /**
     * Generate a new VUID
     */
    fun generateUUID(): String = VUIDGenerator.generate()

    /**
     * Generate a new VUID (alias)
     */
    fun generateVUID(): String = VUIDGenerator.generate()

    // ==================== Registration ====================

    /**
     * Register an element
     * @return The element's VUID
     */
    fun registerElement(element: VUIDElement): String {
        elements[element.vuid] = element

        // Index by name
        element.name?.let { name ->
            nameIndex.getOrPut(name.lowercase()) { mutableSetOf() }.add(element.vuid)
        }

        // Index by type
        typeIndex.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.vuid)

        // Index by parent
        element.parent?.let { parent ->
            hierarchyIndex.getOrPut(parent) { mutableSetOf() }.add(element.vuid)
        }

        // Add to ordered list
        synchronized(orderedElements) {
            orderedElements.add(element.vuid)
        }

        return element.vuid
    }

    /**
     * Unregister an element
     * @return true if element was removed
     */
    fun unregisterElement(vuid: String): Boolean {
        val element = elements.remove(vuid) ?: return false

        // Remove from name index
        element.name?.let { name ->
            nameIndex[name.lowercase()]?.remove(vuid)
        }

        // Remove from type index
        typeIndex[element.type.lowercase()]?.remove(vuid)

        // Remove from hierarchy index
        element.parent?.let { parent ->
            hierarchyIndex[parent]?.remove(vuid)
        }

        // Remove from ordered list
        synchronized(orderedElements) {
            orderedElements.remove(vuid)
        }

        return true
    }

    // ==================== Lookups ====================

    /**
     * Find element by VUID
     */
    fun findByUUID(vuid: String): VUIDElement? = elements[vuid]

    /**
     * Find elements by name
     */
    fun findByName(name: String): List<VUIDElement> {
        val vuids = nameIndex[name.lowercase()] ?: return emptyList()
        return vuids.mapNotNull { elements[it] }
    }

    /**
     * Find elements by type
     */
    fun findByType(type: String): List<VUIDElement> {
        val vuids = typeIndex[type.lowercase()] ?: return emptyList()
        return vuids.mapNotNull { elements[it] }
    }

    /**
     * Find element by position (1-indexed)
     * @param position 1 = first, 2 = second, -1 = last
     */
    fun findByPosition(position: Int): VUIDElement? {
        synchronized(orderedElements) {
            if (orderedElements.isEmpty()) return null

            val index = when {
                position > 0 -> position - 1
                position < 0 -> orderedElements.size + position
                else -> return null
            }

            if (index < 0 || index >= orderedElements.size) return null

            return elements[orderedElements[index]]
        }
    }

    /**
     * Find element in direction from current element
     */
    fun findInDirection(fromVuid: String, direction: String): VUIDElement? {
        val current = elements[fromVuid] ?: return null
        val currentPos = current.position ?: return null

        return when (direction.lowercase()) {
            "left" -> findNearestInDirection(currentPos.x, currentPos.y, -1, 0)
            "right" -> findNearestInDirection(currentPos.x, currentPos.y, 1, 0)
            "up" -> findNearestInDirection(currentPos.x, currentPos.y, 0, -1)
            "down" -> findNearestInDirection(currentPos.x, currentPos.y, 0, 1)
            "next" -> findByRelativePosition(fromVuid, 1)
            "previous" -> findByRelativePosition(fromVuid, -1)
            "first" -> findByPosition(1)
            "last" -> findByPosition(-1)
            else -> null
        }
    }

    private fun findNearestInDirection(x: Float, y: Float, dx: Int, dy: Int): VUIDElement? {
        var nearest: VUIDElement? = null
        var minDistance = Float.MAX_VALUE

        for (element in elements.values) {
            val pos = element.position ?: continue

            val inDirection = when {
                dx < 0 -> pos.x < x
                dx > 0 -> pos.x > x
                dy < 0 -> pos.y < y
                dy > 0 -> pos.y > y
                else -> false
            }

            if (inDirection) {
                val distance = kotlin.math.abs(pos.x - x) + kotlin.math.abs(pos.y - y)
                if (distance < minDistance) {
                    minDistance = distance
                    nearest = element
                }
            }
        }

        return nearest
    }

    private fun findByRelativePosition(fromVuid: String, offset: Int): VUIDElement? {
        synchronized(orderedElements) {
            val currentIndex = orderedElements.indexOf(fromVuid)
            if (currentIndex < 0) return null

            val newIndex = currentIndex + offset
            if (newIndex < 0 || newIndex >= orderedElements.size) return null

            return elements[orderedElements[newIndex]]
        }
    }

    /**
     * Get all elements
     */
    fun getAllElements(): List<VUIDElement> = elements.values.toList()

    /**
     * Navigate to element in direction
     */
    fun navigate(fromVuid: String, direction: String): VUIDElement? {
        return findInDirection(fromVuid, direction)
    }

    /**
     * Register element with auto-generated VUID
     */
    fun registerWithAutoVUID(
        name: String,
        type: String,
        position: VUIDPosition? = null,
        actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap()
    ): String {
        val element = VUIDElement(
            name = name,
            type = type,
            position = position,
            actions = actions
        )
        return registerElement(element)
    }

    // ==================== Actions ====================

    /**
     * Execute action on element
     */
    suspend fun executeAction(vuid: String, action: String, parameters: Map<String, Any>): Boolean {
        val element = elements[vuid] ?: return false
        return element.executeAction(action, parameters)
    }

    /**
     * Process voice command
     */
    suspend fun processVoiceCommand(command: String): VUIDCommandResult {
        val startTime = System.currentTimeMillis()
        val words = command.lowercase().split(" ")

        return try {
            when {
                words.contains("click") -> processClickCommand(words, startTime)
                words.contains("select") -> processSelectCommand(words, startTime)
                words.contains("go") || words.contains("move") -> processMoveCommand(words, startTime)
                else -> VUIDCommandResult(
                    success = false,
                    error = "Unrecognized command: $command",
                    executionTime = System.currentTimeMillis() - startTime
                )
            }
        } catch (e: Exception) {
            VUIDCommandResult(
                success = false,
                error = e.message ?: "Unknown error",
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }

    private suspend fun processClickCommand(words: List<String>, startTime: Long): VUIDCommandResult {
        // Extract target name after "click"
        val clickIndex = words.indexOf("click")
        val targetName = words.drop(clickIndex + 1).joinToString(" ")

        val elements = findByName(targetName)
        if (elements.isEmpty()) {
            return VUIDCommandResult(
                success = false,
                error = "No element found with name: $targetName",
                executionTime = System.currentTimeMillis() - startTime
            )
        }

        val target = elements.first()
        val success = target.executeAction("click")

        return VUIDCommandResult(
            success = success,
            targetVUID = target.vuid,
            action = "click",
            message = if (success) "Clicked ${target.name}" else "Click failed",
            executionTime = System.currentTimeMillis() - startTime
        )
    }

    private fun processSelectCommand(words: List<String>, startTime: Long): VUIDCommandResult {
        val position = when {
            words.contains("first") -> 1
            words.contains("second") -> 2
            words.contains("third") -> 3
            words.contains("fourth") -> 4
            words.contains("fifth") -> 5
            words.contains("last") -> -1
            else -> words.find { it.toIntOrNull() != null }?.toInt() ?: 1
        }

        val element = findByPosition(position)
            ?: return VUIDCommandResult(
                success = false,
                error = "No element at position $position",
                executionTime = System.currentTimeMillis() - startTime
            )

        return VUIDCommandResult(
            success = true,
            targetVUID = element.vuid,
            action = "select",
            message = "Selected ${element.name ?: element.type} at position $position",
            executionTime = System.currentTimeMillis() - startTime
        )
    }

    private fun processMoveCommand(words: List<String>, startTime: Long): VUIDCommandResult {
        val direction = words.find { it in listOf("left", "right", "up", "down", "next", "previous") }
            ?: return VUIDCommandResult(
                success = false,
                error = "No direction specified",
                executionTime = System.currentTimeMillis() - startTime
            )

        // Use first element as starting point if no current focus
        val fromVuid = orderedElements.firstOrNull()
            ?: return VUIDCommandResult(
                success = false,
                error = "No elements registered",
                executionTime = System.currentTimeMillis() - startTime
            )

        val target = findInDirection(fromVuid, direction)
            ?: return VUIDCommandResult(
                success = false,
                error = "No element found in direction: $direction",
                executionTime = System.currentTimeMillis() - startTime
            )

        return VUIDCommandResult(
            success = true,
            targetVUID = target.vuid,
            action = "move",
            message = "Moved $direction to ${target.name ?: target.type}",
            executionTime = System.currentTimeMillis() - startTime
        )
    }

    // ==================== Stats ====================

    /**
     * Registry statistics
     */
    data class Stats(
        val totalElements: Int,
        val enabledElements: Int,
        val typeBreakdown: Map<String, Int>,
        val nameIndexSize: Int,
        val hierarchyIndexSize: Int
    )

    /**
     * Get registry statistics
     */
    fun getStats(): Stats {
        val typeBreakdown = mutableMapOf<String, Int>()
        for ((type, vuids) in typeIndex) {
            typeBreakdown[type] = vuids.size
        }

        return Stats(
            totalElements = elements.size,
            enabledElements = elements.values.count { it.isEnabled },
            typeBreakdown = typeBreakdown,
            nameIndexSize = nameIndex.size,
            hierarchyIndexSize = hierarchyIndex.size
        )
    }

    // ==================== Clear ====================

    /**
     * Clear all elements
     */
    fun clearAll() {
        elements.clear()
        nameIndex.clear()
        typeIndex.clear()
        hierarchyIndex.clear()
        synchronized(orderedElements) {
            orderedElements.clear()
        }
    }
}
