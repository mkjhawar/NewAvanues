# Universal Plugin Architecture - Developer Guide

**Version:** 1.0.0
**Date:** 2026-01-22
**Phase:** 4 Complete

## Overview

The Universal Plugin Architecture provides a flexible, extensible system for adding functionality to VoiceOSCore applications. Plugins can handle voice commands, provide UI interactions, and extend platform capabilities.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Getting Started](#getting-started)
3. [Creating Plugins](#creating-plugins)
4. [Handler Plugins](#handler-plugins)
5. [Android Integration](#android-integration)
6. [Performance Monitoring](#performance-monitoring)
7. [Hot-Reload](#hot-reload)
8. [Migration Guide](#migration-guide)
9. [API Reference](#api-reference)

---

## Architecture Overview

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
├─────────────────────────────────────────────────────────────┤
│  VoiceOSCoreNGApplication                                   │
│    ├── AndroidPluginHost                                    │
│    ├── PluginCommandDispatcher                              │
│    └── PluginHandlerBridge                                  │
├─────────────────────────────────────────────────────────────┤
│                    Plugin Layer                              │
├─────────────────────────────────────────────────────────────┤
│  Built-in Plugins:                                          │
│    ├── NavigationHandlerPlugin                              │
│    ├── UIInteractionPlugin                                  │
│    ├── TextInputPlugin                                      │
│    ├── SystemCommandPlugin                                  │
│    ├── GesturePlugin                                        │
│    ├── SelectionPlugin                                      │
│    └── AppLauncherPlugin                                    │
├─────────────────────────────────────────────────────────────┤
│                    Core Layer                                │
├─────────────────────────────────────────────────────────────┤
│  Plugin SDK:                                                │
│    ├── Plugin (interface)                                   │
│    ├── BasePlugin (abstract)                                │
│    ├── HandlerPlugin (interface)                            │
│    ├── PluginCapability                                     │
│    └── PluginHost                                           │
└─────────────────────────────────────────────────────────────┘
```

### Key Concepts

- **Plugin**: Self-contained unit of functionality
- **Plugin Host**: Manages plugin lifecycle and discovery
- **Handler Plugin**: Specialization for handling voice commands
- **Executor**: Platform-specific implementation of operations
- **Command Dispatcher**: Routes commands to appropriate handlers

---

## Getting Started

### Prerequisites

- Kotlin Multiplatform project
- Android API 24+ (for Android target)
- PluginSystem module dependency

### Basic Setup (KMP-style)

The plugin system is KMP (Kotlin Multiplatform) and provides a common interface with platform-specific implementations.

```kotlin
// 1. In Application.onCreate()
class MyApplication : Application() {
    lateinit var pluginSetup: AndroidPluginSystemSetup
        private set

    override fun onCreate() {
        super.onCreate()

        // Create platform-specific setup (works for Android, iOS, JVM)
        pluginSetup = PluginSystemSetup.create(this) as AndroidPluginSystemSetup

        // Initialize asynchronously
        lifecycleScope.launch {
            val result = pluginSetup.initialize(
                PluginSystemConfig(
                    debugMode = BuildConfig.DEBUG,
                    enablePerformanceMonitoring = true,
                    registerBuiltinPlugins = true
                )
            )

            if (result.success) {
                Log.i("App", "Plugin system initialized: ${result.pluginsLoaded} plugins")
            }
        }
    }

    // Convenience accessors
    val pluginHost get() = pluginSetup.androidPluginHost
    val commandDispatcher get() = pluginSetup.pluginCommandDispatcher
}
```

### Connecting AccessibilityService

```kotlin
// 2. In AccessibilityService.onServiceConnected()
override fun onServiceConnected() {
    super.onServiceConnected()

    // Notify plugin system
    lifecycleScope.launch {
        (application as MyApplication).pluginSetup.onServiceConnected(this@MyService)
    }
}

override fun onDestroy() {
    // Notify plugin system
    lifecycleScope.launch {
        (application as MyApplication).pluginSetup.onServiceDisconnected()
    }

    super.onDestroy()
}
```

### Configuration Options

```kotlin
data class PluginSystemConfig(
    val debugMode: Boolean = false,           // Enable debug logging
    val enablePerformanceMonitoring: Boolean = true,  // Track metrics
    val enableHotReload: Boolean = false,     // Allow runtime plugin updates
    val minHandlerConfidence: Float = 0.7f,   // Min confidence for handler selection
    val registerBuiltinPlugins: Boolean = true,  // Register built-in handlers
    val customPluginFactories: Map<String, () -> Plugin> = emptyMap()  // Custom plugins
)
```

---

## Creating Plugins

### Basic Plugin Structure

```kotlin
class MyPlugin : BasePlugin() {

    override val pluginId: String = "com.example.myplugin"
    override val pluginName: String = "My Custom Plugin"
    override val version: String = "1.0.0"

    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability(
            id = "my_capability",
            name = "My Capability",
            version = "1.0.0",
            interfaces = setOf("MyPlugin"),
            metadata = mapOf("key" to "value")
        )
    )

    override suspend fun onInitialize(): InitResult {
        // Initialization logic
        return InitResult.success("Plugin initialized")
    }

    override suspend fun onShutdown() {
        // Cleanup logic
    }

    override fun getHealthDiagnostics(): Map<String, String> {
        return mapOf(
            "status" to "healthy",
            "customMetric" to "value"
        )
    }
}
```

### Registering Custom Plugins

```kotlin
// Register a custom plugin factory
pluginHost.registerBuiltinPluginFactory("com.example.myplugin") {
    MyPlugin()
}
```

---

## Handler Plugins

Handler plugins process voice commands and execute actions.

### Creating a Handler Plugin

```kotlin
class MyHandlerPlugin(
    private val executorProvider: () -> MyExecutor
) : BasePlugin(), HandlerPlugin {

    override val pluginId: String = "com.example.myhandler"
    override val pluginName: String = "My Handler"
    override val version: String = "1.0.0"

    override val handlerType: HandlerType = HandlerType.NAVIGATION

    override val patterns: List<CommandPattern> = listOf(
        CommandPattern(
            regex = Regex("^my command$", RegexOption.IGNORE_CASE),
            intent = "MY_INTENT",
            requiredEntities = emptySet(),
            examples = listOf("my command")
        )
    )

    override fun canHandle(command: QuantizedCommand, context: HandlerContext): Boolean {
        return patterns.any { it.matches(command.phrase) }
    }

    override suspend fun handle(
        command: QuantizedCommand,
        context: HandlerContext
    ): ActionResult {
        val executor = executorProvider()

        return when (command.phrase.lowercase()) {
            "my command" -> {
                if (executor.doSomething()) {
                    ActionResult.Success("Done!")
                } else {
                    ActionResult.Error("Failed")
                }
            }
            else -> ActionResult.Error("Unknown command")
        }
    }

    override fun getConfidence(command: QuantizedCommand, context: HandlerContext): Float {
        val phrase = command.phrase.lowercase()
        return when {
            phrase == "my command" -> 1.0f
            phrase.contains("my") -> 0.5f
            else -> 0.0f
        }
    }
}
```

### Executor Interface

```kotlin
interface MyExecutor {
    suspend fun doSomething(): Boolean
}

// Android implementation
class AndroidMyExecutor(
    private val serviceRegistry: ServiceRegistry
) : MyExecutor {

    private val accessibilityService: AccessibilityService?
        get() = serviceRegistry.getSync(ServiceRegistry.ACCESSIBILITY_SERVICE)

    override suspend fun doSomething(): Boolean {
        val service = accessibilityService ?: return false
        // Implementation using AccessibilityService
        return true
    }
}
```

---

## Android Integration

### Service Registry

The `ServiceRegistry` provides access to Android services.

```kotlin
// Get AccessibilityService
val service = serviceRegistry.getSync(ServiceRegistry.ACCESSIBILITY_SERVICE)

// Register a service
serviceRegistry.register(ServiceRegistry.ACCESSIBILITY_SERVICE, myService)
```

### Built-in Executors

| Executor | Purpose |
|----------|---------|
| `AndroidNavigationExecutor` | Scroll, swipe gestures |
| `AndroidUIInteractionExecutor` | Click, long-press, toggle |
| `AndroidTextInputExecutor` | Text entry, clipboard |
| `AndroidSystemCommandExecutor` | Back, home, recents |
| `AndroidGestureExecutor` | Complex touch gestures |
| `AndroidSelectionExecutor` | Text selection |
| `AndroidClipboardProvider` | Clipboard operations |
| `AndroidAppLauncherExecutor` | App launching |

---

## Performance Monitoring

### Using PluginPerformanceMonitor

```kotlin
val monitor = PluginPerformanceMonitor.instance

// Time an operation
monitor.time("command_dispatch") {
    dispatcher.dispatch(command, context)
}

// Record plugin execution
monitor.recordPluginExecution(pluginId, executionTimeMs, success = true)

// Get metrics
val metrics = monitor.metrics.value
println("Total commands: ${metrics.systemMetrics.totalCommandsProcessed}")
println("P95 latency: ${metrics.operationMetrics["command_dispatch"]?.percentile95Ms}ms")

// Generate report
println(monitor.getSummaryReport())
```

### Metrics Available

- **Operation Metrics**: Count, success rate, min/max/avg/p95 latency
- **Plugin Metrics**: Commands handled, avg execution time, init time
- **System Metrics**: Total commands, plugins loaded/active, uptime

---

## Hot-Reload

### Reloading Plugins

```kotlin
val reloader = PluginHotReloader(pluginHost)

// Reload a specific plugin
val result = reloader.reloadPlugin("com.example.myplugin")
if (result.success) {
    println("Reloaded in ${result.durationMs}ms")
}

// Replace a plugin with new version
reloader.replacePlugin("com.example.myplugin") {
    MyUpdatedPlugin()
}

// Monitor reload status
reloader.status.collect { status ->
    when (status) {
        is ReloadStatus.Success -> println("Success")
        is ReloadStatus.Failed -> println("Failed: ${status.error}")
    }
}
```

---

## Migration Guide

### From Legacy Handlers to Plugins

1. **Use PluginHandlerBridge** for gradual migration:

```kotlin
val bridge = PluginHandlerBridgeFactory.createForMigration(
    pluginDispatcher = commandDispatcher,
    legacyDispatcher = { command, context ->
        legacyHandlerRegistry.dispatch(command, context)
    }
)

// Commands go to plugins first, then legacy if needed
val result = bridge.dispatch(command, context)
```

2. **Monitor migration progress**:

```kotlin
val metrics = bridge.metrics.value
println("Plugin coverage: ${metrics.pluginCoveragePercent}%")
println("Legacy fallback: ${metrics.legacyFallback}")
```

3. **Disable legacy** when ready:

```kotlin
bridge.disableLegacyFallback()
```

### Handler Mapping

| Legacy Handler | Plugin ID |
|---------------|-----------|
| `NavigationHandler` | `com.augmentalis.voiceoscore.handler.navigation` |
| `UIHandler` | `com.augmentalis.voiceoscore.handler.uiinteraction` |
| `InputHandler` | `com.augmentalis.voiceoscore.handler.textinput` |
| `SystemHandler` | `com.augmentalis.voiceoscore.handler.system` |
| `GestureHandler` | `com.augmentalis.voiceoscore.handler.gesture` |
| `SelectHandler` | `com.augmentalis.voiceoscore.handler.selection` |
| `AppHandler` | `com.augmentalis.voiceoscore.handler.applauncher` |

---

## API Reference

### Core Interfaces

#### Plugin

```kotlin
interface Plugin {
    val pluginId: String
    val pluginName: String
    val version: String
    val capabilities: Set<PluginCapability>
    val state: PluginState

    suspend fun initialize(config: PluginConfig, context: PluginContext): InitResult
    suspend fun shutdown()
    fun getHealthDiagnostics(): Map<String, String>
}
```

#### HandlerPlugin

```kotlin
interface HandlerPlugin : Plugin {
    val handlerType: HandlerType
    val patterns: List<CommandPattern>

    fun canHandle(command: QuantizedCommand, context: HandlerContext): Boolean
    suspend fun handle(command: QuantizedCommand, context: HandlerContext): ActionResult
    fun getConfidence(command: QuantizedCommand, context: HandlerContext): Float
}
```

#### IPluginHost

```kotlin
interface IPluginHost<T> {
    suspend fun loadPlugins()
    fun getPlugin(id: String): Plugin?
    fun getLoadedPlugins(): List<Plugin>
    fun getPluginsByCapability(capabilityId: String): List<Plugin>
}
```

### Result Types

#### ActionResult

```kotlin
sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Error(val message: String) : ActionResult()
    data class Ambiguous(val options: List<DisambiguationOption>) : ActionResult()
}
```

#### InitResult

```kotlin
data class InitResult(
    val success: Boolean,
    val message: String,
    val recoverable: Boolean = true
)
```

---

## Best Practices

1. **Keep plugins focused**: One plugin should do one thing well
2. **Use lazy executors**: Pass providers, not instances, for late binding
3. **Handle service unavailability**: Always check for null AccessibilityService
4. **Test with mocks**: Use mock executors for unit testing
5. **Monitor performance**: Use PluginPerformanceMonitor in debug builds
6. **Version your plugins**: Update version strings for compatibility tracking
7. **Document patterns**: Include example commands in CommandPattern

---

## Troubleshooting

### Plugin Not Loading

1. Check if factory is registered: `BuiltinPluginRegistration.registerAll()`
2. Verify plugin ID matches registration
3. Check initialization logs for errors

### Commands Not Handled

1. Verify `canHandle()` returns true for your command
2. Check confidence score is above threshold (default: 0.7)
3. Ensure AccessibilityService is connected

### Performance Issues

1. Use `PluginPerformanceMonitor` to identify bottlenecks
2. Check P95 latency for slow operations
3. Consider lazy initialization for heavy resources

---

## Support

- Issues: Report to development team
- Documentation: See `/Docs/VoiceOSCore/` for additional docs
- Examples: See built-in plugins in `/Modules/PluginSystem/src/commonMain/kotlin/`
