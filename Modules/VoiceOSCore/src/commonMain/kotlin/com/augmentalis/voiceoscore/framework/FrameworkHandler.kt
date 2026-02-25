package com.augmentalis.voiceoscore

import kotlin.concurrent.Volatile
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.FrameworkType
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

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
     * Whether this handler is a fallback (always returns true from canHandle).
     * Default is false. Only fallback handlers should override this to true.
     */
    val isFallbackHandler: Boolean get() = false

    /**
     * Check if this handler can process the given elements.
     * Fallback handlers should NOT rely on always returning true -
     * they should check actual framework markers.
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
 * Configurable registry for framework handlers.
 * Handlers can be provided at construction time or registered dynamically.
 */
class FrameworkHandlerRegistryImpl(
    handlers: List<FrameworkHandler> = emptyList()
) {
    private val handlers = handlers.toMutableList()

    init {
        if (handlers.isNotEmpty()) {
            this.handlers.sortByDescending { it.getPriority() }
        }
    }

    companion object {
        /**
         * Create registry with default handlers.
         */
        fun withDefaults(): FrameworkHandlerRegistryImpl {
            return FrameworkHandlerRegistryImpl(defaultHandlers())
        }

        /**
         * Get default handler list.
         */
        fun defaultHandlers(): List<FrameworkHandler> = listOf(
            FlutterHandler(),      // Priority 100
            ComposeHandler(),      // Priority 90
            ReactNativeHandler(),  // Priority 80
            WebViewHandler(),      // Priority 70
            UnityHandler(),        // Priority 60
            NativeHandler()        // Priority 0 (fallback)
        )
    }

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
     * Prioritizes non-fallback handlers, then falls back to fallback handlers.
     */
    fun findHandler(elements: List<ElementInfo>): FrameworkHandler? {
        // First try non-fallback handlers
        val specialized = handlers.filter { !it.isFallbackHandler }
            .firstOrNull { it.canHandle(elements) }
        if (specialized != null) return specialized

        // Then try fallback handlers
        return handlers.filter { it.isFallbackHandler }
            .firstOrNull { it.canHandle(elements) }
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
        defaultHandlers().forEach { register(it) }
    }
}

/**
 * Global registry instance (for backward compatibility).
 */
object FrameworkHandlerRegistryGlobal {
    @kotlin.concurrent.Volatile
    private var instance: FrameworkHandlerRegistryImpl? = null
    private val lock = SynchronizedObject()

    fun getInstance(): FrameworkHandlerRegistryImpl {
        return instance ?: synchronized(lock) {
            instance ?: FrameworkHandlerRegistryImpl.withDefaults().also {
                instance = it
            }
        }
    }

    fun register(handler: FrameworkHandler) = getInstance().register(handler)
    fun unregister(handler: FrameworkHandler) = getInstance().unregister(handler)
    fun getHandlers() = getInstance().getHandlers()
    fun findHandler(elements: List<ElementInfo>) = getInstance().findHandler(elements)
    fun getHandler(type: FrameworkType) = getInstance().getHandler(type)
    fun clear() = getInstance().clear()
    fun registerDefaults() {
        instance = FrameworkHandlerRegistryImpl.withDefaults()
    }
}

/**
 * Registry for framework handlers (backward compatible object).
 * Delegates to FrameworkHandlerRegistryGlobal for singleton behavior.
 */
object FrameworkHandlerRegistry {
    fun register(handler: FrameworkHandler) = FrameworkHandlerRegistryGlobal.register(handler)
    fun unregister(handler: FrameworkHandler) = FrameworkHandlerRegistryGlobal.unregister(handler)
    fun getHandlers() = FrameworkHandlerRegistryGlobal.getHandlers()
    fun findHandler(elements: List<ElementInfo>) = FrameworkHandlerRegistryGlobal.findHandler(elements)
    fun getHandler(type: FrameworkType) = FrameworkHandlerRegistryGlobal.getHandler(type)
    fun clear() = FrameworkHandlerRegistryGlobal.clear()
    fun registerDefaults() = FrameworkHandlerRegistryGlobal.registerDefaults()
}
