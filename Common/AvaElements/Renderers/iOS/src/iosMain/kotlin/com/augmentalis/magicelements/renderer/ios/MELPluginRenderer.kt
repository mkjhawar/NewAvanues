package com.augmentalis.magicelements.renderer.ios

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.magicelements.core.mel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * iOS MEL Plugin Renderer
 *
 * Renders MEL-based plugins to SwiftUI components on iOS. Integrates with the
 * PluginRuntime to provide reactive UI updates and event dispatching.
 *
 * ## Features
 * - Tier 1 enforcement (Apple App Store compliance)
 * - Reactive state updates
 * - Event dispatching to plugin reducers
 * - Component tree caching for performance
 * - SwiftUI bridge integration
 *
 * ## Usage
 *
 * ```kotlin
 * // Load plugin
 * val loader = PluginLoader()
 * val runtime = loader.loadMELPlugin(PluginSource.File("calculator.yaml"))
 *
 * // Create renderer
 * val renderer = MELPluginRenderer(runtime)
 *
 * // Render to SwiftUI
 * val swiftUIView = renderer.render()
 *
 * // Handle user interaction
 * renderer.handleEvent("onTap", "increment")
 * ```
 *
 * @property runtime Plugin runtime instance
 *
 * @since 2.0.0
 */
class MELPluginRenderer(
    private val runtime: PluginRuntime
) {
    /**
     * Component factory for creating SwiftUI components
     */
    private val componentFactory = iOSMELComponentFactory()

    /**
     * Expression parser for evaluating bindings
     */
    private val expressionParser = ExpressionParser()

    /**
     * Reactive renderer for UI updates
     */
    private val reactiveRenderer: ReactiveRenderer

    /**
     * Coroutine scope for state observation
     */
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Cached SwiftUI view tree
     */
    private var cachedView: SwiftUIView? = null

    /**
     * Event listeners for UI updates
     */
    private val updateListeners = mutableListOf<(SwiftUIView) -> Unit>()

    init {
        // Create reactive renderer with state and component factory
        val bindingResolver = BindingResolver(runtime.state, expressionParser)
        reactiveRenderer = ReactiveRenderer(
            state = runtime.state,
            resolver = bindingResolver,
            componentFactory = componentFactory
        )

        // Subscribe to state changes
        scope.launch {
            runtime.stateFlow.collect { newState ->
                // State changed - trigger re-render
                onStateChanged()
            }
        }

        // Subscribe to reactive renderer updates
        reactiveRenderer.subscribe { changedPaths ->
            cachedView?.let { view ->
                updateListeners.forEach { listener ->
                    listener(view)
                }
            }
        }
    }

    /**
     * Render the plugin UI to SwiftUI
     *
     * @return SwiftUIView bridge model
     */
    fun render(): SwiftUIView {
        // Get UI root from plugin definition
        val uiRoot = runtime.getMetadata().let {
            // The runtime holds the definition which has the UI root
            // We need to access it via the runtime
            getUIRootFromRuntime()
        }

        // Render through reactive renderer
        val component = reactiveRenderer.render(uiRoot)

        // Convert to SwiftUIView
        val swiftUIView = component as? SwiftUIView
            ?: SwiftUIView(
                type = ViewType.Text,
                properties = mapOf("text" to "Error: Invalid component type")
            )

        // Attach event handlers
        val viewWithEvents = attachEventHandlers(swiftUIView, uiRoot)

        // Cache the result
        cachedView = viewWithEvents

        return viewWithEvents
    }

    /**
     * Handle an event triggered from the UI
     *
     * @param eventName Event name (e.g., "onTap", "onChange")
     * @param actionExpr Action expression (e.g., "increment", "setValue(42)")
     */
    fun handleEvent(eventName: String, actionExpr: String) {
        // Parse action expression
        val (reducerName, params) = parseActionExpression(actionExpr)

        // Dispatch to runtime
        runtime.dispatch(reducerName, params)
    }

    /**
     * Subscribe to UI update events
     *
     * @param listener Callback receiving updated SwiftUIView
     */
    fun onUpdate(listener: (SwiftUIView) -> Unit) {
        updateListeners.add(listener)
    }

    /**
     * Unsubscribe from UI updates
     */
    fun removeUpdateListener(listener: (SwiftUIView) -> Unit) {
        updateListeners.remove(listener)
    }

    /**
     * Get current cached view
     */
    fun getCachedView(): SwiftUIView? = cachedView

    /**
     * Destroy the renderer and clean up resources
     */
    fun destroy() {
        reactiveRenderer.clear()
        updateListeners.clear()
        cachedView = null
        runtime.destroy()
    }

    // ========== Private Helper Methods ==========

    /**
     * Get UI root from plugin runtime
     */
    private fun getUIRootFromRuntime(): UINode {
        return runtime.getUIRoot()
    }

    /**
     * Attach event handlers to SwiftUI view tree
     */
    private fun attachEventHandlers(view: SwiftUIView, uiNode: UINode): SwiftUIView {
        // Check if node has events
        if (uiNode.events.isEmpty() && (uiNode.children == null || uiNode.children.isEmpty())) {
            return view
        }

        // Create onTapGesture modifier for events
        val eventModifiers = uiNode.events.map { (eventName, actionExpr) ->
            when (eventName) {
                "onTap", "onClick" -> {
                    // Create action that will be handled by the renderer
                    SwiftUIModifier(
                        type = ModifierType.OnTapGesture,
                        value = actionExpr
                    )
                }
                else -> null
            }
        }.filterNotNull()

        // Recursively attach to children
        val updatedChildren = view.children.zip(uiNode.children ?: emptyList()).map { (childView, childNode) ->
            attachEventHandlers(childView, childNode)
        }

        return view.copy(
            modifiers = view.modifiers + eventModifiers,
            children = updatedChildren
        )
    }

    /**
     * Parse action expression into reducer name and parameters
     *
     * Examples:
     * - "increment" -> ("increment", emptyMap())
     * - "appendDigit(7)" -> ("appendDigit", mapOf("digit" to 7))
     * - "setOperator('+')" -> ("setOperator", mapOf("op" to "+"))
     */
    private fun parseActionExpression(expr: String): Pair<String, Map<String, Any>> {
        val trimmed = expr.trim()

        // Check for function call with params
        val callMatch = Regex("""(\w+)\((.*)\)""").find(trimmed)

        return if (callMatch != null) {
            val name = callMatch.groupValues[1]
            val argsStr = callMatch.groupValues[2].trim()

            if (argsStr.isEmpty()) {
                name to emptyMap()
            } else {
                // Parse positional arguments
                val positionalArgs = argsStr.split(",").map { arg ->
                    val argTrimmed = arg.trim()
                    when {
                        argTrimmed.startsWith("'") || argTrimmed.startsWith("\"") -> {
                            // String
                            argTrimmed.removeSurrounding("'").removeSurrounding("\"")
                        }
                        argTrimmed == "true" || argTrimmed == "false" -> {
                            // Boolean
                            argTrimmed.toBoolean()
                        }
                        argTrimmed.toIntOrNull() != null -> {
                            // Integer
                            argTrimmed.toInt()
                        }
                        argTrimmed.toDoubleOrNull() != null -> {
                            // Double
                            argTrimmed.toDouble()
                        }
                        else -> argTrimmed
                    }
                }

                // Map to named params based on reducer definition
                val namedParams = mapToNamedParams(name, positionalArgs)
                name to namedParams
            }
        } else {
            // Simple reducer name without params
            trimmed to emptyMap()
        }
    }

    /**
     * Map positional arguments to named parameters based on reducer definition
     */
    private fun mapToNamedParams(
        reducerName: String,
        positionalArgs: List<Any>
    ): Map<String, Any> {
        // Get the reducer definition from runtime
        val reducer = runtime.getReducer(reducerName)

        if (reducer == null) {
            // Reducer not found - use positional params as fallback
            return positionalArgs.mapIndexed { index, value ->
                "arg$index" to value
            }.toMap()
        }

        // Map positional args to named params based on reducer.params
        val namedParams = mutableMapOf<String, Any>()
        reducer.params.forEachIndexed { index, paramName ->
            if (index < positionalArgs.size) {
                namedParams[paramName] = positionalArgs[index]
            }
        }

        return namedParams
    }

    /**
     * Handle state change notification
     */
    private fun onStateChanged() {
        // Re-render on state change
        val newView = render()
        cachedView = newView

        // Notify listeners
        updateListeners.forEach { listener ->
            listener(newView)
        }
    }

    companion object {
        /**
         * Create a renderer from a plugin runtime
         */
        fun create(runtime: PluginRuntime): MELPluginRenderer {
            return MELPluginRenderer(runtime)
        }
    }
}

/**
 * Extension function to render a PluginRuntime to SwiftUI
 */
fun PluginRuntime.toSwiftUI(): SwiftUIView {
    val renderer = MELPluginRenderer(this)
    return renderer.render()
}
