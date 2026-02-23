/**
 * Math Calculator
 *
 * Parses natural language math expressions and computes results.
 * Supports arithmetic, trigonometry, logarithms, percentages, and more.
 *
 * Created: 2025-12-03
 */

package com.augmentalis.intentactions.math

import kotlin.math.*

/**
 * Result of a mathematical calculation
 */
data class CalculationResult(
    val success: Boolean,
    val result: Double? = null,
    val formattedResult: String = "",
    val expression: String = "",
    val error: String? = null
)

/**
 * Math calculator with natural language parsing
 */
object MathCalculator {

    /**
     * Calculate from natural language input
     *
     * Examples:
     * - "12 times 10" -> 120
     * - "square root of 144" -> 12
     * - "15% of 80" -> 12
     */
    fun calculate(input: String): CalculationResult {
        val normalized = input.lowercase().trim()

        return try {
            when {
                // Basic arithmetic
                containsOperation(normalized, listOf("plus", "add", "sum of")) -> {
                    val (a, b) = extractTwoNumbers(normalized)
                    success(a + b, "$a + $b")
                }
                containsOperation(normalized, listOf("minus", "subtract", "take away", "from")) -> {
                    val (a, b) = extractTwoNumbers(normalized)
                    success(a - b, "$a - $b")
                }
                containsOperation(normalized, listOf("times", "multiply", "x", "multiplied by")) -> {
                    val (a, b) = extractTwoNumbers(normalized)
                    success(a * b, "$a * $b")
                }
                containsOperation(normalized, listOf("divided by", "divide", "over")) -> {
                    val (a, b) = extractTwoNumbers(normalized)
                    if (b == 0.0) {
                        error("Cannot divide by zero")
                    } else {
                        success(a / b, "$a / $b")
                    }
                }

                // Powers and roots
                normalized.contains("square root") || normalized.contains("sqrt") -> {
                    val num = extractSingleNumber(normalized)
                    if (num < 0) {
                        error("Cannot take square root of negative number")
                    } else {
                        success(sqrt(num), "sqrt($num)")
                    }
                }
                normalized.contains("squared") -> {
                    val num = extractSingleNumber(normalized)
                    success(num.pow(2), "$num^2")
                }
                normalized.contains("cubed") -> {
                    val num = extractSingleNumber(normalized)
                    success(num.pow(3), "$num^3")
                }
                containsOperation(normalized, listOf("to the power", "raised to", "power of")) -> {
                    val (base, exp) = extractTwoNumbers(normalized)
                    success(base.pow(exp), "$base^$exp")
                }

                // Percentages
                normalized.contains("percent of") || normalized.contains("% of") -> {
                    val (percent, num) = extractTwoNumbers(normalized)
                    success((percent / 100) * num, "$percent% of $num")
                }
                normalized.contains("half of") -> {
                    val num = extractSingleNumber(normalized)
                    success(num / 2, "0.5 * $num")
                }
                normalized.contains("double") || normalized.contains("twice") -> {
                    val num = extractSingleNumber(normalized)
                    success(num * 2, "2 * $num")
                }

                // Trigonometry (degrees)
                normalized.contains("sine") || normalized.contains("sin of") -> {
                    val degrees = extractSingleNumber(normalized)
                    success(sin(degreesToRadians(degrees)), "sin(${degrees}deg)")
                }
                normalized.contains("cosine") || normalized.contains("cos of") -> {
                    val degrees = extractSingleNumber(normalized)
                    success(cos(degreesToRadians(degrees)), "cos(${degrees}deg)")
                }
                normalized.contains("tangent") || normalized.contains("tan of") -> {
                    val degrees = extractSingleNumber(normalized)
                    success(tan(degreesToRadians(degrees)), "tan(${degrees}deg)")
                }

                // Logarithms
                normalized.contains("natural log") || normalized.contains("ln of") -> {
                    val num = extractSingleNumber(normalized)
                    if (num <= 0) {
                        error("Logarithm undefined for non-positive numbers")
                    } else {
                        success(ln(num), "ln($num)")
                    }
                }
                normalized.contains("log of") -> {
                    val num = extractSingleNumber(normalized)
                    if (num <= 0) {
                        error("Logarithm undefined for non-positive numbers")
                    } else {
                        success(log10(num), "log10($num)")
                    }
                }

                // Rounding
                normalized.contains("round") -> {
                    val num = extractSingleNumber(normalized)
                    success(round(num), "round($num)")
                }
                normalized.contains("floor") -> {
                    val num = extractSingleNumber(normalized)
                    success(floor(num), "floor($num)")
                }
                normalized.contains("ceiling") || normalized.contains("ceil") -> {
                    val num = extractSingleNumber(normalized)
                    success(ceil(num), "ceil($num)")
                }

                // Other operations
                normalized.contains("absolute") || normalized.contains("abs of") -> {
                    val num = extractSingleNumber(normalized)
                    success(abs(num), "|$num|")
                }
                normalized.contains("factorial") -> {
                    val num = extractSingleNumber(normalized).toInt()
                    if (num < 0) {
                        error("Factorial undefined for negative numbers")
                    } else if (num > 20) {
                        error("Factorial too large (max 20)")
                    } else {
                        success(factorial(num).toDouble(), "$num!")
                    }
                }

                // Constants
                normalized.contains("what is pi") || normalized == "pi" -> {
                    success(PI, "pi")
                }
                normalized.contains("value of e") || normalized == "e" -> {
                    success(E, "e")
                }

                // Fallback: try to parse as simple expression
                else -> {
                    parseSimpleExpression(normalized)
                }
            }
        } catch (e: Exception) {
            error("Could not parse: ${e.message}")
        }
    }

    /**
     * Convert degrees to radians (KMP-compatible replacement for Math.toRadians)
     */
    private fun degreesToRadians(degrees: Double): Double = degrees * PI / 180.0

    /**
     * Check if input contains any of the operation keywords
     */
    private fun containsOperation(input: String, keywords: List<String>): Boolean {
        return keywords.any { input.contains(it) }
    }

    /**
     * Extract single number from input
     * Examples: "square root of 144" -> 144
     */
    private fun extractSingleNumber(input: String): Double {
        val numberPattern = Regex("""-?\d+\.?\d*""")
        val match = numberPattern.find(input)
            ?: throw IllegalArgumentException("No number found in: $input")
        return match.value.toDouble()
    }

    /**
     * Extract two numbers from input
     * Examples: "12 times 10" -> (12, 10)
     */
    private fun extractTwoNumbers(input: String): Pair<Double, Double> {
        val numberPattern = Regex("""-?\d+\.?\d*""")
        val matches = numberPattern.findAll(input).toList()

        if (matches.size < 2) {
            throw IllegalArgumentException("Need two numbers, found ${matches.size}")
        }

        val first = matches[0].value.toDouble()
        val second = matches[1].value.toDouble()

        return Pair(first, second)
    }

    /**
     * Parse arithmetic expression with proper operator precedence (PEMDAS)
     * Supports: +, -, *, /, ^, parentheses
     * Examples: "12 + 10", "5 * 3", "10*3*4/52^2"
     */
    private fun parseSimpleExpression(input: String): CalculationResult {
        // Clean input: keep only digits, operators, parentheses, decimal points
        val cleaned = input.replace(Regex("""[^\d+\-*/^().\s]"""), "").replace(" ", "")

        if (cleaned.isEmpty()) {
            return error("No valid expression found")
        }

        return try {
            val result = evaluateExpression(cleaned)
            success(result, cleaned)
        } catch (e: Exception) {
            error("Could not evaluate: ${e.message}")
        }
    }

    /**
     * Recursive descent parser for arithmetic expressions with PEMDAS precedence
     * Grammar:
     *   expression = term (('+' | '-') term)*
     *   term = power (('*' | '/') power)*
     *   power = factor ('^' power)?   // right associative
     *   factor = number | '(' expression ')' | '-' factor
     */
    private fun evaluateExpression(expr: String): Double {
        val parser = ExpressionParser(expr)
        val result = parser.parseExpression()
        if (parser.hasMore()) {
            throw IllegalArgumentException("Unexpected character at position ${parser.pos}")
        }
        return result
    }

    /**
     * Simple recursive descent expression parser
     */
    private class ExpressionParser(private val expr: String) {
        var pos = 0

        fun hasMore(): Boolean = pos < expr.length

        fun parseExpression(): Double {
            var result = parseTerm()
            while (hasMore() && (peek() == '+' || peek() == '-')) {
                val op = consume()
                val right = parseTerm()
                result = if (op == '+') result + right else result - right
            }
            return result
        }

        private fun parseTerm(): Double {
            var result = parsePower()
            while (hasMore() && (peek() == '*' || peek() == '/')) {
                val op = consume()
                val right = parsePower()
                result = if (op == '*') result * right else {
                    if (right == 0.0) throw ArithmeticException("Division by zero")
                    result / right
                }
            }
            return result
        }

        private fun parsePower(): Double {
            val base = parseFactor()
            return if (hasMore() && peek() == '^') {
                consume() // consume '^'
                val exp = parsePower() // right associative
                base.pow(exp)
            } else {
                base
            }
        }

        private fun parseFactor(): Double {
            skipWhitespace()

            // Handle negative numbers
            if (hasMore() && peek() == '-') {
                consume()
                return -parseFactor()
            }

            // Handle parentheses
            if (hasMore() && peek() == '(') {
                consume() // consume '('
                val result = parseExpression()
                if (!hasMore() || peek() != ')') {
                    throw IllegalArgumentException("Missing closing parenthesis")
                }
                consume() // consume ')'
                return result
            }

            // Parse number
            return parseNumber()
        }

        private fun parseNumber(): Double {
            skipWhitespace()
            val start = pos
            while (hasMore() && (peek().isDigit() || peek() == '.')) {
                pos++
            }
            if (start == pos) {
                throw IllegalArgumentException("Expected number at position $pos")
            }
            return expr.substring(start, pos).toDouble()
        }

        private fun peek(): Char = expr[pos]

        private fun consume(): Char = expr[pos++]

        private fun skipWhitespace() {
            while (hasMore() && peek().isWhitespace()) pos++
        }
    }

    /**
     * Calculate factorial
     */
    private fun factorial(n: Int): Long {
        if (n <= 1) return 1
        var result = 1L
        for (i in 2..n) {
            result *= i
        }
        return result
    }

    /**
     * Create success result with formatted output.
     * Uses KMP-compatible formatting (no String.format).
     */
    private fun success(result: Double, expression: String): CalculationResult {
        val formatted = when {
            result == result.toLong().toDouble() -> result.toLong().toString()
            else -> {
                // Format to 6 decimal places without String.format (KMP-compatible)
                val isNegative = result < 0
                val absResult = kotlin.math.abs(result)
                val scaled = (absResult * 1_000_000).toLong()
                val intPart = absResult.toLong()
                val fracPart = scaled - intPart * 1_000_000
                val fracStr = fracPart.toString().padStart(6, '0').trimEnd('0')
                if (fracStr.isEmpty()) {
                    if (isNegative) "-$intPart" else intPart.toString()
                } else {
                    if (isNegative) "-$intPart.$fracStr" else "$intPart.$fracStr"
                }
            }
        }

        return CalculationResult(
            success = true,
            result = result,
            formattedResult = formatted,
            expression = expression
        )
    }

    /**
     * Create error result
     */
    private fun error(message: String): CalculationResult {
        return CalculationResult(
            success = false,
            error = message
        )
    }
}
