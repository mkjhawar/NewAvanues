package com.augmentalis.magicelements.renderers.android

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.magicelements.core.mel.*
import kotlinx.coroutines.flow.collectAsState

/**
 * Composable functions for rendering MEL plugins with reactive state.
 *
 * Provides high-level Composables for integrating MEL plugins into Android apps.
 * Handles state observation, event dispatching, and automatic re-rendering.
 *
 * ## Usage Examples
 *
 * ### Basic Plugin Rendering
 * ```kotlin
 * @Composable
 * fun CalculatorScreen() {
 *     val runtime = remember {
 *         loadMELPlugin("calculator.yaml")
 *     }
 *     MELPlugin(runtime)
 * }
 * ```
 *
 * ### With Loading State
 * ```kotlin
 * @Composable
 * fun PluginScreen(pluginId: String) {
 *     MELPluginWithLoading(
 *         pluginLoader = { loadPluginById(pluginId) },
 *         onError = { error ->
 *             Text("Failed to load plugin: ${error.message}")
 *         }
 *     )
 * }
 * ```
 *
 * ### With State Persistence
 * ```kotlin
 * @Composable
 * fun PersistentPlugin(pluginId: String) {
 *     val savedState = rememberSavedPluginState(pluginId)
 *     MELPluginWithState(
 *         pluginId = pluginId,
 *         initialState = savedState,
 *         onStateChange = { state ->
 *             savePluginState(pluginId, state)
 *         }
 *     )
 * }
 * ```
 *
 * @since 2.0.0
 */

/**
 * Render a MEL plugin with reactive state observation.
 *
 * This is the main entry point for rendering MEL plugins in Compose.
 * It observes the plugin state and automatically re-renders when state changes.
 *
 * @param runtime Plugin runtime with state and reducer engine
 * @param modifier Modifier for the plugin container
 * @param onError Error handler callback
 */
@Composable
fun MELPlugin(
    runtime: PluginRuntime,
    modifier: Modifier = Modifier,
    onError: ((Throwable) -> Unit)? = null
) {
    // Observe plugin state reactively
    val state by runtime.stateFlow.collectAsState()

    // Track errors
    var error by remember { mutableStateOf<Throwable?>(null) }

    // Handle errors
    LaunchedEffect(error) {
        error?.let { onError?.invoke(it) }
    }

    Box(modifier = modifier) {
        if (error != null) {
            // Show error state
            ErrorState(error = error!!)
        } else {
            try {
                // Render the plugin
                val renderer = remember(runtime) {
                    MELPluginRenderer(runtime)
                }
                renderer.Render()
            } catch (e: Exception) {
                error = e
            }
        }
    }
}

/**
 * Render a MEL plugin from a definition with loading state.
 *
 * Shows a loading indicator while the plugin is being loaded,
 * then renders the plugin once ready.
 *
 * @param pluginLoader Suspending function that loads the plugin definition
 * @param modifier Modifier for the plugin container
 * @param onError Error handler callback
 * @param loadingContent Optional custom loading content
 */
@Composable
fun MELPluginWithLoading(
    pluginLoader: suspend () -> PluginDefinition,
    modifier: Modifier = Modifier,
    onError: ((Throwable) -> Unit)? = null,
    loadingContent: @Composable () -> Unit = { DefaultLoadingContent() }
) {
    var runtime by remember { mutableStateOf<PluginRuntime?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<Throwable?>(null) }

    // Load plugin definition
    LaunchedEffect(Unit) {
        try {
            val definition = pluginLoader()
            runtime = PluginRuntime.create(definition, Platform.ANDROID)
            isLoading = false
        } catch (e: Exception) {
            error = e
            isLoading = false
            onError?.invoke(e)
        }
    }

    Box(modifier = modifier) {
        when {
            isLoading -> loadingContent()
            error != null -> ErrorState(error = error!!)
            runtime != null -> MELPlugin(runtime!!, onError = onError)
        }
    }
}

/**
 * Render a MEL plugin with state management.
 *
 * Allows passing initial state and observing state changes.
 *
 * @param pluginDefinition Plugin definition
 * @param initialState Initial state to restore (optional)
 * @param modifier Modifier for the plugin container
 * @param onStateChange Callback invoked when state changes
 * @param onError Error handler callback
 */
@Composable
fun MELPluginWithState(
    pluginDefinition: PluginDefinition,
    initialState: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    modifier: Modifier = Modifier,
    onStateChange: ((Map<String, kotlinx.serialization.json.JsonElement>) -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null
) {
    val runtime = remember(pluginDefinition) {
        PluginRuntime.create(pluginDefinition, Platform.ANDROID).apply {
            // Restore initial state if provided
            initialState?.let { updateState(it) }
        }
    }

    // Observe state changes
    val state by runtime.stateFlow.collectAsState()

    // Notify state changes
    LaunchedEffect(state) {
        onStateChange?.invoke(state.snapshot())
    }

    MELPlugin(
        runtime = runtime,
        modifier = modifier,
        onError = onError
    )
}

/**
 * Render a MEL plugin from YAML/JSON source.
 *
 * @param source Plugin source as YAML or JSON string
 * @param format Source format (YAML or JSON)
 * @param modifier Modifier for the plugin container
 * @param onError Error handler callback
 */
@Composable
fun MELPluginFromSource(
    source: String,
    format: PluginSourceFormat = PluginSourceFormat.JSON,
    modifier: Modifier = Modifier,
    onError: ((Throwable) -> Unit)? = null
) {
    var runtime by remember { mutableStateOf<PluginRuntime?>(null) }
    var error by remember { mutableStateOf<Throwable?>(null) }

    // Parse source and create runtime
    LaunchedEffect(source, format) {
        try {
            runtime = PluginRuntime.fromSource(source, format, Platform.ANDROID)
        } catch (e: Exception) {
            error = e
            onError?.invoke(e)
        }
    }

    Box(modifier = modifier) {
        when {
            error != null -> ErrorState(error = error!!)
            runtime != null -> MELPlugin(runtime!!, onError = onError)
            else -> DefaultLoadingContent()
        }
    }
}

/**
 * Render multiple MEL plugins in a list.
 *
 * Useful for plugin galleries or dashboards.
 *
 * @param runtimes List of plugin runtimes to render
 * @param modifier Modifier for the container
 * @param itemSpacing Spacing between plugins
 */
@Composable
fun MELPluginList(
    runtimes: List<PluginRuntime>,
    modifier: Modifier = Modifier,
    itemSpacing: Int = 16
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(itemSpacing.dp)
    ) {
        runtimes.forEach { runtime ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                MELPlugin(runtime = runtime)
            }
        }
    }
}

/**
 * Render a MEL plugin with debug information.
 *
 * Shows plugin metadata, state, and stats below the rendered UI.
 * Useful for development and debugging.
 *
 * @param runtime Plugin runtime
 * @param modifier Modifier for the container
 * @param showState Whether to show the current state
 * @param showStats Whether to show runtime statistics
 */
@Composable
fun MELPluginDebug(
    runtime: PluginRuntime,
    modifier: Modifier = Modifier,
    showState: Boolean = true,
    showStats: Boolean = true
) {
    Column(modifier = modifier) {
        // Render the plugin
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            MELPlugin(runtime = runtime)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show debug information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Plugin metadata
                val metadata = runtime.getMetadata()
                Text(
                    text = "Plugin: ${metadata.name} v${metadata.version}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "ID: ${metadata.id}",
                    style = MaterialTheme.typography.bodySmall
                )

                // Tier info
                val tierInfo = runtime.getTierInfo()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tier: ${tierInfo.effective}${if (tierInfo.downgraded) " (downgraded from ${tierInfo.requested})" else ""}",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Current state
                if (showState) {
                    val state by runtime.stateFlow.collectAsState()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "State:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = state.snapshot().toString(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Runtime stats
                if (showStats) {
                    val stats = runtime.getStats()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stats:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Dispatches: ${stats.dispatchCount}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Text(
                        text = "State variables: ${stats.stateSize}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Text(
                        text = "Reducers: ${stats.reducerCount}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// Default UI Components
// ============================================================================

/**
 * Default loading indicator.
 */
@Composable
private fun DefaultLoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading plugin...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Error state display.
 */
@Composable
private fun ErrorState(error: Throwable) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Plugin Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = error.message ?: "Unknown error",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (error is PluginRuntimeException) {
                Text(
                    text = "Runtime error - check plugin definition",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

// ============================================================================
// State Management Helpers
// ============================================================================

/**
 * Remember a saved plugin state.
 *
 * This would integrate with DataStore or SharedPreferences in a full implementation.
 */
@Composable
fun rememberSavedPluginState(
    pluginId: String
): Map<String, kotlinx.serialization.json.JsonElement>? {
    // TODO: Implement state persistence
    // For now, return null (no saved state)
    return null
}

/**
 * Save plugin state.
 *
 * This would integrate with DataStore or SharedPreferences in a full implementation.
 */
fun savePluginState(
    pluginId: String,
    state: Map<String, kotlinx.serialization.json.JsonElement>
) {
    // TODO: Implement state persistence
    println("Saving state for plugin $pluginId: $state")
}

/**
 * Load a MEL plugin from a file path.
 *
 * This would integrate with PlatformPluginLoader in a full implementation.
 */
suspend fun loadMELPlugin(path: String): PluginRuntime {
    // TODO: Implement plugin loading from file
    // For now, throw an error
    throw NotImplementedError("Plugin loading from file not yet implemented")
}

/**
 * Load a MEL plugin by ID.
 *
 * This would integrate with a plugin repository in a full implementation.
 */
suspend fun loadPluginById(pluginId: String): PluginDefinition {
    // TODO: Implement plugin loading by ID
    // For now, throw an error
    throw NotImplementedError("Plugin loading by ID not yet implemented")
}
