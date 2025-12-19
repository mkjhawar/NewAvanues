package com.augmentalis.magicelements.core.modules

import com.augmentalis.magicelements.core.mel.functions.PluginTier

/**
 * Base implementation for AvaCode modules.
 * Provides method registration and tier enforcement.
 *
 * Example:
 * ```kotlin
 * class VoiceModule : BaseModule("voice", "1.0.0", PluginTier.DATA) {
 *     init {
 *         registerMethod("listen", PluginTier.DATA, "Start listening for speech") { args, tier ->
 *             // Implementation
 *         }
 *         registerMethod("wake", PluginTier.LOGIC, "Enable wake word detection") { args, tier ->
 *             // Implementation
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseModule(
    override val name: String,
    override val version: String,
    override val minimumTier: PluginTier
) : AvaCodeModule {

    /**
     * Method handler type.
     */
    fun interface MethodHandler {
        suspend fun invoke(args: List<Any?>, tier: PluginTier): Any?
    }

    /**
     * Registered methods.
     */
    private val methods = mutableMapOf<String, RegisteredMethod>()

    /**
     * Internal method registration.
     */
    private data class RegisteredMethod(
        val info: ModuleMethod,
        val handler: MethodHandler
    )

    /**
     * Register a method.
     *
     * @param name Method name
     * @param tier Minimum tier required
     * @param description Method description
     * @param returnType Return type description
     * @param parameters Parameter descriptors
     * @param isAsync Whether method is async (streaming)
     * @param handler Method implementation
     */
    protected fun registerMethod(
        name: String,
        tier: PluginTier,
        description: String,
        returnType: String = "Any",
        parameters: List<MethodParameter> = emptyList(),
        isAsync: Boolean = false,
        handler: MethodHandler
    ) {
        methods[name] = RegisteredMethod(
            info = ModuleMethod(
                name = name,
                description = description,
                parameters = parameters,
                returnType = returnType,
                tier = tier,
                isAsync = isAsync
            ),
            handler = handler
        )
    }

    /**
     * Simplified registration for common cases.
     */
    protected fun method(
        name: String,
        tier: PluginTier = PluginTier.DATA,
        description: String = "",
        handler: MethodHandler
    ) {
        registerMethod(name, tier, description, handler = handler)
    }

    override suspend fun execute(method: String, args: List<Any?>, tier: PluginTier): Any? {
        val registered = methods[method]
            ?: throw ModuleMethodNotFoundException(name, method)

        // Check tier
        if (registered.info.tier == PluginTier.LOGIC && tier == PluginTier.DATA) {
            throw ModuleTierException(name, method, PluginTier.LOGIC, tier)
        }

        return try {
            registered.handler.invoke(args, tier)
        } catch (e: ModuleException) {
            throw e
        } catch (e: Exception) {
            throw ModuleException(name, method, e.message ?: "Unknown error", e)
        }
    }

    override fun isMethodAvailable(method: String, tier: PluginTier): Boolean {
        val registered = methods[method] ?: return false
        return when {
            registered.info.tier == PluginTier.DATA -> true
            registered.info.tier == PluginTier.LOGIC && tier == PluginTier.LOGIC -> true
            else -> false
        }
    }

    override fun listMethods(tier: PluginTier): List<ModuleMethod> {
        return methods.values
            .filter { isMethodAvailable(it.info.name, tier) }
            .map { it.info }
    }

    override suspend fun initialize() {
        // Override in subclasses if needed
    }

    override suspend fun dispose() {
        // Override in subclasses if needed
    }

    // ========== Argument Helpers ==========

    /**
     * Get required argument at index.
     */
    protected inline fun <reified T> List<Any?>.arg(index: Int, name: String): T {
        if (index >= size) {
            throw ModuleArgumentException(this@BaseModule.name, "", "Missing required argument: $name")
        }
        val value = this[index]
        if (value !is T) {
            throw ModuleArgumentException(
                this@BaseModule.name, "",
                "Argument '$name' expected ${T::class.simpleName}, got ${value?.let { it::class.simpleName } ?: "null"}"
            )
        }
        return value
    }

    /**
     * Get optional argument at index with default.
     */
    protected inline fun <reified T> List<Any?>.argOrDefault(index: Int, default: T): T {
        if (index >= size) return default
        val value = this[index] ?: return default
        return if (value is T) value else default
    }

    /**
     * Get optional argument at index.
     */
    protected inline fun <reified T> List<Any?>.argOrNull(index: Int): T? {
        if (index >= size) return null
        val value = this[index] ?: return null
        return value as? T
    }

    /**
     * Get argument as String, with coercion.
     */
    protected fun List<Any?>.argString(index: Int, name: String): String {
        if (index >= size) {
            throw ModuleArgumentException(this@BaseModule.name, "", "Missing required argument: $name")
        }
        return this[index]?.toString()
            ?: throw ModuleArgumentException(this@BaseModule.name, "", "Argument '$name' cannot be null")
    }

    /**
     * Get argument as Number.
     */
    protected fun List<Any?>.argNumber(index: Int, name: String): Number {
        if (index >= size) {
            throw ModuleArgumentException(this@BaseModule.name, "", "Missing required argument: $name")
        }
        return when (val value = this[index]) {
            is Number -> value
            is String -> value.toDoubleOrNull()
                ?: throw ModuleArgumentException(this@BaseModule.name, "", "Cannot convert '$value' to number")
            else -> throw ModuleArgumentException(
                this@BaseModule.name, "",
                "Argument '$name' expected Number, got ${value?.let { it::class.simpleName } ?: "null"}"
            )
        }
    }

    /**
     * Get argument as Boolean.
     */
    protected fun List<Any?>.argBoolean(index: Int, name: String): Boolean {
        if (index >= size) {
            throw ModuleArgumentException(this@BaseModule.name, "", "Missing required argument: $name")
        }
        return when (val value = this[index]) {
            is Boolean -> value
            is String -> value.lowercase() in listOf("true", "1", "yes")
            is Number -> value.toDouble() != 0.0
            else -> throw ModuleArgumentException(
                this@BaseModule.name, "",
                "Argument '$name' expected Boolean, got ${value?.let { it::class.simpleName } ?: "null"}"
            )
        }
    }

    /**
     * Get argument as Map (options object).
     */
    @Suppress("UNCHECKED_CAST")
    protected fun List<Any?>.argOptions(index: Int): Map<String, Any?> {
        if (index >= size) return emptyMap()
        return this[index] as? Map<String, Any?> ?: emptyMap()
    }
}
