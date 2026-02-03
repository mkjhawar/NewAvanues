package com.augmentalis.magicelements.core.modules

import com.augmentalis.magicelements.core.mel.functions.PluginTier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central registry for AvaCode modules.
 * Manages module lifecycle and provides execution routing.
 *
 * Usage:
 * ```kotlin
 * // Register modules (done at app startup)
 * ModuleRegistry.register(VoiceModule(speechRecognition))
 * ModuleRegistry.register(DeviceModule(deviceManager))
 *
 * // Execute from MEL
 * val result = ModuleRegistry.execute("voice", "listen", emptyList(), PluginTier.DATA)
 * ```
 */
object ModuleRegistry {
    private val modules = mutableMapOf<String, AvaCodeModule>()
    private val initialized = mutableSetOf<String>()
    private val mutex = Mutex()

    /**
     * Register a module.
     * Replaces existing module with same name.
     */
    fun register(module: AvaCodeModule) {
        modules[module.name] = module
    }

    /**
     * Unregister a module.
     */
    suspend fun unregister(name: String) {
        mutex.withLock {
            modules[name]?.dispose()
            modules.remove(name)
            initialized.remove(name)
        }
    }

    /**
     * Get a registered module.
     */
    fun get(name: String): AvaCodeModule? = modules[name]

    /**
     * Check if a module is registered.
     */
    fun isRegistered(name: String): Boolean = name in modules

    /**
     * Execute a module method.
     *
     * @param module Module name (e.g., "voice")
     * @param method Method name (e.g., "listen")
     * @param args Method arguments
     * @param tier Current plugin tier
     * @return Method result
     */
    suspend fun execute(
        module: String,
        method: String,
        args: List<Any?>,
        tier: PluginTier
    ): Any? {
        val mod = modules[module]
            ?: throw ModuleException(module, method, "Module not registered")

        // Check tier
        if (mod.minimumTier == PluginTier.LOGIC && tier == PluginTier.DATA) {
            throw ModuleTierException(module, method, PluginTier.LOGIC, tier)
        }

        // Ensure initialized
        ensureInitialized(mod)

        // Execute
        return mod.execute(method, args, tier)
    }

    /**
     * Execute with shorthand notation: "module.method"
     */
    suspend fun execute(
        call: String,
        args: List<Any?>,
        tier: PluginTier
    ): Any? {
        val parts = call.split(".", limit = 2)
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid module call: $call. Expected format: module.method")
        }
        return execute(parts[0], parts[1], args, tier)
    }

    /**
     * List all registered modules.
     */
    fun listModules(): List<ModuleInfo> {
        return modules.values.map { mod ->
            ModuleInfo(
                name = mod.name,
                version = mod.version,
                minimumTier = mod.minimumTier,
                methodCount = mod.listMethods(PluginTier.LOGIC).size
            )
        }
    }

    /**
     * List all methods for a module at given tier.
     */
    fun listMethods(module: String, tier: PluginTier): List<ModuleMethod> {
        return modules[module]?.listMethods(tier) ?: emptyList()
    }

    /**
     * Check if a method is available.
     */
    fun isMethodAvailable(module: String, method: String, tier: PluginTier): Boolean {
        return modules[module]?.isMethodAvailable(method, tier) ?: false
    }

    /**
     * Clear all modules (for testing).
     */
    suspend fun clear() {
        mutex.withLock {
            modules.values.forEach { it.dispose() }
            modules.clear()
            initialized.clear()
        }
    }

    /**
     * Initialize module if not already done.
     */
    private suspend fun ensureInitialized(module: AvaCodeModule) {
        if (module.name !in initialized) {
            mutex.withLock {
                if (module.name !in initialized) {
                    module.initialize()
                    initialized.add(module.name)
                }
            }
        }
    }
}

/**
 * Module info for listing.
 */
data class ModuleInfo(
    val name: String,
    val version: String,
    val minimumTier: PluginTier,
    val methodCount: Int
)
