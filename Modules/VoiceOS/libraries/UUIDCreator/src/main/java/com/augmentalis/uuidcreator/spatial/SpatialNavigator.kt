package com.augmentalis.uuidcreator.spatial

import com.augmentalis.uuidcreator.core.VUIDRegistry
import com.augmentalis.uuidcreator.models.*
import kotlin.math.*

/**
 * Spatial navigation system for UUID elements
 * Handles directional navigation and position-based targeting
 */
class SpatialNavigator(private val registry: VUIDRegistry) {
    
    /**
     * Navigation directions
     */
    enum class Direction {
        LEFT, RIGHT, UP, DOWN, FORWARD, BACKWARD,
        NORTH, SOUTH, EAST, WEST,
        NEXT, PREVIOUS, FIRST, LAST
    }
    
    /**
     * Navigation result
     */
    data class NavigationResult(
        val target: VUIDElement?,
        val distance: Float = Float.MAX_VALUE,
        val direction: Direction,
        val confidence: Float = 0f
    )
    
    /**
     * Navigate from one element to another in specified direction
     */
    fun navigate(fromUUID: String, direction: Direction): NavigationResult {
        val sourceElement = registry.findByVUID(fromUUID) 
            ?: return NavigationResult(null, Float.MAX_VALUE, direction, 0f)
        
        val sourcePos = sourceElement.position 
            ?: return NavigationResult(null, Float.MAX_VALUE, direction, 0f)
        
        return when (direction) {
            Direction.LEFT, Direction.WEST -> navigateLeft(sourcePos)
            Direction.RIGHT, Direction.EAST -> navigateRight(sourcePos)
            Direction.UP, Direction.NORTH -> navigateUp(sourcePos)
            Direction.DOWN, Direction.SOUTH -> navigateDown(sourcePos)
            Direction.FORWARD -> navigateForward(sourcePos)
            Direction.BACKWARD -> navigateBackward(sourcePos)
            Direction.NEXT -> navigateNext(sourceElement)
            Direction.PREVIOUS -> navigatePrevious(sourceElement)
            Direction.FIRST -> navigateFirst()
            Direction.LAST -> navigateLast()
        }
    }
    
    /**
     * Navigate to element at specific position (1st, 2nd, 3rd, etc.)
     */
    fun navigateToPosition(position: Int): NavigationResult {
        val elements = registry.getEnabledElements()
            .filter { it.position != null }
            .sortedWith(compareBy({ it.position?.row }, { it.position?.column }, { it.position?.index }))
        
        val targetIndex = if (position == -1) elements.size - 1 else position - 1
        val target = elements.getOrNull(targetIndex)
        
        return NavigationResult(
            target = target,
            distance = if (target != null) 0f else Float.MAX_VALUE,
            direction = if (position == -1) Direction.LAST else Direction.FIRST,
            confidence = if (target != null) 1.0f else 0f
        )
    }
    
    /**
     * Find nearest element in any direction
     */
    fun findNearest(fromUUID: String, maxDistance: Float = Float.MAX_VALUE): NavigationResult? {
        val sourceElement = registry.findByVUID(fromUUID) ?: return null
        val sourcePos = sourceElement.position ?: return null

        val nearest = registry.getEnabledElements()
            .filter { it.uuid != fromUUID && it.position != null }
            .minByOrNull { element ->
                element.position?.let { calculateDistance(sourcePos, it) } ?: Float.MAX_VALUE
            }

        return nearest?.position?.let { nearestPos ->
            val distance = calculateDistance(sourcePos, nearestPos)
            if (distance <= maxDistance) {
                NavigationResult(
                    target = nearest,
                    distance = distance,
                    direction = calculateDirection(sourcePos, nearestPos),
                    confidence = 1.0f - (distance / maxDistance)
                )
            } else null
        }
    }
    
    /**
     * Navigate left
     */
    private fun navigateLeft(sourcePos: UUIDPosition): NavigationResult {
        val candidates = registry.getEnabledElements()
            .filter { element -> element.position?.let { it.x < sourcePos.x } == true }

        return findBestCandidate(candidates) { pos ->
            val dx = sourcePos.x - pos.x
            val dy = abs(sourcePos.y - pos.y)
            dx + dy * 0.5f // Prefer horizontal movement
        }?.let { element ->
            element.position?.let { pos ->
                NavigationResult(element, calculateDistance(sourcePos, pos), Direction.LEFT, 0.9f)
            }
        } ?: NavigationResult(null, Float.MAX_VALUE, Direction.LEFT, 0f)
    }
    
    /**
     * Navigate right
     */
    private fun navigateRight(sourcePos: UUIDPosition): NavigationResult {
        val candidates = registry.getEnabledElements()
            .filter { element -> element.position?.let { it.x > sourcePos.x } == true }

        return findBestCandidate(candidates) { pos ->
            val dx = pos.x - sourcePos.x
            val dy = abs(sourcePos.y - pos.y)
            dx + dy * 0.5f
        }?.let { element ->
            element.position?.let { pos ->
                NavigationResult(element, calculateDistance(sourcePos, pos), Direction.RIGHT, 0.9f)
            }
        } ?: NavigationResult(null, Float.MAX_VALUE, Direction.RIGHT, 0f)
    }
    
    /**
     * Navigate up
     */
    private fun navigateUp(sourcePos: UUIDPosition): NavigationResult {
        val candidates = registry.getEnabledElements()
            .filter { element -> element.position?.let { it.y < sourcePos.y } == true }

        return findBestCandidate(candidates) { pos ->
            val dx = abs(sourcePos.x - pos.x)
            val dy = sourcePos.y - pos.y
            dy + dx * 0.5f
        }?.let { element ->
            element.position?.let { pos ->
                NavigationResult(element, calculateDistance(sourcePos, pos), Direction.UP, 0.9f)
            }
        } ?: NavigationResult(null, Float.MAX_VALUE, Direction.UP, 0f)
    }
    
    /**
     * Navigate down
     */
    private fun navigateDown(sourcePos: UUIDPosition): NavigationResult {
        val candidates = registry.getEnabledElements()
            .filter { element -> element.position?.let { it.y > sourcePos.y } == true }

        return findBestCandidate(candidates) { pos ->
            val dx = abs(sourcePos.x - pos.x)
            val dy = pos.y - sourcePos.y
            dy + dx * 0.5f
        }?.let { element ->
            element.position?.let { pos ->
                NavigationResult(element, calculateDistance(sourcePos, pos), Direction.DOWN, 0.9f)
            }
        } ?: NavigationResult(null, Float.MAX_VALUE, Direction.DOWN, 0f)
    }
    
    /**
     * Navigate forward (Z-axis)
     */
    private fun navigateForward(sourcePos: UUIDPosition): NavigationResult {
        val candidates = registry.getEnabledElements()
            .filter { element -> element.position?.let { it.z > sourcePos.z } == true }

        return findBestCandidate(candidates) { pos ->
            abs(pos.z - sourcePos.z)
        }?.let { element ->
            element.position?.let { pos ->
                NavigationResult(element, calculateDistance(sourcePos, pos), Direction.FORWARD, 0.8f)
            }
        } ?: NavigationResult(null, Float.MAX_VALUE, Direction.FORWARD, 0f)
    }
    
    /**
     * Navigate backward (Z-axis)
     */
    private fun navigateBackward(sourcePos: UUIDPosition): NavigationResult {
        val candidates = registry.getEnabledElements()
            .filter { element -> element.position?.let { it.z < sourcePos.z } == true }

        return findBestCandidate(candidates) { pos ->
            abs(sourcePos.z - pos.z)
        }?.let { element ->
            element.position?.let { pos ->
                NavigationResult(element, calculateDistance(sourcePos, pos), Direction.BACKWARD, 0.8f)
            }
        } ?: NavigationResult(null, Float.MAX_VALUE, Direction.BACKWARD, 0f)
    }
    
    /**
     * Navigate to next element (by index)
     */
    private fun navigateNext(sourceElement: UUIDElement): NavigationResult {
        val currentIndex = sourceElement.position?.index ?: 0
        val candidates = registry.getEnabledElements()
            .filter { element -> element.position?.let { it.index > currentIndex } == true }
            .sortedBy { it.position?.index ?: Int.MAX_VALUE }

        return candidates.firstOrNull()?.let {
            NavigationResult(it, 1f, Direction.NEXT, 1.0f)
        } ?: NavigationResult(null, Float.MAX_VALUE, Direction.NEXT, 0f)
    }
    
    /**
     * Navigate to previous element (by index)
     */
    private fun navigatePrevious(sourceElement: UUIDElement): NavigationResult {
        val currentIndex = sourceElement.position?.index ?: 0
        val candidates = registry.getEnabledElements()
            .filter { element -> element.position?.let { it.index < currentIndex } == true }
            .sortedByDescending { it.position?.index ?: Int.MIN_VALUE }

        return candidates.firstOrNull()?.let {
            NavigationResult(it, 1f, Direction.PREVIOUS, 1.0f)
        } ?: NavigationResult(null, Float.MAX_VALUE, Direction.PREVIOUS, 0f)
    }
    
    /**
     * Navigate to first element
     */
    private fun navigateFirst(): NavigationResult {
        val first = registry.getEnabledElements()
            .filter { it.position != null }
            .minByOrNull { it.position?.index ?: Int.MAX_VALUE }

        return NavigationResult(first, 0f, Direction.FIRST, if (first != null) 1.0f else 0f)
    }

    /**
     * Navigate to last element
     */
    private fun navigateLast(): NavigationResult {
        val last = registry.getEnabledElements()
            .filter { it.position != null }
            .maxByOrNull { it.position?.index ?: Int.MIN_VALUE }

        return NavigationResult(last, 0f, Direction.LAST, if (last != null) 1.0f else 0f)
    }
    
    /**
     * Find best candidate using scoring function
     */
    private fun findBestCandidate(
        candidates: List<UUIDElement>,
        scoreFunction: (UUIDPosition) -> Float
    ): UUIDElement? {
        return candidates.minByOrNull { element ->
            element.position?.let { scoreFunction(it) } ?: Float.MAX_VALUE
        }
    }
    
    /**
     * Calculate distance between two positions
     */
    private fun calculateDistance(pos1: UUIDPosition, pos2: UUIDPosition): Float {
        val dx = pos1.x - pos2.x
        val dy = pos1.y - pos2.y
        val dz = pos1.z - pos2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    /**
     * Calculate primary direction between two positions
     */
    private fun calculateDirection(from: UUIDPosition, to: UUIDPosition): Direction {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val dz = to.z - from.z
        
        val absDx = abs(dx)
        val absDy = abs(dy)
        val absDz = abs(dz)
        
        return when {
            absDx > absDy && absDx > absDz -> if (dx > 0) Direction.RIGHT else Direction.LEFT
            absDy > absDx && absDy > absDz -> if (dy > 0) Direction.DOWN else Direction.UP
            absDz > absDx && absDz > absDy -> if (dz > 0) Direction.FORWARD else Direction.BACKWARD
            else -> Direction.RIGHT // Default
        }
    }
}