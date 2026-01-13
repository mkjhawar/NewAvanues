package com.augmentalis.avacode.plugins.platform

/**
 * Example iOS plugin demonstrating the registration pattern.
 *
 * This example shows how to create an iOS plugin that works within
 * Apple's security restrictions (no dynamic code loading).
 *
 * ## Implementation Pattern
 *
 * 1. Create your plugin class (implements your plugin interface)
 * 2. Register it at static initialization time using @EagerInitialization
 * 3. The plugin system will be able to "load" it via the registry
 *
 * @since 1.0.0
 */

/**
 * Example plugin interface.
 *
 * In production, this would be defined in your plugin SDK.
 */
interface ExamplePlugin {
    fun getName(): String
    fun execute(): String
}

/**
 * Example plugin implementation for iOS.
 */
class HelloWorldIosPlugin : ExamplePlugin {
    override fun getName(): String = "HelloWorldPlugin"

    override fun execute(): String = "Hello from iOS plugin!"
}

/**
 * Plugin registration for iOS.
 *
 * This registration MUST happen at static initialization time.
 * The @OptIn and @EagerInitialization annotations ensure this
 * code runs when the module is loaded, before any code tries
 * to use the plugin.
 *
 * ## Important
 * - Use the EXACT class name from your plugin manifest entrypoint
 * - The factory lambda should create a new instance each time
 * - This registration is global and happens once per app launch
 */
@OptIn(ExperimentalStdlibApi::class)
@EagerInitialization
private val helloWorldPluginRegistration = PluginClassLoader.register(
    className = "com.augmentalis.magiccode.plugins.platform.HelloWorldIosPlugin",
    factory = { HelloWorldIosPlugin() }
)

/**
 * Example of how to use the registered plugin.
 *
 * This would typically be called by the plugin system, not directly.
 */
fun exampleUsage() {
    val loader = PluginClassLoader()

    // This works because HelloWorldIosPlugin was registered above
    val plugin = loader.loadClass(
        className = "com.augmentalis.magiccode.plugins.platform.HelloWorldIosPlugin",
        pluginPath = "" // Ignored on iOS
    ) as HelloWorldIosPlugin

    println(plugin.execute()) // Prints: "Hello from iOS plugin!"
}

/**
 * Example of checking available plugins at runtime.
 */
fun listAvailablePlugins(): Set<String> {
    return PluginClassLoader.getRegisteredPlugins()
}

/**
 * Example plugin manifest (YAML) for this iOS plugin.
 *
 * Note: The entrypoint MUST match the className used in registration.
 *
 * ```yaml
 * # plugin.yaml
 * id: com.example.hello-world-ios
 * name: "Hello World iOS Plugin"
 * version: "1.0.0"
 * author: "Example Developer"
 * description: "Example iOS plugin using pre-bundled registration pattern"
 * entrypoint: "com.augmentalis.magiccode.plugins.platform.HelloWorldIosPlugin"
 * manifestVersion: "1.0"
 *
 * # iOS-specific metadata
 * platform:
 *   ios:
 *     registrationMode: "static"  # vs "dynamic" on Android/JVM
 *     minimumOsVersion: "14.0"
 *
 * capabilities:
 *   - "text-processing"
 *
 * permissions: []
 *
 * dependencies: []
 * ```
 */

/**
 * Developer guide for creating iOS plugins.
 *
 * ## Step-by-Step Guide
 *
 * ### 1. Create Plugin Module
 * Create a new Kotlin Multiplatform module with iOS target:
 * ```kotlin
 * // build.gradle.kts
 * kotlin {
 *     iosX64()
 *     iosArm64()
 *     iosSimulatorArm64()
 *
 *     sourceSets {
 *         val iosMain by getting {
 *             dependencies {
 *                 implementation(project(":runtime:plugin-system"))
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ### 2. Implement Plugin Interface
 * ```kotlin
 * // MyPlugin.kt
 * package com.example.myplugin
 *
 * class MyPlugin : PluginInterface {
 *     override fun doSomething() {
 *         println("iOS plugin running!")
 *     }
 * }
 * ```
 *
 * ### 3. Register Plugin (iOS-specific)
 * ```kotlin
 * // IosRegistration.kt (in iosMain source set)
 * package com.example.myplugin
 *
 * import com.augmentalis.avacode.plugins.platform.PluginClassLoader
 *
 * @OptIn(ExperimentalStdlibApi::class)
 * @EagerInitialization
 * private val registration = PluginClassLoader.register(
 *     className = "com.example.myplugin.MyPlugin",
 *     factory = { MyPlugin() }
 * )
 * ```
 *
 * ### 4. Create plugin.yaml Manifest
 * ```yaml
 * id: com.example.my-plugin
 * name: "My Plugin"
 * version: "1.0.0"
 * entrypoint: "com.example.myplugin.MyPlugin"
 * # ... rest of manifest
 * ```
 *
 * ### 5. Link Plugin into Main App
 * In your iOS app's build.gradle.kts:
 * ```kotlin
 * kotlin {
 *     iosX64 {
 *         binaries {
 *             framework {
 *                 // Link the plugin module
 *                 export(project(":plugins:my-plugin"))
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Why This Pattern Works on iOS
 *
 * - iOS prohibits loading unsigned code at runtime (no dlopen, no JIT)
 * - All code must be part of the app bundle at build time
 * - Static registration happens when the framework is loaded
 * - The plugin system uses the registry instead of dynamic loading
 * - This is App Store compliant and performant
 *
 * ## Trade-offs
 *
 * **Pros:**
 * - ✅ App Store compliant
 * - ✅ Fast (no runtime loading overhead)
 * - ✅ Type-safe (compile-time checking)
 * - ✅ No reflection needed
 *
 * **Cons:**
 * - ❌ Cannot add plugins after app is built
 * - ❌ All plugins must be known at compile time
 * - ❌ Larger app bundle (includes all plugins)
 *
 * ## Future: Scripting-Based Plugins
 *
 * For true runtime extensibility on iOS, consider:
 * - JavaScript plugins via JavaScriptCore
 * - Lua plugins via embedded interpreter
 * - WebAssembly plugins (when supported)
 *
 * These would require a separate plugin type and loader implementation.
 */
