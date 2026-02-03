package com.augmentalis.magicelements.renderer.desktop

import androidx.compose.runtime.*
import com.augmentalis.magicelements.core.mel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Desktop MEL Plugin Renderer
 *
 * Renders MEL-based plugins to Compose Desktop components. Integrates with the
 * PluginRuntime to provide reactive UI updates and event dispatching.
 *
 * ## Platform Support
 * - macOS: Tier 1 only (Apple App Store compliance)
 * - Windows: Full Tier 2 support
 * - Linux: Full Tier 2 support
 *
 * ## Features
 * - Platform-aware tier enforcement
 * - Reactive state updates via Compose state management
 * - Event dispatching to plugin reducers
 * - Component tree caching for performance
 * - Desktop-optimized layouts (keyboard, mouse, larger screens)
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
 * // Render in Compose Desktop
 * @Composable
 * fun MyApp() {
 *     renderer.Render()
 * }
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
     * Component factory for creating Compose Desktop components
     */
    private val componentFactory = DesktopMELComponentFactory()

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
     * State flow for triggering recomposition
     */
    private val _renderTrigger = MutableStateFlow(0)
    val renderTrigger: StateFlow<Int> = _renderTrigger

    /**
     * Cached UI root
     */
    private var cachedUIRoot: UINode? = null

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
            // Trigger Compose recomposition
            _renderTrigger.value++
        }
    }

    /**
     * Render the plugin UI using Compose
     *
     * This is a Composable function that renders the plugin UI tree.
     * It automatically recomposes when the plugin state changes.
     */
    @Composable
    fun Render() {
        // Observe render trigger to force recomposition
        val trigger by renderTrigger.collectAsState()

        // Get or cache UI root
        val uiRoot = cachedUIRoot ?: getUIRootFromRuntime().also { cachedUIRoot = it }

        // Render the UI tree
        RenderUINode(uiRoot)
    }

    /**
     * Render a single UI node
     */
    @Composable
    private fun RenderUINode(node: UINode) {
        // Render through reactive renderer
        val component = reactiveRenderer.render(node)

        // If component is Composable, invoke it
        if (component is ComposableComponent) {
            component.content()
        }
    }

    /**
     * Handle an event triggered from the UI
     *
     * @param eventName Event name (e.g., "onTap", "onClick", "onChange")
     * @param actionExpr Action expression (e.g., "increment", "setValue(42)")
     */
    fun handleEvent(eventName: String, actionExpr: String) {
        // Parse action expression
        val (reducerName, params) = parseActionExpression(actionExpr)

        // Dispatch to runtime
        runtime.dispatch(reducerName, params)
    }

    /**
     * Destroy the renderer and clean up resources
     */
    fun destroy() {
        reactiveRenderer.clear()
        cachedUIRoot = null
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
                // Parse arguments
                val args = parseArguments(argsStr)
                name to args
            }
        } else {
            // Simple reducer name without params
            trimmed to emptyMap()
        }
    }

    /**
     * Parse function arguments
     *
     * Supports: numbers, strings, booleans
     */
    private fun parseArguments(argsStr: String): Map<String, Any> {
        val args = argsStr.split(",").mapIndexed { index, arg ->
            val trimmed = arg.trim()
            val value: Any = when {
                trimmed.startsWith("'") || trimmed.startsWith("\"") -> {
                    // String
                    trimmed.removeSurrounding("'").removeSurrounding("\"")
                }
                trimmed == "true" || trimmed == "false" -> {
                    // Boolean
                    trimmed.toBoolean()
                }
                trimmed.toIntOrNull() != null -> {
                    // Integer
                    trimmed.toInt()
                }
                trimmed.toDoubleOrNull() != null -> {
                    // Double
                    trimmed.toDouble()
                }
                else -> trimmed
            }

            // Use positional params (param0, param1, etc.)
            "param$index" to value
        }.toMap()

        return args
    }

    /**
     * Handle state change notification
     */
    private fun onStateChanged() {
        // Trigger recomposition
        _renderTrigger.value++
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
 * Extension function to render a PluginRuntime in Compose Desktop
 */
@Composable
fun PluginRuntime.RenderCompose() {
    val renderer = remember { MELPluginRenderer(this) }
    DisposableEffect(Unit) {
        onDispose {
            renderer.destroy()
        }
    }
    renderer.Render()
}
