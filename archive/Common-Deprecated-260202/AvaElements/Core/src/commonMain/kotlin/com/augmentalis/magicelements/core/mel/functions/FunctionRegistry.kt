package com.augmentalis.magicelements.core.mel.functions

/**
 * Central registry for MEL built-in functions with tier enforcement.
 *
 * Tier 1 (DATA): Apple-safe whitelist only
 * Tier 2 (LOGIC): All functions including extended APIs
 */
object FunctionRegistry {
    private val tier1Functions = mutableMapOf<String, MELFunction>()
    private val tier2Functions = mutableMapOf<String, MELFunction>()

    /**
     * Register a function with tier specification.
     *
     * @param category Function category (math, string, array, etc.)
     * @param name Function name within category
     * @param tier Minimum tier required to use this function
     * @param fn The function implementation
     */
    fun register(category: String, name: String, tier: PluginTier, fn: MELFunction) {
        val key = "$category.$name"
        when (tier) {
            PluginTier.DATA -> {
                tier1Functions[key] = fn
                tier2Functions[key] = fn  // Tier 2 includes all Tier 1
            }
            PluginTier.LOGIC -> {
                tier2Functions[key] = fn
            }
        }
    }

    /**
     * Execute a function with tier enforcement.
     *
     * @param category Function category
     * @param name Function name
     * @param args Arguments to pass to function
     * @param tier Current plugin tier
     * @return Function result
     * @throws SecurityException if function not allowed in current tier
     * @throws IllegalArgumentException if function not found
     */
    fun execute(category: String, name: String, args: List<Any>, tier: PluginTier): Any {
        val key = "$category.$name"

        val registry = when (tier) {
            PluginTier.DATA -> tier1Functions
            PluginTier.LOGIC -> tier2Functions
        }

        val function = registry[key]
            ?: throw if (key in tier2Functions) {
                SecurityException("Function $key requires Tier 2 (LOGIC) but plugin is Tier 1 (DATA)")
            } else {
                IllegalArgumentException("Unknown function: $key")
            }

        return function.invoke(args)
    }

    /**
     * Check if a function is available in the given tier.
     */
    fun isAvailable(category: String, name: String, tier: PluginTier): Boolean {
        val key = "$category.$name"
        return when (tier) {
            PluginTier.DATA -> key in tier1Functions
            PluginTier.LOGIC -> key in tier2Functions
        }
    }

    /**
     * Get all available functions for a tier.
     */
    fun listFunctions(tier: PluginTier): List<String> {
        return when (tier) {
            PluginTier.DATA -> tier1Functions.keys.toList()
            PluginTier.LOGIC -> tier2Functions.keys.toList()
        }.sorted()
    }

    /**
     * Initialize all built-in functions.
     * Called automatically on first access.
     */
    init {
        // Register all Tier 1 functions
        MathFunctions.register()
        StringFunctions.register()
        ArrayFunctions.register()
        LogicFunctions.register()
        ObjectFunctions.register()
        DateFunctions.register()
    }
}

/**
 * Function interface for MEL built-in functions.
 */
fun interface MELFunction {
    /**
     * Invoke the function with given arguments.
     *
     * @param args Function arguments
     * @return Function result
     * @throws IllegalArgumentException for invalid arguments
     */
    fun invoke(args: List<Any>): Any
}

/**
 * Plugin tier levels.
 */
enum class PluginTier {
    /** Tier 1: Data Mode - Apple-safe declarative templates */
    DATA,

    /** Tier 2: Logic Mode - Full expression engine (non-Apple only) */
    LOGIC
}

/**
 * Base exception for MEL function errors.
 */
open class MELFunctionException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thrown when wrong number of arguments provided.
 */
class MELArgumentCountException(
    functionName: String,
    expected: Int,
    actual: Int
) : MELFunctionException("Function $functionName expects $expected arguments, got $actual")

/**
 * Thrown when argument has wrong type.
 */
class MELArgumentTypeException(
    functionName: String,
    argIndex: Int,
    expectedType: String,
    actualValue: Any?
) : MELFunctionException(
    "Function $functionName argument $argIndex expects $expectedType, got ${actualValue?.let { it::class.simpleName } ?: "null"}"
)

/**
 * Type coercion utilities for MEL functions.
 */
object TypeCoercion {
    /**
     * Coerce value to Number (Int or Double).
     * Supports: Number, String (parseable), Boolean (true=1, false=0)
     */
    fun toNumber(value: Any): Number {
        return when (value) {
            is Number -> value
            is String -> value.toDoubleOrNull() ?: value.toIntOrNull()
                ?: throw IllegalArgumentException("Cannot convert '$value' to number")
            is Boolean -> if (value) 1 else 0
            else -> throw IllegalArgumentException("Cannot convert ${value::class.simpleName} to number")
        }
    }

    /**
     * Coerce value to String.
     */
    fun toString(value: Any): String {
        return value.toString()
    }

    /**
     * Coerce value to Boolean.
     * Falsy: false, 0, "", null
     * Truthy: everything else
     */
    fun toBoolean(value: Any?): Boolean {
        return when (value) {
            null -> false
            is Boolean -> value
            is Number -> value.toDouble() != 0.0
            is String -> value.isNotEmpty()
            is Collection<*> -> value.isNotEmpty()
            is Map<*, *> -> value.isNotEmpty()
            else -> true
        }
    }

    /**
     * Coerce value to List.
     */
    @Suppress("UNCHECKED_CAST")
    fun toList(value: Any): List<Any> {
        return when (value) {
            is List<*> -> value as List<Any>
            is Array<*> -> value.toList() as List<Any>
            is Collection<*> -> value.toList() as List<Any>
            else -> throw IllegalArgumentException("Cannot convert ${value::class.simpleName} to list")
        }
    }

    /**
     * Coerce value to Map.
     */
    @Suppress("UNCHECKED_CAST")
    fun toMap(value: Any): Map<String, Any> {
        return when (value) {
            is Map<*, *> -> value as Map<String, Any>
            else -> throw IllegalArgumentException("Cannot convert ${value::class.simpleName} to map")
        }
    }
}
