package com.augmentalis.magicelements.core.mel

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.Renderer
import com.augmentalis.magicelements.core.mel.functions.PluginTier
import kotlinx.serialization.json.*

/**
 * Reactive renderer that efficiently re-renders UI on state changes
 *
 * Tracks which UI nodes depend on which state paths and triggers minimal
 * re-renders when state changes occur. Converts UINode trees into actual
 * AvaElements Component instances.
 *
 * ## Features
 * - Dependency tracking: Maps state paths to dependent nodes
 * - Minimal re-renders: Only updates affected nodes
 * - Component conversion: UINode -> AvaElements Components
 * - Observable state: Subscribes to state change notifications
 *
 * ## Usage Example
 *
 * ```kotlin
 * val state = PluginState(...)
 * val evaluator = ExpressionEvaluator(...)
 * val resolver = BindingResolver(state, evaluator)
 * val renderer = ReactiveRenderer(
 *     uiRoot = uiNode,
 *     state = state,
 *     reducerEngine = reducerEngine,
 *     tier = tier,
 *     resolver = resolver,
 *     componentFactory = componentFactory,
 *     onDispatch = { action, params -> dispatch(action, params) }
 * )
 *
 * // Initial render
 * val component = renderer.render()
 *
 * // Subscribe to state changes
 * renderer.subscribe { changedPaths ->
 *     // Re-render affected nodes automatically
 * }
 * ```
 *
 * @property uiRoot Root UI node to render
 * @property state Plugin state to observe
 * @property reducerEngine Reducer engine for dispatching actions
 * @property tier Plugin tier
 * @property resolver Binding resolver for evaluating expressions
 * @property componentFactory Factory for creating AvaElements components
 * @property onDispatch Callback for dispatching actions
 *
 * @since 2.0.0
 */
class ReactiveRenderer(
    private val uiRoot: UINode,
    private var state: PluginState,
    private val reducerEngine: ReducerEngine,
    private val tier: PluginTier,
    private val resolver: BindingResolver,
    private val componentFactory: ComponentFactory,
    private val onDispatch: (String, Map<String, Any>) -> Unit
) {
    /**
     * Event binder for wiring UI events to reducers
     */
    private val eventBinder = EventBinder(reducerEngine)

    /**
     * Dependency map: state path -> list of node IDs that depend on it
     */
    private val dependencies = mutableMapOf<String, MutableSet<String>>()

    /**
     * Reverse map: node ID -> set of state paths it depends on
     */
    private val nodeDependencies = mutableMapOf<String, MutableSet<String>>()

    /**
     * Cached component tree
     */
    private var cachedTree: Component? = null

    /**
     * Cached individual node components for selective updates
     */
    private val nodeCache = mutableMapOf<String, Component>()

    /**
     * Map of node IDs to their UINode definitions
     */
    private val nodeMap = mutableMapOf<String, UINode>()

    /**
     * Current UI node tree
     */
    private var currentUINode: UINode? = null

    /**
     * Change listeners
     */
    private val listeners = mutableListOf<(Set<String>) -> Unit>()

    /**
     * StateObserver for efficient change tracking
     */
    private val stateObserver = StateObserver()

    /**
     * Render the UI node tree to AvaElements components
     *
     * @return Root AvaElements component
     */
    fun render(): Component {
        currentUINode = uiRoot

        // Clear caches for fresh render
        nodeCache.clear()
        nodeMap.clear()
        dependencies.clear()
        nodeDependencies.clear()

        // Build dependency map and populate node map
        buildDependencies(uiRoot)

        // Resolve bindings and convert to components
        val component = renderNode(uiRoot)

        // Cache the result
        cachedTree = component

        return component
    }

    /**
     * Re-render the entire tree
     *
     * Called when state changes affect multiple nodes.
     */
    fun rerender() {
        currentUINode?.let { node ->
            cachedTree = renderNode(node)
            notifyListeners(emptySet()) // Signal full re-render
        }
    }

    /**
     * Re-render only affected nodes based on changed state paths
     *
     * @param changedPaths Set of state paths that changed
     */
    fun rerenderAffected(changedPaths: Set<String>) {
        if (changedPaths.isEmpty()) {
            rerender()
            return
        }

        // Normalize paths (remove "state." prefix if present for matching)
        val normalizedPaths = changedPaths.map { path ->
            if (path.startsWith("state.")) path else "state.$path"
        }.toSet()

        // Find affected node IDs using both exact match and prefix match
        val affectedNodeIds = mutableSetOf<String>()
        normalizedPaths.forEach { path ->
            // Exact match
            dependencies[path]?.let { nodeIds ->
                affectedNodeIds.addAll(nodeIds)
            }
            // Also check for parent path matches (e.g., "state.items" affects "state.items.0.text")
            dependencies.forEach { (depPath, nodeIds) ->
                if (depPath.startsWith("$path.") || path.startsWith("$depPath.")) {
                    affectedNodeIds.addAll(nodeIds)
                }
            }
        }

        if (affectedNodeIds.isEmpty()) {
            // No matching dependencies found, do full re-render as fallback
            rerender()
            notifyListeners(changedPaths)
            return
        }

        // Selective re-render: only update affected nodes
        var needsFullRerender = false
        affectedNodeIds.forEach { nodeId ->
            val node = nodeMap[nodeId]
            if (node != null) {
                try {
                    val updatedComponent = renderNode(node, nodeId)
                    nodeCache[nodeId] = updatedComponent
                } catch (e: Exception) {
                    // If selective update fails, mark for full re-render
                    needsFullRerender = true
                }
            } else {
                // Node not in map, need full re-render
                needsFullRerender = true
            }
        }

        if (needsFullRerender) {
            rerender()
        }

        // Notify listeners
        notifyListeners(changedPaths)
    }

    /**
     * Called when state changes. Updates internal state and triggers re-render.
     */
    fun onStateChanged(newState: PluginState, changedPaths: Set<String> = emptySet()) {
        state = newState
        if (changedPaths.isEmpty()) {
            rerender()
        } else {
            rerenderAffected(changedPaths)
        }
    }

    /**
     * Subscribe to render updates
     *
     * @param listener Callback receiving set of changed state paths
     */
    fun subscribe(listener: (Set<String>) -> Unit) {
        listeners.add(listener)
    }

    /**
     * Unsubscribe a listener
     */
    fun unsubscribe(listener: (Set<String>) -> Unit) {
        listeners.remove(listener)
    }

    /**
     * Build dependency map for efficient re-rendering
     * Also populates nodeMap and nodeDependencies for selective updates
     */
    private fun buildDependencies(node: UINode, nodeId: String = node.id ?: generateNodeId(node)) {
        // Store node in map for later lookup
        nodeMap[nodeId] = node

        // Extract state paths from bindings
        val paths = node.getReferencedStatePaths()

        // Build forward dependencies (path -> nodes)
        paths.forEach { path ->
            dependencies.getOrPut(path) { mutableSetOf() }.add(nodeId)
        }

        // Build reverse dependencies (node -> paths)
        if (paths.isNotEmpty()) {
            nodeDependencies[nodeId] = paths.toMutableSet()
        }

        // Recursively build for children
        node.children?.forEachIndexed { index, child ->
            val childId = child.id ?: "$nodeId.child.$index"
            buildDependencies(child, childId)
        }
    }

    /**
     * Render a single UI node to an AvaElements component
     *
     * @param node The UINode to render
     * @param nodeId Optional explicit node ID (used during selective re-render)
     */
    private fun renderNode(node: UINode, nodeId: String? = null): Component {
        val effectiveNodeId = nodeId ?: node.id ?: generateNodeId(node)

        // Resolve bindings
        val resolvedProps = resolver.resolve(node)

        // Wire event handlers from EventBinder
        val eventHandlers = eventBinder.bind(node)

        // Recursively render children
        val childComponents = node.children?.mapIndexed { index, child ->
            val childId = child.id ?: "$effectiveNodeId.child.$index"
            renderNode(child, childId)
        }

        // Create component using factory
        val component = componentFactory.create(
            type = node.type,
            props = resolvedProps,
            callbacks = eventHandlers,
            children = childComponents,
            id = node.id
        )

        // Cache the rendered component
        nodeCache[effectiveNodeId] = component

        return component
    }

    /**
     * Notify all listeners of changes
     */
    private fun notifyListeners(changedPaths: Set<String>) {
        listeners.forEach { listener ->
            listener(changedPaths)
        }
    }

    /**
     * Generate a unique node ID
     */
    private fun generateNodeId(node: UINode): String {
        return "${node.type}_${node.hashCode()}"
    }

    /**
     * Get the current cached component tree
     */
    fun getCachedTree(): Component? = cachedTree

    /**
     * Clear all dependencies and cache
     */
    fun clear() {
        dependencies.clear()
        nodeDependencies.clear()
        nodeCache.clear()
        nodeMap.clear()
        cachedTree = null
        currentUINode = null
        listeners.clear()
        stateObserver.clear()
    }

    /**
     * Destroy the renderer and clean up resources
     */
    fun destroy() {
        clear()
    }

    /**
     * Get the StateObserver for external subscriptions
     * Allows PluginRuntime to wire state change notifications
     */
    fun getStateObserver(): StateObserver = stateObserver

    /**
     * Subscribe to state changes via StateObserver
     * Returns observer ID for unsubscription
     */
    fun observeState(
        paths: List<String>? = null,
        callback: (StateChangeBatch) -> Unit
    ): String {
        return stateObserver.subscribe(paths, callback)
    }

    /**
     * Unsubscribe from state changes
     */
    fun stopObserving(observerId: String): Boolean {
        return stateObserver.unsubscribe(observerId)
    }

    /**
     * Get the number of nodes being tracked
     */
    fun getTrackedNodeCount(): Int = nodeMap.size

    /**
     * Get the number of state paths being watched
     */
    fun getWatchedPathCount(): Int = dependencies.size

    /**
     * Get cached component for a specific node ID
     */
    fun getCachedNode(nodeId: String): Component? = nodeCache[nodeId]

    /**
     * Get all node IDs that depend on a specific state path
     */
    fun getDependentNodes(statePath: String): Set<String> {
        val normalizedPath = if (statePath.startsWith("state.")) statePath else "state.$statePath"
        return dependencies[normalizedPath]?.toSet() ?: emptySet()
    }

    companion object {
        /**
         * Create a ReactiveRenderer with default configuration
         */
        fun create(
            state: PluginState,
            parser: ExpressionParser,
            componentFactory: ComponentFactory
        ): ReactiveRenderer {
            val resolver = BindingResolver(state, parser)
            return ReactiveRenderer(state, resolver, componentFactory)
        }
    }
}

/**
 * Component factory for creating AvaElements components from UINodes
 *
 * Converts generic UINode definitions into specific AvaElements component instances.
 * Handles type mapping and prop conversion.
 *
 * @since 2.0.0
 */
interface ComponentFactory {
    /**
     * Create a component from a UINode definition
     *
     * @param type Component type name (e.g., "Text", "Button", "Column")
     * @param props Resolved props as JSON elements
     * @param callbacks Event handler callbacks
     * @param children Child components (if any)
     * @param id Optional component ID
     * @return AvaElements component instance
     */
    fun create(
        type: String,
        props: Map<String, JsonElement>,
        callbacks: Map<String, () -> Unit> = emptyMap(),
        children: List<Component>?,
        id: String?
    ): Component

    /**
     * Check if a component type is supported
     */
    fun supports(type: String): Boolean
}

/**
 * Default component factory implementation
 *
 * Maps common component types to AvaElements components.
 * Supports: Text, Button, Column, Row, Container, Divider
 *
 * @since 2.0.0
 */
class DefaultComponentFactory : ComponentFactory {
    override fun create(
        type: String,
        props: Map<String, JsonElement>,
        callbacks: Map<String, () -> Unit>,
        children: List<Component>?,
        id: String?
    ): Component {
        return when (type) {
            "Text" -> createText(props, id)
            "Button" -> createButton(props, callbacks, id)
            "Column" -> createColumn(props, children, id)
            "Row" -> createRow(props, children, id)
            "Container" -> createContainer(props, children, id)
            "Divider" -> createDivider(props, id)
            else -> throw IllegalArgumentException("Unsupported component type: $type")
        }
    }

    override fun supports(type: String): Boolean {
        return type in setOf("Text", "Button", "Column", "Row", "Container", "Divider")
    }

    private fun createText(props: Map<String, JsonElement>, id: String?): Component {
        // Import actual Text component from phase1
        return com.augmentalis.avaelements.components.phase1.display.Text(
            id = id,
            content = props["value"]?.jsonPrimitive?.contentOrNull
                ?: props["content"]?.jsonPrimitive?.contentOrNull
                ?: ""
        )
    }

    private fun createButton(
        props: Map<String, JsonElement>,
        callbacks: Map<String, () -> Unit>,
        id: String?
    ): Component {
        val onClick = callbacks["onTap"] ?: callbacks["onClick"] ?: {}
        return com.augmentalis.avaelements.components.phase1.form.Button(
            id = id,
            label = props["label"]?.jsonPrimitive?.contentOrNull ?: "Button",
            onClick = onClick
        )
    }

    private fun createColumn(
        props: Map<String, JsonElement>,
        children: List<Component>?,
        id: String?
    ): Component {
        return com.augmentalis.avaelements.components.phase1.layout.Column(
            id = id,
            children = children ?: emptyList()
        )
    }

    private fun createRow(
        props: Map<String, JsonElement>,
        children: List<Component>?,
        id: String?
    ): Component {
        return com.augmentalis.avaelements.components.phase1.layout.Row(
            id = id,
            children = children ?: emptyList()
        )
    }

    private fun createContainer(
        props: Map<String, JsonElement>,
        children: List<Component>?,
        id: String?
    ): Component {
        return com.augmentalis.avaelements.components.phase1.layout.Container(
            id = id,
            child = children?.firstOrNull()
        )
    }

    private fun createDivider(props: Map<String, JsonElement>, id: String?): Component {
        // Placeholder - actual Divider component will be imported
        return com.augmentalis.avaelements.components.phase1.display.Text(
            id = id,
            content = "---"
        )
    }
}

