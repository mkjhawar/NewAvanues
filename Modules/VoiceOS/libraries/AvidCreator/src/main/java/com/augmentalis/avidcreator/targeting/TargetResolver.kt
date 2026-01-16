package com.augmentalis.avidcreator.targeting

import com.augmentalis.avidcreator.core.AvidRegistry
import com.augmentalis.avidcreator.models.*

/**
 * Resolves UI element targeting using various strategies
 * Extracted from UIKitVoiceCommandSystem for framework-agnostic use
 */
class TargetResolver(private val registry: AvidRegistry) {
    
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
        val elements: List<AvidElement>,
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
        val element = registry.findByVUID(uuid)
        
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
        // Validate source element exists (element itself not used, just UUID)
        registry.findByVUID(fromUUID) ?: return TargetResult(emptyList(), 0f, "hierarchy-source-not-found")
        
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
            registry.findParent(element.avid)?.name?.contains(context, ignoreCase = true) == true
        }
        
        return if (contextElements.isNotEmpty()) {
            val confidence = if (fromUUID != null && contextElements.any { it.avid == fromUUID }) 0.9f else 0.7f
            TargetResult(contextElements, confidence, "context-match")
        } else {
            TargetResult(emptyList(), 0f, "context-no-match")
        }
    }
    
    /**
     * Resolve by recent usage
     *
     * Returns elements sorted by most recent access timestamp. Supports filtering
     * by type (e.g., "recent button") and limiting results (e.g., "recent 5").
     *
     * Usage examples:
     * - "recent" → Returns top 10 recently accessed elements
     * - "recent button" → Returns recently accessed buttons
     * - "recent 5" → Returns 5 most recently accessed elements
     * - "recent 3 button" → Returns 3 most recently accessed buttons
     *
     * @param request Target request with optional value for filtering/limiting
     * @return TargetResult with recently accessed elements
     */
    private fun resolveByRecent(request: TargetRequest): TargetResult {
        // Parse the request value for type filters and limits
        var typeFilter: String? = null
        var limit = 10  // Default limit

        request.value?.let { value ->
            val parts = value.trim().split("\\s+".toRegex())

            // Parse parts: could be "button", "5", "3 button", "button 5", etc.
            for (part in parts) {
                val numValue = part.toIntOrNull()
                if (numValue != null) {
                    limit = numValue
                } else {
                    // Assume it's a type filter
                    typeFilter = part
                }
            }
        }

        // Get recently accessed elements from registry
        val recentElements = try {
            kotlinx.coroutines.runBlocking {
                registry.getRecentlyAccessedElements(limit = limit.coerceIn(1, 100))
            }
        } catch (e: Exception) {
            return TargetResult(emptyList(), 0f, "recent-error")
        }

        // Filter by type if specified
        val filteredElements = if (typeFilter != null) {
            recentElements.filter { element ->
                element.type.equals(typeFilter, ignoreCase = true)
            }
        } else {
            recentElements
        }

        // Only return enabled elements
        val enabledElements = filteredElements.filter { it.isEnabled }

        return if (enabledElements.isNotEmpty()) {
            val confidence = if (typeFilter != null) 0.85f else 0.9f
            val strategy = if (typeFilter != null) "recent-filtered" else "recent-match"
            TargetResult(enabledElements, confidence, strategy)
        } else {
            // Check if there were recent elements but none were enabled
            if (recentElements.isNotEmpty()) {
                TargetResult(emptyList(), 0f, "recent-none-enabled")
            } else {
                TargetResult(emptyList(), 0f, "recent-no-history")
            }
        }
    }
    
    /**
     * Resolve by proximity to another element
     */
    private fun resolveByProximity(request: TargetRequest): TargetResult {
        val fromUUID = request.fromUUID ?: return TargetResult(emptyList(), 0f, "proximity-no-source")
        val sourceElement = registry.findByVUID(fromUUID) ?: return TargetResult(emptyList(), 0f, "proximity-source-not-found")
        val sourcePos = sourceElement.position ?: return TargetResult(emptyList(), 0f, "proximity-no-source-position")
        
        // Find nearest elements
        val nearbyElements = registry.getEnabledElements()
            .filter { it.avid != fromUUID && it.position != null }
            .sortedBy { element ->
                val pos = element.position ?: return@sortedBy Float.MAX_VALUE
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