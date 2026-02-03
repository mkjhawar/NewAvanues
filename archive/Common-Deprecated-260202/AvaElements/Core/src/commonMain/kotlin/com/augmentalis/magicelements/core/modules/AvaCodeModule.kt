package com.augmentalis.magicelements.core.modules

import com.augmentalis.magicelements.core.mel.functions.PluginTier
import kotlinx.coroutines.flow.Flow

/**
 * Base interface for all AvaCode modules.
 * Modules expose platform capabilities to MEL plugins.
 *
 * Usage in MEL:
 * ```
 * @voice.listen()
 * @device.info()
 * @command.execute("open settings")
 * ```
 */
interface AvaCodeModule {
    /** Module name (e.g., "voice", "device", "command") */
    val name: String

    /** Module version */
    val version: String

    /** Minimum tier required to use this module */
    val minimumTier: PluginTier

    /**
     * Execute a module method.
     *
     * @param method Method name (e.g., "listen", "speak", "info")
     * @param args Method arguments
     * @param tier Current plugin tier
     * @return Method result
     * @throws ModuleException if method fails or tier insufficient
     */
    suspend fun execute(method: String, args: List<Any?>, tier: PluginTier): Any?

    /**
     * Check if a method is available at the given tier.
     */
    fun isMethodAvailable(method: String, tier: PluginTier): Boolean

    /**
     * List all methods available at the given tier.
     */
    fun listMethods(tier: PluginTier): List<ModuleMethod>

    /**
     * Initialize the module with platform context.
     * Called once when the module is first accessed.
     */
    suspend fun initialize()

    /**
     * Cleanup module resources.
     */
    suspend fun dispose()
}

/**
 * Module method descriptor.
 */
data class ModuleMethod(
    val name: String,
    val description: String,
    val parameters: List<MethodParameter>,
    val returnType: String,
    val tier: PluginTier,
    val isAsync: Boolean = false
)

/**
 * Method parameter descriptor.
 */
data class MethodParameter(
    val name: String,
    val type: String,
    val required: Boolean = true,
    val defaultValue: Any? = null,
    val description: String = ""
)

/**
 * Module execution exception.
 */
open class ModuleException(
    val module: String,
    val method: String,
    message: String,
    cause: Throwable? = null
) : Exception("[$module.$method] $message", cause)

/**
 * Thrown when method requires higher tier.
 */
class ModuleTierException(
    module: String,
    method: String,
    requiredTier: PluginTier,
    currentTier: PluginTier
) : ModuleException(
    module, method,
    "Method requires $requiredTier but plugin is $currentTier"
)

/**
 * Thrown when method not found.
 */
class ModuleMethodNotFoundException(
    module: String,
    method: String
) : ModuleException(module, method, "Method not found")

/**
 * Thrown when arguments are invalid.
 */
class ModuleArgumentException(
    module: String,
    method: String,
    message: String
) : ModuleException(module, method, message)
