package com.augmentalis.magicelements.core.mel.functions

/**
 * Logic functions for MEL (Tier 1 - Apple-safe).
 *
 * Provides conditional logic and comparison operations.
 */
object LogicFunctions {
    fun register() {
        val tier = PluginTier.DATA

        // Conditional
        FunctionRegistry.register("logic", "if", tier) { args ->
            requireArgs("logic.if", args, 3)
            val condition = TypeCoercion.toBoolean(args[0])
            if (condition) args[1] else args[2]
        }

        // Boolean operations
        FunctionRegistry.register("logic", "and", tier) { args ->
            requireMinArgs("logic.and", args, 2)
            args.all { TypeCoercion.toBoolean(it) }
        }

        FunctionRegistry.register("logic", "or", tier) { args ->
            requireMinArgs("logic.or", args, 2)
            args.any { TypeCoercion.toBoolean(it) }
        }

        FunctionRegistry.register("logic", "not", tier) { args ->
            requireArgs("logic.not", args, 1)
            !TypeCoercion.toBoolean(args[0])
        }

        FunctionRegistry.register("logic", "xor", tier) { args ->
            requireArgs("logic.xor", args, 2)
            val a = TypeCoercion.toBoolean(args[0])
            val b = TypeCoercion.toBoolean(args[1])
            a xor b
        }

        // Equality
        FunctionRegistry.register("logic", "equals", tier) { args ->
            requireArgs("logic.equals", args, 2)
            val a = args[0]
            val b = args[1]
            compareValues(a, b) == 0
        }

        FunctionRegistry.register("logic", "notEquals", tier) { args ->
            requireArgs("logic.notEquals", args, 2)
            val a = args[0]
            val b = args[1]
            compareValues(a, b) != 0
        }

        // Comparisons (numeric)
        FunctionRegistry.register("logic", "gt", tier) { args ->
            requireArgs("logic.gt", args, 2)
            val a = TypeCoercion.toNumber(args[0]).toDouble()
            val b = TypeCoercion.toNumber(args[1]).toDouble()
            a > b
        }

        FunctionRegistry.register("logic", "gte", tier) { args ->
            requireArgs("logic.gte", args, 2)
            val a = TypeCoercion.toNumber(args[0]).toDouble()
            val b = TypeCoercion.toNumber(args[1]).toDouble()
            a >= b
        }

        FunctionRegistry.register("logic", "lt", tier) { args ->
            requireArgs("logic.lt", args, 2)
            val a = TypeCoercion.toNumber(args[0]).toDouble()
            val b = TypeCoercion.toNumber(args[1]).toDouble()
            a < b
        }

        FunctionRegistry.register("logic", "lte", tier) { args ->
            requireArgs("logic.lte", args, 2)
            val a = TypeCoercion.toNumber(args[0]).toDouble()
            val b = TypeCoercion.toNumber(args[1]).toDouble()
            a <= b
        }

        // Type checking
        FunctionRegistry.register("logic", "isNull", tier) { args ->
            requireArgs("logic.isNull", args, 1)
            args[0] == null
        }

        FunctionRegistry.register("logic", "isNotNull", tier) { args ->
            requireArgs("logic.isNotNull", args, 1)
            args[0] != null
        }

        FunctionRegistry.register("logic", "isNumber", tier) { args ->
            requireArgs("logic.isNumber", args, 1)
            args[0] is Number
        }

        FunctionRegistry.register("logic", "isString", tier) { args ->
            requireArgs("logic.isString", args, 1)
            args[0] is String
        }

        FunctionRegistry.register("logic", "isBoolean", tier) { args ->
            requireArgs("logic.isBoolean", args, 1)
            args[0] is Boolean
        }

        FunctionRegistry.register("logic", "isList", tier) { args ->
            requireArgs("logic.isList", args, 1)
            args[0] is List<*>
        }

        FunctionRegistry.register("logic", "isMap", tier) { args ->
            requireArgs("logic.isMap", args, 1)
            args[0] is Map<*, *>
        }

        // Range checking
        FunctionRegistry.register("logic", "between", tier) { args ->
            requireArgs("logic.between", args, 3)
            val value = TypeCoercion.toNumber(args[0]).toDouble()
            val min = TypeCoercion.toNumber(args[1]).toDouble()
            val max = TypeCoercion.toNumber(args[2]).toDouble()
            value in min..max
        }

        FunctionRegistry.register("logic", "inList", tier) { args ->
            requireArgs("logic.inList", args, 2)
            val value = args[0]
            val list = TypeCoercion.toList(args[1])
            list.contains(value)
        }

        // Coalesce (return first non-null value)
        FunctionRegistry.register("logic", "coalesce", tier) { args ->
            requireMinArgs("logic.coalesce", args, 2)
            args.firstOrNull { it != null } ?: args.last()
        }

        // Default value
        FunctionRegistry.register("logic", "default", tier) { args ->
            requireArgs("logic.default", args, 2)
            val value = args[0]
            val default = args[1]
            if (value == null || (value is String && value.isEmpty())) {
                default
            } else {
                value
            }
        }
    }

    /**
     * Compare two values with type coercion.
     * Returns: -1 if a < b, 0 if a == b, 1 if a > b
     */
    private fun compareValues(a: Any?, b: Any?): Int {
        // Handle nulls
        if (a == null && b == null) return 0
        if (a == null) return -1
        if (b == null) return 1

        // Direct equality check
        if (a == b) return 0

        // Try numeric comparison
        if (a is Number && b is Number) {
            val aDouble = a.toDouble()
            val bDouble = b.toDouble()
            return aDouble.compareTo(bDouble)
        }

        // Try string comparison
        if (a is String && b is String) {
            return a.compareTo(b)
        }

        // Try boolean comparison
        if (a is Boolean && b is Boolean) {
            return a.compareTo(b)
        }

        // Try comparing as strings
        val aStr = TypeCoercion.toString(a)
        val bStr = TypeCoercion.toString(b)
        return aStr.compareTo(bStr)
    }

    private fun requireArgs(name: String, args: List<Any>, expected: Int) {
        if (args.size != expected) {
            throw MELArgumentCountException(name, expected, args.size)
        }
    }

    private fun requireMinArgs(name: String, args: List<Any>, min: Int) {
        if (args.size < min) {
            throw MELFunctionException("$name requires at least $min arguments, got ${args.size}")
        }
    }
}
