package com.augmentalis.magicelements.core.mel.functions

/**
 * String functions for MEL (Tier 1 - Apple-safe).
 *
 * All functions accept any type and coerce to String.
 */
object StringFunctions {
    fun register() {
        val tier = PluginTier.DATA

        // Concatenation
        FunctionRegistry.register("string", "concat", tier) { args ->
            args.joinToString("") { TypeCoercion.toString(it) }
        }

        // Length
        FunctionRegistry.register("string", "length", tier) { args ->
            requireArgs("string.length", args, 1)
            TypeCoercion.toString(args[0]).length
        }

        // Substring
        FunctionRegistry.register("string", "substring", tier) { args ->
            when (args.size) {
                2 -> {
                    // substring(str, start)
                    val str = TypeCoercion.toString(args[0])
                    val start = TypeCoercion.toNumber(args[1]).toInt()
                    str.substring(start.coerceIn(0, str.length))
                }
                3 -> {
                    // substring(str, start, end)
                    val str = TypeCoercion.toString(args[0])
                    val start = TypeCoercion.toNumber(args[1]).toInt()
                    val end = TypeCoercion.toNumber(args[2]).toInt()
                    str.substring(
                        start.coerceIn(0, str.length),
                        end.coerceIn(0, str.length)
                    )
                }
                else -> throw MELArgumentCountException("string.substring", 2, args.size)
            }
        }

        // Case conversion
        FunctionRegistry.register("string", "uppercase", tier) { args ->
            requireArgs("string.uppercase", args, 1)
            TypeCoercion.toString(args[0]).uppercase()
        }

        FunctionRegistry.register("string", "lowercase", tier) { args ->
            requireArgs("string.lowercase", args, 1)
            TypeCoercion.toString(args[0]).lowercase()
        }

        FunctionRegistry.register("string", "capitalize", tier) { args ->
            requireArgs("string.capitalize", args, 1)
            TypeCoercion.toString(args[0]).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }

        // Trimming
        FunctionRegistry.register("string", "trim", tier) { args ->
            requireArgs("string.trim", args, 1)
            TypeCoercion.toString(args[0]).trim()
        }

        FunctionRegistry.register("string", "trimStart", tier) { args ->
            requireArgs("string.trimStart", args, 1)
            TypeCoercion.toString(args[0]).trimStart()
        }

        FunctionRegistry.register("string", "trimEnd", tier) { args ->
            requireArgs("string.trimEnd", args, 1)
            TypeCoercion.toString(args[0]).trimEnd()
        }

        // Replace
        FunctionRegistry.register("string", "replace", tier) { args ->
            requireArgs("string.replace", args, 3)
            val str = TypeCoercion.toString(args[0])
            val old = TypeCoercion.toString(args[1])
            val new = TypeCoercion.toString(args[2])
            str.replace(old, new)
        }

        FunctionRegistry.register("string", "replaceFirst", tier) { args ->
            requireArgs("string.replaceFirst", args, 3)
            val str = TypeCoercion.toString(args[0])
            val old = TypeCoercion.toString(args[1])
            val new = TypeCoercion.toString(args[2])
            str.replaceFirst(old, new)
        }

        // Split and Join
        FunctionRegistry.register("string", "split", tier) { args ->
            requireArgs("string.split", args, 2)
            val str = TypeCoercion.toString(args[0])
            val delimiter = TypeCoercion.toString(args[1])
            str.split(delimiter)
        }

        FunctionRegistry.register("string", "join", tier) { args ->
            requireArgs("string.join", args, 2)
            val list = TypeCoercion.toList(args[0])
            val separator = TypeCoercion.toString(args[1])
            list.joinToString(separator) { TypeCoercion.toString(it) }
        }

        // Predicates
        FunctionRegistry.register("string", "startsWith", tier) { args ->
            requireArgs("string.startsWith", args, 2)
            val str = TypeCoercion.toString(args[0])
            val prefix = TypeCoercion.toString(args[1])
            str.startsWith(prefix)
        }

        FunctionRegistry.register("string", "endsWith", tier) { args ->
            requireArgs("string.endsWith", args, 2)
            val str = TypeCoercion.toString(args[0])
            val suffix = TypeCoercion.toString(args[1])
            str.endsWith(suffix)
        }

        FunctionRegistry.register("string", "contains", tier) { args ->
            requireArgs("string.contains", args, 2)
            val str = TypeCoercion.toString(args[0])
            val substring = TypeCoercion.toString(args[1])
            str.contains(substring)
        }

        // Search
        FunctionRegistry.register("string", "indexOf", tier) { args ->
            requireArgs("string.indexOf", args, 2)
            val str = TypeCoercion.toString(args[0])
            val substring = TypeCoercion.toString(args[1])
            str.indexOf(substring)
        }

        FunctionRegistry.register("string", "lastIndexOf", tier) { args ->
            requireArgs("string.lastIndexOf", args, 2)
            val str = TypeCoercion.toString(args[0])
            val substring = TypeCoercion.toString(args[1])
            str.lastIndexOf(substring)
        }

        // Padding
        FunctionRegistry.register("string", "padStart", tier) { args ->
            requireArgs("string.padStart", args, 3)
            val str = TypeCoercion.toString(args[0])
            val length = TypeCoercion.toNumber(args[1]).toInt()
            val padChar = TypeCoercion.toString(args[2]).firstOrNull() ?: ' '
            str.padStart(length, padChar)
        }

        FunctionRegistry.register("string", "padEnd", tier) { args ->
            requireArgs("string.padEnd", args, 3)
            val str = TypeCoercion.toString(args[0])
            val length = TypeCoercion.toNumber(args[1]).toInt()
            val padChar = TypeCoercion.toString(args[2]).firstOrNull() ?: ' '
            str.padEnd(length, padChar)
        }

        // Repeat
        FunctionRegistry.register("string", "repeat", tier) { args ->
            requireArgs("string.repeat", args, 2)
            val str = TypeCoercion.toString(args[0])
            val count = TypeCoercion.toNumber(args[1]).toInt().coerceAtLeast(0)
            str.repeat(count)
        }

        // Validation
        FunctionRegistry.register("string", "isEmpty", tier) { args ->
            requireArgs("string.isEmpty", args, 1)
            TypeCoercion.toString(args[0]).isEmpty()
        }

        FunctionRegistry.register("string", "isBlank", tier) { args ->
            requireArgs("string.isBlank", args, 1)
            TypeCoercion.toString(args[0]).isBlank()
        }
    }

    private fun requireArgs(name: String, args: List<Any>, expected: Int) {
        if (args.size != expected) {
            throw MELArgumentCountException(name, expected, args.size)
        }
    }
}
