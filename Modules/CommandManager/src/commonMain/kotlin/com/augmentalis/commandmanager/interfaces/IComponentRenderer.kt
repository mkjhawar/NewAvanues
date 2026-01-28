/**
 * IComponentRenderer.kt - Interface for platform-specific component renderers
 *
 * Defines the contract for rendering YAML-defined components to native UI.
 * Platform implementations (Android, iOS, Desktop) provide concrete renderers.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * Architecture:
 * - IComponentRenderer: Main interface for full component rendering
 * - IWidgetRenderer: Interface for individual widget rendering
 * - RenderContext: Contextual data passed during rendering
 * - RenderResult: Output from rendering (platform-specific view reference)
 */
package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.OverlayTheme

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN RENDERER INTERFACE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-agnostic interface for rendering YAML components to native UI.
 *
 * Platform implementations should:
 * - Parse ComponentDefinition
 * - Create native view hierarchy
 * - Apply theme tokens
 * - Handle data bindings
 * - Support accessibility
 *
 * ## Usage
 *
 * ```kotlin
 * // Android implementation
 * class AndroidComponentRenderer(
 *     private val context: Context
 * ) : IComponentRenderer {
 *     override fun render(
 *         definition: ComponentDefinition,
 *         theme: OverlayTheme,
 *         context: RenderContext
 *     ): RenderResult {
 *         val rootView = createViewFromLayout(definition.layout)
 *         return RenderResult.success(rootView)
 *     }
 * }
 *
 * // Usage
 * val renderer = AndroidComponentRenderer(context)
 * val definition = ComponentFactory.parse(yamlString)
 * val result = renderer.render(definition, theme, renderContext)
 * ```
 */
interface IComponentRenderer {

    /**
     * Render a component definition to native UI.
     *
     * @param definition Parsed YAML component definition
     * @param theme Theme configuration for styling
     * @param context Render context with data bindings and callbacks
     * @return RenderResult containing native view or error
     */
    fun render(
        definition: ComponentDefinition,
        theme: OverlayTheme,
        context: RenderContext
    ): RenderResult

    /**
     * Update an existing rendered component with new data.
     *
     * More efficient than full re-render for data-only changes.
     *
     * @param result Previous render result
     * @param data Updated data bindings
     * @return Updated RenderResult or error
     */
    fun update(
        result: RenderResult,
        data: Map<String, Any>
    ): RenderResult

    /**
     * Dispose of rendered component and release resources.
     *
     * @param result Render result to dispose
     */
    fun dispose(result: RenderResult)

    /**
     * Check if this renderer supports the given component type.
     *
     * @param type Component type to check
     * @return true if this renderer can handle the type
     */
    fun supports(type: ComponentType): Boolean = true
}

// ═══════════════════════════════════════════════════════════════════════════════
// WIDGET RENDERER INTERFACE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Interface for rendering individual widgets.
 *
 * Allows extensible widget rendering by registering handlers
 * for different WidgetType values.
 *
 * ## Usage
 *
 * ```kotlin
 * class TextWidgetRenderer : IWidgetRenderer {
 *     override val widgetType = WidgetType.TEXT
 *
 *     override fun render(
 *         widget: WidgetDefinition,
 *         theme: OverlayTheme,
 *         context: RenderContext
 *     ): Any {
 *         val textView = TextView(context.platformContext as Context)
 *         textView.text = resolveBinding(widget.props.text, context)
 *         return textView
 *     }
 * }
 * ```
 */
interface IWidgetRenderer {

    /**
     * Widget type this renderer handles.
     */
    val widgetType: WidgetType

    /**
     * Render a widget definition to a platform-native view.
     *
     * @param widget Widget definition from YAML
     * @param theme Theme for styling
     * @param context Render context
     * @return Platform-specific view object (View on Android, UIView on iOS, etc.)
     */
    fun render(
        widget: WidgetDefinition,
        theme: OverlayTheme,
        context: RenderContext
    ): Any

    /**
     * Update an existing widget with new properties.
     *
     * @param view Previously rendered view
     * @param widget Updated widget definition
     * @param context Render context
     * @return Updated view
     */
    fun update(
        view: Any,
        widget: WidgetDefinition,
        context: RenderContext
    ): Any = view
}

// ═══════════════════════════════════════════════════════════════════════════════
// RENDER CONTEXT
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Context passed during component rendering.
 *
 * Contains data bindings, callbacks, and platform-specific context.
 */
data class RenderContext(
    /**
     * Data bindings for template expressions.
     *
     * Keys are binding names, values are the actual data.
     * Used to resolve expressions like `${items}`, `${commandState}`.
     */
    val data: Map<String, Any> = emptyMap(),

    /**
     * Callback functions for user interactions.
     *
     * Keys are event names (e.g., "onItemSelected", "onDismiss").
     */
    val callbacks: Map<String, (Any?) -> Unit> = emptyMap(),

    /**
     * Platform-specific context (e.g., Android Context, iOS UIWindow).
     *
     * Cast to appropriate type in platform implementations.
     */
    val platformContext: Any? = null,

    /**
     * Parent render context (for nested rendering).
     */
    val parent: RenderContext? = null,

    /**
     * Current mode (lite, dev).
     */
    val mode: String = "lite",

    /**
     * Screen dimensions for layout calculations.
     */
    val screenWidth: Float = 0f,
    val screenHeight: Float = 0f,

    /**
     * Density for dp-to-px conversions.
     */
    val density: Float = 1f,

    /**
     * Whether animations are enabled.
     */
    val animationsEnabled: Boolean = true,

    /**
     * Accessibility settings.
     */
    val accessibility: AccessibilitySettings = AccessibilitySettings()
) {
    /**
     * Get a typed value from data bindings.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getData(key: String): T? = data[key] as? T

    /**
     * Get a typed value with default fallback.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getData(key: String, default: T): T = (data[key] as? T) ?: default

    /**
     * Create child context with additional data.
     */
    fun withData(additionalData: Map<String, Any>): RenderContext {
        return copy(
            data = data + additionalData,
            parent = this
        )
    }

    /**
     * Create child context for template iteration.
     */
    fun forItem(itemName: String, item: Any, index: Int): RenderContext {
        return withData(mapOf(
            itemName to item,
            "index" to index
        ))
    }
}

/**
 * Accessibility settings for rendering.
 */
data class AccessibilitySettings(
    /** High contrast mode enabled */
    val highContrast: Boolean = false,

    /** Large text mode enabled */
    val largeText: Boolean = false,

    /** Reduced motion mode enabled */
    val reducedMotion: Boolean = false,

    /** Screen reader active */
    val screenReaderActive: Boolean = false,

    /** Font scale factor */
    val fontScale: Float = 1f
)

// ═══════════════════════════════════════════════════════════════════════════════
// RENDER RESULT
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Result of component rendering.
 *
 * Contains either the rendered view or an error.
 */
sealed class RenderResult {

    /**
     * Successful render result.
     *
     * @property view Platform-specific view object
     * @property viewMap Map of widget IDs to rendered views
     * @property definition Original component definition
     */
    data class Success(
        val view: Any,
        val viewMap: Map<String, Any> = emptyMap(),
        val definition: ComponentDefinition? = null
    ) : RenderResult() {

        /**
         * Get a view by its ID.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> getViewById(id: String): T? = viewMap[id] as? T
    }

    /**
     * Failed render result.
     *
     * @property error Error message
     * @property cause Underlying exception
     */
    data class Failure(
        val error: String,
        val cause: Throwable? = null
    ) : RenderResult()

    companion object {
        /**
         * Create successful result.
         */
        fun success(view: Any, viewMap: Map<String, Any> = emptyMap()): RenderResult {
            return Success(view, viewMap)
        }

        /**
         * Create failure result from error message.
         */
        fun failure(error: String): RenderResult {
            return Failure(error)
        }

        /**
         * Create failure result from exception.
         */
        fun failure(cause: Throwable): RenderResult {
            return Failure(cause.message ?: "Unknown error", cause)
        }
    }

    /**
     * Check if result is successful.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Check if result is failure.
     */
    val isFailure: Boolean get() = this is Failure

    /**
     * Get view if successful, null otherwise.
     */
    val viewOrNull: Any?
        get() = (this as? Success)?.view

    /**
     * Get error if failed, null otherwise.
     */
    val errorOrNull: String?
        get() = (this as? Failure)?.error

    /**
     * Execute action on success.
     */
    inline fun onSuccess(action: (Success) -> Unit): RenderResult {
        if (this is Success) action(this)
        return this
    }

    /**
     * Execute action on failure.
     */
    inline fun onFailure(action: (Failure) -> Unit): RenderResult {
        if (this is Failure) action(this)
        return this
    }

    /**
     * Map success to new result.
     */
    inline fun <T> map(transform: (Success) -> T): T? {
        return when (this) {
            is Success -> transform(this)
            is Failure -> null
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// WIDGET REGISTRY
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Registry for widget renderers.
 *
 * Platform implementations register their widget renderers here.
 */
class WidgetRendererRegistry {

    private val renderers = mutableMapOf<WidgetType, IWidgetRenderer>()

    /**
     * Register a widget renderer.
     */
    fun register(renderer: IWidgetRenderer) {
        renderers[renderer.widgetType] = renderer
    }

    /**
     * Register multiple renderers.
     */
    fun registerAll(vararg renderers: IWidgetRenderer) {
        renderers.forEach { register(it) }
    }

    /**
     * Get renderer for a widget type.
     */
    fun getRenderer(type: WidgetType): IWidgetRenderer? {
        return renderers[type]
    }

    /**
     * Check if renderer exists for type.
     */
    fun hasRenderer(type: WidgetType): Boolean {
        return renderers.containsKey(type)
    }

    /**
     * Get all registered widget types.
     */
    fun registeredTypes(): Set<WidgetType> {
        return renderers.keys.toSet()
    }

    /**
     * Clear all registered renderers.
     */
    fun clear() {
        renderers.clear()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EXPRESSION RESOLVER
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Interface for resolving binding expressions.
 *
 * Implementations parse and evaluate expressions like:
 * - `${items}` - Simple data binding
 * - `${items.length}` - Property access
 * - `${getBadgeColor(item)}` - Function call
 * - `${item.isEnabled ? 'enabled' : 'disabled'}` - Conditional
 */
interface IExpressionResolver {

    /**
     * Resolve an expression to its value.
     *
     * @param expression Expression string (with or without ${})
     * @param context Render context with data
     * @return Resolved value or null
     */
    fun resolve(expression: String, context: RenderContext): Any?

    /**
     * Resolve expression to string.
     */
    fun resolveString(expression: String, context: RenderContext): String {
        return resolve(expression, context)?.toString() ?: ""
    }

    /**
     * Resolve expression to number.
     */
    fun resolveNumber(expression: String, context: RenderContext): Number? {
        val value = resolve(expression, context)
        return when (value) {
            is Number -> value
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    /**
     * Resolve expression to boolean.
     */
    fun resolveBoolean(expression: String, context: RenderContext): Boolean {
        val value = resolve(expression, context)
        return when (value) {
            is Boolean -> value
            is String -> value.lowercase() in listOf("true", "1", "yes")
            is Number -> value.toDouble() != 0.0
            else -> value != null
        }
    }

    /**
     * Check if string contains binding expressions.
     */
    fun isExpression(value: String?): Boolean {
        return value?.contains("\${") == true
    }
}

/**
 * Basic expression resolver implementation.
 *
 * Handles simple variable bindings. Complex expressions
 * should be pre-computed and passed as data.
 */
class BasicExpressionResolver : IExpressionResolver {

    private val expressionPattern = Regex("""\$\{([^}]+)}""")

    override fun resolve(expression: String, context: RenderContext): Any? {
        // If not an expression, return as-is
        if (!isExpression(expression)) {
            return expression
        }

        // Extract expression content
        val match = expressionPattern.find(expression)
        if (match == null) {
            return expression
        }

        val expr = match.groupValues[1].trim()

        // Simple variable lookup
        if (!expr.contains('.') && !expr.contains('(')) {
            return context.data[expr]
        }

        // Property access (e.g., item.number)
        if (expr.contains('.') && !expr.contains('(')) {
            return resolvePropertyPath(expr, context)
        }

        // Function calls should be pre-resolved in data
        // Return the expression for debugging
        return context.data[expr] ?: "[\${$expr}]"
    }

    private fun resolvePropertyPath(path: String, context: RenderContext): Any? {
        val parts = path.split('.')
        var current: Any? = context.data

        for (part in parts) {
            current = when (current) {
                is Map<*, *> -> current[part]
                else -> null
            }
            if (current == null) break
        }

        return current
    }
}
