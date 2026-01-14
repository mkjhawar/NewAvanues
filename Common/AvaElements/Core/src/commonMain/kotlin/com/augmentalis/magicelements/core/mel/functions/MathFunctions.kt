package com.augmentalis.magicelements.core.mel.functions

import kotlin.math.*

/**
 * Math functions for MEL (Tier 1 - Apple-safe).
 *
 * All functions support type coercion from String/Boolean to Number.
 */
object MathFunctions {
    fun register() {
        val tier = PluginTier.DATA

        // Basic arithmetic
        FunctionRegistry.register("math", "add", tier) { args ->
            requireArgs("math.add", args, 2)
            val a = TypeCoercion.toNumber(args[0]).toDouble()
            val b = TypeCoercion.toNumber(args[1]).toDouble()
            a + b
        }

        FunctionRegistry.register("math", "subtract", tier) { args ->
            requireArgs("math.subtract", args, 2)
            val a = TypeCoercion.toNumber(args[0]).toDouble()
            val b = TypeCoercion.toNumber(args[1]).toDouble()
            a - b
        }

        FunctionRegistry.register("math", "multiply", tier) { args ->
            requireArgs("math.multiply", args, 2)
            val a = TypeCoercion.toNumber(args[0]).toDouble()
            val b = TypeCoercion.toNumber(args[1]).toDouble()
            a * b
        }

        FunctionRegistry.register("math", "divide", tier) { args ->
            requireArgs("math.divide", args, 2)
            val a = TypeCoercion.toNumber(args[0]).toDouble()
            val b = TypeCoercion.toNumber(args[1]).toDouble()
            if (b == 0.0) throw ArithmeticException("Division by zero")
            a / b
        }

        FunctionRegistry.register("math", "mod", tier) { args ->
            requireArgs("math.mod", args, 2)
            val a = TypeCoercion.toNumber(args[0]).toDouble()
            val b = TypeCoercion.toNumber(args[1]).toDouble()
            if (b == 0.0) throw ArithmeticException("Modulo by zero")
            a % b
        }

        // Rounding functions
        FunctionRegistry.register("math", "abs", tier) { args ->
            requireArgs("math.abs", args, 1)
            val n = TypeCoercion.toNumber(args[0]).toDouble()
            abs(n)
        }

        FunctionRegistry.register("math", "round", tier) { args ->
            requireArgs("math.round", args, 1)
            val n = TypeCoercion.toNumber(args[0]).toDouble()
            round(n)
        }

        FunctionRegistry.register("math", "floor", tier) { args ->
            requireArgs("math.floor", args, 1)
            val n = TypeCoercion.toNumber(args[0]).toDouble()
            floor(n)
        }

        FunctionRegistry.register("math", "ceil", tier) { args ->
            requireArgs("math.ceil", args, 1)
            val n = TypeCoercion.toNumber(args[0]).toDouble()
            ceil(n)
        }

        // Min/Max
        FunctionRegistry.register("math", "min", tier) { args ->
            requireMinArgs("math.min", args, 2)
            args.map { TypeCoercion.toNumber(it).toDouble() }.minOrNull() ?: 0.0
        }

        FunctionRegistry.register("math", "max", tier) { args ->
            requireMinArgs("math.max", args, 2)
            args.map { TypeCoercion.toNumber(it).toDouble() }.maxOrNull() ?: 0.0
        }

        // Power and roots
        FunctionRegistry.register("math", "pow", tier) { args ->
            requireArgs("math.pow", args, 2)
            val base = TypeCoercion.toNumber(args[0]).toDouble()
            val exponent = TypeCoercion.toNumber(args[1]).toDouble()
            base.pow(exponent)
        }

        FunctionRegistry.register("math", "sqrt", tier) { args ->
            requireArgs("math.sqrt", args, 1)
            val n = TypeCoercion.toNumber(args[0]).toDouble()
            if (n < 0) throw ArithmeticException("Cannot take square root of negative number")
            sqrt(n)
        }

        // Special: Calculator eval function
        // Evaluates: value1 operator value2
        // Example: eval(5, "+", 3) = 8
        FunctionRegistry.register("math", "eval", tier) { args ->
            requireArgs("math.eval", args, 3)
            val a = TypeCoercion.toNumber(args[0]).toDouble()
            val op = args[1].toString()
            val b = TypeCoercion.toNumber(args[2]).toDouble()

            when (op) {
                "+", "add" -> a + b
                "-", "subtract" -> a - b
                "*", "×", "multiply" -> a * b
                "/", "÷", "divide" -> {
                    if (b == 0.0) throw ArithmeticException("Division by zero")
                    a / b
                }
                "%", "mod" -> {
                    if (b == 0.0) throw ArithmeticException("Modulo by zero")
                    a % b
                }
                "^", "pow" -> a.pow(b)
                else -> throw IllegalArgumentException("Unknown operator: $op")
            }
        }

        // Trigonometric functions (bonus - useful for games/graphics)
        FunctionRegistry.register("math", "sin", tier) { args ->
            requireArgs("math.sin", args, 1)
            val n = TypeCoercion.toNumber(args[0]).toDouble()
            sin(n)
        }

        FunctionRegistry.register("math", "cos", tier) { args ->
            requireArgs("math.cos", args, 1)
            val n = TypeCoercion.toNumber(args[0]).toDouble()
            cos(n)
        }

        FunctionRegistry.register("math", "tan", tier) { args ->
            requireArgs("math.tan", args, 1)
            val n = TypeCoercion.toNumber(args[0]).toDouble()
            tan(n)
        }

        // Constants
        FunctionRegistry.register("math", "pi", tier) { args ->
            requireArgs("math.pi", args, 0)
            PI
        }

        FunctionRegistry.register("math", "e", tier) { args ->
            requireArgs("math.e", args, 0)
            E
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
}
