package com.augmentalis.magicelements.renderers.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.augmentalis.avaelements.core.Component
import com.augmentalis.magicelements.core.mel.*
import kotlinx.serialization.json.JsonElement

/**
 * Android Compose renderer for MEL-based plugins.
 *
 * Converts MEL UINode tree into Jetpack Compose components with reactive state.
 * Supports both Tier 1 (DATA) and Tier 2 (LOGIC) plugins on Android.
 *
 * ## Features
 * - Renders MEL UINode tree to Compose components
 * - Reactive state observation via StateFlow.collectAsState()
 * - Event wiring to PluginRuntime.dispatch()
 * - Material 3 component integration
 * - Support for both tiers on Android
 *
 * ## Usage Example
 *
 * ```kotlin
 * @Composable
 * fun CalculatorScreen() {
 *     val runtime = remember {
 *         loadMELPlugin("calculator.yaml")
 *     }
 *     MELPluginRenderer(runtime).Render()
 * }
 * ```
 *
 * @property runtime Plugin runtime with state and reducer engine
 * @property componentFactory Factory for creating Compose components from UINodes
 *
 * @since 2.0.0
 */
class MELPluginRenderer(
    private val runtime: PluginRuntime,
    private val componentFactory: MELComponentFactory = MELComponentFactory()
) {
    /**
     * Render the plugin UI as a Composable.
     *
     * Observes state changes and automatically re-renders affected components.
     */
    @Composable
    fun Render() {
        // Observe plugin state reactively
        val state by runtime.stateFlow.collectAsState()

        // Create binding resolver with current state
        val bindingResolver = remember(state) {
            val parser = ExpressionParser(emptyList()) // Will be used by resolver
            BindingResolver(state, parser)
        }

        // Get the root UI node from the plugin definition
        val rootNode = runtime.getMetadata().let {
            // Access UI definition from runtime
            // Note: This assumes we can access the UI definition
            // We'll need to expose this through the runtime
            getRootUINode()
        }

        // Render the UI tree
        RenderUINode(
            node = rootNode,
            resolver = bindingResolver,
            onEvent = { action, params ->
                runtime.dispatch(action, params)
            }
        )
    }

    /**
     * Render a single UINode and its children.
     *
     * @param node UI node to render
     * @param resolver Binding resolver for evaluating expressions
     * @param onEvent Event handler callback
     */
    @Composable
    private fun RenderUINode(
        node: UINode,
        resolver: BindingResolver,
        onEvent: (action: String, params: Map<String, Any>) -> Unit
    ) {
        // Resolve bindings to get final props
        val resolvedProps = remember(node, resolver) {
            resolver.resolve(node)
        }

        // Extract event handlers
        val eventHandlers = remember(node) {
            node.events.mapValues { (_, actionExpr) ->
                // Parse action expression (e.g., "increment" or "appendDigit('7')")
                { parseAndDispatchAction(actionExpr, onEvent) }
            }
        }

        // Create Compose component using factory
        componentFactory.CreateComponent(
            type = node.type,
            props = resolvedProps,
            eventHandlers = eventHandlers,
            children = {
                // Recursively render children
                node.children?.forEach { child ->
                    RenderUINode(child, resolver, onEvent)
                }
            }
        )
    }

    /**
     * Get the root UI node from the runtime.
     */
    private fun getRootUINode(): UINode {
        return runtime.getUIRoot()
    }

    /**
     * Parse action expression and dispatch to runtime.
     *
     * Handles:
     * - Simple actions: "increment"
     * - Parameterized actions: "appendDigit('7')"
     * - Multiple params: "setValues('a', 42)"
     *
     * @param actionExpr Action expression string
     * @param onEvent Event handler callback
     */
    private fun parseAndDispatchAction(
        actionExpr: String,
        onEvent: (action: String, params: Map<String, Any>) -> Unit
    ) {
        val trimmed = actionExpr.trim()

        // Check if this is a parameterized action
        if (trimmed.contains('(')) {
            // Parse action name and parameters
            val openParen = trimmed.indexOf('(')
            val closeParen = trimmed.lastIndexOf(')')

            if (openParen == -1 || closeParen == -1) {
                // Invalid format, dispatch as simple action
                onEvent(trimmed, emptyMap())
                return
            }

            val actionName = trimmed.substring(0, openParen).trim()
            val paramsStr = trimmed.substring(openParen + 1, closeParen).trim()

            // Parse parameters
            val params = if (paramsStr.isNotEmpty()) {
                parseActionParams(actionName, paramsStr)
            } else {
                emptyMap()
            }

            onEvent(actionName, params)
        } else {
            // Simple action without parameters
            onEvent(trimmed, emptyMap())
        }
    }

    /**
     * Parse action parameters from string and map to named params.
     *
     * Examples:
     * - "'7'" -> ["digit" to "7"] (maps to reducer param name)
     * - "7" -> ["digit" to 7]
     * - "'a', 42" -> maps to reducer param names
     */
    private fun parseActionParams(
        actionName: String,
        paramsStr: String
    ): Map<String, Any> {
        // Parse positional values
        val positionalArgs = paramsStr.split(',').map { part ->
            parseParameterValue(part.trim())
        }

        // Map to named params based on reducer definition
        return mapToNamedParams(actionName, positionalArgs)
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
     * Parse a single parameter value.
     *
     * Handles:
     * - Strings: "'hello'" or "\"world\""
     * - Numbers: 42, 3.14
     * - Booleans: true, false
     */
    private fun parseParameterValue(value: String): Any {
        return when {
            // String literals
            (value.startsWith('\'') && value.endsWith('\'')) ||
            (value.startsWith('"') && value.endsWith('"')) -> {
                value.substring(1, value.length - 1)
            }
            // Boolean literals
            value == "true" -> true
            value == "false" -> false
            // Number literals
            value.toIntOrNull() != null -> value.toInt()
            value.toDoubleOrNull() != null -> value.toDouble()
            // Default to string
            else -> value
        }
    }

    companion object {
        /**
         * Create a renderer from a plugin definition.
         *
         * @param definition Plugin definition
         * @param platform Target platform (defaults to Android)
         * @return Configured renderer
         */
        fun fromDefinition(
            definition: PluginDefinition,
            platform: Platform = Platform.ANDROID
        ): MELPluginRenderer {
            val runtime = PluginRuntime.create(definition, platform)
            return MELPluginRenderer(runtime)
        }

        /**
         * Create a renderer from YAML/JSON source.
         *
         * @param source Plugin source as YAML or JSON string
         * @param format Source format
         * @return Configured renderer
         */
        fun fromSource(
            source: String,
            format: PluginSourceFormat = PluginSourceFormat.JSON
        ): MELPluginRenderer {
            val runtime = PluginRuntime.fromSource(source, format, Platform.ANDROID)
            return MELPluginRenderer(runtime)
        }
    }
}

/**
 * Enhanced ReactiveRenderer for Android Compose integration.
 *
 * This extends the common ReactiveRenderer with Android-specific rendering logic.
 * It bridges between the platform-agnostic MEL UINode tree and Compose components.
 *
 * @property uiRoot Root UINode from plugin definition
 * @property state Current plugin state
 * @property reducerEngine Reducer engine for dispatch
 * @property tier Effective plugin tier
 * @property onDispatch Callback for event dispatch
 */
class AndroidReactiveRenderer(
    private val uiRoot: UINode,
    private val state: PluginState,
    private val reducerEngine: ReducerEngine,
    private val tier: PluginTier,
    private val onDispatch: (action: String, params: Map<String, Any>) -> Unit
) {
    private val componentFactory = MELComponentFactory()
    private var currentState: PluginState = state

    /**
     * Render the UI tree as a Composable.
     */
    @Composable
    fun render(): Component {
        // Create binding resolver
        val parser = ExpressionParser(emptyList())
        val resolver = BindingResolver(currentState, parser)

        // Render the UI tree
        return renderNode(uiRoot, resolver)
    }

    /**
     * Render a single UI node.
     */
    private fun renderNode(node: UINode, resolver: BindingResolver): Component {
        // Resolve bindings
        val resolvedProps = resolver.resolve(node)

        // Recursively render children
        val childComponents = node.children?.map { renderNode(it, resolver) }

        // Create component (this returns a Component, not a Composable)
        // The actual Compose rendering is handled by ComposeRenderer
        return createComponentFromNode(node, resolvedProps, childComponents)
    }

    /**
     * Create an AvaElements Component from a UINode.
     *
     * This creates the data structure; actual rendering happens in ComposeRenderer.
     */
    private fun createComponentFromNode(
        node: UINode,
        props: Map<String, JsonElement>,
        children: List<Component>?
    ): Component {
        // Use the factory to create the component
        // This will need to map MEL types to AvaElements components
        return componentFactory.createComponent(node.type, props, children, node.id)
    }

    /**
     * Handle state change notification.
     */
    fun onStateChanged(newState: PluginState) {
        currentState = newState
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        // Cleanup if needed
    }
}
