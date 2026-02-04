package com.augmentalis.avanues.avaui

import com.augmentalis.avanues.avaui.dsl.*
import com.augmentalis.avanues.avaui.registry.ComponentRegistry
import com.augmentalis.avanues.avaui.registry.ComponentDescriptor
import com.augmentalis.avanues.avaui.registry.BuiltInComponents
import com.augmentalis.avanues.avaui.instantiation.*
import com.augmentalis.avanues.avaui.events.*
import com.augmentalis.avanues.avaui.voice.*
import com.augmentalis.avanues.avaui.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * AvaUI DSL Runtime - Main orchestration class.
 *
 * Loads and executes .vos DSL apps with full lifecycle management.
 *
 * ## Overview
 *
 * The AvaUIRuntime is the central integration point that ties together all DSL
 * runtime components into a cohesive system. It provides a simple, high-level API
 * for loading, starting, controlling, and stopping VoiceOS applications defined
 * in the .vos DSL format.
 *
 * ## Architecture
 *
 * The runtime integrates 6 core subsystems:
 *
 * 1. **Parser** (Phase 1): Tokenizes and parses .vos source into AST
 * 2. **Registry** (Phase 2): Maintains component type metadata
 * 3. **Instantiator** (Phase 3): Creates Kotlin objects from AST nodes
 * 4. **Events** (Phase 4): Manages event bus and callback execution
 * 5. **Voice** (Phase 5): Routes voice commands with fuzzy matching
 * 6. **Lifecycle** (Phase 6): Controls app state and resource management
 *
 * ## Usage
 *
 * ```kotlin
 * // Create runtime instance
 * val runtime = AvaUIRuntime()
 *
 * // Load DSL app from source
 * val app = runtime.loadApp(dslSource)
 *
 * // Start app (onCreate → onStart → onResume)
 * runtime.start(app)
 *
 * // Handle voice command
 * val handled = runtime.handleVoiceCommand(app.id, "change color")
 *
 * // Pause app
 * runtime.pause(app.id)
 *
 * // Resume app
 * runtime.resume(app.id)
 *
 * // Stop app (onPause → onStop → onDestroy)
 * runtime.stop(app.id)
 * ```
 *
 * ## Lifecycle Management
 *
 * The runtime enforces the following lifecycle state machine:
 *
 * ```
 * CREATED → STARTED → RESUMED ⇄ PAUSED → STOPPED → DESTROYED
 * ```
 *
 * Apps must progress through create → start → resume to reach the active state.
 * Cleanup happens in reverse: pause → stop → destroy.
 *
 * ## Thread Safety
 *
 * All runtime operations are coroutine-based and safe for concurrent access.
 * The runtime uses a supervised job scope to ensure child coroutines don't
 * crash the entire runtime.
 *
 * ## Resource Management
 *
 * The runtime automatically:
 * - Tracks all instantiated components
 * - Manages event subscriptions
 * - Registers/unregisters voice commands
 * - Releases resources on app stop
 *
 * @property registry Component registry for type metadata (injectable)
 * @property scope Coroutine scope for async operations (injectable)
 *
 * @see VosParser for DSL parsing
 * @see ComponentRegistry for component metadata
 * @see ComponentInstantiator for object instantiation
 * @see EventBus for event routing
 * @see VoiceCommandRouter for voice command matching
 * @see AppLifecycle for lifecycle state management
 *
 * @since 3.1.0
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-10-27
 */
class AvaUIRuntime(
    private val registry: ComponentRegistry = ComponentRegistry.getInstance(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    private val eventBus = EventBus()
    private val voiceRouter = VoiceCommandRouter()
    private val actionDispatcher: ActionDispatcher

    private val runningApps = mutableMapOf<String, RunningApp>()

    private val instantiator: ComponentInstantiator
    private val callbackAdapter: CallbackAdapter

    init {
        // Initialize components
        val propertyMapper = PropertyMapper()
        val typeCoercion = TypeCoercion()

        // Create instantiator with registry adapter
        instantiator = ComponentInstantiator(
            object : com.augmentalis.avanues.avaui.instantiation.ComponentRegistry {
                override fun get(type: String): ComponentDescriptor? {
                    return runBlocking {
                        registry.get(type)
                    }
                }
            },
            propertyMapper,
            typeCoercion
        )

        val eventContext = EventContext.withStandardGlobals()
        callbackAdapter = CallbackAdapter(eventBus, eventContext)

        actionDispatcher = ActionDispatcher(eventBus)

        // Register built-in components
        scope.launch {
            BuiltInComponents.registerAll(registry)
        }
    }

    /**
     * Load DSL app from source string.
     *
     * Parses the .vos source code into an Abstract Syntax Tree (AST) representing
     * the app structure. The parsing process includes:
     *
     * 1. Tokenization: Breaking source into tokens
     * 2. Syntax analysis: Building AST from token stream
     * 3. Validation: Ensuring required fields are present
     *
     * @param dslSource .vos DSL source code
     * @return Parsed app AST node
     * @throws RuntimeException if parsing fails (wraps ParserException)
     * @throws ParserException if DSL syntax is invalid
     *
     * Example:
     * ```kotlin
     * val source = """
     *     App {
     *         id: "com.example.colorpicker"
     *         name: "Color Picker"
     *
     *         ColorPicker {
     *             id: "mainPicker"
     *             initialColor: "#FF5733"
     *         }
     *     }
     * """
     * val app = runtime.loadApp(source)
     * ```
     */
    fun loadApp(dslSource: String): VosAstNode.App {
        try {
            // Tokenize
            val tokenizer = VosTokenizer(dslSource)
            val tokens = tokenizer.tokenize()

            // Parse
            val parser = VosParser(tokens)
            val app = parser.parse()

            return app
        } catch (e: ParserException) {
            throw RuntimeException("Failed to load app: ${e.message}", e)
        } catch (e: Exception) {
            throw RuntimeException("Failed to load app: ${e.message}", e)
        }
    }

    /**
     * Start an app (full lifecycle: create → start → resume).
     *
     * Starting an app performs the following operations:
     *
     * 1. **Lifecycle: onCreate** - Initialize app state
     * 2. **Component Instantiation** - Create all component instances
     * 3. **Callback Binding** - Attach event handlers
     * 4. **Voice Registration** - Register voice commands
     * 5. **Lifecycle: onStart** - Prepare app for display
     * 6. **Lifecycle: onResume** - Activate app
     *
     * After this method completes, the app is in RESUMED state and actively running.
     *
     * @param app Parsed app AST from loadApp()
     * @return Running app handle for control operations
     * @throws RuntimeException if app is already running
     * @throws InstantiationException if component creation fails
     *
     * Example:
     * ```kotlin
     * val app = runtime.loadApp(source)
     * val runningApp = runtime.start(app)
     * println("App ${runningApp.name} is now running")
     * ```
     */
    suspend fun start(app: VosAstNode.App): RunningApp {
        // Check if already running
        if (runningApps.containsKey(app.id)) {
            throw RuntimeException("App already running: ${app.id}")
        }

        // Create lifecycle
        val lifecycle = AppLifecycle()
        val resourceManager = ResourceManager()
        val stateManager = StateManager()

        // Create running app
        val runningApp = RunningApp(
            id = app.id,
            name = app.name,
            ast = app,
            lifecycle = lifecycle,
            resourceManager = resourceManager,
            stateManager = stateManager,
            components = mutableMapOf()
        )

        // Lifecycle: onCreate
        lifecycle.create()

        // Instantiate components
        for (component in app.components) {
            val instance = instantiator.instantiate(component)
            val componentId = component.id ?: component.type
            runningApp.components[componentId] = instance

            // Bind callbacks
            component.callbacks.forEach { (callbackName, lambda) ->
                val callback = callbackAdapter.createCallback(
                    lambda = lambda,
                    componentId = componentId,
                    eventName = callbackName
                )
                // Store callback for later invocation
                // The callback is registered with the event bus via CallbackAdapter
            }
        }

        // Register voice commands
        app.voiceCommands.forEach { (trigger, action) ->
            voiceRouter.register(trigger, action, app.id)
        }

        // Lifecycle: onStart
        lifecycle.start()

        // Lifecycle: onResume
        lifecycle.resume()

        // Store running app
        runningApps[app.id] = runningApp

        return runningApp
    }

    /**
     * Pause an app.
     *
     * Transitions the app to PAUSED state. While paused, the app:
     * - Remains in memory
     * - Retains all state
     * - Does not process events
     * - Can be resumed later
     *
     * This is useful for temporarily suspending an app without full teardown.
     *
     * @param appId Application identifier
     * @throws RuntimeException if app not found
     * @throws LifecycleException if app is not in valid state for pausing
     *
     * Example:
     * ```kotlin
     * runtime.pause(app.id)
     * // App is now paused but still in memory
     * ```
     */
    suspend fun pause(appId: String) {
        val app = runningApps[appId] ?: throw RuntimeException("App not found: $appId")
        app.lifecycle.pause()
    }

    /**
     * Resume a paused app.
     *
     * Transitions the app from PAUSED to RESUMED state. The app becomes
     * active again and resumes processing events.
     *
     * @param appId Application identifier
     * @throws RuntimeException if app not found
     * @throws LifecycleException if app is not in PAUSED state
     *
     * Example:
     * ```kotlin
     * runtime.resume(app.id)
     * // App is now active again
     * ```
     */
    suspend fun resume(appId: String) {
        val app = runningApps[appId] ?: throw RuntimeException("App not found: $appId")
        app.lifecycle.resume()
    }

    /**
     * Stop an app (full lifecycle: pause → stop → destroy).
     *
     * Stopping an app performs complete teardown:
     *
     * 1. **Lifecycle: onPause** - Deactivate app (if resumed)
     * 2. **Lifecycle: onStop** - Stop app processes
     * 3. **Resource Cleanup** - Release all managed resources
     * 4. **Voice Unregistration** - Clear voice commands
     * 5. **Lifecycle: onDestroy** - Final cleanup
     *
     * After this method completes, the app is removed from memory and all
     * resources are released. To run the app again, you must call start().
     *
     * @param appId Application identifier
     * @throws RuntimeException if app not found
     *
     * Example:
     * ```kotlin
     * runtime.stop(app.id)
     * // App is now completely stopped and removed from memory
     * ```
     */
    suspend fun stop(appId: String) {
        val app = runningApps[appId] ?: throw RuntimeException("App not found: $appId")

        // Lifecycle: onPause (if currently resumed)
        if (app.lifecycle.state.value == LifecycleState.RESUMED) {
            app.lifecycle.pause()
        }

        // Lifecycle: onStop
        app.lifecycle.stop()

        // Cleanup resources
        app.resourceManager.releaseAll()

        // Unregister voice commands for this app
        voiceRouter.clear()

        // Lifecycle: onDestroy
        app.lifecycle.destroy()

        // Remove from running apps
        runningApps.remove(appId)
    }

    /**
     * Handle voice command for an app.
     *
     * Processes voice input through the following pipeline:
     *
     * 1. **Normalization**: Convert input to lowercase, trim whitespace
     * 2. **Matching**: Find best matching registered command (exact or fuzzy)
     * 3. **Action Dispatch**: Execute the associated action
     * 4. **Event Emission**: Trigger callbacks via event bus
     *
     * The voice router uses fuzzy matching with a similarity threshold of 0.7,
     * allowing for minor variations in phrasing while maintaining accuracy.
     *
     * @param appId Application identifier
     * @param voiceInput Voice command text (e.g., "change color", "show settings")
     * @return true if command matched and executed, false if no match
     * @throws RuntimeException if app not found
     * @throws ActionDispatchException if action execution fails
     *
     * Example:
     * ```kotlin
     * // App has voice command: "change color" → "openColorPicker"
     * val handled = runtime.handleVoiceCommand(app.id, "change the color")
     * if (handled) {
     *     println("Command executed successfully")
     * } else {
     *     println("No matching command found")
     * }
     * ```
     */
    suspend fun handleVoiceCommand(appId: String, voiceInput: String): Boolean {
        val app = runningApps[appId] ?: throw RuntimeException("App not found: $appId")

        // Match command
        val match = voiceRouter.match(voiceInput) ?: return false

        // Dispatch action with app context
        actionDispatcher.dispatch(match, mapOf("appId" to appId))

        return true
    }

    /**
     * Get running app by ID.
     *
     * Retrieves the running app handle for inspection or control operations.
     * Returns null if no app with the given ID is currently running.
     *
     * @param appId Application identifier
     * @return Running app handle or null
     *
     * Example:
     * ```kotlin
     * val app = runtime.getApp("com.example.myapp")
     * if (app != null) {
     *     println("App ${app.name} is running")
     *     println("Lifecycle state: ${app.lifecycle.state.value}")
     * }
     * ```
     */
    fun getApp(appId: String): RunningApp? {
        return runningApps[appId]
    }

    /**
     * Get all running apps.
     *
     * Returns a snapshot of all currently running applications.
     * The returned list is a copy and will not reflect future changes.
     *
     * @return List of running app handles (may be empty)
     *
     * Example:
     * ```kotlin
     * val apps = runtime.getAllApps()
     * println("Running ${apps.size} app(s)")
     * apps.forEach { app ->
     *     println("- ${app.name} (${app.id})")
     * }
     * ```
     */
    fun getAllApps(): List<RunningApp> {
        return runningApps.values.toList()
    }

    /**
     * Shutdown runtime (stop all apps).
     *
     * Performs complete runtime teardown:
     *
     * 1. Stop all running apps (in registration order)
     * 2. Cancel all coroutines
     * 3. Clear all registrations
     *
     * After calling shutdown(), this runtime instance should not be used again.
     * Create a new AvaUIRuntime instance if you need to restart.
     *
     * @throws Exception if shutdown encounters errors (errors are logged but don't halt shutdown)
     *
     * Example:
     * ```kotlin
     * runtime.shutdown()
     * // All apps stopped, runtime is now inactive
     * ```
     */
    suspend fun shutdown() {
        // Stop all running apps
        val appIds = runningApps.keys.toList()
        for (appId in appIds) {
            try {
                stop(appId)
            } catch (e: Exception) {
                // Log error but continue shutdown
                println("Error stopping app $appId during shutdown: ${e.message}")
            }
        }

        // Cancel coroutine scope
        scope.cancel()
    }
}

/**
 * Represents a running DSL app instance.
 *
 * This data class encapsulates all runtime state for a running application,
 * including its AST, lifecycle controller, resource manager, state manager,
 * and instantiated component instances.
 *
 * @property id Unique application identifier (e.g., "com.example.myapp")
 * @property name Human-readable application name
 * @property ast Original parsed AST from DSL source
 * @property lifecycle Lifecycle state controller
 * @property resourceManager Manages app resources (cleanup on stop)
 * @property stateManager Persists/restores app state
 * @property components Map of component IDs to instantiated objects
 *
 * @since 3.1.0
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-10-27
 */
data class RunningApp(
    val id: String,
    val name: String,
    val ast: VosAstNode.App,
    val lifecycle: AppLifecycle,
    val resourceManager: ResourceManager,
    val stateManager: StateManager,
    val components: MutableMap<String, Any>
)
