package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkType

/**
 * Base interface for framework-specific element handlers.
 *
 * Each framework (Flutter, Unity, React Native, etc.) has unique
 * characteristics that require specialized handling for element
 * scraping and command generation.
 */
interface FrameworkHandler {

    /**
     * The framework type this handler supports.
     */
    val frameworkType: FrameworkType

    /**
     * Check if this handler can process the given elements.
     */
    fun canHandle(elements: List<ElementInfo>): Boolean

    /**
     * Process elements and extract actionable items.
     */
    fun processElements(elements: List<ElementInfo>): List<ElementInfo>

    /**
     * Get framework-specific element selectors.
     */
    fun getSelectors(): List<String>

    /**
     * Check if an element is actionable in this framework.
     */
    fun isActionable(element: ElementInfo): Boolean

    /**
     * Get the priority for this handler (higher = checked first).
     */
    fun getPriority(): Int
}

/**
 * Result of framework handling operation.
 */
data class FrameworkHandlingResult(
    val frameworkType: FrameworkType,
    val processedElements: List<ElementInfo>,
    val actionableCount: Int,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Registry for framework handlers.
 */
object FrameworkHandlerRegistry {

    private val handlers = mutableListOf<FrameworkHandler>()

    /**
     * Register a framework handler.
     */
    fun register(handler: FrameworkHandler) {
        handlers.add(handler)
        handlers.sortByDescending { it.getPriority() }
    }

    /**
     * Unregister a framework handler.
     */
    fun unregister(handler: FrameworkHandler) {
        handlers.remove(handler)
    }

    /**
     * Get all registered handlers.
     */
    fun getHandlers(): List<FrameworkHandler> = handlers.toList()

    /**
     * Find the appropriate handler for given elements.
     */
    fun findHandler(elements: List<ElementInfo>): FrameworkHandler? {
        return handlers.firstOrNull { it.canHandle(elements) }
    }

    /**
     * Get handler for specific framework type.
     */
    fun getHandler(type: FrameworkType): FrameworkHandler? {
        return handlers.find { it.frameworkType == type }
    }

    /**
     * Clear all registered handlers (for testing).
     */
    fun clear() {
        handlers.clear()
    }

    /**
     * Register default handlers.
     */
    fun registerDefaults() {
        clear()
        register(FlutterHandler())
        register(UnityHandler())
        register(ReactNativeHandler())
        register(WebViewHandler())
        register(NativeHandler())
    }
}
