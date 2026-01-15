/**
 * AvidCreator.kt - Core AVID management class (formerly AvidCreator/AvidCreator)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOS Development Team
 * Created: 2025-12-27
 * Updated: 2026-01-15 - Migrated to AVID naming
 *
 * Provides AVID (Augmentalis Voice ID) generation, element registration,
 * and targeting for voice-controlled UI interaction.
 */
package com.augmentalis.avidcreator

import android.content.Context
import com.augmentalis.avidcreator.core.AvidGenerator
import com.augmentalis.avidcreator.models.AvidCommandResult
import com.augmentalis.avidcreator.models.AvidElement
import com.augmentalis.avidcreator.models.AvidPosition
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Core AVID management class for VoiceOS.
 *
 * Manages UI elements with voice-compatible unique identifiers,
 * providing registration, lookup, and command execution capabilities.
 *
 * Features:
 * - Element registration with automatic AVID generation
 * - Multi-index lookups (by AVID, name, type, position)
 * - Spatial navigation (left, right, up, down)
 * - Voice command processing
 * - Thread-safe operations via ConcurrentHashMap
 */
class AvidElementManager private constructor() {

    companion object {
        @Volatile
        private var instance: AvidElementManager? = null

        @Volatile
        private var applicationContext: Context? = null

        /**
         * Initialize AvidElementManager with application context.
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
        fun getInstance(): AvidElementManager {
            return instance ?: synchronized(this) {
                instance ?: AvidElementManager().also { instance = it }
            }
        }
    }

    // Primary storage
    private val elements = ConcurrentHashMap<String, AvidElement>()

    // Indexes for fast lookups
    private val nameIndex = ConcurrentHashMap<String, MutableSet<String>>()
    private val typeIndex = ConcurrentHashMap<String, MutableSet<String>>()
    private val hierarchyIndex = ConcurrentHashMap<String, MutableSet<String>>()

    // Position-ordered list for positional targeting
    private val orderedElements = mutableListOf<String>()

    // Command events flow for observers
    private val _commandEvents = MutableSharedFlow<AvidCommandResult>()
    val commandEvents: SharedFlow<AvidCommandResult> = _commandEvents.asSharedFlow()

    // ==================== Generation ====================

    /**
     * Generate a new AVID
     */
    fun generateAvid(): String = AvidGenerator.generate()

    // ==================== Registration ====================

    /**
     * Register an element
     * @return The element's AVID
     */
    fun registerElement(element: AvidElement): String {
        elements[element.avid] = element

        // Index by name
        element.name?.let { name ->
            nameIndex.getOrPut(name.lowercase()) { mutableSetOf() }.add(element.avid)
        }

        // Index by type
        typeIndex.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.avid)

        // Index by parent
        element.parent?.let { parent ->
            hierarchyIndex.getOrPut(parent) { mutableSetOf() }.add(element.avid)
        }

        // Add to ordered list
        synchronized(orderedElements) {
            orderedElements.add(element.avid)
        }

        return element.avid
    }

    /**
     * Unregister an element
     * @return true if element was removed
     */
    fun unregisterElement(avid: String): Boolean {
        val element = elements.remove(avid) ?: return false

        // Remove from name index
        element.name?.let { name ->
            nameIndex[name.lowercase()]?.remove(avid)
        }

        // Remove from type index
        typeIndex[element.type.lowercase()]?.remove(avid)

        // Remove from hierarchy index
        element.parent?.let { parent ->
            hierarchyIndex[parent]?.remove(avid)
        }

        // Remove from ordered list
        synchronized(orderedElements) {
            orderedElements.remove(avid)
        }

        return true
    }

    // ==================== Lookups ====================

    /**
     * Find element by AVID
     */
    fun findByAvid(avid: String): AvidElement? = elements[avid]

    /**
     * Find elements by name
     */
    fun findByName(name: String): List<AvidElement> {
        val avids = nameIndex[name.lowercase()] ?: return emptyList()
        return avids.mapNotNull { elements[it] }
    }

    /**
     * Find elements by type
     */
    fun findByType(type: String): List<AvidElement> {
        val avids = typeIndex[type.lowercase()] ?: return emptyList()
        return avids.mapNotNull { elements[it] }
    }

    /**
     * Find element by position (1-indexed)
     * @param position 1 = first, 2 = second, -1 = last
     */
    fun findByPosition(position: Int): AvidElement? {
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
    fun findInDirection(fromAvid: String, direction: String): AvidElement? {
        val current = elements[fromAvid] ?: return null
        val currentPos = current.position ?: return null

        return when (direction.lowercase()) {
            "left" -> findNearestInDirection(currentPos.x, currentPos.y, -1, 0)
            "right" -> findNearestInDirection(currentPos.x, currentPos.y, 1, 0)
            "up" -> findNearestInDirection(currentPos.x, currentPos.y, 0, -1)
            "down" -> findNearestInDirection(currentPos.x, currentPos.y, 0, 1)
            "next" -> findByRelativePosition(fromAvid, 1)
            "previous" -> findByRelativePosition(fromAvid, -1)
            "first" -> findByPosition(1)
            "last" -> findByPosition(-1)
            else -> null
        }
    }

    private fun findNearestInDirection(x: Float, y: Float, dx: Int, dy: Int): AvidElement? {
        var nearest: AvidElement? = null
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

    private fun findByRelativePosition(fromAvid: String, offset: Int): AvidElement? {
        synchronized(orderedElements) {
            val currentIndex = orderedElements.indexOf(fromAvid)
            if (currentIndex < 0) return null

            val newIndex = currentIndex + offset
            if (newIndex < 0 || newIndex >= orderedElements.size) return null

            return elements[orderedElements[newIndex]]
        }
    }

    /**
     * Get all elements
     */
    fun getAllElements(): List<AvidElement> = elements.values.toList()

    /**
     * Navigate to element in direction
     */
    fun navigate(fromAvid: String, direction: String): AvidElement? {
        return findInDirection(fromAvid, direction)
    }

    /**
     * Register element with auto-generated AVID
     */
    fun registerWithAutoAvid(
        name: String,
        type: String,
        position: AvidPosition? = null,
        actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap()
    ): String {
        val element = AvidElement(
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
    suspend fun executeAction(avid: String, action: String, parameters: Map<String, Any>): Boolean {
        val element = elements[avid] ?: return false
        return element.executeAction(action, parameters)
    }

    /**
     * Process voice command
     */
    suspend fun processVoiceCommand(command: String): AvidCommandResult {
        val startTime = System.currentTimeMillis()
        val words = command.lowercase().split(" ")

        return try {
            when {
                words.contains("click") -> processClickCommand(words, startTime)
                words.contains("select") -> processSelectCommand(words, startTime)
                words.contains("go") || words.contains("move") -> processMoveCommand(words, startTime)
                else -> AvidCommandResult(
                    success = false,
                    error = "Unrecognized command: $command",
                    executionTime = System.currentTimeMillis() - startTime
                )
            }
        } catch (e: Exception) {
            AvidCommandResult(
                success = false,
                error = e.message ?: "Unknown error",
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }

    private suspend fun processClickCommand(words: List<String>, startTime: Long): AvidCommandResult {
        // Extract target name after "click"
        val clickIndex = words.indexOf("click")
        val targetName = words.drop(clickIndex + 1).joinToString(" ")

        val elements = findByName(targetName)
        if (elements.isEmpty()) {
            return AvidCommandResult(
                success = false,
                error = "No element found with name: $targetName",
                executionTime = System.currentTimeMillis() - startTime
            )
        }

        val target = elements.first()
        val success = target.executeAction("click")

        return AvidCommandResult(
            success = success,
            targetAvid = target.avid,
            action = "click",
            message = if (success) "Clicked ${target.name}" else "Click failed",
            executionTime = System.currentTimeMillis() - startTime
        )
    }

    private fun processSelectCommand(words: List<String>, startTime: Long): AvidCommandResult {
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
            ?: return AvidCommandResult(
                success = false,
                error = "No element at position $position",
                executionTime = System.currentTimeMillis() - startTime
            )

        return AvidCommandResult(
            success = true,
            targetAvid = element.avid,
            action = "select",
            message = "Selected ${element.name ?: element.type} at position $position",
            executionTime = System.currentTimeMillis() - startTime
        )
    }

    private fun processMoveCommand(words: List<String>, startTime: Long): AvidCommandResult {
        val direction = words.find { it in listOf("left", "right", "up", "down", "next", "previous") }
            ?: return AvidCommandResult(
                success = false,
                error = "No direction specified",
                executionTime = System.currentTimeMillis() - startTime
            )

        // Use first element as starting point if no current focus
        val fromAvid = orderedElements.firstOrNull()
            ?: return AvidCommandResult(
                success = false,
                error = "No elements registered",
                executionTime = System.currentTimeMillis() - startTime
            )

        val target = findInDirection(fromAvid, direction)
            ?: return AvidCommandResult(
                success = false,
                error = "No element found in direction: $direction",
                executionTime = System.currentTimeMillis() - startTime
            )

        return AvidCommandResult(
            success = true,
            targetAvid = target.avid,
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
        for ((type, avids) in typeIndex) {
            typeBreakdown[type] = avids.size
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

// Backward compatibility aliases
@Deprecated("Use AvidElementManager instead", ReplaceWith("AvidElementManager"))
typealias AvidCreator = AvidElementManager

@Deprecated("Use AvidElementManager instead", ReplaceWith("AvidElementManager"))
typealias AvidCreator = AvidElementManager
