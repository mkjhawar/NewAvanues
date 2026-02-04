package com.augmentalis.magicelements.core.mel.functions

/**
 * Array/List functions for MEL (Tier 1 - Apple-safe).
 *
 * All functions work with List<Any> and support type coercion.
 */
object ArrayFunctions {
    fun register() {
        val tier = PluginTier.DATA

        // Length
        FunctionRegistry.register("array", "length", tier) { args ->
            requireArgs("array.length", args, 1)
            val list = TypeCoercion.toList(args[0])
            list.size
        }

        // Access
        FunctionRegistry.register("array", "get", tier) { args ->
            requireArgs("array.get", args, 2)
            val list = TypeCoercion.toList(args[0])
            val index = TypeCoercion.toNumber(args[1]).toInt()
            if (index < 0 || index >= list.size) {
                throw IndexOutOfBoundsException("Index $index out of bounds for array of size ${list.size}")
            }
            list[index]
        }

        FunctionRegistry.register("array", "first", tier) { args ->
            requireArgs("array.first", args, 1)
            val list = TypeCoercion.toList(args[0])
            if (list.isEmpty()) {
                throw NoSuchElementException("Cannot get first element of empty array")
            }
            list.first()
        }

        FunctionRegistry.register("array", "last", tier) { args ->
            requireArgs("array.last", args, 1)
            val list = TypeCoercion.toList(args[0])
            if (list.isEmpty()) {
                throw NoSuchElementException("Cannot get last element of empty array")
            }
            list.last()
        }

        // Modification (returns new list - immutable)
        FunctionRegistry.register("array", "append", tier) { args ->
            requireArgs("array.append", args, 2)
            val list = TypeCoercion.toList(args[0]).toMutableList()
            list.add(args[1])
            list
        }

        FunctionRegistry.register("array", "prepend", tier) { args ->
            requireArgs("array.prepend", args, 2)
            val list = TypeCoercion.toList(args[0]).toMutableList()
            list.add(0, args[1])
            list
        }

        FunctionRegistry.register("array", "remove", tier) { args ->
            requireArgs("array.remove", args, 2)
            val list = TypeCoercion.toList(args[0]).toMutableList()
            val value = args[1]
            list.remove(value)
            list
        }

        FunctionRegistry.register("array", "removeAt", tier) { args ->
            requireArgs("array.removeAt", args, 2)
            val list = TypeCoercion.toList(args[0]).toMutableList()
            val index = TypeCoercion.toNumber(args[1]).toInt()
            if (index < 0 || index >= list.size) {
                throw IndexOutOfBoundsException("Index $index out of bounds for array of size ${list.size}")
            }
            list.removeAt(index)
            list
        }

        // Transformation
        FunctionRegistry.register("array", "reverse", tier) { args ->
            requireArgs("array.reverse", args, 1)
            val list = TypeCoercion.toList(args[0])
            list.reversed()
        }

        FunctionRegistry.register("array", "slice", tier) { args ->
            when (args.size) {
                2 -> {
                    // slice(array, start)
                    val list = TypeCoercion.toList(args[0])
                    val start = TypeCoercion.toNumber(args[1]).toInt().coerceIn(0, list.size)
                    list.subList(start, list.size)
                }
                3 -> {
                    // slice(array, start, end)
                    val list = TypeCoercion.toList(args[0])
                    val start = TypeCoercion.toNumber(args[1]).toInt().coerceIn(0, list.size)
                    val end = TypeCoercion.toNumber(args[2]).toInt().coerceIn(0, list.size)
                    list.subList(start, end)
                }
                else -> throw MELArgumentCountException("array.slice", 2, args.size)
            }
        }

        // Filter - Filter array elements based on predicate
        // For Tier 1 (iOS), predicate must be a simple property check
        FunctionRegistry.register("array", "filter", tier) { args ->
            requireArgs("array.filter", args, 2)
            val list = TypeCoercion.toList(args[0])
            val predicate = TypeCoercion.toString(args[1])

            list.filter { item ->
                when {
                    // Property check: "done" means filter where item.done is truthy
                    !predicate.contains("(") -> {
                        val value = getNestedValue(item, predicate)
                        TypeCoercion.toBoolean(value)
                    }
                    // Expression evaluation for Tier 2
                    else -> throw MELFunctionException("Expression predicates require Tier 2")
                }
            }
        }

        // Map - Transform array elements by extracting property
        FunctionRegistry.register("array", "map", tier) { args ->
            requireArgs("array.map", args, 2)
            val list = TypeCoercion.toList(args[0])
            val mapper = TypeCoercion.toString(args[1])

            list.map { item ->
                // Simple property extraction
                if (!mapper.contains("(")) {
                    getNestedValue(item, mapper)
                } else {
                    throw MELFunctionException("Expression mappers require Tier 2")
                }
            }
        }

        // Note: filter, map are implemented with property-based predicates for Tier 1
        // Full callback/expression support requires Tier 2

        FunctionRegistry.register("array", "contains", tier) { args ->
            requireArgs("array.contains", args, 2)
            val list = TypeCoercion.toList(args[0])
            val value = args[1]
            list.contains(value)
        }

        FunctionRegistry.register("array", "indexOf", tier) { args ->
            requireArgs("array.indexOf", args, 2)
            val list = TypeCoercion.toList(args[0])
            val value = args[1]
            list.indexOf(value)
        }

        FunctionRegistry.register("array", "lastIndexOf", tier) { args ->
            requireArgs("array.lastIndexOf", args, 2)
            val list = TypeCoercion.toList(args[0])
            val value = args[1]
            list.lastIndexOf(value)
        }

        // Sort (only works with comparable types)
        FunctionRegistry.register("array", "sort", tier) { args ->
            requireArgs("array.sort", args, 1)
            val list = TypeCoercion.toList(args[0])
            try {
                @Suppress("UNCHECKED_CAST")
                (list as List<Comparable<Any>>).sorted()
            } catch (e: ClassCastException) {
                throw MELFunctionException("Cannot sort array with non-comparable elements", e)
            }
        }

        FunctionRegistry.register("array", "sortDescending", tier) { args ->
            requireArgs("array.sortDescending", args, 1)
            val list = TypeCoercion.toList(args[0])
            try {
                @Suppress("UNCHECKED_CAST")
                (list as List<Comparable<Any>>).sortedDescending()
            } catch (e: ClassCastException) {
                throw MELFunctionException("Cannot sort array with non-comparable elements", e)
            }
        }

        // Join (convert to string)
        FunctionRegistry.register("array", "join", tier) { args ->
            requireArgs("array.join", args, 2)
            val list = TypeCoercion.toList(args[0])
            val separator = TypeCoercion.toString(args[1])
            list.joinToString(separator) { TypeCoercion.toString(it) }
        }

        // Aggregation
        FunctionRegistry.register("array", "sum", tier) { args ->
            requireArgs("array.sum", args, 1)
            val list = TypeCoercion.toList(args[0])
            list.sumOf { TypeCoercion.toNumber(it).toDouble() }
        }

        FunctionRegistry.register("array", "min", tier) { args ->
            requireArgs("array.min", args, 1)
            val list = TypeCoercion.toList(args[0])
            if (list.isEmpty()) {
                throw NoSuchElementException("Cannot find min of empty array")
            }
            list.minOfOrNull { TypeCoercion.toNumber(it).toDouble() }
                ?: throw NoSuchElementException("Cannot find min of empty array")
        }

        FunctionRegistry.register("array", "max", tier) { args ->
            requireArgs("array.max", args, 1)
            val list = TypeCoercion.toList(args[0])
            if (list.isEmpty()) {
                throw NoSuchElementException("Cannot find max of empty array")
            }
            list.maxOfOrNull { TypeCoercion.toNumber(it).toDouble() }
                ?: throw NoSuchElementException("Cannot find max of empty array")
        }

        FunctionRegistry.register("array", "average", tier) { args ->
            requireArgs("array.average", args, 1)
            val list = TypeCoercion.toList(args[0])
            if (list.isEmpty()) {
                throw NoSuchElementException("Cannot find average of empty array")
            }
            list.map { TypeCoercion.toNumber(it).toDouble() }.average()
        }

        // Utility
        FunctionRegistry.register("array", "isEmpty", tier) { args ->
            requireArgs("array.isEmpty", args, 1)
            val list = TypeCoercion.toList(args[0])
            list.isEmpty()
        }

        FunctionRegistry.register("array", "isNotEmpty", tier) { args ->
            requireArgs("array.isNotEmpty", args, 1)
            val list = TypeCoercion.toList(args[0])
            list.isNotEmpty()
        }

        // Concatenation
        FunctionRegistry.register("array", "concat", tier) { args ->
            requireMinArgs("array.concat", args, 2)
            val result = mutableListOf<Any>()
            for (arg in args) {
                result.addAll(TypeCoercion.toList(arg))
            }
            result
        }

        // Flatten (single level)
        FunctionRegistry.register("array", "flatten", tier) { args ->
            requireArgs("array.flatten", args, 1)
            val list = TypeCoercion.toList(args[0])
            val result = mutableListOf<Any>()
            for (item in list) {
                if (item is List<*>) {
                    @Suppress("UNCHECKED_CAST")
                    result.addAll(item as List<Any>)
                } else {
                    result.add(item)
                }
            }
            result
        }

        // Unique (remove duplicates)
        FunctionRegistry.register("array", "unique", tier) { args ->
            requireArgs("array.unique", args, 1)
            val list = TypeCoercion.toList(args[0])
            list.distinct()
        }
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

    /**
     * Get nested value from an object using dot notation.
     * Example: getNestedValue(obj, "user.profile.name")
     *
     * @param item The object to get the value from
     * @param path The property path (dot-separated)
     * @return The value at the path, or null if not found
     */
    @Suppress("UNCHECKED_CAST")
    private fun getNestedValue(item: Any?, path: String): Any? {
        var current: Any? = item
        val keys = path.split(".")

        for (key in keys) {
            when (current) {
                is Map<*, *> -> {
                    current = (current as Map<String, Any>)[key]
                }
                null -> return null
                else -> throw MELFunctionException("Cannot access property '$key' on non-object type ${current::class.simpleName}")
            }
        }

        return current
    }
}
