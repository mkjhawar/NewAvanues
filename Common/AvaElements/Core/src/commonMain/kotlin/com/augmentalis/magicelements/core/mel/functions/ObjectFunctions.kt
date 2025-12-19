package com.augmentalis.magicelements.core.mel.functions

/**
 * Object/Map functions for MEL (Tier 1 - Apple-safe).
 *
 * All functions work with Map<String, Any>.
 */
object ObjectFunctions {
    fun register() {
        val tier = PluginTier.DATA

        // Access
        FunctionRegistry.register("object", "get", tier) { args ->
            requireArgs("object.get", args, 2)
            val obj = TypeCoercion.toMap(args[0])
            val key = TypeCoercion.toString(args[1])
            obj[key]
        }

        FunctionRegistry.register("object", "has", tier) { args ->
            requireArgs("object.has", args, 2)
            val obj = TypeCoercion.toMap(args[0])
            val key = TypeCoercion.toString(args[1])
            obj.containsKey(key)
        }

        // Modification (returns new map - immutable)
        FunctionRegistry.register("object", "set", tier) { args ->
            requireArgs("object.set", args, 3)
            val obj = TypeCoercion.toMap(args[0]).toMutableMap()
            val key = TypeCoercion.toString(args[1])
            val value = args[2]
            obj[key] = value
            obj
        }

        FunctionRegistry.register("object", "remove", tier) { args ->
            requireArgs("object.remove", args, 2)
            val obj = TypeCoercion.toMap(args[0]).toMutableMap()
            val key = TypeCoercion.toString(args[1])
            obj.remove(key)
            obj
        }

        // Keys and Values
        FunctionRegistry.register("object", "keys", tier) { args ->
            requireArgs("object.keys", args, 1)
            val obj = TypeCoercion.toMap(args[0])
            obj.keys.toList()
        }

        FunctionRegistry.register("object", "values", tier) { args ->
            requireArgs("object.values", args, 1)
            val obj = TypeCoercion.toMap(args[0])
            obj.values.toList()
        }

        FunctionRegistry.register("object", "entries", tier) { args ->
            requireArgs("object.entries", args, 1)
            val obj = TypeCoercion.toMap(args[0])
            obj.entries.map { (k, v) -> mapOf("key" to k, "value" to v) }
        }

        // Merge (shallow merge)
        FunctionRegistry.register("object", "merge", tier) { args ->
            requireMinArgs("object.merge", args, 2)
            val result = mutableMapOf<String, Any>()
            for (arg in args) {
                val map = TypeCoercion.toMap(arg)
                result.putAll(map)
            }
            result
        }

        // Size
        FunctionRegistry.register("object", "size", tier) { args ->
            requireArgs("object.size", args, 1)
            val obj = TypeCoercion.toMap(args[0])
            obj.size
        }

        FunctionRegistry.register("object", "isEmpty", tier) { args ->
            requireArgs("object.isEmpty", args, 1)
            val obj = TypeCoercion.toMap(args[0])
            obj.isEmpty()
        }

        FunctionRegistry.register("object", "isNotEmpty", tier) { args ->
            requireArgs("object.isNotEmpty", args, 1)
            val obj = TypeCoercion.toMap(args[0])
            obj.isNotEmpty()
        }

        // Path access (get nested values with dot notation)
        // Example: getPath(obj, "user.profile.name")
        FunctionRegistry.register("object", "getPath", tier) { args ->
            requireArgs("object.getPath", args, 2)
            var current: Any? = args[0]
            val path = TypeCoercion.toString(args[1])
            val keys = path.split(".")

            for (key in keys) {
                when (current) {
                    is Map<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        current = (current as Map<String, Any>)[key]
                    }
                    null -> return@register null
                    else -> throw MELFunctionException("Cannot access property '$key' on non-object type ${current::class.simpleName}")
                }
            }

            current
        }

        // Set path (set nested values with dot notation)
        // Example: setPath(obj, "user.profile.name", "John")
        FunctionRegistry.register("object", "setPath", tier) { args ->
            requireArgs("object.setPath", args, 3)
            val obj = TypeCoercion.toMap(args[0])
            val path = TypeCoercion.toString(args[1])
            val value = args[2]
            val keys = path.split(".")

            // Deep copy the object
            val result = deepCopyMap(obj)

            // Navigate to the parent of the target key
            var current: MutableMap<String, Any> = result
            for (i in 0 until keys.size - 1) {
                val key = keys[i]
                val next = current[key]
                if (next is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    val nextMap = (next as Map<String, Any>).toMutableMap()
                    current[key] = nextMap
                    current = nextMap
                } else {
                    // Create intermediate objects if they don't exist
                    val newMap = mutableMapOf<String, Any>()
                    current[key] = newMap
                    current = newMap
                }
            }

            // Set the final value
            current[keys.last()] = value

            result
        }

        // Pick (select specific keys)
        FunctionRegistry.register("object", "pick", tier) { args ->
            requireMinArgs("object.pick", args, 2)
            val obj = TypeCoercion.toMap(args[0])
            val result = mutableMapOf<String, Any>()

            for (i in 1 until args.size) {
                val key = TypeCoercion.toString(args[i])
                if (obj.containsKey(key)) {
                    result[key] = obj[key]!!
                }
            }

            result
        }

        // Omit (exclude specific keys)
        FunctionRegistry.register("object", "omit", tier) { args ->
            requireMinArgs("object.omit", args, 2)
            val obj = TypeCoercion.toMap(args[0]).toMutableMap()

            for (i in 1 until args.size) {
                val key = TypeCoercion.toString(args[i])
                obj.remove(key)
            }

            obj
        }
    }

    /**
     * Deep copy a map (one level deep - sufficient for most cases).
     */
    @Suppress("UNCHECKED_CAST")
    private fun deepCopyMap(map: Map<String, Any>): MutableMap<String, Any> {
        val result = mutableMapOf<String, Any>()
        for ((key, value) in map) {
            result[key] = when (value) {
                is Map<*, *> -> deepCopyMap(value as Map<String, Any>)
                is List<*> -> (value as List<Any>).toMutableList()
                else -> value
            }
        }
        return result
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
