package com.augmentalis.magicelements.core.mel

/**
 * Example usage of the MagicUI Expression Language (MEL) parser.
 *
 * Demonstrates:
 * - Parsing simple expressions
 * - Evaluating state references
 * - Evaluating function calls
 * - Evaluating binary operations
 * - Tier enforcement
 */
object MELExample {

    /**
     * Example: Parse and evaluate a simple arithmetic expression.
     */
    fun example1() {
        val expression = "1 + 2 * 3"
        val lexer = ExpressionLexer(expression)
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()

        val evaluator = ExpressionEvaluator(
            state = emptyMap(),
            params = emptyMap(),
            tier = PluginTier.DATA
        )

        val result = evaluator.evaluate(ast)
        println("$expression = $result") // Expected: 7.0
    }

    /**
     * Example: Parse and evaluate a state reference.
     */
    fun example2() {
        val expression = "\$state.count + 1"
        val lexer = ExpressionLexer(expression)
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()

        val state = mapOf("count" to 5)
        val evaluator = ExpressionEvaluator(
            state = state,
            params = emptyMap(),
            tier = PluginTier.DATA
        )

        val result = evaluator.evaluate(ast)
        println("$expression with count=5 = $result") // Expected: 6.0
    }

    /**
     * Example: Parse and evaluate a function call.
     */
    fun example3() {
        val expression = "\$math.add(\$state.count, 10)"
        val lexer = ExpressionLexer(expression)
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()

        val state = mapOf("count" to 5)
        val evaluator = ExpressionEvaluator(
            state = state,
            params = emptyMap(),
            tier = PluginTier.DATA
        )

        val result = evaluator.evaluate(ast)
        println("$expression with count=5 = $result") // Expected: 15.0
    }

    /**
     * Example: Parse and evaluate a complex expression.
     */
    fun example4() {
        val expression = "\$logic.if(\$state.count > 5, \"High\", \"Low\")"
        val lexer = ExpressionLexer(expression)
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()

        val state = mapOf("count" to 10)
        val evaluator = ExpressionEvaluator(
            state = state,
            params = emptyMap(),
            tier = PluginTier.DATA
        )

        val result = evaluator.evaluate(ast)
        println("$expression with count=10 = $result") // Expected: "High"
    }

    /**
     * Example: Nested state references.
     */
    fun example5() {
        val expression = "\$state.user.name"
        val lexer = ExpressionLexer(expression)
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()

        val state = mapOf(
            "user" to mapOf(
                "name" to "Alice",
                "age" to 30
            )
        )

        val evaluator = ExpressionEvaluator(
            state = state,
            params = emptyMap(),
            tier = PluginTier.DATA
        )

        val result = evaluator.evaluate(ast)
        println("$expression = $result") // Expected: "Alice"
    }

    /**
     * Example: Parameter references in reducers.
     */
    fun example6() {
        val expression = "\$math.add(\$state.count, \$increment)"
        val lexer = ExpressionLexer(expression)
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()

        val state = mapOf("count" to 5)
        val params = mapOf("increment" to 3)

        val evaluator = ExpressionEvaluator(
            state = state,
            params = params,
            tier = PluginTier.DATA
        )

        val result = evaluator.evaluate(ast)
        println("$expression with count=5, increment=3 = $result") // Expected: 8.0
    }

    /**
     * Example: Array literal.
     */
    fun example7() {
        val expression = "[1, 2, 3, 4, 5]"
        val lexer = ExpressionLexer(expression)
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()

        val evaluator = ExpressionEvaluator(
            state = emptyMap(),
            params = emptyMap(),
            tier = PluginTier.DATA
        )

        val result = evaluator.evaluate(ast)
        println("$expression = $result") // Expected: [1.0, 2.0, 3.0, 4.0, 5.0]
    }

    /**
     * Example: Object literal.
     */
    fun example8() {
        val expression = "{ x: 10, y: 20 }"
        val lexer = ExpressionLexer(expression)
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()

        val evaluator = ExpressionEvaluator(
            state = emptyMap(),
            params = emptyMap(),
            tier = PluginTier.DATA
        )

        val result = evaluator.evaluate(ast)
        println("$expression = $result") // Expected: {x=10.0, y=20.0}
    }

    /**
     * Example: Logical operations.
     */
    fun example9() {
        val expression = "\$state.enabled && \$state.count > 0"
        val lexer = ExpressionLexer(expression)
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()

        val state = mapOf(
            "enabled" to true,
            "count" to 5
        )

        val evaluator = ExpressionEvaluator(
            state = state,
            params = emptyMap(),
            tier = PluginTier.DATA
        )

        val result = evaluator.evaluate(ast)
        println("$expression with enabled=true, count=5 = $result") // Expected: true
    }

    /**
     * Example: String concatenation.
     */
    fun example10() {
        val expression = "\$string.concat(\"Hello, \", \$state.name, \"!\")"
        val lexer = ExpressionLexer(expression)
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()

        val state = mapOf("name" to "World")

        val evaluator = ExpressionEvaluator(
            state = state,
            params = emptyMap(),
            tier = PluginTier.DATA
        )

        val result = evaluator.evaluate(ast)
        println("$expression with name=World = $result") // Expected: "Hello, World!"
    }
}
