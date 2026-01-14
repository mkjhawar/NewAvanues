package com.augmentalis.magicelements.core.modules

import com.augmentalis.magicelements.core.mel.functions.PluginTier
import kotlinx.coroutines.runBlocking

/**
 * Module integration for MEL expression evaluator.
 * Provides the `@module.method()` syntax support.
 *
 * Usage in expressions:
 * ```
 * @voice.listen()
 * @device.platform()
 * @command.execute("open settings")
 * @browser.open($url)
 * ```
 */
object ModuleIntegration {

    /**
     * Pattern for module calls: @module.method
     */
    private val MODULE_CALL_PATTERN = Regex("""^@(\w+)\.(.+)$""")

    /**
     * Check if an expression is a module call.
     */
    fun isModuleCall(expression: String): Boolean {
        return expression.startsWith("@") && MODULE_CALL_PATTERN.matches(expression.substringBefore("("))
    }

    /**
     * Parse a module call expression.
     *
     * @param expression Full expression like "@voice.listen()" or "@device.screen.width()"
     * @return ModuleCall with module name, method path, and arguments
     */
    fun parseModuleCall(expression: String): ModuleCall? {
        if (!expression.startsWith("@")) return null

        // Extract the call part (before parentheses) and args (inside parentheses)
        val callPart: String
        val argsString: String

        val parenIndex = expression.indexOf('(')
        if (parenIndex != -1) {
            callPart = expression.substring(1, parenIndex) // Remove @ prefix
            val endParen = expression.lastIndexOf(')')
            argsString = if (endParen > parenIndex) {
                expression.substring(parenIndex + 1, endParen)
            } else {
                ""
            }
        } else {
            callPart = expression.substring(1) // Remove @ prefix
            argsString = ""
        }

        // Split module from method (first dot separates module from method path)
        val firstDot = callPart.indexOf('.')
        if (firstDot == -1) return null

        val moduleName = callPart.substring(0, firstDot)
        val methodPath = callPart.substring(firstDot + 1)

        return ModuleCall(
            module = moduleName,
            method = methodPath,
            rawArgs = argsString
        )
    }

    /**
     * Execute a module call.
     *
     * @param call Parsed module call
     * @param args Evaluated arguments
     * @param tier Current plugin tier
     * @return Result of the module method
     */
    suspend fun execute(call: ModuleCall, args: List<Any?>, tier: PluginTier): Any? {
        return ModuleRegistry.execute(call.module, call.method, args, tier)
    }

    /**
     * Execute a module call synchronously (for non-suspend contexts).
     * Use sparingly - prefer suspend version.
     */
    fun executeBlocking(call: ModuleCall, args: List<Any?>, tier: PluginTier): Any? {
        return runBlocking {
            execute(call, args, tier)
        }
    }

    /**
     * Execute a module call from raw expression.
     */
    suspend fun executeExpression(
        expression: String,
        args: List<Any?>,
        tier: PluginTier
    ): Any? {
        val call = parseModuleCall(expression)
            ?: throw IllegalArgumentException("Invalid module call: $expression")
        return execute(call, args, tier)
    }

    /**
     * Get all available modules at a tier.
     */
    fun getAvailableModules(tier: PluginTier): List<ModuleInfo> {
        return ModuleRegistry.listModules().filter { info ->
            when {
                info.minimumTier == PluginTier.DATA -> true
                info.minimumTier == PluginTier.LOGIC && tier == PluginTier.LOGIC -> true
                else -> false
            }
        }
    }

    /**
     * Get available methods for a module at a tier.
     */
    fun getAvailableMethods(module: String, tier: PluginTier): List<ModuleMethod> {
        return ModuleRegistry.listMethods(module, tier)
    }

    /**
     * Check if a module call is valid at the given tier.
     */
    fun isValidCall(expression: String, tier: PluginTier): Boolean {
        val call = parseModuleCall(expression) ?: return false
        return ModuleRegistry.isMethodAvailable(call.module, call.method, tier)
    }
}

/**
 * Parsed module call.
 */
data class ModuleCall(
    val module: String,
    val method: String,
    val rawArgs: String
)

/**
 * Extension functions for MEL integration.
 */

/**
 * Register standard modules with default (null) delegates.
 * Platform implementations should call registerModules() with actual delegates.
 */
fun registerStubModules() {
    ModuleRegistry.register(VoiceModule(null))
    ModuleRegistry.register(DeviceModule(null))
    ModuleRegistry.register(CommandModule(null))
    ModuleRegistry.register(DataModule(null))
    ModuleRegistry.register(BrowserModule(null))
    ModuleRegistry.register(LocalizationModule(null))
    ModuleRegistry.register(AppModule(null))
}

/**
 * Module initializer interface for platform-specific setup.
 */
interface ModuleInitializer {
    /**
     * Register all platform modules with their delegates.
     */
    fun registerModules()

    /**
     * Cleanup all modules.
     */
    suspend fun cleanup()
}
