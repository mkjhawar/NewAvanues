package com.augmentalis.uuidmanager.targeting

import com.augmentalis.uuidmanager.core.UUIDRegistry
import com.augmentalis.uuidmanager.models.*

/**
 * Resolves UI element targeting using various strategies
 * Extracted from UIKitVoiceCommandSystem for framework-agnostic use
 */
class TargetResolver(private val registry: UUIDRegistry) {
    
    /**
     * Target types for different resolution strategies
     */
    enum class TargetType {
        UUID,           // Direct UUID targeting
        NAME,           // Element name/label
        TYPE,           // Element type (button, text, etc.)
        POSITION,       // Spatial position (first, last, third)
        HIERARCHY,      // Parent/child navigation
        CONTEXT,        // Context-based targeting
        RECENT,         // Recently used elements
        PROXIMITY       // Nearest element
    }
    
    /**
     * Target resolution request
     */
    data class TargetRequest(
        val type: TargetType,
        val value: String? = null,
        val position: Int? = null,
        val direction: String? = null,
        val fromUUID: String? = null,
        val filters: Map<String, Any> = emptyMap()
    )
    
    /**
     * Target resolution result
     */
    data class TargetResult(
        val elements: List<UUIDElement>,
        val confidence: Float = 1.0f,
        val strategy: String = "",
        val fallbackUsed: Boolean = false
    )
    
    /**
     * Resolve target using specified strategy
     */
    fun resolve(request: TargetRequest): TargetResult {
        return when (request.type) {
            TargetType.UUID -> resolveByUUID(request)
            TargetType.NAME -> resolveByName(request)
            TargetType.TYPE -> resolveByType(request)
            TargetType.POSITION -> resolveByPosition(request)
            TargetType.HIERARCHY -> resolveByHierarchy(request)
            TargetType.CONTEXT -> resolveByContext(request)
            TargetType.RECENT -> resolveByRecent(request)
            TargetType.PROXIMITY -> resolveByProximity(request)
        }
    }
    
    /**
     * Resolve by direct UUID
     */
    private fun resolveByUUID(request: TargetRequest): TargetResult {
        val uuid = request.value ?: return TargetResult(emptyList(), 0f, "uuid-missing")
        val element = registry.findByUUID(uuid)
        
        return if (element != null && element.isEnabled) {
            TargetResult(listOf(element), 1.0f, "uuid-direct")
        } else {
            TargetResult(emptyList(), 0f, "uuid-not-found")
        }
    }
    
    /**
     * Resolve by element name/label
     */
    private fun resolveByName(request: TargetRequest): TargetResult {
        val name = request.value ?: return TargetResult(emptyList(), 0f, "name-missing")
        val elements = registry.findByName(name).filter { it.isEnabled }
        
        if (elements.isNotEmpty()) {
            val confidence = if (elements.size == 1) 1.0f else 0.8f
            return TargetResult(elements, confidence, "name-match")
        }
        
        // Fallback: partial name matching
        val partialMatches = registry.getEnabledElements().filter { element ->
            element.name?.contains(name, ignoreCase = true) == true ||
            element.description?.contains(name, ignoreCase = true) == true ||
            element.metadata?.label?.contains(name, ignoreCase = true) == true
        }
        
        return if (partialMatches.isNotEmpty()) {
            TargetResult(partialMatches, 0.6f, "name-partial", true)
        } else {
            TargetResult(emptyList(), 0f, "name-no-match")
        }
    }
    
    /**
     * Resolve by element type
     */
    private fun resolveByType(request: TargetRequest): TargetResult {
        val type = request.value ?: return TargetResult(emptyList(), 0f, "type-missing")
        val elements = registry.findByType(type).filter { it.isEnabled }
        
        return if (elements.isNotEmpty()) {
            TargetResult(elements, 0.9f, "type-match")
        } else {
            TargetResult(emptyList(), 0f, "type-no-match")
        }
    }
    
    /**
     * Resolve by position (first, second, third, last, etc.)
     */
    private fun resolveByPosition(request: TargetRequest): TargetResult {
        val position = request.position ?: return TargetResult(emptyList(), 0f, "position-missing")
        
        // Get all enabled elements sorted by position
        val sortedElements = registry.getEnabledElements()
            .filter { it.position != null }
            .sortedWith(compareBy({ it.position?.row }, { it.position?.column }, { it.position?.index }))
        
        val targetIndex = if (position == -1) sortedElements.size - 1 else position - 1
        val element = sortedElements.getOrNull(targetIndex)
        
        return if (element != null) {
            TargetResult(listOf(element), 1.0f, "position-match")
        } else {
            TargetResult(emptyList(), 0f, "position-out-of-bounds")
        }
    }
    
    /**
     * Resolve by hierarchy (parent, child, sibling)
     */
    private fun resolveByHierarchy(request: TargetRequest): TargetResult {
        val fromUUID = request.fromUUID ?: return TargetResult(emptyList(), 0f, "hierarchy-no-source")
        val sourceElement = registry.findByUUID(fromUUID) ?: return TargetResult(emptyList(), 0f, "hierarchy-source-not-found")
        
        return when (request.value) {
            "parent", "up" -> {
                val parent = registry.findParent(fromUUID)
                if (parent != null && parent.isEnabled) {
                    TargetResult(listOf(parent), 1.0f, "hierarchy-parent")
                } else {
                    TargetResult(emptyList(), 0f, "hierarchy-no-parent")
                }
            }
            "child", "down" -> {
                val children = registry.findChildren(fromUUID).filter { it.isEnabled }
                if (children.isNotEmpty()) {
                    TargetResult(children, 0.9f, "hierarchy-children")
                } else {
                    TargetResult(emptyList(), 0f, "hierarchy-no-children")
                }
            }
            else -> TargetResult(emptyList(), 0f, "hierarchy-unknown-relation")
        }
    }
    
    /**
     * Resolve by context (elements in same container/group)
     */
    private fun resolveByContext(request: TargetRequest): TargetResult {
        val context = request.value ?: return TargetResult(emptyList(), 0f, "context-missing")
        val fromUUID = request.fromUUID
        
        // Find elements with matching context
        val contextElements = registry.getEnabledElements().filter { element ->
            // Check if element or its parent matches context
            element.name?.contains(context, ignoreCase = true) == true ||
            element.metadata?.label?.contains(context, ignoreCase = true) == true ||
            registry.findParent(element.uuid)?.name?.contains(context, ignoreCase = true) == true
        }
        
        return if (contextElements.isNotEmpty()) {
            val confidence = if (fromUUID != null && contextElements.any { it.uuid == fromUUID }) 0.9f else 0.7f
            TargetResult(contextElements, confidence, "context-match")
        } else {
            TargetResult(emptyList(), 0f, "context-no-match")
        }
    }
    
    /**
     * Resolve by recent usage (not implemented yet - would need command history)
     */
    private fun resolveByRecent(request: TargetRequest): TargetResult {
        // TODO: Implement recent element tracking
        return TargetResult(emptyList(), 0f, "recent-not-implemented")
    }
    
    /**
     * Resolve by proximity to another element
     */
    private fun resolveByProximity(request: TargetRequest): TargetResult {
        val fromUUID = request.fromUUID ?: return TargetResult(emptyList(), 0f, "proximity-no-source")
        val sourceElement = registry.findByUUID(fromUUID) ?: return TargetResult(emptyList(), 0f, "proximity-source-not-found")
        val sourcePos = sourceElement.position ?: return TargetResult(emptyList(), 0f, "proximity-no-source-position")
        
        // Find nearest elements
        val nearbyElements = registry.getEnabledElements()
            .filter { it.uuid != fromUUID && it.position != null }
            .sortedBy { element ->
                val pos = element.position!!
                val dx = pos.x - sourcePos.x
                val dy = pos.y - sourcePos.y
                kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            }
            .take(5) // Return top 5 nearest
        
        return if (nearbyElements.isNotEmpty()) {
            TargetResult(nearbyElements, 0.8f, "proximity-match")
        } else {
            TargetResult(emptyList(), 0f, "proximity-no-nearby")
        }
    }
}